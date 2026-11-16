package net.mcreator.eldervale.client.hud;

import java.util.Locale;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.procedures.Skill1returniconProcedure;
import net.mcreator.eldervale.procedures.Skill2returniconProcedure;

@EventBusSubscriber(value = Dist.CLIENT)
public class SkillHud {

	// ---- Settings (edit here) ----
	private static final boolean SOUNDS_ENABLED = true;
	private static final String KEY_1 = "V";
	private static final String KEY_2 = "B";

	private static final int SLOT = 32;          // slot size
	private static final int SLOT_X1 = 19;       // x of the first slot
	private static final int SLOT_X2 = 75;       // x of the second slot
	private static final int BOTTOM_OFFSET = 46; // y = screen height - this

	// How long the "active" glow lasts after casting (paladin: shield 5s, zone buff 5s)
	private static final float ACTIVE_SECONDS = 5.0F;

	// ---- Colors 0xAARRGGBB ----
	private static final int FRAME_DARK = 0xFF0B0810;
	private static final int GOLD_LIGHT = 0xFFF2D27A;
	private static final int GOLD = 0xFFC9A24A;
	private static final int GOLD_DARK = 0xFF7A5A1E;
	private static final int BG_TOP = 0xFF2A2430;
	private static final int BG_BOTTOM = 0xFF141018;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;

	private static class Slot {
		double prevCd = -1;
		double peak = 0;
		long flashStart = 0;
		long castStart = 0;
	}

