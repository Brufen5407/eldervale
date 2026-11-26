package net.mcreator.eldervale.client.vfx;

/**
 * Ready-made compositions built from VfxGfx shapes. Every skill is assembled from these,
 * so the whole mod shares one visual language. u is always the progress 0..1 of the layer.
 */
public final class VfxKit {

	// ---- Palettes (primary = edge color, secondary = hot core) ----
	public static final float[] WHITE = {1.00F, 1.00F, 1.00F};
	public static final float[] GOLD = {1.00F, 0.80F, 0.28F};
	public static final float[] HOLY = {1.00F, 0.93F, 0.62F};
	public static final float[] TEAL = {0.28F, 0.95F, 0.88F};
	public static final float[] DEEP_TEAL = {0.06F, 0.50F, 0.58F};
	public static final float[] GREEN = {0.55F, 1.00F, 0.62F};
	public static final float[] FIRE = {1.00F, 0.42F, 0.08F};
	public static final float[] BLOOD = {0.85F, 0.06F, 0.04F};
	public static final float[] EMBER = {1.00F, 0.70F, 0.20F};
	public static final float[] ARCANE = {0.62F, 0.38F, 1.00F};
	public static final float[] ARCANE_BLUE = {0.35F, 0.60F, 1.00F};
	public static final float[] CYAN = {0.45F, 0.95F, 1.00F};
	public static final float[] SHADOW = {0.40F, 0.18F, 0.62F};
	public static final float[] SMOKE = {0.30F, 0.24F, 0.40F};
	public static final float[] MOON = {0.70F, 0.82F, 1.00F};
	public static final float[] FERAL = {0.95F, 0.30F, 0.12F};
	public static final float[] TOXIC = {0.55F, 1.00F, 0.20F};
	public static final float[] ACID = {0.80F, 1.00F, 0.25F};
	public static final float[] LEAF = {0.40F, 0.90F, 0.35F};
	public static final float[] SKY = {0.65F, 0.90F, 1.00F};

	/** Expanding ground shockwave: rippling ring, white leading edge and a low wall of light. */
	public static void shock(VfxGfx g, double x, double y, double z, float rMax, float u, double width, float[] p, float[] s, float power, double amp) {
		float e = VfxMath.easeOut(u);
		float a = (float) Math.pow(1.0F - u, 1.5D) * power;
		double r = VfxMath.lerp(0.3F, rMax, e);
		g.color(p, s);
		g.wavyRing(x, y + 0.03D, z, r, VfxMath.lerp((float) width, (float) width * 0.2F, u), 0.0F, 0.9F * a, g.seg(56), amp * (1.0F - u), 6.0D, u * 9.0D);
		g.color(WHITE, WHITE);
		g.arc(x, y + 0.05D, z, r - 0.08D, r, 0.0D, VfxMath.TAU, 0.0F, 0.8F * a, g.seg(56));
		g.color(p, p);
		g.cone(x, y, y + 0.12D + 0.5D * (1.0F - u), z, r, r, g.seg(48), 0.38F * a, 0.0F);
	}

	/** Vertical beam of light that shoots up and fades. */
	public static void pillar(VfxGfx g, double x, double y, double z, double h, double r, float u, float[] p, float[] s, float power) {
		float rise = VfxMath.easeOut(VfxMath.clamp01(u * 4.0F));
		float a = (1.0F - u) * power;
		g.color(p, s);
		g.cone(x, y, y + h * rise, z, r, r * 0.35D, g.seg(20), 0.9F * a, 0.0F);
		g.cone(x, y, y + h * 0.9D * rise, z, r * 1.8D, r * 0.7D, g.seg(20), 0.30F * a, 0.0F);
	}

	/** Magic circle on the ground: ring, rune ticks and a star polygon {n/skip}. a = alpha, rot = rotation. */
	public static void glyph(VfxGfx g, double x, double y, double z, double rad, float a, float rot, int n, int skip, float[] p, float[] s) {
		if (rad < 0.15D || a <= 0.003F)
			return;
		g.color(p, s);
		g.arc(x, y + 0.010D, z, rad - 0.40D, rad, 0.0D, VfxMath.TAU, 0.0F, 0.20F * a, g.seg(56));
		g.arc(x, y + 0.012D, z, rad - 0.10D, rad, 0.0D, VfxMath.TAU, 0.85F * a, 0.85F * a, g.seg(56));
		int ticks = n * 3;
		for (int k = 0; k < ticks; k++)
			g.spoke(x, y + 0.018D, z, -rot * 0.7D + k * VfxMath.TAU / ticks, rad * 0.86D, rad * 0.95D, 0.03D, 0.75F * a, 0.25F * a);
		double rs = rad * 0.72D;
		g.arc(x, y + 0.014D, z, rs - 0.06D, rs, 0.0D, VfxMath.TAU, 0.6F * a, 0.6F * a, g.seg(40));
		for (int k = 0; k < n; k++) {
			double a0 = rot * 0.6D + k * VfxMath.TAU / n;
			double a1 = rot * 0.6D + ((k + skip) % n) * VfxMath.TAU / n;
			g.flatLine(x + Math.cos(a0) * rs, z + Math.sin(a0) * rs, x + Math.cos(a1) * rs, z + Math.sin(a1) * rs, y + 0.025D, 0.045D, 0.5F * a);
		}
		g.disc(x, y + 0.03D, z, rs * 0.9D, 0.10F * a, 0.0F, g.seg(32));
	}

