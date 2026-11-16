package net.mcreator.eldervale.procedures;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import net.mcreator.eldervale.EldervaleMod;

import java.util.Comparator;

public class DummyAggroProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double i = 0;
		double angle = 0;
		double r = 0;
		double particle_X = 0;
		double particle_Z = 0;
		double particle_Y = 0;
		double elapsed_tick = 0;
		{
			final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
			for (Entity entityiterator : world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(12 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList()) {
				if (!(entityiterator == entity || entityiterator instanceof Player)) {
					if (entityiterator instanceof Mob _entity && entity instanceof LivingEntity _ent)
						_entity.setTarget(_ent);
				}
			}
		}
		EldervaleMod.queueServerWork(160, () -> {
			if (!entity.level().isClientSide())
				entity.discard();
		});
	}
}