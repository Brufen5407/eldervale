package net.mcreator.eldervale.team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;

/**
 * Rules of the Team system (server only). All actions return a Result with a message
 * that the caller (commands now, GUI packets later) shows to the player.
 *
 * Use areTeammates(...) from skills: it is the single place that says who is an ally.
 */
public final class TeamManager {

	// ---- Settings (edit here) ----
	public static final int MAX_MEMBERS = 5;      // leader + 4 members
	private static final int INVITE_TICKS = 1200; // an invite expires after 60 s
	private static final int NAME_MIN = 3;
	private static final int NAME_MAX = 16;

	public record Result(boolean ok, String message) {
	}

	private record Invite(UUID team, UUID from, long expires) {
	}

	private static final Map<UUID, Invite> INVITES = new HashMap<>(); // invited player -> invite

	private TeamManager() {
	}

	private static Result success(String message) {
		return new Result(true, message);
	}

	private static Result failure(String message) {
		return new Result(false, message);
	}

	private static TeamSavedData data(MinecraftServer server) {
		return TeamSavedData.get(server);
	}

	// ---------------------------------------------------------------
	//  Queries
	// ---------------------------------------------------------------
	public static Team teamOf(MinecraftServer server, UUID player) {
		return data(server).getTeamOf(player);
	}

	/** True if both are in the same team and are different players (self is not a teammate). */
	public static boolean areTeammates(MinecraftServer server, UUID a, UUID b) {
		if (a.equals(b))
			return false;
		Team team = data(server).getTeamOf(a);
		return team != null && team.has(b);
	}

	public static boolean areTeammates(Entity a, Entity b) {
		if (!(a instanceof ServerPlayer pa) || !(b instanceof ServerPlayer pb))
			return false;
		return areTeammates(pa.getServer(), pa.getUUID(), pb.getUUID());
	}

	// ---------------------------------------------------------------
	//  Actions
	// ---------------------------------------------------------------
	public static Result create(ServerPlayer player, String rawName) {
		TeamSavedData d = data(player.getServer());
		if (d.getTeamOf(player.getUUID()) != null)
			return failure("You are already in a team.");
		String name = rawName.trim();
		String error = validateName(d, name, null);
		if (error != null)
			return failure(error);
		Team team = new Team(UUID.randomUUID(), name, player.getUUID(), player.getGameProfile().getName());
		d.add(team);
		d.setDirty();
		INVITES.remove(player.getUUID());
		return success("Team \"" + name + "\" created. You are the leader.");
	}

	public static Result invite(ServerPlayer leader, ServerPlayer target) {
		MinecraftServer server = leader.getServer();
		TeamSavedData d = data(server);
		Team team = d.getTeamOf(leader.getUUID());
		if (team == null)
			return failure("You are not in a team.");
		if (!team.isLeader(leader.getUUID()))
			return failure("Only the leader can invite players.");
		if (target.getUUID().equals(leader.getUUID()))
			return failure("You cannot invite yourself.");
		if (d.getTeamOf(target.getUUID()) != null)
			return failure(target.getGameProfile().getName() + " is already in a team.");
		if (team.size() >= MAX_MEMBERS)
			return failure("The team is full (" + MAX_MEMBERS + " players).");

		INVITES.put(target.getUUID(), new Invite(team.getId(), leader.getUUID(), server.overworld().getGameTime() + INVITE_TICKS));
		target.sendSystemMessage(Component.literal("[Team] " + leader.getGameProfile().getName() + " invited you to \"" + team.getName() + "\". Use /eteam accept or /eteam decline (60 s)."));
		return success("Invite sent to " + target.getGameProfile().getName() + ".");
	}

	public static Result accept(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		TeamSavedData d = data(server);
		Invite invite = takeInvite(server, player.getUUID());
		if (invite == null)
			return failure("You have no pending invite.");
		if (d.getTeamOf(player.getUUID()) != null)
			return failure("You are already in a team.");
		Team team = null;
		for (Team t : d.all())
			if (t.getId().equals(invite.team()))
				team = t;
		if (team == null)
			return failure("That team no longer exists.");
		if (team.size() >= MAX_MEMBERS)
			return failure("The team is full (" + MAX_MEMBERS + " players).");

		String name = player.getGameProfile().getName();
		team.addMember(player.getUUID(), name);
		d.index(player.getUUID(), team);
		d.setDirty();
		broadcast(server, team, name + " joined the team.");
		return success("You joined \"" + team.getName() + "\".");
	}

	public static Result decline(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		Invite invite = takeInvite(server, player.getUUID());
		if (invite == null)
			return failure("You have no pending invite.");
		ServerPlayer from = server.getPlayerList().getPlayer(invite.from());
		if (from != null)
			from.sendSystemMessage(Component.literal("[Team] " + player.getGameProfile().getName() + " declined your invite."));
		return success("Invite declined.");
	}

	public static Result leave(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		TeamSavedData d = data(server);
		Team team = d.getTeamOf(player.getUUID());
		if (team == null)
			return failure("You are not in a team.");

		String name = player.getGameProfile().getName();
		if (team.size() == 1) {
			d.remove(team);
			d.setDirty();
			return success("You left the team. It was disbanded because it was empty.");
		}
		team.removeMember(player.getUUID());
		d.unindex(player.getUUID());
		if (team.isLeader(player.getUUID())) {
			UUID next = team.getMembers().keySet().iterator().next(); // longest-standing member
			team.setLeader(next);
			broadcast(server, team, team.nameOf(next) + " is the new leader.");
		}
		d.setDirty();
		broadcast(server, team, name + " left the team.");
		return success("You left \"" + team.getName() + "\".");
	}

