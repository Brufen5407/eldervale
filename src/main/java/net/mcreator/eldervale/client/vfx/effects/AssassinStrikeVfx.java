package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Assassin skill 1: SHADOW STRIKE. START = the dash (position = where the assassin was, dir = toward where he lands,
 * param = distance, arg = 1 if the strike is the backstab). IMPACT = the target that is hit.
 */
public final class AssassinStrikeVfx extends ScriptedVfx {

	private final boolean bonus;
	private final double ox, oy, oz, ex, ey, ez;

	public AssassinStrikeVfx(VfxEventPayload p) {
		super(VfxIds.ASSASSIN_STRIKE, p, 2.0F);
		this.follow = false;
		this.bonus = p.arg() == 1;
		this.ox = p.x();
		this.oy = p.y();
		this.oz = p.z();
		this.ex = p.x() + p.dx() * p.param();
		this.ey = p.y() + p.dy() * p.param();
		this.ez = p.z() + p.dz() * p.param();
		final int seed = this.seed;

		// where he vanished: dark puff, violet ring and a thin pillar
		shadeLayer(0, 16, (g, f, u) -> {
			float a = (1.0F - u) * 0.55F;
			g.color(VfxKit.SMOKE, new float[]{0.10F, 0.06F, 0.16F});
			for (int k = 0; k < 5; k++) {
				double ang = k * VfxMath.TAU / 5.0D + VfxMath.hash(seed, k);
				double r = 0.9D * VfxMath.easeOut(u);
				g.sphere(ox + Math.cos(ang) * r, oy + 0.7D + 0.3D * k * 0.2D, oz + Math.sin(ang) * r, 0.5D + 0.5D * VfxMath.easeOut(u), 5, g.seg(12), 0.7F * a, 0.2F * a, 1.5F, false);
			}
		});
		layer(0, 12, (g, f, u) -> VfxKit.shock(g, ox, oy + 0.04D, oz, 2.6F, u, 0.5D, VfxKit.SHADOW, VfxKit.WHITE, 0.8F, 0.04D));
		layer(0, 10, (g, f, u) -> VfxKit.pillar(g, ox, oy, oz, 3.0D, 0.25D, u, VfxKit.SHADOW, VfxKit.WHITE, 0.7F));

		// the line of the dash: a bright cut of light that fades, with after-images along it
		layer(0, 14, (g, f, u) -> {
			float a = (1.0F - u) * (1.0F - u);
			g.color(VfxKit.SHADOW, VfxKit.WHITE);
			g.beam(ox, oy + 1.0D, oz, ex, ey + 1.0D, ez, 0.16D * (1.0F - u) + 0.02D, 0.9F * a, 0.9F * a);
			g.beam(ox, oy + 0.4D, oz, ex, ey + 0.4D, ez, 0.07D, 0.5F * a, 0.5F * a);
			for (int k = 1; k < 6; k++) {
				double q = k / 6.0D;
				double px = ox + (ex - ox) * q, py = oy + (ey - oy) * q, pz = oz + (ez - oz) * q;
				g.halo(px, py + 1.0D, pz, 1.2D * (1.0F - u), 0.28F * a, 0.0F, g.seg(10));
			}
		});

		sound(0, "assassin_strike_dash", 0.9F, 1.0F);
		flash(0, VfxKit.SHADOW, 0.10F, 5);
		fov(0, 6.0F);
		holdUntil(24);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() != VfxIds.IMPACT)
			return;
		final int t0 = now();
		final boolean crit = bonus;
		final int seed = this.seed;
		// strike direction: from where the assassin lands toward the target
		final double fx = p.x() - ex, fz = p.z() - ez;
		final double fl = Math.max(1.0E-3D, Math.sqrt(fx * fx + fz * fz));
		final double nx = fx / fl, nz = fz / fl;

		// X of two slashes on the target
		layer(t0, 10, (g, f, u) -> {
			double cx = f.tx - nx * 0.4D, cz = f.tz - nz * 0.4D;
			VfxKit.slash(g, cx, f.ty + 1.0D, cz, nx, nz, 0.9D, 1.2D, 0.35D, u, 1.0F, crit ? VfxKit.BLOOD : VfxKit.SHADOW, VfxKit.WHITE);
			VfxKit.slash(g, cx, f.ty + 1.0D, cz, nx, nz, -0.9D, 1.2D, 0.35D, Math.max(0.0F, u - 0.15F) / 0.85F, 1.0F, crit ? VfxKit.BLOOD : VfxKit.SHADOW, VfxKit.WHITE);
			if (crit)
				VfxKit.slash(g, cx, f.ty + 1.0D, cz, nx, nz, 0.0D, 1.4D, 0.4D, Math.max(0.0F, u - 0.3F) / 0.7F, 1.0F, VfxKit.BLOOD, VfxKit.WHITE);
		});
		layer(t0, 9, (g, f, u) -> {
			g.color(crit ? VfxKit.BLOOD : VfxKit.SHADOW, VfxKit.WHITE);
			g.burst(f.tx, f.ty + 1.0D, f.tz, crit ? 14 : 9, (crit ? 2.4D : 1.7D) * VfxMath.easeOut(u), 0.10D * (1.0F - u), 0.95F * (1.0F - u), seed * 0.01D);
			g.halo(f.tx, f.ty + 1.0D, f.tz, (crit ? 3.2D : 2.2D) * VfxMath.easeOut(u), 0.6F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(16));
		});
		layer(t0, 12, (g, f, u) -> VfxKit.shock(g, f.tx, f.ty + 0.04D, f.tz, crit ? 3.4F : 2.4F, u, 0.5D, crit ? VfxKit.BLOOD : VfxKit.SHADOW, VfxKit.WHITE, 0.8F, 0.05D));

		soundAtTarget(t0, "assassin_strike_hit", 1.0F, 1.0F);
		if (crit) {
			soundAtTarget(t0 + 1, "assassin_strike_crit", 0.9F, 1.0F);
			flash(t0, VfxKit.BLOOD, 0.20F, 8);
		}
		shake(t0, crit ? 3.0F : 1.8F, 6, 14.0F, true);
		holdUntil(t0 + 16);
	}
}
