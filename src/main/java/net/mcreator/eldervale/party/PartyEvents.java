package net.mcreator.eldervale.party;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;

import net.minecraft.server.level.ServerPlayer;

/**
 * Party events (server only):
 *  - damage between allies is cancelled (melee, arrows and any damage whose source is an ally);
 *  - login / logout hooks.
 *
 * Skills that damage players WITHOUT a source entity (hurt with a bare DamageSource) are not
 * recognised here: give them the player as source (level.damageSources().playerAttack(player)).
 */
@EventBusSubscriber
public class PartyEvents {

	private static final int WARN_PAUSE_TICKS = 20; // the "cannot damage" toast appears at most once per second
	private static final Map<UUID, Long> LAST_WARN = new HashMap<>();

	// Highest priority: the hit is cancelled before DefenceHandler or other listeners touch it.
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer victim))
			return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer attacker))
			return;
		if (!PartyManager.areAllies(attacker, victim))
			return;

		event.setCanceled(true);
		long now = attacker.level().getGameTime();
		Long last = LAST_WARN.get(attacker.getUUID());
		if (last == null || now - last >= WARN_PAUSE_TICKS) {
			LAST_WARN.put(attacker.getUUID(), now);
			PartySync.toast(attacker, PartyNotifyPayload.ERROR, "Party", "You cannot damage members of your party.");
		}
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PartyManager.onLogin(player);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			PartyManager.onLogout(player);
			LAST_WARN.remove(player.getUUID());
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PartyManager.clearInvites();
		LAST_WARN.clear();
	}
}
