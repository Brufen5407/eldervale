/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.eldervale.init;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.Item;

import net.mcreator.eldervale.item.inventory.ClassselectorInventoryCapability;
import net.mcreator.eldervale.item.ClassselectorItem;
import net.mcreator.eldervale.EldervaleMod;

@EventBusSubscriber
public class EldervaleModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(EldervaleMod.MODID);
	public static final DeferredItem<Item> CLASSSELECTOR;
	static {
		CLASSSELECTOR = REGISTRY.register("classselector", ClassselectorItem::new);
	}

	// Start of user code block custom items
	// End of user code block custom items
	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerItem(Capabilities.ItemHandler.ITEM, (stack, context) -> new ClassselectorInventoryCapability(stack), CLASSSELECTOR.get());
	}
}