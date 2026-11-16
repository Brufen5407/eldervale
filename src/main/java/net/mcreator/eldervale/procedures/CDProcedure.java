package net.mcreator.eldervale.procedures;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

import javax.annotation.Nullable;

@EventBusSubscriber
public class CDProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA <= entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA_MAX - 1) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA + entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA_REGEN / 20;
				_vars.markSyncDirty();
			}
		}
		if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL1_CD > 0) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.SKILL1_CD = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL1_CD - 1;
				_vars.markSyncDirty();
			}
		}
		if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL2_CD > 0) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.SKILL2_CD = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL2_CD - 1;
				_vars.markSyncDirty();
			}
		}
	}
}