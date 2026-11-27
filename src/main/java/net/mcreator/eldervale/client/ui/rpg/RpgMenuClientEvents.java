package net.mcreator.eldervale.client.ui.rpg;

import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import org.lwjgl.glfw.GLFW;

/**
 * Key that opens the Eldervale RPG menu (default I, rebindable).
 *
 * Language entry to add (src/main/resources/assets/eldervale/lang/en_us.json):
 *   "key.eldervale.rpg_menu": "Open RPG menu"
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class RpgMenuClientEvents {

	public static final KeyMapping OPEN = new KeyMapping("key.eldervale.rpg_menu", GLFW.GLFW_KEY_I, "key.categories.eldervale");

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
			mc.setScreen(new RpgMenuScreen(null, RpgMenuScreen.Tab.CHARACTER));
		}
	}
}
