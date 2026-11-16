package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class SetCLASSProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		String classe = "";
		if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).page == 0) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.PLAYER_CLASS = "paladin";
				_vars.markSyncDirty();
			}
			PaladinstatProcedure.execute(entity);
		} else if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).page == 1) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.PLAYER_CLASS = "berserker";
				_vars.markSyncDirty();
			}
			BerserkerStatProcedure.execute(entity);
		} else if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).page == 2) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.PLAYER_CLASS = "ranger";
				_vars.markSyncDirty();
			}
			RangerstatProcedure.execute(entity);
		} else if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).page == 3) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.PLAYER_CLASS = "mage";
				_vars.markSyncDirty();
			}
		} else if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).page == 4) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.PLAYER_CLASS = "assassin";
				_vars.markSyncDirty();
			}
		} else if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).page == 5) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.PLAYER_CLASS = "monk";
				_vars.markSyncDirty();
			}
			MonkStatProcedure.execute(entity);
		} else if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).page == 6) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.PLAYER_CLASS = "werewolf";
				_vars.markSyncDirty();
			}
		} else if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).page == 7) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.PLAYER_CLASS = "alchemist";
				_vars.markSyncDirty();
			}
		} else if (entity.getData(EldervaleModVariables.PLAYER_VARIABLES).page == 8) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.PLAYER_CLASS = "spellblade";
				_vars.markSyncDirty();
			}
		}
		if (entity instanceof Player _player)
			_player.closeContainer();
	}
}