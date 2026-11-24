package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.client.CameraShakeManager;
import net.mcreator.eldervale.client.vfx.VfxEffect;
import net.mcreator.eldervale.client.vfx.VfxGfx;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.client.vfx.VfxScreen;
import net.mcreator.eldervale.client.vfx.VfxSfx;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Monk skill: MEDITATION (Divine Prayer). Visual and audio only, no particles.
 *
 * Timeline (driven by the events the server sends from MonkMeditation):
 *   START : golden pillar, expanding shockwave, the mandala draws itself on the ground, the lotus opens,
 *           the sanctuary domes rise, rays of light and two spirals start to climb.
 *   PULSE : (10 times, every 0.5 s) a healing shockwave sweeps out to the edge of the push area,
 *           the mandala flashes, the lotus breathes, a chime plays (rising arpeggio).
 *   END   : the mandala collapses, a big release shockwave, a pillar and a burst of light.
 *
 * Nothing here changes gameplay. The radius comes from the server (the real push area).
 */
public final class MonkMeditationVfx extends VfxEffect {

	private static final float[] GOLD = {1.00F, 0.80F, 0.28F};
	private static final float[] TEAL = {0.28F, 0.95F, 0.88F};
	private static final float[] DEEP = {0.06F, 0.50F, 0.58F};
	private static final float[] HEAL = {0.55F, 1.00F, 0.62F};
	private static final float[] WHITE = {1.0F, 1.0F, 1.0F};

	private static final String[] CHIMES = {"monk_meditation_pulse_1", "monk_meditation_pulse_2", "monk_meditation_pulse_3"};
	private static final int[] ARPEGGIO = {0, 1, 2, 1, 2, 0, 1, 2, 1, 2};

	private static final int MAX_PULSES = 16;
	private static final int PULSE_LIFE = 11;
	private static final int END_LIFE = 26;

	private final int seed;
	private final float radius;
	private final int totalTicks;

	private double ax, ay, az;
	private boolean ended;
	private int endTick = -1;
	private final int[] pulseTick = new int[MAX_PULSES];
	private int pulseCount;
	private float flash;

	public MonkMeditationVfx(VfxEventPayload p) {
		super(VfxIds.MONK_MEDITATION, p.entityId());
		this.seed = p.seed();
		this.radius = p.param() > 1.0F ? Math.min(p.param(), 16.0F) : 5.0F;
		this.totalTicks = p.arg() > 0 ? p.arg() : 100;
		this.ax = p.x();
		this.ay = p.y();
		this.az = p.z();
	}

	private boolean isLocal() {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.player.getId() == entityId;
	}

	// ---------------------------------------------------------------
	//  Events and logic
	// ---------------------------------------------------------------
	@Override
	public void event(VfxEventPayload p, ClientLevel level) {
		switch (p.phase()) {
			case VfxIds.START -> onStart(level);
			case VfxIds.PULSE -> onPulse(level, p.arg());
			case VfxIds.END -> onEnd(level);
			default -> {
			}
		}
	}

	private void onStart(ClientLevel level) {
		VfxSfx.play(level, "monk_meditation_start", ax, ay, az, 1.0F, 1.0F);
		VfxSfx.play(level, "monk_meditation_aura", ax, ay, az, 0.55F, 1.0F);
		if (isLocal()) {
			VfxScreen.flash(GOLD, 0.22F, 8);
			VfxScreen.fovPunch(-3.0F);
			CameraShakeManager.getInstance().shake(2.0F, 8);
		}
	}

	private void onPulse(ClientLevel level, int index) {
		pulseTick[pulseCount % MAX_PULSES] = ticks;
		pulseCount++;
		flash = 1.0F;
		int note = ARPEGGIO[Math.floorMod(index, ARPEGGIO.length)];
		VfxSfx.play(level, CHIMES[note], ax, ay, az, 0.55F + 0.03F * Math.min(index, 9), 1.0F);
		if (isLocal()) {
			VfxScreen.flash(HEAL, 0.07F, 5);
			CameraShakeManager.getInstance().shake(0.6F, 4);
		}
	}

