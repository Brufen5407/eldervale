package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Spellblade skill 1: ARCANE SLASH. START = the cast (position, horizontal direction, param = path length).
 * A crescent wave of arcane energy runs along the path with afterimages; each monster that is hit (IMPACT, arg = distance
 * along the path in tenths of a block) flashes when the wave reaches it.
 */
public final class SpellbladeSlashVfx extends ScriptedVfx {

	private static final int TRAVEL = 8;     // ticks the wave takes to run the whole path

	public SpellbladeSlashVfx(VfxEventPayload p) {
		super(VfxIds.SPELLBLADE_SLASH, p, 8.0F);
		this.follow = false;
		final double ox = p.x(), oy = p.y(), oz = p.z();
		final double fx = frame.hx, fz = frame.hz;
		final double sx = -fz, sz = fx;
		final float L = this.radius;
		final int seed = this.seed;

		// the swing next to the body
		layer(0, 8, (g, f, u) -> VfxKit.slash(g, ox, oy + 1.2D, oz, fx, fz, 0.12D, 1.7D, 0.5D, u, 1.0F, VfxKit.ARCANE_BLUE, VfxKit.WHITE));

		// the wave: three crescents (one bright, two afterimages) running forward
		layer(1, TRAVEL + 8, (g, f, u) -> {
			float t = f.t - 1.0F;
			for (int k = 0; k < 3; k++) {
				float lag = k * 0.9F;
				float s = VfxMath.clamp01((t - lag) / TRAVEL);
				if (s <= 0.0F)
					continue;
				double d = 0.6D + (L - 0.6D) * VfxMath.easeOut(s);
				float a = (k == 0 ? 1.0F : 0.55F / k) * (1.0F - VfxMath.smooth(0.7F, 1.0F, s));
				double cx = ox + fx * d, cz = oz + fz * d;
				g.color(VfxKit.ARCANE_BLUE, VfxKit.WHITE);
				g.crescent(cx, oy + 1.1D, cz, fx, 0.0D, fz, sx, 0.0D, sz, 1.9D, 0.8D * (k == 0 ? 1.0D : 0.6D), -1.15D, 1.15D, a, g.seg(26));
				if (k == 0) {
					g.color(VfxKit.WHITE, VfxKit.WHITE);
					g.crescent(cx, oy + 1.1D, cz, fx, 0.0D, fz, sx, 0.0D, sz, 1.93D, 0.2D, -1.1D, 1.1D, 0.95F * a, g.seg(26));
					g.color(VfxKit.CYAN, VfxKit.WHITE);
					g.halo(cx, oy + 1.1D, cz, 2.2D, 0.2F * a, 0.0F, g.seg(14));
				}
			}
		});
		// the scorched line on the ground and arcane sparks along the edges
		layer(1, 24, (g, f, u) -> {
			float a = (1.0F - VfxMath.smooth(0.2F, 1.0F, u)) * VfxMath.clamp01(u * 10.0F);
			float run = VfxMath.clamp01((f.t - 1.0F) / TRAVEL);
			double d = L * VfxMath.easeOut(run);
			g.color(VfxKit.ARCANE_BLUE, VfxKit.WHITE);
			g.flatLine(ox, oz, ox + fx * d, oz + fz * d, oy + 0.05D, 0.18D, 0.6F * a);
			g.flatLine(ox + sx * 1.4D, oz + sz * 1.4D, ox + fx * d + sx * 1.4D, oz + fz * d + sz * 1.4D, oy + 0.05D, 0.05D, 0.4F * a);
			g.flatLine(ox - sx * 1.4D, oz - sz * 1.4D, ox + fx * d - sx * 1.4D, oz + fz * d - sz * 1.4D, oy + 0.05D, 0.05D, 0.4F * a);
			for (int i = 0; i < 14; i++) {
				double q = VfxMath.hash(seed, i) * d;
				double side = (VfxMath.hash(seed, i + 20) - 0.5D) * 3.0D;
				float v = ((f.t * 0.08F + VfxMath.hash(seed, i + 40)) % 1.0F);
				g.sliver(ox + fx * q + sx * side, oy + 0.2D + v * 1.6D, oz + fz * q + sz * side, 0.0D, 1.0D, 0.0D, 0.6D, 0.025D, 0.65F * a * (float) Math.sin(Math.PI * v));
			}
		});
		// the end of the path
		layer(TRAVEL - 1, 12, (g, f, u) -> VfxKit.shock(g, ox + fx * L, oy + 0.04D, oz + fz * L, 2.4F, u, 0.5D, VfxKit.ARCANE_BLUE, VfxKit.WHITE, 0.6F, 0.04D));

		sound(0, "spellblade_slash", 1.0F, 1.0F);
		flash(0, VfxKit.ARCANE_BLUE, 0.10F, 5);
		fov(0, 4.0F);
		shake(0, 1.8F, 6, 14.0F, false);
		holdUntil(TRAVEL + 26);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() != VfxIds.IMPACT)
			return;
		// the monster flashes when the wave gets there
		final int when = now() + Math.round(TRAVEL * VfxMath.easeOutInverse(Math.min(1.0F, (p.arg() / 10.0F) / radius)));
		final double hx = p.x(), hy = p.y(), hz = p.z();
		final int seed = this.seed + p.arg();
		layer(when, 9, (g, f, u) -> {
			float a = 1.0F - u;
			g.color(VfxKit.ARCANE_BLUE, VfxKit.WHITE);
			g.burst(hx, hy, hz, 8, 1.5D * VfxMath.easeOut(u), 0.09D * a, 0.95F * a, seed * 0.07D);
			g.halo(hx, hy, hz, 2.0D * VfxMath.easeOut(u) + 0.3D, 0.6F * a * a, 0.0F, g.seg(14));
		});
		soundAt(when, "spellblade_slash_hit", hx, hy, hz, 0.7F, 0.9F + 0.2F * VfxMath.hash(seed, p.arg()));
		holdUntil(when + 12);
	}
}
