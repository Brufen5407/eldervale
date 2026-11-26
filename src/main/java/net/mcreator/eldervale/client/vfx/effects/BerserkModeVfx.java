package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxGfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Berserker passive: BERSERK MODE (below 50% health). Created and stopped on the client by VfxAuras,
 * which watches the synced variable berserk_mode, so every player sees every berserker.
 * Blood-red flame-like blades of light rise around the body, a ring beats on the ground like a heart.
 */
public final class BerserkModeVfx extends ScriptedVfx {

	private static final int BEAT = 24;
	private int closeTick = -1;

	public BerserkModeVfx(VfxEventPayload p) {
		super(VfxIds.BERSERK_MODE, p, 2.0F);
		holdUntil(1_000_000);

		// arg = 1 means "already active when we first saw the player": no activation show, only the aura
		final boolean silent = p.arg() == 1;
		if (!silent) {
			layer(0, 16, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 4.0F, u, 0.9D, VfxKit.BLOOD, VfxKit.EMBER, 0.9F, 0.1D));
			layer(0, 18, (g, f, u) -> VfxKit.pillar(g, f.x, f.y, f.z, 5.0D, 0.5D, u, VfxKit.BLOOD, VfxKit.EMBER, 0.8F));
		}

		// aura that lasts until the mode ends
		layer(0, 1_000_000, (g, f, u) -> {
			float fade = closeTick < 0 ? 1.0F : 1.0F - VfxMath.clamp01((f.t - closeTick) / 10.0F);
			float in = VfxMath.clamp01(f.t / 10.0F);
			float a = fade * in;
			int beat = (int) (f.t % BEAT);
			float pulse = (float) Math.max(Math.exp(-beat / 3.0D), beat >= 6 ? 0.7D * Math.exp(-(beat - 6) / 3.0D) : 0.0D);
			g.color(VfxKit.BLOOD, VfxKit.EMBER);
			// flames: fast rising blades
			for (int k = 0; k < 14; k++) {
				float period = 12.0F + VfxMath.hash(f.seed, k) * 10.0F;
				float v = ((f.t + VfxMath.hash(f.seed, k + 30) * period) % period) / period;
				double ang = VfxMath.hash(f.seed, k + 60) * VfxMath.TAU + f.t * 0.05D;
				double rr = 0.35D + VfxMath.hash(f.seed, k + 90) * 0.35D;
				double hgt = 1.2D + VfxMath.hash(f.seed, k + 120) * 1.0D;
				float al = (float) Math.sin(Math.PI * v) * 0.8F * a;
				g.sliver(f.x + Math.cos(ang) * rr, f.y + 0.2D + v * hgt, f.z + Math.sin(ang) * rr, Math.cos(ang) * 0.15D, 1.0D, Math.sin(ang) * 0.15D, 0.9D + 0.6D * VfxMath.hash(f.seed, k + 150), 0.06D, al);
			}
			// heartbeat ring on the ground
			g.arc(f.x, f.y, f.z, 0.7D + pulse * 0.5D, 0.85D + pulse * 0.55D, 0.0D, VfxMath.TAU, 0.0F, (0.35F + 0.55F * pulse) * a, g.seg(32));
			g.disc(f.x, f.y + 0.01D, f.z, 1.1D, 0.18F * a * (0.6F + pulse), 0.0F, g.seg(18));
			g.halo(f.x, f.y + 1.0D, f.z, 1.7D + pulse * 0.4D, (0.12F + 0.14F * pulse) * a, 0.0F, g.seg(16));
			// red edges of the screen, beating (only for the berserker himself)
			if (f.local)
				net.mcreator.eldervale.client.vfx.VfxScreen.vignette(VfxKit.BLOOD, (0.10F + 0.20F * pulse) * a);
		});

		if (!silent) {
			sound(0, "berserker_mode_on", 1.0F, 1.0F);
			flash(0, VfxKit.BLOOD, 0.22F, 10);
			shake(0, 2.0F, 12, 18.0F, false);
		}
		// heartbeat, one every 1.2 s for about 8 minutes (the effect ends with the mode)
		for (int i = 1; i < 400; i++) {
			sound(i * BEAT, "berserker_heartbeat", 0.45F, 1.0F);
		}
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() == VfxIds.END && closeTick < 0) {
			closeTick = now();
			sound(now(), "berserker_mode_off", 0.7F, 1.0F);
			endAt(now() + 12);
		}
	}
}
