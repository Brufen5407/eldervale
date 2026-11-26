package net.mcreator.eldervale.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.vfx.VfxIds;
import net.mcreator.eldervale.vfx.VfxServer;
import net.mcreator.eldervale.EldervaleMod;

import java.util.Comparator;

/** Berserker skill 1: BERSERK SLAM. Gameplay unchanged; the old particles are replaced by the VFX events. */
public class Berserkeskill1Procedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("berserker") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL1_CD < 1 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 30) {
			entity.setDeltaMovement(new Vec3(0, 0.6, 0));
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.SKILL1_MAX_CD = 240;
				_vars.SKILL1_CD = 240;
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 30;
				_vars.markSyncDirty();
			}
			if (entity instanceof ServerPlayer sp)
				VfxServer.send(sp, VfxIds.BERSERKER_SLAM, VfxIds.START, 0, 3.5F);
			EldervaleMod.queueServerWork(20, () -> {
				{
					final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
					for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(7 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
						if (!(entityiterator == entity)) {
							entityiterator.hurt(new DamageSource(world.holderOrThrow(DamageTypes.PLAYER_ATTACK)), 5);
						}
					}
				}
				if (entity instanceof ServerPlayer sp)
					VfxServer.sendAt(sp, VfxIds.BERSERKER_SLAM, VfxIds.IMPACT, 0, entity.getX(), entity.getY(), entity.getZ(), 3.5F);
			});
		}
	}
}
