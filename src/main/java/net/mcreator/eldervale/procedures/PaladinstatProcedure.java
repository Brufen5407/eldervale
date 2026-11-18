package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class PaladinstatProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("paladin")) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.HEALT = 50;
				_vars.HEALT_MAX = 50;
				_vars.DEFENCE = 0.1;
				_vars.ATTACK_SPEED = 4;
				_vars.ATTACK = 1.5;
				_vars.MANA = 80;
				_vars.MANA_MAX = 80;
				_vars.MANA_REGEN = 4;
				_vars.SPEED = 0.09;
				_vars.markSyncDirty();
			}
		}
	}
}