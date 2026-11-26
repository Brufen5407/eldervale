package net.mcreator.eldervale.client.vfx;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.phys.Vec3;

/**
 * Geometry toolkit of the VFX system: every shape is built from glowing quads, no particles.
 *
 * Colors: color(primary, secondary) sets two colors. Each shape blends them along itself
 * (for example a ring goes from secondary on the inside to primary on the outside,
 * a beam from secondary at the bottom to primary at the top, a sphere from secondary on the face to
 * primary on the rim). A white secondary gives a hot white core with a colored edge.
 *
 * Alpha: every shape takes its own alpha values. With the additive GLOW render type the alpha is the light intensity.
 *
 * Each instance lives for one frame. It has a quad budget: when it is used up, the shapes are skipped
 * (so a crowded fight can never freeze the game). Use seg() for any segment count so the quality setting applies.
 */
public final class VfxGfx {

	private static final float[] DETAIL = {0.55F, 1.0F, 1.5F};

	private final VertexConsumer vc;
	private final Matrix4f m;
	private final double cx, cy, cz;
	private final Vector3f left, up;
	private final float detail;
	private int quadsLeft;

	private float pr = 1.0F, pg = 1.0F, pb = 1.0F;
	private float sr = 1.0F, sg = 1.0F, sb = 1.0F;

	// scratch for strips (no allocation while drawing)
	private final double[] px = new double[96];
	private final double[] py = new double[96];
	private final double[] pz = new double[96];
	private int pn;
	private double qx, qy, qz;

	public VfxGfx(VertexConsumer vc, Matrix4f m, Vec3 cam, Vector3f left, Vector3f up, int quality, int quadBudget) {
		this.vc = vc;
		this.m = m;
		this.cx = cam.x;
		this.cy = cam.y;
		this.cz = cam.z;
		this.left = left;
		this.up = up;
		this.detail = DETAIL[Math.max(0, Math.min(DETAIL.length - 1, quality))];
		this.quadsLeft = quadBudget;
	}

	// ---------------------------------------------------------------
	//  State
	// ---------------------------------------------------------------
	public VfxGfx color(float[] primary, float[] secondary) {
		pr = primary[0];
		pg = primary[1];
		pb = primary[2];
		sr = secondary[0];
		sg = secondary[1];
		sb = secondary[2];
		return this;
	}

	public VfxGfx color(float[] c) {
		return color(c, c);
	}

	/** Segment count scaled by the quality setting. */
	public int seg(int base) {
		return Math.max(6, Math.round(base * detail));
	}

	public boolean hasBudget() {
		return quadsLeft > 0;
	}

	// ---------------------------------------------------------------
	//  Low level
	// ---------------------------------------------------------------
	private boolean take() {
		if (quadsLeft <= 0)
			return false;
		quadsLeft--;
		return true;
	}

	private void v(double x, double y, double z, float a, float t) {
		float k = t < 0.0F ? 0.0F : (t > 1.0F ? 1.0F : t);
		float r = sr + (pr - sr) * k;
		float g = sg + (pg - sg) * k;
		float b = sb + (pb - sb) * k;
		int ia = (int) ((a < 0.0F ? 0.0F : (a > 1.0F ? 1.0F : a)) * 255.0F);
		vc.addVertex(m, (float) (x - cx), (float) (y - cy), (float) (z - cz)).setColor((int) (r * 255.0F), (int) (g * 255.0F), (int) (b * 255.0F), ia);
	}

	private void quad(double x1, double y1, double z1, float a1, float t1, double x2, double y2, double z2, float a2, float t2, double x3, double y3, double z3, float a3, float t3, double x4, double y4, double z4, float a4, float t4) {
		if (!take())
			return;
		v(x1, y1, z1, a1, t1);
		v(x2, y2, z2, a2, t2);
		v(x3, y3, z3, a3, t3);
		v(x4, y4, z4, a4, t4);
	}

