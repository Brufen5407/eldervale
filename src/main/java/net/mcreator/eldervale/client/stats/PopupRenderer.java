package net.mcreator.eldervale.client.stats;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.joml.Matrix4f;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.mcreator.eldervale.stats.PopupPayload;
import net.mcreator.eldervale.stats.PopupState;

/**
 * Draws the floating texts above a target: "CRITICAL!" and "+N XP" (same world-space technique as
 * MobLabelRenderer). The text pops in, rises and fades out. Edit the constants to change the look.
 * Replaces CritPopupRenderer.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class PopupRenderer {

	private static final int OUTLINE = 0x000000;    // 0xRRGGBB
	private static final double MAX_DISTANCE = 40.0D;

	// CRITICAL!
	private static final int CRIT_COLOR = 0xFFE066;
	private static final long CRIT_LIFE = 900_000_000L;
	private static final double CRIT_RISE = 0.9D;
	private static final float CRIT_SCALE = 0.03F;

	// +N XP (smaller and a little slower)
	private static final int XP_COLOR = 0x8CE8FF;
	private static final long XP_LIFE = 1_200_000_000L;
	private static final double XP_RISE = 1.1D;
	private static final float XP_SCALE = 0.022F;

	private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.US);

	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.options.hideGui)
			return;
		long now = System.nanoTime();
		List<PopupState.Popup> popups = PopupState.active(now, CRIT_LIFE, XP_LIFE);
		if (popups.isEmpty())
			return;

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		for (PopupState.Popup popup : popups)
			draw(event, mc, popup, now);
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		RenderSystem.enableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);
	}

	private static void draw(RenderLevelStageEvent event, Minecraft mc, PopupState.Popup popup, long now) {
		Vec3 cam = event.getCamera().getPosition();
		double dx = popup.x() - cam.x();
		double dz = popup.z() - cam.z();
		if (dx * dx + dz * dz > MAX_DISTANCE * MAX_DISTANCE)
			return;

		boolean crit = popup.kind() == PopupPayload.CRIT;
		long life = crit ? CRIT_LIFE : XP_LIFE;
		double riseMax = crit ? CRIT_RISE : XP_RISE;
		float baseScale = crit ? CRIT_SCALE : XP_SCALE;
		int color = crit ? CRIT_COLOR : XP_COLOR;
		String text = crit ? "CRITICAL!" : "+" + NUMBERS.format(popup.amount()) + " XP";

		float t = (float) ((now - popup.startNanos()) / (double) life);
		t = Math.max(0.0F, Math.min(1.0F, t));
		double rise = riseMax * (1.0 - Math.pow(1.0 - t, 3.0));        // fast start, slow end
		float pop = 1.0F + 0.5F * Math.max(0.0F, 1.0F - t / 0.15F);    // quick "punch" in the first 15 %
		int alpha = t < 0.7F ? 255 : Math.max(8, (int) (255 * (1.0F - (t - 0.7F) / 0.3F)));

		Font font = mc.font;
		MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
		PoseStack pose = event.getPoseStack();
		MutableComponent component = Component.literal(text).withStyle(ChatFormatting.BOLD);

		pose.pushPose();
		pose.translate(dx, popup.y() + rise - cam.y(), dz);
		pose.mulPose(event.getCamera().rotation()); // always faces the camera
		float scale = baseScale * pop;
		pose.scale(scale, -scale, scale);
		Matrix4f m = pose.last().pose();
		font.drawInBatch8xOutline(component.getVisualOrderText(), -font.width(component) / 2.0F, -font.lineHeight / 2.0F, (alpha << 24) | color, (alpha << 24) | OUTLINE, m, buffer, LightTexture.FULL_BRIGHT);
		buffer.endBatch();
		pose.popPose();
	}

	@SubscribeEvent
	public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
		PopupState.clear();
	}
}
