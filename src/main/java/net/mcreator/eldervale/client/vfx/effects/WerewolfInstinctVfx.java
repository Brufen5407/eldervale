package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.client.vfx.VfxScreen;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/** Werewolf passive: PREDATOR'S INSTINCT (below 50% HP). Started and stopped by VfxAuras. A slow crimson pulse of hunger around the body. */
public final class WerewolfInstinctVfx extends ScriptedVfx {

	private int closeTick = -1;

	public WerewolfInstinctVfx(VfxEventPayload p) {
		super(VfxIds.WEREWOLF_INSTINCT, p, 2.0F);
		holdUntil(1_000_000);

		layer(0, 14, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 3.4F, u, 0.7D, VfxKit.BLOOD, VfxKit.WHITE, 0.8F, 0.06D));
		layer(0, 1_000_000, (g, f, u) -> {
			float fade = closeTick < 0 ? 1.0F : 1.0F - VfxMath.clamp01((f.t - closeTick) / 10.0F);
			float a = fade * VfxMath.clamp01(f.t / 10.0F);
			float beat = (float) Math.pow(0.5D + 0.5D * Math.sin(f.t * 0.16D), 3.0D);
			g.color(VfxKit.BLOOD, VfxKit.FERAL);
			g.arc(f.x, f.y, f.z, 0.85D + 0.25D * beat, 0.97D + 0.28D * beat, 0.0D, VfxMath.TAU, 0.0F, (0.35F + 0.4F * beat) * a, g.seg(32));
			for (int k = 0; k < 6; k++) {
				float period = 18.0F + VfxMath.hash(f.seed, k) * 12.0F;
				float v = ((f.t + VfxMath.hash(f.seed, k + 30) * period) % period) / period;
				double ang = VfxMath.hash(f.seed, k + 60) * VfxMath.TAU;
				g.sliver(f.x + Math.cos(ang) * 0.6D, f.y + 0.3D + v * 1.7D, f.z + Math.sin(ang) * 0.6D, 0.0D, 1.0D, 0.0D, 0.8D, 0.04D, 0.6F * a * (float) Math.sin(Math.PI * v));
			}
			g.halo(f.x, f.y + 1.0D, f.z, 1.5D + 0.3D * beat, (0.08F + 0.10F * beat) * a, 0.0F, g.seg(14));
			if (f.local)
				VfxScreen.vignette(VfxKit.BLOOD, (0.06F + 0.10F * beat) * a);
		});
		sound(0, "werewolf_instinct", 0.8F, 1.0F);
		shake(0, 1.2F, 8, 14.0F, false);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() == VfxIds.END && closeTick < 0) {
			closeTick = now();
			endAt(now() + 12);
		}
	}
}
