package net.mcreator.eldervale.client.ui.menu;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.util.Mth;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.SharedConstants;

import net.mcreator.eldervale.client.ui.kit.UiKit;
import net.mcreator.eldervale.client.ui.kit.EldervaleButton;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Replaces the vanilla title screen.
 *
 * The buttons do NOT re-implement vanilla logic: a vanilla {@link TitleScreen} is initialised off-screen and
 * each Eldervale button forwards its click to the matching vanilla button. This keeps the multiplayer safety
 * warning, the disabled states, NeoForge's Mods button and anything other mods add working as usual.
 * Singleplayer and Multiplayer have a direct fallback to the vanilla screens if their vanilla button cannot be found.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class EldervaleTitleScreen extends Screen {

	/** Set to false to get the vanilla title screen back without removing any code. */
	private static final boolean ENABLED = true;

	private static final net.minecraft.resources.ResourceLocation LOGO = UiKit.tex("logo");
	private static final int LOGO_W = 164, LOGO_H = 34;
	private static final int BTN_W = 156, BTN_H = 22, BTN_GAP = 5;
	private static final String TAGLINE = "YOUR DESTINY. YOUR CLASS. YOUR LEGEND.";

	/** The intro (fade-in, logo and buttons appearing) plays once per game session. */
	private static boolean introPlayed = false;

	@SubscribeEvent
	public static void onOpening(ScreenEvent.Opening event) {
		if (ENABLED && event.getNewScreen() instanceof TitleScreen) {
			event.setNewScreen(new EldervaleTitleScreen());
		}
	}

	// ---------------------------------------------------------------
	private final MenuBackground background = new MenuBackground();
	private final boolean playIntro;
	private final long introStart = System.nanoTime();

	private final List<EldervaleButton> mainButtons = new ArrayList<>();
	private final List<EldervaleButton> iconButtons = new ArrayList<>();
	private int logoScale = 2, logoX, logoY, tagY;

	public EldervaleTitleScreen() {
		super(Component.translatable("narrator.screen.title"));
		this.playIntro = !introPlayed;
		introPlayed = true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	// ---------------------------------------------------------------
	//  Setup
	// ---------------------------------------------------------------
	@Override
	protected void init() {
		mainButtons.clear();
		iconButtons.clear();

		TitleScreen vanilla = new TitleScreen();
		vanilla.init(this.minecraft, this.width, this.height);

		Button sp = find(vanilla, "menu.singleplayer");
		Button mp = find(vanilla, "menu.multiplayer");
		Button opt = find(vanilla, "menu.options");
		Button quit = find(vanilla, "menu.quit");
		Button lang = find(vanilla, "narrator.button.language");
		Button acc = find(vanilla, "narrator.button.accessibility");
		Button mods = find(vanilla, "fml.menu.mods");

		mainButtons.add(main("menu.singleplayer", sp, () -> this.minecraft.setScreen(new SelectWorldScreen(this))));
		mainButtons.add(main("menu.multiplayer", mp, () -> this.minecraft.setScreen(new JoinMultiplayerScreen(this))));
		mainButtons.add(main("menu.options", opt, null));
		mainButtons.add(main("menu.quit", quit, () -> this.minecraft.stop()));

		iconButtons.add(icon(lang, "options.language", 0));
		iconButtons.add(icon(acc, "options.accessibility.title", 1));
		iconButtons.add(icon(mods, "fml.menu.mods", 2));

		layout();
		mainButtons.forEach(this::addRenderableWidget);
		iconButtons.forEach(this::addRenderableWidget);
	}

	private static Button find(TitleScreen vanilla, String key) {
		for (GuiEventListener l : vanilla.children()) {
			if (l instanceof Button b && b.getMessage().getContents() instanceof TranslatableContents tc && key.equals(tc.getKey()))
				return b;
		}
		return null;
	}

	private EldervaleButton main(String key, Button target, Runnable fallback) {
		Component msg = target != null ? target.getMessage() : Component.translatable(key);
		EldervaleButton b = new EldervaleButton(0, 0, BTN_W, BTN_H, UiKit.title(msg), btn -> {
			if (target != null)
				target.onPress();
			else if (fallback != null)
				fallback.run();
		});
		b.active = target != null ? target.active : fallback != null;
		return b;
	}

	/** Icon buttons exist only when the matching vanilla / NeoForge button was found (Mods is absent without NeoForge's button). */
	private EldervaleButton icon(Button target, String messageKey, int iconIndex) {
		if (target == null)
			return null;
		EldervaleButton b = EldervaleButton.icon(0, 0, 20, target.getMessage(), iconIndex, btn -> target.onPress());
		b.active = target.active;
		return b;
	}

	private void layout() {
		iconButtons.removeIf(java.util.Objects::isNull);

		int stack = 4 * BTN_H + 3 * BTN_GAP;
		int ls = Mth.clamp((int) (this.width * 0.6F / LOGO_W), 1, 3);
		int tagH = 9, tagGap = 8, belowTag = 20;
		while (ls > 1 && LOGO_H * ls + tagGap + tagH + belowTag + stack > this.height - 24)
			ls--;
		this.logoScale = ls;

		int blockH = LOGO_H * ls + tagGap + tagH + belowTag + stack;
		int top = Math.max(10, (this.height - blockH) / 2 - this.height / 24);
		this.logoX = (this.width - LOGO_W * ls) / 2;
		this.logoY = top;
		this.tagY = top + LOGO_H * ls + tagGap;

		int bx = (this.width - BTN_W) / 2;
		int by = tagY + tagH + belowTag;
		for (int i = 0; i < mainButtons.size(); i++)
			mainButtons.get(i).setPosition(bx, by + i * (BTN_H + BTN_GAP));

		int ix = 8, iy = this.height - 28;
		for (EldervaleButton b : iconButtons) {
			b.setPosition(ix, iy);
			ix += 24;
		}
	}

	// ---------------------------------------------------------------
	//  Rendering
	// ---------------------------------------------------------------
	private static float ease(float x) {
		x = Mth.clamp(x, 0.0F, 1.0F);
		float k = 1.0F - x;
		return 1.0F - k * k * k;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		// the scene is drawn in render(); nothing to do here (prevents the vanilla dirt/blur background)
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		float t = playIntro ? (System.nanoTime() - introStart) / 1.0E9F : 99.0F;

		background.render(g, this.width, this.height, mouseX);

		for (int i = 0; i < mainButtons.size(); i++)
			mainButtons.get(i).setAlpha(ease((t - 0.75F - i * 0.12F) / 0.45F));
		for (EldervaleButton b : iconButtons)
			b.setAlpha(ease((t - 1.3F) / 0.5F));

		super.render(g, mouseX, mouseY, partialTick);

		// logo
		float logoA = ease((t - 0.35F) / 0.8F);
		float dy = (1.0F - logoA) * -6.0F;
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, logoA);
		g.pose().pushPose();
		g.pose().translate(logoX, logoY + dy, 0.0F);
		g.pose().scale(logoScale, logoScale, 1.0F);
		g.blit(LOGO, 0, 0, 0.0F, 0.0F, LOGO_W, LOGO_H, LOGO_W, LOGO_H);
		g.pose().popPose();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		// tagline
		float tagA = ease((t - 0.8F) / 0.6F);
		if (tagA > 0.02F)
			UiKit.drawSpaced(g, this.font, TAGLINE, this.width / 2, tagY, UiKit.withAlpha(UiKit.TEXT_DIM, tagA), 1, true);

		// footer (vanilla font, small and quiet)
		float footA = ease((t - 1.3F) / 0.5F);
		if (footA > 0.02F) {
			String version = "Eldervale  -  Minecraft " + SharedConstants.getCurrentVersion().getName();
			Component credits = Component.translatable("title.credits");
			int c = UiKit.withAlpha(UiKit.TEXT_DISABLED, footA);
			g.drawString(this.font, version, this.width - this.font.width(version) - 6, this.height - 22, c, true);
			g.drawString(this.font, credits, this.width - this.font.width(credits) - 6, this.height - 11, c, true);
		}

		// intro fade from black
		if (playIntro) {
			float fade = 1.0F - ease(t / 0.9F);
			if (fade > 0.004F)
				g.fill(0, 0, this.width, this.height, ((int) (fade * 255.0F) << 24));
		}
	}
}
