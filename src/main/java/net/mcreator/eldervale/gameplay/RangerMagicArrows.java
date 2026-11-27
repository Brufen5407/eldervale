package net.mcreator.eldervale.gameplay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.core.registries.Registries;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.init.EldervaleModEntities;
import net.mcreator.eldervale.entity.MagicArrowEntity;
import net.mcreator.eldervale.vfx.VfxIds;
import net.mcreator.eldervale.vfx.VfxServer;

/**
 * Ranger skill 2: Magic Arrows.
 * Mana and cooldown stay in RangerSkill2Procedure. This class detects the cast on the server
 * (SKILL2_CD jumps up to its max), then does the backward leap and fires a volley of 3 arrows.
 * Runs only on the server, so it is safe on a dedicated server.
 * Gameplay unchanged: the old particles and sounds are replaced by VFX events (START at the cast,
 * PULSE for every arrow with the arrow entity id so the client can draw its trail).
 */
@EventBusSubscriber
public class RangerMagicArrows {

	// ---- Settings (edit here) ----
	private static final double RANGE = 7.5D;          // how far it looks for enemies (same as the old skill)
	private static final int ARROWS = 3;               // arrows per cast
	private static final int SHOT_DELAY = 3;           // ticks between arrows (volley)
	private static final float ARROW_SPEED = 2.5F;
	private static final float INACCURACY = 0.6F;      // 0 = perfectly accurate
	private static final double ARROW_ATT_MULT = 2.5D; // x ATTACK (Ranger ATT 1.2 -> 3.0, same as before)
    private static final double ARROW_FALLBACK_DAMAGE = 3.0D;
	private static final int KNOCKBACK = 1;
	private static final float FAN_ANGLE = 10.0F;      // degrees between arrows when there is no enemy
	private static final boolean NEED_LINE_OF_SIGHT = true;

	// Backward leap. Set DO_LEAP to false if you keep the leap block in RangerSkill2Procedure.
	private static final boolean DO_LEAP = true;
	private static final double LEAP_BACK = 0.7D;
	private static final double LEAP_UP = 0.3D;

	private static class Shot {
		final UUID player;
		final UUID target; // null = fire forward
		final int index;
		final float yawOffset;
		int delay;

		Shot(UUID player, UUID target, int index, float yawOffset, int delay) {
			this.player = player;
			this.target = target;
			this.index = index;
			this.yawOffset = yawOffset;
			this.delay = delay;
		}
	}

	private static final Map<UUID, Double> PREV_CD = new HashMap<>();
	private static final List<Shot> PENDING = new ArrayList<>();

	// ---------------------------------------------------------------
	//  Cast detection
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		double cd = v.SKILL2_CD;
		Double prev = PREV_CD.put(player.getUUID(), cd);
		if (prev == null)
			return;

		if ("ranger".equals(v.PLAYER_CLASS) && player.isAlive() && cd - prev > 50.0D && cd >= v.SKILL2_MAX_CD - 10.0D)
			cast(player);
	}

	private static void cast(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		VfxServer.send(player, VfxIds.RANGER_ARROWS, VfxIds.START, 0, 3.0F);

		// nearest hostile targets that the ranger can see
		AABB box = player.getBoundingBox().inflate(RANGE);
		List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box,
				e -> e != player && e.isAlive() && e instanceof Enemy && e.distanceToSqr(player) <= RANGE * RANGE && (!NEED_LINE_OF_SIGHT || player.hasLineOfSight(e)));
		found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
		List<LivingEntity> targets = found.size() > ARROWS ? found.subList(0, ARROWS) : found;

		// one enemy: all arrows at it. Two: A, B, A. Three: one each. None: a fan in front of the ranger.
		for (int i = 0; i < ARROWS; i++) {
			UUID targetId = targets.isEmpty() ? null : targets.get(i % targets.size()).getUUID();
			float yawOffset = targetId == null ? (i - (ARROWS - 1) / 2.0F) * FAN_ANGLE : 0.0F;
			PENDING.add(new Shot(player.getUUID(), targetId, i, yawOffset, i * SHOT_DELAY));
		}

		if (DO_LEAP)
			leap(player);
	}

	private static void leap(ServerPlayer player) {
		double yaw = Math.toRadians(player.getYRot());
		double bx = Math.sin(yaw); // opposite of where the ranger is facing
		double bz = -Math.cos(yaw);
		player.setDeltaMovement(bx * LEAP_BACK, LEAP_UP, bz * LEAP_BACK);
		player.hurtMarked = true;
		player.connection.send(new ClientboundSetEntityMotionPacket(player));
	}

	// ---------------------------------------------------------------
	//  Volley
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (PENDING.isEmpty())
			return;
		MinecraftServer server = event.getServer();
		Iterator<Shot> it = PENDING.iterator();
		while (it.hasNext()) {
			Shot s = it.next();
			if (s.delay-- > 0)
				continue;
			it.remove();
			ServerPlayer player = server.getPlayerList().getPlayer(s.player);
			if (player != null && player.isAlive())
				fire(player, s);
		}
	}

	private static void fire(ServerPlayer player, Shot s) {
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.getEyePosition().add(0.0D, -0.25D, 0.0D);

		LivingEntity target = null;
		if (s.target != null && level.getEntity(s.target) instanceof LivingEntity le && le.isAlive())
			target = le;

		Vec3 dir;
		if (target != null) {
			Vec3 aim = new Vec3(target.getX(), target.getY(0.6D), target.getZ());
			// lead the target a little so moving enemies are hit
			double dist = aim.distanceTo(origin);
			Vec3 vel = target.getDeltaMovement();
			aim = aim.add(vel.x * dist / ARROW_SPEED, 0.0D, vel.z * dist / ARROW_SPEED);
			dir = aim.subtract(origin);
		} else {
			dir = player.getLookAngle().yRot((float) -Math.toRadians(s.yawOffset));
		}
		if (dir.lengthSqr() < 1.0E-6D)
			return;
		Vec3 n = dir.normalize();

		MagicArrowEntity arrow = new MagicArrowEntity(EldervaleModEntities.MAGIC_ARROW.get(), 0, 0, 0, level, weapon(level));
		arrow.setOwner(player);
		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
        arrow.setBaseDamage(v.ATTACK > 0.0D ? v.ATTACK * ARROW_ATT_MULT : ARROW_FALLBACK_DAMAGE);
		arrow.setSilent(true);
		arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
		arrow.setPos(origin.x + n.x, origin.y + n.y, origin.z + n.z);
		arrow.shoot(n.x, n.y, n.z, ARROW_SPEED, INACCURACY);
		level.addFreshEntity(arrow);

		// VFX: muzzle flash + the arrow id (so the client draws the trail and the hit)
		VfxServer.sendAt(player, VfxIds.RANGER_ARROWS, VfxIds.PULSE, arrow.getId(), origin.x + n.x, origin.y + n.y, origin.z + n.z, s.index, n.x, n.y, n.z);
	}

	private static ItemStack weapon(Level level) {
		ItemStack weapon = new ItemStack(Items.ARROW);
		if (KNOCKBACK > 0)
			weapon.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.KNOCKBACK), KNOCKBACK);
		return weapon;
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
		PENDING.clear();
	}
}
