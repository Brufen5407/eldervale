package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Alchemist skill 1: VOLATILE BREW. START = the throw (arg = the potion entity id: its flight gets a glowing trail).
 * IMPACT = the potion bursts into a toxic cloud (arg = cloud duration in ticks, param = radius).
 */
public final class AlchemistBrewVfx extends ScriptedVfx {

	private static final float[] DARK = {0.05F, 0.16F, 0.03F};

	private final int potionId;
	private final Trail potionTrail = new Trail(12);
	private boolean potionGone;
	private double px, py, pz;

	public AlchemistBrewVfx(VfxEventPayload p) {
		super(VfxIds.ALCHEMIST_BREW, p, 4.0F);
		this.potionId = p.arg();
		this.px = p.x();
		this.py = p.y() + 1.4D;
		this.pz = p.z();
		potionTrail.push(px, py, pz);

		layer(0, 80, (g, f, u) -> {
			if (potionGone && f.t > 70)
				return;
			float a = potionGone ? 0.0F : 1.0F;
			VfxKit.trail(g, potionTrail, 12, 0.35D, 0.9F * a, 0.0F, VfxKit.TOXIC, VfxKit.WHITE, 0.0D);
			if (!potionGone)
				VfxKit.orb(g, px, py, pz, 0.16D, 0.9F, VfxKit.TOXIC, VfxKit.WHITE);
		});

		sound(0, "alchemist_throw", 0.8F, 1.0F);
		holdUntil(60);
	}

	@Override
	protected void onTick(ClientLevel level) {
		if (potionGone)
			return;
		Entity e = level.getEntity(potionId);
		if (e == null) {
			potionGone = true;
			return;
		}
		px = e.getX();
		py = e.getY();
		pz = e.getZ();
		potionTrail.push(px, py, pz);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() != VfxIds.IMPACT)
			return;
		final int t0 = now();
		final int life = p.arg() > 0 ? p.arg() : 80;
		final float R = p.param() > 0.0F ? p.param() : radius;
		final int seed = this.seed;
		potionGone = true;

		// burst
		layer(t0, 14, (g, f, u) -> VfxKit.shock(g, f.tx, f.ty + 0.04D, f.tz, R * 1.1F, u, 1.0D, VfxKit.TOXIC, VfxKit.WHITE, 0.9F, 0.1D));
		layer(t0, 10, (g, f, u) -> {
			g.color(VfxKit.TOXIC, VfxKit.WHITE);
			g.halo(f.tx, f.ty + 0.6D, f.tz, 3.8D * VfxMath.easeOut(u), 0.7F * (1.0F - u) * (1.0F - u), 0.0F, g.seg(18));
			g.burst(f.tx, f.ty + 0.6D, f.tz, 12, 2.6D * VfxMath.easeOut(u), 0.12D * (1.0F - u), 0.85F * (1.0F - u), seed * 0.01D);
		});

		// the cloud: dense dark-green volumes lit from inside
		shadeLayer(t0, life + 18, (g, f, u) -> {
			float t = f.t - t0;
			float out = 1.0F - VfxMath.smooth(life - 10.0F, life + 18.0F, t);
			float grow = VfxMath.easeOut(VfxMath.clamp01(t / 12.0F));
			g.color(new float[]{0.22F, 0.55F, 0.10F}, DARK);
			for (int i = 0; i < 12; i++) {
				double ang = VfxMath.hash(seed, i) * VfxMath.TAU + t * 0.015D;
				double rr = (0.3D + 0.6D * VfxMath.hash(seed, i + 20)) * R * 0.85D * grow;
				double hh = 0.4D + 1.1D * VfxMath.hash(seed, i + 40) + 0.1D * Math.sin(t * 0.07D + i);
				double br = (0.8D + 0.6D * VfxMath.hash(seed, i + 60)) * (0.6D + 0.4D * grow);
				float a = 0.5F * out;
				g.sphere(f.tx + Math.cos(ang) * rr, f.ty + hh, f.tz + Math.sin(ang) * rr, br, 5, g.seg(14), a, 0.15F * a, 1.4F, false);
			}
			g.disc(f.tx, f.ty + 0.05D, f.tz, R * grow, 0.40F * out, 0.0F, g.seg(30));
		});
		layer(t0, life + 18, (g, f, u) -> {
			float t = f.t - t0;
			float out = 1.0F - VfxMath.smooth(life - 10.0F, life + 18.0F, t);
			float in = VfxMath.clamp01(t / 8.0F);
			float a = out * in;
			VfxKit.glyph(g, f.tx, f.ty, f.tz, R * 0.95D, 0.65F * a, t * 0.05F, 6, 2, VfxKit.TOXIC, VfxKit.ACID);
			// bubbles of light rising through the cloud
			for (int i = 0; i < 14; i++) {
				float period = 22.0F + VfxMath.hash(seed, i + 100) * 20.0F;
				float v = ((t + VfxMath.hash(seed, i + 120) * period) % period) / period;
				double ang = VfxMath.hash(seed, i + 140) * VfxMath.TAU;
				double rr = VfxMath.hash(seed, i + 160) * R * 0.85D;
				VfxKit.orb(g, f.tx + Math.cos(ang) * rr, f.ty + 0.2D + v * 2.2D, f.tz + Math.sin(ang) * rr, 0.07D + 0.08D * VfxMath.hash(seed, i + 180), 0.8F * a * (float) Math.sin(Math.PI * v), VfxKit.TOXIC, VfxKit.WHITE);
			}
			g.color(VfxKit.TOXIC, VfxKit.WHITE);
			float k = (t % 25.0F) / 25.0F;
			g.arc(f.tx, f.ty + 0.03D, f.tz, R * (0.5D + 0.5D * k) - 0.1D, R * (0.5D + 0.5D * k), 0.0D, VfxMath.TAU, 0.0F, 0.45F * a * (1.0F - k), g.seg(44));
			VfxKit.dome(g, f.tx, f.ty, f.tz, R, 0.14F * a, 0.0F, VfxKit.TOXIC, VfxKit.ACID);
		});

		soundAtTarget(t0, "alchemist_brew_burst", 1.0F, 1.0F);
		soundAtTarget(t0 + 8, "alchemist_brew_loop", 0.6F, 1.0F);
		soundAtTarget(t0 + 50, "alchemist_brew_loop", 0.5F, 1.0F);
		soundAtTarget(t0 + life, "alchemist_brew_end", 0.7F, 1.0F);
		shake(t0, 1.5F, 6, 14.0F, true);
		holdUntil(t0 + life + 20);
	}
}
