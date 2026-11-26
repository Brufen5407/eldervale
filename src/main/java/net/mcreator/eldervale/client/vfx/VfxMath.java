package net.mcreator.eldervale.client.vfx;

/** Small math helpers shared by all effects (easing, envelopes, deterministic random). */
public final class VfxMath {

	public static final double TAU = Math.PI * 2.0D;

	public static float clamp01(float v) {
		return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
	}

	public static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	/** Fast start, slow end. */
	public static float easeOut(float t) {
		float u = 1.0F - clamp01(t);
		return 1.0F - u * u * u;
	}

	/** Slow start, fast end. */
	public static float easeIn(float t) {
		float u = clamp01(t);
		return u * u * u;
	}

	/** Inverse of easeOut: the time fraction at which easeOut reaches the value v. */
	public static float easeOutInverse(float v) {
		return 1.0F - (float) Math.cbrt(1.0F - clamp01(v));
	}

	public static float easeInOut(float t) {
		float u = clamp01(t);
		return u * u * (3.0F - 2.0F * u);
	}

	/** Smoothstep: 0 below edge0, 1 above edge1. */
	public static float smooth(float edge0, float edge1, float x) {
		return easeInOut((x - edge0) / (edge1 - edge0));
	}

	/** Deterministic value in [0,1) from a seed and an index: every client sees the same shapes. */
	public static float hash(int seed, int i) {
		int h = seed * 0x9E3779B1 ^ (i * 0x85EBCA6B);
		h ^= h >>> 15;
		h *= 0x2C1B3C6D;
		h ^= h >>> 12;
		h *= 0x297A2D39;
		h ^= h >>> 15;
		return (h >>> 8) / (float) (1 << 24);
	}

	private VfxMath() {
	}
}