	public static Result disband(ServerPlayer leader) {
		MinecraftServer server = leader.getServer();
		TeamSavedData d = data(server);
		Team team = d.getTeamOf(leader.getUUID());
		if (team == null)
			return failure("You are not in a team.");
		if (!team.isLeader(leader.getUUID()))
			return failure("Only the leader can disband the team.");
		broadcast(server, team, "The team \"" + team.getName() + "\" was disbanded.");
		d.remove(team);
		d.setDirty();
		return success("Team disbanded.");
	}

	public static Result kick(ServerPlayer leader, String memberName) {
		MinecraftServer server = leader.getServer();
		TeamSavedData d = data(server);
		Team team = d.getTeamOf(leader.getUUID());
		if (team == null)
			return failure("You are not in a team.");
		if (!team.isLeader(leader.getUUID()))
			return failure("Only the leader can remove members.");
		UUID target = team.findByName(memberName);
		if (target == null)
			return failure("\"" + memberName + "\" is not in your team.");
		if (target.equals(leader.getUUID()))
			return failure("You cannot remove yourself: use leave or disband.");

		String name = team.nameOf(target);
		team.removeMember(target);
		d.unindex(target);
		d.setDirty();
		ServerPlayer kicked = server.getPlayerList().getPlayer(target);
		if (kicked != null)
			kicked.sendSystemMessage(Component.literal("[Team] You were removed from \"" + team.getName() + "\"."));
		broadcast(server, team, name + " was removed from the team.");
		return success(name + " removed.");
	}

	public static Result transfer(ServerPlayer leader, String memberName) {
		MinecraftServer server = leader.getServer();
		TeamSavedData d = data(server);
		Team team = d.getTeamOf(leader.getUUID());
		if (team == null)
			return failure("You are not in a team.");
		if (!team.isLeader(leader.getUUID()))
			return failure("Only the leader can transfer leadership.");
		UUID target = team.findByName(memberName);
		if (target == null)
			return failure("\"" + memberName + "\" is not in your team.");
		if (target.equals(leader.getUUID()))
			return failure("You are already the leader.");

		team.setLeader(target);
		d.setDirty();
		broadcast(server, team, team.nameOf(target) + " is the new leader.");
		return success("Leadership transferred.");
	}

	public static Result rename(ServerPlayer leader, String rawName) {
		MinecraftServer server = leader.getServer();
		TeamSavedData d = data(server);
		Team team = d.getTeamOf(leader.getUUID());
		if (team == null)
			return failure("You are not in a team.");
		if (!team.isLeader(leader.getUUID()))
			return failure("Only the leader can rename the team.");
		String name = rawName.trim();
		String error = validateName(d, name, team);
		if (error != null)
			return failure(error);
		team.setName(name);
		d.setDirty();
		broadcast(server, team, "The team is now called \"" + name + "\".");
		return success("Team renamed.");
	}

	public static Result info(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		Team team = data(server).getTeamOf(player.getUUID());
		if (team == null)
			return failure("You are not in a team.");
		StringBuilder sb = new StringBuilder("Team \"" + team.getName() + "\" (" + team.size() + "/" + MAX_MEMBERS + ")");
		for (Map.Entry<UUID, String> e : team.getMembers().entrySet()) {
			boolean online = server.getPlayerList().getPlayer(e.getKey()) != null;
			sb.append("\n- ").append(e.getValue());
			if (team.isLeader(e.getKey()))
				sb.append(" (leader)");
			sb.append(online ? " [online]" : " [offline]");
		}
		return success(sb.toString());
	}

	// ---------------------------------------------------------------
	//  Login / logout (the member stays in the team when offline)
	// ---------------------------------------------------------------
	public static void onLogin(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		TeamSavedData d = data(server);
		Team team = d.getTeamOf(player.getUUID());
		if (team == null)
			return;
		String name = player.getGameProfile().getName();
		if (!name.equals(team.nameOf(player.getUUID()))) {
			team.setMemberName(player.getUUID(), name); // the player changed nickname
			d.setDirty();
		}
		broadcastExcept(server, team, player.getUUID(), name + " is online.");
	}

	public static void onLogout(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		INVITES.remove(player.getUUID());
		Team team = data(server).getTeamOf(player.getUUID());
		if (team != null)
			broadcastExcept(server, team, player.getUUID(), player.getGameProfile().getName() + " went offline.");
	}

	public static void clearInvites() {
		INVITES.clear();
	}

	// ---------------------------------------------------------------
	//  Helpers
	// ---------------------------------------------------------------
	/** Returns and removes the invite, or null if there is none or it expired. */
	private static Invite takeInvite(MinecraftServer server, UUID player) {
		Invite invite = INVITES.remove(player);
		if (invite == null || server.overworld().getGameTime() > invite.expires())
			return null;
		return invite;
	}

	private static String validateName(TeamSavedData d, String name, Team self) {
		if (name.length() < NAME_MIN || name.length() > NAME_MAX)
			return "The team name must have " + NAME_MIN + " to " + NAME_MAX + " characters.";
		if (!name.matches("[\\p{L}\\p{N}_ ]+"))
			return "The team name can only contain letters, numbers, spaces and underscores.";
		for (Team t : d.all())
			if (t != self && t.getName().equalsIgnoreCase(name))
				return "A team with that name already exists.";
		return null;
	}

	public static void broadcast(MinecraftServer server, Team team, String message) {
		broadcastExcept(server, team, null, message);
	}

	private static void broadcastExcept(MinecraftServer server, Team team, UUID except, String message) {
		Component text = Component.literal("[Team] " + message);
		for (UUID id : team.getMembers().keySet()) {
			if (id.equals(except))
				continue;
			ServerPlayer member = server.getPlayerList().getPlayer(id);
			if (member != null)
				member.sendSystemMessage(text);
		}
	}
}
