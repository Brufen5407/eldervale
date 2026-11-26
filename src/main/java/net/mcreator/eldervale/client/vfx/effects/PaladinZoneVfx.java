package net.mcreator.eldervale.client.vfx.effects;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxGfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/** Paladin skill 2: HOLY ZONE. A blessed area on the ground (radius 5) and a halo on every ally inside it. */
public final class PaladinZoneVfx extends ScriptedVfx {

	private static final int DURATION = 100;

	private final double zx, zy, zz;
	private final List<Integer> allies = new ArrayList<>();

	public PaladinZoneVfx(VfxEventPayload p) {
		super(VfxIds.PALADIN_ZONE, p, 5.0F);
		this.follow = false;
		this.zx = p.x();
		this.zy = p.y() + 0.04D;
		this.zz = p.z();
		final float R = this.radius;

		// cast burst
		layer(0, 18, (g, f, u) -> VfxKit.pillar(g, f.x, f.y, f.z, 12.0D, 0.7D, u, VfxKit.GOLD, VfxKit.WHITE, 1.0F));
		layer(0, 16, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, R * 1.15F, u, 1.1D, VfxKit.GOLD, VfxKit.WHITE, 1.0F, 0.1D));
		layer(5, 14, (g, f, u) -> VfxKit.shock(g, f.x, f.y, f.z, R, u, 0.5D, VfxKit.HOLY, VfxKit.WHITE, 0.7F, 0.06D));

		// the zone: hexagram glyph that draws itself, dome rim, rising rays, soft central column
		layer(0, DURATION + 14, (g, f, u) -> {
			float build = VfxMath.easeOut(VfxMath.clamp01(f.t / 16.0F));
			float out = 1.0F - VfxMath.smooth(DURATION - 10.0F, DURATION + 14.0F, f.t);
			float pulse = 0.85F + 0.15F * (float) Math.sin(f.t * 0.18F);
			float a = out * pulse;
			g.color(VfxKit.GOLD, VfxKit.TEAL);
			g.disc(f.x, f.y, f.z, R * build, 0.16F * a, 0.0F, g.seg(40));
			VfxKit.glyph(g, f.x, f.y, f.z, R * build, a, f.t * 0.03F, 6, 2, VfxKit.GOLD, VfxKit.WHITE);
			VfxKit.dome(g, f.x, f.y, f.z, R * build, 0.22F * a, 0.0F, VfxKit.GOLD, VfxKit.HOLY);
			VfxKit.rays(g, f.x, f.y, f.z, R, 16, f.t, f.seed, a, VfxKit.GOLD, VfxKit.WHITE);
			g.color(VfxKit.GOLD, VfxKit.WHITE);
			g.cone(f.x, f.y, f.y + 4.5D * build, f.z, 0.5D, 0.4D, g.seg(18), 0.22F * a, 0.0F);
			g.halo(f.x, f.y + 1.0D, f.z, 1.5D * pulse, 0.18F * a, 0.0F, g.seg(16));
		});

		// halos over the allies (live positions)
		layer(4, DURATION - 4, (g, f, u) -> {
			ClientLevel level = Minecraft.getInstance().level;
			if (level == null)
				return;
			float a = VfxMath.smooth(0.0F, 0.08F, u) * (1.0F - VfxMath.smooth(0.85F, 1.0F, u));
			for (int id : allies) {
				Entity e = level.getEntity(id);
				if (e == null)
					continue;
				double ex = e.getX(), ey = e.getY(), ez = e.getZ();
				g.color(VfxKit.GOLD, VfxKit.WHITE);
				g.arc(ex, ey + 0.05D, ez, 0.55D, 0.7D, f.t * 0.1D, f.t * 0.1D + VfxMath.TAU, 0.0F, 0.7F * a, g.seg(24));
				g.cone(ex, ey, ey + 2.0D, ez, 0.5D, 0.45D, g.seg(14), 0.20F * a, 0.0F);
				g.halo(ex, ey + 1.0D, ez, 1.3D, 0.14F * a, 0.0F, g.seg(14));
			}
		});

		sound(0, "paladin_zone_cast", 1.0F, 1.0F);
		sound(40, "paladin_zone_loop", 0.5F, 1.0F);
		sound(DURATION, "paladin_zone_end", 0.8F, 1.0F);
		flash(0, VfxKit.GOLD, 0.20F, 10);
		fov(0, 3.0F);
		vignette(0, VfxKit.GOLD, 0.10F, 60);
		shake(0, 2.0F, 8, 18.0F, false);
		holdUntil(DURATION + 16);
	}

	@Override
	protected void onEvent(VfxEventPayload p, ClientLevel level) {
		if (p.phase() != VfxIds.START)
			return;
		// allies = players inside the zone box when it was cast (same box the server uses)
		for (Player o : level.players()) {
			if (Math.abs(o.getX() - zx) <= 5.0D && Math.abs(o.getY() - zy) <= 5.0D && Math.abs(o.getZ() - zz) <= 5.0D)
				allies.add(o.getId());
		}
	}
}
