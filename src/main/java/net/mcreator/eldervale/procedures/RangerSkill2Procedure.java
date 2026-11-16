package net.mcreator.eldervale.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;

import net.mcreator.eldervale.network.EldervaleModVariables;

public class RangerSkill2Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("ranger") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL2_CD < 1 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 40) {
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 40;
				_vars.SKILL2_CD = 240;
				_vars.SKILL2_MAX_CD = 240;
				_vars.markSyncDirty();
			}
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("eldervale:magic_arrow")), SoundSource.NEUTRAL, 1, 1);
				} else {
					_level.playLocalSound(x, y, z, BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("eldervale:magic_arrow")), SoundSource.NEUTRAL, 1, 1, false);
				}
			}
		}
	}
}