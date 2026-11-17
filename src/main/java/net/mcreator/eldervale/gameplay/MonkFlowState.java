package net.mcreator.eldervale.gameplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Monk passive: FLOW STATE.
 * Every consecutive melee hit raises attack speed: 1st hit +0%, 2nd +5%, 3rd +10%, 4th and beyond +15%.
 * If the monk does not hit for a few seconds, the combo resets.
 *
 * The combo is stored as an attack speed modifier, which Minecraft already syncs to clients:
 * the modifier exists = combo active, its amount = (combo - 1) * 5%. The HUD reads it from there,
 * so no new player variable is needed. Hit effects and sounds are sent by the server to everyone.
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class MonkFlowState {

	// ---- Settings (edit here) ----
	public static final ResourceLocation MODIFIER_ID = ResourceLocation.fromNamespaceAndPath("eldervale", "monk_flow_state");
	public static final double BONUS_PER_HIT = 0.05D;   // +5% attack speed for each hit after the first
	public static final int MAX_COMBO = 4;              // 4th hit and beyond = +15%
	private static final int TIMEOUT_TICKS = 60;        // 3 seconds without a hit resets the combo
	private static final boolean EFFECTS_ENABLED = true;
	private static final boolean SOUNDS_ENABLED = true;

	private static final DustParticleOptions TEAL = new DustParticleOptions(new Vector3f(0.35F, 0.95F, 0.90F), 1.0F);
	private static final DustParticleOptions GOLD = new DustParticleOptions(new Vector3f(1.00F, 0.85F, 0.30F), 1.1F);

	private static class Flow {
		int combo = 0;
		long lastHit = -1000L;
	}

	private static final Map<UUID, Flow> FLOWS = new HashMap<>();

	// ---------------------------------------------------------------
	//  A melee hit lands
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onDamage(LivingDamageEvent.Post event) {
		LivingEntity target = event.getEntity();
		Entity source = event.getSource().getEntity();
		if (!(source instanceof ServerPlayer player) || source == target)
			return;
		if (event.getSource().getDirectEntity() != player) // melee only (no arrows or skills)
			return;

		registerHit(player, target);
	}

	/**
	 * Registers one hit in the Flow State combo (+1 combo point).
	 * Public so monk skills (like the punch barrage) can add a combo point.
	 */
	public static void registerHit(ServerPlayer player, LivingEntity target) {
		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		if (!"monk".equals(v.PLAYER_CLASS))
			return;

		long now = player.level().getGameTime();
		Flow f = FLOWS.computeIfAbsent(player.getUUID(), k -> new Flow());
		if (f.combo > 0 && now - f.lastHit > TIMEOUT_TICKS)
			f.combo = 0; // it had already expired
		if (f.combo > 0 && f.lastHit == now)
			return; // same swing (sweep hits several targets): count it once

		int previous = f.combo;
		f.combo = Math.min(MAX_COMBO, f.combo + 1);
		f.lastHit = now;
		setModifier(player, (f.combo - 1) * BONUS_PER_HIT);

		hitFeedback(player.serverLevel(), player, target, f.combo, previous);
	}

	// ---------------------------------------------------------------
	//  Timeout, aura and cleanup
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		Flow f = FLOWS.get(player.getUUID());
		if (f == null)
			return;

		long now = player.level().getGameTime();
		boolean monk = "monk".equals(player.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS);
		if (!monk || !player.isAlive()) {
			reset(player, f, false);
			return;
		}
		if (now - f.lastHit > TIMEOUT_TICKS) {
			reset(player, f, true);
			return;
		}
		if (EFFECTS_ENABLED && f.combo >= 2 && now % 4 == 0)
			aura(player.serverLevel(), player, f.combo, now);
	}

	private static void reset(ServerPlayer player, Flow f, boolean feedback) {
		setModifier(player, -1.0D);
		FLOWS.remove(player.getUUID());
		if (feedback && f.combo >= 2) {
			ServerLevel level = player.serverLevel();
			double x = player.getX(), y = player.getY(), z = player.getZ();
			if (EFFECTS_ENABLED) {
				for (int i = 0; i < 8; i++) {
					double a = i * (Math.PI * 2.0D / 8.0D);
					level.sendParticles(ParticleTypes.CLOUD, x + Math.cos(a) * 0.5D, y + 0.9D, z + Math.sin(a) * 0.5D, 0, Math.cos(a), 0.1D, Math.sin(a), 0.06D);
				}
			}
			if (SOUNDS_ENABLED)
				level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.4F, 0.6F);
		}
	}

	// A negative amount removes the modifier. Zero or more keeps it (its presence means "combo active").
	private static void setModifier(ServerPlayer player, double amount) {
		AttributeInstance inst = player.getAttribute(Attributes.ATTACK_SPEED);
		if (inst == null)
			return;
		AttributeModifier existing = inst.getModifier(MODIFIER_ID);
		if (amount < 0.0D) {
			if (existing != null)
				inst.removeModifier(MODIFIER_ID);
			return;
		}
		if (existing != null && existing.amount() == amount)
			return;
		if (existing != null)
			inst.removeModifier(MODIFIER_ID);
		inst.addTransientModifier(new AttributeModifier(MODIFIER_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		FLOWS.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		FLOWS.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		FLOWS.clear();
	}

	// ---------------------------------------------------------------
	//  Effects and sounds (sent by the server, everyone sees and hears them)
	// ---------------------------------------------------------------
	private static void hitFeedback(ServerLevel level, ServerPlayer player, LivingEntity target, int combo, int previous) {
		double tx = target.getX(), ty = target.getY(0.5D), tz = target.getZ();

		if (EFFECTS_ENABLED) {
			DustParticleOptions dust = combo >= MAX_COMBO ? GOLD : TEAL;
			level.sendParticles(dust, tx, ty, tz, 3 + combo * 3, 0.3D, 0.4D, 0.3D, 0.02D);
			level.sendParticles(ParticleTypes.CRIT, tx, ty, tz, 3 + combo * 2, 0.3D, 0.4D, 0.3D, 0.25D);
			if (combo >= 2)
				level.sendParticles(ParticleTypes.END_ROD, tx, ty, tz, combo, 0.25D, 0.35D, 0.25D, 0.08D);

			// reaching the maximum: ring of light around the target
			if (combo >= MAX_COMBO && previous < MAX_COMBO) {
				for (int i = 0; i < 14; i++) {
					double a = i * (Math.PI * 2.0D / 14.0D);
					level.sendParticles(ParticleTypes.END_ROD, tx + Math.cos(a) * 0.6D, target.getY() + 0.2D, tz + Math.sin(a) * 0.6D, 0, Math.cos(a), 0.15D, Math.sin(a), 0.12D);
				}
			}
		}

		if (SOUNDS_ENABLED) {
			float pitch = 1.0F + (combo - 1) * 0.25F; // each hit sounds higher: 1.0, 1.25, 1.5, 1.75
			level.playSound(null, tx, ty, tz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, pitch);
			if (combo >= MAX_COMBO && previous < MAX_COMBO)
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.5F, 1.6F);
		}
	}

	private static void aura(ServerLevel level, ServerPlayer player, int combo, long now) {
		double x = player.getX(), y = player.getY(), z = player.getZ();
		DustParticleOptions dust = combo >= MAX_COMBO ? GOLD : TEAL;
		int orbs = combo - 1;
		double rot = now * 0.4D;
		for (int i = 0; i < orbs; i++) {
			double a = rot + i * (Math.PI * 2.0D / orbs);
			level.sendParticles(dust, x + Math.cos(a) * 0.7D, y + 0.9D + Math.sin(now * 0.2D + i) * 0.3D, z + Math.sin(a) * 0.7D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
		if (combo >= 3 && now % 8 == 0)
			level.sendParticles(ParticleTypes.END_ROD, x, y + 0.1D, z, 2, 0.3D, 0.0D, 0.3D, 0.02D);
	}
}
