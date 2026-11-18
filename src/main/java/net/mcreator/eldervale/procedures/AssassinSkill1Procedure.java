package net.mcreator.eldervale.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.EntityAnchorArgument;

import net.mcreator.eldervale.network.EldervaleModVariables;

import java.util.Comparator;

public class AssassinSkill1Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		Entity nearest_entity = null;
		double tp_x = 0;
		double tp_y = 0;
		double tp_z = 0;
		double dot = 0;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("assassin") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL1_CD <= 0 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 25) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(24 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (!(entity == entityiterator) && entityiterator instanceof Monster) {
						nearest_entity = findEntityInWorldRange(world, Monster.class, (entity.getX()), (entity.getY()), (entity.getZ()), 24);
						break;
					}
				}
			}
			if (!(nearest_entity == null)) {
				tp_x = nearest_entity.getX() + Math.sin(entity.getYRot()) * 1.5;
				tp_y = nearest_entity.getY();
				tp_z = nearest_entity.getZ() - Math.cos(entity.getYRot()) * 1.5;
				{
					Entity _ent = entity;
					double _tx = tp_x;
					double _ty = tp_y;
					double _tz = tp_z;
					_ent.teleportTo(_tx, _ty, _tz);
					if (_ent instanceof ServerPlayer _serverPlayer)
						_serverPlayer.connection.teleport(_tx, _ty, _tz, _ent.getYRot(), _ent.getXRot());
				}
				dot = nearest_entity.getLookAngle().x * (entity.getX() - nearest_entity.getX()) + nearest_entity.getLookAngle().z * (entity.getZ() - nearest_entity.getZ());
				entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3((nearest_entity.getX()), (nearest_entity.getY() + 0.5), (entity.getZ())));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 1));
				if (dot > 0) {
					nearest_entity.hurt(new DamageSource(world.holderOrThrow(DamageTypes.PLAYER_ATTACK)), 10);
				} else {
					nearest_entity.hurt(new DamageSource(world.holderOrThrow(DamageTypes.PLAYER_ATTACK)), 15);
				}
				{
					EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
					_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 25;
					_vars.SKILL1_CD = 160;
					_vars.SKILL1_MAX_CD = 160;
					_vars.markSyncDirty();
				}
			}
		}
	}

	private static Entity findEntityInWorldRange(LevelAccessor world, Class<? extends Entity> clazz, double x, double y, double z, double range) {
		return (Entity) world.getEntitiesOfClass(clazz, AABB.ofSize(new Vec3(x, y, z), range, range, range), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(x, y, z))).findFirst().orElse(null);
	}
}