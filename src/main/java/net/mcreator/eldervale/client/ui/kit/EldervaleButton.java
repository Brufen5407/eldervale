package net.mcreator.eldervale.client.ui.kit;

import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Eldervale button: texture plate (normal / hover / disabled), Cinzel label and a short eased hover transition.
 * Still a real {@link Button}: keyboard focus, narration, click sound and tooltips work as in vanilla.
 */
public class EldervaleButton extends Button {

	private static final int TEXT_Y_OFFSET = 0; // tweak if the Cinzel baseline looks off by a pixel

	private final int iconIndex; // -1 = text button
	private float hoverT = 0.0F;
	private long lastNanos = 0L;

	public EldervaleButton(int x, int y, int w, int h, Component message, OnPress onPress) {
		super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
		this.iconIndex = -1;
	}

	private EldervaleButton(int x, int y, int size, Component message, int iconIndex, OnPress onPress) {
		super(x, y, size, size, message, onPress, DEFAULT_NARRATION);
		this.iconIndex = iconIndex;
	}

	/** Square icon button. iconIndex: 0 language, 1 accessibility, 2 mods. The message is used for narration and the tooltip. */
	public static EldervaleButton icon(int x, int y, int size, Component message, int iconIndex, OnPress onPress) {
		EldervaleButton b = new EldervaleButton(x, y, size, message, iconIndex, onPress);
		b.setTooltip(Tooltip.create(message));
		return b;
	}

	@Override
	public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		long now = System.nanoTime();
		float dt = lastNanos == 0L ? 0.016F : Math.min(0.1F, (now - lastNanos) / 1.0E9F);
		lastNanos = now;
		float target = (this.active && this.isHoveredOrFocused()) ? 1.0F : 0.0F;
		hoverT += (target - hoverT) * (1.0F - (float) Math.exp(-dt * 14.0F));

		int state = !this.active ? 2 : (hoverT > 0.5F ? 1 : 0);

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
		UiKit.nineSlice(g, UiKit.BUTTON, getX(), getY(), this.width, this.height, 0, state * 20, 48, 20, 48, 60, 6);
		if (iconIndex >= 0) {
			int row = (this.active && hoverT > 0.5F) ? 16 : 0;
			float a = this.active ? this.alpha : this.alpha * 0.45F;
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, a);
			g.blit(UiKit.ICONS, getX() + (this.width - 16) / 2, getY() + (this.height - 16) / 2, (float) (iconIndex * 16), (float) row, 16, 16, 48, 32);
		}
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		if (iconIndex < 0) {
			Font font = Minecraft.getInstance().font;
			int rgb = this.active ? UiKit.lerpColor(UiKit.TEXT & 0xFFFFFF, UiKit.TEXT_HOVER & 0xFFFFFF, hoverT) : (UiKit.TEXT_DISABLED & 0xFFFFFF);
			int color = UiKit.withAlpha(rgb, this.alpha);
			Component label = this.getMessage();
			int tx = getX() + (this.width - font.width(label)) / 2;
			int ty = getY() + (this.height - 8) / 2 + TEXT_Y_OFFSET;
			g.drawString(font, label, tx, ty, color, true);
		}
	}
}
