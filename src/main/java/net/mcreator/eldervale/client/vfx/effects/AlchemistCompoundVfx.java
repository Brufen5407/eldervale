package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Alchemist skill 2: REACTIVE COMPOUND. START = the throw (arg = potion entity id, trail). IMPACT = it sticks to the enemy
 * (arg = enemy entity id, 0 = it fizzled; param = fuse ticks): an unstable glow that pulses faster and faster.
 * END = the reaction: explosion (param = radius).
 */
public final class AlchemistCompoundVfx extends ScriptedVfx {

	private final int potionId;
	private final Trail potionTrail = new Trail(12);
	private boolean potionGone;
	private double px, py, pz;
	private int targetId;
	private boolean stuck;
	private int stickTick;
	private int fuse = 60;

	public AlchemistCompoundVfx(VfxEventPayload p) {
		super(VfxIds.ALCHEMIST_COMPOUND, p, 3.0F);
		this.potionId = p.arg();
		this.px = p.x();
		this.py = p.y() + 1.4D;
		this.pz = p.z();
		potionTrail.push(px, py, pz);

		layer(0, 80, (g, f, u) -> {
			if (potionGone)
				return;
			VfxKit.trail(g, potionTrail, 12, 0.35D, 0.9F, 0.0F, VfxKit.FIRE, VfxKit.WHITE, 0.0D);
			VfxKit.orb(g, px, py, pz, 0.16D, 0.9F, VfxKit.FIRE, VfxKit.WHITE);
		});
		sound(0, "alchemist_compound_throw", 0.8F, 1.0F);
		holdUntil(90);
	}

