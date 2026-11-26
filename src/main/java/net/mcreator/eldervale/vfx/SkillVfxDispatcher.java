package net.mcreator.eldervale.vfx;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Sends the VFX event for the skills that are implemented by MCreator procedures and have no Java class of their own
 * (the Paladin skills). It detects the cast the same way the other skill classes do: the skill cooldown jumps up to its maximum.
 * It only sends a packet: it never changes mana, cooldown, damage or any other gameplay value.
 * Server only (no client classes): safe on a dedicated server.
 */
@EventBusSubscriber
public final class SkillVfxDispatcher {

	// ---- Settings (edit here) ----
	private static final float SHIELD_RADIUS = 1.3F;
	private static final float ZONE_RADIUS = 5.0F;       // same box as the Holy Zone procedure (5 blocks each side)

	private static final Map<UUID, double[]> PREV_CD = new HashMap<>();

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		double cd1 = v.SKILL1_CD;
		double cd2 = v.SKILL2_CD;
		double[] prev = PREV_CD.get(player.getUUID());
		if (prev == null) {
			PREV_CD.put(player.getUUID(), new double[]{cd1, cd2});
			return;
		}
		double p1 = prev[0];
		double p2 = prev[1];
		prev[0] = cd1;
		prev[1] = cd2;

		if (!player.isAlive() || !"paladin".equals(v.PLAYER_CLASS))
			return;

		// cast: the cooldown jumped up to its maximum
		if (cd1 - p1 > 50.0D && cd1 >= v.SKILL1_MAX_CD - 10.0D)
			VfxServer.send(player, VfxIds.PALADIN_SHIELDS, VfxIds.START, 100, SHIELD_RADIUS);
		if (cd2 - p2 > 50.0D && cd2 >= v.SKILL2_MAX_CD - 10.0D)
			VfxServer.sendAt(player, VfxIds.PALADIN_ZONE, VfxIds.START, 100, player.getX(), player.getY(), player.getZ(), ZONE_RADIUS);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		PREV_CD.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PREV_CD.clear();
	}

	private SkillVfxDispatcher() {
	}
}
