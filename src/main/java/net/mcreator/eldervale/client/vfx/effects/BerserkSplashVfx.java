package net.mcreator.eldervale.client.vfx.effects;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/** Berserk Mode splash: a jagged red bolt jumps from the target to the monster next to it. */
public final class BerserkSplashVfx extends ScriptedVfx {

	public BerserkSplashVfx(VfxEventPayload p) {
		super(VfxIds.BERSERK_SPLASH, p, 2.0F);
		this.follow = false;
		final double sx = p.x(), sy = p.y(), sz = p.z();
		final double len = p.param();
		final double dx = p.dx(), dy = p.dy(), dz = p.dz();
		final double ex = sx + dx * len, ey = sy + dy * len, ez = sz + dz * len;
		final int seed = this.seed;

		layer(0, 7, (g, f, u) -> {
			float a = 1.0F - u;
			g.color(VfxKit.BLOOD, VfxKit.WHITE);
			double px = sx, py = sy, pz = sz;
			int n = 6;
			for (int i = 1; i <= n; i++) {
				double k = i / (double) n;
				double j = (i == n ? 0.0D : 0.22D * (1.0F - u));
				double nx = sx + dx * len * k + (VfxMath.hash(seed, i) - 0.5D) * j * 2.0D;
				double ny = sy + dy * len * k + (VfxMath.hash(seed, i + 10) - 0.5D) * j * 2.0D;
				double nz = sz + dz * len * k + (VfxMath.hash(seed, i + 20) - 0.5D) * j * 2.0D;
				g.beam(px, py, pz, nx, ny, nz, 0.07D, 0.95F * a, 0.95F * a);
				px = nx;
				py = ny;
				pz = nz;
			}
		});
		layer(0, 8, (g, f, u) -> {
			g.color(VfxKit.FIRE, VfxKit.WHITE);
			g.halo(ex, ey, ez, 1.1D * VfxMath.easeOut(u), 0.6F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(14));
			g.burst(ex, ey, ez, 8, 0.9D * VfxMath.easeOut(u), 0.06D * (1.0F - u), 0.8F * (1.0F - u), seed * 0.01D);
		});
		sound(0, "berserker_splash", 0.8F, 1.0F);
	}
}
