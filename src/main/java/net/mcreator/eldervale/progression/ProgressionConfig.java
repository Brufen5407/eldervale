package net.mcreator.eldervale.progression;

/**
 * All the numbers of the progression: XP curve, XP per kill, mob scaling, party sharing.
 * Edit only here. Planned pace: max level 100, ~4400 kills of same-level mobs in total
 * (level 10 after ~50 kills, 25 after ~180, 50 after ~730, 75 after ~2000).
 *
 * XP TO GO FROM LEVEL n TO n+1:
 *     (KILLS_BASE + KILLS_GROWTH * n^2) * (XP_BASE + XP_PER_MOB_LEVEL * n)
 *
 * XP OF A KILL (mob level L):  XP_BASE + XP_PER_MOB_LEVEL * L
 *
 * MOB LEVEL = zone level (Overworld: table of distances from spawn; Nether/End: from the origin) +-MOB_LEVEL_VARIANCE, max MAX_LEVEL.
 *
 * MOB STATS (MobScalingProcedure) grow in PERCENT over the vanilla value, so a Zombie stays weaker than a
 * Vindicator at the same level:
 *     HP     = vanillaHP    * (1 + MOB_HP_PCT_PER_LEVEL    * (L - 1))
 *     DAMAGE = every hit    * (1 + MOB_ATT_PCT_PER_LEVEL   * (L - 1))   (melee, arrows, explosions...)
 *     ARMOR  = vanillaArmor +  MOB_ARMOR_PER_LEVEL * (L - 1), capped
 *     SPEED  = vanillaSpeed * (1 + MOB_SPEED_PCT_PER_LEVEL * (L - 1))
 */
public final class ProgressionConfig {
	private ProgressionConfig() {
	}

	public static final int MAX_LEVEL = 100;

	// ---- Level curve ----
	public static final double KILLS_BASE = 5.0;
	public static final double KILLS_GROWTH = 0.012;

	// ---- XP of a kill ----
	public static final double XP_BASE = 20.0;
	public static final double XP_PER_MOB_LEVEL = 6.0;

	// ---- Level difference (anti-farming and reward for brave players) ----
	/** Mobs this many levels below the player (or less) give full XP. */
	public static final int LOWER_FREE_LEVELS = 5;
	public static final double LOWER_PENALTY_PER_LEVEL = 0.08;
	public static final double MIN_XP_MULTIPLIER = 0.10;
	/** Mobs this many levels above the player (or less) give normal XP, then a bonus. */
	public static final int HIGHER_FREE_LEVELS = 3;
	public static final double HIGHER_BONUS_PER_LEVEL = 0.05;
	public static final double MAX_XP_BONUS = 0.50;

	// ---- Party ----
	/** Members farther than this (or in another dimension) do not share the XP. */
	public static final double PARTY_SHARE_RANGE = 48.0;
	/** Every extra member in range adds this to the total XP of the group, which is then split equally. */
	public static final double PARTY_BONUS_PER_EXTRA_MEMBER = 0.10;

	// ---- Mob stats ----
	public static final double MOB_HP_PCT_PER_LEVEL = 0.06;
	public static final double MOB_ATT_PCT_PER_LEVEL = 0.03;
	public static final double MOB_ARMOR_PER_LEVEL = 0.15;
	public static final double MOB_ARMOR_CAP = 20.0;
	public static final double MOB_SPEED_PCT_PER_LEVEL = 0.003;
	/** Random spread of the level around the zone level (0.05 = +-5 %). */
	public static final double MOB_LEVEL_VARIANCE = 0.05;
	/**
	 * Monsters with more vanilla HP than this (Wither, Warden...) are bosses: they get a level
	 * (so they give XP and show a label) but their stats are NOT scaled.
	 */
	public static final double SKIP_SCALING_ABOVE_HEALTH = 150.0;

	// ---- Overworld zones: level by distance from the world spawn (linear between the points) ----
	// 0-1000: 1-15 | 1000-1500: 15-30 | 1500-2000: 30-45 | 2000-2500: 45-60
	// 2500-3000: 60-70 | 3000-3500: 70-80 | 3500-4000: 80-90 | 4000-4500: 90-100 (then 100)
	public static final double[] OVERWORLD_DISTANCES = { 0, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500 };
	public static final double[] OVERWORLD_LEVELS = { 1, 15, 30, 45, 60, 70, 80, 90, 100 };

	// ---- Nether / End: level = base + distance from the origin / blocksPerLevel, capped ----
	public static final double NETHER_BASE = 35.0;
	public static final double NETHER_BLOCKS_PER_LEVEL = 15.0;
	public static final double NETHER_CAP = 85.0;
	public static final double END_BASE = 75.0;
	public static final double END_BLOCKS_PER_LEVEL = 60.0;
	public static final double END_CAP = 100.0;

	// ---- Feedback (switch off here) ----
	public static final boolean XP_POPUP = true;      // floating "+N XP" above the dead mob (killer and party only)
	public static final boolean XP_SOUND = true;      // soft pickup sound to whoever receives XP
	public static final boolean LEVEL_UP_EFFECTS = true; // title, sounds and particles
}
