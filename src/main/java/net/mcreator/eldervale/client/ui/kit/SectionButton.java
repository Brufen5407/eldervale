package net.mcreator.eldervale.client.ui.kit;

import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;

/** 24x24 section tab used as a real widget (keyboard focus, narration, tooltip). */
public class SectionButton extends Button {

	private final int icon;

	public SectionButton(int x, int y, Component label, int icon, OnPress onPress) {
		super(x, y, 24, 24, label, onPress, DEFAULT_NARRATION);
		this.icon = icon;
		this.setTooltip(Tooltip.create(label));
	}

	@Override
	public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		boolean hot = this.active && this.isHoveredOrFocused();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
		UiKit.drawSectionTab(g, getX(), getY(), !this.active ? 2 : (hot ? 1 : 0), icon, hot);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
}
