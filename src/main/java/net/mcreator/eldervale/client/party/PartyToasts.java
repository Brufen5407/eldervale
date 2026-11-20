package net.mcreator.eldervale.client.party;

import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.gui.GuiGraphics;

import net.mcreator.eldervale.party.PartyClientState;
import net.mcreator.eldervale.party.PartyNotifyPayload;

/**
 * Small notifications at the top center of the screen. Drawn by PartyHud while playing
 * and by PartyScreen while the Party window is open. Each one slides in, waits, fades out
 * and plays a soft sound the first time it is drawn.
 */
public final class PartyToasts {

	private static final int W = 214;
	private static final int H = 30;
	private static final int GAP = 4;
	private static final long FADE_IN_MS = 260L;
	private static final long FADE_OUT_MS = 420L;

	private PartyToasts() {
	}

	public static void render(GuiGraphics g, int screenWidth) {
		List<PartyClientState.Toast> toasts = PartyClientState.activeToasts();
		if (toasts.isEmpty())
			return;
		long now = System.currentTimeMillis();
		long life = PartyClientState.toastLifeMs();
		int y = 8;

		for (PartyClientState.Toast t : toasts) {
			long age = now - t.created;
			if (!t.soundPlayed) {
				t.soundPlayed = true;
				sound(t.kind);
			}
			float in = Math.min(1.0F, age / (float) FADE_IN_MS);
			float out = Math.min(1.0F, (life - age) / (float) FADE_OUT_MS);
			float a = Math.max(0.0F, Math.min(in, out));
			if (a * 255 < 12) {
				y += H + GAP;
				continue;
			}
			float ease = 1.0F - (1.0F - in) * (1.0F - in) * (1.0F - in);
			int x = (screenWidth - W) / 2;
			int dy = (int) ((1.0F - ease) * -12.0F);
			draw(g, t, x, y + dy, a);
			y += H + GAP;
		}
	}

	private static void draw(GuiGraphics g, PartyClientState.Toast t, int x, int y, float a) {
		int accent = accent(t.kind);
		int dark = PartyGfx.fade(PartyGfx.FRAME_DARK, a);

		// frame, body, accent stripe
		PartyGfx.chamfer(g, x - 1, y - 1, W + 2, H + 2, 4, dark);
		PartyGfx.chamfer(g, x, y, W, H, 4, PartyGfx.fade(PartyGfx.GOLD_DARK, a));
		PartyGfx.chamfer(g, x + 1, y + 1, W - 2, H - 2, 3, PartyGfx.fade(PartyGfx.FRAME_DARK, a));
		g.fillGradient(x + 2, y + 2, x + W - 2, y + H - 2, PartyGfx.fade(PartyGfx.BG_TOP, a), PartyGfx.fade(PartyGfx.BG_BOTTOM, a));
		g.fill(x + 2, y + 4, x + 5, y + H - 4, PartyGfx.fade(accent, a));

		// icon badge
		int bx = x + 10;
		int by = y + 5;
		PartyGfx.diamond(g, bx + 9, by + 10, 10, PartyGfx.fade(PartyGfx.shade(accent, 0.35F), a));
		String[] icon = icon(t.kind);
		int scale = 2;
		int ix = bx + 9 - PartyGfx.iconWidth(icon, scale) / 2;
		int iy = by + 10 - icon.length * scale / 2;
		PartyGfx.icon(g, icon, ix, iy, scale, PartyGfx.fade(accent, a));

		// texts
		int tx = x + 34;
		float maxW = W - 34 - 8;
		PartyGfx.plain(g, PartyGfx.ellipsize(t.title, maxW, 0.85F), tx, y + 5, 0.85F, PartyGfx.fade(PartyGfx.GOLD_LIGHT, a));
		String body = t.text;
		if (t.kind == PartyNotifyPayload.INVITE)
			body = body + " Press " + PartyClientEvents.openKeyName() + ".";
		PartyGfx.plain(g, PartyGfx.ellipsize(body, maxW, 0.68F), tx, y + 17, 0.68F, PartyGfx.fade(PartyGfx.TEXT, a));

		// time bar
		long age = System.currentTimeMillis() - t.created;
		float left = 1.0F - Math.min(1.0F, age / (float) PartyClientState.toastLifeMs());
		g.fill(x + 8, y + H - 3, x + W - 8, y + H - 2, PartyGfx.fade(0xFF1B1620, a));
		g.fill(x + 8, y + H - 3, x + 8 + (int) ((W - 16) * left), y + H - 2, PartyGfx.fade(accent, a * 0.8F));
	}

	private static int accent(int kind) {
		return switch (kind) {
			case PartyNotifyPayload.SUCCESS, PartyNotifyPayload.JOIN -> PartyGfx.GREEN;
			case PartyNotifyPayload.ERROR -> PartyGfx.RED;
			case PartyNotifyPayload.INVITE, PartyNotifyPayload.CROWN -> PartyGfx.GOLD_LIGHT;
			case PartyNotifyPayload.LEAVE -> 0xFFFFA24A;
			default -> PartyGfx.PURPLE;
		};
	}

	private static String[] icon(int kind) {
		return switch (kind) {
			case PartyNotifyPayload.SUCCESS -> PartyGfx.ICON_CHECK;
			case PartyNotifyPayload.ERROR, PartyNotifyPayload.LEAVE -> PartyGfx.ICON_CROSS;
			case PartyNotifyPayload.INVITE -> PartyGfx.ICON_MAIL;
			case PartyNotifyPayload.JOIN -> PartyGfx.ICON_PLUS;
			case PartyNotifyPayload.CROWN -> PartyGfx.ICON_CROWN;
			default -> PartyGfx.ICON_SHIELD;
		};
	}

	private static void sound(int kind) {
		switch (kind) {
			case PartyNotifyPayload.SUCCESS, PartyNotifyPayload.JOIN -> PartyGfx.playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, 0.5F);
			case PartyNotifyPayload.ERROR -> PartyGfx.playUi(SoundEvents.VILLAGER_NO, 1.2F, 0.35F);
			case PartyNotifyPayload.INVITE -> PartyGfx.playUi(SoundEvents.NOTE_BLOCK_BELL, 1.2F, 0.6F);
			case PartyNotifyPayload.CROWN -> PartyGfx.playUi(SoundEvents.PLAYER_LEVELUP, 1.4F, 0.35F);
			case PartyNotifyPayload.LEAVE -> PartyGfx.playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.4F);
			default -> PartyGfx.playUi(SoundEvents.UI_BUTTON_CLICK, 1.4F, 0.35F);
		}
	}
}
