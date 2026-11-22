package net.mcreator.eldervale.stats;

/**
 * All tunable numbers of the Player Stats System. Edit only here.
 *
 * FINAL STAT FORMULA (StatCalculator):
 *     final = clamp( (base + flat) * (1 + pct) )
 *   base = class value (ClassProfile), flat/pct = sum of attribute + equipment + temporary bonuses.
 *
 * ATTRIBUTE BONUS (per attribute):
 *     effective points = invested points * class multiplier (ClassProfile: 1.5 / 1.25 / 0.75)
 *     bonus = min(effective points * PER_POINT, CAP)
 *
 * TUNING RULE: every PER_POINT is CAP / FULL_EFFECTIVE_POINTS. A class reaches the cap of an attribute
 * only when it puts MAX_POINTS_PER_ATTRIBUTE points in a main attribute (multiplier 1.5);
 * a 1.25 attribute reaches ~83 % of the cap, a 0.75 attribute ~50 %. Change a CAP or the
 * maximum and the per-point values follow by themselves.
 *
 * Planned progression: max level 100, 3 points per level = ~297 points in total.
 * Crit values use the same unit as PlayerVariables: 5.0 = 5 %, 150.0 = x1.5 damage.
 */
public final class StatsConfig {
	private StatsConfig() {
	}

	// ---- Points ----
	/** Attribute points given for every level gained (used by ProgressionHandler). */
	public static final int POINTS_PER_LEVEL = 3;
	public static final int MAX_POINTS_PER_ATTRIBUTE = 150;
	/** Max points the client can ask to spend in one request. */
	public static final int MAX_SPEND_PER_REQUEST = 10;

	public static final double PRIMARY_MULTIPLIER = 1.5;
	/** Effective points at which an attribute reaches its cap. */
	public static final double FULL_EFFECTIVE_POINTS = MAX_POINTS_PER_ATTRIBUTE * PRIMARY_MULTIPLIER;

	// ---- Strength -> ATT (percent of class base) ----
	public static final double STRENGTH_ATT_PCT_CAP = 1.0;
	public static final double STRENGTH_ATT_PCT = STRENGTH_ATT_PCT_CAP / FULL_EFFECTIVE_POINTS;

	// ---- Vitality -> max HP (flat) ----
	public static final double VITALITY_HP_CAP = 50.0;
	public static final double VITALITY_HP = VITALITY_HP_CAP / FULL_EFFECTIVE_POINTS;

	// ---- Defense -> DEF (fraction, 0.5 = +50 DEF) ----
	public static final double DEFENSE_DEF_CAP = 0.5;
	public static final double DEFENSE_DEF = DEFENSE_DEF_CAP / FULL_EFFECTIVE_POINTS;
	public static final double MAX_DEFENCE = 0.8; // same cap as DefenceHandler

	// ---- Agility -> SPEED and ATT SPEED (percent of base) ----
	public static final double AGILITY_SPEED_PCT_CAP = 0.4;
	public static final double AGILITY_SPEED_PCT = AGILITY_SPEED_PCT_CAP / FULL_EFFECTIVE_POINTS;
	public static final double AGILITY_ATT_SPEED_PCT_CAP = 0.5;
	public static final double AGILITY_ATT_SPEED_PCT = AGILITY_ATT_SPEED_PCT_CAP / FULL_EFFECTIVE_POINTS;

	// ---- Intelligence -> MANA and MANA REGEN (flat) ----
	public static final double INTELLIGENCE_MANA_CAP = 150.0;
	public static final double INTELLIGENCE_MANA = INTELLIGENCE_MANA_CAP / FULL_EFFECTIVE_POINTS;
	public static final double INTELLIGENCE_REGEN_CAP = 5.0;
	public static final double INTELLIGENCE_REGEN = INTELLIGENCE_REGEN_CAP / FULL_EFFECTIVE_POINTS;

	// ---- Dexterity -> critical (flat, same unit as PlayerVariables) ----
	public static final double DEXTERITY_CRIT_CHANCE_CAP = 45.0;
	public static final double DEXTERITY_CRIT_CHANCE = DEXTERITY_CRIT_CHANCE_CAP / FULL_EFFECTIVE_POINTS;
	public static final double DEXTERITY_CRIT_DAMAGE_CAP = 100.0;
	public static final double DEXTERITY_CRIT_DAMAGE = DEXTERITY_CRIT_DAMAGE_CAP / FULL_EFFECTIVE_POINTS;

	// ---- Critical base and system-wide hard limits ----
	public static final double BASE_CRIT_CHANCE = 5.0;
	public static final double MAX_CRIT_CHANCE = 60.0;
	public static final double BASE_CRIT_DAMAGE = 150.0;
	public static final double MAX_CRIT_DAMAGE = 300.0;
	public static final boolean CRIT_EFFECTS = true; // crit particles on the target
	public static final boolean CRIT_SOUND = true; // crit sound at the target (heard by everyone nearby)
	public static final boolean CRIT_TEXT = true; // floating "CRITICAL!" text (shown to the attacker only)
}
