package net.mcreator.eldervale.procedures;

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

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Assigns a level and scaled stats to every hostile mob the first time it enters the world.
 * Lives in the procedures package so NeoForge scans and registers it automatically.
 *
 * Growth per level (applied to the vanilla base value of each stat):
 *   HP    +6% per level
 *   ATT   +3% per level
 *   ARMOR +0.15 per level (Minecraft armor points)
 *   SPEED +0.3% per level
 */
@EventBusSubscriber
public class MobScalingProcedure {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String TAG_LEVEL  = "mob level";
    public static final String TAG_SCALED = "eldervale_scaled";

    // ---- Zone config ----
    private static final double OW_BASE   = 1.0,  OW_BPL  = 50.0,  OW_CAP  = 60.0;
    private static final double NE_BASE   = 35.0, NE_BPL  = 15.0,  NE_CAP  = 85.0;
    private static final double EN_BASE   = 75.0, EN_BPL  = 60.0,  EN_CAP  = 100.0;
    private static final double VARIANCE  = 0.15;
    private static final int    MAX_LEVEL = 100;

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Monster mob)) return;

        // already scaled: skip (flag survives chunk reload and server restart)
        if (mob.getPersistentData().getBoolean(TAG_SCALED)) return;
        mob.getPersistentData().putBoolean(TAG_SCALED, true);

        // skip our own mobs
        if ("eldervale".equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getNamespace())) return;

        int mobLevel = rollLevel(level, mob);
        mob.getPersistentData().putInt(TAG_LEVEL, mobLevel);

        LOGGER.info("[MobScaling] {} level={}", mob.getType().toShortString(), mobLevel);

        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            double scaled = hp.getBaseValue() * (1.0 + 0.06 * (mobLevel - 1));
            hp.setBaseValue(scaled);
            mob.setHealth((float) scaled);
        }

        AttributeInstance att = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (att != null)
            att.setBaseValue(att.getBaseValue() * (1.0 + 0.03 * (mobLevel - 1)));

        AttributeInstance armor = mob.getAttribute(Attributes.ARMOR);
        if (armor != null)
            armor.setBaseValue(Mth.clamp(armor.getBaseValue() + 0.15 * (mobLevel - 1), 0.0, 20.0));

        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null)
            speed.setBaseValue(speed.getBaseValue() * (1.0 + 0.003 * (mobLevel - 1)));
    }

    private static int rollLevel(ServerLevel level, Monster mob) {
        double x = mob.getX(), z = mob.getZ();
        double base, bpl, cap;

        if (level.dimension() == Level.NETHER) {
            base = NE_BASE; bpl = NE_BPL; cap = NE_CAP;
        } else if (level.dimension() == Level.END) {
            base = EN_BASE; bpl = EN_BPL; cap = EN_CAP;
        } else {
            BlockPos spawn = level.getSharedSpawnPos();
            x -= spawn.getX(); z -= spawn.getZ();
            base = OW_BASE; bpl = OW_BPL; cap = OW_CAP;
        }

        double dist = Math.sqrt(x * x + z * z);
        double zone = Math.min(cap, base + dist / bpl);
        double spread = 1.0 + (mob.getRandom().nextDouble() * 2.0 - 1.0) * VARIANCE;
        return Mth.clamp((int) Math.round(zone * spread), 1, MAX_LEVEL);
    }
}
