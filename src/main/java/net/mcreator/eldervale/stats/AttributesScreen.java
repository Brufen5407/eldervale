package net.mcreator.eldervale.client.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;

import net.mcreator.eldervale.client.party.PartyGfx;
import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.stats.AttributeSpendPayload;
import net.mcreator.eldervale.stats.ClassProfile;
import net.mcreator.eldervale.stats.ClientStatsState;
import net.mcreator.eldervale.stats.StatAttribute;
import net.mcreator.eldervale.stats.StatCalculator;
import net.mcreator.eldervale.stats.StatsConfig;

/**
 * Player Attributes window (key K). Left: the 6 attributes with a [+] button (Shift = +5).
 * Right: the stats of the class selection screen (HP, DEF, ATT SPEED, ATT, MANA, MANA REGEN, SPEED)
 * plus Critical Chance / Damage. Green = bonus from attributes; gold = preview when hovering [+].
 * The client never changes stats: every click is a request validated by the server.
 * Same look as PartyScreen / FancyClassScreen. Client only.
 */
public class AttributesScreen extends Screen {

	private static final int PW = 440;
	private static final int PH = 236;
	private static final int LX = 12, LY = 32, LW = 212, LH = 174;
	private static final int RX = 232, RW = 196;

	private static final String[] NAMES = {"STRENGTH", "VITALITY", "DEFENSE", "AGILITY", "INTELLIGENCE", "DEXTERITY"};
	private static final String[] DESCS = {"Increases ATT", "Increases max HP", "Reduces damage taken", "Speed and attack speed", "Mana and mana regen", "Critical chance and damage"};

	// display order = class selection screen, then crit
	private static final String[] LABELS = {"HP", "DEF", "ATT SPEED", "ATT", "MANA", "MANA REGEN", "SPEED", "CRIT CHANCE", "CRIT DAMAGE"};
	private static final int[] COLORS = {0xFFE04848, 0xFF8FA4BF, 0xFFFFB347, 0xFFFF6B3D, 0xFF4AA3FF, 0xFF55D6E0, 0xFF6EE06E, 0xFFFFD84A, 0xFFFF8A4A};
	private static final int[] DEC = {1, 1, 2, 2, 1, 1, 3, 1, 2};
	private static final int[] MIN_DEC = {0, 0, 1, 1, 0, 0, 2, 1, 2};
	private static final double[] BAR_SCALE = new double[7];
	private static final int BONUS_GREEN = 0xFF5FCF6B;

	static {
		for (ClassProfile p : ClassProfile.all()) {
			double[] v = {p.hp(), p.def() * 100.0, p.attSpeed(), p.att(), p.mana(), p.manaRegen(), p.speed()};
			for (int i = 0; i < 7; i++)
				BAR_SCALE[i] = Math.max(BAR_SCALE[i], v[i] * 1.6);
		}
	}

	private record Hit(int x, int y, int w, int h, boolean enabled, Runnable action) {
	}

	private final List<Hit> hits = new ArrayList<>();
	private final long openedAt = System.nanoTime();
	private int lastHover = -1;
	private double mpx, mpy;
	private float curScale = 1.0F;
	private int previewAttr = -1;
	private int previewAmount = 1;

