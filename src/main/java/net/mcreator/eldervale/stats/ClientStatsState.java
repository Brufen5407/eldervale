package net.mcreator.eldervale.stats;

/** Client copy of the invested points (plain data, no client-only imports). Filled by AttributeSyncPayload. */
public final class ClientStatsState {
	private ClientStatsState() {
	}

	private static volatile int[] points = new int[StatAttribute.COUNT];

	public static void set(int[] values) {
		int[] copy = new int[StatAttribute.COUNT];
		System.arraycopy(values, 0, copy, 0, Math.min(values.length, copy.length));
		points = copy;
	}

	public static int get(StatAttribute attribute) {
		return points[attribute.ordinal()];
	}
}
