package net.mcreator.eldervale.client.vfx;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.client.CameraShakeManager;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * An effect described as a script: layers (shapes drawn during a time window) and cues
 * (a sound, a flash, a shake... fired at a given tick). Every skill is a small subclass that only fills the script.
 *
 * Time is in client ticks since the effect started (20 per second). A layer gets u = 0..1 over its life.
 * Events from the server (IMPACT, END...) arrive in onEvent(): the subclass adds more layers and cues
 * relative to now() = the current tick.
 */
public abstract class ScriptedVfx extends VfxEffect {

	/** Draws one layer. u = progress 0..1 inside the layer's time window. */
	public interface Layer {
		void draw(VfxGfx g, Frame f, float u);
	}

	/** Everything a layer needs to know about "where" and "when". Reused every frame. */
	public static final class Frame {
		public double x, y, z;       // caster feet (smoothed), y is slightly above the ground
		public double hx, hz;        // horizontal forward direction at cast time (unit vector)
		public double dx, dy, dz;    // full look direction at cast time
		public double tx, ty, tz;    // target or impact point (valid when hasTarget)
		public boolean hasTarget;
		public float t;              // ticks since the effect started, with partial tick
		public float radius;         // area radius sent by the server
		public int seed;
		public boolean local;        // the caster is the local player
		public Trail trail;          // recent positions of the caster
	}

	/** Last positions of the caster (newest first), used for dash trails. */
	public static final class Trail {
		private final double[] xs, ys, zs;
		private int head, count;

		public Trail(int size) {
			xs = new double[size];
			ys = new double[size];
			zs = new double[size];
		}

		public void push(double x, double y, double z) {
			head = (head + 1) % xs.length;
			xs[head] = x;
			ys[head] = y;
			zs[head] = z;
			count = Math.min(count + 1, xs.length);
		}

		public int size() {
			return count;
		}

		/** i = 0 is the newest point. */
		public double x(int i) {
			return xs[(head - i + xs.length * 2) % xs.length];
		}

		public double y(int i) {
			return ys[(head - i + ys.length * 2) % ys.length];
		}

		public double z(int i) {
			return zs[(head - i + zs.length * 2) % zs.length];
		}
	}

	private static final class LayerDef {
		final int start, life;
		final Layer layer;

		LayerDef(int start, int life, Layer layer) {
			this.start = start;
			this.life = Math.max(1, life);
			this.layer = layer;
		}
	}

	private enum CueType {
		SOUND, FLASH, SHAKE, FOV, VIGNETTE
	}

	private static final class Cue {
		final int tick;
		final CueType type;
		final String name;
		final float a, b;
		final float[] color;
		final int n;
		final boolean atTarget;
		boolean fired;
		boolean fixed;
		double fx, fy, fz;

		Cue(int tick, CueType type, String name, float a, float b, float[] color, int n, boolean atTarget) {
			this.tick = tick;
			this.type = type;
			this.name = name;
			this.a = a;
			this.b = b;
			this.color = color;
			this.n = n;
			this.atTarget = atTarget;
		}
	}

	private final List<LayerDef> layers = new ArrayList<>();
	private final List<LayerDef> shades = new ArrayList<>();
	private final List<Cue> cues = new ArrayList<>();
	protected final Frame frame = new Frame();
	private final Trail trail = new Trail(18);

	protected final int seed;
	protected final float radius;
	protected final int arg;
	/** true = the effect follows the caster, false = it stays where the START event happened */
	protected boolean follow = true;
	/** true = keep playing even if the followed entity died or disappeared (death bursts) */
	protected boolean lingerOnGone;

	private double lastX, lastY, lastZ;
	private int maxTick;
	private int holdUntil;
	private int forcedEnd = Integer.MAX_VALUE;

