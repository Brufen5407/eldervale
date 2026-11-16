package net.mcreator.eldervale.procedures;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class MonkStatProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("monk")) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.HEALT = 35;
				_vars.HEALT_MAX = 35;
				_vars.markSyncDirty();
			}
			if (entity instanceof LivingEntity _livingEntity0 && _livingEntity0.getAttributes().hasAttribute(Attributes.MAX_HEALTH))
				_livingEntity0.getAttribute(Attributes.MAX_HEALTH).setBaseValue(35);
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.ATTACK = 1.8;
				_vars.DEFENCE = 0.05;
				_vars.SPEED = 0.105;
				_vars.MANA_REGEN = 5;
				_vars.MANA = 100;
				_vars.MANA_MAX = 100;
				_vars.markSyncDirty();
			}
			if (entity instanceof LivingEntity _livingEntity1 && _livingEntity1.getAttributes().hasAttribute(Attributes.ATTACK_SPEED))
				_livingEntity1.getAttribute(Attributes.ATTACK_SPEED).setBaseValue(5);
		}
	}
}