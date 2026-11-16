package net.mcreator.eldervale.client.gui;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import net.mcreator.eldervale.world.inventory.ClassselectionMenu;
import net.mcreator.eldervale.init.EldervaleModScreens;

import com.mojang.blaze3d.systems.RenderSystem;

public class ClassselectionScreen extends AbstractContainerScreen<ClassselectionMenu> implements EldervaleModScreens.ScreenAccessor {
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private boolean menuStateUpdateActive = false;

	public ClassselectionScreen(ClassselectionMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 426;
		this.imageHeight = 240;
	}

	@Override
	public void updateMenuState(int elementType, String name, Object elementState) {
		menuStateUpdateActive = true;
		menuStateUpdateActive = false;
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
		guiTools$orderedImages : {
			if (true) {
				int guiTools$xOffset = 0;
				int guiTools$yOffset = 0;
				int guiTools$visibleWidth = 426;
				int guiTools$visibleHeight = 240;
				net.minecraft.resources.ResourceLocation guiTools$image = guiTools$dynamicTexture("", net.minecraft.resources.ResourceLocation.parse("eldervale:textures/screens/class_gui.png"));
				if (guiTools$image != null && guiTools$visibleWidth > 0 && guiTools$visibleHeight > 0)
					guiTools$alphaBlit(guiGraphics, guiTools$image, this.leftPos + 0 + guiTools$xOffset, this.topPos + 0 + guiTools$yOffset, 0, 0, guiTools$visibleWidth, guiTools$visibleHeight, 426, 240);
			}
			if (this.enhanced_image_button_2e37c814f84c44baa59c829d3b90 != null && this.enhanced_image_button_2e37c814f84c44baa59c829d3b90.visible) {
				this.enhanced_image_button_2e37c814f84c44baa59c829d3b90.render(guiGraphics, mouseX, mouseY, partialTicks);
			}
			if (this.enhanced_image_button_2fd35459beb545e6b77f3e6dede5 != null && this.enhanced_image_button_2fd35459beb545e6b77f3e6dede5.visible) {
				this.enhanced_image_button_2fd35459beb545e6b77f3e6dede5.render(guiGraphics, mouseX, mouseY, partialTicks);
			}
			if (this.enhanced_image_button_cd536f70213145929fe82b959bf4 != null && this.enhanced_image_button_cd536f70213145929fe82b959bf4.visible) {
				this.enhanced_image_button_cd536f70213145929fe82b959bf4.render(guiGraphics, mouseX, mouseY, partialTicks);
			}
		}
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	@Override
	public void init() {
		super.init();
		enhanced_image_button_2e37c814f84c44baa59c829d3b90 = new net.minecraft.client.gui.components.ImageButton(this.leftPos + 56, this.topPos + 102, 33, 33,
				new net.minecraft.client.gui.components.WidgetSprites(net.minecraft.resources.ResourceLocation.parse("eldervale:textures/screens/2e37c814-f84c-44ba-a59c-829d3b90edf6.png"),
						net.minecraft.resources.ResourceLocation.parse("eldervale:textures/screens/2e37c814-f84c-44ba-a59c-829d3b90edf6.png")),
				e -> {
					int x = ClassselectionScreen.this.x;
					int y = ClassselectionScreen.this.y;
					if (true) {
						net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.mcreator.eldervale.network.ClassselectionButtonMessage(0, x, y, z));
						net.mcreator.eldervale.network.ClassselectionButtonMessage.handleButtonAction(entity, 0, x, y, z);
					}
				}) {
			@Override
			public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				net.minecraft.resources.ResourceLocation guiTools$normalTexture = net.minecraft.resources.ResourceLocation.parse("eldervale:textures/screens/2e37c814-f84c-44ba-a59c-829d3b90edf6.png");
				net.minecraft.resources.ResourceLocation guiTools$hoveredTexture = guiTools$normalTexture;
				net.minecraft.resources.ResourceLocation guiTools$pressedTexture = guiTools$hoveredTexture;
				boolean mouseOverButton = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
				boolean mousePressed = mouseOverButton && org.lwjgl.glfw.GLFW.glfwGetMouseButton(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
				net.minecraft.resources.ResourceLocation buttonTexture = mousePressed ? guiTools$pressedTexture : mouseOverButton ? guiTools$hoveredTexture : guiTools$normalTexture;
				guiTools$alphaBlit(guiGraphics, buttonTexture, getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addWidget(enhanced_image_button_2e37c814f84c44baa59c829d3b90);
		enhanced_image_button_2fd35459beb545e6b77f3e6dede5 = new net.minecraft.client.gui.components.ImageButton(this.leftPos + 345, this.topPos + 102, 33, 33,
				new net.minecraft.client.gui.components.WidgetSprites(net.minecraft.resources.ResourceLocation.parse("eldervale:textures/screens/2fd35459-beb5-45e6-b77f-3e6dede51c82.png"),
						net.minecraft.resources.ResourceLocation.parse("eldervale:textures/screens/2fd35459-beb5-45e6-b77f-3e6dede51c82.png")),
				e -> {
					int x = ClassselectionScreen.this.x;
					int y = ClassselectionScreen.this.y;
					if (true) {
						net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.mcreator.eldervale.network.ClassselectionButtonMessage(1, x, y, z));
						net.mcreator.eldervale.network.ClassselectionButtonMessage.handleButtonAction(entity, 1, x, y, z);
					}
				}) {
			@Override
			public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				net.minecraft.resources.ResourceLocation guiTools$normalTexture = net.minecraft.resources.ResourceLocation.parse("eldervale:textures/screens/2fd35459-beb5-45e6-b77f-3e6dede51c82.png");
				net.minecraft.resources.ResourceLocation guiTools$hoveredTexture = guiTools$normalTexture;
				net.minecraft.resources.ResourceLocation guiTools$pressedTexture = guiTools$hoveredTexture;
				boolean mouseOverButton = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
				boolean mousePressed = mouseOverButton && org.lwjgl.glfw.GLFW.glfwGetMouseButton(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
				net.minecraft.resources.ResourceLocation buttonTexture = mousePressed ? guiTools$pressedTexture : mouseOverButton ? guiTools$hoveredTexture : guiTools$normalTexture;
				guiTools$alphaBlit(guiGraphics, buttonTexture, getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addWidget(enhanced_image_button_2fd35459beb545e6b77f3e6dede5);
		enhanced_image_button_cd536f70213145929fe82b959bf4 = new net.minecraft.client.gui.components.ImageButton(this.leftPos + 189, this.topPos + 190, 77, 28,
				new net.minecraft.client.gui.components.WidgetSprites(net.minecraft.resources.ResourceLocation.parse("eldervale:textures/screens/cd536f70-2131-4592-9fe8-2b959bf4c341.png"),
						net.minecraft.resources.ResourceLocation.parse("eldervale:textures/screens/cd536f70-2131-4592-9fe8-2b959bf4c341.png")),
				e -> {
					int x = ClassselectionScreen.this.x;
					int y = ClassselectionScreen.this.y;
					if (true) {
						net.neoforged.neoforge.network.PacketDistributor.sendToServer(new net.mcreator.eldervale.network.ClassselectionButtonMessage(2, x, y, z));
						net.mcreator.eldervale.network.ClassselectionButtonMessage.handleButtonAction(entity, 2, x, y, z);
					}
				}) {
			@Override
			public void renderWidget(net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
				net.minecraft.resources.ResourceLocation guiTools$normalTexture = net.minecraft.resources.ResourceLocation.parse("eldervale:textures/screens/cd536f70-2131-4592-9fe8-2b959bf4c341.png");
				net.minecraft.resources.ResourceLocation guiTools$hoveredTexture = guiTools$normalTexture;
				net.minecraft.resources.ResourceLocation guiTools$pressedTexture = guiTools$hoveredTexture;
				boolean mouseOverButton = mouseX >= getX() && mouseY >= getY() && mouseX < getX() + width && mouseY < getY() + height;
				boolean mousePressed = mouseOverButton && org.lwjgl.glfw.GLFW.glfwGetMouseButton(net.minecraft.client.Minecraft.getInstance().getWindow().getWindow(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
				net.minecraft.resources.ResourceLocation buttonTexture = mousePressed ? guiTools$pressedTexture : mouseOverButton ? guiTools$hoveredTexture : guiTools$normalTexture;
				guiTools$alphaBlit(guiGraphics, buttonTexture, getX(), getY(), 0, 0, width, height, width, height);
			}
		};
		this.addWidget(enhanced_image_button_cd536f70213145929fe82b959bf4);
	}

	private static final boolean guiTools$enhancedImageButton = true;
	private net.minecraft.client.gui.components.ImageButton enhanced_image_button_2e37c814f84c44baa59c829d3b90;
	private net.minecraft.client.gui.components.ImageButton enhanced_image_button_2fd35459beb545e6b77f3e6dede5;
	private net.minecraft.client.gui.components.ImageButton enhanced_image_button_cd536f70213145929fe82b959bf4;

	private static net.minecraft.resources.ResourceLocation guiTools$buttonTexture(String value, net.minecraft.resources.ResourceLocation fallback) {
		if (value == null || value.isBlank())
			return fallback;
		try {
			String texture = value.trim().replace('\\', '/');
			if (texture.indexOf(':') >= 0)
				return net.minecraft.resources.ResourceLocation.parse(texture);
			while (texture.startsWith("/"))
				texture = texture.substring(1);
			if (texture.startsWith("textures/screens/"))
				texture = texture.substring("textures/screens/".length());
			if (!texture.endsWith(".png"))
				texture += ".png";
			return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eldervale", "textures/screens/" + texture);
		} catch (RuntimeException ignored) {
			return fallback;
		}
	}

	private static net.minecraft.resources.ResourceLocation guiTools$dynamicTexture(String value, net.minecraft.resources.ResourceLocation fallback) {
		if (value == null || value.isBlank())
			return fallback;
		try {
			String texture = value.trim().replace('\\', '/');
			if (texture.indexOf(':') >= 0)
				return net.minecraft.resources.ResourceLocation.parse(texture);
			while (texture.startsWith("/"))
				texture = texture.substring(1);
			if (texture.startsWith("textures/screens/"))
				texture = texture.substring("textures/screens/".length());
			if (!texture.endsWith(".png"))
				texture += ".png";
			return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eldervale", "textures/screens/" + texture);
		} catch (RuntimeException ignored) {
			return fallback;
		}
	}

	private static void guiTools$alphaBlit(net.minecraft.client.gui.GuiGraphics graphics, net.minecraft.resources.ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		boolean wasBlending = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
		com.mojang.blaze3d.systems.RenderSystem.enableBlend();
		com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
		try {
			graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
		} finally {
			if (!wasBlending)
				com.mojang.blaze3d.systems.RenderSystem.disableBlend();
		}
	}
}