	private void onEnd(ClientLevel level) {
		if (ended)
			return;
		ended = true;
		endTick = ticks;
		VfxSfx.play(level, "monk_meditation_end", ax, ay, az, 1.0F, 1.0F);
		if (isLocal()) {
			VfxScreen.flash(GOLD, 0.32F, 12);
			VfxScreen.fovPunch(4.0F);
			CameraShakeManager.getInstance().shake(4.5F, 12);
		}
	}

	@Override
	protected void tick(ClientLevel level) {
		Entity e = level.getEntity(entityId);
		if (e != null) {
			ax = e.getX();
			ay = e.getY();
			az = e.getZ();
		}
		// the caster left or died: fade out quietly
		if (!ended && ticks > 10 && (e == null || !e.isAlive())) {
			ended = true;
			endTick = ticks;
		}
		// safety: the END packet never arrived
		if (!ended && ticks > totalTicks + 40) {
			ended = true;
			endTick = ticks;
		}
		flash *= 0.78F;
		if (ended && ticks - endTick > END_LIFE + 6)
			dead = true;
		if (!ended && isLocal())
			VfxScreen.vignette(TEAL, 0.16F * VfxMath.clamp01(ticks / 10.0F));
	}

	// ---------------------------------------------------------------
	//  Render
	// ---------------------------------------------------------------
	@Override
	public void renderGlow(VfxGfx g, ClientLevel level, float partial) {
		Entity e = level.getEntity(entityId);
		double x = ax, y = ay, z = az;
		if (e != null) {
			Vec3 p = e.getPosition(partial);
			x = p.x;
			y = p.y;
			z = p.z;
		}
		double gy = y + 0.04D;

		float t = ticks + partial;
		float since = ended ? t - endTick : 0.0F;
		float fadeOut = ended ? VfxMath.smooth(0.30F, 1.0F, since / END_LIFE) : 0.0F;
		float master = VfxMath.clamp01(t / 8.0F) * (1.0F - fadeOut);
		float build = VfxMath.easeOut(VfxMath.clamp01(t / 16.0F));
		float collapse = ended ? VfxMath.easeIn(VfxMath.clamp01(since / 14.0F)) : 0.0F;
		float rs = build * (1.0F - 0.85F * collapse);
		float rad = radius * rs;
		float fl = Math.min(1.0F, flash);
		float rot = t * 0.035F;
		float breath = 0.85F + 0.15F * (float) Math.sin(t * 0.21F);

		if (master > 0.003F) {
			ground(g, x, gy, z, rad, master, breath);
			mandala(g, x, gy, z, rad, master * (1.0F + 0.7F * fl), build, rot);
			lotus(g, x, gy, z, t, master, fl, rot, collapse);
			domes(g, x, y, z, rad, master, breath, fl, build, collapse);
			pillars(g, x, y, z, t, master, build, breath);
			rays(g, x, gy, z, t, master);
			spirals(g, x, gy, z, t, master, build, rot);
			// light at the heart of the monk
			g.color(GOLD, WHITE).halo(x, y + 1.0D, z, 1.1D * breath * (1.0D + 0.5D * fl) * (1.0D - 0.8D * collapse), 0.20F * master, 0.0F, g.seg(20));
		}
		startBurst(g, x, y, z, gy, t);
		pulses(g, x, y, z, gy, t, master);
		if (ended)
			release(g, x, y, z, gy, since);
	}

	// ---- layers ----------------------------------------------------

	private void ground(VfxGfx g, double x, double gy, double z, float rad, float master, float breath) {
		g.color(DEEP, TEAL);
		g.disc(x, gy, z, rad * 0.98D, 0.20F * master * breath, 0.0F, g.seg(40));
	}

