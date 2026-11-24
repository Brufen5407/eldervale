package net.mcreator.eldervale.client.world;

import org.joml.Matrix4f;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.mcreator.eldervale.progression.ClientMobLevels;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class MobLabelRenderer {

	// ---- Limiti per evitare lag (modifica qui) ----
	private static final double LABEL_DISTANCE = 20.0D;          // livello + nome entro questi blocchi
	private static final int MAX_LABELS = 12;                    // massimo di mob con scritta (i piu' vicini)
	private static final boolean REQUIRE_LINE_OF_SIGHT = false;  // true = solo mob che vedi davvero

	// Colori testo 0xRRGGBB
	private static final int LEVEL_COLOR = 0xFFA31A;
	private static final int NAME_COLOR = 0xC48CFF;
	private static final int OUTLINE_COLOR = 0xFF000000;

	// Dimensione del testo
	private static final float SCALE = 0.025F;

	// Altezza sopra la testa del mob, in blocchi
	private static final double HEIGHT_OFFSET = 0.7D;

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
			return;
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.options.hideGui)
			return;
		Entity cam = event.getCamera().getEntity();

		// 1) raccolgo solo i mob abbastanza vicini che hanno un livello (ricevuto dal server)
		double maxSqr = LABEL_DISTANCE * LABEL_DISTANCE;
		List<LivingEntity> near = new ArrayList<>();
		for (Entity e : level.entitiesForRendering()) {
			if (!(e instanceof Monster) || !(e instanceof LivingEntity living) || !e.isAlive())
				continue;
			if (ClientMobLevels.get(e.getId()) <= 0)
				continue;
			if (e.distanceToSqr(cam) > maxSqr)
				continue;
			if (REQUIRE_LINE_OF_SIGHT && cam instanceof LivingEntity viewer && !viewer.hasLineOfSight(e))
				continue;
			near.add(living);
		}
		if (near.isEmpty())
			return;

		// 2) tengo solo i piu' vicini
		if (near.size() > MAX_LABELS) {
			near.sort(Comparator.comparingDouble(e -> e.distanceToSqr(cam)));
			near = near.subList(0, MAX_LABELS);
		}

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();

		for (LivingEntity mob : near)
			drawMobLabel(event, mob);

		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
	}

	private static void drawMobLabel(RenderLevelStageEvent event, LivingEntity mob) {
		Minecraft mc = Minecraft.getInstance();
		Font font = mc.font;
		MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
		Vec3 cam = event.getCamera().getPosition();
		PoseStack pose = event.getPoseStack();

		int lvl = ClientMobLevels.get(mob.getId());

		MutableComponent title = Component.literal(lvl + " ").withStyle(Style.EMPTY.withColor(LEVEL_COLOR))
				.append(mob.getName().copy().withStyle(Style.EMPTY.withColor(NAME_COLOR)));

		pose.pushPose();
		pose.translate(mob.getX() - cam.x(), mob.getY() + mob.getBbHeight() + HEIGHT_OFFSET - cam.y(), mob.getZ() - cam.z());
		pose.mulPose(event.getCamera().rotation()); // il testo guarda sempre la camera
		pose.scale(SCALE, -SCALE, SCALE);

		Matrix4f m = pose.last().pose();

		// livello + nome, centrati
		float titleX = -font.width(title) / 2.0F;
		font.drawInBatch8xOutline(title.getVisualOrderText(), titleX, -font.lineHeight / 2.0F, 0xFFFFFFFF, OUTLINE_COLOR, m, buffer, LightTexture.FULL_BRIGHT);

		buffer.endBatch();
		pose.popPose();
	}
}
