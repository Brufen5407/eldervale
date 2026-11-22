package net.mcreator.eldervale.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.ChatFormatting;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.party.Party;
import net.mcreator.eldervale.party.PartyManager;
import net.mcreator.eldervale.stats.PopupPayload;
import net.mcreator.eldervale.stats.StatsConfig;

/**
 * XP and levels (server only). Replaces XpLevelProcedure.
 *  - XP goes to the player who kills a monster (arrows and skills included) and is shared with the
 *    party members nearby.
 *  - grantXp() is the single place that adds XP and levels up (as many levels as the XP allows).
 *  - Every level gives StatsConfig.POINTS_PER_LEVEL attribute points (PlayerVariables.STAT_POINT).
 *  - The XP needed for the next level is always derived from the level, never from a saved value.
 */
@EventBusSubscriber
public final class ProgressionHandler {
	private ProgressionHandler() {
	}

	// ---------------------------------------------------------------
	//  Curve
	// ---------------------------------------------------------------

	/** XP needed to go from this level to the next one. */
	public static long xpToNext(int level) {
		int n = Mth.clamp(level, 1, ProgressionConfig.MAX_LEVEL - 1);
		double kills = ProgressionConfig.KILLS_BASE + ProgressionConfig.KILLS_GROWTH * n * n;
		double perKill = ProgressionConfig.XP_BASE + ProgressionConfig.XP_PER_MOB_LEVEL * n;
		return Math.round(kills * perKill);
	}

	/** XP of one kill of a mob of this level, before multipliers. */
	public static double xpOfMob(double mobLevel) {
		return ProgressionConfig.XP_BASE + ProgressionConfig.XP_PER_MOB_LEVEL * mobLevel;
	}

	/** Fighting far weaker mobs gives less XP, fighting stronger ones gives a bonus. */
	public static double levelMultiplier(int playerLevel, double mobLevel) {
		double diff = playerLevel - mobLevel;
		if (diff > ProgressionConfig.LOWER_FREE_LEVELS)
			return Math.max(ProgressionConfig.MIN_XP_MULTIPLIER, 1.0 - ProgressionConfig.LOWER_PENALTY_PER_LEVEL * (diff - ProgressionConfig.LOWER_FREE_LEVELS));
		if (-diff > ProgressionConfig.HIGHER_FREE_LEVELS)
			return Math.min(1.0 + ProgressionConfig.MAX_XP_BONUS, 1.0 + ProgressionConfig.HIGHER_BONUS_PER_LEVEL * (-diff - ProgressionConfig.HIGHER_FREE_LEVELS));
		return 1.0;
	}

