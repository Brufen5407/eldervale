package net.mcreator.eldervale.stats;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.eldervale.EldervaleMod;

/**
 * Client -> server. attribute = StatAttribute index (-1 = "just send me my points", used when the GUI opens).
 * The server validates everything; the client never writes stats.
 */
public record AttributeSpendPayload(int attribute, int amount) implements CustomPacketPayload {
	public static final Type<AttributeSpendPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EldervaleMod.MODID, "attribute_spend"));
	public static final StreamCodec<FriendlyByteBuf, AttributeSpendPayload> STREAM_CODEC = StreamCodec.of((buf, msg) -> {
		buf.writeVarInt(msg.attribute() + 1);
		buf.writeVarInt(msg.amount());
	}, buf -> new AttributeSpendPayload(buf.readVarInt() - 1, buf.readVarInt()));

	@Override
	public Type<AttributeSpendPayload> type() {
		return TYPE;
	}

	public static void handle(AttributeSpendPayload message, IPayloadContext context) {
		context.enqueueWork(() -> {
			if (!(context.player() instanceof ServerPlayer player))
				return;
			StatAttribute attribute = StatAttribute.fromIndex(message.attribute());
			if (attribute != null)
				StatCalculator.spend(player, attribute, message.amount());
			else
				StatsNetwork.sync(player);
		});
	}

	@EventBusSubscriber
	public static class Registration {
		@SubscribeEvent
		public static void register(RegisterPayloadHandlersEvent event) {
			PayloadRegistrar registrar = event.registrar("1");
			registrar.playToServer(TYPE, STREAM_CODEC, AttributeSpendPayload::handle);
		}
	}
}