	// ---------------------------------------------------------------
	//  Flat shapes (lie on a horizontal plane at height y)
	// ---------------------------------------------------------------

	/** Part of a ring. Angles in radians. aIn/aOut = alpha at the inner/outer edge. */
	public void arc(double x, double y, double z, double rIn, double rOut, double a0, double a1, float aIn, float aOut, int segs) {
		if (rOut <= 0.0D)
			return;
		rIn = Math.max(0.0D, rIn);
		for (int i = 0; i < segs; i++) {
			double t0 = a0 + (a1 - a0) * i / segs;
			double t1 = a0 + (a1 - a0) * (i + 1) / segs;
			double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
			quad(x + c0 * rIn, y, z + s0 * rIn, aIn, 0.0F,
					x + c0 * rOut, y, z + s0 * rOut, aOut, 1.0F,
					x + c1 * rOut, y, z + s1 * rOut, aOut, 1.0F,
					x + c1 * rIn, y, z + s1 * rIn, aIn, 0.0F);
		}
	}

	public void ring(double x, double y, double z, double rIn, double rOut, float aIn, float aOut, int segs) {
		arc(x, y, z, rIn, rOut, 0.0D, VfxMath.TAU, aIn, aOut, segs);
	}

	/** Filled disc: alpha aCenter in the middle, aEdge on the border. */
	public void disc(double x, double y, double z, double r, float aCenter, float aEdge, int segs) {
		arc(x, y, z, 0.0D, r, 0.0D, VfxMath.TAU, aCenter, aEdge, segs);
	}

	/**
	 * Shockwave ring whose radius and height ripple. amp = size of the ripple, waves = number of bumps around the ring.
	 * Alpha is aIn on the inner edge and aOut on the outer (leading) edge.
	 */
	public void wavyRing(double x, double y, double z, double rad, double width, float aIn, float aOut, int segs, double amp, double waves, double phase) {
		for (int i = 0; i < segs; i++) {
			double t0 = VfxMath.TAU * i / segs;
			double t1 = VfxMath.TAU * (i + 1) / segs;
			double w0 = Math.sin(waves * t0 + phase), w1 = Math.sin(waves * t1 + phase);
			double r0 = rad + amp * w0, r1 = rad + amp * w1;
			double i0 = Math.max(0.0D, r0 - width), i1 = Math.max(0.0D, r1 - width);
			double y0 = y + amp * 0.5D * w0, y1 = y + amp * 0.5D * w1;
			double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
			quad(x + c0 * i0, y0, z + s0 * i0, aIn, 0.0F,
					x + c0 * r0, y0, z + s0 * r0, aOut, 1.0F,
					x + c1 * r1, y1, z + s1 * r1, aOut, 1.0F,
					x + c1 * i1, y1, z + s1 * i1, aIn, 0.0F);
		}
	}

	/** A bar pointing out from the center (rune tick, spoke). */
	public void spoke(double x, double y, double z, double ang, double r0, double r1, double halfWidth, float a0, float a1) {
		double c = Math.cos(ang), s = Math.sin(ang);
		double sx = -s * halfWidth, sz = c * halfWidth;
		quad(x + c * r0 + sx, y, z + s * r0 + sz, a0, 0.0F,
				x + c * r1 + sx, y, z + s * r1 + sz, a1, 1.0F,
				x + c * r1 - sx, y, z + s * r1 - sz, a1, 1.0F,
				x + c * r0 - sx, y, z + s * r0 - sz, a0, 0.0F);
	}

	/** A straight line on the ground between two points. */
	public void flatLine(double x1, double z1, double x2, double z2, double y, double halfWidth, float a) {
		double dx = x2 - x1, dz = z2 - z1;
		double len = Math.sqrt(dx * dx + dz * dz);
		if (len < 1.0E-6D)
			return;
		double ox = -dz / len * halfWidth, oz = dx / len * halfWidth;
		quad(x1 + ox, y, z1 + oz, a, 1.0F, x2 + ox, y, z2 + oz, a, 1.0F, x2 - ox, y, z2 - oz, a, 1.0F, x1 - ox, y, z1 - oz, a, 1.0F);
	}

