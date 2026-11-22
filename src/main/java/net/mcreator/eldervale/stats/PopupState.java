package net.mcreator.eldervale.stats;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Client-side list of active floating texts (plain data, no client-only imports). Drawn by PopupRenderer. */
public final class PopupState {
	private PopupState() {
	}

	public record Popup(double x, double y, double z, long startNanos, int kind, int amount) {
	}

	private static final int MAX_POPUPS = 24;
	private static final List<Popup> POPUPS = new ArrayList<>();

	/** A small random offset keeps multi-hit skills and groups of kills from stacking in one spot. */
	public static synchronized void add(double x, double y, double z, int kind, int amount) {
		ThreadLocalRandom r = ThreadLocalRandom.current();
		if (POPUPS.size() >= MAX_POPUPS)
			POPUPS.remove(0);
		POPUPS.add(new Popup(x + (r.nextDouble() - 0.5) * 0.6, y + r.nextDouble() * 0.3, z + (r.nextDouble() - 0.5) * 0.6, System.nanoTime(), kind, amount));
	}

	/** Drops expired popups (each kind has its own life) and returns a copy of the live ones. */
	public static synchronized List<Popup> active(long nowNanos, long critLifeNanos, long xpLifeNanos) {
		POPUPS.removeIf(p -> nowNanos - p.startNanos() > (p.kind() == PopupPayload.CRIT ? critLifeNanos : xpLifeNanos));
		return new ArrayList<>(POPUPS);
	}

	public static synchronized void clear() {
		POPUPS.clear();
	}
}