	private void mandala(VfxGfx g, double x, double gy, double z, float rad, float a, float build, float rot) {
		if (rad < 0.2F)
			return;
		double sweep = VfxMath.TAU * build;

		// outer ring: draws itself around the circle
		g.color(TEAL, WHITE);
		g.arc(x, gy + 0.010D, z, rad - 0.55D, rad, rot, rot + sweep, 0.0F, 0.22F * a, g.seg(64));
		g.arc(x, gy + 0.012D, z, rad - 0.12D, rad, rot, rot + sweep, 0.90F * a, 0.90F * a, g.seg(64));

		// rune ticks, appear in order while the ring is drawn
		int n = 36;
		for (int k = 0; k < n; k++) {
			if ((k + 0.5F) / n > build)
				break;
			boolean major = k % 3 == 0;
			double ang = -rot * 0.7D + k * VfxMath.TAU / n;
			g.spoke(x, gy + 0.020D, z, ang, rad * (major ? 0.84D : 0.89D), rad * 0.96D, major ? 0.05D : 0.03D, 0.80F * a, 0.30F * a);
		}

		// middle ring with the octagram and counter-rotating ticks
		double rm = rad * 0.72D;
		g.color(TEAL, WHITE);
		g.arc(x, gy + 0.014D, z, rm - 0.07D, rm, 0.0D, VfxMath.TAU, 0.70F * a * build, 0.70F * a * build, g.seg(48));
		double base = -rot * 0.6D;
		for (int k = 0; k < 8; k++) {
			double a0 = base + k * VfxMath.TAU / 8.0D;
			double a1 = base + ((k + 3) % 8) * VfxMath.TAU / 8.0D;
			g.flatLine(x + Math.cos(a0) * rm, z + Math.sin(a0) * rm, x + Math.cos(a1) * rm, z + Math.sin(a1) * rm, gy + 0.025D, 0.05D, 0.50F * a * build);
		}
		for (int k = 0; k < 16; k++)
			g.spoke(x, gy + 0.018D, z, rot * 1.4D + k * VfxMath.TAU / 16.0D, rad * 0.60D, rad * 0.68D, 0.03D, 0.60F * a * build, 0.20F * a * build);

		// spokes from the inner ring to the edge
		double ri = rad * 0.45D;
		for (int k = 0; k < 8; k++)
			g.spoke(x, gy + 0.016D, z, rot * 0.5D + k * VfxMath.TAU / 8.0D, ri, rad * 0.88D, 0.04D, 0.35F * a * build, 0.05F * a * build);

		// inner golden ring with four rotating arcs and a soft core
		g.color(GOLD, WHITE);
		g.arc(x, gy + 0.030D, z, ri - 0.07D, ri, 0.0D, VfxMath.TAU, 0.60F * a, 0.60F * a, g.seg(40));
		for (int k = 0; k < 4; k++) {
			double a0 = -rot * 1.6D + k * VfxMath.TAU / 4.0D;
			g.arc(x, gy + 0.032D, z, ri - 0.14D, ri + 0.04D, a0, a0 + 0.9D, 0.90F * a, 0.90F * a, g.seg(10));
		}
		g.disc(x, gy + 0.034D, z, ri * 0.95D, 0.10F * a, 0.0F, g.seg(32));
	}

	private void lotus(VfxGfx g, double x, double gy, double z, float t, float master, float fl, float rot, float collapse) {
		float open = VfxMath.easeOut(VfxMath.clamp01(t / 20.0F));
		float lb = (1.0F + 0.05F * (float) Math.sin(t * 0.2F) + 0.14F * fl) * (1.0F - 0.9F * collapse);
		if (lb < 0.05F)
			return;
		g.color(GOLD, WHITE);
		for (int k = 0; k < 8; k++) {
			double ang = rot * 0.4D + k * Math.PI / 4.0D;
			g.petal(x, gy + 0.02D, z, ang, Math.toRadians(80.0D - 62.0D * open), 1.45D * lb * (0.5D + 0.5D * open), 0.38D * lb, 0.05F * master, 0.75F * master);
		}
		for (int k = 0; k < 8; k++) {
			double ang = rot * 0.4D + Math.PI / 8.0D + k * Math.PI / 4.0D;
			g.petal(x, gy + 0.03D, z, ang, Math.toRadians(88.0D - 42.0D * open), 0.95D * lb * (0.5D + 0.5D * open), 0.26D * lb, 0.05F * master, 0.80F * master);
		}
		g.color(WHITE, WHITE);
		for (int k = 0; k < 6; k++) {
			double ang = -rot * 0.5D + k * Math.PI / 3.0D;
			g.petal(x, gy + 0.04D, z, ang, Math.toRadians(72.0D), 0.55D * lb * open, 0.14D * lb, 0.10F * master, 0.70F * master);
		}
	}

