package net.mcreator.eldervale.client.party;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.party.PartyStatePayload;
import net.mcreator.eldervale.party.PartyClientState;

/**
 * Compact party list on the left of the screen: class, name, HP and mana of every
 * party member (not yourself). Small on purpose, so it does not cover the view in combat.
 * Also draws the Party toasts while no screen shows them.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class PartyHud {

	// ---- Settings (edit here) ----
	private static final int X = 6;       // left margin
	private static final int Y = 36;      // top of the first row
	private static final int W = 114;     // row width
	private static final int H = 27;      // row height
	private static final int STEP = 30;   // distance between rows
	private static final float LOW_HP = 0.25F;

	// smoothed bar values per member: {hp ratio, mana ratio}
	private static final Map<UUID, float[]> SHOWN = new HashMap<>();
	private static long lastNanos = 0L;

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		Minecraft mc = PartyGfx.mc();
		if (mc.player == null || mc.options.hideGui)
			return;
		GuiGraphics g = event.getGuiGraphics();
		boolean partyScreen = mc.screen instanceof PartyScreen;

		if (!partyScreen)
			PartyToasts.render(g, g.guiWidth());

		if (!PartyClientState.inParty) {
			SHOWN.clear();
			return;
		}
		if (partyScreen)
			return; // the window already lists everybody

		long now = System.nanoTime();
		float dt = lastNanos == 0L ? 0.016F : Math.min(0.1F, (now - lastNanos) / 1.0E9F);
		lastNanos = now;

		UUID me = mc.player.getUUID();
		int row = 0;
		for (PartyStatePayload.MemberInfo m : PartyClientState.members) {
			if (m.id().equals(me))
				continue;
			drawRow(g, m, X, Y + row * STEP, dt);
			row++;
		}
	}

	private static float ratio(float value, float max) {
		return max <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, value / max));
	}

	private static void drawRow(GuiGraphics g, PartyStatePayload.MemberInfo m, int x, int y, float dt) {
		boolean on = m.online();
		int cc = PartyGfx.classColor(m.playerClass());

		float hpTarget = ratio(m.hp(), m.hpMax());
		float manaTarget = ratio(m.mana(), m.manaMax());
		float[] s = SHOWN.computeIfAbsent(m.id(), k -> new float[]{hpTarget, manaTarget});
		float k = 1.0F - (float) Math.exp(-dt * 9.0F);
		s[0] += (hpTarget - s[0]) * k;
		s[1] += (manaTarget - s[1]) * k;

		// frame, body and class stripe
		PartyGfx.chamfer(g, x - 1, y - 1, W + 2, H + 2, 4, PartyGfx.FRAME_DARK);
		PartyGfx.chamfer(g, x, y, W, H, 4, on ? PartyGfx.GOLD_DARK : 0xFF3A3440);
		PartyGfx.chamfer(g, x + 1, y + 1, W - 2, H - 2, 3, PartyGfx.FRAME_DARK);
		g.fillGradient(x + 2, y + 2, x + W - 2, y + H - 2, 0xE02A2430, 0xE0141018);
		g.fill(x + 2, y + 4, x + 4, y + H - 4, on ? cc : 0xFF4A4452);

		// head, name, crown
		PartyGfx.head(g, m.id(), x + 7, y + 5, 17, !on);
		float nx = x + 29;
		String name = PartyGfx.ellipsize(m.name(), 60.0F, 0.8F);
		PartyGfx.plain(g, name, nx, y + 3.0F, 0.8F, on ? PartyGfx.WHITE : PartyGfx.MUTED);
		if (m.id().equals(PartyClientState.leader))
			PartyGfx.icon(g, PartyGfx.ICON_CROWN, (int) (nx + PartyGfx.width(name, 0.8F) + 3.0F), y + 4, 1, PartyGfx.GOLD_LIGHT);

		// class and level
		String info = PartyGfx.classLabel(m.playerClass()) + " Lv " + m.level();
		PartyGfx.plain(g, info, nx, y + 12.5F, 0.55F, on ? cc : PartyGfx.MUTED);

		if (!on) {
			PartyGfx.plain(g, "OFFLINE", nx, y + 18.5F, 0.55F, PartyGfx.MUTED);
			return;
		}

		// hp number, hp bar, mana bar
		String hpText = (int) Math.ceil(m.hp()) + "/" + (int) Math.ceil(m.hpMax());
		PartyGfx.plain(g, hpText, x + W - 6 - PartyGfx.width(hpText, 0.5F), y + 13.0F, 0.5F, 0xFFB8AFC8);

		int barW = W - 29 - 6;
		boolean low = s[0] < LOW_HP;
		int hpTop = 0xFFE04848;
		int hpBottom = 0xFF8A1F1F;
		if (low) {
			float pulse = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 1000.0 * 8.0));
			hpTop = PartyGfx.shade(0xFFFF6060, 0.7F + 0.3F * pulse);
		}
		PartyGfx.bar(g, (int) nx, y + 18, barW, 4, s[0], hpTop, hpBottom);
		PartyGfx.bar(g, (int) nx, y + 23, barW, 2, s[1], 0xFF4AA3FF, 0xFF1F4F8A);
	}
}
