package net.mcreator.eldervale.party;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Server -> client: a short notification shown as a toast (invite received, member joined...).
 * kind decides icon, color and sound on the client.
 */
@EventBusSubscriber
public record PartyNotifyPayload(int kind, String title, String text) implements CustomPacketPayload {
	public static final int INFO = 0;
	public static final int SUCCESS = 1;
	public static final int ERROR = 2;
	public static final int INVITE = 3;
	public static final int JOIN = 4;
	public static final int LEAVE = 5;
	public static final int CROWN = 6;

	public static final CustomPacketPayload.Type<PartyNotifyPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("eldervale", "party_notify"));
	public static final StreamCodec<FriendlyByteBuf, PartyNotifyPayload> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buf, PartyNotifyPayload m) -> {
		buf.writeVarInt(m.kind());
		buf.writeUtf(m.title(), 48);
		buf.writeUtf(m.text(), 160);
	}, (FriendlyByteBuf buf) -> new PartyNotifyPayload(buf.readVarInt(), buf.readUtf(48), buf.readUtf(160)));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handleData(final PartyNotifyPayload data, final IPayloadContext context) {
		context.enqueueWork(() -> PartyClientState.addToast(data.kind(), data.title(), data.text()));
	}

	@SubscribeEvent
	public static void register(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");
		registrar.playToClient(TYPE, STREAM_CODEC, PartyNotifyPayload::handleData);
	}
}
