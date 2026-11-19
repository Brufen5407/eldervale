package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class AssassinSkill2Procedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("assassin") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL2_CD <= 0 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 30) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 30;
				_vars.SKILL2_CD = 360;
				_vars.SKILL2_MAX_CD = 360;
				_vars.markSyncDirty();
			}
		}
	}
}