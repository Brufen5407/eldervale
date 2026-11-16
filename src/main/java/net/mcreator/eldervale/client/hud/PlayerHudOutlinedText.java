package net.mcreator.eldervale.client.hud;

import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.procedures.LVLLabelProcedure;
import net.mcreator.eldervale.network.EldervaleModVariables;

@EventBusSubscriber(value = Dist.CLIENT)
public class PlayerHudOutlinedText {

	// Nasconde cuori, fame e XP di Minecraft (false = li lascia visibili)
	private static final boolean HIDE_VANILLA = true;

	// ---- Dimensioni (modifica qui) ----
	private static final int BAR_W = 66;       // larghezza barre HP e mana
	private static final int BAR_H = 11;       // altezza barre HP e mana
	private static final int BADGE_SIZE = 20;  // grandezza badge livello
	private static final int XP_MAX_W = 110;   // larghezza massima barra XP
	private static final int XP_H = 8;         // altezza barra XP

	// ---- Colori 0xAARRGGBB (AA deve essere FF) ----
	private static final int FRAME_DARK = 0xFF0B0810;
	private static final int GOLD_LIGHT = 0xFFF2D27A;
	private static final int GOLD = 0xFFC9A24A;
	private static final int GOLD_DARK = 0xFF7A5A1E;
	private static final int BG_TOP = 0xFF2A2430;
	private static final int BG_BOTTOM = 0xFF141018;

	private static final int HP_TOP = 0xFFFF6B6B, HP_BOTTOM = 0xFFB3141F, HP_TRAIL = 0xFFFFD0D0, HP_GEM = 0xFFFF3B3B;
	private static final int MANA_TOP = 0xFF6EC1FF, MANA_BOTTOM = 0xFF1B55CC, MANA_TRAIL = 0xFFC6E4FF, MANA_GEM = 0xFF3BA0FF;
	private static final int XP_TOP = 0xFFFFE17A, XP_BOTTOM = 0xFFD9961A, XP_GEM = 0xFFFFC83B;

	private static final int TEXT_WHITE = 0xFFFFFFFF;
	private static final int TEXT_ORANGE = 0xFFFFA31A;
	private static final int OUTLINE_BLACK = 0xFF000000;
	private static final int OUTLINE_BROWN = 0xFF1A0F00;

	// Spessore del contorno del testo, in unità della GUI
	private static final float OUTLINE = 1.0F;

	// ---- Animazione ----
	private static class Smooth {
		double main, trail;
		boolean init;

		void update(double target, double dt) {
			if (!init) {
				main = trail = target;
				init = true;
				return;
			}
			main += (target - main) * (1.0 - Math.exp(-dt * 18.0));
			if (trail < main)
				trail = main;
			else
				trail += (main - trail) * (1.0 - Math.exp(-dt * 2.5));
		}
	}

	private static final Smooth HP = new Smooth();
	private static final Smooth MANA = new Smooth();
	private static final Smooth XP = new Smooth();
	private static long lastNanos = 0L;

	@SubscribeEvent
	public static void onLayer(RenderGuiLayerEvent.Pre event) {
		if (!HIDE_VANILLA)
			return;
		ResourceLocation n = event.getName();
		if (n.equals(VanillaGuiLayers.PLAYER_HEALTH) || n.equals(VanillaGuiLayers.FOOD_LEVEL) || n.equals(VanillaGuiLayers.EXPERIENCE_BAR) || n.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.options.hideGui)
			return;

		long now = System.nanoTime();
		double dt = lastNanos == 0L ? 0.016 : (now - lastNanos) / 1.0E9;
		lastNanos = now;
		dt = Math.max(0.0, Math.min(dt, 0.1));

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		double hp = v.HEALT, maxHp = v.HEALT_MAX;
		double mana = v.MANA, maxMana = v.MANA_MAX;
		double xp = v.XP, maxXp = v.XP_NEXT_LEVEL;

		HP.update(ratio(hp, maxHp), dt);
		MANA.update(ratio(mana, maxMana), dt);
		XP.update(ratio(xp, maxXp), dt);

		GuiGraphics g = event.getGuiGraphics();
		int w = g.guiWidth();
		int h = g.guiHeight();
		int cx = w / 2;

		// La hotbar di Minecraft va da cx-91 a cx+91.

		// ---- HP (sinistra) e MANA (destra), sopra la hotbar ----
		int barY = h - 37;
		int hpX = cx - 82;
		int manaX = cx + 16;

		drawBar(g, hpX, barY, BAR_W, BAR_H, HP.main, HP.trail, HP_TOP, HP_BOTTOM, HP_TRAIL, HP_GEM, num(hp, true) + "/" + num(maxHp, true), HP.main < 0.25 && maxHp > 0, true, true);
		drawBar(g, manaX, barY, BAR_W, BAR_H, MANA.main, MANA.trail, MANA_TOP, MANA_BOTTOM, MANA_TRAIL, MANA_GEM, num(mana, true) + "/" + num(maxMana, true), false, true, true);

		// ---- Badge livello + XP, a destra della hotbar ----
		int bs = BADGE_SIZE;
		int bx = cx + 97;
		int by = h - 6 - bs;

		int xpX = bx + bs + 1;
		int xpW = Math.max(30, Math.min(XP_MAX_W, w - 10 - xpX));
		int xpY = by + bs / 2 - XP_H / 2;

		drawBar(g, xpX, xpY, xpW, XP_H, XP.main, XP.main, XP_TOP, XP_BOTTOM, 0, XP_GEM, num(xp, false) + " / " + num(maxXp, false), false, false, true);

		chamfer(g, bx - 2, by - 2, bs + 4, bs + 4, 5, FRAME_DARK);
		chamfer(g, bx - 1, by - 1, bs + 2, bs + 2, 4, GOLD);
		chamfer(g, bx, by, bs, bs, 3, FRAME_DARK);
		chamfer(g, bx + 2, by + 2, bs - 4, bs - 4, 2, 0xFF2A1E12);

		drawCentered(g, "LV", bx + bs / 2.0F, by + 3, GOLD, OUTLINE_BLACK, 0.45F);

		Font font = mc.font;
		String lvl = java.util.Objects.toString(LVLLabelProcedure.execute(player), "").trim();
		float lvlScale = Math.min(1.3F, (bs - 6.0F) / Math.max(1, font.width(lvl)));
		float areaTop = by + 7;
		float areaH = bs - 9;
		float lvlY = areaTop + (areaH - 7 * lvlScale) / 2.0F;
		drawCentered(g, lvl, bx + bs / 2.0F, lvlY, TEXT_ORANGE, OUTLINE_BROWN, lvlScale);
	}

