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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Assassin skill: SHADOW STRIKE.
 *  - Dashes behind the nearest monster within RANGE blocks and strikes it.
 *  - Damage = ATT x DAMAGE_MULT, or ATT x BONUS_MULT if the target was NOT looking at the
 *    Assassin when the skill was cast (checked before the teleport).
 *  - The Assassin becomes invisible for 4 s.
 *  - No target in range, or no free space behind it: mana and cooldown are refunded.
 *
 * Mana (25) and cooldown (160 ticks = 8 s) stay in the MCreator procedure AssassinSkill1.
 * This class detects the cast on the server (the SKILL1 cooldown jumps up), the same
 * pattern used by MonkPunchBarrage and AssassinSmokeBomb.
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class AssassinShadowStrike {

	// ---- Settings (edit here) ----
	private static final double RANGE = 8.0D;
	private static final double BEHIND_DISTANCE = 1.5D;   // distance behind the target (1.0 is tried if blocked)
	private static final double DAMAGE_MULT = 2.0D;       // x ATT
	private static final double BONUS_MULT = 3.0D;        // x ATT, target not looking at the Assassin
	private static final int INVISIBILITY_TICKS = 80;     // 4 s
	private static final double MANA_COST = 25.0D;        // same value as the MCreator procedure (used for the refund)

	private static final Map<UUID, Double> PREV_CD = new HashMap<>();

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		double cd = v.SKILL1_CD;
		double max = v.SKILL1_MAX_CD;
		Double prev = PREV_CD.put(player.getUUID(), cd);
		if (prev == null)
			return;

		// cast: the cooldown jumped up to its maximum
		if ("assassin".equals(v.PLAYER_CLASS) && player.isAlive() && cd - prev > 50.0D && cd >= max - 10.0D) {
			if (!strike(player, v))
				refund(v);
		}
	}

	/** @return false if the skill could not be used (nothing was changed) */
	private static boolean strike(ServerPlayer player, EldervaleModVariables.PlayerVariables v) {
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

		// was the target looking at the Assassin? (before moving)
		Vec3 look = target.getLookAngle();
		Vec3 toPlayer = player.position().subtract(target.position());
		boolean lookingAtPlayer = look.x * toPlayer.x + look.z * toPlayer.z > 0.0D;

		// spot behind the target (opposite to where it looks), only if there is room
		Vec3 flat = new Vec3(look.x, 0.0D, look.z);
		if (flat.lengthSqr() < 1.0E-4D)
			flat = new Vec3(toPlayer.x, 0.0D, toPlayer.z).scale(-1.0D);
		flat = flat.normalize();

		double tx = 0, ty = target.getY(), tz = 0;
		boolean found = false;
		for (double dist : new double[]{BEHIND_DISTANCE, 1.0D}) {
			tx = target.getX() - flat.x * dist;
			tz = target.getZ() - flat.z * dist;
			if (level.noCollision(player, player.getBoundingBox().move(tx - player.getX(), ty - player.getY(), tz - player.getZ()))) {
				found = true;
				break;
			}
		}
		if (!found)
			return false;

		// teleport and face the target
		float yaw = Mth.wrapDegrees((float) (Mth.atan2(target.getZ() - tz, target.getX() - tx) * 57.29577951308232D) - 90.0F);
		player.connection.teleport(tx, ty, tz, yaw, 0.0F);

		// invisibility and the strike (playerAttack: the source is the player, so XP and passives work)
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_TICKS, 0, false, false));
		double mult = lookingAtPlayer ? DAMAGE_MULT : BONUS_MULT;
		target.hurt(level.damageSources().playerAttack(player), (float) (v.ATTACK * mult));
		return true;
	}

	private static void refund(EldervaleModVariables.PlayerVariables v) {
		v.MANA = Math.min(v.MANA_MAX, v.MANA + MANA_COST);
		v.SKILL1_CD = 0;
		v.markSyncDirty();
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
	}
}
