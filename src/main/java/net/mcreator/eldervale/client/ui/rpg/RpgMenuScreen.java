package net.mcreator.eldervale.client.ui.rpg;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;

import net.mcreator.eldervale.client.party.PartyGfx;
import net.mcreator.eldervale.client.stats.AttributesScreen;
import net.mcreator.eldervale.client.ui.kit.UiKit;
import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.stats.AttributeSpendPayload;
import net.mcreator.eldervale.stats.ClassProfile;
import net.mcreator.eldervale.stats.ClientStatsState;
import net.mcreator.eldervale.stats.StatAttribute;
import net.mcreator.eldervale.stats.StatsConfig;

/**
 * Eldervale RPG menu (key I, or from the pause menu).
 * Left: section rail. Right: the selected section.
 * Functional: Character, Equipment (read-only view of the equipped vanilla slots), Inventory (opens the vanilla inventory),
 * Skills, Status. Placeholders for now (no function): Quests, Map, Settings.
 * Every number is read from the existing systems (PlayerVariables, ClassProfile, ClientStatsState); nothing is simulated.
 * The client never changes stats. Client only.
 */
public class RpgMenuScreen extends Screen {

	public enum Tab {
		CHARACTER("Character"), EQUIPMENT("Equipment"), INVENTORY("Inventory"), SKILLS("Skills"), STATUS("Status"), QUESTS("Quests"), MAP("Map"), SETTINGS("Settings");

		public final String label;

		Tab(String label) {
			this.label = label;
		}

		public boolean placeholder() {
			return this == QUESTS || this == MAP || this == SETTINGS;
		}
	}

	// ---- layout (logical units, the whole window is scaled) ----
	private static final int PW = 420, PH = 264;
	private static final int TAB_X = 14, TAB_Y = 34, TAB = 24, TAB_GAP = 2;
	private static final int CX = 48, CY = 34, CW = PW - 14 - 48, CH = 206;

	// ---- colours (muted; gold is for accents only) ----
	private static final int RULE = 0xFF4A3E2A;
	private static final int HP_COLOR = 0xFFA8403A;
	private static final int MANA_COLOR = 0xFF3F6FA8;
	private static final int XP_COLOR = 0xFFB08D4A;
	private static final int GOOD = 0xFF7FBF82;
	private static final int BAD = 0xFFC9605A;
	private static final int DIM = UiKit.TEXT_DIM;
	private static final int WHITE = 0xFFEDE6D6;

	private record Hit(int x, int y, int w, int h, Runnable action) {
	}

	private record EqSlot(String label, EquipmentSlot slot) {
	}

	private static final EqSlot[] EQUIPMENT = {new EqSlot("Weapon", EquipmentSlot.MAINHAND), new EqSlot("Offhand", EquipmentSlot.OFFHAND), new EqSlot("Head", EquipmentSlot.HEAD), new EqSlot("Chest", EquipmentSlot.CHEST), new EqSlot("Legs", EquipmentSlot.LEGS), new EqSlot("Feet", EquipmentSlot.FEET)};

	private static final String[] ATTR_NAMES = {"Strength", "Vitality", "Defense", "Agility", "Intelligence", "Dexterity"};
	private static final String[] STAT_LABELS = {"HP", "DEF", "ATT SPEED", "ATT", "MANA", "MANA REGEN", "SPEED", "CRIT CHANCE", "CRIT DAMAGE"};

	private final Screen parent;
	private Tab tab;
	private final List<Hit> hits = new ArrayList<>();
	private final long openedAt = System.nanoTime();
	private float curScale = 1.0F;
	private double mpx, mpy;
	private int lastHover = -1;
	private Component tooltip;
	private ItemStack tooltipStack = ItemStack.EMPTY;

	public RpgMenuScreen(Screen parent, Tab tab) {
		super(Component.literal("Eldervale"));
		this.parent = parent;
		this.tab = tab;
	}

	@Override
	protected void init() {
		// ask the server for the current attribute points ("just send me my points", same request as the K key)
		PacketDistributor.sendToServer(new AttributeSpendPayload(-1, 0));
	}

