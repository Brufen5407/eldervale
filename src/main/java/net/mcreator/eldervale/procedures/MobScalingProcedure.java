package net.mcreator.eldervale.procedures;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;

import net.mcreator.eldervale.EldervaleMod;
import net.mcreator.eldervale.progression.ProgressionConfig;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Mob levels and stat scaling. Server side only. All numbers live in ProgressionConfig.
 *
 * 1) The first time a hostile mob enters the world it gets a level (distance from spawn / origin).
 *    HP, armor and speed are scaled once, in percent over the vanilla value.
 *    Mobs above SKIP_SCALING_ABOVE_HEALTH (bosses) get a level but keep their vanilla stats.
 * 2) Damage is NOT written into the ATTACK_DAMAGE attribute (Skeletons, Ghasts, Creepers have none).
 *    It is a multiplier on every hit the mob deals, so melee, arrows, fireballs and explosions scale alike.
 *
 * The level is stored in the entity persistent data (saved with the mob) under TAG_LEVEL and sent to the
 * clients by MobLevelPayload, because persistent data is NOT synchronised to the client.
 */
@EventBusSubscriber
public class MobScalingProcedure {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Same key as the old system (ProgressionHandler reads it). Stored as int. */
	public static final String TAG_LEVEL = "mob level";
	/** Set the first time the mob is processed: guarantees "only once", survives reload and restart. */
	public static final String TAG_SCALED = "eldervale_scaled";
	/** Boss: has a level but no stat scaling (no damage multiplier either). */
	public static final String TAG_UNSCALED = "eldervale_unscaled";

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		if (!(event.getEntity() instanceof Monster mob))
			return;

		CompoundTag data = mob.getPersistentData();
		if (data.getBoolean(TAG_SCALED))
			return;
		data.putBoolean(TAG_SCALED, true);

		// our own mobs (decoy...) are never levelled
		if (EldervaleMod.MODID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace()))
			return;

		int mobLevel = rollLevel(level, mob);
		data.putInt(TAG_LEVEL, mobLevel);

		AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
		if (hp != null && hp.getBaseValue() > ProgressionConfig.SKIP_SCALING_ABOVE_HEALTH) {
			data.putBoolean(TAG_UNSCALED, true);
			LOGGER.debug("[MobScaling] {} level={} (boss, stats not scaled)", mob.getType().toShortString(), mobLevel);
			return;
		}

		int steps = mobLevel - 1;

		if (hp != null) {
			hp.setBaseValue(hp.getBaseValue() * (1.0 + ProgressionConfig.MOB_HP_PCT_PER_LEVEL * steps));
			mob.setHealth(mob.getMaxHealth());
		}

		AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
		if (armor != null)
			armor.setBaseValue(Mth.clamp(armor.getBaseValue() + ProgressionConfig.MOB_ARMOR_PER_LEVEL * steps, 0.0, ProgressionConfig.MOB_ARMOR_CAP));

		AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null)
			speed.setBaseValue(speed.getBaseValue() * (1.0 + ProgressionConfig.MOB_SPEED_PCT_PER_LEVEL * steps));

		LOGGER.debug("[MobScaling] {} level={} hp={}", mob.getType().toShortString(), mobLevel, mob.getMaxHealth());
	}

	/** Runs before DefenceHandler (HIGH), so the player's defence reduces the already-scaled damage. */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onIncomingDamage(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide())
			return;
		Entity attacker = event.getSource().getEntity(); // for arrows / fireballs this is the shooter
		if (!(attacker instanceof Monster mob))
			return;
		CompoundTag data = mob.getPersistentData();
		int mobLevel = data.getInt(TAG_LEVEL);
		if (mobLevel <= 1 || data.getBoolean(TAG_UNSCALED))
			return;
		event.setAmount((float) (event.getAmount() * (1.0 + ProgressionConfig.MOB_ATT_PCT_PER_LEVEL * (mobLevel - 1))));
	}

	private static int rollLevel(ServerLevel level, Monster mob) {
		double x = mob.getX(), z = mob.getZ();
		double zone;

		if (level.dimension() == Level.NETHER) {
			zone = Math.min(ProgressionConfig.NETHER_CAP, ProgressionConfig.NETHER_BASE + Math.sqrt(x * x + z * z) / ProgressionConfig.NETHER_BLOCKS_PER_LEVEL);
		} else if (level.dimension() == Level.END) {
			zone = Math.min(ProgressionConfig.END_CAP, ProgressionConfig.END_BASE + Math.sqrt(x * x + z * z) / ProgressionConfig.END_BLOCKS_PER_LEVEL);
		} else {
			BlockPos spawn = level.getSharedSpawnPos();
			x -= spawn.getX();
			z -= spawn.getZ();
			zone = overworldZoneLevel(Math.sqrt(x * x + z * z));
		}

		double spread = 1.0 + (mob.getRandom().nextDouble() * 2.0 - 1.0) * ProgressionConfig.MOB_LEVEL_VARIANCE;
		return Mth.clamp((int) Math.round(zone * spread), 1, ProgressionConfig.MAX_LEVEL);
	}

	/** Linear interpolation over the distance/level table in ProgressionConfig. */
	private static double overworldZoneLevel(double dist) {
		double[] d = ProgressionConfig.OVERWORLD_DISTANCES;
		double[] l = ProgressionConfig.OVERWORLD_LEVELS;
		int last = d.length - 1;
		if (dist <= d[0])
			return l[0];
		if (dist >= d[last])
			return l[last];
		for (int i = 1; i <= last; i++) {
			if (dist <= d[i]) {
				double t = (dist - d[i - 1]) / (d[i] - d[i - 1]);
				return l[i - 1] + t * (l[i] - l[i - 1]);
			}
		}
		return l[last];
	}
}
