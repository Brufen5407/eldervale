package net.mcreator.eldervale.party;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Rules of the Party system (server only). All actions return a Result with a message
 * that the caller (the GUI packets) shows to the player as a toast.
 *
 * Use areAllies(...) from skills: it is the single place that says who is an ally.
 */
public final class PartyManager {

	// ---- Settings (edit here) ----
	public static final int MAX_MEMBERS = 5;      // leader + 4 members
	private static final int INVITE_TICKS = 1200; // an invite expires after 60 s
	private static final int NAME_MIN = 3;
	private static final int NAME_MAX = 16;

	public record Result(boolean ok, String message) {
	}

	public record PendingInvite(String partyName, String fromName, int secondsLeft) {
	}

	private record Invite(UUID party, UUID from, long expires) {
	}

	private static final Map<UUID, Invite> INVITES = new HashMap<>(); // invited player -> invite

	private PartyManager() {
	}

	private static Result success(String message) {
		return new Result(true, message);
	}

	private static Result failure(String message) {
		return new Result(false, message);
	}

	private static PartySavedData data(MinecraftServer server) {
		return PartySavedData.get(server);
	}

	// ---------------------------------------------------------------
	//  Queries
	// ---------------------------------------------------------------
	public static Party partyOf(MinecraftServer server, UUID player) {
		return data(server).getPartyOf(player);
	}

	/** True if both are in the same party and are different players (self is not an ally). */
	public static boolean areAllies(MinecraftServer server, UUID a, UUID b) {
		if (a.equals(b))
			return false;
		Party party = data(server).getPartyOf(a);
		return party != null && party.has(b);
	}

	public static boolean areAllies(Entity a, Entity b) {
		if (!(a instanceof ServerPlayer pa) || !(b instanceof ServerPlayer pb))
			return false;
		return areAllies(pa.getServer(), pa.getUUID(), pb.getUUID());
	}

	/** The invite waiting for this player, or null (expired invites are removed). */
	public static PendingInvite pendingInvite(MinecraftServer server, UUID player) {
		Invite invite = INVITES.get(player);
		if (invite == null)
			return null;
		long left = invite.expires() - server.overworld().getGameTime();
		if (left <= 0) {
			INVITES.remove(player);
			return null;
		}
		Party party = data(server).byId(invite.party());
		if (party == null) {
			INVITES.remove(player);
			return null;
		}
		String from = party.nameOf(invite.from());
		return new PendingInvite(party.getName(), from == null ? "?" : from, (int) Math.ceil(left / 20.0D));
	}

	// ---------------------------------------------------------------
	//  Actions
	// ---------------------------------------------------------------
	public static Result create(ServerPlayer player, String rawName) {
		PartySavedData d = data(player.getServer());
		if (d.getPartyOf(player.getUUID()) != null)
			return failure("You are already in a party.");
		String name = rawName.trim();
		String error = validateName(d, name, null);
		if (error != null)
			return failure(error);
		Party party = new Party(UUID.randomUUID(), name, player.getUUID(), player.getGameProfile().getName());
		saveStats(party, player);
		d.add(party);
		d.setDirty();
		INVITES.remove(player.getUUID());
		return success("Party \"" + name + "\" created. You are the leader.");
	}

	public static Result invite(ServerPlayer leader, ServerPlayer target) {
		MinecraftServer server = leader.getServer();
		PartySavedData d = data(server);
		Party party = d.getPartyOf(leader.getUUID());
		if (party == null)
			return failure("You are not in a party.");
		if (!party.isLeader(leader.getUUID()))
			return failure("Only the leader can invite players.");
		if (target.getUUID().equals(leader.getUUID()))
			return failure("You cannot invite yourself.");
		if (d.getPartyOf(target.getUUID()) != null)
			return failure(target.getGameProfile().getName() + " is already in a party.");
		if (party.size() >= MAX_MEMBERS)
			return failure("The party is full (" + MAX_MEMBERS + " players).");

		INVITES.put(target.getUUID(), new Invite(party.getId(), leader.getUUID(), server.overworld().getGameTime() + INVITE_TICKS));
		PartySync.toast(target, PartyNotifyPayload.INVITE, "Party invite", leader.getGameProfile().getName() + " invited you to \"" + party.getName() + "\".");
		return success("Invite sent to " + target.getGameProfile().getName() + ".");
	}

