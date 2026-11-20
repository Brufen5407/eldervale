package net.mcreator.eldervale.party;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;

/**
 * Data of one party: id, name, leader and members.
 * Each member keeps the last known name, class and level, so the party list can show offline members.
 * Members keep their join order (the leader is the first at creation).
 * Server-side data only: no client classes here.
 */
public class Party {

	public static class Member {
		private String name;
		private String playerClass = "";
		private int level = 1;

		Member(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public String getPlayerClass() {
			return playerClass;
		}

		public int getLevel() {
			return level;
		}
	}

	private final UUID id;
	private String name;
	private UUID leader;
	private final LinkedHashMap<UUID, Member> members = new LinkedHashMap<>();

	public Party(UUID id, String name, UUID leader, String leaderName) {
		this.id = id;
		this.name = name;
		this.leader = leader;
		this.members.put(leader, new Member(leaderName));
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

	public Map<UUID, Member> getMembers() {
		return Collections.unmodifiableMap(members);
	}

	public boolean has(UUID player) {
		return members.containsKey(player);
	}

	public int size() {
		return members.size();
	}

	public String nameOf(UUID player) {
		Member m = members.get(player);
		return m == null ? null : m.name;
	}

	void addMember(UUID player, String playerName) {
		members.put(player, new Member(playerName));
	}

	void removeMember(UUID player) {
		members.remove(player);
	}

	void setMemberName(UUID player, String playerName) {
		Member m = members.get(player);
		if (m != null)
			m.name = playerName;
	}

	void setMemberStats(UUID player, String playerClass, int level) {
		Member m = members.get(player);
		if (m != null) {
			m.playerClass = playerClass == null ? "" : playerClass;
			m.level = level;
		}
	}

	/** Case-insensitive search among the stored member names. */
	public UUID findByName(String playerName) {
		for (Map.Entry<UUID, Member> e : members.entrySet())
			if (e.getValue().name.equalsIgnoreCase(playerName))
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
		for (Map.Entry<UUID, Member> e : members.entrySet()) {
			CompoundTag m = new CompoundTag();
			m.putUUID("Id", e.getKey());
			m.putString("Name", e.getValue().name);
			m.putString("Class", e.getValue().playerClass);
			m.putInt("Level", e.getValue().level);
			list.add(m);
		}
		tag.put("Members", list);
		return tag;
	}

	public static Party load(CompoundTag tag) {
		UUID leader = tag.getUUID("Leader");
		Party party = new Party(tag.getUUID("Id"), tag.getString("Name"), leader, "?");
		party.members.clear();
		ListTag list = tag.getList("Members", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag m = list.getCompound(i);
			Member member = new Member(m.getString("Name"));
			member.playerClass = m.getString("Class");
			member.level = Math.max(1, m.getInt("Level"));
			party.members.put(m.getUUID("Id"), member);
		}
		if (!party.members.containsKey(leader))
			party.members.put(leader, new Member("?"));
		return party;
	}
}