	// ---------------------------------------------------------------
	//  Volumes
	// ---------------------------------------------------------------

	/** Cylinder or cone from y0 to y1. a0 = alpha at the bottom, a1 = at the top. Bottom uses the secondary color. */
	public void cone(double x, double y0, double y1, double z, double r0, double r1, int segs, float a0, float a1) {
		for (int i = 0; i < segs; i++) {
			double t0 = VfxMath.TAU * i / segs;
			double t1 = VfxMath.TAU * (i + 1) / segs;
			double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
			quad(x + c0 * r0, y0, z + s0 * r0, a0, 0.0F,
					x + c1 * r0, y0, z + s1 * r0, a0, 0.0F,
					x + c1 * r1, y1, z + s1 * r1, a1, 1.0F,
					x + c0 * r1, y1, z + s0 * r1, a1, 1.0F);
		}
	}

	/**
	 * Sphere or dome with a bright rim (fresnel): the edge seen from the camera glows, the face stays transparent.
	 * aFace = alpha facing the camera, aRim = alpha on the silhouette, pow = how thin the rim is (2 soft, 4 thin).
	 * upperOnly = only the upper half (a dome).
	 */
	public void sphere(double x, double y, double z, double rad, int bands, int segs, float aFace, float aRim, float pow, boolean upperOnly) {
		if (rad < 0.05D)
			return;
		double start = upperOnly ? 0.0D : -Math.PI / 2.0D;
		double range = upperOnly ? Math.PI / 2.0D : Math.PI;
		for (int i = 0; i < bands; i++) {
			double p0 = start + range * i / bands;
			double p1 = start + range * (i + 1) / bands;
			double r0 = rad * Math.cos(p0), r1 = rad * Math.cos(p1);
			double h0 = rad * Math.sin(p0), h1 = rad * Math.sin(p1);
			for (int j = 0; j < segs; j++) {
				if (!take())
					return;
				double t0 = VfxMath.TAU * j / segs;
				double t1 = VfxMath.TAU * (j + 1) / segs;
				double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
				fv(x + c0 * r0, y + h0, z + s0 * r0, x, y, z, rad, aFace, aRim, pow);
				fv(x + c1 * r0, y + h0, z + s1 * r0, x, y, z, rad, aFace, aRim, pow);
				fv(x + c1 * r1, y + h1, z + s1 * r1, x, y, z, rad, aFace, aRim, pow);
				fv(x + c0 * r1, y + h1, z + s0 * r1, x, y, z, rad, aFace, aRim, pow);
			}
		}
	}

	private void fv(double vx, double vy, double vz, double ox, double oy, double oz, double rad, float aFace, float aRim, float pow) {
		double nx = (vx - ox) / rad, ny = (vy - oy) / rad, nz = (vz - oz) / rad;
		double ex = cx - vx, ey = cy - vy, ez = cz - vz;
		double el = Math.sqrt(ex * ex + ey * ey + ez * ez);
		if (el < 1.0E-6D)
			el = 1.0E-6D;
		double d = Math.abs((nx * ex + ny * ey + nz * ez) / el);
		float f = (float) Math.pow(Math.max(0.0D, 1.0D - d), pow);
		v(vx, vy, vz, aFace + (aRim - aFace) * f, f);
	}

	/** A flower petal. tilt = angle above the ground in radians (about 1.4 = nearly vertical, 0.3 = open). */
	public void petal(double x, double y, double z, double ang, double tilt, double len, double halfWidth, float aBase, float aTip) {
		double ch = Math.cos(tilt), sv = Math.sin(tilt);
		double dx = Math.cos(ang) * ch, dy = sv, dz = Math.sin(ang) * ch;
		double sx = -Math.sin(ang), sz = Math.cos(ang);
		double mx = x + dx * len * 0.5D, my = y + dy * len * 0.5D, mz = z + dz * len * 0.5D;
		float aMid = (aBase + aTip) * 0.5F;
		quad(x, y, z, aBase, 0.0F,
				mx + sx * halfWidth, my, mz + sz * halfWidth, aMid, 0.5F,
				x + dx * len, y + dy * len, z + dz * len, aTip, 1.0F,
				mx - sx * halfWidth, my, mz - sz * halfWidth, aMid, 0.5F);
	}

