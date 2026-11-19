package net.mcreator.eldervale.team;

import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Team events (server only):
 *  - damage between teammates is cancelled (melee, arrows and any damage whose source is a teammate);
 *  - join / leave notifications for the team.
 *
 * Skills that damage players WITHOUT a source entity (hurt with a bare DamageSource) are not
 * recognised here: give them the player as source (level.damageSources().playerAttack(player)).
 */
@EventBusSubscriber
public class TeamEvents {

	// Highest priority: the hit is cancelled before DefenceHandler or other listeners touch it.
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer victim))
			return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer attacker))
			return;
		if (TeamManager.areTeammates(attacker, victim)) {
			event.setCanceled(true);
			attacker.displayClientMessage(Component.literal("You cannot damage members of your team."), true);
		}
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			TeamManager.onLogin(player);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			TeamManager.onLogout(player);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		TeamManager.clearInvites();
	}
}
