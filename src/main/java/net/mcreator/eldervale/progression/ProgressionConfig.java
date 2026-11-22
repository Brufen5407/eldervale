package net.mcreator.eldervale.progression;

/**
 * All the numbers of the progression: XP curve, XP per kill, mob scaling, party sharing.
 * Edit only here. Planned pace: max level 100, ~4400 kills of same-level mobs in total
 * (level 10 after ~50 kills, 25 after ~180, 50 after ~730, 75 after ~2000).
 *
 * XP TO GO FROM LEVEL n TO n+1:
 *     (KILLS_BASE + KILLS_GROWTH * n^2) * (XP_BASE + XP_PER_MOB_LEVEL * n)
 *   = "kills needed at level n" * "XP of a mob of level n"
 *
 * XP OF A KILL (mob level L):  XP_BASE + XP_PER_MOB_LEVEL * L
 *   then * level-difference multiplier * party share.
 *
 * MOB LEVEL = zone level (distance from the world spawn / world origin, per dimension) +-MOB_LEVEL_VARIANCE.
 * MOB STATS: HP = MOB_HP_BASE + MOB_HP_PER_LEVEL * L,  ATT = MOB_ATT_BASE + MOB_ATT_PER_LEVEL * L.
 * Re-tune the mob HP / ATT together with the weapons when custom equipment arrives.
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
	public static final double MOB_HP_BASE = 20.0;
	public static final double MOB_HP_PER_LEVEL = 1.5;
	public static final double MOB_ATT_BASE = 3.0;
	public static final double MOB_ATT_PER_LEVEL = 0.07;
	/** Random spread of the level around the zone level (0.15 = +-15 %). */
	public static final double MOB_LEVEL_VARIANCE = 0.15;
	/** Monsters with more vanilla HP than this (Wither, Warden...) are bosses: not scaled for now. */
	public static final double SKIP_SCALING_ABOVE_HEALTH = 60.0;

	// ---- Zones: level = base + distance / blocksPerLevel, capped ----
	public static final double OVERWORLD_BASE = 1.0;
	public static final double OVERWORLD_BLOCKS_PER_LEVEL = 50.0;
	public static final double OVERWORLD_CAP = 60.0;
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
