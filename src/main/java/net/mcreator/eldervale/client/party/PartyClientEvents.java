package net.mcreator.eldervale.client.party;

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

import net.mcreator.eldervale.party.PartyClientState;
import net.mcreator.eldervale.party.PartyActionPayload;

/**
 * Client events of the Party system:
 *  - the key that opens the Party window (default O, rebindable in the controls menu);
 *  - reset of the client state when leaving a world.
 *
 * Language entries to add (src/main/resources/assets/eldervale/lang/en_us.json):
 *   "key.eldervale.party": "Open Party menu",
 *   "key.categories.eldervale": "Eldervale"   (only if the category is not there yet)
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class PartyClientEvents {

	public static final KeyMapping OPEN = new KeyMapping("key.eldervale.party", GLFW.GLFW_KEY_O, "key.categories.eldervale");

	public static String openKeyName() {
		return OPEN.getTranslatedKeyMessage().getString();
	}

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
			PacketDistributor.sendToServer(new PartyActionPayload(PartyActionPayload.SYNC, ""));
			mc.setScreen(new PartyScreen());
		}
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		PartyClientState.reset();
	}
}
