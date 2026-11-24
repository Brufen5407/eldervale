package net.mcreator.eldervale.client.vfx;

import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

/**
 * Screen effects for the local player: color flash, colored vignette on the edges and a small FOV punch.
 * Effects call flash(), vignette() and fovPunch(); everything fades by itself.
 * vignette() must be called every tick while it should stay (it fades out when not refreshed).
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class VfxScreen {

	private static final float[] NONE = {1.0F, 1.0F, 1.0F};

	// flash
	private static float flashA, prevFlashA, flashDecay = 0.15F;
	private static float[] flashC = NONE;
	// vignette
	private static float vigA, prevVigA, vigTarget;
	private static float[] vigC = NONE;
	// fov
	private static float fov, prevFov;

	/** Full screen color flash that fades out in 'ticks'. */
	public static void flash(float[] color, float alpha, int ticks) {
		if (!VfxManager.SCREEN_EFFECTS)
			return;
		if (alpha >= flashA) {
			flashA = alpha;
			flashC = color;
			flashDecay = alpha / Math.max(1, ticks);
		}
	}

	/** Colored glow on the screen edges. Call every tick to keep it. */
	public static void vignette(float[] color, float alpha) {
		if (!VfxManager.SCREEN_EFFECTS)
			return;
		vigTarget = Math.max(vigTarget, alpha);
		vigC = color;
	}

	/** Sudden change of the field of view in degrees (negative = zoom in, positive = zoom out). It returns to normal by itself. */
	public static void fovPunch(float degrees) {
		if (!VfxManager.SCREEN_EFFECTS)
			return;
		fov = degrees;
	}

	static void reset() {
		flashA = prevFlashA = 0.0F;
		vigA = prevVigA = vigTarget = 0.0F;
		fov = prevFov = 0.0F;
	}

	static void tick() {
		prevFlashA = flashA;
		flashA = Math.max(0.0F, flashA - flashDecay);

		prevVigA = vigA;
		vigA += (vigTarget - vigA) * 0.18F;
		if (vigA < 0.002F && vigTarget <= 0.0F)
			vigA = 0.0F;
		vigTarget = 0.0F;

		prevFov = fov;
		fov *= 0.80F;
		if (Math.abs(fov) < 0.02F)
			fov = 0.0F;
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		if (!VfxManager.SCREEN_EFFECTS)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui)
			return;
		float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		float fa = VfxMath.lerp(prevFlashA, flashA, partial);
		float va = VfxMath.lerp(prevVigA, vigA, partial);
		if (fa < 0.004F && va < 0.004F)
			return;

		GuiGraphics g = event.getGuiGraphics();
		int w = g.guiWidth(), h = g.guiHeight();

		if (va >= 0.004F) {
			int steps = 12;
			int thickX = (int) (w * 0.20F), thickY = (int) (h * 0.26F);
			for (int i = 0; i < steps; i++) {
				float f = 1.0F - i / (float) steps;
				int col = argb(va * f * f, vigC);
				int x0 = thickX * i / steps, x1 = thickX * (i + 1) / steps;
				int y0 = thickY * i / steps, y1 = thickY * (i + 1) / steps;
				g.fill(x0, 0, x1, h, col);               // left
				g.fill(w - x1, 0, w - x0, h, col);       // right
				g.fill(0, y0, w, y1, col);               // top
				g.fill(0, h - y1, w, h - y0, col);       // bottom
			}
		}
		if (fa >= 0.004F)
			g.fill(0, 0, w, h, argb(fa, flashC));
	}

	@SubscribeEvent
	public static void onComputeFov(ViewportEvent.ComputeFov event) {
		if (!VfxManager.SCREEN_EFFECTS)
			return;
		float f = VfxMath.lerp(prevFov, fov, (float) event.getPartialTick());
		if (f != 0.0F)
			event.setFOV(event.getFOV() + f);
	}

	private static int argb(float a, float[] c) {
		int ia = (int) (VfxMath.clamp01(a) * 255.0F);
		int r = (int) (VfxMath.clamp01(c[0]) * 255.0F);
		int g = (int) (VfxMath.clamp01(c[1]) * 255.0F);
		int b = (int) (VfxMath.clamp01(c[2]) * 255.0F);
		return (ia << 24) | (r << 16) | (g << 8) | b;
	}

	private VfxScreen() {
	}
}
