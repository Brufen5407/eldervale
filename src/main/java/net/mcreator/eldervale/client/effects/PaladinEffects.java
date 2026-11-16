package net.mcreator.eldervale.client.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.Holder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Visual and sound effects for the Paladin.
 * Everything runs on the client by watching synced player variables,
 * so every player sees the effects of every paladin.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class PaladinEffects {

	// ---- Settings (edit here) ----
	private static final boolean EFFECTS_ENABLED = true;
	private static final boolean SOUNDS_ENABLED = true;
	private static final boolean USE_HOLY_PARTICLE = true; // extra golden sparkles (see holy() at the bottom)
	private static final boolean SHOW_SACRED_AURA = true;  // passive ring on the ground
	private static final double MAX_DISTANCE = 48.0D;      // only paladins within this range get effects

	private static final double SHIELD_RADIUS = 0.95D;
	private static final int SHIELD_TICKS = 100;           // shield duration (5s), only used for the ending blink

	private static final double ZONE_HALF = 5.0D;          // Holy Zone half-size (matches inflate(10 / 2d))
	private static final int ZONE_WAVE_TICKS = 10;
	private static final int ZONE_RING_TICKS = 40;
	private static final int ZONE_HALO_TICKS = 100;        // allies keep a halo for 5s

	private static final double SACRED_BOX = 3.0D;         // Sacred Zone half-size (matches inflate(6 / 2d))
	private static final int GRACE_TICKS = 20;             // ignore events right after a player appears

	private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.82F, 0.30F), 1.0F);
	private static final DustParticleOptions GOLD_DUST_SMALL = new DustParticleOptions(new Vector3f(1.0F, 0.90F, 0.50F), 0.6F);

	// ---------------------------------------------------------------
	//  State
	// ---------------------------------------------------------------
	private static class PState {
		boolean prevShield;
		double prevCd2;
		int age = 0;
		int shieldAge = -1;
		int monsterCheck = 0;
		boolean monsterNear = false;
	}

	private static class Zone {
		final double x, y, z;
		int age = 0;
		final List<UUID> allies = new ArrayList<>();

		Zone(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
	}

	private static final Map<UUID, PState> STATES = new HashMap<>();
	private static final List<Zone> ZONES = new ArrayList<>();
	private static long tickCount = 0;

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			STATES.clear();
			ZONES.clear();
			return;
		}
		if (!EFFECTS_ENABLED || mc.isPaused())
			return;

		tickCount++;
		RandomSource rnd = level.random;
		double maxSqr = MAX_DISTANCE * MAX_DISTANCE;
		Set<UUID> seen = new HashSet<>();

		for (Player p : level.players()) {
			if (p.distanceToSqr(mc.player) > maxSqr)
				continue;
			EldervaleModVariables.PlayerVariables v = p.getData(EldervaleModVariables.PLAYER_VARIABLES);
			if (!"paladin".equals(v.PLAYER_CLASS))
				continue;

			UUID id = p.getUUID();
			seen.add(id);
			PState s = STATES.computeIfAbsent(id, k -> new PState());
			if (s.age == 0) {
				s.prevShield = v.paladin_shields;
				s.prevCd2 = v.SKILL2_CD;
			}
			boolean grace = s.age < GRACE_TICKS;
			s.age++;

			handleShield(level, rnd, p, v, s, grace);
			handleZoneCast(level, p, v, s, grace);
			handleAura(level, rnd, p, s);
		}

		STATES.keySet().retainAll(seen);
		updateZones(level, rnd);
	}

	// ---------------------------------------------------------------
	//  Holy Shields
	// ---------------------------------------------------------------
	private static void handleShield(ClientLevel level, RandomSource rnd, Player p, EldervaleModVariables.PlayerVariables v, PState s, boolean grace) {
		double px = p.getX(), py = p.getY(), pz = p.getZ();
		boolean on = v.paladin_shields;

		if (on && !s.prevShield) {
			s.shieldAge = 0;
			if (!grace) {
				shieldStartBurst(level, px, py, pz);
				sound(level, px, py, pz, SoundEvents.BEACON_ACTIVATE, 0.7F, 1.5F);
			}
		} else if (!on && s.prevShield) {
			s.shieldAge = -1;
			if (!grace) {
				shieldEndBurst(level, px, py, pz);
				sound(level, px, py, pz, SoundEvents.BEACON_DEACTIVATE, 0.6F, 1.3F);
			}
		}
		s.prevShield = on;
		if (!on)
			return;

		if (s.shieldAge < 0)
			s.shieldAge = 0;
		int age = s.shieldAge++;

		// blink during the last second
		if (age >= SHIELD_TICKS - 20 && (age / 2) % 2 == 1)
			return;

		double base = age * 0.4D;
		double bob = Math.sin(age * 0.15D) * 0.25D;
		for (int k = 0; k < 3; k++) {
			double a = base + k * (Math.PI * 2.0D / 3.0D);
			level.addParticle(GOLD_DUST, px + Math.cos(a) * SHIELD_RADIUS, py + 0.35D + bob, pz + Math.sin(a) * SHIELD_RADIUS, 0, 0, 0);
			double a2 = -base * 0.8D + k * (Math.PI * 2.0D / 3.0D) + Math.PI / 3.0D;
			level.addParticle(GOLD_DUST, px + Math.cos(a2) * SHIELD_RADIUS, py + 1.4D - bob, pz + Math.sin(a2) * SHIELD_RADIUS, 0, 0, 0);
		}
		if (age % 2 == 0) {
			double th = rnd.nextDouble() * Math.PI * 2.0D;
			double ph = Math.acos(2.0D * rnd.nextDouble() - 1.0D);
			double r = SHIELD_RADIUS + 0.2D;
			level.addParticle(ParticleTypes.END_ROD, px + r * Math.sin(ph) * Math.cos(th), py + 0.9D + r * Math.cos(ph) * 0.9D, pz + r * Math.sin(ph) * Math.sin(th), 0, 0.02D, 0);
		}
	}

	private static void shieldStartBurst(ClientLevel level, double x, double y, double z) {
		for (int i = 0; i < 32; i++) {
			double a = i * (Math.PI * 2.0D / 32.0D);
			level.addParticle(ParticleTypes.END_ROD, x + Math.cos(a) * 0.6D, y + 0.1D, z + Math.sin(a) * 0.6D, Math.cos(a) * 0.18D, 0.04D, Math.sin(a) * 0.18D);
		}
		for (int i = 0; i < 10; i++) {
			double yy = y + 0.2D + i * 0.18D;
			level.addParticle(ParticleTypes.END_ROD, x, yy, z, 0, 0.05D, 0);
			if (USE_HOLY_PARTICLE)
				level.addParticle(holy(), x, yy, z, 0, 0.08D, 0);
		}
	}

	private static void shieldEndBurst(ClientLevel level, double x, double y, double z) {
		for (int i = 0; i < 16; i++) {
			double a = i * (Math.PI * 2.0D / 16.0D);
			level.addParticle(ParticleTypes.END_ROD, x + Math.cos(a) * 0.8D, y + 0.9D, z + Math.sin(a) * 0.8D, Math.cos(a) * 0.1D, 0.03D, Math.sin(a) * 0.1D);
		}
	}

	// ---------------------------------------------------------------
	//  Holy Zone (detected when SKILL2_CD jumps up to its max)
	// ---------------------------------------------------------------
	private static void handleZoneCast(ClientLevel level, Player p, EldervaleModVariables.PlayerVariables v, PState s, boolean grace) {
		double cd = v.SKILL2_CD;
		boolean cast = cd - s.prevCd2 > 50.0D && cd >= v.SKILL2_MAX_CD - 10.0D;
		s.prevCd2 = cd;
		if (!cast || grace)
			return;

		Zone z = new Zone(p.getX(), p.getY(), p.getZ());
		for (Player o : level.players()) {
			if (Math.abs(o.getX() - z.x) <= ZONE_HALF && Math.abs(o.getY() - z.y) <= ZONE_HALF && Math.abs(o.getZ() - z.z) <= ZONE_HALF)
				z.allies.add(o.getUUID());
		}
		ZONES.add(z);
		sound(level, z.x, z.y, z.z, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.2F);
		sound(level, z.x, z.y, z.z, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F, 1.0F);
	}

	private static void updateZones(ClientLevel level, RandomSource rnd) {
		Iterator<Zone> it = ZONES.iterator();
		while (it.hasNext()) {
			Zone z = it.next();
			z.age++;
			int age = z.age;

			// expanding wave
			if (age <= ZONE_WAVE_TICKS) {
				double r = ZONE_HALF * age / ZONE_WAVE_TICKS;
				int n = 18 + (int) (r * 6);
				double rot = age * 0.3D;
				for (int i = 0; i < n; i++) {
					double a = rot + i * (Math.PI * 2.0D / n);
					level.addParticle(ParticleTypes.END_ROD, z.x + Math.cos(a) * r, z.y + 0.1D, z.z + Math.sin(a) * r, 0, 0.01D, 0);
				}
			}

			// light column in the center
			if (age <= 12) {
				double yy = z.y + age * 0.25D;
				level.addParticle(ParticleTypes.END_ROD, z.x, yy, z.z, 0, 0.05D, 0);
				if (USE_HOLY_PARTICLE)
					level.addParticle(holy(), z.x + (rnd.nextDouble() - 0.5D) * 0.6D, yy, z.z + (rnd.nextDouble() - 0.5D) * 0.6D, 0, 0.08D, 0);
			}

			// ground ring that lingers
			if (age > ZONE_WAVE_TICKS && age <= ZONE_WAVE_TICKS + ZONE_RING_TICKS && age % 2 == 0) {
				for (int i = 0; i < 14; i++) {
					double a = rnd.nextDouble() * Math.PI * 2.0D;
					level.addParticle(GOLD_DUST, z.x + Math.cos(a) * ZONE_HALF, z.y + 0.08D, z.z + Math.sin(a) * ZONE_HALF, 0, 0, 0);
				}
			}

			// halo on everyone who got the buff
			if (age <= ZONE_HALO_TICKS && age % 3 == 0) {
				for (UUID id : z.allies) {
					Player o = level.getPlayerByUUID(id);
					if (o == null)
						continue;
					double a = rnd.nextDouble() * Math.PI * 2.0D;
					level.addParticle(ParticleTypes.END_ROD, o.getX() + Math.cos(a) * 0.5D, o.getY() + 0.1D + rnd.nextDouble() * 1.6D, o.getZ() + Math.sin(a) * 0.5D, 0, 0.03D, 0);
				}
			}

			if (age > ZONE_HALO_TICKS)
				it.remove();
		}
	}

	// ---------------------------------------------------------------
	//  Sacred Zone aura (passive): ring on the ground while monsters are in range
	// ---------------------------------------------------------------
	private static void handleAura(ClientLevel level, RandomSource rnd, Player p, PState s) {
		if (!SHOW_SACRED_AURA)
			return;
		if (--s.monsterCheck <= 0) {
			s.monsterCheck = 10;
			AABB box = new AABB(p.getX(), p.getY(), p.getZ(), p.getX(), p.getY(), p.getZ()).inflate(SACRED_BOX);
			s.monsterNear = !level.getEntitiesOfClass(Monster.class, box).isEmpty();
		}
		if (!s.monsterNear || tickCount % 3 != 0)
			return;

		for (int i = 0; i < 5; i++) {
			double a = rnd.nextDouble() * Math.PI * 2.0D;
			level.addParticle(GOLD_DUST_SMALL, p.getX() + Math.cos(a) * SACRED_BOX, p.getY() + 0.08D, p.getZ() + Math.sin(a) * SACRED_BOX, 0, 0, 0);
		}
		if (tickCount % 6 == 0) {
			double a = rnd.nextDouble() * Math.PI * 2.0D;
			level.addParticle(ParticleTypes.END_ROD, p.getX() + Math.cos(a) * SACRED_BOX, p.getY() + 0.1D, p.getZ() + Math.sin(a) * SACRED_BOX, 0, 0.03D, 0);
		}
	}

	// ---------------------------------------------------------------
	//  Helpers
	// ---------------------------------------------------------------
	// Extra golden sparkles. To use your own custom particle, replace WAX_ON
	// with: (SimpleParticleType) EldervaleModParticleTypes.YOUR_NAME.get()
	// (check the exact name inside init/EldervaleModParticleTypes.java)
	private static ParticleOptions holy() {
		return ParticleTypes.WAX_ON;
	}

	private static void sound(ClientLevel level, double x, double y, double z, SoundEvent s, float volume, float pitch) {
		if (SOUNDS_ENABLED)
			level.playLocalSound(x, y, z, s, SoundSource.PLAYERS, volume, pitch, false);
	}

	private static void sound(ClientLevel level, double x, double y, double z, Holder<SoundEvent> s, float volume, float pitch) {
		sound(level, x, y, z, s.value(), volume, pitch);
	}
}
