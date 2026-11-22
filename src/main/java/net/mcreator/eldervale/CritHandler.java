package net.mcreator.eldervale;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

@EventBusSubscriber(modid = "eldervale")
public class CritHandler {

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        // Turn the crit off (no bonus damage, no crit particles/sound)
        event.setCriticalHit(false);
        // Neutralize the multiplier as a fallback
        event.setDamageMultiplier(1.0F);
    }
}