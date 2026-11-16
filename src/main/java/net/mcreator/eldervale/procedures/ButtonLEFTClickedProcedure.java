package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class ButtonLEFTClickedProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).page > 0) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.page = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).page - 1;
				_vars.markSyncDirty();
			}
		}
	}
}