package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class AssassinstatProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("assassin")) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.HEALT = 25;
				_vars.HEALT_MAX = 25;
				_vars.DEFENCE = 0.02;
				_vars.ATTACK_SPEED = 5;
				_vars.ATTACK = 1.5;
				_vars.MANA = 70;
				_vars.MANA_MAX = 70;
				_vars.MANA_REGEN = 4;
				_vars.SPEED = 0.14;
				_vars.markSyncDirty();
			}
		}
	}
}