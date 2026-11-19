package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class AssassinSkill1Procedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		Entity nearest_entity = null;
		double tp_x = 0;
		double tp_y = 0;
		double tp_z = 0;
		double dot = 0;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("assassin") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL1_CD <= 0 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 25) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 25;
				_vars.SKILL1_CD = 160;
				_vars.SKILL1_MAX_CD = 160;
				_vars.markSyncDirty();
			}
		}
	}
}