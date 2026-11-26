package net.mcreator.eldervale.client.vfx.effects;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Assassin skill 2: SMOKE BOMB. A dark cloud (radius 4) that stays on the spot for 5 s:
 * billowing smoke volumes (dark, translucent) lit by violet light, a shadow rune on the ground and rising wisps.
 */
public final class AssassinSmokeVfx extends ScriptedVfx {

	private static final float[] DARK = {0.10F, 0.06F, 0.16F};
	private static final int BILLOWS = 14;

	public AssassinSmokeVfx(VfxEventPayload p) {
		super(VfxIds.ASSASSIN_SMOKE, p, 4.0F);
		this.follow = false;
		final float R = this.radius;
		final int life = p.arg() > 0 ? p.arg() : 100;
		final int seed = this.seed;

		// smoke volumes: burst out from the center, then hover and slowly swirl
		shadeLayer(0, life + 20, (g, f, u) -> {
			float t = f.t;
			float out = 1.0F - VfxMath.smooth(life - 12.0F, life + 20.0F, t);
			float grow = VfxMath.easeOut(VfxMath.clamp01(t / 14.0F));
			g.color(VfxKit.SMOKE, DARK);
			for (int i = 0; i < BILLOWS; i++) {
				double ang = VfxMath.hash(seed, i) * VfxMath.TAU + t * (0.012D + 0.008D * VfxMath.hash(seed, i + 5));
				double rr = (0.4D + 0.55D * VfxMath.hash(seed, i + 20)) * R * 0.85D * grow;
				double hh = 0.5D + 1.3D * VfxMath.hash(seed, i + 40) + 0.12D * Math.sin(t * 0.06D + i);
				double br = (0.9D + 0.7D * VfxMath.hash(seed, i + 60)) * (0.6D + 0.4D * grow);
				float a = 0.55F * out;
				g.sphere(f.x + Math.cos(ang) * rr, f.y + hh, f.z + Math.sin(ang) * rr, br, 5, g.seg(14), a, 0.18F * a, 1.4F, false);
			}
			// low dense layer on the ground
			g.disc(f.x, f.y + 0.05D, f.z, R * grow, 0.45F * out, 0.0F, g.seg(32));
		});

		// light inside the smoke
		layer(0, 12, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, R * 1.2F, u, 1.0D, VfxKit.SHADOW, VfxKit.WHITE, 0.9F, 0.1D));
		layer(0, 14, (g, f, u) -> {
			g.color(VfxKit.SHADOW, VfxKit.WHITE);
			g.halo(f.x, f.y + 0.9D, f.z, 4.5D * (1.0F - u) + 1.0D, 0.55F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(18));
		});
		layer(0, life + 20, (g, f, u) -> {
			float t = f.t;
			float out = 1.0F - VfxMath.smooth(life - 12.0F, life + 20.0F, t);
			float in = VfxMath.clamp01(t / 10.0F);
			float a = out * in;
			float pulse = 0.8F + 0.2F * (float) Math.sin(t * 0.17F);
			VfxKit.glyph(g, f.x, f.y, f.z, R * 0.95D, 0.7F * a * pulse, t * 0.05F, 7, 3, VfxKit.SHADOW, VfxKit.ARCANE);
			// the edge of the cloud breathes
			float k = (t % 25.0F) / 25.0F;
			g.color(VfxKit.SHADOW, VfxKit.WHITE);
			g.arc(f.x, f.y + 0.03D, f.z, R * (0.6D + 0.4D * k) - 0.1D, R * (0.6D + 0.4D * k), 0.0D, VfxMath.TAU, 0.0F, 0.5F * a * (1.0F - k), g.seg(48));
			VfxKit.rays(g, f.x, f.y, f.z, R, 14, t, seed, 0.8F * a, VfxKit.SHADOW, VfxKit.ARCANE);
		});

		sound(0, "assassin_smoke_cast", 1.0F, 1.0F);
		sound(8, "assassin_smoke_loop", 0.55F, 1.0F);
		sound(56, "assassin_smoke_loop", 0.45F, 1.0F);
		sound(life, "assassin_smoke_end", 0.8F, 1.0F);
		flash(0, VfxKit.SHADOW, 0.18F, 8);
		vignette(0, VfxKit.SHADOW, 0.10F, 40);
		shake(0, 1.5F, 8, 14.0F, false);
		holdUntil(life + 22);
	}
}
