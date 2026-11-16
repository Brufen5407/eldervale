/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.eldervale.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.mcreator.eldervale.client.model.Modelranger_decoy;
import net.mcreator.eldervale.client.model.Modelpaladin_shield;
import net.mcreator.eldervale.client.model.Modelmagic_arrow;

@EventBusSubscriber(Dist.CLIENT)
public class EldervaleModModels {
	@SubscribeEvent
	public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelranger_decoy.LAYER_LOCATION, Modelranger_decoy::createBodyLayer);
		event.registerLayerDefinition(Modelmagic_arrow.LAYER_LOCATION, Modelmagic_arrow::createBodyLayer);
		event.registerLayerDefinition(Modelpaladin_shield.LAYER_LOCATION, Modelpaladin_shield::createBodyLayer);
	}
}