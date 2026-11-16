package net.mcreator.eldervale.procedures;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;

import net.mcreator.eldervale.network.EldervaleModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class XpLevelProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event.getEntity() != null) {
			execute(event, event.getSource(), event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(DamageSource damagesource, Entity entity, Entity sourceentity) {
		execute(null, damagesource, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, DamageSource damagesource, Entity entity, Entity sourceentity) {
		if (damagesource == null || entity == null || sourceentity == null)
			return;
		if (damagesource.is(DamageTypes.PLAYER_ATTACK)) {
			if (entity instanceof Monster) {
				{
					EldervaleModVariables.PlayerVariables _vars = sourceentity.getData(EldervaleModVariables.PLAYER_VARIABLES);
					_vars.XP = sourceentity.getData(EldervaleModVariables.PLAYER_VARIABLES).XP
							+ Math.floor((entity.getPersistentData().getDouble("mob level") * 0.2 + Math.floor(entity.getPersistentData().getDouble("mob level") / 12)) * ((entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1) / 24d));
					_vars.markSyncDirty();
				}
			}
		}
		if (sourceentity.getData(EldervaleModVariables.PLAYER_VARIABLES).XP >= sourceentity.getData(EldervaleModVariables.PLAYER_VARIABLES).XP_NEXT_LEVEL) {
			{
				EldervaleModVariables.PlayerVariables _vars = sourceentity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.STAT_POINT = sourceentity.getData(EldervaleModVariables.PLAYER_VARIABLES).STAT_POINT + 1;
				_vars.LEVEL = sourceentity.getData(EldervaleModVariables.PLAYER_VARIABLES).LEVEL + 1;
				_vars.XP = (sourceentity.getData(EldervaleModVariables.PLAYER_VARIABLES).XP - sourceentity.getData(EldervaleModVariables.PLAYER_VARIABLES).XP_NEXT_LEVEL) / 100;
				_vars.XP_NEXT_LEVEL = Math.ceil(sourceentity.getData(EldervaleModVariables.PLAYER_VARIABLES).XP_NEXT_LEVEL * 1.5);
				_vars.markSyncDirty();
			}
		}
	}
}