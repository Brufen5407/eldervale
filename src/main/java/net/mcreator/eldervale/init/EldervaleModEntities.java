/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.eldervale.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.Registries;

import net.mcreator.eldervale.entity.ManichinoRangerEntity;
import net.mcreator.eldervale.entity.MagicArrowEntity;
import net.mcreator.eldervale.EldervaleMod;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class EldervaleModEntities {
	public static final DeferredRegister<EntityType<?>> REGISTRY = DeferredRegister.create(Registries.ENTITY_TYPE, EldervaleMod.MODID);
	public static final DeferredHolder<EntityType<?>, EntityType<ManichinoRangerEntity>> MANICHINO_RANGER = register("manichino_ranger",
			EntityType.Builder.<ManichinoRangerEntity>of(ManichinoRangerEntity::new, MobCategory.MONSTER).setShouldReceiveVelocityUpdates(true).setTrackingRange(1).setUpdateInterval(3).fireImmune().sized(0.6f, 1.8f));
	public static final DeferredHolder<EntityType<?>, EntityType<MagicArrowEntity>> MAGIC_ARROW = register("magic_arrow",
			EntityType.Builder.<MagicArrowEntity>of(MagicArrowEntity::new, MobCategory.MISC).setShouldReceiveVelocityUpdates(true).setTrackingRange(64).setUpdateInterval(1).sized(0.5f, 0.5f));

	// Start of user code block custom entities
	// End of user code block custom entities
	private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(String registryname, EntityType.Builder<T> entityTypeBuilder) {
		return REGISTRY.register(registryname, () -> (EntityType<T>) entityTypeBuilder.build(registryname));
	}

	@SubscribeEvent
	public static void init(RegisterSpawnPlacementsEvent event) {
		ManichinoRangerEntity.init(event);
	}

	@SubscribeEvent
	public static void registerAttributes(EntityAttributeCreationEvent event) {
		event.put(MANICHINO_RANGER.get(), ManichinoRangerEntity.createAttributes().build());
	}
}