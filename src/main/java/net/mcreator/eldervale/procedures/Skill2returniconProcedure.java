package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class Skill2returniconProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		String skill2_image = "";
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("paladin")) {
			skill2_image = "paladin_skill2.png";
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("berserker")) {
			skill2_image = "berserker_skill2.png";
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("ranger")) {
			skill2_image = "ranger_skill2.png";
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("monk")) {
			skill2_image = "monk_skill1.png";
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("assassin")) {
			skill2_image = "assassin_skill2.png";
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("werewolf")) {
			skill2_image = "werewolfskill2.png";
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("alchemist")) {
			skill2_image = "alchemistskill2.png";
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("spellblade")) {
			skill2_image = "spellbladeskill2.png";
		}
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("mage")) {
			skill2_image = "mageskill2.png";
		}
		return skill2_image;
	}
}