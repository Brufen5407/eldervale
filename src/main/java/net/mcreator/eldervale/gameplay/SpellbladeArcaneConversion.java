package net.mcreator.eldervale.gameplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Spellblade passive: ARCANE CONVERSION.
 *  - Every physical (melee) attack that lands restores MANA_GAIN mana.
 *  - It can trigger once every COOLDOWN_TICKS (2 s).
 *  - Skill hits (Arcane Slash, Phantom Edge) do not count: SpellbladeSkills sets skillDamage.
 *  - If mana is already full the effect is not consumed, so the 2 s pause is not wasted.
 *
 * Uses LivingDamageEvent.Post, so only hits that actually dealt damage count ("andato a segno").
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class SpellbladeArcaneConversion {

	// ---- Settings (edit here) ----
	private static final double MANA_GAIN = 2.0D;
	private static final int COOLDOWN_TICKS = 40;    // 2 s

	/** true only while SpellbladeSkills is applying skill damage (server thread, synchronous) */
	static boolean skillDamage = false;

	private static final Map<UUID, Long> NEXT_PROC = new HashMap<>();

	@SubscribeEvent
	public static void onDamage(LivingDamageEvent.Post event) {
		if (skillDamage)
			return;
		DamageSource source = event.getSource();
		if (!source.is(DamageTypes.PLAYER_ATTACK))
			return;
		if (!(source.getEntity() instanceof ServerPlayer player) || event.getEntity() == player)
			return;
		if (event.getNewDamage() <= 0.0F)
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		if (!"spellblade".equals(v.PLAYER_CLASS) || v.MANA >= v.MANA_MAX)
			return;

		long now = player.level().getGameTime();
		Long next = NEXT_PROC.get(player.getUUID());
		if (next != null && now < next)
			return;
		NEXT_PROC.put(player.getUUID(), now + COOLDOWN_TICKS);

		v.MANA = Math.min(v.MANA_MAX, v.MANA + MANA_GAIN);
		v.markSyncDirty();
	}

	// ---------------------------------------------------------------
	//  Cleanup
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		NEXT_PROC.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		NEXT_PROC.clear();
	}
}
