package net.mcreator.eldervale.client.renderer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import net.mcreator.eldervale.entity.ManichinoRangerEntity;
import net.mcreator.eldervale.client.model.Modelranger_decoy;

public class ManichinoRangerRenderer extends MobRenderer<ManichinoRangerEntity, Modelranger_decoy<ManichinoRangerEntity>> {
	private final ResourceLocation entityTexture = ResourceLocation.parse("eldervale:textures/entities/ranger_decoy.png");

	public ManichinoRangerRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelranger_decoy<ManichinoRangerEntity>(context.bakeLayer(Modelranger_decoy.LAYER_LOCATION)), 0.5f);
	}

	@Override
	public ResourceLocation getTextureLocation(ManichinoRangerEntity entity) {
		if (entity.getTexture() != "ranger_decoy")
			return ResourceLocation.parse("eldervale:textures/entities/" + entity.getTexture() + ".png");
		return entityTexture;
	}
}