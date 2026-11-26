package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Monk skill 2/1: PUNCH BARRAGE. START = the teleport in front of the enemy (a streak of light and a lotus ring
 * at both ends). PULSE = each of the 5 punches (impact star, ring, light line from the monk's side).
 * The last punch (param = 1) is a golden finisher.
 */
public final class MonkBarrageVfx extends ScriptedVfx {

	public MonkBarrageVfx(VfxEventPayload p) {
		super(VfxIds.MONK_BARRAGE, p, 2.0F);
		this.follow = false;
		final double ox = p.x(), oy = p.y(), oz = p.z();
		final double dx = p.dx(), dy = p.dy(), dz = p.dz(), len = p.param();
		final double ex = ox + dx * len, ey = oy + dy * len, ez = oz + dz * len;
		final int seed = this.seed;

		// departure and arrival
		layer(0, 12, (g, f, u) -> {
			VfxKit.shock(g, ox, oy + 0.04D, oz, 2.4F, u, 0.5D, VfxKit.TEAL, VfxKit.WHITE, 0.8F, 0.04D);
			g.color(VfxKit.TEAL, VfxKit.WHITE);
			g.halo(ox, oy + 0.9D, oz, 2.0D * (1.0F - u) + 0.5D, 0.5F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(14));
		});
		layer(1, 14, (g, f, u) -> {
			VfxKit.shock(g, ex, ey + 0.04D, ez, 2.8F, u, 0.5D, VfxKit.GOLD, VfxKit.WHITE, 0.9F, 0.05D);
			VfxKit.glyph(g, ex, ey + 0.04D, ez, 1.5D, 0.8F * (1.0F - u), f.t * 0.2F, 5, 2, VfxKit.GOLD, VfxKit.WHITE);
			g.color(VfxKit.GOLD, VfxKit.WHITE);
			g.halo(ex, ey + 0.9D, ez, 2.2D * (1.0F - u) + 0.4D, 0.5F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(14));
		});
		// the streak between the two points
		layer(0, 12, (g, f, u) -> {
			float a = (1.0F - u) * (1.0F - u);
			g.color(VfxKit.TEAL, VfxKit.WHITE);
			g.beam(ox, oy + 1.0D, oz, ex, ey + 1.0D, ez, 0.14D * (1.0F - u) + 0.02D, 0.9F * a, 0.9F * a);
			g.beam(ox, oy + 0.5D, oz, ex, ey + 0.5D, ez, 0.06D, 0.5F * a, 0.5F * a);
			for (int k = 0; k < 8; k++) {
				double q = k / 8.0D;
				g.sliver(ox + (ex - ox) * q, oy + (ey - oy) * q + 0.4D + 1.1D * VfxMath.hash(seed, k), oz + (ez - oz) * q, dx, dy, dz, 1.0D, 0.03D, 0.7F * a);
			}
		});

		sound(0, "monk_barrage_start", 0.9F, 1.0F);
		flash(0, VfxKit.TEAL, 0.10F, 5);
		fov(0, 6.0F);
		holdUntil(40);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() != VfxIds.PULSE)
			return;
		final int t0 = now();
		final int index = p.arg();
		final boolean last = p.param() >= 1.0F;
		final double hx = p.x(), hy = p.y(), hz = p.z();
		final double side = index % 2 == 0 ? 0.3D : -0.3D;
		final int seed = this.seed + index * 31;

		layer(t0, 8, (g, f, u) -> {
			float a = 1.0F - u;
			g.color(last ? VfxKit.GOLD : VfxKit.TEAL, VfxKit.WHITE);
			g.burst(hx + side, hy, hz, last ? 14 : 8, (last ? 2.6D : 1.4D) * VfxMath.easeOut(u), (last ? 0.14D : 0.08D) * a, 0.95F * a, seed * 0.07D);
			g.halo(hx + side, hy, hz, (last ? 3.0D : 1.6D) * VfxMath.easeOut(u) + 0.2D, 0.65F * a * a, 0.0F, g.seg(14));
		});
		layer(t0, 10, (g, f, u) -> VfxKit.shock(g, hx, hy - 0.6D, hz, last ? 4.0F : 2.0F, u, last ? 0.9D : 0.4D, last ? VfxKit.GOLD : VfxKit.TEAL, VfxKit.WHITE, 0.8F, 0.04D));
		if (last) {
			layer(t0, 18, (g, f, u) -> VfxKit.pillar(g, hx, hy - 0.6D, hz, 7.0D, 0.55D, u, VfxKit.GOLD, VfxKit.WHITE, 0.9F));
			soundAtTarget(t0, "monk_barrage_end", 1.0F, 1.0F);
			flash(t0, VfxKit.GOLD, 0.20F, 9);
			shake(t0, 3.5F, 9, 18.0F, true);
		}
		soundAtTarget(t0, "monk_barrage_punch", 0.85F, 0.9F + index * 0.07F);
		shake(t0, 0.9F, 3, 12.0F, true);
		holdUntil(t0 + 22);
	}
}
