package net.mcreator.eldervale.stats;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.server.level.ServerPlayer;

public final class StatsNetwork {
	private StatsNetwork() {
	}

	/** Sends the invested points to their owner only. */
	public static void sync(ServerPlayer player) {
		PacketDistributor.sendToPlayer(player, new AttributeSyncPayload(player.getData(StatsAttachments.PLAYER_ATTRIBUTES).snapshot()));
	}
}
