package net.mcreator.eldervale.gameplay;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;

import net.mcreator.eldervale.network.EldervaleModVariables;

import java.util.List;

/**
 * Server-safe Berserker logic. Replaces BerserkerPassiveProcedure and BerserkerModeDMGProcedure.
 * Runs only on the server (it never touches client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class BerserkerGameplay {

	// ---- Settings (edit here) ----
	private static final double HEALTH_THRESHOLD = 0.5D;   // Berserk Mode below 50% health
	private static final double SPEED_BONUS = 0.5D;        // +50% movement speed
	private static final double ATTACK_BONUS = 0.5D;       // +50% attack damage
	private static final float SPLASH_RATIO = 0.5F;        // nearby mob takes 50% of the damage dealt
	private static final double SPLASH_RANGE = 2.0D;       // how far from the target the splash reaches
	private static final boolean SPLASH_EFFECTS = true;    // spark line + sound on the splash

	private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath("eldervale", "berserk_mode_speed");
	private static final ResourceLocation ATTACK_ID = ResourceLocation.fromNamespaceAndPath("eldervale", "berserk_mode_attack");

	// Prevents the splash hit from triggering another splash
	private static boolean splashing = false;

	// ---------------------------------------------------------------
	//  Berserk Mode (passive): on below 50% health, off above
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		Player player = event.getEntity();
		if (player.level().isClientSide())
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		boolean berserker = "berserker".equals(v.PLAYER_CLASS);
		boolean shouldBeOn = berserker && player.isAlive() && player.getHealth() < player.getMaxHealth() * HEALTH_THRESHOLD;

		// only sync when the value actually changes (the old procedure sent a packet every tick)
		if (v.berserk_mode != shouldBeOn) {
			v.berserk_mode = shouldBeOn;
			v.markSyncDirty();
		}

		setModifier(player, Attributes.MOVEMENT_SPEED, SPEED_ID, shouldBeOn ? SPEED_BONUS : 0.0D);
		setModifier(player, Attributes.ATTACK_DAMAGE, ATTACK_ID, shouldBeOn ? ATTACK_BONUS : 0.0D);
	}

	private static void setModifier(Player player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
		AttributeInstance inst = player.getAttribute(attribute);
		if (inst == null)
			return;
		AttributeModifier existing = inst.getModifier(id);
		if (amount == 0.0D) {
			if (existing != null)
				inst.removeModifier(id);
			return;
		}
		if (existing != null && existing.amount() == amount)
			return;
		if (existing != null)
			inst.removeModifier(id);
		inst.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
	}

	// ---------------------------------------------------------------
	//  Berserk Mode: the nearest monster takes 50% of the damage dealt
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (splashing)
			return;
		LivingEntity target = event.getEntity();
		if (target == null || target.level().isClientSide())
			return;

		Entity source = event.getSource().getEntity();
		if (!(source instanceof ServerPlayer attacker))
			return;

		EldervaleModVariables.PlayerVariables v = attacker.getData(EldervaleModVariables.PLAYER_VARIABLES);
		if (!v.berserk_mode || !"berserker".equals(v.PLAYER_CLASS))
			return;
		if (!(target.level() instanceof ServerLevel level))
			return;

		// nearest other monster around the target (no crash if there is none)
		AABB box = target.getBoundingBox().inflate(SPLASH_RANGE);
		List<Monster> near = level.getEntitiesOfClass(Monster.class, box, e -> e != target && e.isAlive());
		Monster best = null;
		double bestDist = Double.MAX_VALUE;
		for (Monster m : near) {
			double d = m.distanceToSqr(target);
			if (d < bestDist) {
				bestDist = d;
				best = m;
			}
		}
		if (best == null)
			return;

		splashing = true;
		try {
			best.hurt(level.damageSources().playerAttack(attacker), event.getAmount() * SPLASH_RATIO);
		} finally {
			splashing = false;
		}

		if (SPLASH_EFFECTS) {
			double x1 = target.getX(), y1 = target.getY() + target.getBbHeight() * 0.6D, z1 = target.getZ();
			double x2 = best.getX(), y2 = best.getY() + best.getBbHeight() * 0.6D, z2 = best.getZ();
			for (int i = 0; i <= 8; i++) {
				double f = i / 8.0D;
				level.sendParticles(ParticleTypes.CRIT, x1 + (x2 - x1) * f, y1 + (y2 - y1) * f, z1 + (z2 - z1) * f, 1, 0.02D, 0.02D, 0.02D, 0.01D);
			}
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, x2, y2, z2, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			level.playSound(null, x2, y2, z2, SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 0.8F, 0.8F);
		}
	}
}
