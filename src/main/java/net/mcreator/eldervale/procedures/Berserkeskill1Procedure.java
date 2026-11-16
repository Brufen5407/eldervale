package net.mcreator.eldervale.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.EldervaleMod;

import java.util.Comparator;

public class Berserkeskill1Procedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		boolean check = false;
		Entity aoe_mob = null;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("berserker") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL1_CD < 1 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 30) {
			entity.setDeltaMovement(new Vec3(0, 0.6, 0));
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.SKILL1_MAX_CD = 240;
				_vars.SKILL1_CD = 240;
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 30;
				_vars.markSyncDirty();
			}
			EldervaleMod.queueServerWork(20, () -> {
				{
					final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
					for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(7 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
						if (!(entityiterator == entity)) {
							entityiterator.hurt(new DamageSource(world.holderOrThrow(DamageTypes.PLAYER_ATTACK)), 5);
						}
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.ANGRY_VILLAGER, (entityiterator.getX()), (entityiterator.getY()), (entityiterator.getZ()), 25, 0.2, 0.5, 0.2, 0.02);
					}
				}
			});
		}
	}
}