	public static Result accept(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		PartySavedData d = data(server);
		Invite invite = takeInvite(server, player.getUUID());
		if (invite == null)
			return failure("You have no pending invite.");
		if (d.getPartyOf(player.getUUID()) != null)
			return failure("You are already in a party.");
		Party party = d.byId(invite.party());
		if (party == null)
			return failure("That party no longer exists.");
		if (party.size() >= MAX_MEMBERS)
			return failure("The party is full (" + MAX_MEMBERS + " players).");

		String name = player.getGameProfile().getName();
		party.addMember(player.getUUID(), name);
		saveStats(party, player);
		d.index(player.getUUID(), party);
		d.setDirty();
		PartySync.toastParty(server, party, player.getUUID(), PartyNotifyPayload.JOIN, "Party", name + " joined the party.");
		return success("You joined \"" + party.getName() + "\".");
	}

	public static Result decline(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		Invite invite = takeInvite(server, player.getUUID());
		if (invite == null)
			return failure("You have no pending invite.");
		ServerPlayer from = server.getPlayerList().getPlayer(invite.from());
		if (from != null)
			PartySync.toast(from, PartyNotifyPayload.INFO, "Party", player.getGameProfile().getName() + " declined your invite.");
		return success("Invite declined.");
	}

	public static Result leave(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		PartySavedData d = data(server);
		Party party = d.getPartyOf(player.getUUID());
		if (party == null)
			return failure("You are not in a party.");

		String name = player.getGameProfile().getName();
		if (party.size() == 1) {
			d.remove(party);
			d.setDirty();
			return success("You left the party. It was disbanded because it was empty.");
		}
		party.removeMember(player.getUUID());
		d.unindex(player.getUUID());
		if (party.isLeader(player.getUUID())) {
			UUID next = party.getMembers().keySet().iterator().next(); // longest-standing member
			party.setLeader(next);
			PartySync.toastParty(server, party, null, PartyNotifyPayload.CROWN, "New leader", party.nameOf(next) + " now leads the party.");
		}
		d.setDirty();
		PartySync.toastParty(server, party, null, PartyNotifyPayload.LEAVE, "Party", name + " left the party.");
		return success("You left \"" + party.getName() + "\".");
	}

	public static Result disband(ServerPlayer leader) {
		MinecraftServer server = leader.getServer();
		PartySavedData d = data(server);
		Party party = d.getPartyOf(leader.getUUID());
		if (party == null)
			return failure("You are not in a party.");
		if (!party.isLeader(leader.getUUID()))
			return failure("Only the leader can disband the party.");
		PartySync.toastParty(server, party, leader.getUUID(), PartyNotifyPayload.INFO, "Party", "\"" + party.getName() + "\" was disbanded.");
		d.remove(party);
		d.setDirty();
		return success("Party disbanded.");
	}

	public static Result kick(ServerPlayer leader, String memberName) {
		MinecraftServer server = leader.getServer();
		PartySavedData d = data(server);
		Party party = d.getPartyOf(leader.getUUID());
		if (party == null)
			return failure("You are not in a party.");
		if (!party.isLeader(leader.getUUID()))
			return failure("Only the leader can remove members.");
		UUID target = party.findByName(memberName);
		if (target == null)
			return failure("\"" + memberName + "\" is not in your party.");
		if (target.equals(leader.getUUID()))
			return failure("You cannot remove yourself: use leave or disband.");

		String name = party.nameOf(target);
		party.removeMember(target);
		d.unindex(target);
		d.setDirty();
		ServerPlayer kicked = server.getPlayerList().getPlayer(target);
		if (kicked != null)
			PartySync.toast(kicked, PartyNotifyPayload.ERROR, "Party", "You were removed from \"" + party.getName() + "\".");
		PartySync.toastParty(server, party, null, PartyNotifyPayload.LEAVE, "Party", name + " was removed from the party.");
		// the removed player must refresh his own state too
		if (kicked != null)
			PartySync.send(kicked);
		return success(name + " removed.");
	}