	protected ScriptedVfx(int vfxId, VfxEventPayload p, float defaultRadius) {
		super(vfxId, p.entityId());
		this.seed = p.seed();
		this.radius = p.param() > 0.0F ? p.param() : defaultRadius;
		this.arg = p.arg();
		this.lastX = p.x();
		this.lastY = p.y();
		this.lastZ = p.z();
		double hl = Math.sqrt(p.dx() * p.dx() + p.dz() * p.dz());
		frame.hx = hl > 1.0E-4D ? p.dx() / hl : 0.0D;
		frame.hz = hl > 1.0E-4D ? p.dz() / hl : 1.0D;
		frame.dx = p.dx();
		frame.dy = p.dy();
		frame.dz = p.dz();
		frame.radius = this.radius;
		frame.seed = this.seed;
		frame.trail = trail;
		trail.push(lastX, lastY, lastZ);
	}

	// ---------------------------------------------------------------
	//  Script API
	// ---------------------------------------------------------------
	/** Current tick of the effect (use it to schedule things when a server event arrives). */
	protected final int now() {
		return ticks;
	}

	protected final void layer(int start, int life, Layer layer) {
		layers.add(new LayerDef(start, life, layer));
		maxTick = Math.max(maxTick, start + life);
	}

	/** Same as layer() but drawn with normal translucent blending (dark smoke, shadows) instead of additive light. */
	protected final void shadeLayer(int start, int life, Layer layer) {
		shades.add(new LayerDef(start, life, layer));
		maxTick = Math.max(maxTick, start + life);
	}

	/** Ends the effect at this tick whatever else is scheduled (used by effects that last until an END event). */
	protected final void endAt(int tick) {
		forcedEnd = Math.min(forcedEnd, tick);
	}

	/** Keeps the effect alive at least until this tick (use while waiting for a server event). */
	protected final void holdUntil(int tick) {
		holdUntil = Math.max(holdUntil, tick);
	}

	protected final void sound(int tick, String name, float volume, float pitch) {
		cues.add(new Cue(tick, CueType.SOUND, name, volume, pitch, null, 0, false));
		maxTick = Math.max(maxTick, tick);
	}

	/** Same as sound() but played at the target point instead of the caster. */
	protected final void soundAtTarget(int tick, String name, float volume, float pitch) {
		cues.add(new Cue(tick, CueType.SOUND, name, volume, pitch, null, 0, true));
		maxTick = Math.max(maxTick, tick);
	}

	/** Sound at an explicit world position (a monster that gets hit, a point on the ground...). */
	protected final void soundAt(int tick, String name, double x, double y, double z, float volume, float pitch) {
		Cue c = new Cue(tick, CueType.SOUND, name, volume, pitch, null, 0, false);
		c.fixed = true;
		c.fx = x;
		c.fy = y;
		c.fz = z;
		cues.add(c);
		maxTick = Math.max(maxTick, tick);
	}

	/** Full screen flash (only for the caster). */
	protected final void flash(int tick, float[] color, float alpha, int fadeTicks) {
		cues.add(new Cue(tick, CueType.FLASH, null, alpha, 0.0F, color, fadeTicks, false));
	}

	/** Camera shake. The caster gets the full power; others get less depending on the distance (range in blocks). */
	protected final void shake(int tick, float power, int shakeTicks, float range, boolean atTarget) {
		cues.add(new Cue(tick, CueType.SHAKE, null, power, range, null, shakeTicks, atTarget));
	}

	/** FOV change in degrees (only for the caster). */
	protected final void fov(int tick, float degrees) {
		cues.add(new Cue(tick, CueType.FOV, null, degrees, 0.0F, null, 0, false));
	}

	/** Colored screen edges for 'ticks' ticks (only for the caster). */
	protected final void vignette(int tick, float[] color, float alpha, int duration) {
		cues.add(new Cue(tick, CueType.VIGNETTE, null, alpha, 0.0F, color, duration, false));
		maxTick = Math.max(maxTick, tick + duration);
	}

	/** Called at the end of every client tick (track entities, update custom state). */
	protected void onTick(ClientLevel level) {
	}

