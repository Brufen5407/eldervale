package net.mcreator.eldervale.vfx;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.eldervale.client.vfx.VfxManager;

/**
 * One small packet that tells clients "play this effect, in this phase".
 * The server sends it only to the players that track the caster (see VfxServer).
 * The client builds the whole visual from these values, so nothing else is synced.
 *
 *  - vfxId / phase : which effect and which moment (VfxIds)
 *  - arg           : free integer (pulse index, total duration, ...)
 *  - entityId      : the entity the effect follows (the caster)
 *  - seed          : same seed for every client, so everyone sees the same shapes
 *  - x,y,z         : position where it happens
 *  - param         : free float (radius, power, ...)
 *  - dx,dy,dz      : direction (look vector of the caster)
 */
@EventBusSubscriber
public record VfxEventPayload(int vfxId, int phase, int arg, int entityId, int seed, double x, double y, double z, float param, float dx, float dy, float dz) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<VfxEventPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("eldervale", "vfx_event"));
	public static final StreamCodec<FriendlyByteBuf, VfxEventPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> p.write(buf), VfxEventPayload::read);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	private void write(FriendlyByteBuf buf) {
		buf.writeVarInt(vfxId);
		buf.writeByte(phase);
		buf.writeVarInt(arg);
		buf.writeVarInt(entityId);
		buf.writeInt(seed);
		buf.writeDouble(x);
		buf.writeDouble(y);
		buf.writeDouble(z);
		buf.writeFloat(param);
		buf.writeFloat(dx);
		buf.writeFloat(dy);
		buf.writeFloat(dz);
	}

	private static VfxEventPayload read(FriendlyByteBuf buf) {
		int vfxId = buf.readVarInt();
		int phase = buf.readByte();
		int arg = buf.readVarInt();
		int entityId = buf.readVarInt();
		int seed = buf.readInt();
		double x = buf.readDouble();
		double y = buf.readDouble();
		double z = buf.readDouble();
		float param = buf.readFloat();
		float dx = buf.readFloat();
		float dy = buf.readFloat();
		float dz = buf.readFloat();
		return new VfxEventPayload(vfxId, phase, arg, entityId, seed, x, y, z, param, dx, dy, dz);
	}

	public static void handleData(final VfxEventPayload data, final IPayloadContext context) {
		context.enqueueWork(() -> VfxManager.handle(data));
	}

	@SubscribeEvent
	public static void register(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");
		registrar.playToClient(TYPE, STREAM_CODEC, VfxEventPayload::handleData);
	}
}