	private void domes(VfxGfx g, double x, double y, double z, float rad, float master, float breath, float fl, float build, float collapse) {
		// inner shell around the monk
		g.color(TEAL, DEEP);
		g.sphere(x, y, z, 1.9D * (1.0D - 0.6D * collapse) * build, 8, g.seg(28), 0.025F * master, (0.55F * breath + 0.30F * fl) * master, 2.4F, true);
		// big sanctuary dome that marks the push area
		g.color(GOLD, TEAL);
		g.sphere(x, y, z, rad, 10, g.seg(36), 0.0F, 0.20F * master * (1.0F + 0.6F * fl), 3.0F, true);
	}

	private void pillars(VfxGfx g, double x, double y, double z, float t, float master, float build, float breath) {
		float startEnv = 1.0F - VfxMath.clamp01(t / 22.0F);
		float rise = VfxMath.easeOut(VfxMath.clamp01(t / 7.0F));
		if (startEnv > 0.0F) {
			g.color(GOLD, WHITE);
			g.cone(x, y, y + 10.0D * rise, z, 0.50D, 0.18D, g.seg(20), 0.90F * startEnv, 0.0F);
			g.color(TEAL, WHITE);
			g.cone(x, y, y + 9.0D * rise, z, 0.90D, 0.40D, g.seg(20), 0.35F * startEnv, 0.0F);
		}
		// soft standing column of calm light
		g.color(TEAL, WHITE);
		double h = 4.2D * build;
		g.cone(x, y, y + h, z, 0.55D, 0.45D, g.seg(20), 0.20F * master * breath, 0.0F);
		g.cone(x, y, y + h, z, 0.32D, 0.26D, g.seg(16), 0.25F * master * breath, 0.0F);
		g.cone(x, y, y + h, z, 0.16D, 0.12D, g.seg(12), 0.35F * master * breath, 0.0F);
	}

	// slivers of light that rise from the ground and fade (geometry, not particles)
	private void rays(VfxGfx g, double x, double gy, double z, float t, float master) {
		g.color(GOLD, WHITE);
		for (int i = 0; i < 18; i++) {
			float period = 34.0F + VfxMath.hash(seed, i + 300) * 30.0F;
			float phase = VfxMath.hash(seed, i + 400) * period;
			float u = ((t + phase) % period) / period;
			double ang = VfxMath.hash(seed, i) * VfxMath.TAU + t * 0.012D;
			double rr = 0.9D + VfxMath.hash(seed, i + 200) * (radius * 0.9D - 0.9D);
			double hgt = 2.4D + VfxMath.hash(seed, i + 500) * 1.8D;
			float a = (float) Math.pow(Math.sin(Math.PI * u), 1.2D) * 0.65F * master;
			double len = 0.7D + 0.5D * VfxMath.hash(seed, i + 600);
			g.sliver(x + Math.cos(ang) * rr, gy + u * hgt, z + Math.sin(ang) * rr, 0.0D, 1.0D, 0.0D, len, 0.035D, a);
		}
	}

	private void spirals(VfxGfx g, double x, double gy, double z, float t, float master, float build, float rot) {
		int n = Math.min(60, g.seg(28));
		for (int h = 0; h < 2; h++) {
			g.stripBegin();
			for (int i = 0; i < n; i++) {
				float u = i / (float) (n - 1);
				double ang = rot * 3.2D + h * Math.PI + u * VfxMath.TAU * 1.6D;
				double rr = 0.80D + 0.12D * Math.sin(u * Math.PI * 2.0D + t * 0.15D);
				g.stripPoint(x + Math.cos(ang) * rr, gy + 0.10D + u * 3.4D, z + Math.sin(ang) * rr);
			}
			g.color(TEAL, WHITE);
			g.stripEnd(0.22D, 0.55F * master * build, 0.05F * master);
		}
	}

	// ---- moments ---------------------------------------------------

