package net.mcreator.eldervale.client.vfx;

import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.client.multiplayer.ClientLevel;

import net.mcreator.eldervale.vfx.VfxSounds;

/** Plays the VFX sounds on this client, with a small random pitch variation. */
public final class VfxSfx {

	public static void play(ClientLevel level, String name, double x, double y, double z, float volume, float pitch) {
		if (!VfxManager.SOUNDS_ENABLED || level == null)
			return;
		SoundEvent sound = VfxSounds.get(name);
		if (sound == null)
			return;
		float varied = pitch * (0.97F + level.random.nextFloat() * 0.06F);
		level.playLocalSound(x, y, z, sound, SoundSource.PLAYERS, volume, varied, false);
	}

	private VfxSfx() {
	}
}
