package net.mcreator.eldervale.gameplay;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.joml.Vector3f;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;

/**
 * Alchemist passive: CONTAMINATION.
 *  - Every potion that hits an enemy (Volatile Brew, Reactive Compound) adds 1 stack.
 *  - On the 3rd stack the contamination bursts: ATT x BURST_MULT extra damage, stacks reset.
 *  - Stacks are lost after TIMEOUT_TICKS without a new application.
 *  - Stacks are tracked per (target, alchemist), so two alchemists do not mix their stacks.
 *
 * Also exposes damage(...), the damage helper shared by the Alchemist skills
 * (magic damage credited to the player, so XP and kill credit work, with no knockback option).
 * Runs only on the server (no client classes), so it is safe on a dedicated server.
 */
@EventBusSubscriber
public class AlchemistContamination {

	// ---- Settings (edit here) ----
	private static final int MAX_STACKS = 3;
	private static final int TIMEOUT_TICKS = 200;       // 10 s without a new application = stacks lost
	private static final double BURST_MULT = 5.0D;      // x ATT
	private static final int DISPLAY_INTERVAL = 10;     // ticks between stack indicators

	private static final DustParticleOptions TOXIC = new DustParticleOptions(new Vector3f(0.45F, 0.85F, 0.20F), 1.0F);

	private record Key(UUID target, UUID owner) {
	}

	private static class Entry {
		final ResourceKey<Level> dim;
		int stacks = 0;
		int timeout = TIMEOUT_TICKS;

		Entry(ResourceKey<Level> dim) {
			this.dim = dim;
		}
	}

	private static final Map<Key, Entry> ENTRIES = new HashMap<>();

	// ---------------------------------------------------------------
	//  API used by the skills
	// ---------------------------------------------------------------

	/** Adds one stack to the target. att = the alchemist's ATT at the moment of the hit. */
	public static void apply(ServerPlayer owner, LivingEntity target, double att) {
		if (!(target.level() instanceof ServerLevel level) || !target.isAlive())
			return;
		Key key = new Key(target.getUUID(), owner.getUUID());
		Entry e = ENTRIES.computeIfAbsent(key, k -> new Entry(level.dimension()));
		e.stacks++;
		e.timeout = TIMEOUT_TICKS;

		if (e.stacks >= MAX_STACKS) {
			ENTRIES.remove(key);
			burst(level, owner, target, att);
		} else {
			level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 0.6F, 0.7F + e.stacks * 0.2F);
			showStacks(level, target, e.stacks);
		}
	}

	/** Magic damage credited to the player (the kill gives XP). noKnockback = keep the target's motion. */
	public static void damage(ServerPlayer owner, LivingEntity target, float amount, boolean noKnockback) {
		if (!target.isAlive())
			return;
		Vec3 motion = target.getDeltaMovement();
		target.invulnerableTime = 0;
		target.hurt(target.level().damageSources().indirectMagic(null, owner), amount);
		if (noKnockback) {
			target.setDeltaMovement(motion);
			target.hurtMarked = false;
		}
	}

	// ---------------------------------------------------------------
	//  Burst
	// ---------------------------------------------------------------
	private static void burst(ServerLevel level, ServerPlayer owner, LivingEntity target, double att) {
		double x = target.getX(), y = target.getY(0.5D), z = target.getZ();
		level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
		level.sendParticles(TOXIC, x, y, z, 40, 0.5D, 0.5D, 0.5D, 0.05D);
		level.sendParticles(ParticleTypes.SNEEZE, x, y, z, 15, 0.4D, 0.4D, 0.4D, 0.08D);
		level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7F, 1.4F);
		level.playSound(null, x, y, z, SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 0.9F, 0.6F);
		damage(owner, target, (float) (att * BURST_MULT), true);
	}

	private static void showStacks(ServerLevel level, LivingEntity target, int stacks) {
		double y = target.getY() + target.getBbHeight() + 0.4D;
		for (int i = 0; i < stacks; i++)
			level.sendParticles(TOXIC, target.getX() + (i - (stacks - 1) / 2.0D) * 0.35D, y, target.getZ(), 2, 0.04D, 0.04D, 0.04D, 0.0D);
	}

	// ---------------------------------------------------------------
	//  Server tick: timeout and stack indicator
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (ENTRIES.isEmpty())
			return;
		MinecraftServer server = event.getServer();
		boolean show = server.getTickCount() % DISPLAY_INTERVAL == 0;

		Iterator<Map.Entry<Key, Entry>> it = ENTRIES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Key, Entry> me = it.next();
			Entry e = me.getValue();
			ServerLevel level = server.getLevel(e.dim);
			if (level == null || --e.timeout <= 0) {
				it.remove();
				continue;
			}
			if (!show)
				continue;
			if (level.getEntity(me.getKey().target()) instanceof LivingEntity t && t.isAlive())
				showStacks(level, t, e.stacks);
			else
				it.remove();
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ENTRIES.clear();
	}
}
