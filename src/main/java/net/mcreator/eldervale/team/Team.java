package net.mcreator.eldervale.team;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;

/**
 * Data of one team: id, name, leader and members (player UUID -> last known name).
 * The last known name lets the team list show offline members.
 * Members keep their join order (the leader is the first at creation).
 * Server-side data only: no client classes here.
 */
public class Team {
	private final UUID id;
	private String name;
	private UUID leader;
	private final LinkedHashMap<UUID, String> members = new LinkedHashMap<>();

	public Team(UUID id, String name, UUID leader, String leaderName) {
		this.id = id;
		this.name = name;
		this.leader = leader;
		this.members.put(leader, leaderName);
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UUID getLeader() {
		return leader;
	}

	public void setLeader(UUID leader) {
		this.leader = leader;
	}

	public boolean isLeader(UUID player) {
		return leader.equals(player);
	}

	public Map<UUID, String> getMembers() {
		return Collections.unmodifiableMap(members);
	}

	public boolean has(UUID player) {
		return members.containsKey(player);
	}

	public int size() {
		return members.size();
	}

	public String nameOf(UUID player) {
		return members.get(player);
	}

	void addMember(UUID player, String playerName) {
		members.put(player, playerName);
	}

	void removeMember(UUID player) {
		members.remove(player);
	}

	void setMemberName(UUID player, String playerName) {
		if (members.containsKey(player))
			members.put(player, playerName);
	}

	/** Case-insensitive search among the stored member names. */
	public UUID findByName(String playerName) {
		for (Map.Entry<UUID, String> e : members.entrySet())
			if (e.getValue().equalsIgnoreCase(playerName))
				return e.getKey();
		return null;
	}

	// ---------------------------------------------------------------
	//  Save / load
	// ---------------------------------------------------------------
	public CompoundTag save() {
		CompoundTag tag = new CompoundTag();
		tag.putUUID("Id", id);
		tag.putString("Name", name);
		tag.putUUID("Leader", leader);
		ListTag list = new ListTag();
		for (Map.Entry<UUID, String> e : members.entrySet()) {
			CompoundTag m = new CompoundTag();
			m.putUUID("Id", e.getKey());
			m.putString("Name", e.getValue());
			list.add(m);
		}
		tag.put("Members", list);
		return tag;
	}

	public static Team load(CompoundTag tag) {
		UUID leader = tag.getUUID("Leader");
		Team team = new Team(tag.getUUID("Id"), tag.getString("Name"), leader, "?");
		team.members.clear();
		ListTag list = tag.getList("Members", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag m = list.getCompound(i);
			team.members.put(m.getUUID("Id"), m.getString("Name"));
		}
		if (!team.members.containsKey(leader))
			team.members.put(leader, "?");
		return team;
	}
}