	// ---------------------------------------------------------------
	//  Camera facing shapes
	// ---------------------------------------------------------------

	/** Soft glow disc that always faces the camera (a light bloom). Two rings give a smooth falloff. */
	public void halo(double x, double y, double z, double r, float aCenter, float aEdge, int segs) {
		if (r <= 0.0D)
			return;
		float aMid = aCenter * 0.28F + aEdge * 0.72F;
		for (int i = 0; i < segs; i++) {
			double t0 = VfxMath.TAU * i / segs;
			double t1 = VfxMath.TAU * (i + 1) / segs;
			double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
			double m0x = (left.x() * c0 + up.x() * s0), m0y = (left.y() * c0 + up.y() * s0), m0z = (left.z() * c0 + up.z() * s0);
			double m1x = (left.x() * c1 + up.x() * s1), m1y = (left.y() * c1 + up.y() * s1), m1z = (left.z() * c1 + up.z() * s1);
			double h = r * 0.5D;
			quad(x, y, z, aCenter, 0.0F, x + m0x * h, y + m0y * h, z + m0z * h, aMid, 0.0F, x + m1x * h, y + m1y * h, z + m1z * h, aMid, 0.0F, x, y, z, aCenter, 0.0F);
			quad(x + m0x * h, y + m0y * h, z + m0z * h, aMid, 0.0F, x + m0x * r, y + m0y * r, z + m0z * r, aEdge, 1.0F, x + m1x * r, y + m1y * r, z + m1z * r, aEdge, 1.0F, x + m1x * h, y + m1y * h, z + m1z * h, aMid, 0.0F);
		}
	}

	/** Spikes radiating from a point, in the plane that faces the camera. */
	public void burst(double x, double y, double z, int n, double len, double thick, float a, double rot) {
		for (int k = 0; k < n; k++) {
			double ang = rot + k * (VfxMath.TAU / n);
			double ca = Math.cos(ang), sa = Math.sin(ang);
			double dx = left.x() * ca + up.x() * sa, dy = left.y() * ca + up.y() * sa, dz = left.z() * ca + up.z() * sa;
			double ox = -left.x() * sa + up.x() * ca, oy = -left.y() * sa + up.y() * ca, oz = -left.z() * sa + up.z() * ca;
			double tx = x + dx * len, ty = y + dy * len, tz = z + dz * len;
			quad(x + ox * thick, y + oy * thick, z + oz * thick, a, 0.0F, tx, ty, tz, 0.0F, 1.0F, tx, ty, tz, 0.0F, 1.0F, x - ox * thick, y - oy * thick, z - oz * thick, a, 0.0F);
		}
	}

	/**
	 * A thin diamond of light along a direction, with a hot center (secondary color) and tinted ends.
	 * Used for light blades, rising rays and sparks made of geometry.
	 */
	public void sliver(double x, double y, double z, double dx, double dy, double dz, double len, double halfWidth, float a) {
		double dl = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (dl < 1.0E-6D)
			return;
		dx /= dl;
		dy /= dl;
		dz /= dl;
		double vx = x - cx, vy = y - cy, vz = z - cz;
		double sx = dy * vz - dz * vy, sy = dz * vx - dx * vz, sz = dx * vy - dy * vx;
		double sl = Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (sl < 1.0E-9D)
			return;
		double k = halfWidth / sl;
		sx *= k;
		sy *= k;
		sz *= k;
		double tail = len * 0.35D, tip = len * 0.65D;
		quad(x - dx * tail, y - dy * tail, z - dz * tail, 0.0F, 1.0F,
				x + sx, y + sy, z + sz, a, 0.0F,
				x + dx * tip, y + dy * tip, z + dz * tip, 0.0F, 1.0F,
				x - sx, y - sy, z - sz, a, 0.0F);
	}

