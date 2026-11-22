package net.mcreator.eldervale.client.stats;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

import net.mcreator.eldervale.stats.StatAttribute;
import net.mcreator.eldervale.stats.ClientStatsState;
import net.mcreator.eldervale.stats.AttributeSpendPayload;

/**
 * Client events of the Attributes window: the key that opens it (default K, rebindable)
 * and the reset of the client state when leaving a world.
 *
 * Language entry to add (src/main/resources/assets/eldervale/lang/en_us.json):
 *   "key.eldervale.attributes": "Open Attributes menu"
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class StatsClientEvents {

	public static final KeyMapping OPEN = new KeyMapping("key.eldervale.attributes", GLFW.GLFW_KEY_K, "key.categories.eldervale");

	@SubscribeEvent
	public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
		event.register(OPEN);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		while (OPEN.consumeClick()) {
			if (mc.player == null || mc.screen != null)
				continue;
			// attribute -1 = "just send me my points"
			PacketDistributor.sendToServer(new AttributeSpendPayload(-1, 0));
			mc.setScreen(new AttributesScreen());
		}
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		ClientStatsState.set(new int[StatAttribute.COUNT]);
	}
}
