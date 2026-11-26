package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Spellblade skill 2: PHANTOM EDGE. START = the cast: the sword is conjured in the air. IMPACT = a spectral blade
 * (a real sword shape made of light) falls on the target (arg = slow duration, param = height of the target).
 */
public final class SpellbladePhantomVfx extends ScriptedVfx {

	public SpellbladePhantomVfx(VfxEventPayload p) {
		super(VfxIds.SPELLBLADE_PHANTOM, p, 8.0F);

		// the mage-blade gathers energy in front of the caster
		layer(0, 14, (g, f, u) -> {
			float a = (float) Math.sin(Math.PI * u);
			VfxKit.glyph(g, f.x, f.y, f.z, 1.7D, 0.8F * a, f.t * 0.2F, 4, 1, VfxKit.CYAN, VfxKit.WHITE);
			g.color(VfxKit.CYAN, VfxKit.WHITE);
			g.halo(f.x + f.hx * 0.8D, f.y + 1.3D, f.z + f.hz * 0.8D, 1.4D * a, 0.4F * a, 0.0F, g.seg(14));
		});
		layer(0, 10, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, 2.6F, u, 0.5D, VfxKit.CYAN, VfxKit.WHITE, 0.7F, 0.04D));
		sound(0, "spellblade_phantom_cast", 0.9F, 1.0F);
		flash(0, VfxKit.CYAN, 0.10F, 5);
		holdUntil(20);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() != VfxIds.IMPACT)
			return;
		final int t0 = now();
		final int slow = p.arg() > 0 ? p.arg() : 60;
		final float height = p.param() > 0.0F ? p.param() : 1.8F;
		final int seed = this.seed;
		final int fall = 5;

		// link of light between the caster and the target
		layer(t0, 8, (g, f, u) -> {
			float a = (1.0F - u) * (1.0F - u);
			g.color(VfxKit.CYAN, VfxKit.WHITE);
			g.beam(f.x, f.y + 1.4D, f.z, f.tx, f.ty + height * 0.7D, f.tz, 0.08D * (1.0F - u) + 0.01D, 0.7F * a, 0.9F * a);
		});

		// the spectral sword falls point-down onto the target
		layer(t0, fall + 12, (g, f, u) -> {
			float t = f.t - t0;
			float fallP = VfxMath.easeIn(VfxMath.clamp01(t / fall));
			float stay = 1.0F - VfxMath.smooth(fall + 2.0F, fall + 12.0F, t);
			double tipY = f.ty + height * 0.55D + 7.0D * (1.0F - fallP);
			double bladeLen = 3.2D;
			double tx = f.tx, tz = f.tz;
			g.color(VfxKit.CYAN, VfxKit.WHITE);
			// blade
			g.beam(tx, tipY, tz, tx, tipY + bladeLen, tz, 0.17D, 0.95F * stay, 0.95F * stay);
			g.beam(tx, tipY, tz, tx, tipY + bladeLen * 0.8D, tz, 0.06D, 1.0F * stay, 1.0F * stay);
			// tip glint
			VfxKit.orb(g, tx, tipY, tz, 0.16D, 1.0F * stay, VfxKit.CYAN, VfxKit.WHITE);
			// crossguard and grip
			double gy = tipY + bladeLen;
			double ca = f.t * 0.0D + VfxMath.hash(seed, 7) * VfxMath.TAU;
			g.beam(tx - Math.cos(ca) * 0.55D, gy, tz - Math.sin(ca) * 0.55D, tx + Math.cos(ca) * 0.55D, gy, tz + Math.sin(ca) * 0.55D, 0.07D, 0.9F * stay, 0.9F * stay);
			g.beam(tx, gy, tz, tx, gy + 0.7D, tz, 0.05D, 0.8F * stay, 0.8F * stay);
			VfxKit.orb(g, tx, gy + 0.8D, tz, 0.1D, 0.9F * stay, VfxKit.CYAN, VfxKit.WHITE);
			// the glow of the whole sword
			g.halo(tx, tipY + bladeLen * 0.5D, tz, 2.4D, 0.18F * stay, 0.0F, g.seg(14));
			// speed streaks above while it falls
			if (t < fall) {
				g.color(VfxKit.CYAN, VfxKit.WHITE);
				for (int k = 0; k < 4; k++)
					g.sliver(tx + (VfxMath.hash(seed, k) - 0.5D) * 0.6D, tipY + bladeLen + 1.5D + k * 0.5D, tz + (VfxMath.hash(seed, k + 9) - 0.5D) * 0.6D, 0.0D, 1.0D, 0.0D, 2.0D, 0.03D, 0.6F * (1.0F - fallP));
			}
		});

		// impact
		final int tHit = t0 + fall;
		layer(tHit, 16, (g, f, u) -> VfxKit.shock(g, f.tx, f.ty + 0.04D, f.tz, 3.6F, u, 0.9D, VfxKit.CYAN, VfxKit.WHITE, 1.0F, 0.1D));
		layer(tHit, 12, (g, f, u) -> VfxKit.pillar(g, f.tx, f.ty, f.tz, 6.0D, 0.5D, u, VfxKit.CYAN, VfxKit.WHITE, 0.9F));
		layer(tHit, 10, (g, f, u) -> {
			float e = VfxMath.easeOut(u), a = 1.0F - u;
			g.color(VfxKit.CYAN, VfxKit.WHITE);
			g.burst(f.tx, f.ty + height * 0.5D, f.tz, 14, 2.8D * e, 0.12D * a, 0.95F * a, seed * 0.01D);
			g.halo(f.tx, f.ty + height * 0.5D, f.tz, 3.6D * e + 0.4D, 0.7F * a * a, 0.0F, g.seg(18));
		});
		// frost runes on the ground for the length of the slowness
		layer(tHit, slow, (g, f, u) -> {
			float a = VfxMath.clamp01(u * 10.0F) * (1.0F - VfxMath.smooth(0.8F, 1.0F, u));
			VfxKit.glyph(g, f.tx, f.ty + 0.04D, f.tz, 1.5D, 0.7F * a, f.t * 0.05F, 4, 1, VfxKit.CYAN, VfxKit.WHITE);
			VfxKit.rays(g, f.tx, f.ty, f.tz, 1.2D, 6, f.t, seed, 0.7F * a, VfxKit.CYAN, VfxKit.WHITE);
		});

		soundAtTarget(tHit, "spellblade_phantom_hit", 1.2F, 1.0F);
		flash(tHit, VfxKit.CYAN, 0.18F, 8);
		fov(tHit, 4.0F);
		shake(tHit, 3.0F, 9, 18.0F, true);
		holdUntil(tHit + slow + 2);
	}
}
