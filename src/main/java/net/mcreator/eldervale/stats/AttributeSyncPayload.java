package net.mcreator.eldervale.stats;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.eldervale.EldervaleMod;

/** Server -> owning client only: invested points per attribute. Final stats travel via PlayerVariables as today. */
public record AttributeSyncPayload(int[] points) implements CustomPacketPayload {
	public static final Type<AttributeSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EldervaleMod.MODID, "attribute_sync"));
	public static final StreamCodec<FriendlyByteBuf, AttributeSyncPayload> STREAM_CODEC = StreamCodec.of((buf, msg) -> {
		buf.writeVarInt(msg.points().length);
		for (int value : msg.points())
			buf.writeVarInt(value);
	}, buf -> {
		int length = Math.min(buf.readVarInt(), StatAttribute.COUNT);
		int[] values = new int[length];
		for (int i = 0; i < length; i++)
			values[i] = buf.readVarInt();
		return new AttributeSyncPayload(values);
	});

	@Override
	public Type<AttributeSyncPayload> type() {
		return TYPE;
	}

	public static void handle(AttributeSyncPayload message, IPayloadContext context) {
		context.enqueueWork(() -> ClientStatsState.set(message.points()));
	}

	@EventBusSubscriber
	public static class Registration {
		@SubscribeEvent
		public static void register(RegisterPayloadHandlersEvent event) {
			PayloadRegistrar registrar = event.registrar("1");
			registrar.playToClient(TYPE, STREAM_CODEC, AttributeSyncPayload::handle);
		}
	}
}
