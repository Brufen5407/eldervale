package net.mcreator.eldervale.client.vfx.effects;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxGfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/**
 * Ranger skill 2: MAGIC ARROWS. Backward leap with an arcane circle and wind streaks,
 * then a muzzle flash for each of the 3 arrows (PULSE events) and a glowing trail that follows every arrow.
 */
public final class RangerArrowsVfx extends ScriptedVfx {

	private static final class Track {
		final int id;
		final Trail trail = new Trail(10);
		double lx, ly, lz;
		boolean gone;
		int goneTicks;

		Track(int id, double x, double y, double z) {
			this.id = id;
			this.lx = x;
			this.ly = y;
			this.lz = z;
			trail.push(x, y, z);
		}
	}

	private final List<Track> tracks = new ArrayList<>();

	public RangerArrowsVfx(VfxEventPayload p) {
		super(VfxIds.RANGER_ARROWS, p, 3.0F);
		final double sx = p.x(), sy = p.y() + 0.04D, sz = p.z();

		// arcane circle stamped under the feet where the ranger took off
		layer(0, 22, (g, f, u) -> {
			float a = (1.0F - VfxMath.smooth(0.5F, 1.0F, u)) * VfxMath.clamp01(u * 8.0F);
			VfxKit.glyph(g, sx, sy, sz, 1.9D, 0.85F * a, f.t * 0.12F, 5, 2, VfxKit.LEAF, VfxKit.WHITE);
		});
		layer(0, 12, (g, f, u) -> VfxKit.shock(g, sx, sy, sz, 3.2F, u, 0.7D, VfxKit.LEAF, VfxKit.WHITE, 0.8F, 0.06D));
		// the leap: ribbons behind the body and wind streaks
		layer(0, 18, (g, f, u) -> {
			float a = 1.0F - VfxMath.smooth(0.4F, 1.0F, u);
			VfxKit.trail(g, f.trail, 14, 0.5D, 0.8F * a, 0.0F, VfxKit.LEAF, VfxKit.WHITE, 0.9D);
			VfxKit.trail(g, f.trail, 12, 0.3D, 0.7F * a, 0.0F, VfxKit.TEAL, VfxKit.WHITE, 0.4D);
			g.color(VfxKit.LEAF, VfxKit.WHITE);
			for (int k = 0; k < 6; k++) {
				float h = VfxMath.hash(f.seed, k);
				double side = (VfxMath.hash(f.seed, k + 9) - 0.5D) * 1.8D;
				double up = 0.3D + h * 1.4D;
				double back = 0.5D + h * 1.5D;
				g.sliver(f.x + f.hx * back - f.hz * side, f.y + up, f.z + f.hz * back + f.hx * side, f.hx, 0.0D, f.hz, 1.2D, 0.025D, 0.6F * a);
			}
		});

		sound(0, "ranger_arrows_cast", 1.0F, 1.0F);
		flash(0, VfxKit.LEAF, 0.08F, 5);
		fov(0, 4.0F);
		shake(0, 1.0F, 5, 12.0F, false);
		holdUntil(30);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() != VfxIds.PULSE)
			return;
		// one arrow left the bow: muzzle flash + start tracking the arrow entity
		final int t0 = now();
		final double ox = p.x(), oy = p.y(), oz = p.z();
		final double dx = p.dx(), dy = p.dy(), dz = p.dz();
		final int index = (int) p.param();

		layer(t0, 7, (g, f, u) -> {
			float a = 1.0F - u;
			g.color(VfxKit.LEAF, VfxKit.WHITE);
			g.halo(ox, oy, oz, 1.0D * VfxMath.easeOut(u) + 0.2D, 0.7F * a * a, 0.0F, g.seg(14));
			g.beam(ox, oy, oz, ox + dx * 2.2D, oy + dy * 2.2D, oz + dz * 2.2D, 0.14D * a, 0.9F * a, 0.0F);
			g.burst(ox, oy, oz, 6, 0.8D * VfxMath.easeOut(u), 0.05D * a, 0.7F * a, index * 1.1D);
		});
		sound(t0, "ranger_arrow_shot", 0.8F, 0.95F + index * 0.12F);

		if (p.arg() > 0) {
			Entity e = level.getEntity(p.arg());
			tracks.add(new Track(p.arg(), e != null ? e.getX() : ox, e != null ? e.getY() : oy, e != null ? e.getZ() : oz));
			holdUntil(t0 + 40);
		}
	}

	@Override
	protected void onTick(ClientLevel level) {
		for (int i = tracks.size() - 1; i >= 0; i--) {
			Track t = tracks.get(i);
			Entity e = level.getEntity(t.id);
			if (e != null && !t.gone) {
				t.lx = e.getX();
				t.ly = e.getY();
				t.lz = e.getZ();
				t.trail.push(t.lx, t.ly, t.lz);
			} else if (!t.gone) {
				// the arrow disappeared: it hit something here
				t.gone = true;
				soundHit(t);
			} else if (++t.goneTicks > 8) {
				tracks.remove(i);
			}
		}
	}

	private void soundHit(Track t) {
		net.mcreator.eldervale.client.vfx.VfxSfx.play(net.minecraft.client.Minecraft.getInstance().level, "ranger_arrow_hit", t.lx, t.ly, t.lz, 0.7F, 1.0F);
	}

	// the arrows are drawn by a permanent layer that walks the track list
	{
		layer(0, 400, (g, f, u) -> {
			for (int i = 0; i < tracks.size(); i++) {
				Track t = tracks.get(i);
				float fade = t.gone ? 1.0F - t.goneTicks / 8.0F : 1.0F;
				if (fade <= 0.0F)
					continue;
				VfxKit.trail(g, t.trail, 10, 0.32D, 0.9F * fade, 0.0F, VfxKit.LEAF, VfxKit.WHITE, 0.0D);
				if (!t.gone)
					VfxKit.orb(g, t.lx, t.ly, t.lz, 0.14D, 0.9F, VfxKit.LEAF, VfxKit.WHITE);
				else {
					g.color(VfxKit.LEAF, VfxKit.WHITE);
					float k = t.goneTicks / 8.0F;
					g.halo(t.lx, t.ly, t.lz, 0.4D + 0.9D * k, 0.6F * (1.0F - k) * (1.0F - k), 0.0F, g.seg(12));
					g.burst(t.lx, t.ly, t.lz, 7, 0.9D * VfxMath.easeOut(k), 0.05D * (1.0F - k), 0.8F * (1.0F - k), t.id * 0.7D);
				}
			}
		});
	}
}
