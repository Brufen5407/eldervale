package net.mcreator.eldervale.vfx;

import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

/**
 * Server side helper: sends a VFX event to the players that can see it.
 * It only sends a packet. It never changes gameplay, so it is safe to call from any skill class.
 */
public final class VfxServer {

	private static final double RANGE = 96.0D;

	/** Event that follows the caster. 'arg' and 'param' are free values (see VfxEventPayload). */
	public static void send(ServerPlayer owner, int vfxId, int phase, int arg, float param) {
		Vec3 look = owner.getLookAngle();
		VfxEventPayload payload = new VfxEventPayload(vfxId, phase, arg, owner.getId(), owner.getRandom().nextInt(), owner.getX(), owner.getY(), owner.getZ(), param, (float) look.x, (float) look.y, (float) look.z);
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(owner, payload);
	}

	/** Event with a world position (impact point, target...). Sent to the players near that point. */
	public static void sendAt(ServerPlayer owner, int vfxId, int phase, int arg, double x, double y, double z, float param) {
		Vec3 look = owner.getLookAngle();
		VfxEventPayload payload = new VfxEventPayload(vfxId, phase, arg, owner.getId(), owner.getRandom().nextInt(), x, y, z, param, (float) look.x, (float) look.y, (float) look.z);
		PacketDistributor.sendToPlayersNear(owner.serverLevel(), null, x, y, z, RANGE, payload);
	}

	/** Same, with an explicit direction (for example the direction of a dash). */
	public static void sendAt(ServerPlayer owner, int vfxId, int phase, int arg, double x, double y, double z, float param, double dx, double dy, double dz) {
		VfxEventPayload payload = new VfxEventPayload(vfxId, phase, arg, owner.getId(), owner.getRandom().nextInt(), x, y, z, param, (float) dx, (float) dy, (float) dz);
		PacketDistributor.sendToPlayersNear(owner.serverLevel(), null, x, y, z, RANGE, payload);
	}

	/** Event not tied to a player (monster effects, thrown potions...). 'owner' is only used for the entity id. */
	public static void sendAt(ServerLevel level, int owner, int vfxId, int phase, int arg, double x, double y, double z, float param) {
		VfxEventPayload payload = new VfxEventPayload(vfxId, phase, arg, owner, level.getRandom().nextInt(), x, y, z, param, 0.0F, 1.0F, 0.0F);
		PacketDistributor.sendToPlayersNear(level, null, x, y, z, RANGE, payload);
	}

	private VfxServer() {
	}
}
