package net.mcreator.eldervale.client.vfx.effects;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxGfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/** Paladin skill 1: HOLY SHIELDS (5 s of invulnerability). A golden barrier that blinks in the last second. */
public final class PaladinShieldsVfx extends ScriptedVfx {

	private static final int DURATION = 100;

	public PaladinShieldsVfx(VfxEventPayload p) {
		super(VfxIds.PALADIN_SHIELDS, p, 1.3F);

		// cast: pillar, shockwave, glyph under the feet
		layer(0, 14, (g, f, u) -> VfxKit.pillar(g, f.x, f.y, f.z, 7.0D, 0.45D, u, VfxKit.GOLD, VfxKit.WHITE, 0.9F));
		layer(0, 14, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 3.2F, u, 0.7D, VfxKit.GOLD, VfxKit.WHITE, 0.9F, 0.06D));
		layer(0, DURATION, (g, f, u) -> {
			float a = (1.0F - VfxMath.smooth(0.85F, 1.0F, u)) * VfxMath.clamp01(f.t / 8.0F);
			VfxKit.glyph(g, f.x, f.y, f.z, 1.7D, 0.8F * a * blink(f.t), f.t * 0.05F, 6, 2, VfxKit.GOLD, VfxKit.WHITE);
		});

		// the barrier: fresnel sphere around the body, two tilted orbiting rings, soft heart light
		layer(0, DURATION, (g, f, u) -> {
			float a = VfxMath.clamp01(f.t / 6.0F) * blink(f.t);
			float pulse = 0.85F + 0.15F * (float) Math.sin(f.t * 0.3F);
			double cy = f.y + 0.95D;
			g.color(VfxKit.GOLD, VfxKit.HOLY);
			g.sphere(f.x, cy, f.z, 1.25D * (0.7D + 0.3D * VfxMath.easeOut(f.t / 8.0F)), 9, g.seg(28), 0.05F * a, 0.85F * a * pulse, 2.2F, false);
			double rot = f.t * 0.12D;
			for (int k = 0; k < 2; k++) {
				double tilt = 0.55D + k * 0.9D;
				double ux = Math.cos(rot * (k == 0 ? 1 : -1)), uz = Math.sin(rot * (k == 0 ? 1 : -1));
				double vx = -uz * Math.cos(tilt), vz = ux * Math.cos(tilt), vy = Math.sin(tilt);
				g.color(VfxKit.GOLD, VfxKit.WHITE);
				g.arcPlane(f.x, cy, f.z, ux, 0.0D, uz, vx, vy, vz, 1.28D, 1.38D, 0.0D, VfxMath.TAU, 0.0F, 0.9F * a, g.seg(40));
			}
			g.color(VfxKit.GOLD, VfxKit.WHITE);
			g.halo(f.x, cy, f.z, 1.6D, 0.22F * a, 0.0F, g.seg(18));
		});
		layer(0, DURATION, (g, f, u) -> VfxKit.rays(g, f.x, f.y, f.z, 1.0D, 6, f.t, f.seed, 0.8F * blink(f.t), VfxKit.GOLD, VfxKit.WHITE));

		// end: the barrier breaks into an outward shockwave
		layer(DURATION, 14, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 3.0F, u, 0.5D, VfxKit.GOLD, VfxKit.WHITE, 0.8F, 0.05D));
		layer(DURATION, 12, (g, f, u) -> {
			g.color(VfxKit.GOLD, VfxKit.WHITE);
			g.burst(f.x, f.y + 0.95D, f.z, 12, 2.2D * VfxMath.easeOut(u), 0.10D * (1.0F - u), 0.85F * (1.0F - u), f.seed * 0.01D);
			g.halo(f.x, f.y + 0.95D, f.z, 3.0D * VfxMath.easeOut(u), 0.5F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(20));
		});

		sound(0, "paladin_shield_start", 1.0F, 1.0F);
		sound(0, "paladin_shield_hum", 0.45F, 1.0F);
		sound(DURATION - 20, "paladin_shield_warn", 0.8F, 1.0F);
		sound(DURATION, "paladin_shield_end", 1.0F, 1.0F);
		flash(0, VfxKit.GOLD, 0.16F, 8);
		vignette(0, VfxKit.GOLD, 0.10F, DURATION);
		shake(0, 1.2F, 6, 14.0F, false);
		shake(DURATION, 1.5F, 8, 14.0F, false);
	}

	/** blink during the last second so the player knows the shield is about to end */
	private static float blink(float t) {
		if (t < DURATION - 20)
			return 1.0F;
		return ((int) (t / 2.0F)) % 2 == 0 ? 1.0F : 0.25F;
	}
}
