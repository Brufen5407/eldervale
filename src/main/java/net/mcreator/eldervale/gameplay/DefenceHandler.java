package net.mcreator.eldervale.gameplay;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Defence and Holy Shields. Replaces DefenceappliedProcedure.
 * Runs only on the server and only for players, so it is safe on a dedicated server.
 *
 * Order of the pipeline for damage received by a player:
 *   MobScalingProcedure (HIGHEST, mob level multiplier) -> this handler (HIGH, DEF) -> vanilla armor.
 * Damage that bypasses invulnerability (void, /kill) ignores shield, defence and the Berserk drawback.
 */
@EventBusSubscriber
public class DefenceHandler {

	// ---- Settings (edit here) ----
	private static final double MAX_DEFENCE = 0.8D;          // defence is capped at 80% damage reduction
	private static final float BERSERK_DAMAGE_TAKEN = 1.3F;  // damage multiplier while in Berserk Mode (1.0 = no drawback)
	private static final boolean SHIELD_EFFECTS = true;      // sparks + sound when the shield blocks a hit
	private static final int SHIELD_FX_COOLDOWN = 4;         // ticks between block effects (avoids spam)

	// High priority: the shield cancels the event before other listeners see it,
	// and defence is applied before damage-sharing effects read the final amount.
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		// void, /kill: nothing reduces (or increases) this damage
		if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);

		// ---- Holy Shields: no damage at all (no hit animation, no knockback) ----
		if (v.paladin_shields) {
			event.setCanceled(true);
			if (SHIELD_EFFECTS)
				shieldFx(player);
			return;
		}

		// ---- Defence, then the Berserk Mode drawback (applied on top, not instead) ----
		float amount = event.getAmount();
		double defence = Math.max(0.0D, Math.min(MAX_DEFENCE, v.DEFENCE));
		amount *= (float) (1.0D - defence);
		if (v.berserk_mode)
			amount *= BERSERK_DAMAGE_TAKEN;
		event.setAmount(amount);
	}

	private static void shieldFx(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		long now = level.getGameTime();
		CompoundTag data = player.getPersistentData();
		if (now - data.getLong("eldervale_shield_fx") < SHIELD_FX_COOLDOWN)
			return;
		data.putLong("eldervale_shield_fx", now);

		double x = player.getX(), y = player.getY(), z = player.getZ();
		level.sendParticles(ParticleTypes.END_ROD, x, y + 1.0D, z, 14, 0.45D, 0.6D, 0.45D, 0.06D);
		level.sendParticles(ParticleTypes.CRIT, x, y + 1.0D, z, 10, 0.4D, 0.5D, 0.4D, 0.3D);
		level.playSound(null, x, y, z, SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.5F, 1.6F);
	}
}
