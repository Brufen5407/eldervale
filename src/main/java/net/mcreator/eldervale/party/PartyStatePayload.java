package net.mcreator.eldervale.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
 * Server -> client: snapshot of the receiver's party (members with class, level, hp, mana, distance)
 * and of the pending invite, if any. Sent when something changes and every few ticks to party members.
 */
@EventBusSubscriber
public record PartyStatePayload(boolean inParty, String partyName, UUID leader, int maxMembers, List<MemberInfo> members, String inviteParty, String inviteFrom, int inviteSeconds) implements CustomPacketPayload {

	/** distance is -1 when the member is offline or in another dimension. */
	public record MemberInfo(UUID id, String name, String playerClass, int level, boolean online, float hp, float hpMax, float mana, float manaMax, float distance) {
	}

	public static final CustomPacketPayload.Type<PartyStatePayload> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("eldervale", "party_state"));
	public static final StreamCodec<FriendlyByteBuf, PartyStatePayload> STREAM_CODEC = StreamCodec.of((FriendlyByteBuf buf, PartyStatePayload m) -> {
		buf.writeBoolean(m.inParty());
		buf.writeUtf(m.partyName(), 32);
		buf.writeUUID(m.leader());
		buf.writeVarInt(m.maxMembers());
		buf.writeVarInt(m.members().size());
		for (MemberInfo i : m.members()) {
			buf.writeUUID(i.id());
			buf.writeUtf(i.name(), 32);
			buf.writeUtf(i.playerClass(), 32);
			buf.writeVarInt(i.level());
			buf.writeBoolean(i.online());
			buf.writeFloat(i.hp());
			buf.writeFloat(i.hpMax());
			buf.writeFloat(i.mana());
			buf.writeFloat(i.manaMax());
			buf.writeFloat(i.distance());
		}
		buf.writeUtf(m.inviteParty(), 32);
		buf.writeUtf(m.inviteFrom(), 32);
		buf.writeVarInt(m.inviteSeconds());
	}, (FriendlyByteBuf buf) -> {
		boolean inParty = buf.readBoolean();
		String partyName = buf.readUtf(32);
		UUID leader = buf.readUUID();
		int max = buf.readVarInt();
		int count = Math.min(buf.readVarInt(), 16);
		List<MemberInfo> members = new ArrayList<>();
		for (int k = 0; k < count; k++)
			members.add(new MemberInfo(buf.readUUID(), buf.readUtf(32), buf.readUtf(32), buf.readVarInt(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
		return new PartyStatePayload(inParty, partyName, leader, max, members, buf.readUtf(32), buf.readUtf(32), buf.readVarInt());
	});

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	public static void handleData(final PartyStatePayload data, final IPayloadContext context) {
		context.enqueueWork(() -> PartyClientState.update(data));
	}

	@SubscribeEvent
	public static void register(final RegisterPayloadHandlersEvent event) {
		final PayloadRegistrar registrar = event.registrar("1");
		registrar.playToClient(TYPE, STREAM_CODEC, PartyStatePayload::handleData);
	}
}
