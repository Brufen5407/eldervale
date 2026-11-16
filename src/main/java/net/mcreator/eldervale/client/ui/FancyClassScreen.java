package net.mcreator.eldervale.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.core.Holder;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;

import net.mcreator.eldervale.world.inventory.ClassselectionMenu;
import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.network.ClassselectionButtonMessage;
import net.mcreator.eldervale.client.gui.ClassselectionScreen;

@EventBusSubscriber(value = Dist.CLIENT)
public class FancyClassScreen extends AbstractContainerScreen<ClassselectionMenu> {

	// Button IDs of the MCreator GUI (if the arrows go the wrong way, swap PREV and NEXT)
	private static final int PREV_ID = 0;
	private static final int NEXT_ID = 1;
	private static final int CONFIRM_ID = 2;

	// ---- Sounds ----
	private static final boolean SOUNDS_ENABLED = true;
	private static final float SOUND_VOLUME = 1.0F; // global multiplier, 0.0 - 1.0

	// ---------------------------------------------------------------
	//  CLASS DATA  (edit texts and stats here)
	//  stats = {HP, DEF, ATT SPEED, ATT, MANA, MANA REGEN, SPEED}
	// ---------------------------------------------------------------
	private record Skill(String name, String cost, String desc) {}

	private record Cls(String name, String icon, int color, double[] stats, Skill[] skills) {}

	private static final String[] LABELS = {"HP", "DEF", "ATT SPEED", "ATT", "MANA", "MANA REGEN", "SPEED"};
	private static final int[] STAT_COLORS = {0xE04848, 0x8FA4BF, 0xFFB347, 0xFF6B3D, 0x4AA3FF, 0x55D6E0, 0x6EE06E};
	private static final String[] KINDS = {"SKILL 1", "SKILL 2", "PASSIVE"};

	private static final Cls[] CLASSES = {
			new Cls("PALADIN", "paladin_image.png", 0xF2D27A, new double[]{50, 10, 4.0, 1.5, 80, 4, 0.09}, new Skill[]{
					new Skill("HOLY SHIELDS", "50 mana - CD 30s", "For 5 seconds the paladin takes no damage."),
					new Skill("HOLY ZONE", "30 mana - CD 20s", "The paladin gains resistance and regeneration for 2s. In a team, allies receive the same effects for 5 seconds."),
					new Skill("SACRED ZONE", "", "Monsters within a radius of 6 are slowed.")}),
			new Cls("BERSERKER", "berserker_icon.png", 0xE0453A, new double[]{35, 4, 3.2, 2.5, 50, 2, 0.10}, new Skill[]{
					new Skill("BERSERK SLAM", "30 mana - CD 12s", "A small leap that deals 5 damage on impact in an area of 7."),
					new Skill("BERSERK CHARGE", "20 mana - CD 14s", "The player dashes forward and knocks back all enemies in an area of 5."),
					new Skill("BERSERK MODE", "", "Below 50% health the player gains a 50% boost to attack and speed. When attacking, nearby mobs take 50% of the damage dealt.")}),
			new Cls("RANGER", "ranger_icom.png", 0x5FCF6B, new double[]{25, 2, 4.4, 1.2, 120, 6, 0.12}, new Skill[]{
					new Skill("DECOY", "35 mana - CD 25s", "The player gains speed and invisibility for 5s, allowing escape, and summons a dummy that draws mob aggro for 8s, then disappears."),
					new Skill("MAGIC ARROWS", "40 mana - CD 12s", "The player leaps backward and fires 3 magic arrows at the enemy."),
					new Skill("CRAZY REFLEX", "", "When the player takes damage they gain Speed I for 3 seconds. Cooldown 8s.")}),
			new Cls("MAGE", "mage_icon.png", 0x6EA8FF, new double[]{20, 1, 3.0, 0.5, 160, 10, 0.10}, null),
			new Cls("ASSASSIN", "assassin_icon.png", 0xA55CFF, new double[]{25, 2, 5.0, 1.5, 70, 4, 0.14}, null),
			new Cls("MONK", "monk_icon.png", 0xFFA24A, new double[]{35, 5, 4.8, 1.8, 90, 6, 0.13}, null),
			new Cls("WEREWOLF", "werewolf_icon.png", 0xB08E6A, new double[]{40, 6, 4.5, 1.7, 60, 3, 0.12}, null),
			new Cls("ALCHEMIST", "alchemist_icon.png", 0x4FE0C0, new double[]{20, 1, 3.2, 0.7, 150, 10, 0.10}, null),
			new Cls("SPELLBLADE", "spellblade_icon.png", 0xFF6BD6, new double[]{30, 4, 4.2, 1.7, 120, 8, 0.11}, null)};

