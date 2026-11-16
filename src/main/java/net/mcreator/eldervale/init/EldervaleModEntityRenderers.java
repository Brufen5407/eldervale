/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.eldervale.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.eldervale.client.renderer.ManichinoRangerRenderer;
import net.mcreator.eldervale.client.renderer.MagicArrowRenderer;

@EventBusSubscriber(Dist.CLIENT)
public class EldervaleModEntityRenderers {
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(EldervaleModEntities.MANICHINO_RANGER.get(), ManichinoRangerRenderer::new);
		event.registerEntityRenderer(EldervaleModEntities.MAGIC_ARROW.get(), MagicArrowRenderer::new);
	}
}