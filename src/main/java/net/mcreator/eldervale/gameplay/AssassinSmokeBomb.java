package net.mcreator.eldervale.gameplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Assassin skill: SMOKE BOMB.
 *  - Always: the Assassin gets invisibility (5 s) and Speed II (2 s), even with no monsters nearby.
 *  - Monsters within RADIUS are slowed, lose the Assassin as target and cannot target him
 *    again for AGGRO_TICKS (5 s).
 *  - A smoke cloud (particles + sounds) stays on the spot for CLOUD_TICKS (5 s).
 *
 * Mana (30) and cooldown (360 ticks = 18 s) stay in the MCreator procedure AssassinSkill2.
 * This class detects the cast on the server (the SKILL2 cooldown jumps up), the same
 * pattern used by MonkPunchBarrage and MonkMeditation.
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 *
 * Custom sound: if a sound event "eldervale:smoke_bomb" is registered (MCreator > Sounds),
 * it is played instead of the vanilla sound mix below.
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
	private static final boolean EFFECTS_ENABLED = true;
	private static final boolean SOUNDS_ENABLED = true;

	private static final ResourceLocation CUSTOM_SOUND = ResourceLocation.fromNamespaceAndPath("eldervale", "smoke_bomb");
	private static final DustParticleOptions PURPLE = new DustParticleOptions(new Vector3f(0.55F, 0.35F, 0.90F), 1.5F);

	private static class Smoke {
		final Set<UUID> mobs;
		int left;

		Smoke(Set<UUID> mobs, int left) {
			this.mobs = mobs;
			this.left = left;
		}
	}

	private static class Cloud {
		final ResourceKey<Level> dim;
		final double x, y, z;
		int age = 0;

		Cloud(ResourceKey<Level> dim, double x, double y, double z) {
			this.dim = dim;
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private static final Map<UUID, Double> PREV_CD = new HashMap<>();
	private static final Map<UUID, Smoke> SMOKES = new HashMap<>(); // player -> monsters that cannot target him
	private static final List<Cloud> CLOUDS = new ArrayList<>();

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
		Vec3 c = player.position();

		// monsters in the cloud: slowed, aggro removed, and kept away from the Assassin for AGGRO_TICKS
		Set<UUID> affected = new HashSet<>();
		for (Monster monster : level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(RADIUS), m -> m.isAlive() && m.distanceToSqr(player) <= RADIUS * RADIUS)) {
			monster.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, SLOW_LEVEL, false, true));
			dropTarget(monster, player);
			affected.add(monster.getUUID());
			if (EFFECTS_ENABLED)
				level.sendParticles(ParticleTypes.SMOKE, monster.getX(), monster.getY(0.5D), monster.getZ(), 10, 0.3D, 0.4D, 0.3D, 0.02D);
		}
		if (!affected.isEmpty())
			SMOKES.put(player.getUUID(), new Smoke(affected, AGGRO_TICKS));

		// always applied, with or without monsters (the skill is also an escape)
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, INVISIBILITY_TICKS, 0, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SPEED_TICKS, SPEED_LEVEL, false, false));

		CLOUDS.add(new Cloud(level.dimension(), c.x, c.y, c.z));
		castFx(level, c);
	}

	// Goal-based mobs use getTarget/setTarget; brain-based mobs (piglins, warden...) keep it in a memory
	private static void dropTarget(Mob mob, ServerPlayer player) {
		if (mob.getTarget() == player)
			mob.setTarget(null);
		mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
	}

	// ---------------------------------------------------------------
	//  Server tick: keeps the aggro off and animates the cloud
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (SMOKES.isEmpty() && CLOUDS.isEmpty())
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

		Iterator<Cloud> ci = CLOUDS.iterator();
		while (ci.hasNext()) {
			Cloud c = ci.next();
			ServerLevel level = server.getLevel(c.dim);
			if (level == null || c.age >= CLOUD_TICKS) {
				ci.remove();
				continue;
			}
			if (c.age % 4 == 0)
				cloudFx(level, c);
			if (c.age % 20 == 0 && SOUNDS_ENABLED)
				sound(level, c.x, c.y, c.z, SoundEvents.CAMPFIRE_CRACKLE, 0.4F, 0.7F);
			c.age++;
		}
	}

	// ---------------------------------------------------------------
	//  Effects and sounds (sent by the server, everyone sees and hears them)
	// ---------------------------------------------------------------
	private static void castFx(ServerLevel level, Vec3 c) {
		if (EFFECTS_ENABLED) {
			level.sendParticles(ParticleTypes.POOF, c.x, c.y + 0.6D, c.z, 40, 0.5D, 0.4D, 0.5D, 0.12D);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 0.5D, c.z, 25, 0.6D, 0.4D, 0.6D, 0.05D);
			// expanding ring of smoke
			for (int i = 0; i < 36; i++) {
				double a = i * (Math.PI * 2.0D / 36.0D);
				level.sendParticles(ParticleTypes.CLOUD, c.x, c.y + 0.3D, c.z, 0, Math.cos(a), 0.05D, Math.sin(a), 0.35D);
				if (i % 3 == 0)
					level.sendParticles(PURPLE, c.x + Math.cos(a) * RADIUS * 0.5D, c.y + 0.6D, c.z + Math.sin(a) * RADIUS * 0.5D, 3, 0.2D, 0.3D, 0.2D, 0.0D);
			}
		}
		if (SOUNDS_ENABLED) {
			SoundEvent custom = BuiltInRegistries.SOUND_EVENT.get(CUSTOM_SOUND);
			if (custom != null) {
				sound(level, c.x, c.y, c.z, custom, 1.0F, 1.0F);
			} else {
				sound(level, c.x, c.y, c.z, SoundEvents.FIRECHARGE_USE, 0.8F, 0.6F);
				sound(level, c.x, c.y, c.z, SoundEvents.SPLASH_POTION_BREAK, 0.7F, 0.7F);
				sound(level, c.x, c.y, c.z, SoundEvents.GENERIC_EXTINGUISH_FIRE, 0.9F, 0.8F);
			}
		}
	}

	private static void cloudFx(ServerLevel level, Cloud c) {
		if (!EFFECTS_ENABLED)
			return;
		for (int i = 0; i < 8; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2.0D;
			double r = RADIUS * Math.sqrt(level.getRandom().nextDouble());
			level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x + Math.cos(a) * r, c.y + 0.2D, c.z + Math.sin(a) * r, 0, 0.0D, 0.08D, 0.0D, 1.0D);
		}
		for (int i = 0; i < 3; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2.0D;
			double r = RADIUS * Math.sqrt(level.getRandom().nextDouble());
			level.sendParticles(PURPLE, c.x + Math.cos(a) * r, c.y + 0.5D, c.z + Math.sin(a) * r, 2, 0.3D, 0.3D, 0.3D, 0.01D);
		}
	}

	private static void sound(ServerLevel level, double x, double y, double z, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, volume, pitch);
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
		CLOUDS.clear();
	}
}