	// ---- Utilità ----

	private static double ratio(double value, double max) {
		if (max <= 0 || Double.isNaN(value) || Double.isNaN(max))
			return 0.0;
		return Math.max(0.0, Math.min(1.0, value / max));
	}

	private static String num(double v, boolean up) {
		if (Double.isNaN(v))
			return "0";
		return String.valueOf((long) (up ? Math.ceil(v) : Math.floor(v)));
	}

	private static float pulse() {
		return (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 1000.0 * 6.0));
	}

	private static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int i = -r; i <= r; i++) {
			int half = r - Math.abs(i);
			g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color);
		}
	}

	// rettangolo con gli angoli smussati
	private static void chamfer(GuiGraphics g, int x, int y, int w, int h, int c, int color) {
		for (int i = 0; i < c; i++) {
			g.fill(x + c - i, y + i, x + w - c + i, y + i + 1, color);
			g.fill(x + c - i, y + h - 1 - i, x + w - c + i, y + h - i, color);
		}
		g.fill(x, y + c, x + w, y + h - c, color);
	}

	private static void drawBar(GuiGraphics g, int x, int y, int w, int h, double ratio, double trail, int top, int bottom, int trailColor, int gem, String label, boolean lowAlert, boolean capLeft, boolean capRight) {
		// bagliore rosso quando la vita è bassa
		if (lowAlert) {
			int a = (int) (50 + 130 * pulse());
			g.fill(x - 4, y - 4, x + w + 4, y + h + 4, (a << 24) | 0xFF2020);
		}

		// cornice
		g.fill(x - 2, y - 2, x + w + 2, y + h + 2, FRAME_DARK);
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, GOLD);
		g.fill(x - 1, y - 1, x + w + 1, y, GOLD_LIGHT);
		g.fill(x - 1, y + h, x + w + 1, y + h + 1, GOLD_DARK);

		// sfondo
		g.fillGradient(x, y, x + w, y + h, BG_TOP, BG_BOTTOM);

		// riempimento + scia
		int fw = (int) Math.round(ratio * w);
		int tw = (int) Math.round(trail * w);
		if (tw > fw)
			g.fill(x + fw, y, x + tw, y + h, trailColor);
		if (fw > 0) {
			g.fillGradient(x, y, x + fw, y + h, top, bottom);
			g.fill(x, y, x + fw, y + Math.max(1, h / 2), 0x26FFFFFF); // lucido
			g.fill(x, y, x + fw, y + 1, 0x66FFFFFF);
			if (fw > 1 && ratio < 0.999)
				g.fill(x + fw - 1, y, x + fw, y + h, 0x77FFFFFF); // bordo luminoso sul fronte
		}

		// divisioni al 10%
		for (int i = 1; i < 10; i++) {
			int tx = x + w * i / 10;
			g.fill(tx, y, tx + 1, y + h, 0x30000000);
		}

		// terminali a rombo con gemma
		int cy = y + h / 2;
		if (capLeft)
			cap(g, x - 2, cy, gem);
		if (capRight)
			cap(g, x + w + 1, cy, gem);

		// numeri
		float s = h >= 11 ? 0.75F : 0.65F;
		drawCentered(g, label, x + w / 2.0F, y + (h - 7 * s) / 2.0F, TEXT_WHITE, OUTLINE_BLACK, s);
	}

	private static void cap(GuiGraphics g, int cx, int cy, int gem) {
		diamond(g, cx, cy, 5, FRAME_DARK);
		diamond(g, cx, cy, 4, GOLD);
		diamond(g, cx, cy, 3, FRAME_DARK);
		diamond(g, cx, cy, 2, gem);
		g.fill(cx - 1, cy - 1, cx, cy, 0xAAFFFFFF); // riflesso
	}

	private static void drawCentered(GuiGraphics g, String text, float cx, float y, int color, int outlineColor, float scale) {
		if (text == null || text.isEmpty())
			return;
		Font font = Minecraft.getInstance().font;
		float width = font.width(text) * scale;
		float o = OUTLINE / scale;

		g.pose().pushPose();
		g.pose().translate(cx - width / 2.0F, y, 0.0F);
		g.pose().scale(scale, scale, 1.0F);

		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx == 0 && dy == 0)
					continue;
				g.pose().pushPose();
				g.pose().translate(dx * o, dy * o, 0.0F);
				g.drawString(font, text, 0, 0, outlineColor, false);
				g.pose().popPose();
			}
		}
		g.drawString(font, text, 0, 0, color, false);

		g.pose().popPose();
	}
}