package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class WerewolfSkill2Procedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		boolean check = false;
		Entity aoe_mob = null;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("werewolf") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL2_CD < 1 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 20) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.SKILL2_CD = 200;
				_vars.SKILL2_MAX_CD = 200;
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 20;
				_vars.markSyncDirty();
			}
		}
	}
}