	private static final ResourceLocation[] ICONS = new ResourceLocation[CLASSES.length];
	private static final double[] MAX = new double[7];

	static {
		for (int i = 0; i < CLASSES.length; i++)
			ICONS[i] = ResourceLocation.fromNamespaceAndPath("eldervale", "textures/screens/" + CLASSES[i].icon());
		for (Cls c : CLASSES)
			for (int i = 0; i < 7; i++)
				MAX[i] = Math.max(MAX[i], c.stats()[i]);
	}

	// ---------------------------------------------------------------
	//  Colors and sizes
	// ---------------------------------------------------------------
	private static final int PW = 440;
	private static final int PH = 240;

	private static final int FRAME_DARK = 0xFF0B0810;
	private static final int GOLD_LIGHT = 0xFFF2D27A;
	private static final int GOLD = 0xFFC9A24A;
	private static final int GOLD_DARK = 0xFF7A5A1E;
	private static final int BG_TOP = 0xFF2A2430;
	private static final int BG_BOTTOM = 0xFF141018;
	private static final int WHITE = 0xFFFFFFFF;
	private static final int BLACK = 0xFF000000;

	// ---------------------------------------------------------------
	//  Replaces the old MCreator screen when it opens
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onOpening(ScreenEvent.Opening event) {
		if (event.getNewScreen() instanceof ClassselectionScreen old) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null)
				return;
			event.setNewScreen(new FancyClassScreen(old.getMenu(), mc.player.getInventory(), old.getTitle()));
		}
	}

	// ---------------------------------------------------------------
	//  Screen state
	// ---------------------------------------------------------------
	private final Player player;
	private final int menuX, menuY, menuZ;

	private int page = -1;
	private int dir = 1;
	private float anim = 1.0F;
	private final double[] cur = new double[7];
	private boolean curInit = false;
	private long lastNanos = 0L;
	private int hover = -1;
	private int lastHover = -1;
	private boolean openSoundPlayed = false;

	public FancyClassScreen(ClassselectionMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.player = menu.entity;
		this.menuX = menu.x;
		this.menuY = menu.y;
		this.menuZ = menu.z;
	}

	@Override
	protected void init() {
		super.init();
		if (!openSoundPlayed) {
			openSoundPlayed = true;
			playUi(SoundEvents.ENCHANTMENT_TABLE_USE, 1.3F, 0.5F);
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
	}

	// ---------------------------------------------------------------
	//  Sounds
	// ---------------------------------------------------------------
	private static void playUi(SoundEvent sound, float pitch, float volume) {
		if (!SOUNDS_ENABLED || volume <= 0.0F)
			return;
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, Math.min(1.0F, volume * SOUND_VOLUME)));
	}

	private static void playUi(Holder<SoundEvent> sound, float pitch, float volume) {
		playUi(sound.value(), pitch, volume);
	}

	// ---------------------------------------------------------------
	//  Input
	// ---------------------------------------------------------------
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) {
			float s = panelScale();
			float ox = (this.width - PW * s) / 2.0F;
			float oy = (this.height - PH * s) / 2.0F;
			int h = hitTest((mouseX - ox) / s, (mouseY - oy) / s);
			if (h >= 0) {
				press(h);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean keyPressed(int key, int scancode, int mods) {
		if (key == 256) { // ESC
			playUi(SoundEvents.UI_BUTTON_CLICK, 0.7F, 0.8F);
			this.minecraft.player.closeContainer();
			return true;
		}
		if (key == 263) { // left arrow
			press(0);
			return true;
		}
		if (key == 262) { // right arrow
			press(1);
			return true;
		}
		if (key == 257 || key == 335) { // Enter
			press(2);
			return true;
		}
		return super.keyPressed(key, scancode, mods);
	}

	// 0 = previous, 1 = next, 2 = confirm
	private void press(int which) {
		int id = which == 0 ? PREV_ID : which == 1 ? NEXT_ID : CONFIRM_ID;
		if (which == 2) {
			playUi(SoundEvents.UI_BUTTON_CLICK, 1.0F, 1.0F);
			playUi(SoundEvents.PLAYER_LEVELUP, 1.0F, 0.6F);
		} else {
			playUi(SoundEvents.UI_BUTTON_CLICK, which == 1 ? 1.2F : 0.9F, 1.0F);
		}
		PacketDistributor.sendToServer(new ClassselectionButtonMessage(id, menuX, menuY, menuZ));
		ClassselectionButtonMessage.handleButtonAction(player, id, menuX, menuY, menuZ);
	}

	private static boolean in(double px, double py, int x, int y, int w, int h) {
		return px >= x && py >= y && px < x + w && py < y + h;
	}

	private int hitTest(double px, double py) {
		if (in(px, py, 14, 188, 18, 16))
			return 0;
		if (in(px, py, 120, 188, 18, 16))
			return 1;
		if (in(px, py, 140, 210, 160, 22))
			return 2;
		return -1;
	}

	// ---------------------------------------------------------------
	//  Rendering
	// ---------------------------------------------------------------
	private float panelScale() {
		float fit = Math.min((this.width - 16.0F) / PW, (this.height - 16.0F) / PH);
		if (fit >= 1.0F)
			return Math.min((float) Math.floor(fit), 3.0F);
		return Math.max(0.4F, fit);
	}

	private int readPage() {
		try {
			double v = Minecraft.getInstance().player.getData(EldervaleModVariables.PLAYER_VARIABLES).page;
			int i = (int) Math.round(v);
			return Math.max(0, Math.min(CLASSES.length - 1, i));
		} catch (Exception e) {
			return 0;
		}
	}

	private void syncPage() {
		int p = readPage();
		if (page < 0) {
			page = p;
			return;
		}
		if (p != page) {
			int d = p - page;
			if (d > 1)
				d = -1;
			else if (d < -1)
				d = 1;
			dir = d > 0 ? 1 : -1;
			anim = 0.0F;
			page = p;
			playUi(SoundEvents.BOOK_PAGE_TURN, dir > 0 ? 1.15F : 0.9F, 0.7F);
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		double dt = lastNanos == 0L ? 0.016 : (now - lastNanos) / 1.0E9;
		lastNanos = now;
		dt = Math.max(0.0, Math.min(dt, 0.1));

		syncPage();
		anim = Math.min(1.0F, anim + (float) (dt * 4.0));

		double[] target = CLASSES[page].stats();
		for (int i = 0; i < 7; i++) {
			double t = target[i] / MAX[i];
			if (!curInit)
				cur[i] = t;
			else
				cur[i] += (t - cur[i]) * (1.0 - Math.exp(-dt * 10.0));
		}
		curInit = true;

		g.fill(0, 0, this.width, this.height, 0xC0070510);

		float s = panelScale();
		float ox = (this.width - PW * s) / 2.0F;
		float oy = (this.height - PH * s) / 2.0F;
		hover = hitTest((mouseX - ox) / s, (mouseY - oy) / s);

		// soft chime when the mouse moves onto a button
		if (hover != lastHover) {
			if (hover >= 0)
				playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 1.8F, 0.25F);
			lastHover = hover;
		}

		g.pose().pushPose();
		g.pose().translate(ox, oy, 0.0F);
		g.pose().scale(s, s, 1.0F);

		drawPanel(g);
		drawLeft(g);
		drawStats(g);
		drawSkills(g);
		drawButtons(g);

		g.pose().popPose();
	}

	private void drawPanel(GuiGraphics g) {
		int c = CLASSES[page].color();

		g.fill(-3, -3, PW + 3, PH + 3, FRAME_DARK);
		g.fill(-2, -2, PW + 2, PH + 2, GOLD_DARK);
		g.fill(-1, -1, PW + 1, PH + 1, GOLD);
		g.fill(0, 0, PW, PH, FRAME_DARK);
		g.fillGradient(2, 2, PW - 2, PH - 2, BG_TOP, BG_BOTTOM);
		g.fillGradient(2, 2, PW - 2, 28, 0x30FFFFFF, 0x00FFFFFF);

		// corners
		int[][] corners = {{0, 0}, {PW, 0}, {0, PH}, {PW, PH}};
		for (int[] k : corners) {
			diamond(g, k[0], k[1], 7, FRAME_DARK);
			diamond(g, k[0], k[1], 6, GOLD);
			diamond(g, k[0], k[1], 4, FRAME_DARK);
			diamond(g, k[0], k[1], 3, argb(c, 0xFF));
		}

		// title
		outlinedC(g, "CHOOSE YOUR CLASS", PW / 2.0F, 9, 1.2F, GOLD_LIGHT, BLACK);
		g.fill(24, 26, PW - 24, 27, GOLD_DARK);
		diamond(g, PW / 2, 26, 3, GOLD);

		// column boxes
		box(g, 12, 32, 140, 206);
		box(g, 148, 32, 266, 206);
		box(g, 274, 32, 430, 206);
	}

	private void box(GuiGraphics g, int x1, int y1, int x2, int y2) {
		g.fill(x1, y1, x2, y2, 0x40000000);
		g.fill(x1, y1, x2, y1 + 1, 0x60C9A24A);
		g.fill(x1, y2 - 1, x2, y2, 0x60C9A24A);
		g.fill(x1, y1, x1 + 1, y2, 0x60C9A24A);
		g.fill(x2 - 1, y1, x2, y2, 0x60C9A24A);
	}

	private void drawLeft(GuiGraphics g) {
		Cls c = CLASSES[page];
		float ease = 1.0F - (float) Math.pow(1.0F - anim, 3.0);

		// spotlight behind the character
		g.fillGradient(14, 34, 138, 160, argb(c.color(), 0x00), argb(c.color(), 0x48));
		g.fill(24, 160, 128, 161, argb(c.color(), 0xA0));

		// image with slide-in and a light floating motion
		float bob = (float) Math.sin(System.currentTimeMillis() / 1000.0 * 2.0) * 1.5F;
		float off = dir * (1.0F - ease) * 40.0F;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, ease);
		g.pose().pushPose();
		g.pose().translate(30.0F + off, 34.0F + bob, 0.0F);
		g.pose().scale(1.15F, 1.15F, 1.0F);
		g.blit(ICONS[page], 0, 0, 0.0F, 0.0F, 80, 107, 80, 107);
		g.pose().popPose();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		// name
		int a = Math.max(8, (int) (255 * ease));
		outlinedC(g, c.name(), 76, 165, 1.3F, argb(c.color(), a), argb(0x000000, a));
	}

	private void drawStats(GuiGraphics g) {
		Cls c = CLASSES[page];
		outlined(g, "STATS", 154, 36, 0.8F, GOLD_LIGHT, BLACK);

		for (int i = 0; i < 7; i++) {
			int y = 48 + i * 20;
			outlined(g, LABELS[i], 154, y, 0.7F, 0xFFD8D0E0, BLACK);
			outlinedR(g, fmt(i, c.stats()[i]), 260, y - 0.5F, 0.8F, WHITE, BLACK);
			bar(g, 154, y + 10, 106, 5, cur[i], argb(STAT_COLORS[i], 0xFF), argb(shade(STAT_COLORS[i], 0.5F), 0xFF));
		}

		outlinedC(g, (page + 1) + " / " + CLASSES.length, 207, 194, 0.7F, 0xFFB8AFC8, BLACK);
	}

	private void drawSkills(GuiGraphics g) {
		Cls c = CLASSES[page];
		outlined(g, "SKILLS", 280, 36, 0.8F, GOLD_LIGHT, BLACK);

		int x = 280, w = 144, top = 48, avail = 154;
		Skill[] sk = c.skills();

		if (sk == null) {
			for (int i = 0; i < 3; i++) {
				int y = top + i * 34;
				outlined(g, KINDS[i], x, y + 1.5F, 0.6F, argb(c.color(), 0xFF), BLACK);
				outlined(g, "???", x + this.font.width(KINDS[i]) * 0.6F + 4, y, 0.85F, 0xFF8A8396, BLACK);
				plain(g, "Coming soon", x, y + 11, 0.62F, 0xFF8A8396);
				g.fill(x, y + 22, x + w, y + 23, 0x30C9A24A);
			}
			return;
		}

		float ds = 0.48F;
		List<List<FormattedCharSequence>> lines = new ArrayList<>();
		float[] cands = {0.66F, 0.6F, 0.54F, 0.48F};
		for (float cand : cands) {
			ds = cand;
			lines.clear();
			float total = 0;
			for (Skill k : sk) {
				List<FormattedCharSequence> l = this.font.split(Component.literal(k.desc()), (int) (w / ds));
				lines.add(l);
				total += blockHeight(k, l.size(), ds);
			}
			if (total <= avail)
				break;
		}

		float y = top;
		for (int i = 0; i < sk.length; i++) {
			Skill k = sk[i];
			outlined(g, KINDS[i], x, y + 1.5F, 0.6F, argb(c.color(), 0xFF), BLACK);
			float kw = this.font.width(KINDS[i]) * 0.6F;
			outlined(g, k.name(), x + kw + 4, y, 0.85F, WHITE, BLACK);

			float yy = y + 10;
			if (!k.cost().isEmpty()) {
				plain(g, k.cost(), x, yy, 0.62F, 0xFF7FC8FF);
				yy += 7;
			}
			for (FormattedCharSequence line : lines.get(i)) {
				plain(g, line, x, yy, ds, 0xFFE6E0F0);
				yy += 10 * ds;
			}

			float bh = blockHeight(k, lines.get(i).size(), ds);
			if (i < sk.length - 1)
				g.fill(x, (int) (y + bh - 4), x + w, (int) (y + bh - 3), 0x30C9A24A);
			y += bh;
		}
	}

	private float blockHeight(Skill k, int lineCount, float ds) {
		float head = k.cost().isEmpty() ? 10 : 17;
		return head + lineCount * 10 * ds + 8;
	}

	private void drawButtons(GuiGraphics g) {
		Cls c = CLASSES[page];

		arrowButton(g, 14, 188, 18, 16, true, hover == 0);
		arrowButton(g, 120, 188, 18, 16, false, hover == 1);

		// page dots
		for (int i = 0; i < CLASSES.length; i++) {
			int cx = 44 + i * 8;
			if (i == page) {
				diamond(g, cx, 196, 4, FRAME_DARK);
				diamond(g, cx, 196, 3, argb(c.color(), 0xFF));
			} else {
				diamond(g, cx, 196, 1, GOLD_DARK);
			}
		}

		// confirm
		int bx = 140, by = 210, bw = 160, bh = 22;
		boolean hov = hover == 2;
		g.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, FRAME_DARK);
		g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, hov ? GOLD_LIGHT : GOLD);
		g.fillGradient(bx, by, bx + bw, by + bh, argb(shade(c.color(), hov ? 1.0F : 0.75F), 0xFF), argb(shade(c.color(), hov ? 0.55F : 0.35F), 0xFF));
		g.fill(bx, by, bx + bw, by + 1, 0x66FFFFFF);
		outlinedC(g, "SELECT " + c.name(), bx + bw / 2.0F, by + (bh - 9 * 0.95F) / 2.0F, 0.95F, WHITE, BLACK);
	}

	private void arrowButton(GuiGraphics g, int x, int y, int w, int h, boolean left, boolean hov) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, FRAME_DARK);
		g.fill(x, y, x + w, y + h, hov ? GOLD_LIGHT : GOLD);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hov ? 0xFF3A3040 : 0xFF1C1722);
		int cx = x + w / 2;
		int cy = y + h / 2;
		int col = hov ? WHITE : GOLD_LIGHT;
		for (int dy = -3; dy <= 3; dy++) {
			int xo = Math.abs(dy);
			if (left)
				g.fill(cx - 2 + xo, cy + dy, cx + xo, cy + dy + 1, col);
			else
				g.fill(cx - xo, cy + dy, cx + 2 - xo, cy + dy + 1, col);
		}
	}

	private void bar(GuiGraphics g, int x, int y, int w, int h, double ratio, int top, int bottom) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BLACK);
		g.fill(x, y, x + w, y + h, 0xFF1B1620);
		int fw = (int) Math.round(Math.max(0.0, Math.min(1.0, ratio)) * w);
		if (fw > 0) {
			g.fillGradient(x, y, x + fw, y + h, top, bottom);
			g.fill(x, y, x + fw, y + 1, 0x66FFFFFF);
		}
	}

	// ---------------------------------------------------------------
	//  Utilities
	// ---------------------------------------------------------------
	private static String fmt(int stat, double v) {
		if (stat == 0 || stat == 1 || stat == 4)
			return String.valueOf((int) Math.round(v));
		if (stat == 2 || stat == 3)
			return String.format(Locale.ROOT, "%.1f", v);
		if (stat == 5)
			return (int) Math.round(v) + "/s";
		return String.format(Locale.ROOT, "%.2f", v);
	}

	private static int argb(int rgb, int a) {
		return (a << 24) | (rgb & 0xFFFFFF);
	}

	private static int shade(int rgb, float f) {
		int r = (int) (((rgb >> 16) & 255) * f);
		int gr = (int) (((rgb >> 8) & 255) * f);
		int b = (int) ((rgb & 255) * f);
		return (r << 16) | (gr << 8) | b;
	}

	private static void diamond(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int i = -r; i <= r; i++) {
			int half = r - Math.abs(i);
			g.fill(cx - half, cy + i, cx + half + 1, cy + i + 1, color);
		}
	}

	private void outlined(GuiGraphics g, String text, float x, float y, float scale, int color, int outline) {
		if (text == null || text.isEmpty())
			return;
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
				g.drawString(this.font, text, 0, 0, outline, false);
				g.pose().popPose();
			}
		}
		g.drawString(this.font, text, 0, 0, color, false);
		g.pose().popPose();
	}

	private void outlinedC(GuiGraphics g, String text, float cx, float y, float scale, int color, int outline) {
		outlined(g, text, cx - this.font.width(text) * scale / 2.0F, y, scale, color, outline);
	}

	private void outlinedR(GuiGraphics g, String text, float rx, float y, float scale, int color, int outline) {
		outlined(g, text, rx - this.font.width(text) * scale, y, scale, color, outline);
	}

	private void plain(GuiGraphics g, String text, float x, float y, float scale, int color) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0.0F);
		g.pose().scale(scale, scale, 1.0F);
		g.drawString(this.font, text, 0, 0, color, true);
		g.pose().popPose();
	}

	private void plain(GuiGraphics g, FormattedCharSequence text, float x, float y, float scale, int color) {
		g.pose().pushPose();
		g.pose().translate(x, y, 0.0F);
		g.pose().scale(scale, scale, 1.0F);
		g.drawString(this.font, text, 0, 0, color, true);
		g.pose().popPose();
	}
}
