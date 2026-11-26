package net.mcreator.eldervale.gameplay;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;

import net.mcreator.eldervale.vfx.VfxIds;
import net.mcreator.eldervale.vfx.VfxServer;

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
 * Gameplay unchanged: the old particles and sounds are replaced by VFX events sent for the target
 * (START = a stack was added, arg = stack count; IMPACT = the burst).
 */
@EventBusSubscriber
public class AlchemistContamination {

	// ---- Settings (edit here) ----
	private static final int MAX_STACKS = 3;
	private static final int TIMEOUT_TICKS = 200;       // 10 s without a new application = stacks lost
	private static final double BURST_MULT = 5.0D;      // x ATT

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
			// VFX keyed on the target entity: the client shows the stacks above its head
			VfxServer.sendAt(level, target.getId(), VfxIds.ALCHEMIST_CONTAMINATION, VfxIds.START, e.stacks, target.getX(), target.getY(), target.getZ(), target.getBbHeight());
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
		VfxServer.sendAt(level, target.getId(), VfxIds.ALCHEMIST_CONTAMINATION, VfxIds.IMPACT, 0, target.getX(), target.getY(0.5D), target.getZ(), target.getBbHeight());
		damage(owner, target, (float) (att * BURST_MULT), true);
	}

	// ---------------------------------------------------------------
	//  Server tick: timeout
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		if (ENTRIES.isEmpty())
			return;
		MinecraftServer server = event.getServer();

		Iterator<Map.Entry<Key, Entry>> it = ENTRIES.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Key, Entry> me = it.next();
			Entry e = me.getValue();
			ServerLevel level = server.getLevel(e.dim);
			if (level == null || --e.timeout <= 0) {
				it.remove();
				continue;
			}
			if (!(level.getEntity(me.getKey().target()) instanceof LivingEntity t) || !t.isAlive())
				it.remove();
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		ENTRIES.clear();
	}
}
