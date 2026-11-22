package net.mcreator.eldervale.stats;

import java.util.Collection;
import java.util.Map;

/**
 * Single source of truth for the class base stats (values from the CLASSI PDF) and the
 * attribute multipliers of each class.
 *
 * def is a fraction (PDF "DIF 10" = 0.10), same scale as PlayerVariables.DEFENCE.
 * Multiplier order = StatAttribute order: STR, VIT, DEF, AGI, INT, DEX.
 * 1.5 = main attribute, 1.25 = secondary, 0.75 = the others.
 */
public record ClassProfile(double hp, double def, double attSpeed, double att, double mana, double manaRegen, double speed, double[] multipliers) {

	private static final Map<String, ClassProfile> PROFILES = Map.of(
			//                                   hp    def   atkSpd att  mana  regen speed   STR   VIT   DEF   AGI   INT   DEX
			"paladin",    new ClassProfile(50, 0.10, 4.0, 1.5,  80,  4, 0.09,  new double[]{0.75, 1.50, 1.25, 0.75, 0.75, 0.75}),
			"berserker",  new ClassProfile(35, 0.04, 3.2, 2.5,  50,  2, 0.10,  new double[]{1.50, 1.25, 0.75, 0.75, 0.75, 0.75}),
			"ranger",     new ClassProfile(25, 0.02, 4.4, 1.2, 120,  6, 0.12,  new double[]{0.75, 0.75, 0.75, 1.25, 0.75, 1.50}),
			"mage",       new ClassProfile(20, 0.01, 3.0, 0.5, 160, 10, 0.10,  new double[]{0.75, 0.75, 0.75, 0.75, 1.50, 0.75}),
			"assassin",   new ClassProfile(25, 0.02, 5.0, 1.5,  70,  4, 0.14,  new double[]{0.75, 0.75, 0.75, 1.50, 0.75, 1.25}),
			"monk",       new ClassProfile(35, 0.05, 5.0, 1.8, 100,  5, 0.105, new double[]{1.25, 0.75, 0.75, 1.50, 0.75, 0.75}),
			"werewolf",   new ClassProfile(40, 0.06, 4.5, 1.7,  60,  3, 0.12,  new double[]{1.50, 1.25, 0.75, 0.75, 0.75, 0.75}),
			"alchemist",  new ClassProfile(20, 0.01, 3.2, 0.7, 150, 10, 0.10,  new double[]{0.75, 0.75, 0.75, 0.75, 1.50, 1.25}),
			"spellblade", new ClassProfile(30, 0.04, 4.2, 1.7, 120,  8, 0.11,  new double[]{1.50, 0.75, 0.75, 0.75, 1.25, 0.75}));

	/** Returns null if the player has no valid class yet (the default PLAYER_CLASS is not a class). */
	public static ClassProfile get(String playerClass) {
		return playerClass == null ? null : PROFILES.get(playerClass);
	}

	/** All class profiles (used by the GUI to scale the stat bars). */
	public static Collection<ClassProfile> all() {
		return PROFILES.values();
	}

	public double multiplier(StatAttribute attribute) {
		return multipliers[attribute.ordinal()];
	}
}
