package net.mcreator.eldervale.client.hud;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.gameplay.MonkFlowState;

/**
 * Combo indicator for the Monk's Flow State, shown under the crosshair.
 * It reads the combo from the attack speed modifier that the server already syncs,
 * so it works in single player and on a server.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class MonkFlowHud {

	// ---- Settings (edit here) ----
	private static final boolean ENABLED = true;
	private static final int OFFSET_Y = 20;  // distance below the crosshair

	private static final int TEAL = 0x59F2E6;
	private static final int GOLD = 0xFFD94D;
	private static final int FRAME_DARK = 0x0B0810;
	private static final int PIP_OFF = 0x2A2430;
	private static final int BLACK = 0x000000;

	private static int displayCombo = 0;
	private static int lastCombo = 0;
	private static long popStart = 0L;
	private static float alpha = 0.0F;
	private static long lastNanos = 0L;

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (!ENABLED || player == null || mc.options.hideGui)
			return;

		long now = System.nanoTime();
		double dt = lastNanos == 0L ? 0.016 : (now - lastNanos) / 1.0E9;
		lastNanos = now;
		dt = Math.max(0.0, Math.min(dt, 0.1));

		int combo = readCombo(player);
		if (combo > lastCombo)
			popStart = now;
		lastCombo = combo;
		if (combo > 0)
			displayCombo = combo;

		float target = combo > 0 ? 1.0F : 0.0F;
		alpha += (target - alpha) * (float) (1.0 - Math.exp(-dt * 12.0));
		if (alpha < 0.03F || displayCombo <= 0)
			return;

		GuiGraphics g = event.getGuiGraphics();
		int cx = g.guiWidth() / 2;
		int cy = g.guiHeight() / 2 + OFFSET_Y;
		int a = (int) (255 * alpha);
		boolean max = displayCombo >= MonkFlowState.MAX_COMBO;
		int color = max ? GOLD : TEAL;

		// label: FLOW +10%
		int bonus = (int) Math.round((displayCombo - 1) * MonkFlowState.BONUS_PER_HIT * 100.0);
		String label = bonus > 0 ? "FLOW +" + bonus + "%" : "FLOW";
		outlinedC(g, label, cx, cy - 12, 0.75F, (a << 24) | color, a << 24 | BLACK);

		// pips
		double pop = (now - popStart) / 1.0E9;
		for (int i = 0; i < MonkFlowState.MAX_COMBO; i++) {
			int px = cx + (int) ((i - (MonkFlowState.MAX_COMBO - 1) / 2.0) * 12);
			if (i < displayCombo) {
				int r = 4;
				if (i == displayCombo - 1 && pop < 0.25)
					r += (int) Math.round(2.0 * (1.0 - pop / 0.25));
				diamond(g, px, cy, r + 1, (a << 24) | FRAME_DARK);
				diamond(g, px, cy, r, (a << 24) | color);
				diamond(g, px, cy - 1, Math.max(1, r - 3), ((a * 2 / 3) << 24) | 0xFFFFFF);
			} else {
				diamond(g, px, cy, 5, (a << 24) | FRAME_DARK);
				diamond(g, px, cy, 3, (a << 24) | PIP_OFF);
			}
		}
	}

	// The server stores the combo as an attack speed modifier: present = active, amount = (combo - 1) * 5%
	private static int readCombo(Player player) {
		AttributeInstance inst = player.getAttribute(Attributes.ATTACK_SPEED);
		if (inst == null)
			return 0;
		AttributeModifier m = inst.getModifier(MonkFlowState.MODIFIER_ID);
		if (m == null)
			return 0;
		int c = (int) Math.round(m.amount() / MonkFlowState.BONUS_PER_HIT) + 1;
		return Math.max(1, Math.min(MonkFlowState.MAX_COMBO, c));
	}

	private static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int i = -r; i <= r; i++) {
			int half = r - Math.abs(i);
			g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color);
		}
	}

	private static void outlinedC(GuiGraphics g, String text, float cx, float y, float scale, int color, int outline) {
		Font font = Minecraft.getInstance().font;
		float x = cx - font.width(text) * scale / 2.0F;
		float o = 1.0F / scale;
		g.pose().pushPose();
		g.pose().translate(x, y, 0.0F);
		g.pose().scale(scale, scale, 1.0F);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0)
					continue;
				g.pose().pushPose();
				g.pose().translate(dx * o, dy * o, 0.0F);
				g.drawString(font, text, 0, 0, outline, false);
				g.pose().popPose();
			}
		}
		g.drawString(font, text, 0, 0, color, false);
		g.pose().popPose();
	}
}
