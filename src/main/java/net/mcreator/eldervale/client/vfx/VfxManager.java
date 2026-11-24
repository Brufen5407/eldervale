package net.mcreator.eldervale.client.vfx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.client.vfx.effects.MonkMeditationVfx;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * The only place that ticks and renders the VFX of every class.
 * One tick handler, one render handler, one budget. To add an effect:
 *  1) add an id in VfxIds, 2) write a class that extends VfxEffect, 3) add it to create() below.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class VfxManager {

	// ---- Settings (edit here) ----
	public static final boolean EFFECTS_ENABLED = true;
	public static final boolean SOUNDS_ENABLED = true;
	public static final boolean SCREEN_EFFECTS = true;      // flash, vignette, FOV punch
	public static final int QUALITY = 2;                    // 0 = low, 1 = medium, 2 = high
	private static final double MAX_DISTANCE = 64.0D;       // effects farther than this are not created
	private static final int MAX_EFFECTS = 64;
	private static final int[] QUAD_BUDGET = {6000, 12000, 24000};   // max quads drawn per frame

	private static final List<VfxEffect> EFFECTS = new ArrayList<>();
	private static final Map<Long, VfxEffect> BY_KEY = new HashMap<>();

	private static long key(int vfxId, int entityId) {
		return ((long) vfxId << 32) | (entityId & 0xFFFFFFFFL);
	}

	// ---------------------------------------------------------------
	//  Events from the server
	// ---------------------------------------------------------------
	public static void handle(VfxEventPayload p) {
		if (!EFFECTS_ENABLED)
			return;
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null)
			return;

		long k = key(p.vfxId(), p.entityId());
		if (p.phase() == VfxIds.START) {
			if (mc.player.distanceToSqr(p.x(), p.y(), p.z()) > MAX_DISTANCE * MAX_DISTANCE)
				return;
			VfxEffect old = BY_KEY.remove(k);
			if (old != null) {
				EFFECTS.remove(old);
			}
			VfxEffect fx = create(p);
			if (fx == null)
				return;
			if (EFFECTS.size() >= MAX_EFFECTS) {
				VfxEffect oldest = EFFECTS.remove(0);
				BY_KEY.values().remove(oldest);
			}
			EFFECTS.add(fx);
			BY_KEY.put(k, fx);
			fx.event(p, level);
			return;
		}
		VfxEffect fx = BY_KEY.get(k);
		if (fx != null)
			fx.event(p, level);
	}

	private static VfxEffect create(VfxEventPayload p) {
		return switch (p.vfxId()) {
			case VfxIds.MONK_MEDITATION -> new MonkMeditationVfx(p);
			default -> null;
		};
	}

	// ---------------------------------------------------------------
	//  Tick
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			EFFECTS.clear();
			BY_KEY.clear();
			VfxScreen.reset();
			return;
		}
		if (mc.isPaused())
			return;

		for (int i = EFFECTS.size() - 1; i >= 0; i--) {
			VfxEffect fx = EFFECTS.get(i);
			fx.tickBase(level);
			if (fx.isDead()) {
				EFFECTS.remove(i);
				BY_KEY.values().remove(fx);
			}
		}
		VfxScreen.tick();
	}

	// ---------------------------------------------------------------
	//  Render
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
			return;
		if (!EFFECTS_ENABLED || EFFECTS.isEmpty())
			return;
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null)
			return;

		Camera camera = event.getCamera();
		PoseStack pose = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		int budget = QUAD_BUDGET[Math.max(0, Math.min(QUAD_BUDGET.length - 1, QUALITY))];

		// pass 1: glowing shapes (additive)
		VertexConsumer glowVc = buffers.getBuffer(VfxRenderTypes.GLOW);
		VfxGfx glow = new VfxGfx(glowVc, pose.last().pose(), camera.getPosition(), camera.getLeftVector(), camera.getUpVector(), QUALITY, budget);
		boolean needShade = false;
		for (int i = 0; i < EFFECTS.size(); i++) {
			VfxEffect fx = EFFECTS.get(i);
			fx.renderGlow(glow, level, partial);
			needShade |= fx.usesShade();
		}
		buffers.endBatch(VfxRenderTypes.GLOW);

		// pass 2: dark shapes (normal blending), only if someone uses it
		if (needShade) {
			VertexConsumer shadeVc = buffers.getBuffer(VfxRenderTypes.SHADE);
			VfxGfx shade = new VfxGfx(shadeVc, pose.last().pose(), camera.getPosition(), camera.getLeftVector(), camera.getUpVector(), QUALITY, budget / 2);
			for (int i = 0; i < EFFECTS.size(); i++) {
				VfxEffect fx = EFFECTS.get(i);
				if (fx.usesShade())
					fx.renderShade(shade, level, partial);
			}
			buffers.endBatch(VfxRenderTypes.SHADE);
		}
	}

	private VfxManager() {
	}
}
