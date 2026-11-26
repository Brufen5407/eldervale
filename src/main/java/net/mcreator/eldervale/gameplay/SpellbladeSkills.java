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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.vfx.VfxIds;
import net.mcreator.eldervale.vfx.VfxServer;

/**
 * Spellblade skills: ARCANE SLASH (skill 1) and PHANTOM EDGE (skill 2).
 *
 * Mana and cooldown stay in the MCreator procedures SpellbladeSkill1 (25 mana, 160 ticks)
 * and SpellbladeSkill2 (40 mana, 320 ticks). This class detects the cast on the server
 * (the SKILL1_CD / SKILL2_CD value jumps up to its maximum), the same pattern used by
 * AssassinShadowStrike and MonkPunchBarrage.
 *
 * ARCANE SLASH : magic wave in front of the player, hits every monster along its path.
 * PHANTOM EDGE : magic blade on the nearest monster within RANGE, plus a short slowness.
 *                No target in range: mana and cooldown are refunded.
 *
 * Damage uses playerAttack as source so that XP on kill works (XpLevelProcedure checks it);
 * the flag SpellbladeArcaneConversion.skillDamage stops the passive from triggering on skill hits.
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 * Gameplay unchanged: the old particles and vanilla sounds are replaced by VFX events
 * (START = the cast, IMPACT = every monster that is hit).
 */
@EventBusSubscriber
public class SpellbladeSkills {

	// ---- Settings (edit here) ----
	// Arcane Slash
	private static final double SLASH_LENGTH = 8.0D;        // blocks in front of the player
	private static final double SLASH_HALF_WIDTH = 1.5D;    // blocks on each side
	private static final double SLASH_DAMAGE_MULT = 2.0D;   // x ATT
	// Phantom Edge
	private static final double EDGE_RANGE = 8.0D;
	private static final double EDGE_DAMAGE_MULT = 3.5D;    // x ATT
	private static final int EDGE_SLOW_TICKS = 60;          // 3 s
	private static final int EDGE_SLOW_LEVEL = 1;           // 1 = Slowness II
	private static final double EDGE_MANA_COST = 40.0D;     // same value as the MCreator procedure (used for the refund)

	/** previous {SKILL1_CD, SKILL2_CD} per player, updated in place (no allocation per tick) */
	private static final Map<UUID, double[]> PREV_CD = new HashMap<>();

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

		if (!"spellblade".equals(v.PLAYER_CLASS) || !player.isAlive())
			return;

		// cast: the cooldown jumped up to its maximum
		if (cd1 - p1 > 50.0D && cd1 >= v.SKILL1_MAX_CD - 10.0D)
			arcaneSlash(player, v);
		if (cd2 - p2 > 50.0D && cd2 >= v.SKILL2_MAX_CD - 10.0D) {
			if (!phantomEdge(player, v))
				refundSkill2(v);
		}
	}

	// ---------------------------------------------------------------
	//  SKILL 1 - ARCANE SLASH
	// ---------------------------------------------------------------
	private static void arcaneSlash(ServerPlayer player, EldervaleModVariables.PlayerVariables v) {
		ServerLevel level = player.serverLevel();

		// horizontal direction of the player's look
		Vec3 look = player.getLookAngle();
		Vec3 dir = new Vec3(look.x, 0.0D, look.z);
		dir = dir.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : dir.normalize();

		float damage = (float) (v.ATTACK * SLASH_DAMAGE_MULT);
		double px = player.getX();
		double pz = player.getZ();

		// VFX: the wave (param = length of the path)
		VfxServer.sendAt(player, VfxIds.SPELLBLADE_SLASH, VfxIds.START, 0, px, player.getY(), pz, (float) SLASH_LENGTH, dir.x, 0.0D, dir.z);

		for (Monster m : level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(SLASH_LENGTH, 2.5D, SLASH_LENGTH), e -> e.isAlive())) {
			double rx = m.getX() - px;
			double rz = m.getZ() - pz;
			double forward = rx * dir.x + rz * dir.z;
			if (forward < 0.0D || forward > SLASH_LENGTH)
				continue;
			double lateral = Math.abs(rx * dir.z - rz * dir.x);
			if (lateral > SLASH_HALF_WIDTH + m.getBbWidth() * 0.5D)
				continue;
			double dy = m.getY() - player.getY();
			if (dy < -2.0D || dy > 2.5D)
				continue;
			hit(player, m, damage);
			// the client plays the hit when the wave reaches it (arg = distance along the path in tenths of a block)
			VfxServer.sendAt(player, VfxIds.SPELLBLADE_SLASH, VfxIds.IMPACT, (int) (forward * 10.0D), m.getX(), m.getY() + m.getBbHeight() * 0.5D, m.getZ(), 0.0F);
		}
	}

	// ---------------------------------------------------------------
	//  SKILL 2 - PHANTOM EDGE
	// ---------------------------------------------------------------
	/** @return false if there is no target in range (nothing was changed) */
	private static boolean phantomEdge(ServerPlayer player, EldervaleModVariables.PlayerVariables v) {
		ServerLevel level = player.serverLevel();

		// nearest monster within EDGE_RANGE (real distance, not a box)
		Monster target = null;
		double best = EDGE_RANGE * EDGE_RANGE;
		for (Monster m : level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(EDGE_RANGE), e -> e.isAlive())) {
			double d = m.distanceToSqr(player);
			if (d <= best) {
				best = d;
				target = m;
			}
		}
		if (target == null)
			return false;

		hit(player, target, (float) (v.ATTACK * EDGE_DAMAGE_MULT));
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EDGE_SLOW_TICKS, EDGE_SLOW_LEVEL, false, true));

		// VFX: START = the cast, IMPACT = the blade lands on the target (arg = slow duration)
		VfxServer.send(player, VfxIds.SPELLBLADE_PHANTOM, VfxIds.START, 0, (float) EDGE_RANGE);
		VfxServer.sendAt(player, VfxIds.SPELLBLADE_PHANTOM, VfxIds.IMPACT, EDGE_SLOW_TICKS, target.getX(), target.getY(), target.getZ(), target.getBbHeight());
		return true;
	}

	private static void refundSkill2(EldervaleModVariables.PlayerVariables v) {
		v.MANA = Math.min(v.MANA_MAX, v.MANA + EDGE_MANA_COST);
		v.SKILL2_CD = 0;
		v.markSyncDirty();
	}

	// ---------------------------------------------------------------
	//  Shared
	// ---------------------------------------------------------------
	/** playerAttack source (XP and kill credit work); the flag keeps Arcane Conversion from firing on skill hits */
	private static void hit(ServerPlayer player, LivingEntity target, float damage) {
		SpellbladeArcaneConversion.skillDamage = true;
		try {
			target.hurt(player.level().damageSources().playerAttack(player), damage);
		} finally {
			SpellbladeArcaneConversion.skillDamage = false;
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
	}
}
