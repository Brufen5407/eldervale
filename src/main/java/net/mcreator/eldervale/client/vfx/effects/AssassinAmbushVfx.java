package net.mcreator.eldervale.client.vfx.effects;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Assassin passive: DEADLY AMBUSH. A quick shadow slash on the enemy that was not looking at the assassin.
 * START: position = the enemy that was hit, dir = direction of the hit.
 */
public final class AssassinAmbushVfx extends ScriptedVfx {

	public AssassinAmbushVfx(VfxEventPayload p) {
		super(VfxIds.ASSASSIN_AMBUSH, p, 1.5F);
		this.follow = false;
		final double hx = p.x(), hy = p.y(), hz = p.z();
		final double dx = p.dx(), dz = p.dz();
		final double dl = Math.max(1.0E-3D, Math.sqrt(dx * dx + dz * dz));
		final double fx = dx / dl, fz = dz / dl;
		final int seed = this.seed;

		layer(0, 8, (g, f, u) -> {
			VfxKit.slash(g, hx - fx * 0.5D, hy, hz - fz * 0.5D, fx, fz, 0.6D + 0.5D * VfxMath.hash(seed, 1), 1.0D, 0.3D, u, 0.9F, VfxKit.SHADOW, VfxKit.WHITE);
			g.color(VfxKit.SHADOW, VfxKit.WHITE);
			g.burst(hx, hy, hz, 7, 1.2D * VfxMath.easeOut(u), 0.07D * (1.0F - u), 0.85F * (1.0F - u), seed * 0.01D);
			g.halo(hx, hy, hz, 1.6D * VfxMath.easeOut(u), 0.45F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(12));
		});
		sound(0, "assassin_ambush", 0.8F, 1.0F);
		shake(0, 1.0F, 4, 10.0F, false);
	}
}