	@Override
	protected void onTick(ClientLevel level) {
		if (!potionGone) {
			Entity e = level.getEntity(potionId);
			if (e == null)
				potionGone = true;
			else {
				px = e.getX();
				py = e.getY();
				pz = e.getZ();
				potionTrail.push(px, py, pz);
			}
		}
		// the target moves: remember where the glow should be if it disappears
		if (stuck && targetId != 0) {
			Entity t = level.getEntity(targetId);
			if (t != null) {
				px = t.getX();
				py = t.getY(0.6D);
				pz = t.getZ();
			}
		}
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		final int t0 = now();
		final int seed = this.seed;
		if (p.phase() == VfxIds.IMPACT) {
			potionGone = true;
			if (p.arg() == 0) {
				// it hit a block or something else: a small fizzle
				shadeLayer(t0, 16, (g, f, u) -> {
					g.color(VfxKit.SMOKE, new float[]{0.1F, 0.08F, 0.08F});
					g.sphere(f.tx, f.ty + 0.3D + 0.5D * u, f.tz, 0.5D + 0.6D * u, 5, g.seg(12), 0.45F * (1.0F - u), 0.1F, 1.4F, false);
				});
				layer(t0, 8, (g, f, u) -> {
					g.color(VfxKit.FIRE, VfxKit.WHITE);
					g.halo(f.tx, f.ty + 0.2D, f.tz, 1.4D * VfxMath.easeOut(u), 0.5F * (1.0F - u), 0.0F, g.seg(12));
				});
				soundAtTarget(t0, "alchemist_brew_end", 0.6F, 1.3F);
				holdUntil(t0 + 20);
				return;
			}
			stuck = true;
			targetId = p.arg();
			stickTick = t0;
			fuse = p.param() > 0.0F ? (int) p.param() : 60;
			layer(t0, 10, (g, f, u) -> {
				g.color(VfxKit.FIRE, VfxKit.WHITE);
				g.halo(px, py, pz, 1.6D * VfxMath.easeOut(u), 0.6F * (1.0F - u), 0.0F, g.seg(12));
			});
			// unstable glow on the target, faster and brighter as the fuse burns
			layer(t0, fuse + 2, (g, f, u) -> {
				float t = f.t - stickTick;
				float k = t / fuse;
				float rate = 0.35F + 1.6F * k * k;
				float pulse = 0.5F + 0.5F * (float) Math.sin(t * rate * 2.0F);
				float a = 0.5F + 0.5F * k;
				VfxKit.orb(g, px, py, pz, 0.35D + 0.25D * k + 0.1D * pulse, (0.5F + 0.5F * pulse) * a, VfxKit.FIRE, VfxKit.WHITE);
				g.color(VfxKit.FIRE, VfxKit.WHITE);
				g.arcPlane(px, py, pz, 1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.95D - 0.4D * k, 1.02D - 0.4D * k, t * 0.2D, t * 0.2D + VfxMath.TAU, 0.0F, 0.7F * a, g.seg(28));
				g.arcPlane(px, py, pz, 1.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, 1.1D - 0.5D * k, 1.17D - 0.5D * k, -t * 0.25D, -t * 0.25D + VfxMath.TAU, 0.0F, 0.6F * a, g.seg(28));
				for (int i = 0; i < 6; i++) {
					double ang = VfxMath.hash(seed, i) * VfxMath.TAU + t * 0.2D;
					float v = ((t * 0.07F * rate + VfxMath.hash(seed, i + 9)) % 1.0F);
					g.sliver(px + Math.cos(ang) * 0.5D, py - 0.4D + v * 1.2D, pz + Math.sin(ang) * 0.5D, 0.0D, 1.0D, 0.0D, 0.6D, 0.03D, 0.7F * a * (float) Math.sin(Math.PI * v));
				}
			});
			soundAtTarget(t0, "alchemist_compound_stick", 0.9F, 1.0F);
			// ticking that speeds up
			int[] ticks = {fuse - 40, fuse - 28, fuse - 18, fuse - 11, fuse - 6, fuse - 3};
			for (int i = 0; i < ticks.length; i++)
				soundAtTarget(t0 + Math.max(2, ticks[i]), "alchemist_compound_tick", 0.6F, 0.8F + i * 0.1F);
			holdUntil(t0 + fuse + 12);
		} else if (p.phase() == VfxIds.END) {
			stuck = false;
			final float R = p.param() > 0.0F ? p.param() : radius;
			final double cx = p.x(), cy = p.y(), cz = p.z();
			layer(t0, 18, (g, f, u) -> VfxKit.shock(g, cx, cy - 0.9D, cz, R * 1.4F, u, 1.4D, VfxKit.FIRE, VfxKit.EMBER, 1.0F, 0.15D));
			layer(t0 + 2, 14, (g, f, u) -> VfxKit.shock(g, cx, cy - 0.9D, cz, R, u, 0.6D, VfxKit.ACID, VfxKit.WHITE, 0.8F, 0.08D));
			layer(t0, 20, (g, f, u) -> VfxKit.pillar(g, cx, cy - 0.9D, cz, 7.0D, 0.9D, u, VfxKit.FIRE, VfxKit.WHITE, 0.9F));
			layer(t0, 12, (g, f, u) -> {
				float e = VfxMath.easeOut(u), a = (1.0F - u) * (1.0F - u);
				g.color(VfxKit.FIRE, VfxKit.WHITE);
				g.sphere(cx, cy, cz, 0.4D + R * 0.9D * e, 8, g.seg(24), 0.10F * a, 0.9F * a, 1.6F, false);
				g.halo(cx, cy, cz, 5.0D * e + 0.8D, 0.8F * a, 0.0F, g.seg(20));
				g.burst(cx, cy, cz, 16, 3.8D * e, 0.18D * (1.0F - u), 0.95F * (1.0F - u), seed * 0.01D);
			});
			shadeLayer(t0, 30, (g, f, u) -> {
				g.color(VfxKit.SMOKE, new float[]{0.08F, 0.06F, 0.06F});
				for (int i = 0; i < 6; i++) {
					double ang = i * VfxMath.TAU / 6.0D;
					double r = R * 0.6D * VfxMath.easeOut(u);
					g.sphere(cx + Math.cos(ang) * r, cy + u * 1.5D, cz + Math.sin(ang) * r, 0.7D + 0.6D * u, 5, g.seg(12), 0.5F * (1.0F - u), 0.1F, 1.4F, false);
				}
			});
			soundAtTarget(t0, "alchemist_compound_explode", 1.4F, 1.0F);
			flash(t0, VfxKit.FIRE, 0.26F, 10);
			shake(t0, 4.5F, 12, 24.0F, true);
			holdUntil(t0 + 34);
		}
	}
}
