package net.mcreator.eldervale.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.ChatFormatting;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.party.Party;
import net.mcreator.eldervale.party.PartyManager;
import net.mcreator.eldervale.stats.PopupPayload;
import net.mcreator.eldervale.stats.StatsConfig;
import net.mcreator.eldervale.stats.StatCalculator;
import net.mcreator.eldervale.procedures.MobScalingProcedure;

/**
 * Handles XP gain and level-up. Replaces XpLevelProcedure (deleted from MCreator).
 *
 * XP of a kill, step by step (all numbers in ProgressionConfig):
 *   1. base XP of the mob        = XP_BASE + XP_PER_MOB_LEVEL * mobLevel
 *   2. group total (Party)       = base * (1 + PARTY_BONUS_PER_EXTRA_MEMBER * (members in range - 1))
 *   3. share per member          = group total / members in range (killer alone = full XP)
 *   4. level-difference multiplier, calculated for EACH member with his own level (anti-farming)
 *   5. final XP                  = round(share * multiplier), at least 1
 */
@EventBusSubscriber
public final class ProgressionHandler {

    private ProgressionHandler() {}

    // ---------------------------------------------------------------
    //  XP curve
    // ---------------------------------------------------------------

    /** XP needed to go from level n to n+1. */
    public static long xpToNext(int level) {
        int n = Mth.clamp(level, 1, ProgressionConfig.MAX_LEVEL - 1);
        double kills = ProgressionConfig.KILLS_BASE + ProgressionConfig.KILLS_GROWTH * n * n;
        double perKill = ProgressionConfig.XP_BASE + ProgressionConfig.XP_PER_MOB_LEVEL * n;
        return Math.round(kills * perKill);
    }

    /** Raw XP value of one kill at the given mob level (before party bonus and level difference). */
    public static double xpOfKill(int mobLevel) {
        return ProgressionConfig.XP_BASE + ProgressionConfig.XP_PER_MOB_LEVEL * mobLevel;
    }

    /**
     * Anti-farming / bravery multiplier.
     * Mob up to LOWER_FREE_LEVELS below the player: 1.0, then -LOWER_PENALTY_PER_LEVEL per extra level (min MIN_XP_MULTIPLIER).
     * Mob up to HIGHER_FREE_LEVELS above the player: 1.0, then +HIGHER_BONUS_PER_LEVEL per extra level (max +MAX_XP_BONUS).
     */
    public static double levelDifferenceMultiplier(int playerLevel, int mobLevel) {
        int diff = mobLevel - playerLevel;
        if (diff < -ProgressionConfig.LOWER_FREE_LEVELS) {
            int extra = -diff - ProgressionConfig.LOWER_FREE_LEVELS;
            return Math.max(ProgressionConfig.MIN_XP_MULTIPLIER, 1.0 - ProgressionConfig.LOWER_PENALTY_PER_LEVEL * extra);
        }
        if (diff > ProgressionConfig.HIGHER_FREE_LEVELS) {
            int extra = diff - ProgressionConfig.HIGHER_FREE_LEVELS;
            return 1.0 + Math.min(ProgressionConfig.MAX_XP_BONUS, ProgressionConfig.HIGHER_BONUS_PER_LEVEL * extra);
        }
        return 1.0;
    }

    // ---------------------------------------------------------------
    //  Kill event
    // ---------------------------------------------------------------

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        if (!(victim instanceof Monster)) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        int mobLevel = victim.getPersistentData().getInt(MobScalingProcedure.TAG_LEVEL);
        if (mobLevel <= 0) mobLevel = 1;

        double popupY = victim.getY() + victim.getBbHeight() + 0.2;

        List<ServerPlayer> group = collectGroup(killer);
        // Party bonus: every extra member in range raises the group total, which is then split equally
        double groupTotal = xpOfKill(mobLevel)
                * (1.0 + ProgressionConfig.PARTY_BONUS_PER_EXTRA_MEMBER * (group.size() - 1));
        double share = groupTotal / group.size();

