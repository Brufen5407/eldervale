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
			"monk_meditation_end",
			"paladin_shield_start",
			"paladin_shield_hum",
			"paladin_shield_warn",
			"paladin_shield_end",
			"paladin_zone_cast",
			"paladin_zone_loop",
			"paladin_zone_end",
			"berserker_slam_jump",
			"berserker_slam_impact",
			"berserker_charge",
			"berserker_charge_hit",
			"berserker_mode_on",
			"berserker_mode_off",
			"berserker_heartbeat",
			"berserker_splash",
			"ranger_decoy_cast",
			"ranger_decoy_pulse",
			"ranger_decoy_end",
			"ranger_arrows_cast",
			"ranger_arrow_shot",
			"ranger_arrow_hit",
			"ranger_reflex",
			"mage_nova_cast",
			"mage_meteor_cast",
			"mage_meteor_fall",
			"mage_meteor_impact",
			"mage_stack",
			"assassin_strike_dash",
			"assassin_strike_hit",
			"assassin_strike_crit",
			"assassin_smoke_cast",
			"assassin_smoke_loop",
			"assassin_smoke_end",
			"assassin_ambush",
			"monk_barrage_start",
			"monk_barrage_punch",
			"monk_barrage_end",
			"werewolf_transform_on",
			"werewolf_transform_off",
			"werewolf_pounce_launch",
			"werewolf_pounce_land",
			"werewolf_instinct",
			"alchemist_throw",
			"alchemist_brew_burst",
			"alchemist_brew_loop",
			"alchemist_brew_end",
			"alchemist_compound_throw",
			"alchemist_compound_stick",
			"alchemist_compound_tick",
			"alchemist_compound_explode",
			"alchemist_stack",
			"alchemist_contam_burst",
			"spellblade_slash",
			"spellblade_slash_hit",
			"spellblade_phantom_cast",
			"spellblade_phantom_hit",
			"spellblade_conversion"
	};

	/** own namespace: MCreator regenerates assets/eldervale/sounds.json and would erase these entries */
	public static final String NAMESPACE = "eldervale_vfx";

	private static final Map<String, SoundEvent> EVENTS = new HashMap<>();

	@SubscribeEvent
	public static void onRegister(RegisterEvent event) {
		event.register(Registries.SOUND_EVENT, helper -> {
			for (String name : NAMES) {
				ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NAMESPACE, name);
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
