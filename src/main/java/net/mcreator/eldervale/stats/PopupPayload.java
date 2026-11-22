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

/**
 * Server -> one client: "show a floating text at this position". Purely visual.
 * kind CRIT = "CRITICAL!", kind XP = "+amount XP". Replaces CritFeedbackPayload.
 */
public record PopupPayload(double x, double y, double z, int kind, int amount) implements CustomPacketPayload {
	public static final int CRIT = 0;
	public static final int XP = 1;

	public static final Type<PopupPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EldervaleMod.MODID, "popup"));
	public static final StreamCodec<FriendlyByteBuf, PopupPayload> STREAM_CODEC = StreamCodec.of((buf, msg) -> {
		buf.writeDouble(msg.x());
		buf.writeDouble(msg.y());
		buf.writeDouble(msg.z());
		buf.writeVarInt(msg.kind());
		buf.writeVarInt(msg.amount());
	}, buf -> new PopupPayload(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readVarInt(), buf.readVarInt()));

	@Override
	public Type<PopupPayload> type() {
		return TYPE;
	}

	public static void handle(PopupPayload message, IPayloadContext context) {
		context.enqueueWork(() -> PopupState.add(message.x(), message.y(), message.z(), message.kind(), message.amount()));
	}

	@EventBusSubscriber
	public static class Registration {
		@SubscribeEvent
		public static void register(RegisterPayloadHandlersEvent event) {
			PayloadRegistrar registrar = event.registrar("1");
			registrar.playToClient(TYPE, STREAM_CODEC, PopupPayload::handle);
		}
	}
}
