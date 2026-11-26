package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Berserker skill 1: BERSERK SLAM. A jump with a fiery trail, then the impact (IMPACT event, 1 s later).
 */
public final class BerserkerSlamVfx extends ScriptedVfx {

	private static final int JUMP_TICKS = 20;

	public BerserkerSlamVfx(VfxEventPayload p) {
		super(VfxIds.BERSERKER_SLAM, p, 3.5F);

		// take-off: small blast under the feet and a fiery ribbon behind the jump
		layer(0, 10, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 2.2F, u, 0.5D, VfxKit.FIRE, VfxKit.EMBER, 0.8F, 0.05D));
		layer(0, JUMP_TICKS + 4, (g, f, u) -> {
			float a = 1.0F - VfxMath.smooth(0.7F, 1.0F, u);
			VfxKit.trail(g, f.trail, 16, 0.55D, 0.85F * a, 0.0F, VfxKit.FIRE, VfxKit.EMBER, 0.5D);
			VfxKit.trail(g, f.trail, 12, 0.3D, 0.7F * a, 0.0F, VfxKit.BLOOD, VfxKit.EMBER, 0.2D);
			g.color(VfxKit.FIRE, VfxKit.EMBER);
			g.halo(f.x, f.y + 0.4D, f.z, 1.3D, 0.30F * a, 0.0F, g.seg(16));
		});

		sound(0, "berserker_slam_jump", 1.0F, 1.0F);
		flash(0, VfxKit.FIRE, 0.10F, 6);
		shake(0, 1.5F, 6, 14.0F, false);
		holdUntil(JUMP_TICKS + 40);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() != VfxIds.IMPACT)
			return;
		final int t0 = now();
		final float R = p.param() > 0.0F ? p.param() : radius;
		final int seed = this.seed;

		layer(t0, 18, (g, f, u) -> VfxKit.shock(g, f.tx, f.ty + 0.04D, f.tz, R * 1.25F, u, 1.3D, VfxKit.FIRE, VfxKit.EMBER, 1.0F, 0.18D));
		layer(t0 + 2, 16, (g, f, u) -> VfxKit.shock(g, f.tx, f.ty + 0.04D, f.tz, R, u, 0.6D, VfxKit.BLOOD, VfxKit.WHITE, 0.8F, 0.1D));
		layer(t0, 22, (g, f, u) -> VfxKit.pillar(g, f.tx, f.ty, f.tz, 6.5D, 0.9D, u, VfxKit.FIRE, VfxKit.EMBER, 0.9F));
		// cracks in the ground: radial lines of lava light that stay a while and cool down
		layer(t0, 44, (g, f, u) -> {
			float a = (1.0F - VfxMath.smooth(0.3F, 1.0F, u)) * VfxMath.clamp01(u * 12.0F);
			g.color(VfxKit.FIRE, VfxKit.EMBER);
			for (int k = 0; k < 14; k++) {
				double ang = k * VfxMath.TAU / 14.0D + VfxMath.hash(seed, k) * 0.35D;
				double len = R * (0.55D + 0.5D * VfxMath.hash(seed, k + 50)) * VfxMath.easeOut(u * 5.0F);
				g.spoke(f.tx, f.ty + 0.05D, f.tz, ang, 0.3D, len, 0.07D + 0.05D * VfxMath.hash(seed, k + 90), 0.95F * a, 0.1F * a);
			}
			g.disc(f.tx, f.ty + 0.045D, f.tz, R * 0.55D, 0.35F * a, 0.0F, g.seg(24));
		});
		// flash of the impact
		layer(t0, 10, (g, f, u) -> {
			g.color(VfxKit.FIRE, VfxKit.WHITE);
			g.burst(f.tx, f.ty + 0.6D, f.tz, 16, 3.2D * VfxMath.easeOut(u), 0.16D * (1.0F - u), 0.9F * (1.0F - u), seed * 0.01D);
			g.halo(f.tx, f.ty + 0.6D, f.tz, 4.5D * VfxMath.easeOut(u), 0.7F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(22));
		});

		soundAtTarget(t0, "berserker_slam_impact", 1.4F, 1.0F);
		flash(t0, VfxKit.FIRE, 0.30F, 10);
		fov(t0, 5.0F);
		shake(t0, 5.0F, 14, 26.0F, true);
	}
}
