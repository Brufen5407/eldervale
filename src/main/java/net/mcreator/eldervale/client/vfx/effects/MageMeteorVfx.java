package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Mage skill 2: METEOR STRIKE. START = the cast around the mage. IMPACT (sent right after, with the target point)
 * = warning circle, falling meteor for 'arg' ticks (the server uses 20), then the explosion at the target.
 */
public final class MageMeteorVfx extends ScriptedVfx {

	private static final double START_HEIGHT = 14.0D;

	public MageMeteorVfx(VfxEventPayload p) {
		super(VfxIds.MAGE_METEOR, p, 4.0F);

		// the mage gathers power: a rune circle rises around him and a column of fire-colored light
		layer(0, 24, (g, f, u) -> {
			float a = VfxMath.clamp01(u * 6.0F) * (1.0F - VfxMath.smooth(0.6F, 1.0F, u));
			VfxKit.glyph(g, f.x, f.y, f.z, 1.9D, 0.9F * a, f.t * 0.2F, 5, 2, VfxKit.FIRE, VfxKit.WHITE);
			g.color(VfxKit.FIRE, VfxKit.WHITE);
			g.cone(f.x, f.y, f.y + 3.0D, f.z, 0.7D, 0.3D, g.seg(16), 0.35F * a, 0.0F);
		});
		layer(0, 12, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 2.8F, u, 0.5D, VfxKit.FIRE, VfxKit.WHITE, 0.7F, 0.05D));

		sound(0, "mage_meteor_cast", 1.0F, 1.0F);
		flash(0, VfxKit.FIRE, 0.12F, 6);
		shake(0, 1.5F, 8, 14.0F, false);
		holdUntil(40);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() != VfxIds.IMPACT)
			return;
		final int t0 = now();
		final int fall = p.arg() > 0 ? p.arg() : 20;
		final float R = p.param() > 0.0F ? p.param() : radius;
		final int tHit = t0 + fall;
		final int seed = this.seed;

		// warning circle on the ground: closes in until the meteor lands
		layer(t0, fall, (g, f, u) -> {
			float pulse = 0.7F + 0.3F * (float) Math.sin(f.t * 0.9F);
			VfxKit.glyph(g, f.tx, f.ty + 0.04D, f.tz, R, 0.85F * VfxMath.clamp01(u * 8.0F) * pulse, f.t * 0.12F, 6, 2, VfxKit.FIRE, VfxKit.WHITE);
			float k = 1.0F - u;
			g.color(VfxKit.BLOOD, VfxKit.FIRE);
			g.arc(f.tx, f.ty + 0.05D, f.tz, R * (1.0D + 0.9D * k) - 0.12D, R * (1.0D + 0.9D * k), 0.0D, VfxMath.TAU, 0.0F, 0.9F, g.seg(56));
			g.disc(f.tx, f.ty + 0.045D, f.tz, R * 0.9D, 0.20F * u, 0.0F, g.seg(28));
		});

		// the falling meteor: fireball with a long ribbon of fire behind it
		layer(t0, fall, (g, f, u) -> {
			double h = START_HEIGHT * (1.0D - u * u);
			double mx = f.tx, my = f.ty + h, mz = f.tz;
			double tail = Math.min(7.0D, 1.5D + 6.0D * u);
			g.color(VfxKit.FIRE, VfxKit.WHITE);
			g.stripBegin();
			for (int i = 0; i <= 12; i++) {
				double k = i / 12.0D;
				g.stripPoint(mx + Math.sin(k * 9.0D + f.t) * 0.12D * k, my + tail * k, mz + Math.cos(k * 9.0D + f.t) * 0.12D * k);
			}
			g.stripEnd(1.6D, 0.0F, 0.95F);
			g.color(VfxKit.EMBER, VfxKit.WHITE);
			g.stripBegin();
			for (int i = 0; i <= 10; i++) {
				double k = i / 10.0D;
				g.stripPoint(mx, my + tail * 0.7D * k, mz);
			}
			g.stripEnd(0.7D, 0.0F, 1.0F);
			VfxKit.orb(g, mx, my, mz, 0.9D, 1.0F, VfxKit.FIRE, VfxKit.WHITE);
			g.color(VfxKit.FIRE, VfxKit.WHITE);
			g.halo(mx, my, mz, 3.4D, 0.45F, 0.0F, g.seg(20));
			// beam from the ground to the meteor so the player sees where it lands
			g.beam(f.tx, f.ty + 0.05D, f.tz, mx, my, mz, 0.05D, 0.35F * u, 0.0F);
		});

		// impact
		layer(tHit, 22, (g, f, u) -> VfxKit.shock(g, f.tx, f.ty + 0.04D, f.tz, R * 1.5F, u, 1.6D, VfxKit.FIRE, VfxKit.EMBER, 1.0F, 0.22D));
		layer(tHit + 2, 18, (g, f, u) -> VfxKit.shock(g, f.tx, f.ty + 0.04D, f.tz, R, u, 0.7D, VfxKit.BLOOD, VfxKit.WHITE, 0.8F, 0.1D));
		layer(tHit, 26, (g, f, u) -> VfxKit.pillar(g, f.tx, f.ty, f.tz, 16.0D, 1.4D, u, VfxKit.FIRE, VfxKit.WHITE, 1.0F));
		layer(tHit, 14, (g, f, u) -> {
			float e = VfxMath.easeOut(u), a = (1.0F - u) * (1.0F - u);
			g.color(VfxKit.FIRE, VfxKit.WHITE);
			g.sphere(f.tx, f.ty + 0.5D, f.tz, 0.5D + R * 1.1D * e, 9, g.seg(28), 0.10F * a, 0.9F * a, 1.6F, false);
			g.halo(f.tx, f.ty + 1.0D, f.tz, 7.0D * e + 1.0D, 0.9F * a, 0.0F, g.seg(24));
			g.burst(f.tx, f.ty + 1.0D, f.tz, 18, 5.0D * e, 0.22D * (1.0F - u), 0.95F * (1.0F - u), seed * 0.01D);
		});
		// crater: cracks and a glowing patch that cools down
		layer(tHit, 70, (g, f, u) -> {
			float a = (1.0F - VfxMath.smooth(0.15F, 1.0F, u)) * VfxMath.clamp01(u * 15.0F);
			g.color(VfxKit.FIRE, VfxKit.EMBER);
			for (int k = 0; k < 16; k++) {
				double ang = k * VfxMath.TAU / 16.0D + VfxMath.hash(seed, k) * 0.3D;
				double len = R * (0.6D + 0.55D * VfxMath.hash(seed, k + 40)) * VfxMath.easeOut(u * 6.0F);
				g.spoke(f.tx, f.ty + 0.05D, f.tz, ang, 0.4D, len, 0.07D + 0.05D * VfxMath.hash(seed, k + 80), 0.9F * a, 0.1F * a);
			}
			g.disc(f.tx, f.ty + 0.045D, f.tz, R * 0.65D, 0.45F * a, 0.0F, g.seg(28));
			VfxKit.rays(g, f.tx, f.ty, f.tz, R * 0.8D, 10, f.t, seed, a, VfxKit.FIRE, VfxKit.EMBER);
		});

		soundAtTarget(t0, "mage_meteor_fall", 1.2F, 1.0F);
		soundAtTarget(tHit, "mage_meteor_impact", 1.6F, 1.0F);
		flash(tHit, VfxKit.FIRE, 0.38F, 14);
		fov(tHit, 7.0F);
		shake(tHit, 6.5F, 18, 34.0F, true);
		shake(t0, 0.8F, fall, 20.0F, true);
		holdUntil(tHit + 75);
	}
}
