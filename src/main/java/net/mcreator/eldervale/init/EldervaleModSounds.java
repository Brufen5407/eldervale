/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.eldervale.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import net.mcreator.eldervale.EldervaleMod;

public class EldervaleModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(Registries.SOUND_EVENT, EldervaleMod.MODID);
	public static final DeferredHolder<SoundEvent, SoundEvent> MAGIC_ARROW = REGISTRY.register("magic_arrow", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("eldervale", "magic_arrow")));
}