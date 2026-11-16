package net.mcreator.eldervale.procedures;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.client.model.Modelpaladin_shield;

import javax.annotation.Nullable;

@EventBusSubscriber(Dist.CLIENT)
public class ShieldsrenderingProcedure {
	@SubscribeEvent
	public static void afterModelRegistration(EntityRenderersEvent.AddLayers event) {
		execute(event);
	}

	public static void execute() {
		execute(null);
	}

	private static void execute(@Nullable Event event) {
		EldervaleModVariables.paladin_shield_texture = ResourceLocation.fromNamespaceAndPath("eldervale", "textures/entities/paladin_shield.png");
		EldervaleModVariables.paladin_shield = new Modelpaladin_shield(Minecraft.getInstance().getEntityModels().bakeLayer(Modelpaladin_shield.LAYER_LOCATION));
	}
}