	@Override
	public boolean isPauseScreen() {
		return parent != null && parent.isPauseScreen();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	// ---------------------------------------------------------------
	//  Scale and input
	// ---------------------------------------------------------------
	private void updateScale() {
		float fit = Math.min((this.width - 16.0F) / PW, (this.height - 16.0F) / PH);
		float base = fit >= 1.0F ? Math.min((float) Math.floor(fit), 3.0F) : Math.max(0.4F, fit);
		double open = Math.min(1.0, (System.nanoTime() - openedAt) / 1.0E9 / 0.18);
		float ease = (float) (1.0 - Math.pow(1.0 - open, 3.0));
		curScale = base * (0.95F + 0.05F * ease);
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

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 0) {
			updateScale();
			double px = toPx(mx), py = toPy(my);
			for (Hit h : hits) {
				if (in(px, py, h.x(), h.y(), h.w(), h.h())) {
					h.action().run();
					return true;
				}
			}
		}
		return super.mouseClicked(mx, my, button);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		if (RpgMenuClientEvents.OPEN.matches(key, scancode)) {
			this.onClose();
			return true;
		}
		if (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_8) {
			select(Tab.values()[key - GLFW.GLFW_KEY_1]);
			return true;
		}
		if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) {
			int n = Tab.values().length;
			select(Tab.values()[(tab.ordinal() + (key == GLFW.GLFW_KEY_DOWN ? 1 : n - 1)) % n]);
			return true;
		}
		return super.keyPressed(key, scancode, mods);
	}

	private void select(Tab t) {
		if (t != tab) {
			tab = t;
			PartyGfx.playUi(SoundEvents.UI_BUTTON_CLICK, 1.2F, 0.6F);
		}
	}

	// ---------------------------------------------------------------
	//  Render
	// ---------------------------------------------------------------
	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, this.width, this.height, 0xB00A0810);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		updateScale();
		mpx = toPx(mouseX);
		mpy = toPy(mouseY);
		hits.clear();
		tooltip = null;
		tooltipStack = ItemStack.EMPTY;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		EldervaleModVariables.PlayerVariables vars = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		int accent = vars.PLAYER_CLASS == null || vars.PLAYER_CLASS.isEmpty() ? UiKit.GOLD : PartyGfx.classColor(vars.PLAYER_CLASS);

		renderBackground(g, mouseX, mouseY, partialTick);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		g.pose().pushPose();
		g.pose().translate(this.width / 2.0F, this.height / 2.0F, 0.0F);
		g.pose().scale(curScale, curScale, 1.0F);
		g.pose().translate(-PW / 2.0F, -PH / 2.0F, 0.0F);

		UiKit.nineSlice(g, UiKit.PANEL, 0, 0, PW, PH, 0, 0, 32, 32, 32, 32, 8);
		drawHeader(g, player, vars, accent);
		drawRail(g);

		UiKit.nineSlice(g, UiKit.CARD, CX, CY, CW, CH, 0, 0, 32, 32, 32, 64, 4);
		g.drawString(this.font, UiKit.title(tab.label), CX + 8, CY + 8, UiKit.TEXT_HOVER, true);
		g.fill(CX + 8, CY + 21, CX + CW - 8, CY + 22, RULE);

		int ix = CX + 8, iy = CY + 28, iw = CW - 16, ih = CH - 36;
		switch (tab) {
			case CHARACTER -> drawCharacter(g, player, vars, ix, iy, iw, ih);
			case EQUIPMENT -> drawEquipment(g, player, ix, iy, iw, ih);
			case INVENTORY -> drawInventory(g, player, ix, iy, iw, ih);
			case SKILLS -> drawSkills(g, vars, ix, iy, iw, ih);
			case STATUS -> drawStatus(g, vars, ix, iy, iw, ih);
			default -> drawPlaceholder(g, tab, ix, iy, iw, ih);
		}

		String hint = "1-8 / Up / Down: section     " + RpgMenuClientEvents.OPEN.getTranslatedKeyMessage().getString() + " / ESC: close";
		g.drawString(this.font, hint, PW - 14 - this.font.width(hint), PH - 17, DIM, false);

		g.pose().popPose();

		// tooltips are drawn unscaled so they stay inside the real screen
		if (!tooltipStack.isEmpty())
			g.renderTooltip(this.font, tooltipStack, mouseX, mouseY);
		else if (tooltip != null)
			g.renderTooltip(this.font, tooltip, mouseX, mouseY);

		// soft chime when the mouse moves onto a clickable area
		int hover = -1;
		for (int i = 0; i < hits.size(); i++) {
			Hit h = hits.get(i);
			if (in(mpx, mpy, h.x(), h.y(), h.w(), h.h()))
				hover = i;
		}
		if (hover != lastHover) {
			if (hover >= 0)
				PartyGfx.playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 1.8F, 0.15F);
			lastHover = hover;
		}
	}

	private void drawHeader(GuiGraphics g, Player player, EldervaleModVariables.PlayerVariables vars, int accent) {
		String name = player.getGameProfile().getName();
		g.drawString(this.font, UiKit.title(name), 14, 12, UiKit.TEXT, true);
		Component lv = UiKit.title("Lv " + (int) Math.round(vars.LEVEL));
		Component cls = UiKit.title(PartyGfx.classLabel(vars.PLAYER_CLASS));
		int x = PW - 14;
		x -= this.font.width(lv);
		g.drawString(this.font, lv, x, 12, UiKit.GOLD, true);
		x -= 10 + this.font.width(cls);
		g.drawString(this.font, cls, x, 12, accent, true);
		g.fill(14, 27, PW - 14, 28, RULE);
	}

	private void drawRail(GuiGraphics g) {
		for (Tab t : Tab.values()) {
			int i = t.ordinal();
			int x = TAB_X, y = TAB_Y + i * (TAB + TAB_GAP);
			boolean sel = t == tab;
			boolean hov = in(mpx, mpy, x, y, TAB, TAB);
			hits.add(new Hit(x, y, TAB, TAB, () -> select(t)));
			UiKit.drawSectionTab(g, x, y, sel || hov ? 1 : (t.placeholder() ? 2 : 0), i, sel || hov);
			if (hov)
				tooltip = Component.literal(t.label + (t.placeholder() ? " (coming soon)" : ""));
		}
	}

	// ---------------------------------------------------------------
	//  Shared drawing
	// ---------------------------------------------------------------
	private void drawModel(GuiGraphics g, int x, int y, int w, int h) {
		UiKit.nineSlice(g, UiKit.SLOT, x, y, w, h, 0, 0, 18, 18, 18, 18, 2);
		InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 2, y + 2, x + w - 2, y + h - 2, 70, 0.0625F, (float) mpx, (float) mpy, Minecraft.getInstance().player);
	}

	private void bar(GuiGraphics g, int x, int y, int w, String label, String value, double ratio, int color) {
		g.drawString(this.font, label, x, y, DIM, false);
		g.drawString(this.font, value, x + w - this.font.width(value), y, WHITE, false);
		int by = y + 10, bh = 6;
		g.fill(x - 1, by - 1, x + w + 1, by + bh + 1, 0xFF0A090D);
		g.fill(x, by, x + w, by + bh, 0xFF15141B);
		int fw = (int) Math.round(Math.max(0.0, Math.min(1.0, ratio)) * w);
		if (fw > 0) {
			g.fill(x, by, x + fw, by + bh, color);
			g.fill(x, by, x + fw, by + 1, UiKit.lerpColor(color & 0xFFFFFF, 0xFFFFFF, 0.25F) | 0xFF000000);
		}
	}

	private void statRow(GuiGraphics g, int x, int y, int w, String label, String value, int valueColor) {
		g.drawString(this.font, label, x, y, DIM, false);
		g.drawString(this.font, value, x + w - this.font.width(value), y, valueColor, false);
	}

	private void notClassYet(GuiGraphics g, int ix, int iy, int iw) {
		String t = "Choose a class first.";
		g.drawString(this.font, t, ix + (iw - this.font.width(t)) / 2, iy + 60, DIM, false);
	}

	// ---------------------------------------------------------------
	//  Character
	// ---------------------------------------------------------------
	private void drawCharacter(GuiGraphics g, Player player, EldervaleModVariables.PlayerVariables v, int ix, int iy, int iw, int ih) {
		drawModel(g, ix, iy, 104, ih);
		int rx = ix + 116, rw = iw - 116;

		g.drawString(this.font, player.getGameProfile().getName(), rx, iy, WHITE, true);
		String cls = PartyGfx.classLabel(v.PLAYER_CLASS) + "  -  Level " + (int) Math.round(v.LEVEL);
		g.drawString(this.font, cls, rx, iy + 11, v.PLAYER_CLASS == null || v.PLAYER_CLASS.isEmpty() ? DIM : PartyGfx.classColor(v.PLAYER_CLASS), false);

		int y = iy + 28;
		double xpNext = Math.max(1.0, v.XP_NEXT_LEVEL);
		bar(g, rx, y, rw, "EXP", RpgFormat.num(v.XP, 0, 0) + " / " + RpgFormat.num(v.XP_NEXT_LEVEL, 0, 0), v.XP / xpNext, XP_COLOR);
		y += 22;
		float hp = player.getHealth(), maxHp = player.getMaxHealth();
		bar(g, rx, y, rw, "HP", RpgFormat.num(hp, 1, 0) + " / " + RpgFormat.num(maxHp, 1, 0), maxHp > 0 ? hp / maxHp : 0.0, HP_COLOR);
		y += 22;
		bar(g, rx, y, rw, "MANA", RpgFormat.num(v.MANA, 0, 0) + " / " + RpgFormat.num(v.MANA_MAX, 0, 0), v.MANA_MAX > 0 ? v.MANA / v.MANA_MAX : 0.0, MANA_COLOR);
		y += 28;

		int colW = (rw - 14) / 2;
		int c2 = rx + colW + 14;
		statRow(g, rx, y, colW, "ATT", RpgFormat.num(v.ATTACK, 2, 1), WHITE);
		statRow(g, c2, y, colW, "DEF", RpgFormat.num(v.DEFENCE * 100.0, 1, 0), WHITE);
		y += 13;
		statRow(g, rx, y, colW, "ATT SPEED", RpgFormat.num(v.ATTACK_SPEED, 2, 1), WHITE);
		statRow(g, c2, y, colW, "SPEED", RpgFormat.num(v.SPEED, 3, 2), WHITE);
		y += 13;
		statRow(g, rx, y, colW, "MANA REGEN", RpgFormat.num(v.MANA_REGEN, 1, 0) + "/s", WHITE);
		statRow(g, c2, y, colW, "CRIT CHANCE", RpgFormat.num(v.CRITICAL_CHANCE, 1, 1) + "%", WHITE);
		y += 13;
		statRow(g, rx, y, colW, "CRIT DAMAGE", "x" + RpgFormat.num(v.CRITICAL_DAMAGE / 100.0, 2, 2), WHITE);
		int free = (int) Math.floor(v.STAT_POINT);
		statRow(g, c2, y, colW, "FREE POINTS", String.valueOf(free), free > 0 ? UiKit.TEXT_HOVER : DIM);
	}

	// ---------------------------------------------------------------
	//  Equipment (read-only: items are changed in the inventory)
	// ---------------------------------------------------------------
	private void drawEquipment(GuiGraphics g, Player player, int ix, int iy, int iw, int ih) {
		drawModel(g, ix, iy, 104, ih);
		int rx = ix + 116;
		int y = iy;
		for (EqSlot e : EQUIPMENT) {
			ItemStack stack = player.getItemBySlot(e.slot());
			UiKit.nineSlice(g, UiKit.SLOT, rx, y, 18, 18, 0, 0, 18, 18, 18, 18, 2);
			if (!stack.isEmpty()) {
				g.renderItem(stack, rx + 1, y + 1);
				g.renderItemDecorations(this.font, stack, rx + 1, y + 1);
				if (in(mpx, mpy, rx, y, 18, 18))
					tooltipStack = stack;
			}
			g.drawString(this.font, e.label(), rx + 26, y, DIM, false);
			String name = stack.isEmpty() ? "Empty" : this.font.plainSubstrByWidth(stack.getHoverName().getString(), iw - 116 - 30);
			g.drawString(this.font, name, rx + 26, y + 9, stack.isEmpty() ? DIM : WHITE, false);
			y += 22;
		}
		y += 4;
		g.drawString(this.font, "Accessories", rx, y, DIM, false);
		for (int i = 0; i < 3; i++) {
			UiKit.nineSlice(g, UiKit.SLOT, rx + i * 22, y + 11, 18, 18, 0, 0, 18, 18, 18, 18, 2);
		}
		g.drawString(this.font, "Coming soon", rx + 3 * 22 + 6, y + 16, DIM, false);
	}

	// ---------------------------------------------------------------
	//  Inventory
	// ---------------------------------------------------------------
	private void drawInventory(GuiGraphics g, Player player, int ix, int iy, int iw, int ih) {
		int used = 0;
		for (ItemStack s : player.getInventory().items)
			if (!s.isEmpty())
				used++;
		g.drawString(this.font, "Slots used", ix, iy, DIM, false);
		String u = used + " / " + player.getInventory().items.size();
		g.drawString(this.font, u, ix + 70, iy, WHITE, false);

		int bw = 150, bh = 22, bx = ix, by = iy + 22;
		Runnable open = () -> this.minecraft.setScreen(new InventoryScreen(this.minecraft.player));
		hits.add(new Hit(bx, by, bw, bh, open));
		boolean hov = in(mpx, mpy, bx, by, bw, bh);
		UiKit.nineSlice(g, UiKit.BUTTON, bx, by, bw, bh, 0, hov ? 20 : 0, 48, 20, 48, 60, 6);
		Component label = UiKit.title("Open inventory");
		g.drawString(this.font, label, bx + (bw - this.font.width(label)) / 2, by + 7, hov ? UiKit.TEXT_HOVER : UiKit.TEXT, true);

		g.drawString(this.font, "The Eldervale inventory is planned. For now this opens the standard one.", ix, by + 36, DIM, false);
	}

	// ---------------------------------------------------------------
	//  Skills
	// ---------------------------------------------------------------
	private void drawSkills(GuiGraphics g, EldervaleModVariables.PlayerVariables v, int ix, int iy, int iw, int ih) {
		ClassCatalog.Info info = ClassCatalog.get(v.PLAYER_CLASS);
		if (info == null) {
			notClassYet(g, ix, iy, iw);
			return;
		}
		String[] keys = {"V", "B", ""};
		int rowH = 56;
		for (int i = 0; i < info.skills().length; i++) {
			ClassCatalog.Skill s = info.skills()[i];
			int y = iy + i * rowH;

			UiKit.nineSlice(g, UiKit.SLOT, ix, y + 2, 32, 32, 0, 0, 18, 18, 18, 18, 2);
			if (s.iconTexture() != null) {
				g.blit(s.iconTexture(), ix + 3, y + 5, 0.0F, 0.0F, 26, 26, 26, 26);
			} else {
				g.blit(UiKit.RPG_ICONS, ix + 8, y + 10, 3 * 16.0F, 0.0F, 16, 16, 128, 32);
			}

			int tx = ix + 42, tw = iw - 42;
			Component name = UiKit.title(s.name());
			g.drawString(this.font, name, tx, y + 2, WHITE, true);

			// state tag (right aligned)
			String state;
			int stateColor;
			if (s.passive()) {
				state = "PASSIVE";
				stateColor = DIM;
			} else {
				double cd = i == 0 ? v.SKILL1_CD : v.SKILL2_CD;
				if (cd > 0.5) {
					state = "COOLDOWN " + RpgFormat.num(cd / 20.0, 1, 1) + "s";
					stateColor = UiKit.GOLD;
				} else if (v.MANA < s.mana()) {
					state = "NOT ENOUGH MANA";
					stateColor = BAD;
				} else {
					state = "READY";
					stateColor = GOOD;
				}
			}
			g.drawString(this.font, state, tx + tw - this.font.width(state), y + 3, stateColor, false);

			String meta = s.kind() + (keys[i].isEmpty() ? "" : " [" + keys[i] + "]");
			if (!s.passive())
				meta += "   -   " + s.mana() + " mana   -   CD " + s.cooldown() + "s";
			g.drawString(this.font, meta, tx, y + 14, s.passive() ? DIM : 0xFF7FA8D8, false);

			List<FormattedCharSequence> lines = this.font.split(Component.literal(s.desc()), tw);
			int n = Math.min(3, lines.size());
			for (int l = 0; l < n; l++) {
				FormattedCharSequence line = lines.get(l);
				g.drawString(this.font, line, tx, y + 26 + l * 10, 0xFFC9C2B4, false);
			}
			if (i < info.skills().length - 1)
				g.fill(ix, y + rowH - 3, ix + iw, y + rowH - 2, 0x30C9A45A);
		}
	}

	// ---------------------------------------------------------------
	//  Status
	// ---------------------------------------------------------------
	private void drawStatus(GuiGraphics g, EldervaleModVariables.PlayerVariables v, int ix, int iy, int iw, int ih) {
		ClassProfile profile = ClassProfile.get(v.PLAYER_CLASS);
		if (profile == null) {
			notClassYet(g, ix, iy, iw);
			return;
		}
		int free = (int) Math.floor(v.STAT_POINT);
		int lw = 150;

		g.drawString(this.font, "ATTRIBUTES", ix, iy, DIM, false);
		String f = free + " free";
		g.drawString(this.font, f, ix + lw - this.font.width(f), iy, free > 0 ? UiKit.TEXT_HOVER : DIM, false);
		StatAttribute[] attrs = StatAttribute.values();
		for (int i = 0; i < attrs.length; i++) {
			StatAttribute a = attrs[i];
			int y = iy + 14 + i * 16;
			int pts = ClientStatsState.get(a);
			double mult = profile.multiplier(a);
			boolean main = mult >= 1.25;
			g.fill(ix, y - 1, ix + 2, y + 10, main ? PartyGfx.classColor(v.PLAYER_CLASS) : 0xFF4A4452);
			g.drawString(this.font, ATTR_NAMES[a.ordinal()], ix + 8, y, WHITE, false);
			g.drawString(this.font, "x" + RpgFormat.num(mult, 2, 1), ix + 84, y, main ? PartyGfx.classColor(v.PLAYER_CLASS) : DIM, false);
			String p = pts >= StatsConfig.MAX_POINTS_PER_ATTRIBUTE ? "MAX" : String.valueOf(pts);
			g.drawString(this.font, p, ix + lw - this.font.width(p), y, pts > 0 ? GOOD : DIM, false);
		}

		int bw = lw, bh = 22, bx = ix, by = iy + ih - bh - 2;
		Runnable open = () -> {
			PacketDistributor.sendToServer(new AttributeSpendPayload(-1, 0));
			this.minecraft.setScreen(new AttributesScreen());
		};
		hits.add(new Hit(bx, by, bw, bh, open));
		boolean hov = in(mpx, mpy, bx, by, bw, bh);
		UiKit.nineSlice(g, UiKit.BUTTON, bx, by, bw, bh, 0, hov ? 20 : 0, 48, 20, 48, 60, 6);
		Component label = UiKit.title("Spend points");
		g.drawString(this.font, label, bx + (bw - this.font.width(label)) / 2, by + 7, hov ? UiKit.TEXT_HOVER : UiKit.TEXT, true);

		// stats: base -> current (+ bonus from attributes / equipment)
		int rx = ix + lw + 24, rw = iw - lw - 24;
		g.drawString(this.font, "STATS", rx, iy, DIM, false);
		double[] base = {profile.hp(), profile.def() * 100.0, profile.attSpeed(), profile.att(), profile.mana(), profile.manaRegen(), profile.speed(), StatsConfig.BASE_CRIT_CHANCE, StatsConfig.BASE_CRIT_DAMAGE / 100.0};
		double[] cur = {v.HEALT_MAX, v.DEFENCE * 100.0, v.ATTACK_SPEED, v.ATTACK, v.MANA_MAX, v.MANA_REGEN, v.SPEED, v.CRITICAL_CHANCE, v.CRITICAL_DAMAGE / 100.0};
		int[] dec = {1, 1, 2, 2, 1, 1, 3, 1, 2};
		int[] minDec = {0, 0, 1, 1, 0, 0, 2, 1, 2};
		for (int i = 0; i < 9; i++) {
			int y = iy + 14 + i * 14;
			String value = (i == 8 ? "x" : "") + RpgFormat.num(cur[i], dec[i], minDec[i]) + (i == 5 ? "/s" : i == 7 ? "%" : "");
			g.drawString(this.font, STAT_LABELS[i], rx, y, DIM, false);
			int right = rx + rw;
			g.drawString(this.font, value, right - this.font.width(value), y, WHITE, false);
			double bonus = cur[i] - base[i];
			if (Math.abs(bonus) >= 0.5 * Math.pow(10.0, -dec[i])) {
				String b = "(" + RpgFormat.signed(bonus, dec[i]) + ")";
				g.drawString(this.font, b, right - this.font.width(value) - 4 - this.font.width(b), y, bonus > 0 ? GOOD : BAD, false);
			}
		}
	}

	// ---------------------------------------------------------------
	//  Placeholders (no function yet)
	// ---------------------------------------------------------------
	private void drawPlaceholder(GuiGraphics g, Tab t, int ix, int iy, int iw, int ih) {
		int cx = ix + iw / 2, cy = iy + ih / 2 - 16;
		g.pose().pushPose();
		g.pose().translate(cx - 24, cy - 40, 0.0F);
		g.pose().scale(3.0F, 3.0F, 1.0F);
		g.blit(UiKit.RPG_ICONS, 0, 0, t.ordinal() * 16.0F, 0.0F, 16, 16, 128, 32);
		g.pose().popPose();
		Component c = UiKit.title("Coming soon");
		g.drawString(this.font, c, cx - this.font.width(c) / 2, cy + 18, UiKit.TEXT_HOVER, true);
		String d = switch (t) {
			case QUESTS -> "Active and completed quests will appear here.";
			case MAP -> "The world map will appear here.";
			default -> "Interface settings will appear here.";
		};
		g.drawString(this.font, d, cx - this.font.width(d) / 2, cy + 34, DIM, false);
	}
}
