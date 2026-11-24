package net.mcreator.eldervale.client.renderer;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.client.model.Modelwerewolf_form;

/**
 * Client, mod-bus events only: registers the layer of Modelwerewolf_form and bakes the model
 * (same pattern as ShieldsrenderingProcedure for the Paladin shield).
 * Kept in its own class (not inside an MCreator-generated file) so MCreator cannot overwrite it.
 */
@EventBusSubscriber(Dist.CLIENT)
public class WerewolfFormModelSetup {

	/** Baked model, null until the layers are ready. Used by WerewolfFormRenderer. */
	public static Modelwerewolf_form<Entity> model = null;

	@SubscribeEvent
	public static void registerLayer(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(Modelwerewolf_form.LAYER_LOCATION, Modelwerewolf_form::createBodyLayer);
	}

	@SubscribeEvent
	public static void bakeModel(EntityRenderersEvent.AddLayers event) {
		model = new Modelwerewolf_form<>(Minecraft.getInstance().getEntityModels().bakeLayer(Modelwerewolf_form.LAYER_LOCATION));
	}
}
