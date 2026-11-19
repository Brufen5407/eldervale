package net.mcreator.eldervale.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class Skill2classProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("paladin")) {
			Paladin_Skill2Procedure.execute(world, x, y, z, entity);
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("berserker")) {
			Berserkeskill2Procedure.execute(world, x, y, z, entity);
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("ranger")) {
			RangerSkill2Procedure.execute(world, x, y, z, entity);
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("monk")) {
			Monkskill2Procedure.execute(entity);
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("assassin")) {
			AssassinSkill2Procedure.execute(entity);
		}
	}
}