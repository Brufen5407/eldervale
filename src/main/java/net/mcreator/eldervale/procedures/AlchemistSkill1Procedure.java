package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class AlchemistSkill1Procedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		boolean check = false;
		Entity aoe_mob = null;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("alchemist") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL1_CD < 1 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 35) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.SKILL1_CD = 240;
				_vars.SKILL1_MAX_CD = 240;
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 35;
				_vars.markSyncDirty();
			}
		}
	}
}