	private void startBurst(VfxGfx g, double x, double y, double z, double gy, float t) {
		float u = t / 12.0F;
		if (u >= 1.0F)
			return;
		float a = (1.0F - u) * (1.0F - u);
		float e = VfxMath.easeOut(u);
		g.color(TEAL, WHITE);
		g.wavyRing(x, gy + 0.03D, z, VfxMath.lerp(0.5F, radius * 1.15F, e), VfxMath.lerp(1.0F, 0.2F, u), 0.0F, 0.85F * a, g.seg(56), 0.08D * (1.0F - u), 6.0D, t * 0.4D);
		g.color(GOLD, WHITE);
		g.halo(x, y + 1.0D, z, 1.8D * (1.0F - u), 0.5F * a, 0.0F, g.seg(20));
	}

	private void pulses(VfxGfx g, double x, double y, double z, double gy, float t, float master) {
		int count = Math.min(pulseCount, MAX_PULSES);
		for (int k = 0; k < count; k++) {
			float p = t - pulseTick[k];
			if (p < 0.0F || p > PULSE_LIFE)
				continue;
			float u = p / PULSE_LIFE;
			float e = VfxMath.easeOut(u);
			float a = (float) Math.pow(1.0F - u, 1.5D) * 0.9F;
			double rr = VfxMath.lerp(0.6F, radius, e);

			g.color(HEAL, WHITE);
			g.wavyRing(x, gy + 0.05D, z, rr, VfxMath.lerp(0.9F, 0.18F, u), 0.0F, a, g.seg(56), 0.10D * (1.0F - u), 6.0D, t * 0.5D + k);
			g.color(WHITE, WHITE);
			g.arc(x, gy + 0.06D, z, rr - 0.07D, rr, 0.0D, VfxMath.TAU, 0.0F, 0.85F * a, g.seg(56));
			// a low wall of light on the wavefront gives the shockwave volume
			g.color(HEAL, HEAL);
			g.cone(x, gy, gy + 0.15D + 0.55D * (1.0F - u), z, rr, rr, g.seg(48), 0.40F * a, 0.0F);
			// short column of healing light on the monk
			g.color(HEAL, WHITE);
			g.cone(x, y, y + 2.2D * VfxMath.easeOut(p / 5.0F), z, 0.30D, 0.10D, g.seg(14), 0.50F * (1.0F - u), 0.0F);
		}
	}

	private void release(VfxGfx g, double x, double y, double z, double gy, float p) {
		if (p < 0.0F || p > END_LIFE)
			return;
		float u = p / END_LIFE;
		float e = VfxMath.easeOut(u);
		float a = (float) Math.pow(1.0F - u, 1.4D);

		g.color(GOLD, WHITE);
		g.wavyRing(x, gy + 0.05D, z, VfxMath.lerp(0.8F, radius * 1.5F, e), VfxMath.lerp(1.6F, 0.3F, u), 0.0F, 0.95F * a, g.seg(64), 0.14D * (1.0F - u), 8.0D, p * 0.5D);
		if (p > 4.0F) {
			float u2 = (p - 4.0F) / (END_LIFE - 4.0F);
			float e2 = VfxMath.easeOut(u2);
			g.color(WHITE, WHITE);
			g.arc(x, gy + 0.07D, z, VfxMath.lerp(0.5F, radius * 1.1F, e2) - 0.14D, VfxMath.lerp(0.5F, radius * 1.1F, e2), 0.0D, VfxMath.TAU, 0.0F, 0.9F * (1.0F - u2), g.seg(64));
		}
		g.color(GOLD, GOLD);
		g.cone(x, gy, gy + 0.2D + 1.1D * (1.0F - u), z, VfxMath.lerp(0.8F, radius * 1.5F, e), VfxMath.lerp(0.8F, radius * 1.5F, e), g.seg(56), 0.45F * a, 0.0F);

		g.color(GOLD, WHITE);
		g.cone(x, y, y + 11.0D * VfxMath.easeOut(p / 5.0F), z, 0.80D * (1.0F - 0.6F * u), 0.25D, g.seg(22), 0.85F * (1.0F - u), 0.0F);
		g.burst(x, y + 1.1D, z, 14, 3.4D * VfxMath.easeOut(Math.min(1.0F, u * 2.0F)), 0.14D * (1.0F - u), a, p * 0.05D);
		g.halo(x, y + 1.1D, z, VfxMath.lerp(0.5F, 4.0F, e), 0.7F * a * a, 0.0F, g.seg(24));
	}
}
