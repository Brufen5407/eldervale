package net.mcreator.eldervale.vfx;

/**
 * Shared ids for the VFX system (server and client).
 * Add one constant per effect. Never reuse or renumber an existing id.
 */
public final class VfxIds {

	// ---- Effects ----
	public static final int MONK_MEDITATION = 1;
	public static final int MONK_BARRAGE = 2;

	public static final int PALADIN_SHIELDS = 10;
	public static final int PALADIN_ZONE = 11;

	public static final int BERSERKER_SLAM = 20;
	public static final int BERSERKER_CHARGE = 21;
	public static final int BERSERK_MODE = 22;
	public static final int BERSERK_SPLASH = 23;

	public static final int RANGER_DECOY = 30;
	public static final int RANGER_ARROWS = 31;
	public static final int RANGER_REFLEX = 32;

	public static final int MAGE_NOVA = 40;
	public static final int MAGE_METEOR = 41;
	public static final int MAGE_MASTERY = 42;

	public static final int ASSASSIN_STRIKE = 50;
	public static final int ASSASSIN_SMOKE = 51;
	public static final int ASSASSIN_AMBUSH = 52;

	public static final int WEREWOLF_BEAST = 60;
	public static final int WEREWOLF_POUNCE = 61;
	public static final int WEREWOLF_INSTINCT = 62;

	public static final int ALCHEMIST_BREW = 70;
	public static final int ALCHEMIST_COMPOUND = 71;
	public static final int ALCHEMIST_CONTAMINATION = 72;

	public static final int SPELLBLADE_SLASH = 80;
	public static final int SPELLBLADE_PHANTOM = 81;
	public static final int SPELLBLADE_CONVERSION = 82;

	// ---- Phases of an effect ----
	public static final int START = 0;
	public static final int PULSE = 1;
	public static final int IMPACT = 2;
	public static final int END = 3;

	private VfxIds() {
	}
}
