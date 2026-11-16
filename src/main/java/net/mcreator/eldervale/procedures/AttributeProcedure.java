package net.mcreator.eldervale.procedures;

import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class AttributeProcedure {
	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (entity.getPersistentData().getBoolean("first_time") == true) {
			if (entity instanceof LivingEntity _livingEntity1 && _livingEntity1.getAttributes().hasAttribute(Attributes.MAX_HEALTH))
				_livingEntity1.getAttribute(Attributes.MAX_HEALTH).setBaseValue(entity.getData(EldervaleModVariables.PLAYER_VARIABLES).HEALT_MAX);
			if (entity instanceof LivingEntity _livingEntity2 && _livingEntity2.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE))
				_livingEntity2.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(entity.getData(EldervaleModVariables.PLAYER_VARIABLES).ATTACK);
			if (entity instanceof LivingEntity _livingEntity3 && _livingEntity3.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
				_livingEntity3.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SPEED);
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.DEFENCE = 0;
				_vars.markSyncDirty();
			}
			entity.getPersistentData().putBoolean("first_time", false);
		}
	}
}