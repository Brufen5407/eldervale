package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class SetCLASSProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
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
			AssassinstatProcedure.execute(entity);
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
		if (entity instanceof LivingEntity _livingEntity1 && _livingEntity1.getAttributes().hasAttribute(Attributes.MAX_HEALTH))
			_livingEntity1.getAttribute(Attributes.MAX_HEALTH).setBaseValue(entity.getData(EldervaleModVariables.PLAYER_VARIABLES).HEALT_MAX);
		if (entity instanceof LivingEntity _livingEntity2 && _livingEntity2.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE))
			_livingEntity2.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(entity.getData(EldervaleModVariables.PLAYER_VARIABLES).ATTACK);
		if (entity instanceof LivingEntity _livingEntity3 && _livingEntity3.getAttributes().hasAttribute(Attributes.MOVEMENT_SPEED))
			_livingEntity3.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SPEED);
		if (entity instanceof LivingEntity _livingEntity4 && _livingEntity4.getAttributes().hasAttribute(Attributes.ATTACK_SPEED))
			_livingEntity4.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(entity.getData(EldervaleModVariables.PLAYER_VARIABLES).ATTACK_SPEED);
	}
}