	private static final Slot S1 = new Slot();
	private static final Slot S2 = new Slot();

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.options.hideGui)
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		GuiGraphics g = event.getGuiGraphics();
		int y = g.guiHeight() - BOTTOM_OFFSET;
		long now = System.nanoTime();

		String cls = v.PLAYER_CLASS;
		boolean paladin = "paladin".equals(cls);
		int[] cost = costs(cls);

		ResourceLocation icon1 = iconTexture(java.util.Objects.toString(Skill1returniconProcedure.execute(player), ""), "paladin_skill1.png");
		ResourceLocation icon2 = iconTexture(java.util.Objects.toString(Skill2returniconProcedure.execute(player), ""), "paladin_skill2.png");

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1, 1, 1, 1);

		drawSlot(g, S1, SLOT_X1, y, v.SKILL1_CD, v.SKILL1_MAX_CD, icon1, KEY_1, cost == null ? -1 : cost[0], v.MANA, paladin, paladin && v.paladin_shields, now);
		drawSlot(g, S2, SLOT_X2, y, v.SKILL2_CD, v.SKILL2_MAX_CD, icon2, KEY_2, cost == null ? -1 : cost[1], v.MANA, paladin, true, now);

		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	// Mana costs per class (from your class sheet). Paladin is confirmed in the procedures.
	private static int[] costs(String cls) {
		if (cls == null)
			return null;
		return switch (cls) {
			case "paladin" -> new int[]{50, 30};
			case "berserker" -> new int[]{30, 20};
			case "ranger" -> new int[]{35, 40};
			default -> null;
		};
	}

	private static void drawSlot(GuiGraphics g, Slot st, int x, int y, double cd, double maxCd, ResourceLocation icon, String key, int cost, double mana, boolean trackActive, boolean activeGate, long now) {
		// ---- detect events ----
		if (st.prevCd < 0)
			st.prevCd = cd;
		if (st.prevCd > 0.5 && cd <= 0.5) {
			st.flashStart = now;
			playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, 0.5F);
		}
		if (cd - st.prevCd > 50.0)
			st.castStart = now;
		st.prevCd = cd;

		if (cd > st.peak)
			st.peak = cd;
		if (cd <= 0.5)
			st.peak = 0;

		boolean onCd = cd > 0.5;
		double max = maxCd > 0 ? Math.max(maxCd, cd) : Math.max(st.peak, cd);
		double progress = onCd && max > 0 ? 1.0 - Math.min(1.0, cd / max) : 1.0;

		double sinceCast = st.castStart == 0 ? 999.0 : (now - st.castStart) / 1.0E9;
		boolean active = trackActive && activeGate && sinceCast < ACTIVE_SECONDS;

		// ---- glow when active ----
		if (active) {
			float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 1000.0 * 7.0));
			int a = (int) (45 + 90 * pulse);
			chamfer(g, x - 4, y - 4, SLOT + 8, SLOT + 8, 6, (a << 24) | 0xFFC840);
		}

		frame(g, x, y, active);

		// ---- icon ----
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		g.blit(icon, x + 3, y + 3, 0, 0, 26, 26, 26, 26);

		// ---- not enough mana ----
		boolean noMana = !onCd && cost > 0 && mana <= cost;
		if (noMana)
			g.fill(x + 3, y + 3, x + 29, y + 29, 0x99102050);

		// ---- cooldown sweep + seconds ----
		if (onCd) {
			sweep(g, x + 3, y + 3, 26, progress);
			double seconds = cd / 20.0;
			String t = seconds >= 10 ? String.valueOf((int) Math.ceil(seconds)) : String.format(Locale.ROOT, "%.1f", seconds);
			outlinedC(g, t, x + SLOT / 2.0F, y + 12.0F, 1.0F, WHITE, BLACK);
		}

		// ---- "ready" flash ----
		if (st.flashStart != 0) {
			double e = (now - st.flashStart) / 1.0E9;
			if (e < 0.45) {
				int a = (int) ((1.0 - e / 0.45) * 170);
				g.fill(x + 3, y + 3, x + 29, y + 29, (a << 24) | 0xFFF0B0);
			}
		}

		// ---- active timer bar above the slot ----
		if (active) {
			double rem = 1.0 - sinceCast / ACTIVE_SECONDS;
			int bw = (int) Math.round(26 * Math.max(0.0, Math.min(1.0, rem)));
			g.fill(x + 2, y - 7, x + 30, y - 3, FRAME_DARK);
			g.fill(x + 3, y - 6, x + 29, y - 4, 0xFF1B1620);
			if (bw > 0)
				g.fillGradient(x + 3, y - 6, x + 3 + bw, y - 4, GOLD_LIGHT, GOLD_DARK);
		}

		// ---- mana cost (top right) ----
		if (cost > 0) {
			String c = String.valueOf(cost);
			float w = mc().font.width(c) * 0.65F;
			outlined(g, c, x + SLOT - 4 - w, y + 4, 0.65F, noMana ? 0xFFFF6060 : 0xFF7FC8FF, BLACK);
		}

		// ---- key cap (bottom center) ----
		keyCap(g, key, x + SLOT / 2, y + SLOT - 5);
	}

	// ---------------------------------------------------------------
	//  Drawing helpers
	// ---------------------------------------------------------------
	private static void frame(GuiGraphics g, int x, int y, boolean active) {
		int gold = active ? GOLD_LIGHT : GOLD;
		chamfer(g, x - 1, y - 1, SLOT + 2, SLOT + 2, 4, FRAME_DARK);
		chamfer(g, x, y, SLOT, SLOT, 4, gold);
		chamfer(g, x + 2, y + 2, SLOT - 4, SLOT - 4, 3, FRAME_DARK);
		g.fillGradient(x + 3, y + 3, x + SLOT - 3, y + SLOT - 3, BG_TOP, BG_BOTTOM);
		g.fill(x + 4, y, x + SLOT - 4, y + 1, GOLD_LIGHT);
		g.fill(x + 4, y + SLOT - 1, x + SLOT - 4, y + SLOT, GOLD_DARK);
	}

	// radial cooldown: progress 0 = just used (all dark), 1 = ready
	private static void sweep(GuiGraphics g, int x, int y, int size, double progress) {
		double c = size / 2.0;
		for (int py = 0; py < size; py++) {
			for (int px = 0; px < size; px++) {
				double dx = px + 0.5 - c;
				double dy = py + 0.5 - c;
				double a = Math.atan2(dx, -dy); // 0 at the top, clockwise
				if (a < 0)
					a += Math.PI * 2.0;
				double f = a / (Math.PI * 2.0);
				if (f >= progress)
					g.fill(x + px, y + py, x + px + 1, y + py + 1, 0xB0000000);
			}
		}
	}

	private static void keyCap(GuiGraphics g, String key, int cx, int y) {
		g.fill(cx - 7, y, cx + 7, y + 11, FRAME_DARK);
		g.fill(cx - 6, y + 1, cx + 6, y + 10, GOLD);
		g.fill(cx - 5, y + 2, cx + 5, y + 9, 0xFF2A2430);
		outlinedC(g, key, cx, y + 2.3F, 0.8F, WHITE, BLACK);
	}

	private static void chamfer(GuiGraphics g, int x, int y, int w, int h, int c, int color) {
		for (int i = 0; i < c; i++) {
			g.fill(x + c - i, y + i, x + w - c + i, y + i + 1, color);
			g.fill(x + c - i, y + h - 1 - i, x + w - c + i, y + h - i, color);
		}
		g.fill(x, y + c, x + w, y + h - c, color);
	}

	private static Minecraft mc() {
		return Minecraft.getInstance();
	}

	private static void outlined(GuiGraphics g, String text, float x, float y, float scale, int color, int outline) {
		if (text == null || text.isEmpty())
			return;
		Font font = mc().font;
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

	private static void outlinedC(GuiGraphics g, String text, float cx, float y, float scale, int color, int outline) {
		outlined(g, text, cx - mc().font.width(text) * scale / 2.0F, y, scale, color, outline);
	}

	// Same logic MCreator used for dynamic icon names
	private static ResourceLocation iconTexture(String value, String fallbackFile) {
		ResourceLocation fallback = ResourceLocation.fromNamespaceAndPath("eldervale", "textures/screens/" + fallbackFile);
		if (value == null || value.isBlank())
			return fallback;
		try {
			String texture = value.trim().replace('\\', '/');
			if (texture.indexOf(':') >= 0)
				return ResourceLocation.parse(texture);
			while (texture.startsWith("/"))
				texture = texture.substring(1);
			if (texture.startsWith("textures/screens/"))
				texture = texture.substring("textures/screens/".length());
			if (!texture.endsWith(".png"))
				texture += ".png";
			return ResourceLocation.fromNamespaceAndPath("eldervale", "textures/screens/" + texture);
		} catch (RuntimeException ignored) {
			return fallback;
		}
	}

	private static void playUi(SoundEvent sound, float pitch, float volume) {
		if (!SOUNDS_ENABLED)
			return;
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	private static void playUi(Holder<SoundEvent> sound, float pitch, float volume) {
		playUi(sound.value(), pitch, volume);
	}
}
