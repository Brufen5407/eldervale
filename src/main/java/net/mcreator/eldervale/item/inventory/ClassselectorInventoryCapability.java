package net.mcreator.eldervale.item.inventory;

import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;

import net.mcreator.eldervale.world.inventory.ClassselectionMenu;
import net.mcreator.eldervale.init.EldervaleModItems;

import javax.annotation.Nonnull;

@EventBusSubscriber
public class ClassselectorInventoryCapability extends ComponentItemHandler {
	@SubscribeEvent
	public static void onItemDropped(ItemTossEvent event) {
		if (event.getEntity().getItem().getItem() == EldervaleModItems.CLASSSELECTOR.get()) {
			Player player = event.getPlayer();
			if (player.containerMenu instanceof ClassselectionMenu)
				player.closeContainer();
		}
	}

	public ClassselectorInventoryCapability(MutableDataComponentHolder parent) {
		super(parent, DataComponents.CONTAINER, 0);
	}

	@Override
	public int getSlotLimit(int slot) {
		return 1;
	}

	@Override
	public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
		return stack.getItem() != EldervaleModItems.CLASSSELECTOR.get();
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		return super.getStackInSlot(slot).copy();
	}
}