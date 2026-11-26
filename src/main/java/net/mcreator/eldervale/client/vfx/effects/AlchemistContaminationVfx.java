package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Alchemist passive: CONTAMINATION. The effect belongs to the contaminated enemy (its entity id): START = a stack was added
 * (arg = stacks 1-2, param = height of the enemy): toxic orbs circle above its head. IMPACT = the 3rd stack, the burst.
 */
public final class AlchemistContaminationVfx extends ScriptedVfx {

	private static final int TIMEOUT = 200;
	private boolean bursting;
	private int burstTick;

	public AlchemistContaminationVfx(VfxEventPayload p) {
		super(VfxIds.ALCHEMIST_CONTAMINATION, p, 2.0F);
		final int stacks = Math.max(1, Math.min(3, p.arg()));
		final float height = p.param() > 0.0F ? p.param() : 1.8F;

		layer(0, TIMEOUT, (g, f, u) -> {
			if (bursting)
				return;
			float a = VfxMath.clamp01(f.t / 5.0F) * (1.0F - VfxMath.smooth(0.9F, 1.0F, u));
			for (int k = 0; k < stacks; k++) {
				double ang = f.t * 0.12D + k * VfxMath.TAU / stacks;
				double r = 0.32D;
				VfxKit.orb(g, f.x + Math.cos(ang) * r, f.y + height + 0.35D + 0.05D * Math.sin(f.t * 0.2D + k), f.z + Math.sin(ang) * r, 0.1D, a, VfxKit.TOXIC, VfxKit.WHITE);
			}
			g.color(VfxKit.TOXIC, VfxKit.WHITE);
			g.arc(f.x, f.y + height + 0.35D, f.z, 0.30D, 0.34D, 0.0D, VfxMath.TAU, 0.0F, 0.4F * a, g.seg(20));
			// a faint toxic haze on the enemy that grows with the stacks
			g.halo(f.x, f.y + height * 0.5D, f.z, 0.9D + 0.25D * stacks, 0.05F * stacks * a, 0.0F, g.seg(12));
		});
		layer(0, 8, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 1.6F, u, 0.3D, VfxKit.TOXIC, VfxKit.WHITE, 0.5F, 0.02D));

		sound(0, "alchemist_stack", 0.6F, 0.85F + 0.15F * stacks);
		holdUntil(TIMEOUT);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() != VfxIds.IMPACT || bursting)
			return;
		bursting = true;
		lingerOnGone = true;
		burstTick = now();
		final int t0 = now();
		final int seed = this.seed;
		final float height = p.param() > 0.0F ? p.param() : 1.8F;

		layer(t0, 14, (g, f, u) -> VfxKit.shock(g, f.x, f.y + 0.04D, f.z, 3.2F, u, 0.8D, VfxKit.TOXIC, VfxKit.WHITE, 1.0F, 0.1D));
		layer(t0, 12, (g, f, u) -> {
			float e = VfxMath.easeOut(u), a = (1.0F - u) * (1.0F - u);
			g.color(VfxKit.TOXIC, VfxKit.WHITE);
			g.sphere(f.x, f.y + height * 0.5D, f.z, 0.4D + 2.2D * e, 8, g.seg(22), 0.08F * a, 0.9F * a, 1.7F, false);
			g.halo(f.x, f.y + height * 0.5D, f.z, 4.0D * e + 0.6D, 0.8F * a, 0.0F, g.seg(18));
			g.burst(f.x, f.y + height * 0.5D, f.z, 14, 3.0D * e, 0.14D * (1.0F - u), 0.9F * (1.0F - u), seed * 0.01D);
		});
		layer(t0, 16, (g, f, u) -> VfxKit.pillar(g, f.x, f.y, f.z, 4.5D, 0.6D, u, VfxKit.TOXIC, VfxKit.WHITE, 0.8F));

		sound(t0, "alchemist_contam_burst", 1.0F, 1.0F);
		flash(t0, VfxKit.TOXIC, 0.08F, 5);
		shake(t0, 1.8F, 8, 16.0F, false);
		endAt(t0 + 18);
	}
}
