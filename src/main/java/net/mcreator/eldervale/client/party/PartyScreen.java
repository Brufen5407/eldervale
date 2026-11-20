package net.mcreator.eldervale.client.party;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;

import net.mcreator.eldervale.party.PartyStatePayload;
import net.mcreator.eldervale.party.PartyClientState;
import net.mcreator.eldervale.party.PartyActionPayload;

/**
 * The Party window. Pages are changed with the arrows (or the left / right keys):
 *   in a party:     MEMBERS - INVITE PLAYER - MANAGE - LEAVE
 *   without party:  CREATE PARTY - INVITES
 * Everything is drawn by hand (same look as FancyClassScreen) and every action is sent to the
 * server with PartyActionPayload; the server answers with the new state, so the window
 * always shows what the server knows. Client only.
 */
public class PartyScreen extends Screen {

	private static final int PW = 400;
	private static final int PH = 236;
	// content box
	private static final int CX = 12;
	private static final int CY = 32;
	private static final int CW = 376;
	private static final int CH = 152;

	private static final int STYLE_NORMAL = 0;
	private static final int STYLE_PRIMARY = 1;
	private static final int STYLE_DANGER = 2;
	private static final int STYLE_SUCCESS = 3;

	private record Hit(int x, int y, int w, int h, boolean enabled, Runnable action) {
	}

	/** Minimal text input, drawn by the screen. */
	private static final class Field {
		final String hint;
		final int max;
		final boolean allowSpace;
		String text = "";
		int x, y, w, h;

		Field(String hint, int max, boolean allowSpace) {
			this.hint = hint;
			this.max = max;
			this.allowSpace = allowSpace;
		}

		boolean type(char c) {
			if (text.length() >= max)
				return false;
			if (!(Character.isLetterOrDigit(c) || c == '_' || (allowSpace && c == ' ')))
				return false;
			text += c;
			return true;
		}

		void backspace() {
			if (!text.isEmpty())
				text = text.substring(0, text.length() - 1);
		}

		void paste(String s) {
			for (char c : s.toCharArray())
				type(c);
		}

		boolean contains(double px, double py) {
			return w > 0 && px >= x && py >= y && px < x + w && py < y + h;
		}
	}

	private final List<Hit> hits = new ArrayList<>();
	private final Field createField = new Field("Party name", 16, true);
	private final Field renameField = new Field("New party name", 16, true);
	private final Field searchField = new Field("Search player...", 16, false);
	private Field focus = null;

	private int page = 0;
	private int dir = 1;
	private float anim = 1.0F;
	private boolean confirmLeave = false;
	private boolean confirmDisband = false;
	private boolean lastInParty;
	private final long openedAt = System.nanoTime();
	private long lastNanos = 0L;
	private int lastHover = -1;
	private double mpx, mpy; // mouse in panel coordinates
	private float curScale = 1.0F;

	public PartyScreen() {
		super(Component.literal("Party"));
		lastInParty = PartyClientState.inParty;
		if (!lastInParty && PartyClientState.hasInvite())
			page = 1; // open straight on the invite
	}

	@Override
	protected void init() {
		super.init();
		PartyClientState.listener = this::onState;
	}