	public static Result transfer(ServerPlayer leader, String memberName) {
		MinecraftServer server = leader.getServer();
		PartySavedData d = data(server);
		Party party = d.getPartyOf(leader.getUUID());
		if (party == null)
			return failure("You are not in a party.");
		if (!party.isLeader(leader.getUUID()))
			return failure("Only the leader can transfer leadership.");
		UUID target = party.findByName(memberName);
		if (target == null)
			return failure("\"" + memberName + "\" is not in your party.");
		if (target.equals(leader.getUUID()))
			return failure("You are already the leader.");

		party.setLeader(target);
		d.setDirty();
		PartySync.toastParty(server, party, null, PartyNotifyPayload.CROWN, "New leader", party.nameOf(target) + " now leads the party.");
		return success("Leadership transferred.");
	}

	public static Result rename(ServerPlayer leader, String rawName) {
		MinecraftServer server = leader.getServer();
		PartySavedData d = data(server);
		Party party = d.getPartyOf(leader.getUUID());
		if (party == null)
			return failure("You are not in a party.");
		if (!party.isLeader(leader.getUUID()))
			return failure("Only the leader can rename the party.");
		String name = rawName.trim();
		String error = validateName(d, name, party);
		if (error != null)
			return failure(error);
		party.setName(name);
		d.setDirty();
		PartySync.toastParty(server, party, leader.getUUID(), PartyNotifyPayload.INFO, "Party", "The party is now called \"" + name + "\".");
		return success("Party renamed.");
	}

	// ---------------------------------------------------------------
	//  Login / logout (the member stays in the party when offline)
	// ---------------------------------------------------------------
	public static void onLogin(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		PartySavedData d = data(server);
		Party party = d.getPartyOf(player.getUUID());
		if (party == null)
			return;
		String name = player.getGameProfile().getName();
		if (!name.equals(party.nameOf(player.getUUID())))
			party.setMemberName(player.getUUID(), name); // the player changed nickname
		saveStats(party, player);
		d.setDirty();
		PartySync.toastParty(server, party, player.getUUID(), PartyNotifyPayload.INFO, "Party", name + " is online.");
	}

	public static void onLogout(ServerPlayer player) {
		MinecraftServer server = player.getServer();
		INVITES.remove(player.getUUID());
		PartySavedData d = data(server);
		Party party = d.getPartyOf(player.getUUID());
		if (party == null)
			return;
		saveStats(party, player);
		d.setDirty();
		PartySync.toastParty(server, party, player.getUUID(), PartyNotifyPayload.INFO, "Party", player.getGameProfile().getName() + " went offline.");
	}

	public static void clearInvites() {
		INVITES.clear();
	}

	// ---------------------------------------------------------------
	//  Helpers
	// ---------------------------------------------------------------
	private static void saveStats(Party party, ServerPlayer player) {
		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		party.setMemberStats(player.getUUID(), v.PLAYER_CLASS, (int) Math.round(v.LEVEL));
	}

	/** Returns and removes the invite, or null if there is none or it expired. */
	private static Invite takeInvite(MinecraftServer server, UUID player) {
		Invite invite = INVITES.remove(player);
		if (invite == null || server.overworld().getGameTime() > invite.expires())
			return null;
		return invite;
	}

	private static String validateName(PartySavedData d, String name, Party self) {
		if (name.length() < NAME_MIN || name.length() > NAME_MAX)
			return "The party name must have " + NAME_MIN + " to " + NAME_MAX + " characters.";
		if (!name.matches("[\\p{L}\\p{N}_ ]+"))
			return "The party name can only contain letters, numbers, spaces and underscores.";
		for (Party p : d.all())
			if (p != self && p.getName().equalsIgnoreCase(name))
				return "A party with that name already exists.";
		return null;
	}
}
