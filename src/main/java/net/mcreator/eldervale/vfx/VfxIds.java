package net.mcreator.eldervale.vfx;

/**
 * Shared ids for the VFX system (server and client).
 * Add one constant per effect. Never reuse or renumber an existing id.
 */
public final class VfxIds {

	// ---- Effects ----
	public static final int MONK_MEDITATION = 1;

	// ---- Phases of an effect ----
	public static final int START = 0;
	public static final int PULSE = 1;
	public static final int IMPACT = 2;
	public static final int END = 3;

	private VfxIds() {
	}
}
