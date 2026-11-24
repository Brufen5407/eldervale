package net.mcreator.eldervale.gameplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Werewolf skill 1: BEAST TRANSFORMATION.
 *  - 8 s of beast form: +30% attack speed, +20% movement speed, 1 HP regenerated every second.
 *  - Sets PlayerVariables.werewolf_mode (synced to all players) so the client can draw the beast model.
 *  - Ends on: timeout, death, logout, class change, server stop.
 *
 * Mana (30) and cooldown (500 ticks = 25 s) stay in the MCreator procedure WerewolfSkill1.
 * This class detects the cast on the server (the SKILL1 cooldown jumps up), the same pattern used by
 * AssassinShadowStrike. Server only (no client classes): safe on a dedicated server.
 * The bonuses are transient modifiers with their own IDs: StatCalculator (base values) never touches them,
 * and the future Predator's Instinct passive can use different IDs and stack additively.
 */
@EventBusSubscriber
public class WerewolfBeastTransformation {

	// ---- Settings (edit here) ----
	private static final int DURATION_TICKS = 160;          // 8 s
	private static final double ATTACK_SPEED_BONUS = 0.30D; // +30%
	private static final double MOVE_SPEED_BONUS = 0.20D;   // +20%
	private static final float REGEN_AMOUNT = 1.0F;         // HP per interval
	private static final int REGEN_INTERVAL = 20;           // ticks (1 s)

	private static final ResourceLocation ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath("eldervale", "werewolf_beast_attack_speed");
	private static final ResourceLocation MOVE_SPEED_ID = ResourceLocation.fromNamespaceAndPath("eldervale", "werewolf_beast_speed");

	private static final Map<UUID, Double> PREV_CD = new HashMap<>();
	private static final Map<UUID, Integer> REMAINING = new HashMap<>();

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		UUID id = player.getUUID();
		boolean werewolf = "werewolf".equals(v.PLAYER_CLASS);

		// cast: the cooldown jumped up to its maximum
		double cd = v.SKILL1_CD;
		Double prev = PREV_CD.put(id, cd);
		if (prev != null && werewolf && player.isAlive() && !REMAINING.containsKey(id) && cd - prev > 50.0D && cd >= v.SKILL1_MAX_CD - 10.0D)
			start(player, v);

		// active form
		Integer left = REMAINING.get(id);
		if (left == null)
			return;
		if (!werewolf || !player.isAlive()) {
			stop(player, v, false);
			return;
		}
		left--;
		REMAINING.put(id, left);
		if ((DURATION_TICKS - left) % REGEN_INTERVAL == 0)
			player.heal(REGEN_AMOUNT);
		if (left <= 0)
			stop(player, v, true);
	}

	private static void start(ServerPlayer player, EldervaleModVariables.PlayerVariables v) {
		REMAINING.put(player.getUUID(), DURATION_TICKS);
		setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, ATTACK_SPEED_BONUS);
		setModifier(player, Attributes.MOVEMENT_SPEED, MOVE_SPEED_ID, MOVE_SPEED_BONUS);
		v.werewolf_mode = true;
		v.markSyncDirty();

		ServerLevel level = player.serverLevel();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 1.5F, 0.6F);
		level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.4D, 0.6D, 0.4D, 0.05D);
	}

	private static void stop(ServerPlayer player, EldervaleModVariables.PlayerVariables v, boolean expired) {
		REMAINING.remove(player.getUUID());
		setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, 0.0D);
		setModifier(player, Attributes.MOVEMENT_SPEED, MOVE_SPEED_ID, 0.0D);
		if (v.werewolf_mode) {
			v.werewolf_mode = false;
			v.markSyncDirty();
		}
		if (expired) {
			ServerLevel level = player.serverLevel();
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOLF_WHINE, SoundSource.PLAYERS, 1.0F, 0.8F);
			level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.4D, 0.6D, 0.4D, 0.02D);
		}
	}

	private static void setModifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id, double amount) {
		AttributeInstance inst = player.getAttribute(attribute);
		if (inst == null)
			return;
		if (inst.getModifier(id) != null)
			inst.removeModifier(id);
		if (amount != 0.0D)
			inst.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
	}

	// ---------------------------------------------------------------
	//  Cleanup
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && REMAINING.containsKey(player.getUUID()))
			stop(player, player.getData(EldervaleModVariables.PLAYER_VARIABLES), false);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			stop(player, player.getData(EldervaleModVariables.PLAYER_VARIABLES), false);
			PREV_CD.remove(player.getUUID());
		}
	}

	/** werewolf_mode is saved with the player: clear a stale flag (crash / server stop while transformed). */
	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
			if (v.werewolf_mode) {
				v.werewolf_mode = false;
				v.markSyncDirty();
			}
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PREV_CD.clear();
		REMAINING.clear();
	}
}
