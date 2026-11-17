package net.mcreator.eldervale.client.effects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Holder;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.network.EldervaleModVariables;

/**
 * VFX for the two Monk skills:
 *  - Skill 1, Punch Barrage (cooldown in SKILL1_CD): teleport trail, light pillars and a flurry of impact bursts.
 *  - Skill 2, Meditation (cooldown in SKILL2_CD): lotus, rotating mandala, dome and a pulse of light every second.
 * The effects are drawn directly in the world with glowing geometry (no textures needed).
 * Everything runs on the client by watching synced variables, so on a server everyone sees them.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class MonkVfx {

	// ---- Settings (edit here) ----
	private static final boolean EFFECTS_ENABLED = true;
	private static final boolean SOUNDS_ENABLED = true;      // sounds of the Meditation skill (the barrage sounds come from the server)
	private static final double MAX_DISTANCE = 48.0D;
	private static final int GRACE_TICKS = 20;
	private static final int MEDITATION_TICKS = 100;         // 5 pulses, one per second
	private static final double MEDITATION_RADIUS = 2.5D;    // matches the push area of the skill
	private static final int[] PUNCH_TICKS = {3, 7, 11, 15, 19}; // ticks after the teleport (matches the server punches)

	private static final float[] TEAL = {0.30F, 0.95F, 0.90F};
	private static final float[] GOLD = {1.00F, 0.82F, 0.30F};
	private static final float[] WHITE = {1.00F, 1.00F, 1.00F};
	private static final float[] HEAL = {0.55F, 1.00F, 0.60F};

	// effect types
	private static final int WAVE = 0;
	private static final int PILLAR = 1;
	private static final int MANDALA = 2;
	private static final int DOME = 3;
	private static final int LOTUS = 4;
	private static final int TRAIL = 5;
	private static final int BURST = 6;

	// ---------------------------------------------------------------
	//  State
	// ---------------------------------------------------------------
	private static class Fx {
		final int type;
		final double x, y, z;
		final double start;
		final int life;
		final float r, g, b;
		final double p1, p2, p3, p4;

		Fx(int type, double x, double y, double z, double start, int life, float[] c, double p1, double p2, double p3, double p4) {
			this.type = type;
			this.x = x;
			this.y = y;
			this.z = z;
			this.start = start;
			this.life = life;
			this.r = c[0];
			this.g = c[1];
			this.b = c[2];
			this.p1 = p1;
			this.p2 = p2;
			this.p3 = p3;
			this.p4 = p4;
		}
	}

	private static class MState {
		int age = 0;
		double prevCd1, prevCd2;
		double lastX, lastY, lastZ;
		// Punch Barrage
		int tpWatch = 0;
		double ax, ay, az;      // where the monk was when he cast
		int arriveWait = -1;
		int barrageT = -1;
		double tx, ty, tz;      // where the punches land
		// Meditation
		int medT = -1;
		double mx, my, mz;
	}

	private static final Map<UUID, MState> STATES = new HashMap<>();
	private static final List<Fx> FX = new ArrayList<>();
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
			FX.clear();
			return;
		}
		if (mc.isPaused())
			return;

		tickCount++;
		FX.removeIf(f -> tickCount - f.start > f.life + 2);
		if (!EFFECTS_ENABLED)
			return;

		RandomSource rnd = level.random;
		double maxSqr = MAX_DISTANCE * MAX_DISTANCE;
		Set<UUID> seen = new HashSet<>();

		for (Player p : level.players()) {
			if (p.distanceToSqr(mc.player) > maxSqr)
				continue;
			EldervaleModVariables.PlayerVariables v = p.getData(EldervaleModVariables.PLAYER_VARIABLES);
			if (!"monk".equals(v.PLAYER_CLASS))
				continue;

			UUID id = p.getUUID();
			seen.add(id);
			MState s = STATES.computeIfAbsent(id, k -> new MState());
			if (s.age == 0) {
				s.prevCd1 = v.SKILL1_CD;
				s.prevCd2 = v.SKILL2_CD;
				s.lastX = p.getX();
				s.lastY = p.getY();
				s.lastZ = p.getZ();
			}
			boolean grace = s.age < GRACE_TICKS;
			s.age++;

			handleBarrage(level, rnd, p, v, s, grace);
			handleMeditation(level, rnd, p, v, s, grace);

			s.lastX = p.getX();
			s.lastY = p.getY();
			s.lastZ = p.getZ();
		}
		STATES.keySet().retainAll(seen);
	}

	// ---------------------------------------------------------------
	//  Skill 1: Punch Barrage
	// ---------------------------------------------------------------
	private static void handleBarrage(ClientLevel level, RandomSource rnd, Player p, EldervaleModVariables.PlayerVariables v, MState s, boolean grace) {
		double cd = v.SKILL1_CD;
		boolean cast = cd - s.prevCd1 > 50.0D && cd >= v.SKILL1_MAX_CD - 10.0D;
		s.prevCd1 = cd;
		double x = p.getX(), y = p.getY(), z = p.getZ();

		if (cast && !grace) {
			s.tpWatch = 12;
			s.ax = s.lastX;
			s.ay = s.lastY;
			s.az = s.lastZ;
		}
		if (s.tpWatch > 0) {
			s.tpWatch--;
			double dx = x - s.ax, dy = y - s.ay, dz = z - s.az;
			if (s.arriveWait < 0 && dx * dx + dy * dy + dz * dz > 4.0D)
				s.arriveWait = 2; // wait a moment so the position has settled
		}
		if (s.arriveWait >= 0) {
			if (s.arriveWait == 0) {
				s.tpWatch = 0;
				onTeleport(level, rnd, p, s, x, y, z);
			}
			s.arriveWait--;
		}

		if (s.barrageT >= 0) {
			for (int k = 0; k < PUNCH_TICKS.length; k++) {
				if (s.barrageT == PUNCH_TICKS[k])
					punchFx(level, rnd, s, k, k == PUNCH_TICKS.length - 1);
			}
			s.barrageT++;
			if (s.barrageT > PUNCH_TICKS[PUNCH_TICKS.length - 1] + 6)
				s.barrageT = -1;
		}
	}

	private static void onTeleport(ClientLevel level, RandomSource rnd, Player p, MState s, double bx, double by, double bz) {
		// departure
		wave(s.ax, s.ay, s.az, 12, TEAL, 1.4D, 0.5D);
		pillar(s.ax, s.ay, s.az, 14, TEAL, 0.4D, 2.2D);
		// trail between the two points
		fx(TRAIL, s.ax, s.ay + 0.9D, s.az, 12, TEAL, bx, by + 0.9D, bz, 0.5D);
		fx(TRAIL, s.ax, s.ay + 0.9D, s.az, 10, WHITE, bx, by + 0.9D, bz, 0.18D);
		// arrival
		wave(bx, by, bz, 14, GOLD, 2.0D, 0.55D);
		pillar(bx, by, bz, 16, TEAL, 0.45D, 2.6D);
		for (int i = 0; i < 10; i++)
			level.addParticle(ParticleTypes.END_ROD, bx + off(rnd, 0.4D), by + 0.2D + rnd.nextDouble() * 1.6D, bz + off(rnd, 0.4D), 0, 0.04D, 0);

		// where the punches will land: the enemy in front, or the spot ahead of the monk
		LivingEntity target = nearestEnemy(level, bx, by, bz, 4.0D);
		if (target != null) {
			s.tx = target.getX();
			s.ty = target.getY(0.6D);
			s.tz = target.getZ();
		} else {
			Vec3 look = p.getLookAngle();
			s.tx = bx + look.x * 1.4D;
			s.ty = by + 1.2D;
			s.tz = bz + look.z * 1.4D;
		}
		s.barrageT = 0;
	}

	private static void punchFx(ClientLevel level, RandomSource rnd, MState s, int k, boolean last) {
		double x = s.tx + off(rnd, 0.35D);
		double y = s.ty + off(rnd, 0.3D);
		double z = s.tz + off(rnd, 0.35D);
		fx(BURST, x, y, z, last ? 12 : 8, last ? GOLD : TEAL, last ? 1.7D : 0.9D, last ? 0.10D : 0.07D, 0, 0);
		if (!last)
			return;
		wave(s.tx, s.ty - 1.0D, s.tz, 14, GOLD, 2.2D, 0.6D);
		pillar(s.tx, s.ty - 1.0D, s.tz, 16, GOLD, 0.5D, 3.0D);
		for (int i = 0; i < 12; i++) {
			double a = i * (Math.PI * 2.0D / 12.0D);
			level.addParticle(ParticleTypes.END_ROD, s.tx, s.ty, s.tz, Math.cos(a) * 0.2D, 0.05D, Math.sin(a) * 0.2D);
		}
	}

	// ---------------------------------------------------------------
	//  Skill 2: Meditation
	// ---------------------------------------------------------------
	private static void handleMeditation(ClientLevel level, RandomSource rnd, Player p, EldervaleModVariables.PlayerVariables v, MState s, boolean grace) {
		double cd = v.SKILL2_CD;
		boolean cast = cd - s.prevCd2 > 50.0D && cd >= v.SKILL2_MAX_CD - 10.0D;
		s.prevCd2 = cd;
		double x = p.getX(), y = p.getY(), z = p.getZ();

		if (cast && !grace) {
			s.medT = 0;
			s.mx = x;
			s.my = y;
			s.mz = z;
			fx(LOTUS, x, y, z, MEDITATION_TICKS, GOLD, 1.0D, 0, 0, 0);
			fx(MANDALA, x, y, z, MEDITATION_TICKS, TEAL, MEDITATION_RADIUS + 0.1D, 0, 0, 0);
			fx(DOME, x, y, z, MEDITATION_TICKS, TEAL, 1.7D, 0, 0, 0);
			pillar(x, y, z, 22, GOLD, 0.6D, 3.2D);
			wave(x, y, z, 18, TEAL, MEDITATION_RADIUS, 0.5D);
			sound(level, x, y, z, SoundEvents.BEACON_ACTIVATE, 0.7F, 1.2F);
			sound(level, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 0.8F);
		}
		if (s.medT < 0)
			return;

		int t = s.medT;
		if (t > 0 && t % 20 == 0) {
			if (t < MEDITATION_TICKS) {
				// a pulse of healing light every second
				int i = t / 20;
				wave(s.mx, s.my, s.mz, 16, HEAL, MEDITATION_RADIUS, 0.5D);
				pillar(s.mx, s.my, s.mz, 10, HEAL, 0.35D, 1.8D);
				sound(level, s.mx, s.my, s.mz, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.0F + i * 0.12F);
				sound(level, s.mx, s.my, s.mz, SoundEvents.BELL_BLOCK, 0.3F, 1.3F);
			} else {
				// final release
				wave(s.mx, s.my, s.mz, 24, GOLD, 3.8D, 0.8D);
				wave(s.mx, s.my, s.mz, 18, WHITE, 2.6D, 0.4D);
				pillar(s.mx, s.my, s.mz, 20, GOLD, 0.7D, 4.0D);
				fx(BURST, s.mx, s.my + 1.0D, s.mz, 14, GOLD, 2.2D, 0.12D, 0, 0);
				for (int i = 0; i < 16; i++) {
					double a = i * (Math.PI * 2.0D / 16.0D);
					level.addParticle(ParticleTypes.END_ROD, s.mx, s.my + 0.8D, s.mz, Math.cos(a) * 0.25D, 0.06D, Math.sin(a) * 0.25D);
				}
				sound(level, s.mx, s.my, s.mz, SoundEvents.BEACON_DEACTIVATE, 0.7F, 1.2F);
				sound(level, s.mx, s.my, s.mz, SoundEvents.BELL_BLOCK, 0.7F, 0.9F);
				sound(level, s.mx, s.my, s.mz, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.9F);
				s.medT = -1;
				return;
			}
		}

		// spirit lights rising inside the dome
		if (t % 3 == 0)
			level.addParticle(ParticleTypes.END_ROD, s.mx + off(rnd, 1.3D), s.my + 0.1D + rnd.nextDouble() * 0.6D, s.mz + off(rnd, 1.3D), 0, 0.03D, 0);
		s.medT++;
	}

	// ---------------------------------------------------------------
	//  Rendering
	// ---------------------------------------------------------------
	@SubscribeEvent
	public static void onRenderLevel(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES)
			return;
		if (!EFFECTS_ENABLED || FX.isEmpty())
			return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null)
			return;

		Camera camera = event.getCamera();
		Vec3 cam = camera.getPosition();
		PoseStack pose = event.getPoseStack();
		MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
		VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
		Draw d = new Draw(vc, pose.last().pose(), cam, camera.getLeftVector(), camera.getUpVector());
		double now = tickCount + event.getPartialTick().getGameTimeDeltaPartialTick(false);

		for (Fx f : FX) {
			double age = now - f.start;
			if (age < 0.0D || age > f.life)
				continue;
			paint(d, f, age, (float) (age / f.life));
		}
		buffers.endBatch(RenderType.lightning());
	}

	private static void paint(Draw d, Fx f, double age, float t) {
		switch (f.type) {
			case WAVE -> {
				float rad = (float) (f.p1 * easeOut(t));
				float w = (float) (f.p2 * (1.0F - 0.6F * t));
				float a = (1.0F - t) * (1.0F - t);
				d.col(f.r, f.g, f.b);
				d.ring(f.x, f.y + 0.06D, f.z, Math.max(0.0F, rad - w), rad, 0.0F, 0.95F * a, 40);
				d.col(1.0F, 1.0F, 1.0F);
				d.ring(f.x, f.y + 0.07D, f.z, Math.max(0.0F, rad - w * 0.18F), rad, 0.0F, 0.9F * a, 40);
			}
			case PILLAR -> {
				float rise = easeOut(Math.min(1.0F, t * 3.0F));
				float h = (float) (f.p2 * rise);
				float env = t < 0.2F ? t / 0.2F : 1.0F - (t - 0.2F) / 0.8F;
				d.col(f.r, f.g, f.b);
				d.column(f.x, f.y, f.y + h, f.z, f.p1, 14, 0.75F * env, 0.0F);
				d.col(1.0F, 1.0F, 1.0F);
				d.column(f.x, f.y, f.y + h * 0.9F, f.z, f.p1 * 0.45D, 12, 0.7F * env, 0.0F);
			}
			case MANDALA -> {
				float rr = (float) f.p1;
				float env = Math.max(0.0F, Math.min(1.0F, (float) age / 10.0F) * Math.min(1.0F, (float) (f.life - age) / 15.0F));
				double rot = age * 0.05D;
				d.col(f.r, f.g, f.b);
				d.ring(f.x, f.y + 0.05D, f.z, rr - 0.10F, rr, 0.8F * env, 0.8F * env, 48);
				d.ring(f.x, f.y + 0.05D, f.z, rr * 0.72F - 0.07F, rr * 0.72F, 0.7F * env, 0.7F * env, 40);
				d.ring(f.x, f.y + 0.05D, f.z, 0.0F, rr * 0.45F, 0.20F * env, 0.05F * env, 32);
				for (int k = 0; k < 8; k++)
					d.radialQuad(f.x, f.y + 0.05D, f.z, rot + k * (Math.PI / 4.0D), rr * 0.45F, rr, 0.045D, 0.5F * env, 0.15F * env);
				d.col(1.0F, 0.85F, 0.35F);
				for (int k = 0; k < 4; k++) {
					double a0 = -rot * 1.6D + k * (Math.PI / 2.0D);
					d.ringArc(f.x, f.y + 0.06D, f.z, rr * 0.45F - 0.06F, rr * 0.45F + 0.02F, a0, a0 + 0.9D, 0.9F * env, 0.9F * env, 10);
				}
			}
			case DOME -> {
				float rr = (float) f.p1;
				float env = Math.max(0.0F, Math.min(1.0F, (float) age / 12.0F) * Math.min(1.0F, (float) (f.life - age) / 15.0F));
				float pulse = 0.7F + 0.3F * (float) Math.sin(age * 0.25D);
				d.col(f.r, f.g, f.b);
				d.dome(f.x, f.y, f.z, rr, 7, 24, 0.30F * env * pulse, 0.05F * env);
				d.col(1.0F, 1.0F, 1.0F);
				d.ring(f.x, f.y + 0.06D, f.z, rr - 0.05F, rr, 0.0F, 0.5F * env * pulse, 32);
			}
			case LOTUS -> {
				float size = (float) f.p1;
				float open = easeOut(Math.min(1.0F, (float) age / 18.0F));
				float env = Math.max(0.0F, Math.min(1.0F, (float) (f.life - age) / 10.0F));
				double rot = age * 0.02D;
				d.col(f.r, f.g, f.b);
				for (int k = 0; k < 8; k++)
					d.petal(f.x, f.y + 0.04D, f.z, rot + k * (Math.PI / 4.0D), Math.toRadians(80.0D - 62.0D * open), 1.25D * size * (0.5D + 0.5D * open), 0.32D * size, 0.05F * env, 0.65F * env);
				d.col(1.0F, 1.0F, 0.85F);
				for (int k = 0; k < 8; k++)
					d.petal(f.x, f.y + 0.06D, f.z, rot + Math.PI / 8.0D + k * (Math.PI / 4.0D), Math.toRadians(88.0D - 40.0D * open), 0.8D * size * (0.5D + 0.5D * open), 0.22D * size, 0.05F * env, 0.7F * env);
			}
			case TRAIL -> {
				float a = 1.0F - t;
				d.col(f.r, f.g, f.b);
				d.ribbon(f.x, f.y, f.z, f.p1, f.p2, f.p3, f.p4 * (1.0F - 0.5F * t), 0.1F * a, 0.9F * a);
			}
			case BURST -> {
				float len = (float) (f.p1 * easeOut(Math.min(1.0F, t * 2.0F)));
				float th = (float) (f.p2 * (1.0F - t));
				float a = 1.0F - t;
				d.col(f.r, f.g, f.b);
				d.burst(f.x, f.y, f.z, 10, len, th, a, f.start * 0.7D);
				d.col(1.0F, 1.0F, 1.0F);
				d.burst(f.x, f.y, f.z, 6, len * 0.6D, th * 0.8D, a, f.start * 1.3D);
			}
			default -> {
			}
		}
	}

	private static float easeOut(float t) {
		float u = 1.0F - t;
		return 1.0F - u * u * u;
	}

	// Draws glowing quads (both faces) relative to the camera
	private static class Draw {
		final VertexConsumer vc;
		final Matrix4f m;
		final double cx, cy, cz;
		final Vector3f left, up;
		float r = 1.0F, g = 1.0F, b = 1.0F;

		Draw(VertexConsumer vc, Matrix4f m, Vec3 cam, Vector3f left, Vector3f up) {
			this.vc = vc;
			this.m = m;
			this.cx = cam.x;
			this.cy = cam.y;
			this.cz = cam.z;
			this.left = left;
			this.up = up;
		}

		void col(float r, float g, float b) {
			this.r = r;
			this.g = g;
			this.b = b;
		}

		void v(double x, double y, double z, float a) {
			int ia = (int) (Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
			vc.addVertex(m, (float) (x - cx), (float) (y - cy), (float) (z - cz)).setColor((int) (r * 255.0F), (int) (g * 255.0F), (int) (b * 255.0F), ia);
		}

		void quad(double x1, double y1, double z1, float a1, double x2, double y2, double z2, float a2, double x3, double y3, double z3, float a3, double x4, double y4, double z4, float a4) {
			v(x1, y1, z1, a1);
			v(x2, y2, z2, a2);
			v(x3, y3, z3, a3);
			v(x4, y4, z4, a4);
			v(x4, y4, z4, a4);
			v(x3, y3, z3, a3);
			v(x2, y2, z2, a2);
			v(x1, y1, z1, a1);
		}

		void ringArc(double x, double y, double z, double r0, double r1, double aStart, double aEnd, float a0, float a1, int segs) {
			for (int i = 0; i < segs; i++) {
				double t0 = aStart + (aEnd - aStart) * i / segs;
				double t1 = aStart + (aEnd - aStart) * (i + 1) / segs;
				double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
				quad(x + c0 * r0, y, z + s0 * r0, a0, x + c0 * r1, y, z + s0 * r1, a1, x + c1 * r1, y, z + s1 * r1, a1, x + c1 * r0, y, z + s1 * r0, a0);
			}
		}

		void ring(double x, double y, double z, double r0, double r1, float a0, float a1, int segs) {
			ringArc(x, y, z, r0, r1, 0.0D, Math.PI * 2.0D, a0, a1, segs);
		}

		void column(double x, double y0, double y1, double z, double rad, int segs, float aBottom, float aTop) {
			for (int i = 0; i < segs; i++) {
				double t0 = i * (Math.PI * 2.0D / segs);
				double t1 = (i + 1) * (Math.PI * 2.0D / segs);
				double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
				quad(x + c0 * rad, y0, z + s0 * rad, aBottom, x + c1 * rad, y0, z + s1 * rad, aBottom, x + c1 * rad, y1, z + s1 * rad, aTop, x + c0 * rad, y1, z + s0 * rad, aTop);
			}
		}

		void dome(double x, double y, double z, double rad, int bands, int segs, float aEdge, float aTop) {
			for (int i = 0; i < bands; i++) {
				double p0 = (Math.PI / 2.0D) * i / bands;
				double p1 = (Math.PI / 2.0D) * (i + 1) / bands;
				double r0 = rad * Math.cos(p0), r1 = rad * Math.cos(p1);
				double h0 = rad * Math.sin(p0), h1 = rad * Math.sin(p1);
				float f0 = aEdge + (aTop - aEdge) * (float) (i / (double) bands);
				float f1 = aEdge + (aTop - aEdge) * (float) ((i + 1) / (double) bands);
				for (int j = 0; j < segs; j++) {
					double t0 = j * (Math.PI * 2.0D / segs);
					double t1 = (j + 1) * (Math.PI * 2.0D / segs);
					double c0 = Math.cos(t0), s0 = Math.sin(t0), c1 = Math.cos(t1), s1 = Math.sin(t1);
					quad(x + c0 * r0, y + h0, z + s0 * r0, f0, x + c1 * r0, y + h0, z + s1 * r0, f0, x + c1 * r1, y + h1, z + s1 * r1, f1, x + c0 * r1, y + h1, z + s0 * r1, f1);
				}
			}
		}

		void radialQuad(double x, double y, double z, double ang, double r0, double r1, double hw, float a0, float a1) {
			double c = Math.cos(ang), s = Math.sin(ang);
			double sx = -s, sz = c;
			quad(x + c * r0 + sx * hw, y, z + s * r0 + sz * hw, a0, x + c * r1 + sx * hw, y, z + s * r1 + sz * hw, a1, x + c * r1 - sx * hw, y, z + s * r1 - sz * hw, a1, x + c * r0 - sx * hw, y, z + s * r0 - sz * hw, a0);
		}

		void petal(double x, double y, double z, double ang, double tilt, double len, double hw, float aBase, float aTip) {
			double ch = Math.cos(tilt), sv = Math.sin(tilt);
			double dx = Math.cos(ang) * ch, dy = sv, dz = Math.sin(ang) * ch;
			double sx = -Math.sin(ang), sz = Math.cos(ang);
			double mx = x + dx * len * 0.5D, my = y + dy * len * 0.5D, mz = z + dz * len * 0.5D;
			float aMid = (aBase + aTip) * 0.5F;
			quad(x, y, z, aBase, mx + sx * hw, my, mz + sz * hw, aMid, x + dx * len, y + dy * len, z + dz * len, aTip, mx - sx * hw, my, mz - sz * hw, aMid);
		}

		// a ribbon that always faces the camera
		void ribbon(double x0, double y0, double z0, double x1, double y1, double z1, double width, float a0, float a1) {
			double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
			double mx = (x0 + x1) / 2.0D - cx, my = (y0 + y1) / 2.0D - cy, mz = (z0 + z1) / 2.0D - cz;
			double sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
			double len = Math.sqrt(sx * sx + sy * sy + sz * sz);
			if (len < 1.0E-6D)
				return;
			double k = width / 2.0D / len;
			sx *= k;
			sy *= k;
			sz *= k;
			quad(x0 + sx, y0 + sy, z0 + sz, a0, x1 + sx, y1 + sy, z1 + sz, a1, x1 - sx, y1 - sy, z1 - sz, a1, x0 - sx, y0 - sy, z0 - sz, a0);
		}

		// spikes radiating in the plane facing the camera
		void burst(double x, double y, double z, int n, double len, double thick, float a, double rot) {
			for (int k = 0; k < n; k++) {
				double ang = rot + k * (Math.PI * 2.0D / n);
				double ca = Math.cos(ang), sa = Math.sin(ang);
				double dx = left.x() * ca + up.x() * sa, dy = left.y() * ca + up.y() * sa, dz = left.z() * ca + up.z() * sa;
				double px = -left.x() * sa + up.x() * ca, py = -left.y() * sa + up.y() * ca, pz = -left.z() * sa + up.z() * ca;
				double tx = x + dx * len, ty = y + dy * len, tz = z + dz * len;
				quad(x + px * thick, y + py * thick, z + pz * thick, a, tx, ty, tz, 0.0F, tx, ty, tz, 0.0F, x - px * thick, y - py * thick, z - pz * thick, a);
			}
		}
	}

	// ---------------------------------------------------------------
	//  Helpers
	// ---------------------------------------------------------------
	private static void fx(int type, double x, double y, double z, int life, float[] c, double p1, double p2, double p3, double p4) {
		if (FX.size() > 160)
			FX.remove(0);
		FX.add(new Fx(type, x, y, z, tickCount, life, c, p1, p2, p3, p4));
	}

	private static void wave(double x, double y, double z, int life, float[] c, double radius, double width) {
		fx(WAVE, x, y, z, life, c, radius, width, 0, 0);
	}

	private static void pillar(double x, double y, double z, int life, float[] c, double radius, double height) {
		fx(PILLAR, x, y, z, life, c, radius, height, 0, 0);
	}

	private static LivingEntity nearestEnemy(ClientLevel level, double x, double y, double z, double range) {
		LivingEntity best = null;
		double bestD = range * range;
		for (Entity e : level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity le) || !(e instanceof Enemy) || !le.isAlive())
				continue;
			double d = e.distanceToSqr(x, y, z);
			if (d < bestD) {
				bestD = d;
				best = le;
			}
		}
		return best;
	}

	private static double off(RandomSource rnd, double r) {
		return (rnd.nextDouble() - 0.5D) * 2.0D * r;
	}

	private static void sound(ClientLevel level, double x, double y, double z, SoundEvent s, float volume, float pitch) {
		if (SOUNDS_ENABLED)
			level.playLocalSound(x, y, z, s, SoundSource.PLAYERS, volume, pitch, false);
	}

	private static void sound(ClientLevel level, double x, double y, double z, Holder<SoundEvent> s, float volume, float pitch) {
		sound(level, x, y, z, s.value(), volume, pitch);
	}
}
