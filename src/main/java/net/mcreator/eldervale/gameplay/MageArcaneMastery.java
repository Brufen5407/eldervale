package net.mcreator.eldervale.gameplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;

/**
 * Mage passive: ARCANE MASTERY.
 *  - Every skill cast gives +10% magic damage for 4 s, stacking up to 3 times (max +30%).
 *  - Casting again refreshes the 4 s timer of all stacks.
 *  - The bonus applies to the damage of Mage skills (Arcane Nova, Meteor Strike), read when the
 *    damage is dealt: a Meteor Strike that lands while the stacks are still active gets the bonus.
 *
 * State only, no events besides cleanup: MageSkills calls onCast() and multiplier().
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class MageArcaneMastery {

	// ---- Settings (edit here) ----
	private static final double BONUS_PER_STACK = 0.10D;
	private static final int MAX_STACKS = 3;
	private static final int DURATION_TICKS = 80;    // 4 s

	private static final class State {
		int stacks;
		long expires;
	}

	private static final Map<UUID, State> STATES = new HashMap<>();

	/** call once per skill cast */
	static void onCast(ServerPlayer player) {
		long now = player.level().getGameTime();
		State s = STATES.computeIfAbsent(player.getUUID(), k -> new State());
		if (now >= s.expires)
			s.stacks = 0;
		s.stacks = Math.min(MAX_STACKS, s.stacks + 1);
		s.expires = now + DURATION_TICKS;
	}

	/** damage multiplier (1.0 when no stacks are active) */
	static double multiplier(ServerPlayer player) {
		State s = STATES.get(player.getUUID());
		if (s == null || player.level().getGameTime() >= s.expires)
			return 1.0D;
		return 1.0D + BONUS_PER_STACK * s.stacks;
	}

	// ---------------------------------------------------------------
	//  Cleanup
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		STATES.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		STATES.clear();
	}
}
