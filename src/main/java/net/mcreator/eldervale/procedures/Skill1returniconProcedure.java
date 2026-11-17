package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class Skill1returniconProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		String skill1_image = "";
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("paladin")) {
			skill1_image = "paladin_skill1.png";
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("berserker")) {
			skill1_image = "berserker_skill1.png";
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("ranger")) {
			skill1_image = "ranger_skill1.png";
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("monk")) {
			skill1_image = "monk_skill2.png";
		}
		return skill1_image;
	}
}