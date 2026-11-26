package net.mcreator.eldervale.procedures;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.init.EldervaleModEntities;
import net.mcreator.eldervale.vfx.VfxIds;
import net.mcreator.eldervale.vfx.VfxServer;

/** Ranger skill 1: DECOY. Gameplay unchanged; the loud vanilla sound is replaced by the VFX event. */
public class RangerSkill1Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("ranger") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL1_CD < 1 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 35) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 1, true, false));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1, true, false));
			if (world instanceof ServerLevel _level) {
				Entity entityToSpawn = EldervaleModEntities.MANICHINO_RANGER.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
				if (entityToSpawn != null) {
					entityToSpawn.setYRot(world.getRandom().nextFloat() * 360F);
				}
			}
			if (entity instanceof ServerPlayer sp)
				VfxServer.sendAt(sp, VfxIds.RANGER_DECOY, VfxIds.START, 0, x, y, z, 2.5F);
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 35;
				_vars.SKILL1_CD = 500;
				_vars.SKILL1_MAX_CD = 500;
				_vars.markSyncDirty();
			}
		}
	}
}