	public AttributesScreen() {
		super(Component.literal("Attributes"));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---------------------------------------------------------------
	//  Scale and mouse (same logic as PartyScreen)
	// ---------------------------------------------------------------
	private void updateScale() {
		float fit = Math.min((this.width - 16.0F) / PW, (this.height - 16.0F) / PH);
		float base = fit >= 1.0F ? Math.min((float) Math.floor(fit), 3.0F) : Math.max(0.4F, fit);
		double open = Math.min(1.0, (System.nanoTime() - openedAt) / 1.0E9 / 0.2);
		float ease = (float) (1.0 - Math.pow(1.0 - open, 3.0));
		curScale = base * (0.92F + 0.08F * ease);
	}

	private double toPx(double mx) {
		return (mx - this.width / 2.0) / curScale + PW / 2.0;
	}

	private double toPy(double my) {
		return (my - this.height / 2.0) / curScale + PH / 2.0;
	}

	private static boolean in(double px, double py, int x, int y, int w, int h) {
		return px >= x && py >= y && px < x + w && py < y + h;
	}

	// ---------------------------------------------------------------
	//  Input
	// ---------------------------------------------------------------
	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button != 0)
			return super.mouseClicked(mx, my, button);
		updateScale();
		double px = toPx(mx);
		double py = toPy(my);
		for (Hit h : hits) {
			if (h.enabled() && in(px, py, h.x(), h.y(), h.w(), h.h())) {
				h.action().run();
				return true;
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		if (StatsClientEvents.OPEN.matches(key, scancode)) {
			this.onClose();
			return true;
		}
		return super.keyPressed(key, scancode, mods);
	}

	private void requestSpend(int attributeIndex) {
		int amount = Screen.hasShiftDown() ? 5 : 1;
		PartyGfx.playUi(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.3F, 0.35F);
		PacketDistributor.sendToServer(new AttributeSpendPayload(attributeIndex, amount));
	}

	// ---------------------------------------------------------------
	//  Data (all read from synced client state)
	// ---------------------------------------------------------------
	private static int[] invested() {
		int[] pts = new int[StatAttribute.COUNT];
		for (StatAttribute a : StatAttribute.values())
			pts[a.ordinal()] = ClientStatsState.get(a);
		return pts;
	}

	/** Result -> display units: DEF x100 (as in the class screen), crit damage as multiplier. */
	private static double[] display(StatCalculator.Result r) {
		return new double[]{r.hp(), r.def() * 100.0, r.attSpeed(), r.att(), r.mana(), r.manaRegen(), r.speed(), r.critChance(), r.critDamage() / 100.0};
	}

	private static String num(double v, int dec, int minDec) {
		String s = String.format(Locale.ROOT, "%." + dec + "f", v);
		int dot = s.indexOf('.');
		if (dot >= 0) {
			int min = dot + 1 + minDec;
			int end = s.length();
			while (end > min && s.charAt(end - 1) == '0')
				end--;
			s = s.substring(0, end);
			if (s.endsWith("."))
				s = s.substring(0, s.length() - 1);
		}
		return s;
	}

	private static String fmtValue(int i, double v) {
		String s = num(v, DEC[i], MIN_DEC[i]);
		return i == 5 ? s + "/s" : i == 7 ? s + "%" : i == 8 ? "x" + s : s;
	}

	private static String fmtDelta(int i, double v) {
		String s = (v < 0 ? "-" : "+") + num(Math.abs(v), DEC[i], 0);
		return i == 7 ? s + "%" : i == 5 ? s + "/s" : s;
	}

	private static boolean visible(int i, double delta) {
		return Math.abs(delta) >= 0.5 * Math.pow(10.0, -DEC[i]);
	}

	private static String signed(double v, String suffix) {
		return (v < 0 ? "-" : "+") + num(Math.abs(v), 1, 0) + suffix;
	}

	private static String effectText(StatAttribute a, StatCalculator.Bonus b) {
		return switch (a) {
			case STRENGTH -> signed(b.pct(StatCalculator.Stat.ATT) * 100.0, "% ATT");
			case VITALITY -> signed(b.flat(StatCalculator.Stat.HP), " HP");
			case DEFENSE -> signed(b.flat(StatCalculator.Stat.DEF) * 100.0, " DEF");
			case AGILITY -> signed(b.pct(StatCalculator.Stat.SPEED) * 100.0, "% SPEED   ") + signed(b.pct(StatCalculator.Stat.ATT_SPEED) * 100.0, "% ATT SPEED");
			case INTELLIGENCE -> signed(b.flat(StatCalculator.Stat.MANA), " MANA   ") + signed(b.flat(StatCalculator.Stat.MANA_REGEN), "/s REGEN");
			case DEXTERITY -> signed(b.flat(StatCalculator.Stat.CRIT_CHANCE), "% CRIT   ") + signed(b.flat(StatCalculator.Stat.CRIT_DAMAGE), "% CRIT DMG");
		};
	}

	// ---------------------------------------------------------------
	//  Render
	// ---------------------------------------------------------------
	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		updateScale();
		mpx = toPx(mouseX);
		mpy = toPy(mouseY);
		hits.clear();
		previewAttr = -1;

		EldervaleModVariables.PlayerVariables vars = PartyGfx.mc().player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		ClassProfile profile = ClassProfile.get(vars.PLAYER_CLASS);
		int accent = profile == null ? PartyGfx.GOLD : PartyGfx.classColor(vars.PLAYER_CLASS);

		g.fill(0, 0, this.width, this.height, 0xC0070510);
		g.pose().pushPose();
		g.pose().translate(this.width / 2.0F, this.height / 2.0F, 0.0F);
		g.pose().scale(curScale, curScale, 1.0F);
		g.pose().translate(-PW / 2.0F, -PH / 2.0F, 0.0F);

		PartyGfx.window(g, PW, PH, accent);
		drawHeader(g, vars, accent);
		PartyGfx.inset(g, LX, LY, LW, LH);
		PartyGfx.inset(g, RX, LY, RW, LH);

		if (profile == null) {
			PartyGfx.outlinedC(g, "Choose a class first.", PW / 2.0F, LY + 80, 1.0F, PartyGfx.MUTED, PartyGfx.BLACK);
		} else {
			int[] pts = invested();
			int free = (int) Math.floor(vars.STAT_POINT);
			drawAttributes(g, profile, pts, free, accent);
			drawStats(g, vars, profile, pts);
		}
		drawFooter(g);
		g.pose().popPose();

		// soft chime when the mouse moves onto a button
		int hover = -1;
		for (int i = 0; i < hits.size(); i++) {
			Hit h = hits.get(i);
			if (h.enabled() && in(mpx, mpy, h.x(), h.y(), h.w(), h.h()))
				hover = i;
		}
		if (hover != lastHover) {
			if (hover >= 0)
				PartyGfx.playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 1.8F, 0.2F);
			lastHover = hover;
		}
	}

	private void drawHeader(GuiGraphics g, EldervaleModVariables.PlayerVariables vars, int accent) {
		PartyGfx.icon(g, PartyGfx.ICON_SHIELD, 14, 8, 2, accent);
		PartyGfx.outlined(g, "ATTRIBUTES", 34, 9, 1.3F, PartyGfx.GOLD_LIGHT, PartyGfx.BLACK);
		String lv = "Lv " + (int) Math.round(vars.LEVEL);
		String cls = PartyGfx.classLabel(vars.PLAYER_CLASS);
		String name = PartyGfx.mc().player.getGameProfile().getName();
		float x = PW - 16;
		PartyGfx.outlinedR(g, lv, x, 10, 1.0F, PartyGfx.GOLD, PartyGfx.BLACK);
		x -= PartyGfx.width(lv, 1.0F) + 8;
		PartyGfx.outlinedR(g, cls, x, 10, 1.0F, accent, PartyGfx.BLACK);
		x -= PartyGfx.width(cls, 1.0F) + 8;
		PartyGfx.outlinedR(g, PartyGfx.ellipsize(name, 120.0F, 1.0F), x, 10, 1.0F, PartyGfx.WHITE, PartyGfx.BLACK);
		g.fill(24, 26, PW - 24, 27, PartyGfx.GOLD_DARK);
		PartyGfx.diamond(g, PW / 2, 26, 3, PartyGfx.GOLD);
	}

	private void drawFooter(GuiGraphics g) {
		PartyGfx.plain(g, "Shift + click = spend 5 points", 14, 214, 0.65F, PartyGfx.MUTED);
		String close = StatsClientEvents.OPEN.getTranslatedKeyMessage().getString() + " / ESC to close";
		PartyGfx.plain(g, close, PW - 14 - PartyGfx.width(close, 0.65F), 214, 0.65F, PartyGfx.MUTED);
		int[] pts = invested();
		int spent = 0;
		for (int p : pts)
			spent += p;
		String s = "Points spent: " + spent;
		PartyGfx.plain(g, s, PW / 2.0F - PartyGfx.width(s, 0.65F) / 2.0F, 214, 0.65F, PartyGfx.GOLD);
	}

	// ---------------------------------------------------------------
	//  Left column: attributes
	// ---------------------------------------------------------------
	private void drawAttributes(GuiGraphics g, ClassProfile profile, int[] pts, int free, int accent) {
		PartyGfx.plain(g, "ATTRIBUTE POINTS", LX + 8, LY + 6, 0.75F, PartyGfx.GOLD_LIGHT);
		String f = String.valueOf(free);
		int fc = free > 0 ? PartyGfx.GOLD_LIGHT : PartyGfx.MUTED;
		PartyGfx.outlinedR(g, f, LX + LW - 8, LY + 3, 1.3F, fc, PartyGfx.BLACK);
		if (free > 0) {
			float pulse = 0.5F + 0.5F * (float) Math.sin(System.currentTimeMillis() / 300.0);
			PartyGfx.diamond(g, LX + LW - 14 - (int) PartyGfx.width(f, 1.3F), LY + 9, 3, PartyGfx.fade(PartyGfx.GOLD_LIGHT, 0.4F + 0.6F * pulse));
		}

		for (StatAttribute a : StatAttribute.values()) {
			int i = a.ordinal();
			int x = LX + 4;
			int y = LY + 20 + i * 25;
			int w = LW - 8;
			int h = 23;
			double mult = profile.multiplier(a);
			boolean main = mult >= 1.25;
			boolean capped = pts[i] >= StatsConfig.MAX_POINTS_PER_ATTRIBUTE;

			g.fill(x, y, x + w, y + h, 0x40000000);
			g.fill(x, y, x + 2, y + h, main ? accent : 0xFF4A4452);

			float nx = x + 8;
			PartyGfx.outlined(g, NAMES[i], nx, y + 3, 0.9F, PartyGfx.WHITE, PartyGfx.BLACK);
			float tx = nx + PartyGfx.width(NAMES[i], 0.9F) + 6;
			if (main)
				PartyGfx.diamond(g, (int) tx + 2, y + 7, mult >= 1.5 ? 3 : 2, accent);
			PartyGfx.plain(g, "x" + num(mult, 2, 1), tx + 8, y + 5, 0.55F, main ? accent : PartyGfx.MUTED);

			String line;
			int lc;
			if (pts[i] > 0) {
				line = effectText(a, StatCalculator.attributeBonus(profile, a, pts[i]));
				lc = BONUS_GREEN;
			} else {
				line = DESCS[i];
				lc = PartyGfx.MUTED;
			}
			PartyGfx.plain(g, line, nx, y + 14, 0.6F, lc);

			// invested value and [+]
			int bw = 20, bh = 17;
			int bx = x + w - bw - 4;
			int by = y + 3;
			PartyGfx.outlinedR(g, capped ? "MAX" : String.valueOf(pts[i]), bx - 6, y + 6, 1.0F, capped ? PartyGfx.GOLD_LIGHT : PartyGfx.WHITE, PartyGfx.BLACK);
			boolean enabled = free > 0 && !capped;
			int idx = i;
			hits.add(new Hit(bx, by, bw, bh, enabled, () -> requestSpend(idx)));
			boolean hov = enabled && in(mpx, mpy, bx, by, bw, bh);
			if (hov) {
				previewAttr = i;
				previewAmount = Math.max(1, Math.min(Screen.hasShiftDown() ? 5 : 1, Math.min(free, StatsConfig.MAX_POINTS_PER_ATTRIBUTE - pts[i])));
			}
			plusButton(g, bx, by, bw, bh, enabled, hov);
		}
	}

	private void plusButton(GuiGraphics g, int x, int y, int w, int h, boolean enabled, boolean hov) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, PartyGfx.FRAME_DARK);
		g.fill(x, y, x + w, y + h, enabled ? (hov ? PartyGfx.GOLD_LIGHT : PartyGfx.GOLD) : 0xFF3A3440);
		int top = enabled ? (hov ? 0xFF5A4A2A : 0xFF3A3040) : 0xFF2A2630;
		int bottom = enabled ? (hov ? 0xFF3A2E18 : 0xFF1C1722) : 0xFF1C1822;
		g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x33FFFFFF);
		PartyGfx.icon(g, PartyGfx.ICON_PLUS, x + (w - 7) / 2, y + (h - 7) / 2, 1, enabled ? (hov ? PartyGfx.WHITE : PartyGfx.GOLD_LIGHT) : PartyGfx.MUTED);
	}

	// ---------------------------------------------------------------
	//  Right column: stats
	// ---------------------------------------------------------------
	private void drawStats(GuiGraphics g, EldervaleModVariables.PlayerVariables vars, ClassProfile profile, int[] pts) {
		PartyGfx.outlined(g, "STATS", RX + 8, LY + 5, 0.8F, PartyGfx.GOLD_LIGHT, PartyGfx.BLACK);

		StatCalculator.Result cur = new StatCalculator.Result(vars.HEALT_MAX, vars.ATTACK, vars.DEFENCE, vars.ATTACK_SPEED, vars.SPEED, vars.MANA_MAX, vars.MANA_REGEN, vars.CRITICAL_CHANCE, vars.CRITICAL_DAMAGE);
		StatCalculator.Result base = new StatCalculator.Result(profile.hp(), profile.att(), profile.def(), profile.attSpeed(), profile.speed(), profile.mana(), profile.manaRegen(), StatsConfig.BASE_CRIT_CHANCE, StatsConfig.BASE_CRIT_DAMAGE);
		double[] c = display(cur);
		double[] b = display(base);
		double[] d = new double[9];
		if (previewAttr >= 0) {
			int[] after = pts.clone();
			after[previewAttr] += previewAmount;
			double[] a1 = display(StatCalculator.compute(profile, after, null));
			double[] a0 = display(StatCalculator.compute(profile, pts, null));
			for (int i = 0; i < 9; i++)
				d[i] = a1[i] - a0[i];
		}

		for (int i = 0; i < 9; i++) {
			int y = LY + 16 + i * 17;
			int x = RX + 8;
			int rx = RX + RW - 8;
			double bonus = c[i] - b[i];
			boolean hasBar = i < 7;

			PartyGfx.outlined(g, LABELS[i], x, y, 0.7F, 0xFFD8D0E0, PartyGfx.BLACK);

			String value = fmtValue(i, c[i]);
			PartyGfx.outlinedR(g, value, rx, y - 0.5F, 0.8F, PartyGfx.WHITE, PartyGfx.BLACK);
			float left = rx - PartyGfx.width(value, 0.8F) - 4;
			if (visible(i, bonus)) {
				String bs = "(" + fmtDelta(i, bonus) + ")";
				PartyGfx.plain(g, bs, left - PartyGfx.width(bs, 0.62F), y + 0.5F, 0.62F, BONUS_GREEN);
				left -= PartyGfx.width(bs, 0.62F) + 4;
			}
			if (visible(i, d[i])) {
				String ds = fmtDelta(i, d[i]);
				PartyGfx.plain(g, ds, left - PartyGfx.width(ds, 0.7F), y + 0.5F, 0.7F, PartyGfx.GOLD_LIGHT);
			}

			if (hasBar)
				statBar(g, x, y + 10, RW - 16, 4, b[i], c[i], d[i], BAR_SCALE[i], COLORS[i]);
		}
	}

	private void statBar(GuiGraphics g, int x, int y, int w, int h, double base, double cur, double delta, double scale, int color) {
		double rb = Math.max(0.0, Math.min(1.0, base / scale));
		double rc = Math.max(0.0, Math.min(1.0, cur / scale));
		double rp = Math.max(0.0, Math.min(1.0, (cur + delta) / scale));
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, PartyGfx.BLACK);
		g.fill(x, y, x + w, y + h, 0xFF1B1620);
		int fb = (int) Math.round(Math.min(rb, rc) * w);
		int fc = (int) Math.round(rc * w);
		int fp = (int) Math.round(rp * w);
		if (fb > 0)
			g.fillGradient(x, y, x + fb, y + h, color, PartyGfx.shade(color, 0.5F));
		if (fc > fb)
			g.fillGradient(x + fb, y, x + fc, y + h, BONUS_GREEN, PartyGfx.shade(BONUS_GREEN, 0.5F));
		if (fp > fc) {
			float pulse = 0.55F + 0.45F * (float) Math.sin(System.currentTimeMillis() / 150.0);
			g.fill(x + fc, y, x + fp, y + h, PartyGfx.fade(PartyGfx.GOLD_LIGHT, pulse));
		}
		if (fc > 0)
			g.fill(x, y, x + Math.max(fc, fb), y + 1, 0x55FFFFFF);
	}
}
