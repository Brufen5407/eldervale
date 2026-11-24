package net.mcreator.eldervale.vfx;

import java.util.HashMap;
import java.util.Map;

import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

/**
 * Registers the VFX sound events (one per name in NAMES) and gives access to them.
 * Every name here needs: an entry in assets/eldervale/sounds.json and a mono .ogg in assets/eldervale/sounds/.
 * Add a new sound by adding its name to NAMES.
 */
@EventBusSubscriber
public final class VfxSounds {

	private static final String[] NAMES = {
			"monk_meditation_start",
			"monk_meditation_aura",
			"monk_meditation_pulse_1",
			"monk_meditation_pulse_2",
			"monk_meditation_pulse_3",
			"monk_meditation_end"
	};

	private static final Map<String, SoundEvent> EVENTS = new HashMap<>();

	@SubscribeEvent
	public static void onRegister(RegisterEvent event) {
		event.register(Registries.SOUND_EVENT, helper -> {
			for (String name : NAMES) {
				ResourceLocation id = ResourceLocation.fromNamespaceAndPath("eldervale", name);
				SoundEvent sound = SoundEvent.createVariableRangeEvent(id);
				EVENTS.put(name, sound);
				helper.register(id, sound);
			}
		});
	}

	public static SoundEvent get(String name) {
		return EVENTS.get(name);
	}

	private VfxSounds() {
	}
}
