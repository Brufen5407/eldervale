package net.mcreator.eldervale.stats;

import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;

import net.mcreator.eldervale.progression.ProgressionHandler;

/** Recalculation triggers. No tick event: login, respawn, dimension change and class-menu close only. */
@EventBusSubscriber
public class StatsEvents {
	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			refresh(player);
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			refresh(player);
	}

	@SubscribeEvent
	public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			StatsNetwork.sync(player);
	}

	/** SetCLASSProcedure closes the class menu after choosing: recalculate if the class changed. */
	@SubscribeEvent
	public static void onContainerClose(PlayerContainerEvent.Close event) {
		if (event.getEntity() instanceof ServerPlayer player)
			StatCalculator.recalculateIfClassChanged(player);
	}

	private static void refresh(ServerPlayer player) {
		ProgressionHandler.normalize(player); // brings an old save to the current XP curve
		StatCalculator.recalculate(player);
		StatsNetwork.sync(player);
	}
}
