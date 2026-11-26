package net.mcreator.eldervale.client.vfx.effects;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/** Ranger passive: CRAZY REFLEX (speed for 3 s after taking damage). Wind streaks and a quick ring. */
public final class RangerReflexVfx extends ScriptedVfx {

	public RangerReflexVfx(VfxEventPayload p) {
		super(VfxIds.RANGER_REFLEX, p, 2.0F);

		layer(0, 10, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 2.4F, u, 0.4D, VfxKit.SKY, VfxKit.WHITE, 0.7F, 0.03D));
		layer(0, 60, (g, f, u) -> {
			float a = (1.0F - VfxMath.smooth(0.55F, 1.0F, u)) * VfxMath.clamp01(f.t / 4.0F);
			VfxKit.trail(g, f.trail, 12, 0.3D, 0.5F * a, 0.0F, VfxKit.SKY, VfxKit.WHITE, 0.9D);
			g.color(VfxKit.SKY, VfxKit.WHITE);
			for (int k = 0; k < 5; k++) {
				float h = VfxMath.hash(f.seed, k);
				double ang = h * VfxMath.TAU + f.t * 0.2D;
				float v = ((f.t * 0.12F + h * 3.0F) % 1.0F);
				g.sliver(f.x + Math.cos(ang) * 0.6D, f.y + 0.2D + v * 1.7D, f.z + Math.sin(ang) * 0.6D, 0.0D, 1.0D, 0.0D, 0.7D, 0.02D, 0.55F * a * (float) Math.sin(Math.PI * v));
			}
		});
		sound(0, "ranger_reflex", 0.7F, 1.0F);
		fov(0, 3.0F);
	}
}
