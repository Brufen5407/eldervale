package net.mcreator.eldervale.client.vfx.effects;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.Minecraft;

import net.mcreator.eldervale.client.vfx.ScriptedVfx;
import net.mcreator.eldervale.client.vfx.VfxKit;
import net.mcreator.eldervale.client.vfx.VfxMath;
import net.mcreator.eldervale.vfx.VfxEventPayload;
import net.mcreator.eldervale.vfx.VfxIds;

/** Spellblade passive: ARCANE CONVERSION. A spark of mana flies from the enemy that was hit to the Spellblade and a ring closes on him. */
public final class SpellbladeConversionVfx extends ScriptedVfx {

	private static final int FLIGHT = 6;

	public SpellbladeConversionVfx(VfxEventPayload p) {
		super(VfxIds.SPELLBLADE_CONVERSION, p, 1.0F);
		final double hx = p.x(), hy = p.y(), hz = p.z();
		final int ownerId = p.entityId();

		layer(0, FLIGHT, (g, f, u) -> {
			Entity e = Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getEntity(ownerId);
			double tx = e != null ? e.getX() : f.x, ty = e != null ? e.getY() + 1.1D : f.y + 1.1D, tz = e != null ? e.getZ() : f.z;
			float k = VfxMath.easeIn(u);
			double x = hx + (tx - hx) * k, y = hy + (ty - hy) * k + 0.4D * Math.sin(Math.PI * k), z = hz + (tz - hz) * k;
			VfxKit.orb(g, x, y, z, 0.12D, 1.0F, VfxKit.ARCANE_BLUE, VfxKit.WHITE);
			g.color(VfxKit.ARCANE_BLUE, VfxKit.WHITE);
			double px = hx + (tx - hx) * Math.max(0.0F, k - 0.25F), py = hy + (ty - hy) * Math.max(0.0F, k - 0.25F), pz = hz + (tz - hz) * Math.max(0.0F, k - 0.25F);
			g.beam(px, py, pz, x, y, z, 0.05D, 0.0F, 0.8F);
		});
		layer(FLIGHT, 10, (g, f, u) -> {
			float a = 1.0F - u;
			g.color(VfxKit.ARCANE_BLUE, VfxKit.WHITE);
			g.arc(f.x, f.y + 0.05D, f.z, 1.2D * (1.0F - VfxMath.easeOut(u)) + 0.2D, 1.3D * (1.0F - VfxMath.easeOut(u)) + 0.3D, 0.0D, VfxMath.TAU, 0.0F, 0.8F * a, g.seg(28));
			g.halo(f.x, f.y + 1.1D, f.z, 1.2D * VfxMath.easeOut(u), 0.5F * a * a, 0.0F, g.seg(12));
		});
		sound(FLIGHT - 1, "spellblade_conversion", 0.6F, 1.0F);
	}
}
