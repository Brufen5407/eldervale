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

import java.util.Comparator;

public class Berserkeskill2Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		boolean check = false;
		Entity aoe_mob = null;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("berserker") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL2_CD < 1 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 20) {
			entity.setDeltaMovement(new Vec3((1.7 * entity.getLookAngle().x), (1 * entity.getLookAngle().y), (1.7 * entity.getLookAngle().z)));
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.POOF, (entity.getX()), (entity.getY()), (entity.getZ()), 10, 0, 0.3, 0, 0.02);
			{
				double _x = (entity.getX());
				double _y = (entity.getY());
				double _z = (entity.getZ());
				double _radius = 5;
				double _power = 1.5;
				net.minecraft.world.phys.Vec3 _centerPos = new net.minecraft.world.phys.Vec3(_x, _y, _z);
				net.minecraft.world.phys.AABB _area = new net.minecraft.world.phys.AABB(_centerPos, _centerPos).inflate(_radius);
				for (net.minecraft.world.entity.Entity _ent : world.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, _area)) {
					if (_ent.position().distanceToSqr(_centerPos) <= _radius * _radius) {
						net.minecraft.world.phys.Vec3 _vec = _ent.position().subtract(_centerPos);
						if (_vec.lengthSqr() > 1.0E-4D) {
							net.minecraft.world.phys.Vec3 _motion = _vec.normalize().scale(_power);
							_ent.setDeltaMovement(_motion);
							_ent.hurtMarked = true;
							if (_ent instanceof net.minecraft.server.level.ServerPlayer _player) {
								_player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(_ent));
							}
						}
					}
				}
			}
			{
				final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
				for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(7 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
					if (!(entityiterator == entity)) {
						entityiterator.hurt(new DamageSource(world.holderOrThrow(DamageTypes.PLAYER_ATTACK)), 2);
					}
				}
			}
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.SKILL2_CD = 280;
				_vars.SKILL2_MAX_CD = 280;
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 20;
				_vars.markSyncDirty();
			}
		}
	}
}