	// ---------------------------------------------------------------
	//  Ribbons (strip of light that follows a path, always facing the camera)
	// ---------------------------------------------------------------
	public void stripBegin() {
		pn = 0;
	}

	public void stripPoint(double x, double y, double z) {
		if (pn < px.length) {
			px[pn] = x;
			py[pn] = y;
			pz[pn] = z;
			pn++;
		}
	}

	/**
	 * Draws the ribbon through the points added since stripBegin().
	 * width = full width in the middle; the ends taper to a point. Alpha goes from aStart to aEnd along the path.
	 * The center line uses the secondary color, the edges the primary color.
	 */
	public void stripEnd(double width, float aStart, float aEnd) {
		if (pn < 2)
			return;
		int last = pn - 1;
		for (int i = 0; i < last; i++) {
			float u0 = i / (float) last, u1 = (i + 1) / (float) last;
			float s0 = (float) Math.pow(Math.sin(Math.PI * u0), 0.7D);
			float s1 = (float) Math.pow(Math.sin(Math.PI * u1), 0.7D);
			float al0 = VfxMath.lerp(aStart, aEnd, u0) * s0;
			float al1 = VfxMath.lerp(aStart, aEnd, u1) * s1;

			perp(i);
			double ax = qx * width * 0.5D * s0, ay = qy * width * 0.5D * s0, az = qz * width * 0.5D * s0;
			perp(i + 1);
			double bx = qx * width * 0.5D * s1, by = qy * width * 0.5D * s1, bz = qz * width * 0.5D * s1;

			// left half
			quad(px[i], py[i], pz[i], al0, 0.0F,
					px[i + 1], py[i + 1], pz[i + 1], al1, 0.0F,
					px[i + 1] + bx, py[i + 1] + by, pz[i + 1] + bz, al1 * 0.25F, 1.0F,
					px[i] + ax, py[i] + ay, pz[i] + az, al0 * 0.25F, 1.0F);
			// right half
			quad(px[i], py[i], pz[i], al0, 0.0F,
					px[i] - ax, py[i] - ay, pz[i] - az, al0 * 0.25F, 1.0F,
					px[i + 1] - bx, py[i + 1] - by, pz[i + 1] - bz, al1 * 0.25F, 1.0F,
					px[i + 1], py[i + 1], pz[i + 1], al1, 0.0F);
		}
	}

	// unit vector across the path at point i, facing the camera
	private void perp(int i) {
		int i0 = Math.max(0, i - 1), i1 = Math.min(pn - 1, i + 1);
		double tx = px[i1] - px[i0], ty = py[i1] - py[i0], tz = pz[i1] - pz[i0];
		double vx = px[i] - cx, vy = py[i] - cy, vz = pz[i] - cz;
		double sx = ty * vz - tz * vy, sy = tz * vx - tx * vz, sz = tx * vy - ty * vx;
		double l = Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (l < 1.0E-9D) {
			qx = 0.0D;
			qy = 1.0D;
			qz = 0.0D;
			return;
		}
		qx = sx / l;
		qy = sy / l;
		qz = sz / l;
	}

	// ---------------------------------------------------------------
	//  Shapes in any plane (slashes, vertical rings, discs facing a direction)
	// ---------------------------------------------------------------

