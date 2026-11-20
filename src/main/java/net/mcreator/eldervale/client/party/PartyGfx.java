package net.mcreator.eldervale.client.party;

import java.util.Locale;
import java.util.UUID;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Drawing helpers shared by the Party screen, the Party HUD and the toasts.
 * Same visual language as FancyClassScreen and SkillHud (dark panels, gold frames),
 * with purple as the Party accent. Client only.
 */
public final class PartyGfx {

	// ---- Colors 0xAARRGGBB ----
	public static final int FRAME_DARK = 0xFF0B0810;
	public static final int GOLD_LIGHT = 0xFFF2D27A;
	public static final int GOLD = 0xFFC9A24A;
	public static final int GOLD_DARK = 0xFF7A5A1E;
	public static final int BG_TOP = 0xFF2A2430;
	public static final int BG_BOTTOM = 0xFF141018;
	public static final int WHITE = 0xFFFFFFFF;
	public static final int BLACK = 0xFF000000;
	public static final int TEXT = 0xFFE6E0F0;
	public static final int MUTED = 0xFF8A8396;
	public static final int PURPLE = 0xFF9B6BFF;
	public static final int GREEN = 0xFF5FCF6B;
	public static final int RED = 0xFFE0453A;
	public static final int BLUE = 0xFF4AA3FF;

	// ---- Icons: '#' = filled pixel ----
	public static final String[] ICON_CROWN = {"#...#...#", "##.###.##", "#########", "#########", "#########", ".#######."};
	public static final String[] ICON_CHECK = {"......#", ".....##", "#...##.", "##.##..", ".###...", "..#...."};
	public static final String[] ICON_CROSS = {"##...##", "###.###", ".#####.", "..###..", ".#####.", "###.###", "##...##"};
	public static final String[] ICON_PLUS = {"...#...", "...#...", "...#...", "#######", "...#...", "...#...", "...#..."};
	public static final String[] ICON_MAIL = {"#########", "##.....##", "#.#...#.#", "#..#.#..#", "#...#...#", "#########"};
	public static final String[] ICON_SHIELD = {"#######", "#.....#", "#.....#", "#.....#", ".#...#.", ".#...#.", "..#.#..", "...#..."};

	private PartyGfx() {
	}

	public static Minecraft mc() {
		return Minecraft.getInstance();
	}

	// ---------------------------------------------------------------
	//  Classes
	// ---------------------------------------------------------------
	public static int classColor(String cls) {
		if (cls == null)
			return 0xFF9A98A6;
		return switch (cls.toLowerCase(Locale.ROOT)) {
			case "paladin" -> 0xFFF2D27A;
			case "berserker" -> 0xFFE0453A;
			case "ranger" -> 0xFF5FCF6B;
			case "mage" -> 0xFF6EA8FF;
			case "assassin" -> 0xFFA55CFF;
			case "monk" -> 0xFFFFA24A;
			case "werewolf" -> 0xFFB08E6A;
			case "alchemist" -> 0xFF4FE0C0;
			case "spellblade" -> 0xFFFF6BD6;
			default -> 0xFF9A98A6;
		};
	}

	public static String classLabel(String cls) {
		if (cls == null || cls.isEmpty())
			return "No class";
		return cls.substring(0, 1).toUpperCase(Locale.ROOT) + cls.substring(1).toLowerCase(Locale.ROOT);
	}

	// ---------------------------------------------------------------
	//  Colors
	// ---------------------------------------------------------------
	public static int shade(int argb, float f) {
		int r = (int) (((argb >> 16) & 255) * f);
		int g = (int) (((argb >> 8) & 255) * f);
		int b = (int) ((argb & 255) * f);
		return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
	}

	/** Multiplies the alpha of a color by f (0..1). */
	public static int fade(int argb, float f) {
		int a = (int) (((argb >>> 24) & 255) * Math.max(0.0F, Math.min(1.0F, f)));
		return (a << 24) | (argb & 0xFFFFFF);
	}

	// ---------------------------------------------------------------
	//  Text
	// ---------------------------------------------------------------
	public static float width(String text, float scale) {
		return mc().font.width(text) * scale;
	}

