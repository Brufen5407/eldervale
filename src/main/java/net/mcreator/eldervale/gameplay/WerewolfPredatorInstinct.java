package net.mcreator.eldervale.gameplay;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
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
 * Werewolf passive: PREDATOR'S INSTINCT.
 *  - Below 50% of max HP: +15% movement speed and +15% attack speed.
 *  - Removed as soon as HP is back to 50% or more, on death, or if the class is no longer werewolf.
 *
 * The state is the presence of the modifier itself (no maps to clean up, correct after respawn/relog).
 * Modifiers are transient with their own IDs, so they stack additively with Beast Transformation
 * (+30% / +20%): both active = +45% attack speed and +35% movement speed.
 * Server only (no client classes): safe on a dedicated server.
 */
@EventBusSubscriber
public class WerewolfPredatorInstinct {

	// ---- Settings (edit here) ----
	private static final float HP_THRESHOLD = 0.5F;         // strictly below 50% of max HP
	private static final double ATTACK_SPEED_BONUS = 0.15D; // +15%
	private static final double MOVE_SPEED_BONUS = 0.15D;   // +15%

	private static final ResourceLocation ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath("eldervale", "werewolf_instinct_attack_speed");
	private static final ResourceLocation MOVE_SPEED_ID = ResourceLocation.fromNamespaceAndPath("eldervale", "werewolf_instinct_speed");

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		AttributeInstance move = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (move == null)
			return;
		boolean active = move.getModifier(MOVE_SPEED_ID) != null;

		boolean wanted = player.isAlive() && "werewolf".equals(player.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS) && player.getHealth() < player.getMaxHealth() * HP_THRESHOLD;
		if (wanted == active)
			return; // nothing to change (the usual case: no work per tick)

		setModifier(player, Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, wanted ? ATTACK_SPEED_BONUS : 0.0D);
		setModifier(player, Attributes.MOVEMENT_SPEED, MOVE_SPEED_ID, wanted ? MOVE_SPEED_BONUS : 0.0D);

		if (wanted) {
			ServerLevel level = player.serverLevel();
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WOLF_GROWL, SoundSource.PLAYERS, 0.8F, 0.9F);
			level.sendParticles(ParticleTypes.ANGRY_VILLAGER, player.getX(), player.getY() + 1.8D, player.getZ(), 5, 0.3D, 0.2D, 0.3D, 0.0D);
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
}
