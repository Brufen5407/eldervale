package net.mcreator.eldervale.stats;

/** The six player attributes. The order is also the network and ClassProfile order: do not reorder. */
public enum StatAttribute {
	STRENGTH("strength"),
	VITALITY("vitality"),
	DEFENSE("defense"),
	AGILITY("agility"),
	INTELLIGENCE("intelligence"),
	DEXTERITY("dexterity");

	public static final int COUNT = values().length;
	public final String key;

	StatAttribute(String key) {
		this.key = key;
	}

	/** Returns null for an invalid index (never trust the client). */
	public static StatAttribute fromIndex(int index) {
		StatAttribute[] all = values();
		return index >= 0 && index < all.length ? all[index] : null;
	}
}
