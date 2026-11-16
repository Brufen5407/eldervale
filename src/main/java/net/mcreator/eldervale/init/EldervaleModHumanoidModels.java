package net.mcreator.eldervale.init;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.client.model.Modelpaladin_shield;

import java.util.Map;
import java.util.Collections;

@EventBusSubscriber(Dist.CLIENT)
public class EldervaleModHumanoidModels {
	public static PlayerModel SHIELDS;

	@SubscribeEvent
	public static void initModels(EntityRenderersEvent.AddLayers event) {
		Modelpaladin_shield shields_temp = new Modelpaladin_shield(Minecraft.getInstance().getEntityModels().bakeLayer(Modelpaladin_shield.LAYER_LOCATION));
		SHIELDS = new PlayerModel(new ModelPart(Collections.emptyList(),
				Map.ofEntries(Map.entry("hat", new ModelPart(Collections.emptyList(), Collections.emptyMap())), Map.entry("head", new ModelPart(Collections.emptyList(), Collections.emptyMap())), Map.entry("body", shields_temp.shields),
						Map.entry("left_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap())), Map.entry("right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap())),
						Map.entry("left_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap())), Map.entry("right_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap())),
						Map.entry("left_sleeve", new ModelPart(Collections.emptyList(), Collections.emptyMap())), Map.entry("right_sleeve", new ModelPart(Collections.emptyList(), Collections.emptyMap())),
						Map.entry("left_pants", new ModelPart(Collections.emptyList(), Collections.emptyMap())), Map.entry("right_pants", new ModelPart(Collections.emptyList(), Collections.emptyMap())),
						Map.entry("jacket", new ModelPart(Collections.emptyList(), Collections.emptyMap())), Map.entry("cloak", new ModelPart(Collections.emptyList(), Collections.emptyMap())),
						Map.entry("ear", new ModelPart(Collections.emptyList(), Collections.emptyMap())))),
				false);
	}
}