/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.eldervale.init;

import org.lwjgl.glfw.GLFW;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

import net.mcreator.eldervale.network.Skill2Message;
import net.mcreator.eldervale.network.Skill1Message;

@EventBusSubscriber(Dist.CLIENT)
public class EldervaleModKeyMappings {
	public static final KeyMapping SKILL_1 = new KeyMapping("key.eldervale.skill_1", GLFW.GLFW_KEY_V, "key.category.eldervale.gameplay") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new Skill1Message(0, 0));
				Skill1Message.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};
	public static final KeyMapping SKILL_2 = new KeyMapping("key.eldervale.skill_2", GLFW.GLFW_KEY_B, "key.category.eldervale.gameplay") {
		private boolean isDownOld = false;

		@Override
		public void setDown(boolean isDown) {
			super.setDown(isDown);
			if (isDownOld != isDown && isDown) {
				PacketDistributor.sendToServer(new Skill2Message(0, 0));
				Skill2Message.pressAction(Minecraft.getInstance().player, 0, 0);
			}
			isDownOld = isDown;
		}
	};

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(SKILL_1);
		event.register(SKILL_2);
	}

	@EventBusSubscriber(Dist.CLIENT)
	public static class KeyEventListener {
		@SubscribeEvent
		public static void onClientTick(ClientTickEvent.Post event) {
			if (Minecraft.getInstance().screen == null) {
				SKILL_1.consumeClick();
				SKILL_2.consumeClick();
			}
		}
	}
}