package net.mcreator.eldervale.gameplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Mage skills: ARCANE NOVA (skill 1) and METEOR STRIKE (skill 2).
 *
 * Mana and cooldown stay in the MCreator procedures MageSkill1 (40 mana, 240 ticks) and
 * MageSkill2 (65 mana, 400 ticks). This class detects the cast on the server (the SKILL1_CD /
 * SKILL2_CD value jumps up to its maximum), the same pattern used by the Assassin skills.
 *
 * ARCANE NOVA   : magic explosion around the Mage, damages and knocks back every monster within NOVA_RADIUS.
 * METEOR STRIKE : the point is the block the Mage looks at, within METEOR_RANGE (if nothing is hit, the
 *                 ground under the end of the line of sight). A meteor falls for METEOR_FALL_TICKS, then
 *                 explodes and damages every monster within METEOR_RADIUS. No block damage.
 *
 * Both casts feed MageArcaneMastery (passive) and use its multiplier on the damage.
 * Damage uses playerAttack as source so that XP on kill works (XpLevelProcedure checks it).
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class MageSkills {

	// ---- Settings (edit here) ----
	// Arcane Nova
	private static final double NOVA_RADIUS = 5.0D;
	private static final double NOVA_DAMAGE_MULT = 6.0D;     // x ATT (Mage ATT is low by design)
	private static final double NOVA_KNOCKBACK = 1.2D;
	private static final double NOVA_LIFT = 0.35D;
	// Meteor Strike
	private static final double METEOR_RANGE = 12.0D;
	private static final double METEOR_RADIUS = 4.0D;
	private static final double METEOR_DAMAGE_MULT = 14.0D;  // x ATT
	private static final int METEOR_FALL_TICKS = 20;         // 1 s
	private static final double METEOR_START_HEIGHT = 14.0D;

	/** previous {SKILL1_CD, SKILL2_CD} per player, updated in place (no allocation per tick) */
	private static final Map<UUID, double[]> PREV_CD = new HashMap<>();

	private static final class Meteor {
		final ServerLevel level;
		final Vec3 pos;
		final UUID owner;
		int elapsed;

		Meteor(ServerLevel level, Vec3 pos, UUID owner) {
			this.level = level;
			this.pos = pos;
			this.owner = owner;
		}
	}

	private static final List<Meteor> METEORS = new ArrayList<>();

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		double cd1 = v.SKILL1_CD;
		double cd2 = v.SKILL2_CD;
		double[] prev = PREV_CD.get(player.getUUID());
		if (prev == null) {
			PREV_CD.put(player.getUUID(), new double[]{cd1, cd2});
			return;
		}
		double p1 = prev[0];
		double p2 = prev[1];
		prev[0] = cd1;
		prev[1] = cd2;

		if (!"mage".equals(v.PLAYER_CLASS) || !player.isAlive())
			return;

		// cast: the cooldown jumped up to its maximum
		if (cd1 - p1 > 50.0D && cd1 >= v.SKILL1_MAX_CD - 10.0D) {
			MageArcaneMastery.onCast(player);
			arcaneNova(player, v);
		}
		if (cd2 - p2 > 50.0D && cd2 >= v.SKILL2_MAX_CD - 10.0D) {
			MageArcaneMastery.onCast(player);
			meteorStrike(player);
		}
	}

	// ---------------------------------------------------------------
	//  SKILL 1 - ARCANE NOVA
	// ---------------------------------------------------------------
	private static void arcaneNova(ServerPlayer player, EldervaleModVariables.PlayerVariables v) {
		ServerLevel level = player.serverLevel();
		float damage = (float) (v.ATTACK * NOVA_DAMAGE_MULT * MageArcaneMastery.multiplier(player));

		for (Monster m : level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(NOVA_RADIUS), e -> e.isAlive())) {
			if (m.distanceToSqr(player) > NOVA_RADIUS * NOVA_RADIUS)
				continue;
			m.hurt(level.damageSources().playerAttack(player), damage);

			// knockback away from the Mage
			double dx = m.getX() - player.getX();
			double dz = m.getZ() - player.getZ();
			double len = Math.sqrt(dx * dx + dz * dz);
			if (len < 1.0E-3D) {
				dx = 1.0D;
				dz = 0.0D;
				len = 1.0D;
			}
			m.setDeltaMovement(m.getDeltaMovement().add(dx / len * NOVA_KNOCKBACK, NOVA_LIFT, dz / len * NOVA_KNOCKBACK));
			m.hurtMarked = true;
		}

		// visual: expanding arcane rings + burst in the centre
		double cx = player.getX();
		double cy = player.getY() + 0.2D;
		double cz = player.getZ();
		for (int ring = 1; ring <= 3; ring++) {
			double r = NOVA_RADIUS * ring / 3.0D;
			int points = ring * 12;
			for (int i = 0; i < points; i++) {
				double a = Math.PI * 2.0D * i / points;
				level.sendParticles(ParticleTypes.END_ROD, cx + Math.cos(a) * r, cy, cz + Math.sin(a) * r, 1, 0.0D, 0.05D, 0.0D, 0.02D);
			}
		}
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, cx, cy + 0.8D, cz, 30, 0.8D, 0.6D, 0.8D, 0.3D);
		level.sendParticles(ParticleTypes.EXPLOSION, cx, cy + 0.5D, cz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		level.playSound(null, cx, player.getY(), cz, SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.8F);
		level.playSound(null, cx, player.getY(), cz, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 0.8F, 1.4F);
	}

	// ---------------------------------------------------------------
	//  SKILL 2 - METEOR STRIKE
	// ---------------------------------------------------------------
	private static void meteorStrike(ServerPlayer player) {
		ServerLevel level = player.serverLevel();

		// selected point: where the Mage looks, within METEOR_RANGE
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(METEOR_RANGE));
		BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 point = hit.getLocation();
		if (hit.getType() == HitResult.Type.MISS) {
			// nothing in the line of sight: use the ground under its end (if any)
			BlockHitResult ground = level.clip(new ClipContext(end, end.add(0.0D, -METEOR_RANGE, 0.0D), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
			if (ground.getType() != HitResult.Type.MISS)
				point = ground.getLocation();
		}

		METEORS.add(new Meteor(level, point, player.getUUID()));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.6F);
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (METEORS.isEmpty())
			return;

		Iterator<Meteor> it = METEORS.iterator();
		while (it.hasNext()) {
			Meteor m = it.next();
			m.elapsed++;
			double p = Math.min(1.0D, (double) m.elapsed / METEOR_FALL_TICKS);

			// falling meteor (accelerating) + warning circle on the ground
			double y = m.pos.y + METEOR_START_HEIGHT * (1.0D - p * p);
			m.level.sendParticles(ParticleTypes.FLAME, m.pos.x, y, m.pos.z, 8, 0.3D, 0.3D, 0.3D, 0.02D);
			m.level.sendParticles(ParticleTypes.LARGE_SMOKE, m.pos.x, y + 0.5D, m.pos.z, 3, 0.3D, 0.3D, 0.3D, 0.02D);
			if (m.elapsed % 5 == 1) {
				for (int i = 0; i < 16; i++) {
					double a = Math.PI * 2.0D * i / 16.0D;
					m.level.sendParticles(ParticleTypes.END_ROD, m.pos.x + Math.cos(a) * METEOR_RADIUS, m.pos.y + 0.1D, m.pos.z + Math.sin(a) * METEOR_RADIUS, 1, 0.0D, 0.0D, 0.0D, 0.0D);
				}
			}

			if (m.elapsed >= METEOR_FALL_TICKS) {
				impact(m);
				it.remove();
			}
		}
	}

	private static void impact(Meteor m) {
		ServerLevel level = m.level;
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, m.pos.x, m.pos.y + 0.5D, m.pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		level.sendParticles(ParticleTypes.FLAME, m.pos.x, m.pos.y + 0.5D, m.pos.z, 40, METEOR_RADIUS * 0.4D, 0.4D, METEOR_RADIUS * 0.4D, 0.15D);
		level.playSound(null, m.pos.x, m.pos.y, m.pos.z, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.PLAYERS, 1.5F, 0.7F);

		// the caster must still be online to deal damage (kill credit and ATT come from it)
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(m.owner);
		if (owner == null || !owner.isAlive())
			return;
		EldervaleModVariables.PlayerVariables v = owner.getData(EldervaleModVariables.PLAYER_VARIABLES);
		float damage = (float) (v.ATTACK * METEOR_DAMAGE_MULT * MageArcaneMastery.multiplier(owner));

		net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(m.pos, m.pos).inflate(METEOR_RADIUS, 3.0D, METEOR_RADIUS);
		for (Monster t : level.getEntitiesOfClass(Monster.class, box, e -> e.isAlive())) {
			if (t.distanceToSqr(m.pos) <= METEOR_RADIUS * METEOR_RADIUS)
				t.hurt(level.damageSources().playerAttack(owner), damage);
		}
	}

	// ---------------------------------------------------------------
	//  Cleanup
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		PREV_CD.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PREV_CD.clear();
		METEORS.clear();
	}
}
