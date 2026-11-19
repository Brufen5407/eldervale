package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class WerewolfStatProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("werewolf")) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.HEALT = 40;
				_vars.HEALT_MAX = 40;
				_vars.DEFENCE = 0.06;
				_vars.ATTACK_SPEED = 4.5;
				_vars.ATTACK = 1.7;
				_vars.MANA = 60;
				_vars.MANA_MAX = 60;
				_vars.MANA_REGEN = 3;
				_vars.SPEED = 0.12;
				_vars.markSyncDirty();
			}
		}
	}
}