package net.mcreator.eldervale.party;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;

/**
 * Parties saved with the world (in the overworld data storage, like PortalSavedData).
 * Pending invites are NOT saved: they live in PartyManager and expire on their own.
 * After changing a Party, call setDirty() so the world saves it.
 */
public class PartySavedData extends SavedData {
	private static final String DATA_NAME = "eldervale_parties";

	private final Map<UUID, Party> parties = new HashMap<>();
	private final Map<UUID, UUID> playerParty = new HashMap<>(); // player -> party id

	public static final SavedData.Factory<PartySavedData> FACTORY = new SavedData.Factory<>(PartySavedData::new, PartySavedData::load, null);

	public static PartySavedData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	public PartySavedData() {
	}

	public Party getPartyOf(UUID player) {
		UUID id = playerParty.get(player);
		return id == null ? null : parties.get(id);
	}

	public Party byId(UUID id) {
		return parties.get(id);
	}

	public Collection<Party> all() {
		return parties.values();
	}

	void add(Party party) {
		parties.put(party.getId(), party);
		for (UUID member : party.getMembers().keySet())
			playerParty.put(member, party.getId());
	}

	void remove(Party party) {
		parties.remove(party.getId());
		for (UUID member : party.getMembers().keySet())
			playerParty.remove(member, party.getId());
	}

	void index(UUID player, Party party) {
		playerParty.put(player, party.getId());
	}

	void unindex(UUID player) {
		playerParty.remove(player);
	}

	public static PartySavedData load(CompoundTag tag, HolderLookup.Provider provider) {
		PartySavedData data = new PartySavedData();
		ListTag list = tag.getList("Parties", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++)
			data.add(Party.load(list.getCompound(i)));
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
		ListTag list = new ListTag();
		for (Party party : parties.values())
			list.add(party.save());
		tag.put("Parties", list);
		return tag;
	}
}