	/** Ring segment in the plane spanned by the unit vectors u and v. Angles in radians, measured from u toward v. */
	public void arcPlane(double x, double y, double z, double ux, double uy, double uz, double vx, double vy, double vz, double rIn, double rOut, double a0, double a1, float aIn, float aOut, int segs) {
		if (rOut <= 0.0D)
			return;
		rIn = Math.max(0.0D, rIn);
		for (int i = 0; i < segs; i++) {
			double t0 = a0 + (a1 - a0) * i / segs;
			double t1 = a0 + (a1 - a0) * (i + 1) / segs;
			double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
			double d0x = ux * c0 + vx * s0, d0y = uy * c0 + vy * s0, d0z = uz * c0 + vz * s0;
			double d1x = ux * c1 + vx * s1, d1y = uy * c1 + vy * s1, d1z = uz * c1 + vz * s1;
			quad(x + d0x * rIn, y + d0y * rIn, z + d0z * rIn, aIn, 0.0F,
					x + d0x * rOut, y + d0y * rOut, z + d0z * rOut, aOut, 1.0F,
					x + d1x * rOut, y + d1y * rOut, z + d1z * rOut, aOut, 1.0F,
					x + d1x * rIn, y + d1y * rIn, z + d1z * rIn, aIn, 0.0F);
		}
	}

	/**
	 * Sword slash: a crescent of light in the plane (u,v) with sharp tips. thick = width in the middle.
	 * The bright edge (primary color) is on the outside, the inside fades out. a = peak alpha.
	 */
	public void crescent(double x, double y, double z, double ux, double uy, double uz, double vx, double vy, double vz, double radius, double thick, double a0, double a1, float a, int segs) {
		for (int i = 0; i < segs; i++) {
			double s0 = i / (double) segs, s1 = (i + 1) / (double) segs;
			double t0 = a0 + (a1 - a0) * s0, t1 = a0 + (a1 - a0) * s1;
			double w0 = thick * Math.pow(Math.sin(Math.PI * s0), 0.8D), w1 = thick * Math.pow(Math.sin(Math.PI * s1), 0.8D);
			double c0 = Math.cos(t0), n0 = Math.sin(t0), c1 = Math.cos(t1), n1 = Math.sin(t1);
			double d0x = ux * c0 + vx * n0, d0y = uy * c0 + vy * n0, d0z = uz * c0 + vz * n0;
			double d1x = ux * c1 + vx * n1, d1y = uy * c1 + vy * n1, d1z = uz * c1 + vz * n1;
			float al0 = a * (float) Math.pow(Math.sin(Math.PI * s0), 0.5D), al1 = a * (float) Math.pow(Math.sin(Math.PI * s1), 0.5D);
			quad(x + d0x * (radius - w0), y + d0y * (radius - w0), z + d0z * (radius - w0), 0.0F, 0.0F,
					x + d0x * radius, y + d0y * radius, z + d0z * radius, al0, 1.0F,
					x + d1x * radius, y + d1y * radius, z + d1z * radius, al1, 1.0F,
					x + d1x * (radius - w1), y + d1y * (radius - w1), z + d1z * (radius - w1), 0.0F, 0.0F);
		}
	}

	/** Straight beam between two points, always facing the camera, with a hot center (secondary color). */
	public void beam(double x0, double y0, double z0, double x1, double y1, double z1, double halfWidth, float a0, float a1) {
		double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
		double mx = (x0 + x1) * 0.5D - cx, my = (y0 + y1) * 0.5D - cy, mz = (z0 + z1) * 0.5D - cz;
		double sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
		double l = Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (l < 1.0E-9D)
			return;
		double k = halfWidth / l;
		sx *= k;
		sy *= k;
		sz *= k;
		quad(x0, y0, z0, a0, 0.0F, x1, y1, z1, a1, 0.0F, x1 + sx, y1 + sy, z1 + sz, a1 * 0.2F, 1.0F, x0 + sx, y0 + sy, z0 + sz, a0 * 0.2F, 1.0F);
		quad(x0, y0, z0, a0, 0.0F, x0 - sx, y0 - sy, z0 - sz, a0 * 0.2F, 1.0F, x1 - sx, y1 - sy, z1 - sz, a1 * 0.2F, 1.0F, x1, y1, z1, a1, 0.0F);
	}
}
