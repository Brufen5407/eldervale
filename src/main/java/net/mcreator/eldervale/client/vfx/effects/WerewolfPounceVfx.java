package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/** Werewolf skill 2: FERAL POUNCE. START = the leap (feral ribbons and a take-off burst). IMPACT = the landing (claw rakes and shockwave, radius 3.5). */
public final class WerewolfPounceVfx extends ScriptedVfx {

	private static final int FLIGHT = 14;

	public WerewolfPounceVfx(VfxEventPayload p) {
		super(VfxIds.WEREWOLF_POUNCE, p, 3.5F);
		final double sx = p.x(), sy = p.y() + 0.04D, sz = p.z();

		layer(0, 10, (g, f, u) -> VfxKit.shock(g, sx, sy, sz, 2.6F, u, 0.6D, VfxKit.FERAL, VfxKit.WHITE, 0.8F, 0.05D));
		layer(0, FLIGHT + 8, (g, f, u) -> {
			float a = 1.0F - VfxMath.smooth(0.55F, 1.0F, u);
			VfxKit.trail(g, f.trail, 16, 0.7D, 0.85F * a, 0.0F, VfxKit.FERAL, VfxKit.WHITE, 0.9D);
			VfxKit.trail(g, f.trail, 14, 0.4D, 0.7F * a, 0.0F, VfxKit.MOON, VfxKit.WHITE, 0.5D);
			g.color(VfxKit.FERAL, VfxKit.WHITE);
			g.halo(f.x, f.y + 0.9D, f.z, 1.6D, 0.22F * a, 0.0F, g.seg(14));
		});
		sound(0, "werewolf_pounce_launch", 1.0F, 1.0F);
		fov(0, 6.0F);
		shake(0, 1.5F, 6, 14.0F, false);
		holdUntil(FLIGHT + 40);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() != VfxIds.IMPACT)
			return;
		final int t0 = now();
		final float R = p.param() > 0.0F ? p.param() : radius;
		final int seed = this.seed;

		layer(t0, 16, (g, f, u) -> VfxKit.shock(g, f.tx, f.ty + 0.04D, f.tz, R * 1.2F, u, 1.1D, VfxKit.FERAL, VfxKit.EMBER, 1.0F, 0.14D));
		layer(t0 + 2, 14, (g, f, u) -> VfxKit.shock(g, f.tx, f.ty + 0.04D, f.tz, R * 0.8F, u, 0.5D, VfxKit.MOON, VfxKit.WHITE, 0.8F, 0.06D));
		layer(t0, 14, (g, f, u) -> VfxKit.pillar(g, f.tx, f.ty, f.tz, 4.0D, 0.7D, u, VfxKit.FERAL, VfxKit.WHITE, 0.8F));
		// claw rakes on the ground: four parallel scratches that glow and fade
		layer(t0, 40, (g, f, u) -> {
			float a = (1.0F - VfxMath.smooth(0.2F, 1.0F, u)) * VfxMath.clamp01(u * 14.0F);
			double ang = VfxMath.hash(seed, 3) * VfxMath.TAU;
			double cx = Math.cos(ang), cz = Math.sin(ang);
			g.color(VfxKit.FERAL, VfxKit.WHITE);
			for (int k = -2; k <= 1; k++) {
				double off = (k + 0.5D) * 0.55D;
				double ox = f.tx - cz * off, oz = f.tz + cx * off;
				double len = R * 0.9D * VfxMath.easeOut(u * 6.0F);
				g.flatLine(ox - cx * len, oz - cz * len, ox + cx * len, oz + cz * len, f.ty + 0.05D, 0.09D, 0.95F * a);
			}
			g.disc(f.tx, f.ty + 0.045D, f.tz, R * 0.5D, 0.3F * a, 0.0F, g.seg(22));
		});
		layer(t0, 9, (g, f, u) -> {
			g.color(VfxKit.FERAL, VfxKit.WHITE);
			g.burst(f.tx, f.ty + 0.6D, f.tz, 12, 2.8D * VfxMath.easeOut(u), 0.13D * (1.0F - u), 0.9F * (1.0F - u), seed * 0.01D);
			g.halo(f.tx, f.ty + 0.6D, f.tz, 3.6D * VfxMath.easeOut(u), 0.6F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(18));
		});

		soundAtTarget(t0, "werewolf_pounce_land", 1.3F, 1.0F);
		flash(t0, VfxKit.FERAL, 0.20F, 9);
		fov(t0, 4.0F);
		shake(t0, 4.5F, 12, 24.0F, true);
		holdUntil(t0 + 44);
	}
}
