package net.mcreator.eldervale.progression;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Gives every hostile mob a level (from its zone) and the scaled stats of that level.
 * Replaces MobattributeProcedure completely — that procedure must be deleted from MCreator.
 *
 * Rules:
 *  - Each vanilla mob type has its own base stats (closest to their vanilla values) and growth
 *    factors, so a zombie feels different from a skeleton.
 *  - HP_final  = HP_base  * (1 + HP_GROWTH  * (level - 1))
 *  - ATT_final = ATT_base * (1 + ATT_GROWTH * (level - 1))
 *  - The flag "eldervale_scaled" is saved in persistent NBT so a chunk reload never re-scales.
 *  - Bosses (vanilla max HP > BOSS_HP_THRESHOLD) are not stat-scaled but still get a level
 *    for the mob label and XP calculation.
 *  - Mobs from the "eldervale" namespace are skipped entirely.
 */
@EventBusSubscriber
public final class MobScaling {

	public static final String TAG_LEVEL  = "mob level";
	private static final String TAG_SCALED = "eldervale_scaled";

	// Vanilla mobs with HP above this are treated as bosses (Wither = 300, Warden = 500, Elder Guardian = 80).
	private static final double BOSS_HP_THRESHOLD = 60.0;

	private MobScaling() {}

	// ---------------------------------------------------------------
	//  Per-type profile
	// ---------------------------------------------------------------

	/** Base stats and growth factors for a mob type. Growth is % per level (0.05 = +5 % per level). */
	private record MobProfile(double hpBase, double hpGrowth, double attBase, double attGrowth) {
		/** HP at the given level. */
		double hp(int level)  { return hpBase  * (1.0 + hpGrowth  * (level - 1)); }
		/** ATT at the given level. */
		double att(int level) { return attBase * (1.0 + attGrowth * (level - 1)); }
	}

	private static final MobProfile FALLBACK = new MobProfile(20, 0.05, 3, 0.04);

	/** Returns the profile for this entity type key (path only, namespace already checked). */
	private static MobProfile profileOf(ResourceLocation type) {
		return switch (type.getPath()) {
			// ---- Undead ----
			case "zombie",
			     "zombie_villager",
			     "husk",
			     "drowned"             -> new MobProfile(20, 0.06, 3,  0.04);
			case "skeleton",
			     "stray"               -> new MobProfile(20, 0.04, 2,  0.05);
			case "wither_skeleton"     -> new MobProfile(35, 0.05, 8,  0.05);
			case "zombie_horse"        -> new MobProfile(15, 0.05, 2,  0.04);
			// ---- Arthropods ----
			case "spider",
			     "cave_spider"         -> new MobProfile(16, 0.05, 2,  0.05);
			case "silverfish",
			     "endermite"           -> new MobProfile(8,  0.04, 1,  0.05);
			// ---- Nether ----
			case "blaze"               -> new MobProfile(20, 0.05, 6,  0.05);
			case "ghast"               -> new MobProfile(10, 0.04, 6,  0.04);
			case "magma_cube"          -> new MobProfile(16, 0.06, 4,  0.04);
			case "piglin",
			     "piglin_brute"        -> new MobProfile(16, 0.05, 5,  0.05);
			case "hoglin",
			     "zoglin"              -> new MobProfile(40, 0.06, 6,  0.04);
			// ---- End ----
			case "enderman"            -> new MobProfile(40, 0.05, 7,  0.04);
			case "shulker"             -> new MobProfile(30, 0.04, 4,  0.04);
			// ---- Overworld hostile ----
			case "creeper"             -> new MobProfile(20, 0.05, 3,  0.00); // creeper ATT = explosion, not melee
			case "witch"               -> new MobProfile(26, 0.04, 2,  0.04);
			case "slime"               -> new MobProfile(8,  0.06, 3,  0.04);
			case "phantom"             -> new MobProfile(20, 0.05, 6,  0.05);
			case "guardian"            -> new MobProfile(30, 0.05, 6,  0.05);
			// ---- Illagers ----
			case "pillager",
			     "illusioner"          -> new MobProfile(24, 0.05, 5,  0.05);
			case "vindicator"          -> new MobProfile(24, 0.05, 8,  0.05);
			case "evoker"              -> new MobProfile(24, 0.04, 6,  0.04);
			case "ravager"             -> new MobProfile(100,0.04,12,  0.04);
			// everything else uses FALLBACK
			default                    -> null;
		};
	}

