package net.mcreator.eldervale.client.vfx;

import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.vfx.VfxEventPayload;

/**
 * Base class of every visual effect. One instance = one effect playing on this client.
 * The VfxManager ticks it (20 times per second) and renders it every frame.
 */
public abstract class VfxEffect {

	public final int vfxId;
	public final int entityId;

	/** Client ticks since the effect was created. */
	protected int ticks;
	/** Set to true when the effect has finished: the manager removes it. */
	protected boolean dead;

	protected VfxEffect(int vfxId, int entityId) {
		this.vfxId = vfxId;
		this.entityId = entityId;
	}

	final void tickBase(ClientLevel level) {
		ticks++;
		tick(level);
	}

	public final boolean isDead() {
		return dead;
	}

	/** Logic every client tick (timers, sounds, screen effects). Do not draw here. */
	protected abstract void tick(ClientLevel level);

	/** A new phase arrived from the server (START, PULSE, IMPACT, END). */
	public abstract void event(VfxEventPayload payload, ClientLevel level);

	/** Draw the glowing (additive) shapes. Called every frame. */
	public abstract void renderGlow(VfxGfx g, ClientLevel level, float partial);

	/** Draw the dark (translucent) shapes. Optional. */
	public void renderShade(VfxGfx g, ClientLevel level, float partial) {
	}

	/** True if renderShade draws something (lets the manager skip the second pass). */
	public boolean usesShade() {
		return false;
	}
}