	// ---------------------------------------------------------------
	//  Kills
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onMobDeath(LivingDeathEvent event) {
		LivingEntity victim = event.getEntity();
		if (!(victim instanceof Monster) || victim.level().isClientSide())
			return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer killer))
			return;

		double mobLevel = victim.getPersistentData().getDouble(MobScaling.TAG_LEVEL);
		if (mobLevel <= 0.0)
			mobLevel = MobScaling.bossLevel(victim.getMaxHealth());
		double baseXp = xpOfMob(mobLevel);

		List<ServerPlayer> group = collectGroup(killer);
		int n = group.size();
		double share = (1.0 + ProgressionConfig.PARTY_BONUS_PER_EXTRA_MEMBER * (n - 1)) / n;
		double popupY = victim.getY() + victim.getBbHeight() + 0.2;

		for (ServerPlayer player : group) {
			int level = Mth.clamp((int) Math.round(player.getData(EldervaleModVariables.PLAYER_VARIABLES).LEVEL), 1, ProgressionConfig.MAX_LEVEL);
			long xp = Math.max(1L, Math.round(baseXp * share * levelMultiplier(level, mobLevel)));
			grantXp(player, xp, false);
			if (level < ProgressionConfig.MAX_LEVEL)
				xpFeedback(player, victim.getX(), popupY, victim.getZ(), xp);
		}
	}

	/** The killer plus the party members in the same dimension and range (everyone is a separate player). */
	private static List<ServerPlayer> collectGroup(ServerPlayer killer) {
		List<ServerPlayer> group = new ArrayList<>();
		group.add(killer);
		Party party = PartyManager.partyOf(killer.getServer(), killer.getUUID());
		if (party == null)
			return group;
		double rangeSqr = ProgressionConfig.PARTY_SHARE_RANGE * ProgressionConfig.PARTY_SHARE_RANGE;
		for (UUID id : party.getMembers().keySet()) {
			if (id.equals(killer.getUUID()))
				continue;
			ServerPlayer member = killer.getServer().getPlayerList().getPlayer(id);
			if (member != null && member.level() == killer.level() && member.distanceToSqr(killer) <= rangeSqr)
				group.add(member);
		}
		return group;
	}

	// ---------------------------------------------------------------
	//  XP and level up
	// ---------------------------------------------------------------

	/** Adds XP and levels up as many times as needed. silent = no level-up effects (used at login). */
	public static void grantXp(ServerPlayer player, long amount, boolean silent) {
		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		int level = Mth.clamp((int) Math.round(v.LEVEL), 1, ProgressionConfig.MAX_LEVEL);
		int gained = 0;

		if (level < ProgressionConfig.MAX_LEVEL) {
			v.XP += Math.max(0L, amount);
			while (level < ProgressionConfig.MAX_LEVEL && v.XP >= xpToNext(level)) {
				v.XP -= xpToNext(level);
				level++;
				gained++;
			}
		}

		v.LEVEL = level;
		if (level >= ProgressionConfig.MAX_LEVEL) {
			v.XP_NEXT_LEVEL = xpToNext(ProgressionConfig.MAX_LEVEL - 1);
			v.XP = v.XP_NEXT_LEVEL; // full bar at the maximum level
		} else {
			v.XP_NEXT_LEVEL = xpToNext(level);
		}
		if (gained > 0) {
			v.STAT_POINT += (double) gained * StatsConfig.POINTS_PER_LEVEL;
			if (!silent)
				levelUpFeedback(player, level, gained);
		}
		v.markSyncDirty();
	}

	/** Brings an existing save to the current curve (login / respawn). Silent: no level-up effects. */
	public static void normalize(ServerPlayer player) {
		grantXp(player, 0L, true);
	}

	// ---------------------------------------------------------------
	//  Feedback (visual and sound)
	// ---------------------------------------------------------------
	private static void xpFeedback(ServerPlayer player, double x, double y, double z, long xp) {
		if (ProgressionConfig.XP_POPUP)
			PacketDistributor.sendToPlayer(player, new PopupPayload(x, y, z, PopupPayload.XP, (int) Math.min(xp, Integer.MAX_VALUE)));
		if (ProgressionConfig.XP_SOUND)
			player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 0.8F + player.getRandom().nextFloat() * 0.5F);
	}

	private static void levelUpFeedback(ServerPlayer player, int level, int gained) {
		if (!ProgressionConfig.LEVEL_UP_EFFECTS || !(player.level() instanceof ServerLevel world))
			return;
		boolean max = level >= ProgressionConfig.MAX_LEVEL;
		int points = gained * StatsConfig.POINTS_PER_LEVEL;

		// title: "LEVEL UP!" + subtitle (vanilla title channel, no extra assets)
		player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 20));
		player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("Level " + level + "   +" + points + " attribute points").withStyle(ChatFormatting.YELLOW)));
		player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(max ? "MAX LEVEL!" : "LEVEL UP!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));

		// sounds heard by everyone close by
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
		world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5F, max ? 0.9F : 1.3F);

		// particles around the player
		world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), max ? 80 : 40, 0.5, 0.8, 0.5, 0.4);
		world.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.5, player.getZ(), max ? 40 : 20, 0.4, 0.6, 0.4, 0.1);
	}
}
