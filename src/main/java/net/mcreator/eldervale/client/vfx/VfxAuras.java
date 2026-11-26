package net.mcreator.eldervale.client.vfx;

import java.util.HashSet;
import java.util.Set;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Passive auras that last as long as a state lasts. They are not cast by the server:
 * the client starts and stops them by watching the synced player variables (and the health of nearby players),
 * so every player sees every aura, even if the player came into view while it was already active.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class VfxAuras {

	private static final double MAX_DISTANCE = 64.0D;

	private static final Set<Integer> BERSERK = new HashSet<>();
	private static final Set<Integer> REFLEX = new HashSet<>();
	private static final Set<Integer> BEAST = new HashSet<>();
	private static final Set<Integer> INSTINCT = new HashSet<>();

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			BERSERK.clear();
			REFLEX.clear();
			BEAST.clear();
			INSTINCT.clear();
			return;
		}
		if (mc.isPaused() || !VfxManager.EFFECTS_ENABLED)
			return;

		Set<Integer> seen = new HashSet<>();
		for (Player p : level.players()) {
			if (p.distanceToSqr(mc.player) > MAX_DISTANCE * MAX_DISTANCE)
				continue;
			int id = p.getId();
			seen.add(id);
			EldervaleModVariables.PlayerVariables v = p.getData(EldervaleModVariables.PLAYER_VARIABLES);
			watch(BERSERK, VfxIds.BERSERK_MODE, p, id, "berserker".equals(v.PLAYER_CLASS) && v.berserk_mode);
			// Crazy Reflex: starts when the cooldown flag turns on (the effect ends by itself after 3 s)
			boolean reflex = "ranger".equals(v.PLAYER_CLASS) && v.ranger_hitcooldown;
			if (reflex && REFLEX.add(id))
				VfxManager.handle(payload(VfxIds.RANGER_REFLEX, VfxIds.START, 0, p));
			else if (!reflex)
				REFLEX.remove(id);
			// Beast Transformation: werewolf_mode is synced by the server
			watch(BEAST, VfxIds.WEREWOLF_BEAST, p, id, "werewolf".equals(v.PLAYER_CLASS) && v.werewolf_mode);
			// Predator's Instinct: a werewolf below 50% of its health
			boolean instinct = "werewolf".equals(v.PLAYER_CLASS) && p.isAlive() && p.getHealth() < p.getMaxHealth() * 0.5F;
			watch(INSTINCT, VfxIds.WEREWOLF_INSTINCT, p, id, instinct);
		}
		BERSERK.retainAll(seen);
		REFLEX.retainAll(seen);
		BEAST.retainAll(seen);
		INSTINCT.retainAll(seen);
	}

	/** Starts the aura when the state turns on, stops it when it turns off. First sight of an active aura starts it silently. */
	private static void watch(Set<Integer> active, int vfxId, Player p, int id, boolean on) {
		boolean was = active.contains(id);
		if (on && !was) {
			active.add(id);
			VfxManager.handle(payload(vfxId, VfxIds.START, 0, p));
		} else if (!on && was) {
			active.remove(id);
			VfxManager.handle(payload(vfxId, VfxIds.END, 0, p));
		}
	}

	private static VfxEventPayload payload(int vfxId, int phase, int arg, Player p) {
		return new VfxEventPayload(vfxId, phase, arg, p.getId(), p.getId() * 7919 + 13, p.getX(), p.getY(), p.getZ(), 0.0F, 0.0F, 0.0F, 1.0F);
	}

	private VfxAuras() {
	}
}