	// ---------------------------------------------------------------
	//  Public helpers (used by ProgressionHandler for XP)
	// ---------------------------------------------------------------

	/** HP of a mob at the given level for the fallback profile (used when no saved level is found). */
	public static double fallbackHp(int level)  { return FALLBACK.hp(level);  }
	public static double fallbackAtt(int level) { return FALLBACK.att(level); }

	/** Level derived from vanilla max HP (used for unscaled bosses). */
	public static double bossLevel(double maxHealth) {
		return Mth.clamp((maxHealth - FALLBACK.hpBase) / (FALLBACK.hpBase * FALLBACK.hpGrowth) + 1, 1, ProgressionConfig.MAX_LEVEL);
	}

	// ---------------------------------------------------------------
	//  Event
	// ---------------------------------------------------------------

	@SubscribeEvent
	public static void onJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level))
			return;
		if (!(event.getEntity() instanceof Monster mob))
			return;
		// already processed: persistent data survives chunk unload and server restart
		if (mob.getPersistentData().getBoolean(TAG_SCALED))
			return;
		mob.getPersistentData().putBoolean(TAG_SCALED, true);

		ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());

		// skip our own mobs (they manage their own stats)
		if ("eldervale".equals(type.getNamespace()))
			return;

		int mobLevel = rollLevel(level, mob);

		// bosses: assign level only, do not touch HP/ATT
		if (mob.getMaxHealth() > BOSS_HP_THRESHOLD) {
			mob.getPersistentData().putDouble(TAG_LEVEL, bossLevel(mob.getMaxHealth()));
			return;
		}

		MobProfile profile = profileOf(type);
		if (profile == null) profile = FALLBACK;

		AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
		if (health != null)
			health.setBaseValue(profile.hp(mobLevel));

		AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attack != null && profile.attGrowth > 0.0)
			attack.setBaseValue(profile.att(mobLevel));

		mob.setHealth(mob.getMaxHealth()); // set current HP to new max
		mob.getPersistentData().putDouble(TAG_LEVEL, mobLevel);
	}

	// ---------------------------------------------------------------
	//  Zone level
	// ---------------------------------------------------------------

	/** Zone level from the distance to the spawn (Overworld) or origin (Nether, End), with random spread. */
	private static int rollLevel(ServerLevel level, Monster mob) {
		double x = mob.getX();
		double z = mob.getZ();
		double base, perLevel, cap;

		if (level.dimension() == Level.NETHER) {
			base = ProgressionConfig.NETHER_BASE;
			perLevel = ProgressionConfig.NETHER_BLOCKS_PER_LEVEL;
			cap = ProgressionConfig.NETHER_CAP;
		} else if (level.dimension() == Level.END) {
			base = ProgressionConfig.END_BASE;
			perLevel = ProgressionConfig.END_BLOCKS_PER_LEVEL;
			cap = ProgressionConfig.END_CAP;
		} else {
			BlockPos spawn = level.getSharedSpawnPos();
			x -= spawn.getX();
			z -= spawn.getZ();
			base = ProgressionConfig.OVERWORLD_BASE;
			perLevel = ProgressionConfig.OVERWORLD_BLOCKS_PER_LEVEL;
			cap = ProgressionConfig.OVERWORLD_CAP;
		}

		double zoneLevel = Math.min(cap, base + Math.sqrt(x * x + z * z) / perLevel);
		double spread = 1.0 + (mob.getRandom().nextDouble() * 2.0 - 1.0) * ProgressionConfig.MOB_LEVEL_VARIANCE;
		return Mth.clamp((int) Math.round(zoneLevel * spread), 1, ProgressionConfig.MAX_LEVEL);
	}
}
