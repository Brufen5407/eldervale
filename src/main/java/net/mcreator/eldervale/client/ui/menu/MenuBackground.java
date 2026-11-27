package net.mcreator.eldervale.client.ui.menu;

import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;

import net.mcreator.eldervale.client.ui.kit.UiKit;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Layered pixel-art scene with a very gentle parallax (mouse + slow idle drift) and drifting clouds.
 * Each layer texture is 512x270; the visible area is the centre 480x270 (16px margin for the movement).
 */
public final class MenuBackground {

	private static final ResourceLocation SKY = UiKit.tex("bg_sky");
	private static final ResourceLocation CLOUDS = UiKit.tex("bg_clouds");
	private static final ResourceLocation MOUNTAINS = UiKit.tex("bg_mountains");
	private static final ResourceLocation CASTLE = UiKit.tex("bg_castle");
	private static final ResourceLocation VILLAGE = UiKit.tex("bg_village");
	private static final ResourceLocation FOREST = UiKit.tex("bg_forest");
	private static final ResourceLocation FOREGROUND = UiKit.tex("bg_foreground");
	private static final ResourceLocation VIGNETTE = UiKit.tex("bg_vignette");

	private static final int LW = 512, LH = 270, VIS_W = 480, MARGIN = 16;
	private static final float AMPLITUDE = 5.0F; // art pixels of mouse parallax on a depth-1.0 layer
	private static final float CLOUD_SPEED = 1.6F; // art pixels per second

	private static final long START = System.nanoTime();

	private float smoothX = 0.0F;
	private long last = 0L;

	public void render(GuiGraphics g, int w, int h, int mouseX) {
		long now = System.nanoTime();
		float dt = last == 0L ? 0.016F : Math.min(0.1F, (now - last) / 1.0E9F);
		last = now;
		float t = (now - START) / 1.0E9F;

		float targetX = Mth.clamp((mouseX - w / 2.0F) / (w / 2.0F), -1.0F, 1.0F);
		smoothX += (targetX - smoothX) * (1.0F - (float) Math.exp(-dt * 2.2F));
		float idle = (float) Math.sin(t * 0.15F) * 1.5F;
		float base = smoothX * AMPLITUDE + idle;

		float s = Math.max(h / (float) LH, w / (float) VIS_W);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		layer(g, SKY, w, h, s, 0.0F);
		float drift = (t * CLOUD_SPEED) % LW;
		float cx = base * 0.12F;
		layer(g, CLOUDS, w, h, s, cx - drift);
		layer(g, CLOUDS, w, h, s, cx - drift + LW);
		layer(g, MOUNTAINS, w, h, s, base * 0.30F);
		layer(g, CASTLE, w, h, s, base * 0.50F);
		layer(g, VILLAGE, w, h, s, base * 0.75F);
		layer(g, FOREST, w, h, s, base * 1.00F);
		layer(g, FOREGROUND, w, h, s, base * 1.70F);
		layer(g, VIGNETTE, w, h, s, 0.0F);
	}

	private static void layer(GuiGraphics g, ResourceLocation tex, int w, int h, float s, float xArt) {
		float x0 = (w - VIS_W * s) / 2.0F - MARGIN * s + xArt * s;
		float y0 = h - LH * s;
		g.pose().pushPose();
		g.pose().translate(x0, y0, 0.0F);
		g.pose().scale(s, s, 1.0F);
		g.blit(tex, 0, 0, 0.0F, 0.0F, LW, LH, LW, LH);
		g.pose().popPose();
	}
}
