package net.mcreator.eldervale.client.vfx.effects;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Ranger skill 1: DECOY. The ranger dissolves into light (invisible and fast for 5 s) while the decoy appears
 * where he stood. The decoy keeps a pulsing lure circle for the 8 s it holds the monsters' attention.
 */
public final class RangerDecoyVfx extends ScriptedVfx {

	private static final int LURE_TICKS = 160;

	public RangerDecoyVfx(VfxEventPayload p) {
		super(VfxIds.RANGER_DECOY, p, 2.5F);
		this.follow = false;
		final int seed = this.seed;

		// the ranger vanishes: a shell of light collapses onto him, then a ring flies out
		layer(0, 10, (g, f, u) -> {
			float a = 1.0F - u;
			g.color(VfxKit.LEAF, VfxKit.WHITE);
			g.sphere(f.x, f.y + 0.9D, f.z, 2.0D * (1.0F - VfxMath.easeIn(u)) + 0.2D, 8, g.seg(24), 0.04F * a, 0.9F * a, 2.0F, false);
			g.halo(f.x, f.y + 0.9D, f.z, 1.6D * (1.0F - u) + 0.4D, 0.5F * a, 0.0F, g.seg(16));
		});
		layer(8, 14, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 4.0F, u, 0.8D, VfxKit.LEAF, VfxKit.WHITE, 0.9F, 0.08D));
		layer(8, 12, (g, f, u) -> {
			g.color(VfxKit.LEAF, VfxKit.WHITE);
			for (int k = 0; k < 12; k++) {
				double ang = VfxMath.hash(seed, k) * VfxMath.TAU;
				double r = 0.4D + 3.0D * VfxMath.easeOut(u) * (0.5D + VfxMath.hash(seed, k + 20));
				g.sliver(f.x + Math.cos(ang) * r, f.y + 0.4D + 1.4D * VfxMath.hash(seed, k + 40) + u * 0.8D, f.z + Math.sin(ang) * r, Math.cos(ang), 0.15D, Math.sin(ang), 0.8D, 0.03D, 0.8F * (1.0F - u));
			}
		});
		// the decoy appears
		layer(4, 14, (g, f, u) -> VfxKit.pillar(g, f.x, f.y, f.z, 3.5D, 0.4D, u, VfxKit.TEAL, VfxKit.WHITE, 0.8F));

		// lure circle: slow rune ring and a pulse every second
		layer(10, LURE_TICKS, (g, f, u) -> {
			float a = VfxMath.clamp01(u * 10.0F) * (1.0F - VfxMath.smooth(0.9F, 1.0F, u));
			VfxKit.glyph(g, f.x, f.y, f.z, 1.7D, 0.7F * a, f.t * 0.04F, 3, 1, VfxKit.EMBER, VfxKit.WHITE);
			float k = (f.t % 20.0F) / 20.0F;
			g.color(VfxKit.EMBER, VfxKit.WHITE);
			g.arc(f.x, f.y + 0.02D, f.z, 0.3D + 2.4D * k - 0.1D, 0.3D + 2.4D * k, 0.0D, VfxMath.TAU, 0.0F, 0.7F * a * (1.0F - k), g.seg(40));
		});
		// the decoy fades away
		layer(LURE_TICKS + 10, 14, (g, f, u) -> {
			VfxKit.shock(g, f.x, f.y, f.z, 2.5F, u, 0.6D, VfxKit.LEAF, VfxKit.WHITE, 0.8F, 0.05D);
			g.color(VfxKit.LEAF, VfxKit.WHITE);
			g.halo(f.x, f.y + 1.0D, f.z, 2.0D * VfxMath.easeOut(u), 0.5F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(16));
		});

		sound(0, "ranger_decoy_cast", 1.0F, 1.0F);
		for (int i = 1; i <= 7; i++)
			sound(10 + i * 20, "ranger_decoy_pulse", 0.35F, 1.0F);
		sound(LURE_TICKS + 10, "ranger_decoy_end", 0.8F, 1.0F);
		flash(0, VfxKit.LEAF, 0.12F, 6);
		shake(0, 0.8F, 5, 10.0F, false);
	}
}
