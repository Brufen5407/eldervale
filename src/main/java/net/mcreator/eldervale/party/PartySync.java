package net.mcreator.eldervale.party;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Server -> client traffic of the Party system, and the entry point of the GUI actions.
 *  - handleAction: runs one action of the GUI, then refreshes everybody it touched.
 *  - every UPDATE_INTERVAL ticks each party member receives a fresh snapshot (hp, mana, distance).
 *    Players without a party receive nothing, so the cost is zero outside parties.
 */
@EventBusSubscriber
public final class PartySync {

	private static final int UPDATE_INTERVAL = 10; // 0.5 s

	private PartySync() {
	}

	// ---------------------------------------------------------------
	//  Toasts
	// ---------------------------------------------------------------
	public static void toast(ServerPlayer player, int kind, String title, String text) {
		PacketDistributor.sendToPlayer(player, new PartyNotifyPayload(kind, title, text));
	}

	/** Toast for every online member of the party except one (pass null to include everybody). */
	public static void toastParty(MinecraftServer server, Party party, UUID except, int kind, String title, String text) {
		for (UUID id : party.getMembers().keySet()) {
			if (id.equals(except))
				continue;
			ServerPlayer member = server.getPlayerList().getPlayer(id);
			if (member != null)
				toast(member, kind, title, text);
		}
	}

	// ---------------------------------------------------------------
	//  Snapshot
	// ---------------------------------------------------------------
	public static PartyStatePayload build(ServerPlayer receiver) {
		MinecraftServer server = receiver.getServer();
		Party party = PartyManager.partyOf(server, receiver.getUUID());
		List<PartyStatePayload.MemberInfo> list = new ArrayList<>();
		String partyName = "";
		UUID leader = new UUID(0L, 0L);

		if (party != null) {
			partyName = party.getName();
			leader = party.getLeader();
			for (Map.Entry<UUID, Party.Member> e : party.getMembers().entrySet()) {
				ServerPlayer sp = server.getPlayerList().getPlayer(e.getKey());
				Party.Member stored = e.getValue();
				if (sp != null) {
					EldervaleModVariables.PlayerVariables v = sp.getData(EldervaleModVariables.PLAYER_VARIABLES);
					float distance = sp.level() == receiver.level() ? (float) sp.distanceTo(receiver) : -1.0F;
					list.add(new PartyStatePayload.MemberInfo(e.getKey(), sp.getGameProfile().getName(), v.PLAYER_CLASS, (int) Math.round(v.LEVEL), true, sp.getHealth(), sp.getMaxHealth(), (float) v.MANA, (float) v.MANA_MAX, distance));
				} else {
					list.add(new PartyStatePayload.MemberInfo(e.getKey(), stored.getName(), stored.getPlayerClass(), stored.getLevel(), false, 0.0F, 1.0F, 0.0F, 1.0F, -1.0F));
				}
			}
		}

		PartyManager.PendingInvite invite = PartyManager.pendingInvite(server, receiver.getUUID());
		return new PartyStatePayload(party != null, partyName, leader, PartyManager.MAX_MEMBERS, list, invite == null ? "" : invite.partyName(), invite == null ? "" : invite.fromName(), invite == null ? 0 : invite.secondsLeft());
	}

	public static void send(ServerPlayer receiver) {
		PacketDistributor.sendToPlayer(receiver, build(receiver));
	}

	// ---------------------------------------------------------------
	//  GUI actions
	// ---------------------------------------------------------------
	public static void handleAction(ServerPlayer player, int action, String arg) {
		MinecraftServer server = player.getServer();
		if (arg == null)
			arg = "";

		// everybody touched by the action must refresh: the old members, the new ones, the invited player
		Set<UUID> recipients = new HashSet<>();
		recipients.add(player.getUUID());
		Party before = PartyManager.partyOf(server, player.getUUID());
		if (before != null)
			recipients.addAll(before.getMembers().keySet());

		PartyManager.Result result = null;
		switch (action) {
			case PartyActionPayload.CREATE -> result = PartyManager.create(player, arg);
			case PartyActionPayload.INVITE -> {
				ServerPlayer target = server.getPlayerList().getPlayerByName(arg);
				if (target == null) {
					result = new PartyManager.Result(false, "Player not found.");
				} else {
					result = PartyManager.invite(player, target);
					recipients.add(target.getUUID());
				}
			}
			case PartyActionPayload.ACCEPT -> result = PartyManager.accept(player);
			case PartyActionPayload.DECLINE -> result = PartyManager.decline(player);
			case PartyActionPayload.LEAVE -> result = PartyManager.leave(player);
			case PartyActionPayload.DISBAND -> result = PartyManager.disband(player);
			case PartyActionPayload.KICK -> result = PartyManager.kick(player, arg);
			case PartyActionPayload.LEADER -> result = PartyManager.transfer(player, arg);
			case PartyActionPayload.RENAME -> result = PartyManager.rename(player, arg);
			default -> {
			}
		}

		Party after = PartyManager.partyOf(server, player.getUUID());
		if (after != null)
			recipients.addAll(after.getMembers().keySet());

		if (result != null)
			toast(player, result.ok() ? PartyNotifyPayload.SUCCESS : PartyNotifyPayload.ERROR, "Party", result.message());

		for (UUID id : recipients) {
			ServerPlayer receiver = server.getPlayerList().getPlayer(id);
			if (receiver != null)
				send(receiver);
		}
	}

	// ---------------------------------------------------------------
	//  Periodic update
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		MinecraftServer server = event.getServer();
		if (server.getTickCount() % UPDATE_INTERVAL != 0)
			return;
		for (ServerPlayer player : server.getPlayerList().getPlayers())
			if (PartyManager.partyOf(server, player.getUUID()) != null)
				send(player);
	}
}
