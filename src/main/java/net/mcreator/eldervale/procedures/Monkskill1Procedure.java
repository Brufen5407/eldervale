package net.mcreator.eldervale.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.EldervaleMod;

import java.util.Comparator;

public class Monkskill1Procedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getData(EldervaleModVariables.PLAYER_VARIABLES).PLAYER_CLASS).equals("monk") && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).SKILL1_CD < 1 && entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA >= 35) {
			if (entity instanceof net.minecraft.world.entity.LivingEntity _livingFreeze) {
				net.minecraft.world.entity.ai.attributes.AttributeInstance _attr = _livingFreeze.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
				if (_attr != null) {
					net.minecraft.resources.ResourceLocation _modId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eldervale", "freeze_movement");
					_attr.removeModifier(_modId);
					_attr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(_modId, -1.0d, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
				}
			}
			{
				EldervaleModVariables.PlayerVariables _vars = entity.getData(EldervaleModVariables.PLAYER_VARIABLES);
				_vars.MANA = entity.getData(EldervaleModVariables.PLAYER_VARIABLES).MANA - 35;
				_vars.SKILL1_CD = 400;
				_vars.SKILL1_MAX_CD = 400;
				_vars.markSyncDirty();
			}
			for (int _i1 = 0; _i1 < 5; _i1++) {
				EldervaleMod.queueServerWork(20, () -> {
					{
						final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
						for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(5 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
							if (!(entityiterator == entity) && entityiterator instanceof Monster) {
								if (entityiterator instanceof net.minecraft.world.entity.Entity _ent) {
									net.minecraft.world.phys.Vec3 _backDir = _ent.getLookAngle().reverse();
									double _speed = 1;
									double _dist = 4;
									double _power = true ? (_speed * (_dist * 0.5d)) : _speed;
									net.minecraft.world.phys.Vec3 _motion = _backDir.scale(_power);
									_ent.setDeltaMovement(_motion);
									_ent.hurtMarked = true;
									if (_ent instanceof net.minecraft.server.level.ServerPlayer _player) {
										_player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(_ent));
									}
								}
							}
						}
					}
				});
			}
			EldervaleMod.queueServerWork(20, () -> {
				if (entity instanceof LivingEntity _entity)
					_entity.setHealth((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) + 2);
				EldervaleMod.queueServerWork(20, () -> {
					if (entity instanceof LivingEntity _entity)
						_entity.setHealth((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) + 2);
					EldervaleMod.queueServerWork(20, () -> {
						if (entity instanceof LivingEntity _entity)
							_entity.setHealth((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) + 2);
						EldervaleMod.queueServerWork(20, () -> {
							if (entity instanceof LivingEntity _entity)
								_entity.setHealth((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) + 2);
							EldervaleMod.queueServerWork(20, () -> {
								if (entity instanceof LivingEntity _entity)
									_entity.setHealth((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) + 2);
								if (entity instanceof net.minecraft.world.entity.LivingEntity _livingFreeze) {
									net.minecraft.world.entity.ai.attributes.AttributeInstance _attr = _livingFreeze.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
									if (_attr != null) {
										net.minecraft.resources.ResourceLocation _modId = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("eldervale", "freeze_movement");
										_attr.removeModifier(_modId);
									}
								}
							});
						});
					});
				});
			});
		}
	}
}