        for (ServerPlayer player : group) {
            int playerLevel = Mth.clamp((int) Math.round(
                    player.getData(EldervaleModVariables.PLAYER_VARIABLES).LEVEL), 1, ProgressionConfig.MAX_LEVEL);
            if (playerLevel >= ProgressionConfig.MAX_LEVEL) continue;

            // each member is judged with his own level
            long xp = Math.max(1L, Math.round(share * levelDifferenceMultiplier(playerLevel, mobLevel)));

            grantXp(player, xp, false);
            if (ProgressionConfig.XP_POPUP)
                PacketDistributor.sendToPlayer(player,
                        new PopupPayload(victim.getX(), popupY, victim.getZ(), PopupPayload.XP, (int) xp));
            if (ProgressionConfig.XP_SOUND)
                player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                        0.25F, 0.8F + player.getRandom().nextFloat() * 0.5F);
        }
    }

    private static List<ServerPlayer> collectGroup(ServerPlayer killer) {
        List<ServerPlayer> group = new ArrayList<>();
        group.add(killer);
        Party party = PartyManager.partyOf(killer.getServer(), killer.getUUID());
        if (party == null) return group;
        double rangeSqr = ProgressionConfig.PARTY_SHARE_RANGE * ProgressionConfig.PARTY_SHARE_RANGE;
        for (UUID id : party.getMembers().keySet()) {
            if (id.equals(killer.getUUID())) continue;
            ServerPlayer member = killer.getServer().getPlayerList().getPlayer(id);
            if (member != null
                    && member.level() == killer.level()
                    && member.distanceToSqr(killer) <= rangeSqr)
                group.add(member);
        }
        return group;
    }

    // ---------------------------------------------------------------
    //  XP grant and level-up
    // ---------------------------------------------------------------

    /** Adds XP, levels up as many times as needed, grants attribute points. silent = no title/fx. */
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
        v.XP_NEXT_LEVEL = level >= ProgressionConfig.MAX_LEVEL
                ? xpToNext(ProgressionConfig.MAX_LEVEL - 1)
                : xpToNext(level);
        if (level >= ProgressionConfig.MAX_LEVEL)
            v.XP = v.XP_NEXT_LEVEL;

        if (gained > 0) {
            v.STAT_POINT += (double) gained * StatsConfig.POINTS_PER_LEVEL;
            if (!silent) levelUpFeedback(player, level, gained);
        }
        v.markSyncDirty();
    }

    /** Brings an existing save to the current curve without effects (called at login/respawn). */
    public static void normalize(ServerPlayer player) {
        grantXp(player, 0L, true);
    }

    // ---------------------------------------------------------------
    //  Level-up feedback
    // ---------------------------------------------------------------

    private static void levelUpFeedback(ServerPlayer player, int level, int gained) {
        if (!ProgressionConfig.LEVEL_UP_EFFECTS) return;
        if (!(player.level() instanceof ServerLevel world)) return;

        boolean max = level >= ProgressionConfig.MAX_LEVEL;
        int points = gained * StatsConfig.POINTS_PER_LEVEL;

        player.connection.send(new ClientboundSetTitlesAnimationPacket(5, 50, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.literal("Level " + level + "   +" + points + " attribute points")
                        .withStyle(ChatFormatting.YELLOW)));
        player.connection.send(new ClientboundSetTitleTextPacket(
                Component.literal(max ? "MAX LEVEL!" : "LEVEL UP!")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.5F, max ? 0.9F : 1.3F);

        world.sendParticles(ParticleTypes.TOTEM_OF_UNDYING,
                player.getX(), player.getY() + 1.0, player.getZ(),
                max ? 80 : 40, 0.5, 0.8, 0.5, 0.4);
        world.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 0.5, player.getZ(),
                max ? 40 : 20, 0.4, 0.6, 0.4, 0.1);

        // recalculate stats after level-up (level itself does not change stats yet, but future systems may)
        StatCalculator.recalculate(player);
    }
}