	@Override
	public void removed() {
		PartyClientState.listener = null;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void onState() {
		confirmLeave = false;
		confirmDisband = false;
		if (PartyClientState.inParty != lastInParty) {
			lastInParty = PartyClientState.inParty;
			page = 0;
			anim = 0.0F;
			focus = null;
		}
		page = Math.min(page, pageCount() - 1);
	}

	private int pageCount() {
		return PartyClientState.inParty ? 4 : 2;
	}

	private String pageTitle() {
		if (PartyClientState.inParty)
			return switch (page) {
				case 0 -> "MEMBERS";
				case 1 -> "INVITE PLAYER";
				case 2 -> "MANAGE";
				default -> "LEAVE";
			};
		return page == 0 ? "CREATE PARTY" : "INVITES";
	}

	private void goPage(int d) {
		int n = pageCount();
		page = (page + d + n) % n;
		dir = d;
		anim = 0.0F;
		confirmLeave = false;
		confirmDisband = false;
		focus = null;
		PartyGfx.playUi(SoundEvents.BOOK_PAGE_TURN, d > 0 ? 1.15F : 0.9F, 0.7F);
	}

	private void send(int action, String arg) {
		PacketDistributor.sendToServer(new PartyActionPayload(action, arg));
	}

	// ---------------------------------------------------------------
	//  Scale and mouse
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

		Field clicked = null;
		for (Field f : new Field[]{createField, renameField, searchField})
			if (f.contains(px, py))
				clicked = f;
		if (clicked != null) {
			focus = clicked;
			PartyGfx.playUi(SoundEvents.UI_BUTTON_CLICK, 1.4F, 0.4F);
			return true;
		}
		focus = null;

		for (Hit h : hits) {
			if (h.enabled() && in(px, py, h.x(), h.y(), h.w(), h.h())) {
				PartyGfx.playUi(SoundEvents.UI_BUTTON_CLICK, 1.0F, 0.8F);
				h.action().run();
				return true;
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean charTyped(char c, int mods) {
		if (focus != null) {
			if (focus.type(c))
				PartyGfx.playUi(SoundEvents.UI_BUTTON_CLICK, 1.6F, 0.15F);
			return true;
		}
		return super.charTyped(c, mods);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		if (focus != null) {
			if (key == 256) { // ESC: leave the field first
				focus = null;
				return true;
			}
			if (key == 259) { // backspace
				focus.backspace();
				return true;
			}
			if (key == 257 || key == 335) { // enter
				submit(focus);
				return true;
			}
			if (Screen.hasControlDown() && key == 86) { // ctrl + V
				focus.paste(PartyGfx.mc().keyboardHandler.getClipboard());
				return true;
			}
			return true; // keep other keys (arrows...) inside the field
		}
		if (key == 263) {
			goPage(-1);
			return true;
		}
		if (key == 262) {
			goPage(1);
			return true;
		}
		return super.keyPressed(key, scancode, mods);
	}

	private void submit(Field f) {
		if (f == createField)
			createParty();
		else if (f == renameField)
			renameParty();
		else if (f == searchField) {
			List<PlayerInfo> list = candidates();
			if (list.size() == 1 && PartyClientState.isLeader(PartyGfx.mc().player.getUUID()))
				send(PartyActionPayload.INVITE, list.get(0).getProfile().getName());
		}
	}

	private void createParty() {
		String n = createField.text.trim();
		if (n.length() >= 3) {
			send(PartyActionPayload.CREATE, n);
			createField.text = "";
		}
	}

	private void renameParty() {
		String n = renameField.text.trim();
		if (n.length() >= 3) {
			send(PartyActionPayload.RENAME, n);
			renameField.text = "";
		}
	}

	// ---------------------------------------------------------------
	//  Render
	// ---------------------------------------------------------------
	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		double dt = lastNanos == 0L ? 0.016 : Math.min(0.1, (now - lastNanos) / 1.0E9);
		lastNanos = now;
		anim = Math.min(1.0F, anim + (float) (dt * 4.5));

		updateScale();
		mpx = toPx(mouseX);
		mpy = toPy(mouseY);
		hits.clear();
		createField.w = 0;
		renameField.w = 0;
		searchField.w = 0;

		g.fill(0, 0, this.width, this.height, 0xC0070510);

		g.pose().pushPose();
		g.pose().translate(this.width / 2.0F, this.height / 2.0F, 0.0F);
		g.pose().scale(curScale, curScale, 1.0F);
		g.pose().translate(-PW / 2.0F, -PH / 2.0F, 0.0F);

		PartyGfx.window(g, PW, PH, PartyGfx.PURPLE);
		drawHeader(g);
		PartyGfx.inset(g, CX, CY, CW, CH);
		drawPage(g);

		// soft cross-fade when the page changes
		float f = 1.0F - (1.0F - (float) Math.pow(1.0F - anim, 3.0));
		if (f > 0.02F)
			g.fill(CX + 1, CY + 1, CX + CW - 1, CY + CH - 1, PartyGfx.fade(0xFF1C1722, f * 0.75F));

		drawFooter(g);
		g.pose().popPose();

		PartyToasts.render(g, this.width);

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

	private void drawHeader(GuiGraphics g) {
		PartyGfx.icon(g, PartyGfx.ICON_SHIELD, 14, 8, 2, PartyGfx.PURPLE);
		PartyGfx.outlined(g, "PARTY", 34, 9, 1.3F, PartyGfx.GOLD_LIGHT, PartyGfx.BLACK);
		if (PartyClientState.inParty) {
			String count = PartyClientState.members.size() + "/" + PartyClientState.maxMembers;
			String name = PartyGfx.ellipsize(PartyClientState.partyName, 180.0F, 1.0F);
			PartyGfx.outlinedR(g, count, PW - 16, 10, 1.0F, PartyGfx.GOLD, PartyGfx.BLACK);
			PartyGfx.outlinedR(g, name, PW - 16 - PartyGfx.width(count, 1.0F) - 8, 10, 1.0F, PartyGfx.WHITE, PartyGfx.BLACK);
		}
		g.fill(24, 26, PW - 24, 27, PartyGfx.GOLD_DARK);
		PartyGfx.diamond(g, PW / 2, 26, 3, PartyGfx.GOLD);
	}

	private void drawFooter(GuiGraphics g) {
		int n = pageCount();
		arrow(g, 14, 194, true);
		arrow(g, PW - 36, 194, false);
		PartyGfx.outlinedC(g, pageTitle(), PW / 2.0F, 191, 1.0F, PartyGfx.GOLD_LIGHT, PartyGfx.BLACK);

		int x0 = PW / 2 - (n * 10) / 2 + 5;
		for (int i = 0; i < n; i++) {
			int cx = x0 + i * 10;
			if (i == page) {
				PartyGfx.diamond(g, cx, 208, 4, PartyGfx.FRAME_DARK);
				PartyGfx.diamond(g, cx, 208, 3, PartyGfx.PURPLE);
			} else {
				PartyGfx.diamond(g, cx, 208, 1, PartyGfx.GOLD_DARK);
			}
		}

		PartyGfx.plain(g, "< > change page", 14, 222, 0.6F, PartyGfx.MUTED);
		String esc = "ESC to close";
		PartyGfx.plain(g, esc, PW - 14 - PartyGfx.width(esc, 0.6F), 222, 0.6F, PartyGfx.MUTED);
	}

	private void arrow(GuiGraphics g, int x, int y, boolean left) {
		int w = 22;
		int h = 18;
		hits.add(new Hit(x, y, w, h, true, () -> goPage(left ? -1 : 1)));
		boolean hov = in(mpx, mpy, x, y, w, h);
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, PartyGfx.FRAME_DARK);
		g.fill(x, y, x + w, y + h, hov ? PartyGfx.GOLD_LIGHT : PartyGfx.GOLD);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hov ? 0xFF3A3040 : 0xFF1C1722);
		int cx = x + w / 2;
		int cy = y + h / 2;
		int col = hov ? PartyGfx.WHITE : PartyGfx.GOLD_LIGHT;
		for (int dy = -3; dy <= 3; dy++) {
			int xo = Math.abs(dy);
			if (left)
				g.fill(cx - 2 + xo, cy + dy, cx + xo, cy + dy + 1, col);
			else
				g.fill(cx - xo, cy + dy, cx + 2 - xo, cy + dy + 1, col);
		}
	}

	// ---------------------------------------------------------------
	//  Widgets
	// ---------------------------------------------------------------
	private void button(GuiGraphics g, int x, int y, int w, int h, String label, int style, boolean enabled, Runnable action) {
		hits.add(new Hit(x, y, w, h, enabled, action));
		boolean hov = enabled && in(mpx, mpy, x, y, w, h);
		int base = switch (style) {
			case STYLE_PRIMARY -> PartyGfx.PURPLE;
			case STYLE_DANGER -> PartyGfx.RED;
			case STYLE_SUCCESS -> PartyGfx.GREEN;
			default -> PartyGfx.GOLD;
		};
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, PartyGfx.FRAME_DARK);
		g.fill(x, y, x + w, y + h, enabled ? (hov ? PartyGfx.GOLD_LIGHT : PartyGfx.shade(base, 0.85F)) : 0xFF3A3440);
		int top = enabled ? PartyGfx.shade(base, hov ? 0.85F : 0.5F) : 0xFF2A2630;
		int bottom = enabled ? PartyGfx.shade(base, hov ? 0.5F : 0.28F) : 0xFF1C1822;
		g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, top, bottom);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x44FFFFFF);
		PartyGfx.outlinedC(g, label, x + w / 2.0F, y + (h - 9 * 0.9F) / 2.0F + 0.5F, 0.9F, enabled ? PartyGfx.WHITE : PartyGfx.MUTED, PartyGfx.BLACK);
	}

	private void drawField(GuiGraphics g, Field f, int x, int y, int w, int h) {
		f.x = x;
		f.y = y;
		f.w = w;
		f.h = h;
		boolean foc = focus == f;
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, PartyGfx.FRAME_DARK);
		g.fill(x, y, x + w, y + h, foc ? PartyGfx.PURPLE : PartyGfx.GOLD_DARK);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF15111A);
		float ty = y + (h - 9 * 0.9F) / 2.0F + 0.5F;
		if (f.text.isEmpty() && !foc) {
			PartyGfx.plain(g, f.hint, x + 6, ty, 0.9F, PartyGfx.MUTED);
		} else {
			boolean caret = foc && (System.currentTimeMillis() / 450L) % 2L == 0L;
			PartyGfx.plain(g, f.text + (caret ? "_" : ""), x + 6, ty, 0.9F, PartyGfx.WHITE);
		}
	}

	private void rowBg(GuiGraphics g, int x, int y, int w, int h, int accent, boolean highlight) {
		g.fill(x, y, x + w, y + h, highlight ? 0x509B6BFF : 0x40000000);
		g.fill(x, y, x + 2, y + h, accent);
	}

	private void centerNote(GuiGraphics g, String text, int y) {
		PartyGfx.outlinedC(g, text, PW / 2.0F, y, 0.9F, PartyGfx.MUTED, PartyGfx.BLACK);
	}

	// ---------------------------------------------------------------
	//  Pages
	// ---------------------------------------------------------------
	private void drawPage(GuiGraphics g) {
		if (PartyClientState.inParty) {
			switch (page) {
				case 0 -> memberPage(g);
				case 1 -> invitePage(g);
				case 2 -> managePage(g);
				default -> leavePage(g);
			}
		} else if (page == 0) {
			createPage(g);
		} else {
			invitesPage(g);
		}
	}

	// ---- MEMBERS ----
	private void memberPage(GuiGraphics g) {
		List<PartyStatePayload.MemberInfo> list = PartyClientState.members;
		UUID me = PartyGfx.mc().player.getUUID();
		for (int i = 0; i < PartyClientState.maxMembers; i++) {
			int x = CX + 4;
			int y = CY + 4 + i * 29;
			int w = CW - 8;
			if (i < list.size())
				memberRow(g, list.get(i), me, x, y, w, 26);
			else
				emptySlot(g, x, y, w, 26);
		}
	}

	private void memberRow(GuiGraphics g, PartyStatePayload.MemberInfo m, UUID me, int x, int y, int w, int h) {
		boolean on = m.online();
		boolean leader = m.id().equals(PartyClientState.leader);
		boolean self = m.id().equals(me);
		int cc = PartyGfx.classColor(m.playerClass());

		rowBg(g, x, y, w, h, on ? cc : 0xFF4A4452, self);
		PartyGfx.head(g, m.id(), x + 7, y + 3, 20, !on);

		float nx = x + 34;
		String name = PartyGfx.ellipsize(m.name(), 150.0F, 0.95F);
		PartyGfx.outlined(g, name, nx, y + 3, 0.95F, on ? PartyGfx.WHITE : PartyGfx.MUTED, PartyGfx.BLACK);
		float after = nx + PartyGfx.width(name, 0.95F) + 5.0F;
		if (leader) {
			PartyGfx.icon(g, PartyGfx.ICON_CROWN, (int) after, y + 4, 1, PartyGfx.GOLD_LIGHT);
			after += 14.0F;
		}
		if (self)
			PartyGfx.plain(g, "YOU", after, y + 4.5F, 0.6F, PartyGfx.PURPLE);

		PartyGfx.plain(g, PartyGfx.classLabel(m.playerClass()), nx, y + 15.5F, 0.65F, on ? cc : PartyGfx.MUTED);
		float lvX = nx + PartyGfx.width(PartyGfx.classLabel(m.playerClass()), 0.65F) + 6.0F;
		PartyGfx.plain(g, "Lv " + m.level(), lvX, y + 15.5F, 0.65F, PartyGfx.MUTED);

		// role tag
		String role = leader ? "LEADER" : "MEMBER";
		PartyGfx.outlinedR(g, role, x + w - 8, y + 3, 0.7F, leader ? PartyGfx.GOLD_LIGHT : 0xFF7FC8FF, PartyGfx.BLACK);

		if (!on) {
			PartyGfx.plain(g, "OFFLINE", x + w - 8 - PartyGfx.width("OFFLINE", 0.65F), y + 15.5F, 0.65F, PartyGfx.MUTED);
			return;
		}
		// distance, hp bar, mana bar
		int barW = 84;
		int bx = x + w - 8 - barW;
		if (m.distance() >= 0.0F && !self) {
			String d = (int) m.distance() + " m";
			PartyGfx.plain(g, d, bx - 6 - PartyGfx.width(d, 0.6F), y + 14.0F, 0.6F, PartyGfx.MUTED);
		}
		float hp = m.hpMax() <= 0 ? 0 : Math.max(0.0F, Math.min(1.0F, m.hp() / m.hpMax()));
		float mana = m.manaMax() <= 0 ? 0 : Math.max(0.0F, Math.min(1.0F, m.mana() / m.manaMax()));
		PartyGfx.bar(g, bx, y + 14, barW, 5, hp, 0xFFE04848, 0xFF8A1F1F);
		PartyGfx.bar(g, bx, y + 20, barW, 3, mana, 0xFF4AA3FF, 0xFF1F4F8A);
	}

	private void emptySlot(GuiGraphics g, int x, int y, int w, int h) {
		int c = 0x30C9A24A;
		g.fill(x, y, x + w, y + 1, c);
		g.fill(x, y + h - 1, x + w, y + h, c);
		g.fill(x, y, x + 1, y + h, c);
		g.fill(x + w - 1, y, x + w, y + h, c);
		PartyGfx.icon(g, PartyGfx.ICON_PLUS, x + 12, y + 9, 1, 0xFF5A5468);
		PartyGfx.plain(g, "Free slot", x + 34, y + 9, 0.8F, 0xFF5A5468);
	}

	// ---- INVITE PLAYER ----
	private List<PlayerInfo> candidates() {
		List<PlayerInfo> list = new ArrayList<>();
		ClientPacketListener conn = PartyGfx.mc().getConnection();
		if (conn == null || PartyGfx.mc().player == null)
			return list;
		UUID me = PartyGfx.mc().player.getUUID();
		String filter = searchField.text.toLowerCase(Locale.ROOT);
		for (PlayerInfo info : conn.getOnlinePlayers()) {
			UUID id = info.getProfile().getId();
			if (id.equals(me))
				continue;
			boolean inParty = false;
			for (PartyStatePayload.MemberInfo m : PartyClientState.members)
				if (m.id().equals(id))
					inParty = true;
			if (inParty)
				continue;
			if (!filter.isEmpty() && !info.getProfile().getName().toLowerCase(Locale.ROOT).contains(filter))
				continue;
			list.add(info);
		}
		list.sort(Comparator.comparing((PlayerInfo p) -> p.getProfile().getName().toLowerCase(Locale.ROOT)));
		return list;
	}

	private void invitePage(GuiGraphics g) {
		UUID me = PartyGfx.mc().player.getUUID();
		if (!PartyClientState.isLeader(me)) {
			centerNote(g, "Only the leader can invite players.", CY + 66);
			return;
		}
		boolean full = PartyClientState.members.size() >= PartyClientState.maxMembers;
		drawField(g, searchField, CX + 8, CY + 8, 250, 20);

		List<PlayerInfo> list = candidates();
		String count = list.size() + " online";
		PartyGfx.plain(g, count, CX + CW - 10 - PartyGfx.width(count, 0.75F), CY + 14, 0.75F, PartyGfx.MUTED);
		if (full)
			PartyGfx.plain(g, "The party is full.", CX + 10, CY + 32, 0.7F, PartyGfx.RED);

		if (list.isEmpty()) {
			centerNote(g, "No players found.", CY + 80);
			return;
		}
		int rows = Math.min(4, list.size());
		for (int i = 0; i < rows; i++) {
			PlayerInfo info = list.get(i);
			String name = info.getProfile().getName();
			int x = CX + 4;
			int y = CY + 42 + i * 27;
			int w = CW - 8;
			rowBg(g, x, y, w, 24, PartyGfx.GREEN, false);
			PartyGfx.head(g, info.getProfile().getId(), x + 7, y + 3, 18, false);
			PartyGfx.outlined(g, PartyGfx.ellipsize(name, 170.0F, 0.95F), x + 32, y + 3, 0.95F, PartyGfx.WHITE, PartyGfx.BLACK);
			PartyGfx.plain(g, "ONLINE", x + 32, y + 14.5F, 0.6F, PartyGfx.GREEN);
			button(g, x + w - 70, y + 3, 62, 18, full ? "Full" : "Invite", STYLE_PRIMARY, !full, () -> send(PartyActionPayload.INVITE, name));
		}
		if (list.size() > rows)
			PartyGfx.plain(g, "+" + (list.size() - rows) + " more: type to narrow the list", CX + 10, CY + CH - 10, 0.65F, PartyGfx.MUTED);
	}

	// ---- MANAGE ----
	private void managePage(GuiGraphics g) {
		UUID me = PartyGfx.mc().player.getUUID();
		if (!PartyClientState.isLeader(me)) {
			String leaderName = "?";
			for (PartyStatePayload.MemberInfo m : PartyClientState.members)
				if (m.id().equals(PartyClientState.leader))
					leaderName = m.name();
			PartyGfx.icon(g, PartyGfx.ICON_CROWN, PW / 2 - 18, CY + 26, 4, PartyGfx.GOLD_LIGHT);
			PartyGfx.outlinedC(g, PartyClientState.partyName, PW / 2.0F, CY + 58, 1.3F, PartyGfx.WHITE, PartyGfx.BLACK);
			PartyGfx.outlinedC(g, "Leader: " + leaderName, PW / 2.0F, CY + 76, 0.9F, PartyGfx.GOLD_LIGHT, PartyGfx.BLACK);
			centerNote(g, "Only the leader can manage the party.", CY + 100);
			return;
		}

		drawField(g, renameField, CX + 8, CY + 8, 250, 20);
		String newName = renameField.text.trim();
		button(g, CX + 266, CY + 9, 98, 18, "Rename", STYLE_PRIMARY, newName.length() >= 3, this::renameParty);

		int row = 0;
		for (PartyStatePayload.MemberInfo m : PartyClientState.members) {
			if (m.id().equals(me))
				continue;
			int x = CX + 4;
			int y = CY + 36 + row * 28;
			int w = CW - 8;
			if (row >= 4)
				break;
			rowBg(g, x, y, w, 25, m.online() ? PartyGfx.classColor(m.playerClass()) : 0xFF4A4452, false);
			PartyGfx.head(g, m.id(), x + 7, y + 3, 19, !m.online());
			PartyGfx.outlined(g, PartyGfx.ellipsize(m.name(), 140.0F, 0.95F), x + 33, y + 3, 0.95F, m.online() ? PartyGfx.WHITE : PartyGfx.MUTED, PartyGfx.BLACK);
			PartyGfx.plain(g, PartyGfx.classLabel(m.playerClass()) + "  Lv " + m.level(), x + 33, y + 15, 0.6F, PartyGfx.MUTED);
			String name = m.name();
			button(g, x + w - 140, y + 3, 70, 19, "Leader", STYLE_NORMAL, true, () -> send(PartyActionPayload.LEADER, name));
			button(g, x + w - 66, y + 3, 58, 19, "Remove", STYLE_DANGER, true, () -> send(PartyActionPayload.KICK, name));
			row++;
		}
		if (row == 0)
			centerNote(g, "No other members yet. Invite players from the previous page.", CY + 80);
	}

	// ---- LEAVE ----
	private void leavePage(GuiGraphics g) {
		UUID me = PartyGfx.mc().player.getUUID();
		boolean leader = PartyClientState.isLeader(me);
		PartyGfx.icon(g, PartyGfx.ICON_CROSS, PW / 2 - 14, CY + 14, 4, PartyGfx.shade(PartyGfx.RED, 0.8F));
		PartyGfx.outlinedC(g, "Leave the party?", PW / 2.0F, CY + 50, 1.2F, PartyGfx.GOLD_LIGHT, PartyGfx.BLACK);

		String note = "You can be invited again by the leader.";
		if (leader && PartyClientState.members.size() > 1) {
			String next = "?";
			for (PartyStatePayload.MemberInfo m : PartyClientState.members)
				if (!m.id().equals(me)) {
					next = m.name();
					break;
				}
			note = "You are the leader: leadership will pass to " + next + ".";
		} else if (leader) {
			note = "You are alone: the party will be disbanded.";
		}
		PartyGfx.plain(g, note, PW / 2.0F - PartyGfx.width(note, 0.75F) / 2.0F, CY + 68, 0.75F, PartyGfx.TEXT);

		button(g, PW / 2 - 80, CY + 86, 160, 22, confirmLeave ? "Click again to confirm" : "Leave party", STYLE_DANGER, true, () -> {
			if (!confirmLeave)
				confirmLeave = true;
			else
				send(PartyActionPayload.LEAVE, "");
		});
		if (leader && PartyClientState.members.size() > 1)
			button(g, PW / 2 - 80, CY + 114, 160, 22, confirmDisband ? "Click again to confirm" : "Disband party", STYLE_NORMAL, true, () -> {
				if (!confirmDisband)
					confirmDisband = true;
				else
					send(PartyActionPayload.DISBAND, "");
			});
	}

	// ---- CREATE PARTY (no party) ----
	private void createPage(GuiGraphics g) {
		PartyGfx.icon(g, PartyGfx.ICON_SHIELD, PW / 2 - 17, CY + 8, 5, PartyGfx.PURPLE);
		PartyGfx.outlinedC(g, "Create your party", PW / 2.0F, CY + 54, 1.2F, PartyGfx.GOLD_LIGHT, PartyGfx.BLACK);
		String sub = "Up to " + PartyClientState.maxMembers + " players. Only the leader can invite.";
		PartyGfx.plain(g, sub, PW / 2.0F - PartyGfx.width(sub, 0.75F) / 2.0F, CY + 70, 0.75F, PartyGfx.TEXT);

		drawField(g, createField, PW / 2 - 100, CY + 86, 200, 22);
		button(g, PW / 2 - 50, CY + 114, 100, 22, "Create", STYLE_SUCCESS, createField.text.trim().length() >= 3, this::createParty);

		String hint = "3 to 16 characters: letters, numbers, spaces.";
		PartyGfx.plain(g, hint, PW / 2.0F - PartyGfx.width(hint, 0.6F) / 2.0F, CY + 142, 0.6F, PartyGfx.MUTED);
	}

	// ---- INVITES (no party) ----
	private void invitesPage(GuiGraphics g) {
		if (!PartyClientState.hasInvite()) {
			PartyGfx.icon(g, PartyGfx.ICON_MAIL, PW / 2 - 18, CY + 30, 4, 0xFF5A5468);
			centerNote(g, "No pending invites.", CY + 66);
			String hint = "When a leader invites you, a notification appears here.";
			PartyGfx.plain(g, hint, PW / 2.0F - PartyGfx.width(hint, 0.65F) / 2.0F, CY + 82, 0.65F, PartyGfx.MUTED);
			return;
		}
		PartyGfx.inset(g, PW / 2 - 130, CY + 8, 260, 134);
		PartyGfx.icon(g, PartyGfx.ICON_MAIL, PW / 2 - 13, CY + 16, 3, PartyGfx.GOLD_LIGHT);
		PartyGfx.outlinedC(g, PartyClientState.inviteFrom + " invited you to", PW / 2.0F, CY + 46, 0.85F, PartyGfx.TEXT, PartyGfx.BLACK);
		PartyGfx.outlinedC(g, PartyGfx.ellipsize(PartyClientState.inviteParty, 230.0F, 1.2F), PW / 2.0F, CY + 60, 1.2F, PartyGfx.GOLD_LIGHT, PartyGfx.BLACK);

		long leftMs = Math.max(0L, PartyClientState.inviteDeadlineMs - System.currentTimeMillis());
		PartyGfx.bar(g, PW / 2 - 100, CY + 84, 200, 5, leftMs / 60000.0, PartyGfx.PURPLE, PartyGfx.shade(PartyGfx.PURPLE, 0.5F));
		String left = "Expires in " + (leftMs / 1000L + 1L) + "s";
		PartyGfx.plain(g, left, PW / 2.0F - PartyGfx.width(left, 0.65F) / 2.0F, CY + 94, 0.65F, PartyGfx.MUTED);

		button(g, PW / 2 - 106, CY + 110, 100, 22, "Accept", STYLE_SUCCESS, true, () -> send(PartyActionPayload.ACCEPT, ""));
		button(g, PW / 2 + 6, CY + 110, 100, 22, "Decline", STYLE_DANGER, true, () -> send(PartyActionPayload.DECLINE, ""));
	}
}
