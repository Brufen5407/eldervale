package net.mcreator.eldervale.vfx;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server side helper: sends a VFX event to the players that can see the caster (and to the caster).
 * It only sends a packet. It never changes gameplay, so it is safe to call from any skill class.
 */
public final class VfxServer {

	/** Event that follows the caster. 'arg' and 'param' are free values (see VfxEventPayload). */
	public static void send(ServerPlayer owner, int vfxId, int phase, int arg, float param) {
		Vec3 look = owner.getLookAngle();
		VfxEventPayload payload = new VfxEventPayload(vfxId, phase, arg, owner.getId(), owner.getRandom().nextInt(), owner.getX(), owner.getY(), owner.getZ(), param, (float) look.x, (float) look.y, (float) look.z);
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(owner, payload);
	}

	private VfxServer() {
	}
}
