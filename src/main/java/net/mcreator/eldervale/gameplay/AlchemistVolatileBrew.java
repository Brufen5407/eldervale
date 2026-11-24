package net.mcreator.eldervale.gameplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.component.DataComponents;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Alchemist skill 1: VOLATILE BREW.
 *  - Throws an unstable potion (vanilla ThrownPotion, green). On impact (block or entity) it
 *    bursts into a toxic cloud of RADIUS blocks that lasts CLOUD_TICKS.
 *  - Enemies in the cloud take damage over time (ATT x DAMAGE_PER_SECOND_MULT per second,
 *    applied every PULSE_INTERVAL ticks) and are slowed for SLOW_TICKS the first time they touch it.
 *  - The first contact with the cloud also gives the enemy 1 Contamination stack (passive).
 *
 * Mana (35) and cooldown (240 ticks = 12 s) stay in the MCreator procedure AlchemistSkill1.
 * This class detects the cast on the server (the SKILL1 cooldown jumps up), the same
 * pattern used by AssassinShadowStrike and MonkPunchBarrage.
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class AlchemistVolatileBrew {

	// ---- Settings (edit here) ----
	private static final double RADIUS = 4.0D;
	private static final int CLOUD_TICKS = 80;                 // 4 s
	private static final int PULSE_INTERVAL = 10;              // ticks between damage pulses
	private static final double DAMAGE_PER_SECOND_MULT = 1.5D; // x ATT per second (about ATT x 6 over the whole cloud)
	private static final int SLOW_TICKS = 80;                  // 4 s
	private static final int SLOW_LEVEL = 1;                   // 1 = Slowness II
	private static final float THROW_SPEED = 0.8F;
	private static final int THROW_EXPIRE_TICKS = 200;         // forgets a potion that never landed
	private static final int POTION_COLOR = 0x6BCB3A;
	private static final boolean EFFECTS_ENABLED = true;
	private static final boolean SOUNDS_ENABLED = true;

	private static final DustParticleOptions TOXIC = new DustParticleOptions(new Vector3f(0.45F, 0.85F, 0.20F), 1.5F);

	private record Thrown(UUID owner, int expires) {
	}

	private static class Cloud {
		final ResourceKey<Level> dim;
		final UUID owner;
		final double x, y, z;
		final double att;                       // ATT of the alchemist when the potion landed
		final Set<UUID> touched = new HashSet<>();
		int age = 0;

		Cloud(ResourceKey<Level> dim, UUID owner, double x, double y, double z, double att) {
			this.dim = dim;
			this.owner = owner;
			this.x = x;
			this.y = y;
			this.z = z;
			this.att = att;
		}
	}

	private static final Map<UUID, Double> PREV_CD = new HashMap<>();
	private static final Map<UUID, Thrown> THROWN = new HashMap<>(); // potion entity -> thrower
	private static final List<Cloud> CLOUDS = new ArrayList<>();

	// ---------------------------------------------------------------
	//  Cast detection (the MCreator procedure sets mana and cooldown)
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		double cd = v.SKILL1_CD;
		Double prev = PREV_CD.put(player.getUUID(), cd);
		if (prev == null)
			return;

		if ("alchemist".equals(v.PLAYER_CLASS) && player.isAlive() && cd - prev > 50.0D && cd >= v.SKILL1_MAX_CD - 10.0D)
			cast(player);
	}

	private static void cast(ServerPlayer player) {
		ServerLevel level = player.serverLevel();

		ThrownPotion potion = new ThrownPotion(level, player);
		ItemStack stack = new ItemStack(Items.SPLASH_POTION);
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(POTION_COLOR), List.of()));
		potion.setItem(stack);
		potion.shootFromRotation(player, player.getXRot() - 10.0F, player.getYRot(), 0.0F, THROW_SPEED, 1.0F);
		THROWN.put(potion.getUUID(), new Thrown(player.getUUID(), player.getServer().getTickCount() + THROW_EXPIRE_TICKS));
		level.addFreshEntity(potion);

		if (SOUNDS_ENABLED)
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.6F, 0.8F);
	}

	// ---------------------------------------------------------------
	//  Impact: replaces the vanilla splash with the toxic cloud
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onImpact(ProjectileImpactEvent event) {
		Projectile projectile = event.getProjectile();
		if (projectile.level().isClientSide() || THROWN.isEmpty())
			return;
		Thrown t = THROWN.remove(projectile.getUUID());
		if (t == null)
			return;

		// we handle the impact ourselves: no vanilla splash, the potion is removed
		event.setCanceled(true);
		projectile.discard();

		ServerLevel level = (ServerLevel) projectile.level();
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(t.owner());
		if (owner == null || !owner.isAlive())
			return;

		Vec3 p = event.getRayTraceResult().getLocation();
		double att = owner.getData(EldervaleModVariables.PLAYER_VARIABLES).ATTACK;
		CLOUDS.add(new Cloud(level.dimension(), owner.getUUID(), p.x, p.y, p.z, att));
		burstFx(level, p);
	}

	// ---------------------------------------------------------------
	//  Server tick: cloud pulses and cleanup of lost potions
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (THROWN.isEmpty() && CLOUDS.isEmpty())
			return;
		MinecraftServer server = event.getServer();
		int now = server.getTickCount();

		if (now % 40 == 0)
			THROWN.values().removeIf(t -> t.expires() < now);

		Iterator<Cloud> it = CLOUDS.iterator();
		while (it.hasNext()) {
			Cloud c = it.next();
			ServerLevel level = server.getLevel(c.dim);
			ServerPlayer owner = server.getPlayerList().getPlayer(c.owner);
			if (level == null || owner == null || c.age >= CLOUD_TICKS) {
				it.remove();
				continue;
			}
			if (c.age % 4 == 0)
				cloudFx(level, c);
			if (c.age % PULSE_INTERVAL == 0)
				pulse(level, owner, c);
			c.age++;
		}
	}

	private static void pulse(ServerLevel level, ServerPlayer owner, Cloud c) {
		AABB box = new AABB(c.x - RADIUS, c.y - 1.0D, c.z - RADIUS, c.x + RADIUS, c.y + 3.0D, c.z + RADIUS);
		float damage = (float) (c.att * DAMAGE_PER_SECOND_MULT * PULSE_INTERVAL / 20.0D);

		List<LivingEntity> inside = level.getEntitiesOfClass(LivingEntity.class, box, e -> e instanceof Enemy && e.isAlive() && inRadius(e, c));
		for (LivingEntity e : inside) {
			if (c.touched.add(e.getUUID())) {
				e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, SLOW_LEVEL, false, true));
				AlchemistContamination.apply(owner, e, c.att);
			}
			AlchemistContamination.damage(owner, e, damage, true);
		}
	}

	private static boolean inRadius(LivingEntity e, Cloud c) {
		double dx = e.getX() - c.x;
		double dz = e.getZ() - c.z;
		return dx * dx + dz * dz <= RADIUS * RADIUS;
	}

	// ---------------------------------------------------------------
	//  Effects and sounds (sent by the server, everyone sees and hears them)
	// ---------------------------------------------------------------
	private static void burstFx(ServerLevel level, Vec3 p) {
		if (EFFECTS_ENABLED) {
			level.sendParticles(ParticleTypes.POOF, p.x, p.y + 0.4D, p.z, 25, 0.5D, 0.3D, 0.5D, 0.1D);
			level.sendParticles(ParticleTypes.SNEEZE, p.x, p.y + 0.4D, p.z, 20, 0.6D, 0.3D, 0.6D, 0.08D);
			for (int i = 0; i < 36; i++) {
				double a = i * (Math.PI * 2.0D / 36.0D);
				level.sendParticles(TOXIC, p.x + Math.cos(a) * RADIUS * 0.8D, p.y + 0.3D, p.z + Math.sin(a) * RADIUS * 0.8D, 2, 0.1D, 0.2D, 0.1D, 0.0D);
			}
		}
		if (SOUNDS_ENABLED) {
			level.playSound(null, p.x, p.y, p.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 0.9F, 0.7F);
			level.playSound(null, p.x, p.y, p.z, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.8F, 0.6F);
		}
	}

	private static void cloudFx(ServerLevel level, Cloud c) {
		if (!EFFECTS_ENABLED)
			return;
		for (int i = 0; i < 6; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2.0D;
			double r = RADIUS * Math.sqrt(level.getRandom().nextDouble());
			level.sendParticles(TOXIC, c.x + Math.cos(a) * r, c.y + 0.3D + level.getRandom().nextDouble() * 0.8D, c.z + Math.sin(a) * r, 1, 0.1D, 0.1D, 0.1D, 0.0D);
		}
		for (int i = 0; i < 2; i++) {
			double a = level.getRandom().nextDouble() * Math.PI * 2.0D;
			double r = RADIUS * Math.sqrt(level.getRandom().nextDouble());
			level.sendParticles(ParticleTypes.SNEEZE, c.x + Math.cos(a) * r, c.y + 0.4D, c.z + Math.sin(a) * r, 0, 0.0D, 0.05D, 0.0D, 1.0D);
		}
		if (SOUNDS_ENABLED && c.age % 20 == 0)
			level.playSound(null, c.x, c.y, c.z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 0.5F, 0.8F);
	}

	// ---------------------------------------------------------------
	//  Cleanup
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		PREV_CD.remove(event.getEntity().getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PREV_CD.clear();
		THROWN.clear();
		CLOUDS.clear();
	}
}
