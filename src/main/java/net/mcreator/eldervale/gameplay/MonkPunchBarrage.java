package net.mcreator.eldervale.gameplay;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Monk skill: PUNCH BARRAGE.
 *  1. The monk picks an enemy.
 *  2. He teleports in front of it.
 *  3. He throws 5 punches in a row.
 *  4. Each punch deals 40% of ATT.
 *  5. If at least one punch lands, he gains 1 combo point (Flow State).
 *
 * Mana (25) and cooldown (7s = 140 ticks) are handled by a small MCreator procedure.
 * This class detects the cast on the server (the cooldown jumps up), and if there is
 * no valid target it gives the mana and the cooldown back.
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class MonkPunchBarrage {

	// ---- Settings (edit here) ----
	private static final int SKILL_SLOT = 1;              
	private static final double MANA_COST = 25.0D;         // must match the MCreator procedure (used for the refund)
	private static final int PUNCHES = 5;
	private static final int PUNCH_INTERVAL = 4;           // ticks between punches (4 = 0.2s)
	private static final double DAMAGE_FACTOR = 0.40D;     // each punch deals 40% of ATT
	private static final double RANGE = 10.0D;             // how far the monk can spot an enemy
	private static final double FRONT_DISTANCE = 1.4D;     // how close to the enemy he appears
	private static final double REACH = 3.2D;              // if the enemy gets farther than this, the monk follows it
	private static final boolean NEED_LINE_OF_SIGHT = true;
	private static final boolean EFFECTS_ENABLED = true;
	private static final boolean SOUNDS_ENABLED = true;

	private static final DustParticleOptions TEAL = new DustParticleOptions(new Vector3f(0.35F, 0.95F, 0.90F), 1.0F);
	private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.00F, 0.85F, 0.30F), 1.1F);

	private static class Barrage {
		final UUID player;
		final UUID target;
		LivingEntity lastTarget;
		int done = 0;
		int delay;
		boolean anyHit = false;

		Barrage(UUID player, UUID target, LivingEntity lastTarget, int delay) {
			this.player = player;
			this.target = target;
			this.lastTarget = lastTarget;
			this.delay = delay;
		}
	}

	private static final Map<UUID, Double> PREV_CD = new HashMap<>();
	private static final Map<UUID, Barrage> ACTIVE = new HashMap<>();

	// ---------------------------------------------------------------
	//  Cast detection (the MCreator procedure sets the cooldown)
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		double cd = SKILL_SLOT == 1 ? v.SKILL1_CD : v.SKILL2_CD;
		double max = SKILL_SLOT == 1 ? v.SKILL1_MAX_CD : v.SKILL2_MAX_CD;
		Double prev = PREV_CD.put(player.getUUID(), cd);
		if (prev == null)
			return;

		if ("monk".equals(v.PLAYER_CLASS) && player.isAlive() && !ACTIVE.containsKey(player.getUUID()) && cd - prev > 50.0D && cd >= max - 10.0D)
			cast(player, v);
	}

	private static void cast(ServerPlayer player, EldervaleModVariables.PlayerVariables v) {
		ServerLevel level = player.serverLevel();

		LivingEntity target = findTarget(player);
		if (target == null) {
			fail(player, v, "No target in range");
			return;
		}
		Vec3 dest = findDestination(player, target);
		if (dest == null) {
			fail(player, v, "No room to reach the target");
			return;
		}

		Vec3 from = player.position();
		departureFx(level, from);

		// face the target and teleport in front of it
		float[] rot = rotationTo(dest, player.getEyeHeight(), target);
		player.connection.teleport(dest.x, dest.y, dest.z, rot[0], rot[1]);
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0.0F;

		arrivalFx(level, dest);
		trailFx(level, from, dest);

		ACTIVE.put(player.getUUID(), new Barrage(player.getUUID(), target.getUUID(), target, 2));
	}

	// ---------------------------------------------------------------
	//  Choosing the target and the landing spot
	// ---------------------------------------------------------------
	private static LivingEntity findTarget(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		AABB box = player.getBoundingBox().inflate(RANGE);

		List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && e instanceof Enemy && e.distanceToSqr(player) <= RANGE * RANGE && (!NEED_LINE_OF_SIGHT || player.hasLineOfSight(e)));

		LivingEntity best = null;
		double bestScore = -1.0E9D;
		for (LivingEntity e : list) {
			Vec3 to = new Vec3(e.getX(), e.getY(0.5D), e.getZ()).subtract(eye);
			double dist = to.length();
			if (dist < 1.0E-4D)
				continue;
			double dot = to.scale(1.0D / dist).dot(look);
			double score = dot * 2.0D - dist / RANGE; // prefer what he looks at, then the closest
			if (score > bestScore) {
				bestScore = score;
				best = e;
			}
		}
		return best;
	}

	// The spot on the monk's side of the enemy; if blocked, try other angles around it
	private static Vec3 findDestination(ServerPlayer player, LivingEntity target) {
		Vec3 t = target.position();
		Vec3 from = player.position();
		double baseAngle = Math.atan2(from.z - t.z, from.x - t.x);
		double[] offsets = {0.0D, Math.PI / 4, -Math.PI / 4, Math.PI / 2, -Math.PI / 2, 3 * Math.PI / 4, -3 * Math.PI / 4, Math.PI};
		for (double off : offsets) {
			double a = baseAngle + off;
			Vec3 dest = new Vec3(t.x + Math.cos(a) * FRONT_DISTANCE, t.y, t.z + Math.sin(a) * FRONT_DISTANCE);
			AABB moved = player.getBoundingBox().move(dest.subtract(player.position()));
			if (player.level().noCollision(player, moved))
				return dest;
		}
		return null;
	}

	private static float[] rotationTo(Vec3 pos, float eyeHeight, LivingEntity target) {
		double dx = target.getX() - pos.x;
		double dz = target.getZ() - pos.z;
		double dy = target.getY(0.5D) - (pos.y + eyeHeight);
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
		return new float[]{yaw, pitch};
	}

	private static void fail(ServerPlayer player, EldervaleModVariables.PlayerVariables v, String message) {
		// give the mana and the cooldown back
		v.MANA = Math.min(v.MANA_MAX, v.MANA + MANA_COST);
		if (SKILL_SLOT == 1)
			v.SKILL1_CD = 0;
		else
			v.SKILL2_CD = 0;
		v.markSyncDirty();
		player.displayClientMessage(Component.literal(message), true);
		if (SOUNDS_ENABLED)
			player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 0.6F, 1.2F);
	}

	// ---------------------------------------------------------------
	//  The punches
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (ACTIVE.isEmpty())
			return;
		MinecraftServer server = event.getServer();
		Iterator<Barrage> it = ACTIVE.values().iterator();
		while (it.hasNext()) {
			Barrage b = it.next();
			if (b.delay-- > 0)
				continue;

			ServerPlayer player = server.getPlayerList().getPlayer(b.player);
			Entity entity = player == null ? null : player.serverLevel().getEntity(b.target);
			if (player == null || !player.isAlive() || !(entity instanceof LivingEntity target) || !target.isAlive()) {
				finish(player, b);
				it.remove();
				continue;
			}

			b.lastTarget = target;
			punch(player, target, b);
			b.done++;
			if (b.done >= PUNCHES) {
				finish(player, b);
				it.remove();
			} else {
				b.delay = PUNCH_INTERVAL - 1;
			}
		}
	}

	private static void punch(ServerPlayer player, LivingEntity target, Barrage b) {
		ServerLevel level = player.serverLevel();

		// stay next to the enemy and keep looking at it
		if (player.distanceToSqr(target) > REACH * REACH) {
			Vec3 dest = findDestination(player, target);
			if (dest != null) {
				float[] rot = rotationTo(dest, player.getEyeHeight(), target);
				player.connection.teleport(dest.x, dest.y, dest.z, rot[0], rot[1]);
				player.setDeltaMovement(Vec3.ZERO);
			}
		} else {
			float[] rot = rotationTo(player.position(), player.getEyeHeight(), target);
			float diff = Math.abs(net.minecraft.util.Mth.wrapDegrees(rot[0] - player.getYRot()));
			if (diff > 25.0F) {
				player.connection.teleport(player.getX(), player.getY(), player.getZ(), rot[0], rot[1]);
				player.setDeltaMovement(Vec3.ZERO);
			}
		}

		// ATT: the monk's ATT stat (or the attack damage attribute if the stat is not set)
		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		double att = v.ATTACK > 0.0D ? v.ATTACK : player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float damage = (float) (att * DAMAGE_FACTOR);

		// alternate fists
		player.swing(b.done % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);

		// the source has no direct entity so these punches do not count as separate Flow State hits
		DamageSource base = level.damageSources().playerAttack(player);
		DamageSource source = new DamageSource(base.typeHolder(), null, player);
		target.invulnerableTime = 0; // every punch must land, no invulnerability frames
		boolean hit = target.hurt(source, damage);
		if (hit)
			b.anyHit = true;

		punchFx(level, target, b.done, b.done == PUNCHES - 1, hit);
	}

	private static void finish(ServerPlayer player, Barrage b) {
		if (player == null || !player.isAlive() || !b.anyHit || b.lastTarget == null)
			return;
		MonkFlowState.registerHit(player, b.lastTarget); // +1 combo point
		player.displayClientMessage(Component.literal("COMBO +1"), true);
	}

	// ---------------------------------------------------------------
	//  Effects and sounds (sent by the server, everyone sees and hears them)
	// ---------------------------------------------------------------
	private static void departureFx(ServerLevel level, Vec3 p) {
		if (EFFECTS_ENABLED) {
			level.sendParticles(ParticleTypes.CLOUD, p.x, p.y + 0.9D, p.z, 12, 0.3D, 0.5D, 0.3D, 0.05D);
			level.sendParticles(TEAL, p.x, p.y + 0.9D, p.z, 16, 0.4D, 0.6D, 0.4D, 0.02D);
			level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 0.9D, p.z, 10, 0.3D, 0.6D, 0.3D, 0.1D);
		}
		if (SOUNDS_ENABLED)
			level.playSound(null, p.x, p.y, p.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.6F);
	}

	private static void arrivalFx(ServerLevel level, Vec3 p) {
		if (EFFECTS_ENABLED) {
			level.sendParticles(TEAL, p.x, p.y + 0.9D, p.z, 16, 0.4D, 0.6D, 0.4D, 0.02D);
			for (int i = 0; i < 14; i++) {
				double a = i * (Math.PI * 2.0D / 14.0D);
				level.sendParticles(ParticleTypes.END_ROD, p.x + Math.cos(a) * 0.5D, p.y + 0.15D, p.z + Math.sin(a) * 0.5D, 0, Math.cos(a), 0.1D, Math.sin(a), 0.12D);
			}
		}
		if (SOUNDS_ENABLED) {
			level.playSound(null, p.x, p.y, p.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.9F);
			level.playSound(null, p.x, p.y, p.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.7F);
		}
	}

	private static void trailFx(ServerLevel level, Vec3 from, Vec3 to) {
		if (!EFFECTS_ENABLED)
			return;
		int n = (int) Math.min(24.0D, from.distanceTo(to) * 3.0D);
		for (int i = 1; i < n; i++) {
			double f = i / (double) n;
			double x = from.x + (to.x - from.x) * f;
			double y = from.y + (to.y - from.y) * f + 0.9D;
			double z = from.z + (to.z - from.z) * f;
			level.sendParticles(TEAL, x, y, z, 1, 0.05D, 0.2D, 0.05D, 0.0D);
			if (i % 2 == 0)
				level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.05D, 0.1D, 0.05D, 0.01D);
		}
	}

	private static void punchFx(ServerLevel level, LivingEntity target, int index, boolean last, boolean hit) {
		double tx = target.getX(), ty = target.getY(0.6D), tz = target.getZ();

		if (EFFECTS_ENABLED && hit) {
			double side = index % 2 == 0 ? 0.25D : -0.25D;
			level.sendParticles(ParticleTypes.CRIT, tx + side, ty, tz, 7, 0.25D, 0.3D, 0.25D, 0.3D);
			level.sendParticles(last ? GOLD : TEAL, tx + side, ty, tz, 6, 0.25D, 0.3D, 0.25D, 0.02D);
			if (last) {
				level.sendParticles(ParticleTypes.EXPLOSION, tx, ty, tz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
				for (int i = 0; i < 12; i++) {
					double a = i * (Math.PI * 2.0D / 12.0D);
					level.sendParticles(ParticleTypes.END_ROD, tx, ty, tz, 0, Math.cos(a), 0.1D, Math.sin(a), 0.2D);
				}
			}
		}

		if (SOUNDS_ENABLED) {
			level.playSound(null, tx, ty, tz, SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.PLAYERS, 0.8F, 0.9F + index * 0.08F);
			if (last) {
				level.playSound(null, tx, ty, tz, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.9F, 0.8F);
				level.playSound(null, tx, ty, tz, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 1.0F);
			}
		}
	}

	// ---------------------------------------------------------------
	//  Cleanup
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		UUID id = event.getEntity().getUUID();
		PREV_CD.remove(id);
		ACTIVE.remove(id);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PREV_CD.clear();
		ACTIVE.clear();
	}
}
