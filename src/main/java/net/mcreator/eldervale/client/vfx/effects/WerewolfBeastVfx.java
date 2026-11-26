package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Werewolf skill 1: BEAST TRANSFORMATION (8 s). Started and stopped by VfxAuras from the synced variable werewolf_mode.
 * The change of shape: moonlight column, claw slashes and a bone-shaking shockwave; then a feral aura until the form ends.
 */
public final class WerewolfBeastVfx extends ScriptedVfx {

	private int closeTick = -1;

	public WerewolfBeastVfx(VfxEventPayload p) {
		super(VfxIds.WEREWOLF_BEAST, p, 3.0F);
		holdUntil(1_000_000);
		final int seed = this.seed;

		// transformation burst
		layer(0, 22, (g, f, u) -> VfxKit.pillar(g, f.x, f.y, f.z, 9.0D, 0.9D, u, VfxKit.MOON, VfxKit.WHITE, 0.9F));
		layer(0, 18, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 5.0F, u, 1.2D, VfxKit.FERAL, VfxKit.WHITE, 1.0F, 0.12D));
		layer(3, 16, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 3.4F, u, 0.6D, VfxKit.MOON, VfxKit.WHITE, 0.8F, 0.06D));
		layer(0, 26, (g, f, u) -> {
			float a = (1.0F - VfxMath.smooth(0.4F, 1.0F, u)) * VfxMath.clamp01(u * 8.0F);
			VfxKit.glyph(g, f.x, f.y, f.z, 2.6D, 0.9F * a, f.t * 0.2F, 5, 2, VfxKit.MOON, VfxKit.WHITE);
		});
		// three claw slashes tearing through the air in front of the body
		for (int k = 0; k < 3; k++) {
			final int kk = k;
			layer(2 + k * 2, 9, (g, f, u) -> {
				double off = (kk - 1) * 0.35D;
				VfxKit.slash(g, f.x + f.hz * off, f.y + 1.1D, f.z - f.hx * off, f.hx, f.hz, 0.7D, 1.3D, 0.28D, u, 1.0F, VfxKit.FERAL, VfxKit.WHITE);
			});
		}

		// feral aura until the form ends
		layer(0, 1_000_000, (g, f, u) -> {
			float fade = closeTick < 0 ? 1.0F : 1.0F - VfxMath.clamp01((f.t - closeTick) / 12.0F);
			float a = fade * VfxMath.clamp01(f.t / 10.0F);
			float pulse = 0.8F + 0.2F * (float) Math.sin(f.t * 0.25F);
			g.color(VfxKit.FERAL, VfxKit.WHITE);
			for (int k = 0; k < 12; k++) {
				float period = 14.0F + VfxMath.hash(f.seed, k) * 12.0F;
				float v = ((f.t + VfxMath.hash(f.seed, k + 30) * period) % period) / period;
				double ang = VfxMath.hash(f.seed, k + 60) * VfxMath.TAU + f.t * 0.06D;
				double rr = 0.4D + VfxMath.hash(f.seed, k + 90) * 0.4D;
				g.sliver(f.x + Math.cos(ang) * rr, f.y + 0.2D + v * 1.9D, f.z + Math.sin(ang) * rr, Math.cos(ang) * 0.2D, 1.0D, Math.sin(ang) * 0.2D, 1.0D, 0.05D, 0.75F * a * (float) Math.sin(Math.PI * v));
			}
			g.color(VfxKit.MOON, VfxKit.WHITE);
			g.arc(f.x, f.y, f.z, 0.95D, 1.05D, f.t * 0.08D, f.t * 0.08D + VfxMath.TAU, 0.0F, 0.55F * a * pulse, g.seg(36));
			g.disc(f.x, f.y + 0.01D, f.z, 1.3D, 0.16F * a * pulse, 0.0F, g.seg(18));
			g.halo(f.x, f.y + 1.0D, f.z, 1.9D * pulse, 0.12F * a, 0.0F, g.seg(16));
		});

		sound(0, "werewolf_transform_on", 1.0F, 1.0F);
		flash(0, VfxKit.MOON, 0.24F, 10);
		fov(0, -4.0F);
		shake(0, 3.5F, 14, 22.0F, false);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() == VfxIds.END && closeTick < 0) {
			closeTick = now();
			sound(now(), "werewolf_transform_off", 0.8F, 1.0F);
			endAt(now() + 14);
			final int t0 = now();
			layer(t0, 12, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 2.8F, u, 0.5D, VfxKit.MOON, VfxKit.WHITE, 0.6F, 0.04D));
		}
	}
}
