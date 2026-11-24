package net.mcreator.eldervale.gameplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Werewolf skill 2: FERAL POUNCE.
 *  - Leaps toward the nearest monster within RANGE blocks.
 *  - On landing: area damage (ATT x DAMAGE_MULT) to every monster within RADIUS of the landing spot,
 *    and Slowness on them.
 *  - No target in range: mana and cooldown are refunded.
 *
 * Mana (20) and cooldown (200 ticks = 10 s) stay in the MCreator procedure WerewolfSkill2.
 * This class detects the cast on the server (the SKILL2 cooldown jumps up), the same pattern used
 * by AssassinShadowStrike / MonkPunchBarrage. Server only (no client classes): safe on a dedicated server.
 */
@EventBusSubscriber
public class WerewolfFeralPounce {

	// ---- Settings (edit here) ----
	private static final double RANGE = 8.0D;             // target search range
	private static final double RADIUS = 3.5D;            // area of the landing attack
	private static final double DAMAGE_MULT = 2.0D;       // x ATT
	private static final int SLOW_TICKS = 60;             // 3 s
	private static final int SLOW_AMPLIFIER = 1;          // Slowness II
	private static final double LAUNCH_UP = 0.55D;        // vertical launch speed
	private static final double MIN_HSPEED = 0.4D;        // horizontal launch speed limits
	private static final double MAX_HSPEED = 1.2D;
	private static final double DIST_DIVISOR = 7.5D;      // distance / divisor = horizontal speed (about 12 ticks of flight)
	private static final int MIN_AIR_TICKS = 4;           // ignore onGround right after the launch
	private static final int MAX_AIR_TICKS = 40;          // safety: land anyway after 2 s
	private static final double MANA_COST = 20.0D;        // same value as the MCreator procedure (used for the refund)

	private static final Map<UUID, Double> PREV_CD = new HashMap<>();
	private static final Map<UUID, Integer> AIRBORNE = new HashMap<>();

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		UUID id = player.getUUID();
		double cd = v.SKILL2_CD;
		Double prev = PREV_CD.put(id, cd);

		// cast: the cooldown jumped up to its maximum
		if (prev != null && "werewolf".equals(v.PLAYER_CLASS) && player.isAlive() && !AIRBORNE.containsKey(id) && cd - prev > 50.0D && cd >= v.SKILL2_MAX_CD - 10.0D) {
			if (launch(player))
				AIRBORNE.put(id, 0);
			else
				refund(v);
		}

		// flight: wait for the landing
		Integer ticks = AIRBORNE.get(id);
		if (ticks == null)
			return;
		if (!player.isAlive()) {
			AIRBORNE.remove(id);
			return;
		}
		ticks++;
		if ((ticks >= MIN_AIR_TICKS && player.onGround()) || ticks >= MAX_AIR_TICKS) {
			AIRBORNE.remove(id);
			land(player, v);
		} else {
			AIRBORNE.put(id, ticks);
		}
	}

	/** @return false if the skill could not be used (nothing was changed) */
	private static boolean launch(ServerPlayer player) {
		ServerLevel level = player.serverLevel();

		// nearest monster within RANGE (real distance, not a box)
		Monster target = null;
		double best = RANGE * RANGE;
		for (Monster m : level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(RANGE), e -> e.isAlive())) {
			double d = m.distanceToSqr(player);
			if (d <= best) {
				best = d;
				target = m;
			}
		}
		if (target == null)
			return false;

		double dx = target.getX() - player.getX();
		double dz = target.getZ() - player.getZ();
		double dist = Math.sqrt(dx * dx + dz * dz);
		double speed = Mth.clamp(dist / DIST_DIVISOR, MIN_HSPEED, MAX_HSPEED);
		Vec3 dir = dist < 1.0E-4D ? Vec3.ZERO : new Vec3(dx / dist, 0.0D, dz / dist);

		player.setDeltaMovement(dir.x * speed, LAUNCH_UP, dir.z * speed);
		player.hurtMarked = true; // send the new motion to the client

		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 1.2F, 0.8F);
		level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.2D, player.getZ(), 15, 0.3D, 0.1D, 0.3D, 0.05D);
		return true;
	}

	private static void land(ServerPlayer player, EldervaleModVariables.PlayerVariables v) {
		ServerLevel level = player.serverLevel();
		player.resetFallDistance();

		// playerAttack: the source is the player, so XP and passives work
		float damage = (float) (v.ATTACK * DAMAGE_MULT);
		double r2 = RADIUS * RADIUS;
		for (Monster m : level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(RADIUS), e -> e.isAlive() && e.distanceToSqr(player) <= r2)) {
			m.hurt(level.damageSources().playerAttack(player), damage);
			m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, SLOW_AMPLIFIER));
		}

		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1.5F, 0.7F);
		level.sendParticles(ParticleTypes.CRIT, player.getX(), player.getY() + 0.2D, player.getZ(), 30, 1.2D, 0.2D, 1.2D, 0.2D);
		level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.1D, player.getZ(), 20, 1.0D, 0.1D, 1.0D, 0.05D);
	}

	private static void refund(EldervaleModVariables.PlayerVariables v) {
		v.MANA = Math.min(v.MANA_MAX, v.MANA + MANA_COST);
		v.SKILL2_CD = 0;
		v.markSyncDirty();
	}

	// ---------------------------------------------------------------
	//  Cleanup
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		UUID id = event.getEntity().getUUID();
		PREV_CD.remove(id);
		AIRBORNE.remove(id);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PREV_CD.clear();
		AIRBORNE.clear();
	}
}
