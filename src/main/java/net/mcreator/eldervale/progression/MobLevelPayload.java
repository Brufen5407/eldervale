package net.mcreator.eldervale.progression;

import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;

import net.mcreator.eldervale.EldervaleMod;
import net.mcreator.eldervale.procedures.MobScalingProcedure;

/**
 * Server -> client. Tells a player the level of a mob when the player starts tracking (seeing) it.
 * Needed because entity persistent data is not synchronised: the label renderer reads ClientMobLevels.
 */
public record MobLevelPayload(int entityId, int level) implements CustomPacketPayload {
	public static final Type<MobLevelPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EldervaleMod.MODID, "mob_level"));
	public static final StreamCodec<FriendlyByteBuf, MobLevelPayload> STREAM_CODEC = StreamCodec.of((buf, msg) -> {
		buf.writeVarInt(msg.entityId());
		buf.writeVarInt(msg.level());
	}, buf -> new MobLevelPayload(buf.readVarInt(), buf.readVarInt()));

	@Override
	public Type<MobLevelPayload> type() {
		return TYPE;
	}

	public static void handle(MobLevelPayload message, IPayloadContext context) {
		context.enqueueWork(() -> ClientMobLevels.set(message.entityId(), message.level()));
	}

	@EventBusSubscriber
	public static class Registration {
		@SubscribeEvent
		public static void register(RegisterPayloadHandlersEvent event) {
			PayloadRegistrar registrar = event.registrar("1");
			registrar.playToClient(TYPE, STREAM_CODEC, MobLevelPayload::handle);
		}
	}

	/** Runs for every player that starts seeing a mob (new spawn, chunk reload, entering range). */
	@EventBusSubscriber
	public static class Sync {
		@SubscribeEvent
		public static void onStartTracking(PlayerEvent.StartTracking event) {
			if (!(event.getEntity() instanceof ServerPlayer player))
				return;
			if (!(event.getTarget() instanceof Monster mob))
				return;
			int level = mob.getPersistentData().getInt(MobScalingProcedure.TAG_LEVEL);
			if (level > 0)
				PacketDistributor.sendToPlayer(player, new MobLevelPayload(mob.getId(), level));
		}
	}
}
