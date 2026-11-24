package net.mcreator.eldervale.progression;

import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side copy of the mob levels (entity id -> level), filled by MobLevelPayload.
 * Contains no Minecraft client classes, so it is safe to reference from common code.
 */
public final class ClientMobLevels {
	private ClientMobLevels() {
	}

	private static final Map<Integer, Integer> LEVELS = new HashMap<>();

	public static void set(int entityId, int level) {
		LEVELS.put(entityId, level);
	}

	/** 0 = unknown (not received yet, or the mob has no level). */
	public static int get(int entityId) {
		return LEVELS.getOrDefault(entityId, 0);
	}

	@EventBusSubscriber(value = Dist.CLIENT)
	public static class Cleanup {
		@SubscribeEvent
		public static void onEntityLeave(EntityLeaveLevelEvent event) {
			if (event.getLevel().isClientSide())
				LEVELS.remove(event.getEntity().getId());
		}

		@SubscribeEvent
		public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
			LEVELS.clear();
		}
	}
}
