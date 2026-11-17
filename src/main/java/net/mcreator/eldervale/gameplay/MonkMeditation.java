package net.mcreator.eldervale.gameplay;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Monk skill: MEDITATION.
 * The monk cannot move for 5 seconds. Every second (5 pulses) he heals 2 HP
 * and pushes the hostile monsters around him away.
 *
 * Mana (35) and cooldown (400 ticks) stay in the MCreator procedure. This class detects the cast
 * on the server (the cooldown jumps up) and does the rest, so the pulses are really one per second
 * and the push goes away from the monk. The movement block is always removed at the end,
 * even if the monk dies or leaves the server.
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class MonkMeditation {

	// ---- Settings (edit here) ----
	private static final int SKILL_SLOT = 2;               // 1 = SKILL1 (key V), 2 = SKILL2 (key B)
	private static final int PULSES = 10;                  
	private static final int PULSE_INTERVAL = 10;          // ticks between pulses (10 = 0.5 second)
	private static final float HEAL_PER_PULSE = 2.0F;
	private static final double PUSH_RADIUS = 5.0D;        // same area as the old procedure
	private static final double PUSH_STRENGTH = 2.0D;      // horizontal push 
	private static final double PUSH_UP = 0.35D;
	private static final boolean FREEZE_MOVEMENT = true;

	private static final ResourceLocation FREEZE_ID = ResourceLocation.fromNamespaceAndPath("eldervale", "meditation_freeze");

	private static final Map<UUID, Double> PREV_CD = new HashMap<>();
	private static final Map<UUID, Integer> ACTIVE = new HashMap<>(); // ticks since the cast

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		UUID id = player.getUUID();
		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		boolean monk = "monk".equals(v.PLAYER_CLASS);
		double cd = SKILL_SLOT == 1 ? v.SKILL1_CD : v.SKILL2_CD;
		double max = SKILL_SLOT == 1 ? v.SKILL1_MAX_CD : v.SKILL2_MAX_CD;
		Double prev = PREV_CD.put(id, cd);

		// meditation in progress
		Integer elapsed = ACTIVE.get(id);
		if (elapsed != null) {
			if (!monk || !player.isAlive()) {
				end(player);
				return;
			}
			int e = elapsed + 1;
			ACTIVE.put(id, e);
			if (e % PULSE_INTERVAL == 0)
				pulse(player);
			if (e >= PULSES * PULSE_INTERVAL)
				end(player);
			return;
		}

		// cast: the cooldown jumped up to its maximum
		if (prev != null && monk && player.isAlive() && cd - prev > 50.0D && cd >= max - 10.0D) {
			ACTIVE.put(id, 0);
			setFreeze(player, true);
		}
	}

	private static void pulse(ServerPlayer player) {
		player.heal(HEAL_PER_PULSE);

		ServerLevel level = player.serverLevel();
		AABB area = new AABB(player.getX(), player.getY(), player.getZ(), player.getX(), player.getY(), player.getZ()).inflate(PUSH_RADIUS);
		List<LivingEntity> near = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive() && e instanceof Enemy);
		for (LivingEntity e : near) {
			// push away from the monk (not along the direction the monster is facing)
			double dx = e.getX() - player.getX();
			double dz = e.getZ() - player.getZ();
			double len = Math.sqrt(dx * dx + dz * dz);
			if (len < 1.0E-4D) {
				double yaw = Math.toRadians(player.getYRot());
				dx = -Math.sin(yaw);
				dz = Math.cos(yaw);
			} else {
				dx /= len;
				dz /= len;
			}
			e.setDeltaMovement(dx * PUSH_STRENGTH, PUSH_UP, dz * PUSH_STRENGTH);
			e.hurtMarked = true;
		}
	}

	private static void end(ServerPlayer player) {
		setFreeze(player, false);
		ACTIVE.remove(player.getUUID());
	}

	private static void setFreeze(ServerPlayer player, boolean on) {
		if (!FREEZE_MOVEMENT)
			return;
		AttributeInstance inst = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (inst == null)
			return;
		if (inst.getModifier(FREEZE_ID) != null)
			inst.removeModifier(FREEZE_ID);
		if (on)
			inst.addTransientModifier(new AttributeModifier(FREEZE_ID, -1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	// ---------------------------------------------------------------
	//  Cleanup
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		ACTIVE.remove(event.getEntity().getUUID());
	}

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
