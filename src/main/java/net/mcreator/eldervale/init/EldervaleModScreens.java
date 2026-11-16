/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.eldervale.init;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.eldervale.client.gui.ClassselectionScreen;

@EventBusSubscriber(Dist.CLIENT)
public class EldervaleModScreens {
	@SubscribeEvent
	public static void clientLoad(RegisterMenuScreensEvent event) {
		event.register(EldervaleModMenus.CLASSSELECTION.get(), ClassselectionScreen::new);
	}

	public interface ScreenAccessor {
		void updateMenuState(int elementType, String name, Object elementState);
	}
}