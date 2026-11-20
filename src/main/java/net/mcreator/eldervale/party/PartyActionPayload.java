package net.mcreator.eldervale.party;

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

/**
 * Client -> server: one action of the Party GUI (create, invite, accept...).
 * The server never trusts the client: every rule is checked again in PartyManager.
 */
@EventBusSubscriber
public record PartyActionPayload(int action, String arg) implements CustomPacketPayload {
	public static final int SYNC = 0;      // just send me the current state
	public static final int CREATE = 1;    // arg = party name
	public static final int INVITE = 2;    // arg = player name
	public static final int ACCEPT = 3;
	public static final int DECLINE = 4;
	public static final int LEAVE = 5;
	public static final int DISBAND = 6;
	public static final int KICK = 7;      // arg = member name
	public static final int LEADER = 8;    // arg = member name
	public static final int RENAME = 9;    // arg = new party name

	public static final CustomPacketPayload.Type<PartyActionPayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("eldervale", "party_action"));
	public static final StreamCodec<FriendlyByteBuf, PartyActionPayload> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buf, PartyActionPayload m) -> {
		buf.writeVarInt(m.action());
		buf.writeUtf(m.arg(), 64);
	}, (FriendlyByteBuf buf) -> new PartyActionPayload(buf.readVarInt(), buf.readUtf(64)));

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handleData(final PartyActionPayload data, final IPayloadContext context) {
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player)
				PartySync.handleAction(player, data.action(), data.arg());
		});
	}

	@SubscribeEvent
	public static void register(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");
		registrar.playToServer(TYPE, STREAM_CODEC, PartyActionPayload::handleData);
	}
}
