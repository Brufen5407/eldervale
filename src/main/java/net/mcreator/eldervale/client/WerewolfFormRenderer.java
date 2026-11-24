package net.mcreator.eldervale.client.renderer;

import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;

import net.mcreator.eldervale.procedures.ShieldrenderProcedure;
import net.mcreator.eldervale.network.EldervaleModVariables;

import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Draws the werewolf beast model INSTEAD of the player while PlayerVariables.werewolf_mode is true
 * (the flag is set/cleared on the server by WerewolfBeastTransformation and synced to every client).
 * Third person and other players only: the first-person hand is a separate vanilla render.
 * While transformed, armor, held items and the nameplate are not drawn (the vanilla render is skipped).
 * Reuses ShieldrenderProcedure.renderEntity for the pose / rotation setup.
 */
@EventBusSubscriber(Dist.CLIENT)
public class WerewolfFormRenderer {

	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("eldervale", "textures/entities/werewolf_form.png");

	@SubscribeEvent
	public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
		if (WerewolfFormModelSetup.model == null)
			return;
		Player player = event.getEntity();
		if (player.isInvisible() || player.isSpectator())
			return;
		if (!player.getData(EldervaleModVariables.PLAYER_VARIABLES).werewolf_mode)
			return;

		event.setCanceled(true);
		VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
		ShieldrenderProcedure.renderEntity(event, WerewolfFormModelSetup.model, consumer);
	}
}