	public static void outlined(GuiGraphics g, String text, float x, float y, float scale, int color, int outline) {
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

	public static void outlinedC(GuiGraphics g, String text, float cx, float y, float scale, int color, int outline) {
		outlined(g, text, cx - width(text, scale) / 2.0F, y, scale, color, outline);
	}

	public static void outlinedR(GuiGraphics g, String text, float rx, float y, float scale, int color, int outline) {
		outlined(g, text, rx - width(text, scale), y, scale, color, outline);
	}

	/** Text with a plain drop shadow (cheaper than outlined: used for small text and the HUD). */
	public static void plain(GuiGraphics g, String text, float x, float y, float scale, int color) {
		if (text == null || text.isEmpty())
			return;
		g.pose().pushPose();
		g.pose().translate(x, y, 0.0F);
		g.pose().scale(scale, scale, 1.0F);
		g.drawString(mc().font, text, 0, 0, color, true);
		g.pose().popPose();
	}

	public static String ellipsize(String text, float maxWidth, float scale) {
		if (width(text, scale) <= maxWidth)
			return text;
		String t = text;
		while (t.length() > 1 && width(t + "..", scale) > maxWidth)
			t = t.substring(0, t.length() - 1);
		return t + "..";
	}

	// ---------------------------------------------------------------
	//  Shapes
	// ---------------------------------------------------------------
	public static void chamfer(GuiGraphics g, int x, int y, int w, int h, int c, int color) {
		for (int i = 0; i < c; i++) {
			g.fill(x + c - i, y + i, x + w - c + i, y + i + 1, color);
			g.fill(x + c - i, y + h - 1 - i, x + w - c + i, y + h - i, color);
		}
		g.fill(x, y + c, x + w, y + h - c, color);
	}

	public static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int i = -r; i <= r; i++) {
			int half = r - Math.abs(i);
			g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color);
		}
	}

	/** Main window frame: gold border, dark gradient, corner diamonds in the accent color. Origin (0,0). */
	public static void window(GuiGraphics g, int w, int h, int accent) {
		g.fill(-3, -3, w + 3, h + 3, FRAME_DARK);
		g.fill(-2, -2, w + 2, h + 2, GOLD_DARK);
		g.fill(-1, -1, w + 1, h + 1, GOLD);
		g.fill(0, 0, w, h, FRAME_DARK);
		g.fillGradient(2, 2, w - 2, h - 2, BG_TOP, BG_BOTTOM);
		g.fillGradient(2, 2, w - 2, 28, 0x30FFFFFF, 0x00FFFFFF);
		int[][] corners = {{0, 0}, {w, 0}, {0, h}, {w, h}};
		for (int[] k : corners) {
			diamond(g, k[0], k[1], 7, FRAME_DARK);
			diamond(g, k[0], k[1], 6, GOLD);
			diamond(g, k[0], k[1], 4, FRAME_DARK);
			diamond(g, k[0], k[1], 3, accent);
		}
	}

	/** Dark inset box with a thin gold outline. */
	public static void inset(GuiGraphics g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, 0x40000000);
		g.fill(x, y, x + w, y + 1, 0x60C9A24A);
		g.fill(x, y + h - 1, x + w, y + h, 0x60C9A24A);
		g.fill(x, y, x + 1, y + h, 0x60C9A24A);
		g.fill(x + w - 1, y, x + w, y + h, 0x60C9A24A);
	}

	public static void bar(GuiGraphics g, int x, int y, int w, int h, double ratio, int top, int bottom) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BLACK);
		g.fill(x, y, x + w, y + h, 0xFF1B1620);
		int fw = (int) Math.round(Math.max(0.0, Math.min(1.0, ratio)) * w);
		if (fw > 0) {
			g.fillGradient(x, y, x + fw, y + h, top, bottom);
			if (h >= 3)
				g.fill(x, y, x + fw, y + 1, 0x55FFFFFF);
		}
	}

	/** Draws a pixel icon: every '#' becomes a scale x scale square. */
	public static void icon(GuiGraphics g, String[] rows, int x, int y, int scale, int color) {
		for (int r = 0; r < rows.length; r++)
			for (int c = 0; c < rows[r].length(); c++)
				if (rows[r].charAt(c) == '#')
					g.fill(x + c * scale, y + r * scale, x + (c + 1) * scale, y + (r + 1) * scale, color);
	}

	public static int iconWidth(String[] rows, int scale) {
		return rows[0].length() * scale;
	}

	// ---------------------------------------------------------------
	//  Player heads
	// ---------------------------------------------------------------
	/** Face of a player (with the hat layer). Offline or unknown players get the default skin. */
	public static void head(GuiGraphics g, UUID id, int x, int y, int size, boolean dim) {
		ClientPacketListener conn = mc().getConnection();
		PlayerInfo info = conn == null ? null : conn.getPlayerInfo(id);
		ResourceLocation tex = info != null ? info.getSkin().texture() : DefaultPlayerSkin.get(id).texture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		g.blit(tex, x, y, size, size, 8.0F, 8.0F, 8, 8, 64, 64);
		g.blit(tex, x, y, size, size, 40.0F, 8.0F, 8, 8, 64, 64);
		if (dim)
			g.fill(x, y, x + size, y + size, 0xA0101018);
	}

	// ---------------------------------------------------------------
	//  Sounds
	// ---------------------------------------------------------------
	public static void playUi(SoundEvent sound, float pitch, float volume) {
		mc().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
	}

	public static void playUi(Holder<SoundEvent> sound, float pitch, float volume) {
		playUi(sound.value(), pitch, volume);
	}
}
