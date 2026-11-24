package net.mcreator.eldervale.client.vfx;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Render types for the VFX system.
 *
 *  - GLOW  : additive blending (light adds up), no depth write, no culling. Order does not matter,
 *            so many overlapping shapes never produce sorting artifacts. Used for every glowing shape.
 *  - SHADE : normal translucent blending, for dark or smoky shapes (shadows, smoke, blood).
 *
 * This class extends RenderType only to reach its protected render state shards (standard trick).
 * It is never instantiated.
 */
public final class VfxRenderTypes extends RenderType {

	private static final RenderStateShard.TransparencyStateShard ADDITIVE = new RenderStateShard.TransparencyStateShard("eldervale_additive", () -> {
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
	}, () -> {
		RenderSystem.disableBlend();
		RenderSystem.defaultBlendFunc();
	});

	public static final RenderType GLOW = create("eldervale_vfx_glow", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536, false, false,
			RenderType.CompositeState.builder()
					.setShaderState(POSITION_COLOR_SHADER)
					.setTransparencyState(ADDITIVE)
					.setCullState(NO_CULL)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setWriteMaskState(COLOR_WRITE)
					.createCompositeState(false));

	public static final RenderType SHADE = create("eldervale_vfx_shade", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 1536, false, false,
			RenderType.CompositeState.builder()
					.setShaderState(POSITION_COLOR_SHADER)
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setCullState(NO_CULL)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setWriteMaskState(COLOR_WRITE)
					.createCompositeState(false));

	private VfxRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
		super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
		throw new UnsupportedOperationException("not instantiable");
	}
}
