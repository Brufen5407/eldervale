package net.mcreator.eldervale.client.ui.kit;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * Shared look of the Eldervale menus: palette, textures, font and small drawing helpers.
 * Client only. Everything here is stateless.
 */
public final class UiKit {

	private UiKit() {
	}

	public static final String NS = "eldervale";

	/** Set to false to fall back to the vanilla font (e.g. if cinzel.ttf is missing). */
	public static final boolean USE_CINZEL = true;
	public static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath(NS, "cinzel");

	// ---- textures (assets/eldervale/textures/gui/menu) ----
	public static ResourceLocation tex(String name) {
		return ResourceLocation.fromNamespaceAndPath(NS, "textures/gui/menu/" + name + ".png");
	}

	public static final ResourceLocation BUTTON = tex("button"); // 48x60: normal / hover / disabled, 20px each, border 6
	public static final ResourceLocation PANEL = tex("panel"); // 32x32, border 8
	public static final ResourceLocation CARD = tex("card"); // 32x64: normal / selected, border 4
	public static final ResourceLocation ICONS = tex("icons"); // 48x32: language / accessibility / mods, normal row + hover row
	public static final ResourceLocation TAB = tex("tab"); // 24x72: normal / selected / disabled, 24px each
	public static final ResourceLocation SLOT = tex("slot"); // 18x18 inset slot, border 2
	public static final ResourceLocation RPG_ICONS = tex("rpg_icons"); // 128x32: 8 section icons (16px), normal row + hover row

	// ---- palette (ARGB). Gold is for accents only. ----
	public static final int TEXT = 0xFFD8CDB0; // parchment
	public static final int TEXT_HOVER = 0xFFE3CF93; // antique gold
	public static final int TEXT_DIM = 0xFF8A8296;
	public static final int TEXT_DISABLED = 0xFF5A566A;
	public static final int GOLD = 0xFFC9A45A;

	// ---- text ----
	/** Component rendered with Cinzel (titles, logo line, buttons). */
	public static MutableComponent title(Component c) {
		MutableComponent m = c.copy();
		return USE_CINZEL ? m.withStyle(s -> s.withFont(FONT)) : m;
	}

	public static MutableComponent title(String literal) {
		return title(Component.literal(literal));
	}

	public static int withAlpha(int rgb, float alpha) {
		int a = Math.max(4, Math.min(255, (int) (alpha * 255.0F)));
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	public static int lerpColor(int c1, int c2, float t) {
		int r = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
		int g = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
		int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
		return (r << 16) | (g << 8) | b;
	}

	/** Centered text with extra pixels between letters (used for the tagline). */
	public static void drawSpaced(GuiGraphics g, Font font, String text, int centerX, int y, int color, int spacing, boolean shadow) {
		int total = 0;
		Component[] parts = new Component[text.length()];
		for (int i = 0; i < text.length(); i++) {
			parts[i] = title(String.valueOf(text.charAt(i)));
			total += font.width(parts[i]) + (i < text.length() - 1 ? spacing : 0);
		}
		int x = centerX - total / 2;
		for (Component p : parts) {
			g.drawString(font, p, x, y, color, shadow);
			x += font.width(p) + spacing;
		}
	}

	// ---- 9-slice ----
	/**
	 * Draws a region of a texture as a 9-slice: corners keep their size, edges and centre stretch.
	 * (u, v, sw, sh) is the source region inside a texture of size (tw, th); b is the border in pixels.
	 */
	public static void nineSlice(GuiGraphics g, ResourceLocation t, int x, int y, int w, int h, int u, int v, int sw, int sh, int tw, int th, int b) {
		int mw = w - 2 * b, mh = h - 2 * b;
		int smw = sw - 2 * b, smh = sh - 2 * b;
		// corners
		g.blit(t, x, y, b, b, (float) u, (float) v, b, b, tw, th);
		g.blit(t, x + w - b, y, b, b, (float) (u + sw - b), (float) v, b, b, tw, th);
		g.blit(t, x, y + h - b, b, b, (float) u, (float) (v + sh - b), b, b, tw, th);
		g.blit(t, x + w - b, y + h - b, b, b, (float) (u + sw - b), (float) (v + sh - b), b, b, tw, th);
		// edges
		g.blit(t, x + b, y, mw, b, (float) (u + b), (float) v, smw, b, tw, th);
		g.blit(t, x + b, y + h - b, mw, b, (float) (u + b), (float) (v + sh - b), smw, b, tw, th);
		g.blit(t, x, y + b, b, mh, (float) u, (float) (v + b), b, smh, tw, th);
		g.blit(t, x + w - b, y + b, b, mh, (float) (u + sw - b), (float) (v + b), b, smh, tw, th);
		// centre
		g.blit(t, x + b, y + b, mw, mh, (float) (u + b), (float) (v + b), smw, smh, tw, th);
	}

	/** Square section tab (24x24) with its icon. state: 0 normal, 1 selected/hover, 2 disabled. icon: 0..7 (see RPG_ICONS). */
	public static void drawSectionTab(GuiGraphics g, int x, int y, int state, int icon, boolean hot) {
		g.blit(TAB, x, y, 0.0F, state * 24.0F, 24, 24, 24, 72);
		g.blit(RPG_ICONS, x + 4, y + 4, icon * 16.0F, hot ? 16.0F : 0.0F, 16, 16, 128, 32);
	}
}