	/** Phases from the server after START (IMPACT, PULSE, END). Add layers and cues here. */
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
	}

	// ---------------------------------------------------------------
	//  Engine
	// ---------------------------------------------------------------
	@Override
	public final void event(VfxEventPayload p, ClientLevel level) {
		if (p.phase() == VfxIds.IMPACT) {
			frame.tx = p.x();
			frame.ty = p.y();
			frame.tz = p.z();
			frame.hasTarget = true;
		}
		onEvent(p, level);
	}

	private boolean isLocal() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.player.getId() == entityId;
	}

	@Override
	protected final void tick(ClientLevel level) {
		if (follow) {
			Entity e = level.getEntity(entityId);
			if (e != null) {
				lastX = e.getX();
				lastY = e.getY();
				lastZ = e.getZ();
			}
			// the caster left the area or died: stop the effect
			if (!lingerOnGone && ticks > 10 && (e == null || !e.isAlive()))
				dead = true;
		}
		trail.push(lastX, lastY, lastZ);
		frame.local = isLocal();

		for (int i = 0; i < cues.size(); i++) {
			Cue c = cues.get(i);
			if (!c.fired && ticks >= c.tick) {
				c.fired = true;
				fire(c, level);
			}
		}
		onTick(level);
		if (ticks > Math.max(maxTick, holdUntil) + 2 || ticks > forcedEnd)
			dead = true;
	}

	private void fire(Cue c, ClientLevel level) {
		double x = c.atTarget && frame.hasTarget ? frame.tx : lastX;
		double y = c.atTarget && frame.hasTarget ? frame.ty : lastY;
		double z = c.atTarget && frame.hasTarget ? frame.tz : lastZ;
		if (c.fixed) {
			x = c.fx;
			y = c.fy;
			z = c.fz;
		}
		switch (c.type) {
			case SOUND -> VfxSfx.play(level, c.name, x, y, z, c.a, c.b);
			case FLASH -> {
				if (frame.local)
					VfxScreen.flash(c.color, c.a, c.n);
			}
			case FOV -> {
				if (frame.local)
					VfxScreen.fovPunch(c.a);
			}
			case VIGNETTE -> {
				// refreshed each tick by the layer below
			}
			case SHAKE -> {
				if (!VfxManager.SCREEN_EFFECTS)
					return;
				Minecraft mc = Minecraft.getInstance();
				if (mc.player == null)
					return;
				float power = c.a;
				if (!frame.local || c.atTarget) {
					double d = Math.sqrt(mc.player.distanceToSqr(x, y, z));
					if (d >= c.b)
						return;
					power *= (float) (1.0D - d / c.b);
				}
				if (power > 0.02F)
					CameraShakeManager.getInstance().shake(power, c.n);
			}
		}
	}

	@Override
	public final void renderGlow(VfxGfx g, ClientLevel level, float partial) {
		double x = lastX, y = lastY, z = lastZ;
		if (follow) {
			Entity e = level.getEntity(entityId);
			if (e != null) {
				Vec3 p = e.getPosition(partial);
				x = p.x;
				y = p.y;
				z = p.z;
			}
		}
		frame.x = x;
		frame.y = y + 0.04D;
		frame.z = z;
		frame.t = ticks + partial;

		for (int i = 0; i < layers.size(); i++) {
			LayerDef l = layers.get(i);
			float age = frame.t - l.start;
			if (age < 0.0F || age > l.life)
				continue;
			l.layer.draw(g, frame, age / l.life);
		}
		// vignette cues run for their whole duration
		for (int i = 0; i < cues.size(); i++) {
			Cue c = cues.get(i);
			if (c.type == CueType.VIGNETTE && c.fired && frame.local && frame.t < c.tick + c.n) {
				float in = VfxMath.clamp01((frame.t - c.tick) / 6.0F);
				float out = VfxMath.clamp01((c.tick + c.n - frame.t) / 8.0F);
				VfxScreen.vignette(c.color, c.a * in * out);
			}
		}
	}

	@Override
	public final boolean usesShade() {
		return !shades.isEmpty();
	}

	@Override
	public final void renderShade(VfxGfx g, ClientLevel level, float partial) {
		for (int i = 0; i < shades.size(); i++) {
			LayerDef l = shades.get(i);
			float age = frame.t - l.start;
			if (age < 0.0F || age > l.life)
				continue;
			l.layer.draw(g, frame, age / l.life);
		}
	}
}