	/** Glowing sphere of light: soft bloom plus a bright rim. */
	public static void orb(VfxGfx g, double x, double y, double z, double r, float a, float[] p, float[] s) {
		if (r < 0.03D || a <= 0.003F)
			return;
		g.color(p, s);
		g.halo(x, y, z, r * 2.6D, 0.55F * a, 0.0F, g.seg(18));
		g.color(p, WHITE);
		g.sphere(x, y, z, r, 6, g.seg(16), 0.45F * a, 0.9F * a, 1.6F, false);
	}

	/** Ribbon behind a moving caster (uses the position history of the effect). */
	public static void trail(VfxGfx g, ScriptedVfx.Trail t, int maxPoints, double width, float aHead, float aTail, float[] p, float[] s, double lift) {
		int n = Math.min(Math.min(t.size(), maxPoints), 60);
		if (n < 3)
			return;
		g.color(p, s);
		g.stripBegin();
		for (int i = n - 1; i >= 0; i--)
			g.stripPoint(t.x(i), t.y(i) + lift, t.z(i));
		g.stripEnd(width, aTail, aHead);
	}

	/** Big translucent dome with a bright rim (area marker). */
	public static void dome(VfxGfx g, double x, double y, double z, double r, float rim, float face, float[] p, float[] s) {
		g.color(p, s);
		g.sphere(x, y, z, r, 9, g.seg(32), face, rim, 3.0F, true);
	}

	/** Rays of light that rise from the ground inside a radius (geometric slivers, no particles). */
	public static void rays(VfxGfx g, double x, double y, double z, double radius, int count, float t, int seed, float a, float[] p, float[] s) {
		g.color(p, s);
		for (int i = 0; i < count; i++) {
			float period = 26.0F + VfxMath.hash(seed, i + 300) * 24.0F;
			float u = ((t + VfxMath.hash(seed, i + 400) * period) % period) / period;
			double ang = VfxMath.hash(seed, i) * VfxMath.TAU + t * 0.012D;
			double rr = 0.4D + VfxMath.hash(seed, i + 200) * (radius * 0.9D);
			double hgt = 1.8D + VfxMath.hash(seed, i + 500) * 1.8D;
			float al = (float) Math.pow(Math.sin(Math.PI * u), 1.2D) * 0.65F * a;
			g.sliver(x + Math.cos(ang) * rr, y + u * hgt, z + Math.sin(ang) * rr, 0.0D, 1.0D, 0.0D, 0.6D + 0.5D * VfxMath.hash(seed, i + 600), 0.035D, al);
		}
	}

	/**
	 * Sword slash in front of the caster. The crescent lies in a plane that contains the forward direction;
	 * 'roll' tilts that plane (0 = horizontal sweep, 90 degrees = vertical chop). The sweep angle grows with u.
	 */
	public static void slash(VfxGfx g, double x, double y, double z, double fx, double fz, double roll, double radius, double thick, float u, float a, float[] p, float[] s) {
		double sx = -fz, sz = fx;                    // sideways (horizontal)
		double ux = fx, uy = 0.0D, uz = fz;          // forward
		double cr = Math.cos(roll), sr = Math.sin(roll);
		double vx = sx * cr, vy = sr, vz = sz * cr;  // second axis, rolled up
		float e = VfxMath.easeOut(u);
		double span = Math.PI * 0.95D;
		double a0 = -span * 0.5D + span * 0.1D * (1.0F - e);
		double a1 = a0 + span * e;
		g.color(p, s);
		g.crescent(x, y, z, ux, uy, uz, vx, vy, vz, radius, thick * (1.0F - 0.4F * u), a0, a1, a * (1.0F - u * u), g.seg(28));
		g.color(WHITE, WHITE);
		g.crescent(x, y, z, ux, uy, uz, vx, vy, vz, radius + 0.04D, thick * 0.25D, a0, a1, 0.9F * a * (1.0F - u * u), g.seg(28));
	}

	private VfxKit() {
	}
}
