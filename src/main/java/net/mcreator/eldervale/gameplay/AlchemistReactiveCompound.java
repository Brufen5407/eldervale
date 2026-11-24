package net.mcreator.eldervale.gameplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
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
 * Alchemist skill 2: REACTIVE COMPOUND.
 *  - Throws a substance (vanilla ThrownPotion, orange). If it hits an enemy it sticks to it
 *    (glowing + particles + rising ticking sound) and gives 1 Contamination stack (passive).
 *  - After FUSE_TICKS (3 s) it reacts: explosion of EXPLOSION_RADIUS blocks around the target,
 *    ATT x DAMAGE_MULT to every enemy in it, with a light knockback. No block damage.
 *  - If it hits a block or a non-enemy it is lost (no refund).
 *  - If the target dies before the reaction, the compound is lost.
 *
 * Mana (30) and cooldown (280 ticks = 14 s) stay in the MCreator procedure AlchemistSkill2.
 * This class detects the cast on the server (the SKILL2 cooldown jumps up), the same
 * pattern used by AssassinSmokeBomb and RangerMagicArrows.
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class AlchemistReactiveCompound {

	// ---- Settings (edit here) ----
	private static final int FUSE_TICKS = 60;                  // 3 s
	private static final double EXPLOSION_RADIUS = 3.0D;
	private static final double DAMAGE_MULT = 8.0D;            // x ATT
	private static final double KNOCKBACK = 0.6D;
	private static final float THROW_SPEED = 1.1F;
	private static final int THROW_EXPIRE_TICKS = 200;         // forgets a potion that never landed
	private static final int POTION_COLOR = 0xD9822B;
	private static final boolean GLOW_TARGET = true;
	private static final boolean EFFECTS_ENABLED = true;
	private static final boolean SOUNDS_ENABLED = true;

	private static final DustParticleOptions REACTIVE = new DustParticleOptions(new Vector3f(0.95F, 0.50F, 0.10F), 1.2F);

	private record Thrown(UUID owner, int expires) {
	}

	private static class Attached {
		final ResourceKey<Level> dim;
		final UUID target;
		final UUID owner;
		final double att;                       // ATT of the alchemist when the compound stuck
		int left = FUSE_TICKS;

		Attached(ResourceKey<Level> dim, UUID target, UUID owner, double att) {
			this.dim = dim;
			this.target = target;
			this.owner = owner;
			this.att = att;
		}
	}

	private static final Map<UUID, Double> PREV_CD = new HashMap<>();
	private static final Map<UUID, Thrown> THROWN = new HashMap<>(); // potion entity -> thrower
	private static final List<Attached> ATTACHED = new ArrayList<>();

	// ---------------------------------------------------------------
	//  Cast detection (the MCreator procedure sets mana and cooldown)
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;

		EldervaleModVariables.PlayerVariables v = player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		double cd = v.SKILL2_CD;
		Double prev = PREV_CD.put(player.getUUID(), cd);
		if (prev == null)
			return;

		if ("alchemist".equals(v.PLAYER_CLASS) && player.isAlive() && cd - prev > 50.0D && cd >= v.SKILL2_MAX_CD - 10.0D)
			cast(player);
	}

	private static void cast(ServerPlayer player) {
		ServerLevel level = player.serverLevel();

		ThrownPotion potion = new ThrownPotion(level, player);
		ItemStack stack = new ItemStack(Items.SPLASH_POTION);
		stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(POTION_COLOR), List.of()));
		potion.setItem(stack);
		potion.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, THROW_SPEED, 0.5F);
		THROWN.put(potion.getUUID(), new Thrown(player.getUUID(), player.getServer().getTickCount() + THROW_EXPIRE_TICKS));
		level.addFreshEntity(potion);

		if (SOUNDS_ENABLED)
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW, SoundSource.PLAYERS, 0.6F, 1.1F);
	}

	// ---------------------------------------------------------------
	//  Impact: sticks to the first enemy, otherwise fizzles
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
		HitResult hit = event.getRayTraceResult();
		Vec3 p = hit.getLocation();

		if (owner != null && owner.isAlive() && hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity target && target instanceof Enemy && target.isAlive()) {
			double att = owner.getData(EldervaleModVariables.PLAYER_VARIABLES).ATTACK;
			ATTACHED.add(new Attached(level.dimension(), target.getUUID(), owner.getUUID(), att));
			if (GLOW_TARGET)
				target.addEffect(new MobEffectInstance(MobEffects.GLOWING, FUSE_TICKS, 0, false, false));
			stickFx(level, p);
			AlchemistContamination.apply(owner, target, att);
		} else {
			fizzleFx(level, p);
		}
	}

	// ---------------------------------------------------------------
	//  Server tick: fuse, warning sounds and reaction
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (THROWN.isEmpty() && ATTACHED.isEmpty())
			return;
		MinecraftServer server = event.getServer();
		int now = server.getTickCount();

		if (now % 40 == 0)
			THROWN.values().removeIf(t -> t.expires() < now);

		Iterator<Attached> it = ATTACHED.iterator();
		while (it.hasNext()) {
			Attached a = it.next();
			ServerLevel level = server.getLevel(a.dim);
			ServerPlayer owner = server.getPlayerList().getPlayer(a.owner);
			if (level == null || owner == null || !(level.getEntity(a.target) instanceof LivingEntity target) || !target.isAlive()) {
				it.remove();
				continue;
			}
			a.left--;
			if (a.left <= 0) {
				it.remove();
				explode(level, owner, target, a.att);
				continue;
			}
			fuseFx(level, target, a.left);
		}
	}

	private static void explode(ServerLevel level, ServerPlayer owner, LivingEntity target, double att) {
		Vec3 center = target.position().add(0.0D, target.getBbHeight() / 2.0D, 0.0D);
		float damage = (float) (att * DAMAGE_MULT);

		List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(EXPLOSION_RADIUS),
				e -> e instanceof Enemy && e.isAlive() && e.distanceToSqr(center) <= EXPLOSION_RADIUS * EXPLOSION_RADIUS);
		for (LivingEntity e : hit) {
			AlchemistContamination.damage(owner, e, damage, false);
			if (e.isAlive() && KNOCKBACK > 0.0D) {
				Vec3 d = e.position().subtract(center);
				Vec3 flat = new Vec3(d.x, 0.0D, d.z);
				if (flat.lengthSqr() > 1.0E-4D) {
					flat = flat.normalize().scale(KNOCKBACK);
					e.push(flat.x, 0.3D, flat.z);
					e.hurtMarked = true;
				}
			}
		}
		explosionFx(level, center);
	}

	// ---------------------------------------------------------------
	//  Effects and sounds (sent by the server, everyone sees and hears them)
	// ---------------------------------------------------------------
	private static void stickFx(ServerLevel level, Vec3 p) {
		if (EFFECTS_ENABLED) {
			level.sendParticles(REACTIVE, p.x, p.y, p.z, 20, 0.3D, 0.3D, 0.3D, 0.05D);
			level.sendParticles(ParticleTypes.ITEM_SLIME, p.x, p.y, p.z, 10, 0.3D, 0.3D, 0.3D, 0.05D);
		}
		if (SOUNDS_ENABLED)
			level.playSound(null, p.x, p.y, p.z, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 0.9F, 0.8F);
	}

	private static void fizzleFx(ServerLevel level, Vec3 p) {
		if (EFFECTS_ENABLED)
			level.sendParticles(ParticleTypes.SMOKE, p.x, p.y + 0.2D, p.z, 12, 0.2D, 0.1D, 0.2D, 0.02D);
		if (SOUNDS_ENABLED)
			level.playSound(null, p.x, p.y, p.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 0.5F, 1.2F);
	}

	// particles around the target, and a ticking sound that speeds up as the reaction gets close
	private static void fuseFx(ServerLevel level, LivingEntity target, int left) {
		int interval = left > 40 ? 4 : (left > 20 ? 3 : 2);
		if (EFFECTS_ENABLED && left % interval == 0)
			level.sendParticles(REACTIVE, target.getX(), target.getY(0.6D), target.getZ(), 4, target.getBbWidth() * 0.4D, target.getBbHeight() * 0.3D, target.getBbWidth() * 0.4D, 0.0D);
		if (SOUNDS_ENABLED && (left == 40 || left == 20 || left == 10 || left == 5))
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.7F, 0.8F + (FUSE_TICKS - left) * 0.02F);
	}

	private static void explosionFx(ServerLevel level, Vec3 c) {
		if (EFFECTS_ENABLED) {
			level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 3, 0.8D, 0.5D, 0.8D, 0.0D);
			level.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 30, 0.7D, 0.5D, 0.7D, 0.12D);
			level.sendParticles(REACTIVE, c.x, c.y, c.z, 40, EXPLOSION_RADIUS * 0.4D, 0.5D, EXPLOSION_RADIUS * 0.4D, 0.1D);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y, c.z, 12, 0.6D, 0.4D, 0.6D, 0.05D);
		}
		if (SOUNDS_ENABLED) {
			level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 1.1F);
			level.playSound(null, c.x, c.y, c.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 0.8F);
		}
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
		ATTACHED.clear();
	}
}
