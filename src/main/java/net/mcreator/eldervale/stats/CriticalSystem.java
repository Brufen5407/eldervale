package net.mcreator.eldervale.stats;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Critical hits for player attacks (damage type PLAYER_ATTACK, includes skills that use it).
 * Runs at HIGH priority so Ambush (+25 %) and other NORMAL handlers see the critical amount.
 * Skills with other damage types can call rollMultiplier() explicitly.
 *
 * Feedback (each part can be switched off in StatsConfig): particles, sound, floating "CRITICAL!" text.
 */
@EventBusSubscriber
public class CriticalSystem {

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (!event.getSource().is(DamageTypes.PLAYER_ATTACK))
			return;
		if (!(event.getSource().getEntity() instanceof ServerPlayer player) || event.getEntity() == player)
			return;
		float multiplier = rollMultiplier(player);
		if (multiplier <= 1.0F)
			return;
		event.setAmount(event.getAmount() * multiplier);
		playFeedback(player, event.getEntity());
	}

	/** Returns 1.0 when there is no critical, otherwise CRITICAL_DAMAGE / 100. Public API for skills. */
	public static float rollMultiplier(ServerPlayer player) {
		EldervaleModVariables.PlayerVariables vars = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		if (player.getRandom().nextDouble() * 100.0 < vars.CRITICAL_CHANCE)
			return (float) (vars.CRITICAL_DAMAGE / 100.0);
		return 1.0F;
	}

	/** Particles, sound and popup for a critical hit. Skills that roll manually can call this too. */
	public static void playFeedback(ServerPlayer attacker, Entity target) {
		if (!(target.level() instanceof ServerLevel level))
			return;
		double cx = target.getX();
		double cy = target.getY() + target.getBbHeight() * 0.5;
		double cz = target.getZ();
		if (StatsConfig.CRIT_EFFECTS)
			level.sendParticles(ParticleTypes.CRIT, cx, cy, cz, 12, 0.3, 0.3, 0.3, 0.2);
		if (StatsConfig.CRIT_SOUND) {
			level.playSound(null, cx, cy, cz, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
			level.playSound(null, cx, cy, cz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.6F);
		}
		if (StatsConfig.CRIT_TEXT)
			PacketDistributor.sendToPlayer(attacker, new PopupPayload(cx, target.getY() + target.getBbHeight() + 0.2, cz, PopupPayload.CRIT, 0));
	}
}
