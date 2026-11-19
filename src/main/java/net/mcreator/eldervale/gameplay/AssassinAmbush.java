package net.mcreator.eldervale.gameplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Assassin passive: DEADLY AMBUSH.
 *  - +25% damage against enemies that are not targeting the Assassin.
 *  - Every hit that gets the bonus gives the Assassin speed and invisibility for 2 s,
 *    with a pause of 5 s before it can trigger again.
 *
 * This class REPLACES the MCreator procedure AssassinPassive (which only did the +25%):
 * delete that procedure in MCreator, otherwise the bonus is applied twice.
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class AssassinAmbush {

	// ---- Settings (edit here) ----
	private static final float BONUS = 0.25F;
	private static final int EFFECT_TICKS = 40;       // 2 s
	private static final int SPEED_LEVEL = 0;         // 0 = Speed I (the PDF does not give a level)
	private static final int PAUSE_TICKS = 100;       // 5 s before the effects can trigger again

	private static final Map<UUID, Long> NEXT_PROC = new HashMap<>();

	@SubscribeEvent
	public static void onDamage(LivingIncomingDamageEvent event) {
		if (!(event.getSource().getEntity() instanceof ServerPlayer player))
			return;
		LivingEntity target = event.getEntity();
		if (target == player)
			return;
		if (!"assassin".equals(player.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS))
			return;
		// the bonus does not apply while the enemy is already fighting the Assassin
		if (target instanceof Mob mob && mob.getTarget() == player)
			return;

		event.setAmount(event.getAmount() * (1.0F + BONUS));

		long now = player.level().getGameTime();
		Long next = NEXT_PROC.get(player.getUUID());
		if (next != null && now < next)
			return;
		NEXT_PROC.put(player.getUUID(), now + PAUSE_TICKS);
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, EFFECT_TICKS, 0, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, EFFECT_TICKS, SPEED_LEVEL, false, false));
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
