package net.mcreator.eldervale.client.ui.pause;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.client.party.PartyGfx;
import net.mcreator.eldervale.client.ui.kit.EldervaleButton;
import net.mcreator.eldervale.client.ui.kit.SectionButton;
import net.mcreator.eldervale.client.ui.kit.UiKit;
import net.mcreator.eldervale.client.ui.rpg.RpgMenuScreen;
import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Replaces the vanilla pause screen (ESC).
 * As in the title screen, the vanilla actions are NOT re-implemented: a vanilla {@link PauseScreen} is initialised
 * off-screen and each Eldervale button forwards its click to the matching vanilla button
 * (return to game, advancements, statistics, options, open to LAN, save and quit / disconnect).
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class EldervalePauseScreen extends Screen {

	/** Set to false to get the vanilla pause screen back without removing any code. */
	private static final boolean ENABLED = true;

	private static final int WIN_W = 244;
	private static final int BTN_W = 220, BTN_H = 20, GAP = 4;

	@SubscribeEvent
	public static void onOpening(ScreenEvent.Opening event) {
		if (ENABLED && event.getNewScreen() instanceof PauseScreen && Minecraft.getInstance().player != null) {
			event.setNewScreen(new EldervalePauseScreen());
		}
	}

	private int winX, winY, winH;

	public EldervalePauseScreen() {
		super(Component.translatable("menu.paused"));
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	// ---------------------------------------------------------------
	@Override
	protected void init() {
		PauseScreen vanilla = new PauseScreen(true);
		vanilla.init(this.minecraft, this.width, this.height);

		Button back = find(vanilla, "menu.returnToGame");
		Button adv = find(vanilla, "gui.advancements");
		Button stats = find(vanilla, "gui.stats");
		Button opt = find(vanilla, "menu.options");
		Button lan = find(vanilla, "menu.shareToLan");
		Button quit = find(vanilla, "menu.returnToMenu", "menu.disconnect");

		// rows: each is one full-width button or two half-width buttons
		List<EldervaleButton[]> rows = new ArrayList<>();
		if (back != null)
			rows.add(new EldervaleButton[]{forward(back, false, BTN_W)});
		rows.add(new EldervaleButton[]{new EldervaleButton(0, 0, BTN_W, BTN_H, UiKit.title("Character Menu"), b -> this.minecraft.setScreen(new RpgMenuScreen(this, RpgMenuScreen.Tab.CHARACTER)))});
		if (adv != null && stats != null)
			rows.add(new EldervaleButton[]{forward(adv, false, (BTN_W - GAP) / 2), forward(stats, false, (BTN_W - GAP) / 2)});
		if (opt != null)
			rows.add(new EldervaleButton[]{forward(opt, false, BTN_W)});
		if (lan != null)
			rows.add(new EldervaleButton[]{forward(lan, false, BTN_W)});
		if (quit != null)
			rows.add(new EldervaleButton[]{forward(quit, true, BTN_W)});

		// window geometry
		int headerH = 36, iconsH = 24 + 14;
		winH = 14 + headerH + iconsH + rows.size() * (BTN_H + GAP) + 10;
		winX = (this.width - WIN_W) / 2;
		winY = Math.max(4, (this.height - winH) / 2);

		// section shortcuts: open the RPG menu directly at that section
		int sx = winX + (WIN_W - (8 * 24 + 7 * 4)) / 2;
		int sy = winY + 14 + headerH;
		for (RpgMenuScreen.Tab t : RpgMenuScreen.Tab.values()) {
			SectionButton sb = new SectionButton(sx + t.ordinal() * 28, sy, Component.literal(t.label), t.ordinal(), b -> this.minecraft.setScreen(new RpgMenuScreen(this, t)));
			this.addRenderableWidget(sb);
		}

		int y = sy + iconsH;
		int x0 = winX + (WIN_W - BTN_W) / 2;
		for (EldervaleButton[] row : rows) {
			int x = x0;
			for (EldervaleButton b : row) {
				b.setPosition(x, y);
				this.addRenderableWidget(b);
				x += b.getWidth() + GAP;
			}
			y += BTN_H + GAP;
		}
	}

	private static Button find(PauseScreen vanilla, String... keys) {
		for (GuiEventListener l : vanilla.children()) {
			if (l instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc) {
				for (String k : keys)
					if (k.equals(tc.getKey()))
						return b;
			}
		}
		return null;
	}

	/** once = true: the button is disabled after the click (leaving the world must not run twice). */
	private EldervaleButton forward(Button target, boolean once, int width) {
		EldervaleButton[] self = new EldervaleButton[1];
		self[0] = new EldervaleButton(0, 0, width, BTN_H, UiKit.title(target.getMessage()), btn -> {
			target.onPress();
			if (once)
				self[0].active = false;
		});
		self[0].active = target.active;
		return self[0];
	}

	// ---------------------------------------------------------------
	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		g.fill(0, 0, this.width, this.height, 0xB00A0810);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		renderBackground(g, mouseX, mouseY, partialTick);
		UiKit.nineSlice(g, UiKit.PANEL, winX, winY, WIN_W, winH, 0, 0, 32, 32, 32, 32, 8);

		Minecraft mc = Minecraft.getInstance();
		EldervaleModVariables.PlayerVariables v = mc.player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		int accent = v.PLAYER_CLASS == null || v.PLAYER_CLASS.isEmpty() ? UiKit.GOLD : PartyGfx.classColor(v.PLAYER_CLASS);

		int left = winX + 14, right = winX + WIN_W - 14, y = winY + 12;
		g.drawString(this.font, UiKit.title(mc.player.getGameProfile().getName()), left, y, UiKit.TEXT, true);
		Component lv = UiKit.title("Lv " + (int) Math.round(v.LEVEL));
		g.drawString(this.font, lv, right - this.font.width(lv), y, UiKit.GOLD, true);
		Component cls = UiKit.title(PartyGfx.classLabel(v.PLAYER_CLASS));
		g.drawString(this.font, cls, left, y + 13, accent, true);
		g.fill(left, winY + 14 + 28, right, winY + 14 + 29, 0xFF4A3E2A);

		super.render(g, mouseX, mouseY, partialTick);
	}
}
