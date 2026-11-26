package net.mcreator.eldervale.client.vfx.effects;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/** Mage passive: ARCANE MASTERY. One orbiting orb of arcane power per stack (max 3) for the 4 s the bonus lasts. */
public final class MageMasteryVfx extends ScriptedVfx {

	private static final int DURATION = 80;

	public MageMasteryVfx(VfxEventPayload p) {
		super(VfxIds.MAGE_MASTERY, p, 1.0F);
		final int stacks = Math.max(1, Math.min(3, p.arg()));

		layer(0, DURATION, (g, f, u) -> {
			float a = VfxMath.clamp01(f.t / 5.0F) * (1.0F - VfxMath.smooth(0.85F, 1.0F, u));
			for (int k = 0; k < stacks; k++) {
				double ang = f.t * 0.16D + k * VfxMath.TAU / stacks;
				double r = 0.95D, h = 1.05D + 0.35D * Math.sin(f.t * 0.12D + k * 2.0D);
				double ox = f.x + Math.cos(ang) * r, oy = f.y + h, oz = f.z + Math.sin(ang) * r;
				VfxKit.orb(g, ox, oy, oz, 0.12D, a, VfxKit.ARCANE, VfxKit.WHITE);
				// short arc behind each orb
				g.color(VfxKit.ARCANE, VfxKit.WHITE);
				g.arc(f.x, f.y + h, f.z, r - 0.025D, r + 0.025D, ang - 0.9D, ang, 0.0F, 0.7F * a, g.seg(10));
			}
		});
		layer(0, 8, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 1.8F, u, 0.3D, VfxKit.ARCANE, VfxKit.WHITE, 0.5F, 0.02D));

		sound(0, "mage_stack", 0.55F, 0.9F + 0.12F * stacks);
	}
}
