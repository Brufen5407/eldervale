package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class BerserkerStatProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("berserker")) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.HEALT = 35;
				_vars.HEALT_MAX = 35;
				_vars.DEFENCE = 0.04;
				_vars.ATTACK_SPEED = 3.2;
				_vars.ATTACK = 2.5;
				_vars.MANA = 50;
				_vars.MANA_MAX = 50;
				_vars.MANA_REGEN = 2;
				_vars.SPEED = 0.1;
				_vars.markSyncDirty();
			}
		}
	}
}