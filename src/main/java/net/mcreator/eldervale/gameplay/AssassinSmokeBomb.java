package net.mcreator.eldervale.gameplay;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.vfx.VfxIds;
import net.mcreator.eldervale.vfx.VfxServer;

/**
 * Assassin skill: SMOKE BOMB.
 *  - Always: the Assassin gets invisibility (5 s) and Speed II (2 s), even with no monsters nearby.
 *  - Monsters within RADIUS are slowed, lose the Assassin as target and cannot target him
 *    again for AGGRO_TICKS (5 s).
 *  - The smoke cloud is drawn by the client VFX system (AssassinSmokeVfx) and stays for CLOUD_TICKS (5 s).
 *
 * Mana (30) and cooldown (360 ticks = 18 s) stay in the MCreator procedure AssassinSkill2.
 * This class detects the cast on the server (the SKILL2 cooldown jumps up), the same
 * pattern used by MonkPunchBarrage and MonkMeditation.
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 * Gameplay unchanged: the old particles and vanilla sounds are replaced by one VFX event.
 */
@EventBusSubscriber
public class AssassinSmokeBomb {

	// ---- Settings (edit here) ----
	private static final double RADIUS = 4.0D;
	private static final int SLOW_TICKS = 100;           // 5 s
	private static final int SLOW_LEVEL = 3;             // Slowness IV (value used by the old procedure)
	private static final int AGGRO_TICKS = 100;          // monsters cannot target the Assassin for 5 s
	private static final int INVISIBILITY_TICKS = 100;   // 5 s
	private static final int SPEED_TICKS = 40;           // 2 s
	private static final int SPEED_LEVEL = 1;            // 1 = Speed II ("speed 2" in the CLASSI PDF)
	private static final int CLOUD_TICKS = 100;          // how long the smoke cloud stays (5 s)

	private static class Smoke {
		final Set<UUID> mobs;
		int left;

		Smoke(Set<UUID> mobs, int left) {
			this.mobs = mobs;
			this.left = left;
		}
	}

	private static final Map<UUID, Double> PREV_CD = new HashMap<>();
	private static final Map<UUID, Smoke> SMOKES = new HashMap<>(); // player -> monsters that cannot target him

	// ---------------------------------------------------------------
	//  Cast detection (the MCreator procedure sets the cooldown)
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		double cd = v.SKILL2_CD;
		double max = v.SKILL2_MAX_CD;
		Double prev = PREV_CD.put(player.getUUID(), cd);
		if (prev == null)
			return;

		// cast: the cooldown jumped up to its maximum
		if ("assassin".equals(v.PLAYER_CLASS) && player.isAlive() && cd - prev > 50.0D && cd >= max - 10.0D)
			cast(player);
	}

	private static void cast(ServerPlayer player) {
		ServerLevel level = player.serverLevel();

		// monsters in the cloud: slowed, aggro removed, and kept away from the Assassin for AGGRO_TICKS
		Set<UUID> affected = new HashSet<>();
		for (Monster monster : level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(RADIUS), m -> m.isAlive() && m.distanceToSqr(player) <= RADIUS * RADIUS)) {
			monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, SLOW_LEVEL, false, true));
			dropTarget(monster, player);
			affected.add(monster.getUUID());
		}
		if (!affected.isEmpty())
			SMOKES.put(player.getUUID(), new Smoke(affected, AGGRO_TICKS));

		// always applied, with or without monsters (the skill is also an escape)
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_TICKS, 0, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SPEED_TICKS, SPEED_LEVEL, false, false));

		// VFX: the cloud stays where the bomb was thrown (arg = duration in ticks, param = radius)
		VfxServer.sendAt(player, VfxIds.ASSASSIN_SMOKE, VfxIds.START, CLOUD_TICKS, player.getX(), player.getY(), player.getZ(), (float) RADIUS);
	}

	// Goal-based mobs use getTarget/setTarget; brain-based mobs (piglins, warden...) keep it in a memory
	private static void dropTarget(Mob mob, ServerPlayer player) {
		if (mob.getTarget() == player)
			mob.setTarget(null);
		mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
	}

	// ---------------------------------------------------------------
	//  Server tick: keeps the aggro off
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (SMOKES.isEmpty())
			return;
		MinecraftServer server = event.getServer();

		Iterator<Map.Entry<UUID, Smoke>> it = SMOKES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Smoke> e = it.next();
			Smoke s = e.getValue();
			ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
			if (player == null || --s.left <= 0) {
				it.remove();
				continue;
			}
			ServerLevel level = player.serverLevel();
			for (UUID id : s.mobs) {
				Entity entity = level.getEntity(id);
				if (entity instanceof Mob mob && mob.isAlive() && mob.getTarget() == player)
					dropTarget(mob, player);
			}
		}
	}

	// ---------------------------------------------------------------
	//  Cleanup
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		UUID id = event.getEntity().getUUID();
		PREV_CD.remove(id);
		SMOKES.remove(id);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PREV_CD.clear();
		SMOKES.clear();
	}
}
