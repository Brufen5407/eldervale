package net.mcreator.eldervale.client.vfx.effects;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/** Mage skill 1: ARCANE NOVA. An arcane explosion around the mage that pushes everything out to the radius (5). */
public final class MageNovaVfx extends ScriptedVfx {

	public MageNovaVfx(VfxEventPayload p) {
		super(VfxIds.MAGE_NOVA, p, 5.0F);
		final float R = this.radius;

		// expanding shell of arcane light and the flash at the heart
		layer(0, 12, (g, f, u) -> {
			float e = VfxMath.easeOut(u), a = (1.0F - u) * (1.0F - u);
			g.color(VfxKit.ARCANE, VfxKit.WHITE);
			g.sphere(f.x, f.y + 1.0D, f.z, 0.6D + (R - 0.6D) * e, 10, g.seg(32), 0.05F * a, 0.95F * a, 1.8F, false);
			g.halo(f.x, f.y + 1.0D, f.z, 5.0D * (1.0F - u) + 1.0D, 0.8F * a, 0.0F, g.seg(24));
		});
		layer(0, 16, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, R * 1.1F, u, 1.2D, VfxKit.ARCANE, VfxKit.WHITE, 1.0F, 0.12D));
		layer(2, 16, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, R * 0.85F, u, 0.6D, VfxKit.ARCANE_BLUE, VfxKit.WHITE, 0.8F, 0.06D));
		layer(0, 14, (g, f, u) -> VfxKit.pillar(g, f.x, f.y, f.z, 7.0D, 0.7D, u, VfxKit.ARCANE, VfxKit.WHITE, 0.9F));

		// vertical rings that open outward like a gyroscope
		layer(0, 18, (g, f, u) -> {
			float e = VfxMath.easeOut(u), a = (1.0F - u);
			g.color(VfxKit.ARCANE_BLUE, VfxKit.WHITE);
			for (int k = 0; k < 3; k++) {
				double yaw = k * Math.PI / 3.0D + f.t * 0.1D;
				g.arcPlane(f.x, f.y + 1.0D, f.z, Math.cos(yaw), 0.0D, Math.sin(yaw), 0.0D, 1.0D, 0.0D, R * 0.9D * e - 0.12D, R * 0.9D * e, 0.0D, VfxMath.TAU, 0.0F, 0.9F * a, g.seg(44));
			}
		});
		// rune circle on the ground that burns out
		layer(0, 30, (g, f, u) -> {
			float a = (1.0F - VfxMath.smooth(0.3F, 1.0F, u)) * VfxMath.clamp01(u * 10.0F);
			VfxKit.glyph(g, f.x, f.y, f.z, R, a, f.t * 0.25F * (1.0F - u * 0.7F), 7, 3, VfxKit.ARCANE, VfxKit.WHITE);
		});
		// star burst
		layer(0, 9, (g, f, u) -> {
			g.color(VfxKit.ARCANE, VfxKit.WHITE);
			g.burst(f.x, f.y + 1.0D, f.z, 16, 3.8D * VfxMath.easeOut(u), 0.18D * (1.0F - u), 0.95F * (1.0F - u), f.seed * 0.01D);
		});

		sound(0, "mage_nova_cast", 1.0F, 1.0F);
		flash(0, VfxKit.ARCANE, 0.26F, 9);
		fov(0, 5.0F);
		shake(0, 4.0F, 12, 22.0F, false);
	}
}
