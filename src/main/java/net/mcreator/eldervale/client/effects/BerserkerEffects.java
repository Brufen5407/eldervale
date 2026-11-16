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

import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * Visual, sound and camera effects for the Berserker.
 * Everything runs on the client by watching synced player variables,
 * so on a server every player sees and hears every berserker.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class BerserkerEffects {

	// ---- Settings (edit here) ----
	private static final boolean EFFECTS_ENABLED = true;
	private static final boolean SOUNDS_ENABLED = true;
	private static final boolean HEARTBEAT_SOUND = true;  // heartbeat while Berserk Mode is on
	private static final boolean CAMERA_SHAKE = true;
	private static final boolean DASH_FOV = true;         // FOV kick while charging
	private static final boolean VIGNETTE = true;         // red pulsing screen edges in Berserk Mode
	private static final double MAX_DISTANCE = 48.0D;     // only berserkers within this range get effects

	private static final int SLAM_DELAY = 20;             // ticks between the jump and the impact (matches queueServerWork(20))
	private static final double SLAM_RADIUS = 3.5D;       // matches inflate(7 / 2d)
	private static final double CHARGE_RADIUS = 5.0D;     // matches the knockback radius
	private static final int DASH_TRAIL_TICKS = 12;
	private static final int BEAT_PERIOD = 24;            // ticks between heartbeats
	private static final int GRACE_TICKS = 20;            // ignore events right after a player appears

	private static final DustParticleOptions RED_DUST = new DustParticleOptions(new Vector3f(0.85F, 0.06F, 0.03F), 1.1F);
	private static final DustParticleOptions ORANGE_DUST = new DustParticleOptions(new Vector3f(1.0F, 0.45F, 0.05F), 0.9F);

	// ---------------------------------------------------------------
	//  State
	// ---------------------------------------------------------------
	private static class BState {
		int age = 0;
		boolean prevMode;
		double prevCd1;
		double prevCd2;
		int slamTimer = -1;
		int dashTicks = 0;
	}

	private static class Wave {
		final double x, y, z;
		final int type; // 0 = fire shockwave (slam), 1 = air shockwave (charge)
		final int total;
		final double max;
		int age = 0;

		Wave(double x, double y, double z, int type, int total, double max) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.type = type;
			this.total = total;
			this.max = max;
		}
	}

	private static final Map<UUID, BState> STATES = new HashMap<>();
	private static final List<Wave> WAVES = new ArrayList<>();
	private static long tickCount = 0;

	private static int shakeTicks = 0;
	private static int shakeTotal = 1;
	private static float shakePower = 0.0F;
	private static int dashFovTicks = 0;
	private static boolean localMode = false;

	// ---------------------------------------------------------------
	//  Main tick
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			STATES.clear();
			WAVES.clear();
			shakeTicks = 0;
			dashFovTicks = 0;
			localMode = false;
			return;
		}
		if (mc.isPaused())
			return;

		tickCount++;
		if (shakeTicks > 0)
			shakeTicks--;
		if (dashFovTicks > 0)
			dashFovTicks--;

		EldervaleModVariables.PlayerVariables lv = mc.player.getData(EldervaleModVariables.PLAYER_VARIABLES);
		localMode = EFFECTS_ENABLED && VIGNETTE && "berserker".equals(lv.PLAYER_CLASS) && lv.berserk_mode;

		if (!EFFECTS_ENABLED)
			return;

		RandomSource rnd = level.random;
		double maxSqr = MAX_DISTANCE * MAX_DISTANCE;
		Set<UUID> seen = new HashSet<>();

		for (Player p : level.players()) {
			if (p.distanceToSqr(mc.player) > maxSqr)
				continue;
			EldervaleModVariables.PlayerVariables v = p.getData(EldervaleModVariables.PLAYER_VARIABLES);
			if (!"berserker".equals(v.PLAYER_CLASS))
				continue;

			UUID id = p.getUUID();
			seen.add(id);
			BState s = STATES.computeIfAbsent(id, k -> new BState());
			if (s.age == 0) {
				s.prevMode = v.berserk_mode;
				s.prevCd1 = v.SKILL1_CD;
				s.prevCd2 = v.SKILL2_CD;
			}
			boolean grace = s.age < GRACE_TICKS;
			s.age++;
			boolean local = p == mc.player;

			handleMode(level, rnd, p, v, s, grace, local);
			handleSlam(level, rnd, p, v, s, grace, local);
			handleCharge(level, rnd, p, v, s, grace, local);
		}

		STATES.keySet().retainAll(seen);
		updateWaves(level, rnd);
	}

	// ---------------------------------------------------------------
	//  Berserk Mode (passive)
	// ---------------------------------------------------------------
	private static void handleMode(ClientLevel level, RandomSource rnd, Player p, EldervaleModVariables.PlayerVariables v, BState s, boolean grace, boolean local) {
		boolean mode = v.berserk_mode;
		double x = p.getX(), y = p.getY(), z = p.getZ();

		if (mode && !s.prevMode) {
			if (!grace) {
				for (int i = 0; i < 24; i++) {
					double a = i * (Math.PI * 2.0D / 24.0D);
					level.addParticle(ParticleTypes.FLAME, x + Math.cos(a) * 0.7D, y + 0.1D, z + Math.sin(a) * 0.7D, Math.cos(a) * 0.15D, 0.05D, Math.sin(a) * 0.15D);
				}
				for (int i = 0; i < 6; i++)
					level.addParticle(ParticleTypes.LAVA, x + off(rnd, 0.5D), y + 0.5D + rnd.nextDouble(), z + off(rnd, 0.5D), 0, 0, 0);
				for (int i = 0; i < 6; i++)
					level.addParticle(ParticleTypes.LARGE_SMOKE, x + off(rnd, 0.4D), y + 0.3D, z + off(rnd, 0.4D), 0, 0.05D, 0);
				for (int i = 0; i < 3; i++)
					level.addParticle(ParticleTypes.ANGRY_VILLAGER, x + off(rnd, 0.3D), y + 2.2D, z + off(rnd, 0.3D), 0, 0, 0);
				sound(level, x, y, z, SoundEvents.RAVAGER_ROAR, 1.0F, 0.8F);
				if (local)
					shake(0.5F, 12);
			}
		} else if (!mode && s.prevMode) {
			if (!grace) {
				for (int i = 0; i < 12; i++) {
					double a = rnd.nextDouble() * Math.PI * 2.0D;
					level.addParticle(ParticleTypes.SMOKE, x + Math.cos(a) * 0.4D, y + 0.2D + rnd.nextDouble() * 1.5D, z + Math.sin(a) * 0.4D, Math.cos(a) * 0.05D, 0.04D, Math.sin(a) * 0.05D);
				}
				sound(level, x, y, z, SoundEvents.FIRE_EXTINGUISH, 0.5F, 0.8F);
			}
		}
		s.prevMode = mode;
		if (!mode)
			return;

		// aura
		for (int i = 0; i < 2; i++) {
			double a = rnd.nextDouble() * Math.PI * 2.0D;
			double r = 0.45D + rnd.nextDouble() * 0.2D;
			level.addParticle(RED_DUST, x + Math.cos(a) * r, y + rnd.nextDouble() * 1.9D, z + Math.sin(a) * r, 0, 0.02D, 0);
		}
		if (tickCount % 2 == 0) {
			double a = rnd.nextDouble() * Math.PI * 2.0D;
			level.addParticle(ParticleTypes.FLAME, x + Math.cos(a) * 0.4D, y + 0.2D + rnd.nextDouble() * 1.5D, z + Math.sin(a) * 0.4D, 0, 0.03D, 0);
		}
		if (tickCount % 3 == 0)
			level.addParticle(ParticleTypes.SMOKE, x + off(rnd, 0.3D), y + 1.9D, z + off(rnd, 0.3D), 0, 0.03D, 0);
		if (rnd.nextInt(30) == 0)
			level.addParticle(ParticleTypes.LAVA, x + off(rnd, 0.4D), y + 0.5D + rnd.nextDouble(), z + off(rnd, 0.4D), 0, 0, 0);

		// heartbeat (lub-dub)
		int beat = (int) ((tickCount + p.getId()) % BEAT_PERIOD);
		if (beat == 0 || beat == 6) {
			for (int i = 0; i < 14; i++) {
				double a = i * (Math.PI * 2.0D / 14.0D);
				level.addParticle(RED_DUST, x + Math.cos(a) * 0.9D, y + 0.1D, z + Math.sin(a) * 0.9D, 0, 0, 0);
			}
			if (beat == 0)
				level.addParticle(ParticleTypes.ANGRY_VILLAGER, x, y + 2.2D, z, 0, 0, 0);
			if (HEARTBEAT_SOUND)
				sound(level, x, y, z, SoundEvents.WARDEN_HEARTBEAT, beat == 0 ? 0.6F : 0.4F, beat == 0 ? 0.9F : 0.75F);
		}
	}

	// ---------------------------------------------------------------
	//  Berserk Slam (skill 1): jump, then impact SLAM_DELAY ticks later
	// ---------------------------------------------------------------
	private static void handleSlam(ClientLevel level, RandomSource rnd, Player p, EldervaleModVariables.PlayerVariables v, BState s, boolean grace, boolean local) {
		double cd = v.SKILL1_CD;
		boolean cast = cd - s.prevCd1 > 50.0D && cd >= v.SKILL1_MAX_CD - 10.0D;
		s.prevCd1 = cd;
		if (cast && !grace)
			s.slamTimer = 0;
		if (s.slamTimer < 0)
			return;

		int t = s.slamTimer++;
		double x = p.getX(), y = p.getY(), z = p.getZ();

		if (t == 0) {
			for (int i = 0; i < 14; i++) {
				double a = i * (Math.PI * 2.0D / 14.0D);
				level.addParticle(ParticleTypes.CLOUD, x + Math.cos(a) * 0.5D, y + 0.1D, z + Math.sin(a) * 0.5D, Math.cos(a) * 0.12D, 0.03D, Math.sin(a) * 0.12D);
			}
			for (int i = 0; i < 6; i++)
				level.addParticle(ParticleTypes.FLAME, x + off(rnd, 0.3D), y + 0.1D, z + off(rnd, 0.3D), off(rnd, 0.05D), 0.1D, off(rnd, 0.05D));
			debris(level, rnd, x, y, z, 10, 0.6D, 0.3D);
			sound(level, x, y, z, SoundEvents.RAVAGER_ROAR, 0.8F, 1.2F);
			sound(level, x, y, z, SoundEvents.FIRECHARGE_USE, 0.7F, 0.7F);
			if (local)
				shake(0.35F, 8);
		} else if (t < SLAM_DELAY) {
			// fiery jump trail
			for (int i = 0; i < 3; i++)
				level.addParticle(ParticleTypes.FLAME, x + off(rnd, 0.3D), y + 0.1D, z + off(rnd, 0.3D), 0, -0.02D, 0);
			level.addParticle(ParticleTypes.LARGE_SMOKE, x + off(rnd, 0.2D), y + 0.2D, z + off(rnd, 0.2D), 0, 0, 0);
			if (t % 2 == 0)
				level.addParticle(ORANGE_DUST, x + off(rnd, 0.3D), y + 0.1D, z + off(rnd, 0.3D), 0, 0, 0);
		} else {
			impact(level, rnd, x, y, z);
			s.slamTimer = -1;
		}
	}

	private static void impact(ClientLevel level, RandomSource rnd, double x, double y, double z) {
		WAVES.add(new Wave(x, y, z, 0, 6, SLAM_RADIUS));
		level.addParticle(ParticleTypes.EXPLOSION, x, y + 0.3D, z, 0, 0, 0);

		for (int i = 0; i < 8; i++) {
			double a = rnd.nextDouble() * Math.PI * 2.0D;
			double r = rnd.nextDouble() * SLAM_RADIUS * 0.7D;
			level.addParticle(ParticleTypes.LAVA, x + Math.cos(a) * r, y + 0.2D, z + Math.sin(a) * r, 0, 0, 0);
		}
		for (int i = 0; i < 24; i++) {
			double a = rnd.nextDouble() * Math.PI * 2.0D;
			double sp = 0.25D + rnd.nextDouble() * 0.35D;
			level.addParticle(ParticleTypes.CRIT, x, y + 0.3D, z, Math.cos(a) * sp, 0.15D + rnd.nextDouble() * 0.3D, Math.sin(a) * sp);
		}
		for (int i = 0; i < 10; i++) {
			double a = rnd.nextDouble() * Math.PI * 2.0D;
			level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.2D, z, Math.cos(a) * 0.1D, 0.04D, Math.sin(a) * 0.1D);
		}
		debris(level, rnd, x, y, z, 24, SLAM_RADIUS * 0.6D, 0.35D);

		sound(level, x, y, z, SoundEvents.GENERIC_EXPLODE, 0.5F, 0.9F);
		sound(level, x, y, z, SoundEvents.ANVIL_LAND, 0.7F, 0.5F);
		sound(level, x, y, z, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.6F);

		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			double d = Math.sqrt(mc.player.distanceToSqr(x, y, z));
			if (d < 24.0D)
				shake((float) (1.0D - d / 24.0D), 12);
		}
	}

	// ---------------------------------------------------------------
	//  Berserk Charge (skill 2): dash + shockwave at the starting point
	// ---------------------------------------------------------------
	private static void handleCharge(ClientLevel level, RandomSource rnd, Player p, EldervaleModVariables.PlayerVariables v, BState s, boolean grace, boolean local) {
		double cd = v.SKILL2_CD;
		boolean cast = cd - s.prevCd2 > 50.0D && cd >= v.SKILL2_MAX_CD - 10.0D;
		s.prevCd2 = cd;
		double x = p.getX(), y = p.getY(), z = p.getZ();

		if (cast && !grace) {
			s.dashTicks = DASH_TRAIL_TICKS;
			WAVES.add(new Wave(x, y, z, 1, 8, CHARGE_RADIUS));
			for (int i = 0; i < 10; i++)
				level.addParticle(ParticleTypes.POOF, x + off(rnd, 0.5D), y + 0.2D + rnd.nextDouble() * 0.8D, z + off(rnd, 0.5D), off(rnd, 0.1D), 0.02D, off(rnd, 0.1D));
			for (int i = 0; i < 16; i++) {
				double a = rnd.nextDouble() * Math.PI * 2.0D;
				level.addParticle(ParticleTypes.CRIT, x, y + 0.5D, z, Math.cos(a) * 0.4D, 0.1D, Math.sin(a) * 0.4D);
			}
			sound(level, x, y, z, SoundEvents.FIRECHARGE_USE, 0.9F, 0.7F);
			sound(level, x, y, z, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 0.6F);
			sound(level, x, y, z, SoundEvents.RAVAGER_ATTACK, 0.7F, 1.0F);
			sound(level, x, y, z, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.7F);
			if (local) {
				shake(0.4F, 8);
				dashFovTicks = DASH_TRAIL_TICKS;
			}
		}

		if (s.dashTicks > 0) {
			s.dashTicks--;
			Vec3 look = p.getLookAngle();
			for (int i = 0; i < 4; i++)
				level.addParticle(ParticleTypes.FLAME, x + off(rnd, 0.35D), y + 0.2D + rnd.nextDouble() * 1.3D, z + off(rnd, 0.35D), -look.x * 0.1D, 0.01D, -look.z * 0.1D);
			for (int i = 0; i < 2; i++)
				level.addParticle(ParticleTypes.LARGE_SMOKE, x + off(rnd, 0.3D), y + 0.3D + rnd.nextDouble(), z + off(rnd, 0.3D), 0, 0, 0);
			for (int i = 0; i < 3; i++)
				level.addParticle(ParticleTypes.CRIT, x + off(rnd, 0.3D), y + 0.3D + rnd.nextDouble() * 1.2D, z + off(rnd, 0.3D), -look.x * 0.3D, 0.02D, -look.z * 0.3D);
			level.addParticle(RED_DUST, x + off(rnd, 0.3D), y + 0.2D + rnd.nextDouble() * 1.2D, z + off(rnd, 0.3D), 0, 0, 0);
		}
	}

	// ---------------------------------------------------------------
	//  Expanding shockwaves on the ground
	// ---------------------------------------------------------------
	private static void updateWaves(ClientLevel level, RandomSource rnd) {
		Iterator<Wave> it = WAVES.iterator();
		while (it.hasNext()) {
			Wave w = it.next();
			w.age++;
			double r = w.max * w.age / w.total;
			int n = 16 + (int) (r * 7.0D);
			double rot = w.age * 0.35D;
			for (int i = 0; i < n; i++) {
				double a = rot + i * (Math.PI * 2.0D / n);
				double cx = Math.cos(a), sz = Math.sin(a);
				double px = w.x + cx * r, pz = w.z + sz * r;
				if (w.type == 0) {
					level.addParticle(ParticleTypes.FLAME, px, w.y + 0.1D, pz, cx * 0.04D, 0.02D, sz * 0.04D);
					if (i % 2 == 0)
						level.addParticle(RED_DUST, px, w.y + 0.15D, pz, 0, 0, 0);
					if (i % 5 == 0)
						level.addParticle(ParticleTypes.LARGE_SMOKE, px, w.y + 0.2D, pz, cx * 0.03D, 0.03D, sz * 0.03D);
				} else {
					level.addParticle(ParticleTypes.CLOUD, px, w.y + 0.2D, pz, cx * 0.06D, 0.01D, sz * 0.06D);
					if (i % 3 == 0)
						level.addParticle(ParticleTypes.CRIT, px, w.y + 0.3D, pz, cx * 0.15D, 0.1D, sz * 0.15D);
					if (i % 4 == 0)
						level.addParticle(ORANGE_DUST, px, w.y + 0.15D, pz, 0, 0, 0);
				}
			}
			if (w.age >= w.total)
				it.remove();
		}
	}

	// ---------------------------------------------------------------
	//  Camera: shake + FOV kick (only for the local player)
	// ---------------------------------------------------------------
	private static void shake(float power, int ticks) {
		if (!CAMERA_SHAKE)
			return;
		float current = shakeTicks > 0 ? shakePower * shakeTicks / (float) shakeTotal : 0.0F;
		if (power > current) {
			shakePower = power;
			shakeTicks = ticks;
			shakeTotal = ticks;
		}
	}

	@SubscribeEvent
	public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
		if (!CAMERA_SHAKE || shakeTicks <= 0)
			return;
		float k = shakeTicks / (float) shakeTotal;
		float amp = shakePower * k * 2.2F;
		double t = System.nanoTime() / 1.0E9D;
		event.setPitch(event.getPitch() + (float) Math.sin(t * 61.0D) * amp);
		event.setYaw(event.getYaw() + (float) Math.cos(t * 47.0D) * amp * 0.7F);
		event.setRoll(event.getRoll() + (float) Math.sin(t * 53.0D) * amp * 0.5F);
	}

	@SubscribeEvent
	public static void onFov(ViewportEvent.ComputeFov event) {
		if (!DASH_FOV || dashFovTicks <= 0)
			return;
		float k = dashFovTicks / (float) DASH_TRAIL_TICKS;
		event.setFOV(event.getFOV() + 14.0D * k);
	}

	// ---------------------------------------------------------------
	//  Red pulsing vignette while Berserk Mode is on (local player)
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onGuiPre(RenderGuiEvent.Pre event) {
		if (!localMode)
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.options.hideGui)
			return;

		GuiGraphics g = event.getGuiGraphics();
		int w = g.guiWidth();
		int h = g.guiHeight();

		int beat = (int) ((tickCount + mc.player.getId()) % BEAT_PERIOD);
		float pulse = beatShape(beat);
		int maxA = (int) (45 + 85 * pulse);
		int base = 0x8A0A05;

		int band = Math.max(20, h / 4);
		g.fillGradient(0, 0, w, band, (maxA << 24) | base, base);
		g.fillGradient(0, h - band, w, h, base, (maxA << 24) | base);

		int side = Math.max(16, w / 8);
		int step = 3;
		for (int i = 0; i < side; i += step) {
			float f = 1.0F - i / (float) side;
			int a = (int) (maxA * f * f);
			int c = (a << 24) | base;
			g.fill(i, 0, i + step, h, c);
			g.fill(w - i - step, 0, w - i, h, c);
		}
	}

	private static float beatShape(int beat) {
		float a = (float) Math.exp(-beat / 3.0D);
		float b = beat >= 6 ? 0.7F * (float) Math.exp(-(beat - 6) / 3.0D) : 0.0F;
		return Math.min(1.0F, Math.max(a, b));
	}

	// ---------------------------------------------------------------
	//  Helpers
	// ---------------------------------------------------------------
	private static double off(RandomSource rnd, double r) {
		return (rnd.nextDouble() - 0.5D) * 2.0D * r;
	}

	private static void debris(ClientLevel level, RandomSource rnd, double x, double y, double z, int count, double spread, double up) {
		BlockPos pos = BlockPos.containing(x, y - 0.2D, z);
		BlockState st = level.getBlockState(pos);
		if (st.isAir())
			st = level.getBlockState(pos.below());
		if (st.isAir())
			return;
		ParticleOptions opt = new BlockParticleOption(ParticleTypes.BLOCK, st);
		for (int i = 0; i < count; i++)
			level.addParticle(opt, x + off(rnd, spread), y + 0.1D, z + off(rnd, spread), off(rnd, 0.2D), up * (0.5D + rnd.nextDouble()), off(rnd, 0.2D));
	}

	private static void sound(ClientLevel level, double x, double y, double z, SoundEvent s, float volume, float pitch) {
		if (SOUNDS_ENABLED)
			level.playLocalSound(x, y, z, s, SoundSource.PLAYERS, volume, pitch, false);
	}

	private static void sound(ClientLevel level, double x, double y, double z, Holder<SoundEvent> s, float volume, float pitch) {
		sound(level, x, y, z, s.value(), volume, pitch);
	}
}
