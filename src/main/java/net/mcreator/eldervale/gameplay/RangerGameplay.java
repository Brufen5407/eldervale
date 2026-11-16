package net.mcreator.eldervale.gameplay;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Server-safe Ranger passive (Crazy Reflex).
 * Replaces RangerPassiveProcedure and RangerPassiveCDProcedure.
 * Runs only on the server and only for players.
 */
@EventBusSubscriber
public class RangerGameplay {

	// ---- Settings (edit here) ----
	private static final int REFLEX_SPEED_TICKS = 60;      // 3 seconds of speed
	private static final int REFLEX_SPEED_LEVEL = 0;       // 0 = Speed I, 1 = Speed II
	private static final int REFLEX_COOLDOWN_TICKS = 160;  // 8 seconds

	private static final String READY_KEY = "eldervale_ranger_reflex_ready";

	// Crazy Reflex: when the ranger takes damage, gain speed (then an 8s cooldown)
	@SubscribeEvent
	public static void onDamage(LivingDamageEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		if (!"ranger".equals(v.PLAYER_CLASS) || v.ranger_hitcooldown)
			return;

		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, REFLEX_SPEED_TICKS, REFLEX_SPEED_LEVEL, false, false));
		v.ranger_hitcooldown = true;
		v.markSyncDirty();
		player.getPersistentData().putLong(READY_KEY, player.level().getGameTime() + REFLEX_COOLDOWN_TICKS);
	}

	// Cooldown: the flag goes back to false when the 8 seconds have passed
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide())
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		if (!v.ranger_hitcooldown)
			return;

		boolean ranger = "ranger".equals(v.PLAYER_CLASS);
		CompoundTag data = player.getPersistentData();
		if (!ranger || player.level().getGameTime() >= data.getLong(READY_KEY)) {
			v.ranger_hitcooldown = false;
			v.markSyncDirty();
		}
	}
}
