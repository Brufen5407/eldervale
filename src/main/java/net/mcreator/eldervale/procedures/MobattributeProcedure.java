package net.mcreator.eldervale.procedures;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

import javax.annotation.Nullable;

@EventBusSubscriber
public class MobattributeProcedure {
	@SubscribeEvent
	public static void onEntitySpawned(EntityJoinLevelEvent event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		double randomHP = 0;
		double randomATT = 0;
		if (entity instanceof Monster) {
			randomHP = Mth.nextInt(RandomSource.create(), 60, 600);
			randomATT = Mth.nextInt(RandomSource.create(), 3, 30);
			if (entity instanceof LivingEntity _livingEntity3 && _livingEntity3.getAttributes().hasAttribute(Attributes.MAX_HEALTH))
				_livingEntity3.getAttribute(Attributes.MAX_HEALTH).setBaseValue(randomHP);
			if (entity instanceof LivingEntity _livingEntity4 && _livingEntity4.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE))
				_livingEntity4.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(randomATT);
			entity.getPersistentData().putDouble("mob level", ((entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1) - 59
					+ (entity instanceof LivingEntity _livingEntity6 && _livingEntity6.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? _livingEntity6.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() : 0) - 3));
		}
	}
}