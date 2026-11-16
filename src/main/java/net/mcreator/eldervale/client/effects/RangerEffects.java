package net.mcreator.eldervale.client.effects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joml.Vector3f;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.Holder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.network.EldervaleModVariables;
import net.mcreator.eldervale.init.EldervaleModEntities;

/**
 * Visual and sound effects for the Ranger.
 * Everything runs on the client by watching synced variables and entities,
 * so on a server every player sees and hears every ranger.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class RangerEffects {

	// ---- Settings (edit here) ----
	private static final boolean EFFECTS_ENABLED = true;
	private static final boolean SOUNDS_ENABLED = true;
	private static final boolean INVISIBLE_SHIMMER = true;  // faint green shimmer while Decoy invisibility is active
	private static final boolean TRACK_MANNEQUIN = true;    // aura + puffs on the decoy dummy
	private static final boolean TRACK_ARROWS = true;       // trail + impact on the magic arrows
	private static final double MAX_DISTANCE = 48.0D;       // only effects within this range

	private static final int RUNE_TICKS = 14;               // Magic Arrows rune circle duration
	private static final int LEAP_TICKS = 10;               // trail while jumping backward
	private static final int REFLEX_TICKS = 60;             // wind trail during Crazy Reflex speed (3s)
	private static final int GRACE_TICKS = 20;              // ignore events right after a player appears

	private static final DustParticleOptions GREEN = new DustParticleOptions(new Vector3f(0.25F, 0.90F, 0.45F), 1.0F);
	private static final DustParticleOptions LIGHT_GREEN = new DustParticleOptions(new Vector3f(0.65F, 1.00F, 0.60F), 0.7F);
	private static final DustParticleOptions CYAN = new DustParticleOptions(new Vector3f(0.30F, 1.00F, 0.85F), 0.9F);

	// ---------------------------------------------------------------
	//  State
	// ---------------------------------------------------------------
	private static class RState {
		int age = 0;
		double prevCd1;
		double prevCd2;
		boolean prevReflex;
		boolean prevInvis;
		int leapTicks = 0;
		int runeTicks = 0;
		double runeX, runeY, runeZ;
		int reflexTicks = 0;
		double lastX, lastZ;
	}

	private static final Map<UUID, RState> STATES = new HashMap<>();
	private static final Map<Integer, double[]> MANNEQUINS = new HashMap<>(); // x, y, z, age
	private static final Map<Integer, double[]> ARROWS = new HashMap<>();     // x, y, z, vx, vy, vz, hit
	private static long tickCount = 0;

	// ---------------------------------------------------------------
	//  Main tick
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			STATES.clear();
			MANNEQUINS.clear();
			ARROWS.clear();
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
			if (!"ranger".equals(v.PLAYER_CLASS))
				continue;

			UUID id = p.getUUID();
			seen.add(id);
			RState s = STATES.computeIfAbsent(id, k -> new RState());
			if (s.age == 0) {
				s.prevCd1 = v.SKILL1_CD;
				s.prevCd2 = v.SKILL2_CD;
				s.prevReflex = v.ranger_hitcooldown;
				s.prevInvis = p.isInvisible();
				s.lastX = p.getX();
				s.lastZ = p.getZ();
			}
			boolean grace = s.age < GRACE_TICKS;
			s.age++;
			boolean local = p == mc.player;

			handleDecoy(level, rnd, p, v, s, grace);
			handleArrows(level, rnd, p, v, s, grace);
			handleReflex(level, rnd, p, v, s, grace, local);
		}
		STATES.keySet().retainAll(seen);

		if (TRACK_MANNEQUIN)
			trackMannequins(level, rnd, mc);
		if (TRACK_ARROWS)
			trackArrows(level, rnd, mc);
	}

	// ---------------------------------------------------------------
	//  Decoy (skill 1): smoke-bomb vanish, green shimmer while invisible
	// ---------------------------------------------------------------
	private static void handleDecoy(ClientLevel level, RandomSource rnd, Player p, EldervaleModVariables.PlayerVariables v, RState s, boolean grace) {
		double cd = v.SKILL1_CD;
		boolean cast = cd - s.prevCd1 > 50.0D && cd >= v.SKILL1_MAX_CD - 10.0D;
		s.prevCd1 = cd;
		double x = p.getX(), y = p.getY(), z = p.getZ();

		if (cast && !grace) {
			for (int i = 0; i < 14; i++) {
				double a = i * (Math.PI * 2.0D / 14.0D);
				level.addParticle(ParticleTypes.POOF, x + Math.cos(a) * 0.3D, y + 0.2D + rnd.nextDouble() * 1.2D, z + Math.sin(a) * 0.3D, Math.cos(a) * 0.12D, 0.03D, Math.sin(a) * 0.12D);
			}
			for (int i = 0; i < 8; i++) {
				double a = rnd.nextDouble() * Math.PI * 2.0D;
				level.addParticle(ParticleTypes.CLOUD, x, y + 0.3D + rnd.nextDouble(), z, Math.cos(a) * 0.08D, 0.02D, Math.sin(a) * 0.08D);
			}
			for (int i = 0; i < 20; i++) {
				double a = rnd.nextDouble() * Math.PI * 2.0D;
				double r = rnd.nextDouble() * 1.2D;
				level.addParticle(GREEN, x + Math.cos(a) * r, y + 0.1D + rnd.nextDouble() * 1.5D, z + Math.sin(a) * r, 0, 0, 0);
			}
			leaves(level, rnd, x, y, z, 22);
			sound(level, x, y, z, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.2F);
			sound(level, x, y, z, SoundEvents.GRASS_BREAK, 0.7F, 0.8F);
		}

		boolean invis = p.isInvisible();
		if (invis && INVISIBLE_SHIMMER) {
			level.addParticle(LIGHT_GREEN, x + off(rnd, 0.3D), y + rnd.nextDouble() * 1.8D, z + off(rnd, 0.3D), 0, 0.01D, 0);
			if (tickCount % 4 == 0)
				level.addParticle(ParticleTypes.COMPOSTER, x + off(rnd, 0.3D), y + 0.2D + rnd.nextDouble() * 1.5D, z + off(rnd, 0.3D), 0, 0.02D, 0);
		}
		if (!invis && s.prevInvis && !grace) {
			for (int i = 0; i < 10; i++)
				level.addParticle(ParticleTypes.COMPOSTER, x + off(rnd, 0.4D), y + 0.2D + rnd.nextDouble() * 1.6D, z + off(rnd, 0.4D), 0, 0.04D, 0);
			sound(level, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.5F);
		}
		s.prevInvis = invis;
	}

	// ---------------------------------------------------------------
	//  Magic Arrows (skill 2): rune circle + backward leap trail
	// ---------------------------------------------------------------
	private static void handleArrows(ClientLevel level, RandomSource rnd, Player p, EldervaleModVariables.PlayerVariables v, RState s, boolean grace) {
		double cd = v.SKILL2_CD;
		boolean cast = cd - s.prevCd2 > 50.0D && cd >= v.SKILL2_MAX_CD - 10.0D;
		s.prevCd2 = cd;
		double x = p.getX(), y = p.getY(), z = p.getZ();

		if (cast && !grace) {
			s.runeTicks = RUNE_TICKS;
			s.runeX = x;
			s.runeY = y;
			s.runeZ = z;
			s.leapTicks = LEAP_TICKS;
			sound(level, x, y, z, SoundEvents.CROSSBOW_SHOOT, 0.6F, 1.3F);
			sound(level, x, y, z, SoundEvents.ENCHANTMENT_TABLE_USE, 0.5F, 1.5F);
		}

		if (s.runeTicks > 0) {
			int t = RUNE_TICKS - s.runeTicks;
			double rot = t * 0.3D;
			for (int i = 0; i < 10; i++) {
				double a = rot + i * (Math.PI * 2.0D / 10.0D);
				level.addParticle(CYAN, s.runeX + Math.cos(a) * 1.3D, s.runeY + 0.08D, s.runeZ + Math.sin(a) * 1.3D, 0, 0, 0);
			}
			for (int i = 0; i < 6; i++) {
				double a = -rot * 1.3D + i * (Math.PI * 2.0D / 6.0D);
				level.addParticle(GREEN, s.runeX + Math.cos(a) * 0.7D, s.runeY + 0.08D, s.runeZ + Math.sin(a) * 0.7D, 0, 0, 0);
			}
			if (t % 2 == 0) {
				double a = rnd.nextDouble() * Math.PI * 2.0D;
				level.addParticle(ParticleTypes.END_ROD, s.runeX + Math.cos(a) * 1.3D, s.runeY + 0.1D, s.runeZ + Math.sin(a) * 1.3D, 0, 0.06D, 0);
			}
			s.runeTicks--;
		}

		if (s.leapTicks > 0) {
			for (int i = 0; i < 2; i++)
				level.addParticle(ParticleTypes.CLOUD, x + off(rnd, 0.3D), y + 0.1D + rnd.nextDouble() * 0.6D, z + off(rnd, 0.3D), 0, 0, 0);
			for (int i = 0; i < 3; i++)
				level.addParticle(LIGHT_GREEN, x + off(rnd, 0.3D), y + 0.2D + rnd.nextDouble() * 1.3D, z + off(rnd, 0.3D), 0, 0, 0);
			leaves(level, rnd, x, y, z, 2);
			s.leapTicks--;
		}
	}

	// ---------------------------------------------------------------
	//  Crazy Reflex (passive): burst when it triggers, wind trail while fast
	// ---------------------------------------------------------------
	private static void handleReflex(ClientLevel level, RandomSource rnd, Player p, EldervaleModVariables.PlayerVariables v, RState s, boolean grace, boolean local) {
		boolean on = v.ranger_hitcooldown;
		double x = p.getX(), y = p.getY(), z = p.getZ();

		if (on && !s.prevReflex && !grace) {
			s.reflexTicks = REFLEX_TICKS;
			for (int i = 0; i < 12; i++) {
				double a = i * (Math.PI * 2.0D / 12.0D);
				level.addParticle(ParticleTypes.CLOUD, x + Math.cos(a) * 0.4D, y + 0.2D, z + Math.sin(a) * 0.4D, Math.cos(a) * 0.15D, 0.02D, Math.sin(a) * 0.15D);
			}
			for (int i = 0; i < 10; i++)
				level.addParticle(GREEN, x + off(rnd, 0.5D), y + 0.2D + rnd.nextDouble() * 1.5D, z + off(rnd, 0.5D), 0, 0, 0);
			for (int i = 0; i < 6; i++)
				level.addParticle(ParticleTypes.COMPOSTER, x + off(rnd, 0.4D), y + 0.3D + rnd.nextDouble() * 1.4D, z + off(rnd, 0.4D), 0, 0.03D, 0);
			sound(level, x, y, z, SoundEvents.ENDER_EYE_LAUNCH, 0.5F, 1.6F);
			sound(level, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, 0.4F, 2.0F);
		} else if (!on && s.prevReflex && !grace && local) {
			// passive is ready again
			for (int i = 0; i < 8; i++) {
				double a = i * (Math.PI * 2.0D / 8.0D);
				level.addParticle(ParticleTypes.COMPOSTER, x + Math.cos(a) * 0.6D, y + 0.1D, z + Math.sin(a) * 0.6D, 0, 0.05D, 0);
			}
			sound(level, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, 0.3F, 1.8F);
		}
		s.prevReflex = on;

		double dx = x - s.lastX;
		double dz = z - s.lastZ;
		s.lastX = x;
		s.lastZ = z;
		double speed = Math.sqrt(dx * dx + dz * dz);

		if (s.reflexTicks > 0) {
			s.reflexTicks--;
			if (speed > 0.08D) {
				for (int i = 0; i < 2; i++)
					level.addParticle(LIGHT_GREEN, x + off(rnd, 0.3D), y + 0.2D + rnd.nextDouble() * 1.2D, z + off(rnd, 0.3D), -dx * 0.5D, 0, -dz * 0.5D);
				level.addParticle(ParticleTypes.CLOUD, x + off(rnd, 0.2D), y + 0.1D, z + off(rnd, 0.2D), -dx * 0.3D, 0.01D, -dz * 0.3D);
				if (rnd.nextInt(3) == 0)
					level.addParticle(ParticleTypes.COMPOSTER, x + off(rnd, 0.3D), y + 0.2D + rnd.nextDouble() * 1.2D, z + off(rnd, 0.3D), 0, 0.02D, 0);
			}
		}
	}

	// ---------------------------------------------------------------
	//  Decoy dummy: aura while it exists, puffs when it appears/disappears
	// ---------------------------------------------------------------
	private static void trackMannequins(ClientLevel level, RandomSource rnd, Minecraft mc) {
		EntityType<?> type = EldervaleModEntities.MANICHINO_RANGER.get();
		double maxSqr = MAX_DISTANCE * MAX_DISTANCE;
		Set<Integer> seen = new HashSet<>();

		for (Entity e : level.entitiesForRendering()) {
			if (e.getType() != type || e.distanceToSqr(mc.player) > maxSqr)
				continue;
			int id = e.getId();
			seen.add(id);
			double x = e.getX(), y = e.getY(), z = e.getZ();
			double[] st = MANNEQUINS.get(id);
			if (st == null) {
				MANNEQUINS.put(id, new double[]{x, y, z, 0});
				// appears: green sparks rising
				for (int i = 0; i < 16; i++)
					level.addParticle(ParticleTypes.COMPOSTER, x + off(rnd, 0.5D), y + rnd.nextDouble() * 1.8D, z + off(rnd, 0.5D), 0, 0.05D, 0);
				for (int i = 0; i < 12; i++) {
					double a = i * (Math.PI * 2.0D / 12.0D);
					level.addParticle(GREEN, x + Math.cos(a) * 0.8D, y + 0.1D, z + Math.sin(a) * 0.8D, 0, 0, 0);
				}
				continue;
			}
			st[0] = x;
			st[1] = y;
			st[2] = z;
			st[3]++;

			// green ring pulsing around the dummy
			if (tickCount % 3 == 0) {
				double rot = tickCount * 0.2D;
				for (int i = 0; i < 6; i++) {
					double a = rot + i * (Math.PI * 2.0D / 6.0D);
					level.addParticle(LIGHT_GREEN, x + Math.cos(a) * 0.8D, y + 0.1D, z + Math.sin(a) * 0.8D, 0, 0.02D, 0);
				}
				level.addParticle(ParticleTypes.COMPOSTER, x + off(rnd, 0.4D), y + 0.3D + rnd.nextDouble() * 1.5D, z + off(rnd, 0.4D), 0, 0.03D, 0);
			}
		}

		Iterator<Map.Entry<Integer, double[]>> it = MANNEQUINS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, double[]> en = it.next();
			if (seen.contains(en.getKey()))
				continue;
			if (level.getEntity(en.getKey()) == null) {
				double[] st = en.getValue();
				for (int i = 0; i < 10; i++) {
					double a = rnd.nextDouble() * Math.PI * 2.0D;
					level.addParticle(ParticleTypes.POOF, st[0], st[1] + 0.5D + rnd.nextDouble(), st[2], Math.cos(a) * 0.1D, 0.03D, Math.sin(a) * 0.1D);
				}
				leaves(level, rnd, st[0], st[1], st[2], 14);
				sound(level, st[0], st[1], st[2], SoundEvents.GRASS_BREAK, 0.7F, 0.9F);
			}
			it.remove();
		}
	}

	// ---------------------------------------------------------------
	//  Magic arrows: glowing trail while flying, burst when they stop
	// ---------------------------------------------------------------
	private static void trackArrows(ClientLevel level, RandomSource rnd, Minecraft mc) {
		EntityType<?> type = EldervaleModEntities.MAGIC_ARROW.get();
		double maxSqr = MAX_DISTANCE * MAX_DISTANCE;
		Set<Integer> seen = new HashSet<>();

		for (Entity e : level.entitiesForRendering()) {
			if (e.getType() != type || e.distanceToSqr(mc.player) > maxSqr)
				continue;
			int id = e.getId();
			seen.add(id);
			double[] st = ARROWS.computeIfAbsent(id, k -> new double[7]);
			double x = e.getX(), y = e.getY(), z = e.getZ();
			Vec3 mv = e.getDeltaMovement();
			boolean moving = mv.lengthSqr() > 0.01D;

			if (moving) {
				st[3] = mv.x;
				st[4] = mv.y;
				st[5] = mv.z;
				for (int i = 0; i < 2; i++)
					level.addParticle(CYAN, x + off(rnd, 0.06D), y + off(rnd, 0.06D), z + off(rnd, 0.06D), 0, 0, 0);
				if (tickCount % 2 == 0)
					level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, off(rnd, 0.05D), off(rnd, 0.05D), off(rnd, 0.05D));
			} else if (st[6] == 0.0D && (st[3] != 0.0D || st[4] != 0.0D || st[5] != 0.0D)) {
				// it was flying and now it has stopped: it hit something
				st[6] = 1.0D;
				arrowImpact(level, rnd, x, y, z, st[3], st[4], st[5]);
			}
			st[0] = x;
			st[1] = y;
			st[2] = z;
		}

		Iterator<Map.Entry<Integer, double[]>> it = ARROWS.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Integer, double[]> en = it.next();
			if (seen.contains(en.getKey()))
				continue;
			double[] st = en.getValue();
			if (st[6] == 0.0D && level.getEntity(en.getKey()) == null)
				arrowImpact(level, rnd, st[0], st[1], st[2], st[3], st[4], st[5]);
			it.remove();
		}
	}

	private static void arrowImpact(ClientLevel level, RandomSource rnd, double x, double y, double z, double vx, double vy, double vz) {
		Vec3 back = new Vec3(vx, vy, vz);
		double len = back.length();
		double bx = len > 1.0E-4D ? -vx / len : 0.0D;
		double by = len > 1.0E-4D ? -vy / len : 0.2D;
		double bz = len > 1.0E-4D ? -vz / len : 0.0D;
		for (int i = 0; i < 12; i++)
			level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, bx * 0.15D + off(rnd, 0.15D), by * 0.15D + off(rnd, 0.15D), bz * 0.15D + off(rnd, 0.15D));
		for (int i = 0; i < 8; i++)
			level.addParticle(CYAN, x + off(rnd, 0.15D), y + off(rnd, 0.15D), z + off(rnd, 0.15D), 0, 0, 0);
		for (int i = 0; i < 4; i++)
			level.addParticle(ParticleTypes.COMPOSTER, x + off(rnd, 0.2D), y + off(rnd, 0.2D), z + off(rnd, 0.2D), 0, 0.03D, 0);
		sound(level, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, 0.5F, 1.8F);
	}

	// ---------------------------------------------------------------
	//  Helpers
	// ---------------------------------------------------------------
	private static double off(RandomSource rnd, double r) {
		return (rnd.nextDouble() - 0.5D) * 2.0D * r;
	}

	private static void leaves(ClientLevel level, RandomSource rnd, double x, double y, double z, int count) {
		ParticleOptions opt = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState());
		for (int i = 0; i < count; i++) {
			double a = rnd.nextDouble() * Math.PI * 2.0D;
			double r = 0.2D + rnd.nextDouble() * 0.5D;
			level.addParticle(opt, x + Math.cos(a) * r, y + 0.2D + rnd.nextDouble() * 1.4D, z + Math.sin(a) * r, Math.cos(a) * 0.12D, 0.05D + rnd.nextDouble() * 0.08D, Math.sin(a) * 0.12D);
		}
	}

	private static void sound(ClientLevel level, double x, double y, double z, SoundEvent s, float volume, float pitch) {
		if (SOUNDS_ENABLED)
			level.playLocalSound(x, y, z, s, SoundSource.PLAYERS, volume, pitch, false);
	}

	private static void sound(ClientLevel level, double x, double y, double z, Holder<SoundEvent> s, float volume, float pitch) {
		sound(level, x, y, z, s.value(), volume, pitch);
	}
}
