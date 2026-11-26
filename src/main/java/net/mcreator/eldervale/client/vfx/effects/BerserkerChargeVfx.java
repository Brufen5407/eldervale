package net.mcreator.eldervale.client.vfx.effects;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/** Berserker skill 2: BERSERK CHARGE. Shockwave at the start point, then a burning dash with speed lines. */
public final class BerserkerChargeVfx extends ScriptedVfx {

	private static final int DASH_TICKS = 14;

	public BerserkerChargeVfx(VfxEventPayload p) {
		super(VfxIds.BERSERKER_CHARGE, p, 5.0F);
		final float R = this.radius;
		final double startX = p.x(), startY = p.y() + 0.04D, startZ = p.z();

		// shockwave that pushes everything away (stays at the start point)
		layer(0, 14, (g, f, u) -> VfxKit.shock(g, startX, startY, startZ, R, u, 1.0D, VfxKit.EMBER, VfxKit.WHITE, 0.9F, 0.12D));
		layer(0, 8, (g, f, u) -> {
			g.color(VfxKit.FIRE, VfxKit.WHITE);
			g.halo(startX, startY + 0.9D, startZ, 3.0D * VfxMath.easeOut(u), 0.6F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(20));
		});

		// the dash: ribbons that follow the body and speed lines pointing backwards
		layer(0, DASH_TICKS + 10, (g, f, u) -> {
			float a = 1.0F - VfxMath.smooth(0.5F, 1.0F, u);
			VfxKit.trail(g, f.trail, 18, 0.9D, 0.9F * a, 0.0F, VfxKit.FIRE, VfxKit.EMBER, 1.1D);
			VfxKit.trail(g, f.trail, 16, 0.6D, 0.8F * a, 0.0F, VfxKit.BLOOD, VfxKit.EMBER, 0.6D);
			VfxKit.trail(g, f.trail, 14, 0.4D, 0.7F * a, 0.0F, VfxKit.EMBER, VfxKit.WHITE, 0.25D);
			g.color(VfxKit.EMBER, VfxKit.WHITE);
			for (int k = 0; k < 10; k++) {
				float h = VfxMath.hash(f.seed, k);
				double side = (VfxMath.hash(f.seed, k + 20) - 0.5D) * 2.4D;
				double up = 0.2D + VfxMath.hash(f.seed, k + 40) * 1.6D;
				double back = 0.6D + h * 2.2D + (f.t * 0.35D) % 1.5D;
				g.sliver(f.x - f.hx * back - f.hz * side, f.y + up, f.z - f.hz * back + f.hx * side, -f.hx, 0.0D, -f.hz, 1.4D + h, 0.03D, 0.8F * a);
			}
			g.halo(f.x, f.y + 0.9D, f.z, 1.8D, 0.28F * a, 0.0F, g.seg(16));
		});

		sound(0, "berserker_charge", 1.0F, 1.0F);
		sound(3, "berserker_charge_hit", 0.8F, 1.0F);
		flash(0, VfxKit.FIRE, 0.14F, 6);
		fov(0, 10.0F);
		vignette(0, VfxKit.BLOOD, 0.14F, DASH_TICKS);
		shake(0, 2.5F, 8, 16.0F, false);
	}
}
