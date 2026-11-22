package net.mcreator.eldervale.stats;

import net.neoforged.neoforge.common.util.INBTSerializable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

/**
 * Points invested by the player in each attribute (the only thing saved here).
 * Free points live in PlayerVariables.STAT_POINT, final stats in PlayerVariables:
 * nothing is stored twice.
 */
public class PlayerAttributes implements INBTSerializable<CompoundTag> {
	private final int[] points = new int[StatAttribute.COUNT];

	/** Class the stats were last calculated for. NOT saved: it is only used to detect a class change. */
	public String appliedClass = "";

	public int get(StatAttribute attribute) {
		return points[attribute.ordinal()];
	}

	public void add(StatAttribute attribute, int amount) {
		points[attribute.ordinal()] += amount;
	}

	public int[] snapshot() {
		return points.clone();
	}

	@Override
	public CompoundTag serializeNBT(HolderLookup.Provider lookupProvider) {
		CompoundTag nbt = new CompoundTag();
		for (StatAttribute attribute : StatAttribute.values())
			nbt.putInt(attribute.key, points[attribute.ordinal()]);
		return nbt;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
		for (StatAttribute attribute : StatAttribute.values())
			points[attribute.ordinal()] = Math.max(0, Math.min(StatsConfig.MAX_POINTS_PER_ATTRIBUTE, nbt.getInt(attribute.key)));
	}
}
