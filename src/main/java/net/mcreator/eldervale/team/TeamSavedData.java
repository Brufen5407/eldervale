package net.mcreator.eldervale.team;

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
 * Teams saved with the world (in the overworld data storage, like PortalSavedData).
 * Pending invites are NOT saved: they live in TeamManager and expire on their own.
 * After changing a Team, call setDirty() so the world saves it.
 */
public class TeamSavedData extends SavedData {
	private static final String DATA_NAME = "eldervale_teams";

	private final Map<UUID, Team> teams = new HashMap<>();
	private final Map<UUID, UUID> playerTeam = new HashMap<>(); // player -> team id

	public static final SavedData.Factory<TeamSavedData> FACTORY = new SavedData.Factory<>(TeamSavedData::new, TeamSavedData::load, null);

	public static TeamSavedData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
	}

	public TeamSavedData() {
	}

	public Team getTeamOf(UUID player) {
		UUID id = playerTeam.get(player);
		return id == null ? null : teams.get(id);
	}

	public Collection<Team> all() {
		return teams.values();
	}

	void add(Team team) {
		teams.put(team.getId(), team);
		for (UUID member : team.getMembers().keySet())
			playerTeam.put(member, team.getId());
	}

	void remove(Team team) {
		teams.remove(team.getId());
		for (UUID member : team.getMembers().keySet())
			playerTeam.remove(member, team.getId());
	}

	void index(UUID player, Team team) {
		playerTeam.put(player, team.getId());
	}

	void unindex(UUID player) {
		playerTeam.remove(player);
	}

	public static TeamSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
		TeamSavedData data = new TeamSavedData();
		ListTag list = tag.getList("Teams", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++)
			data.add(Team.load(list.getCompound(i)));
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
		ListTag list = new ListTag();
		for (Team team : teams.values())
			list.add(team.save());
		tag.put("Teams", list);
		return tag;
	}
}
