package net.mcreator.eldervale.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.EldervaleMod;

public class Paladinskill1Procedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).paladin_shields == false && (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("paladin")
				&& entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL1_CD < 1 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 50) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.paladin_shields = true;
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 50;
				_vars.SKILL1_CD = 600;
				_vars.SKILL1_MAX_CD = 600;
				_vars.markSyncDirty();
			}
			EldervaleMod.queueServerWork(100, () -> {
				{
					EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
					_vars.paladin_shields = false;
					_vars.markSyncDirty();
				}
			});
		}
	}
}