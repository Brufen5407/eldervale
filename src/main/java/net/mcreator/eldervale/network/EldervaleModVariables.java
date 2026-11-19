package net.mcreator.eldervale.network;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.client.model.EntityModel;

import net.mcreator.eldervale.EldervaleMod;

import java.util.function.Supplier;

@EventBusSubscriber
public class EldervaleModVariables {
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, EldervaleMod.MODID);
	public static final Supplier<AttachmentType<PlayerVariables>> PLAYER_VARIABLES = ATTACHMENT_TYPES.register("player_variables", () -> AttachmentType.serializable(() -> new PlayerVariables()).build());
	public static EntityModel paladin_shield = null;
	public static ResourceLocation paladin_shield_texture = null;

	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
		EldervaleMod.addNetworkMessage(SavedDataSyncMessage.TYPE, SavedDataSyncMessage.STREAM_CODEC, SavedDataSyncMessage::handleData);
		EldervaleMod.addNetworkMessage(PlayerVariablesSyncMessage.TYPE, PlayerVariablesSyncMessage.STREAM_CODEC, PlayerVariablesSyncMessage::handleData);
	}

	@SubscribeEvent
	public static void onPlayerLoggedInSyncPlayerVariables(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			for (Entity entityiterator : player.level().players())
				if (entityiterator != player && entityiterator instanceof ServerPlayer playeriterator)
					PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(playeriterator.getData(PLAYER_VARIABLES), playeriterator.getId()));
			PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES), player.getId()));
		}
	}

	@SubscribeEvent
	public static void onPlayerRespawnedSyncPlayerVariables(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			for (Entity entityiterator : player.level().players())
				if (entityiterator != player && entityiterator instanceof ServerPlayer playeriterator)
					PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(playeriterator.getData(PLAYER_VARIABLES), playeriterator.getId()));
			PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES), player.getId()));
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimensionSyncPlayerVariables(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			for (Entity entityiterator : player.level().players())
				if (entityiterator != player && entityiterator instanceof ServerPlayer playeriterator)
					PacketDistributor.sendToPlayer(player, new PlayerVariablesSyncMessage(playeriterator.getData(PLAYER_VARIABLES), playeriterator.getId()));
			PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES), player.getId()));
		}
	}

	@SubscribeEvent
	public static void onPlayerTickUpdateSyncPlayerVariables(PlayerTickEvent.Post event) {
		if (event.getEntity() instanceof ServerPlayer player && player.getData(PLAYER_VARIABLES)._syncDirty) {
			PacketDistributor.sendToPlayersInDimension((ServerLevel) player.level(), new PlayerVariablesSyncMessage(player.getData(PLAYER_VARIABLES), player.getId()));
			player.getData(PLAYER_VARIABLES)._syncDirty = false;
		}
	}

	@SubscribeEvent
	public static void clonePlayer(PlayerEvent.Clone event) {
		PlayerVariables original = event.getOriginal().getData(PLAYER_VARIABLES);
		PlayerVariables clone = new PlayerVariables();
		clone.XP = original.XP;
		clone.XP_NEXT_LEVEL = original.XP_NEXT_LEVEL;
		clone.HEALT_MAX = original.HEALT_MAX;
		clone.MANA = original.MANA;
		clone.MANA_MAX = original.MANA_MAX;
		clone.ATTACK = original.ATTACK;
		clone.DEFENCE = original.DEFENCE;
		clone.CRITICAL_CHANCE = original.CRITICAL_CHANCE;
		clone.CRITICAL_DAMAGE = original.CRITICAL_DAMAGE;
		clone.SPEED = original.SPEED;
		clone.GOLD = original.GOLD;
		clone.PLAYER_CLASS = original.PLAYER_CLASS;
		clone.CLASS_LEVEL = original.CLASS_LEVEL;
		clone.SKILL_POINT = original.SKILL_POINT;
		clone.STAT_POINT = original.STAT_POINT;
		clone.LEVEL = original.LEVEL;
		clone.HEALT = original.HEALT;
		clone.page = original.page;
		clone.MANA_REGEN = original.MANA_REGEN;
		clone.SKILL1_CD = original.SKILL1_CD;
		clone.SKILL2_CD = original.SKILL2_CD;
		clone.SKILL1_MAX_CD = original.SKILL1_MAX_CD;
		clone.SKILL2_MAX_CD = original.SKILL2_MAX_CD;
		clone.paladin_shields = original.paladin_shields;
		clone.Monk_Combo_CD = original.Monk_Combo_CD;
		clone.ATTACK_SPEED = original.ATTACK_SPEED;
		if (!event.isWasDeath()) {
			clone.berserk_mode = original.berserk_mode;
			clone.ranger_hitcooldown = original.ranger_hitcooldown;
			clone.werewolf_mode = original.werewolf_mode;
		}
		event.getEntity().setData(PLAYER_VARIABLES, clone);
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			SavedData mapdata = MapVariables.get(event.getEntity().level());
			SavedData worlddata = WorldVariables.get(event.getEntity().level());
			if (mapdata != null)
				PacketDistributor.sendToPlayer(player, new SavedDataSyncMessage(0, mapdata));
			if (worlddata != null)
				PacketDistributor.sendToPlayer(player, new SavedDataSyncMessage(1, worlddata));
		}
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			SavedData worlddata = WorldVariables.get(event.getEntity().level());
			if (worlddata != null)
				PacketDistributor.sendToPlayer(player, new SavedDataSyncMessage(1, worlddata));
		}
	}

	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent.Post event) {
		if (event.getLevel() instanceof ServerLevel level) {
			WorldVariables worldVariables = WorldVariables.get(level);
			if (worldVariables._syncDirty) {
				PacketDistributor.sendToPlayersInDimension(level, new SavedDataSyncMessage(1, worldVariables));
				worldVariables._syncDirty = false;
			}
			MapVariables mapVariables = MapVariables.get(level);
			if (mapVariables._syncDirty) {
				PacketDistributor.sendToAllPlayers(new SavedDataSyncMessage(0, mapVariables));
				mapVariables._syncDirty = false;
			}
		}
	}

	public static class WorldVariables extends SavedData {
		public static final String DATA_NAME = "eldervale_worldvars";
		boolean _syncDirty = false;
		public double mob_level = 0;

		public static WorldVariables load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
			WorldVariables data = new WorldVariables();
			data.read(tag, lookupProvider);
			return data;
		}

		public void read(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
			mob_level = nbt.getDouble("mob_level");
		}

		@Override
		public CompoundTag save(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
			nbt.putDouble("mob_level", mob_level);
			return nbt;
		}

		public void markSyncDirty() {
			this.setDirty();
			this._syncDirty = true;
		}

		static WorldVariables clientSide = new WorldVariables();

		public static WorldVariables get(LevelAccessor world) {
			if (world instanceof ServerLevel level) {
				return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(WorldVariables::new, WorldVariables::load), DATA_NAME);
			} else {
				return clientSide;
			}
		}
	}

	public static class MapVariables extends SavedData {
		public static final String DATA_NAME = "eldervale_mapvars";
		boolean _syncDirty = false;
		public boolean Monk_Combo = false;

		public static MapVariables load(CompoundTag tag, HolderLookup.Provider lookupProvider) {
			MapVariables data = new MapVariables();
			data.read(tag, lookupProvider);
			return data;
		}

		public void read(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
			Monk_Combo = nbt.getBoolean("Monk_Combo");
		}

		@Override
		public CompoundTag save(CompoundTag nbt, HolderLookup.Provider lookupProvider) {
			nbt.putBoolean("Monk_Combo", Monk_Combo);
			return nbt;
		}

		public void markSyncDirty() {
			this.setDirty();
			_syncDirty = true;
		}

		static MapVariables clientSide = new MapVariables();

		public static MapVariables get(LevelAccessor world) {
			if (world instanceof ServerLevelAccessor serverLevelAcc) {
				return serverLevelAcc.getLevel().getServer().getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(new SavedData.Factory<>(MapVariables::new, MapVariables::load), DATA_NAME);
			} else {
				return clientSide;
			}
		}
	}

	public record SavedDataSyncMessage(int dataType, SavedData data) implements CustomPacketPayload {
		public static final Type<SavedDataSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EldervaleMod.MODID, "saved_data_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SavedDataSyncMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, SavedDataSyncMessage message) -> {
			buffer.writeInt(message.dataType);
			if (message.data != null)
				buffer.writeNbt(message.data.save(new CompoundTag(), buffer.registryAccess()));
		}, (RegistryFriendlyByteBuf buffer) -> {
			int dataType = buffer.readInt();
			CompoundTag nbt = buffer.readNbt();
			SavedData data = null;
			if (nbt != null) {
				data = dataType == 0 ? new MapVariables() : new WorldVariables();
				if (data instanceof MapVariables mapVariables)
					mapVariables.read(nbt, buffer.registryAccess());
				else if (data instanceof WorldVariables worldVariables)
					worldVariables.read(nbt, buffer.registryAccess());
			}
			return new SavedDataSyncMessage(dataType, data);
		});

		@Override
		public Type<SavedDataSyncMessage> type() {
			return TYPE;
		}

		public static void handleData(final SavedDataSyncMessage message, final IPayloadContext context) {
			if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
				context.enqueueWork(() -> {
					if (message.dataType == 0)
						MapVariables.clientSide.read(message.data.save(new CompoundTag(), context.player().registryAccess()), context.player().registryAccess());
					else
						WorldVariables.clientSide.read(message.data.save(new CompoundTag(), context.player().registryAccess()), context.player().registryAccess());
				}).exceptionally(e -> {
					context.connection().disconnect(Component.literal(e.getMessage()));
					return null;
				});
			}
		}
	}

	public static class PlayerVariables implements INBTSerializable<CompoundTag> {
		boolean _syncDirty = false;
		public double XP = 0;
		public double XP_NEXT_LEVEL = 100.0;
		public double HEALT_MAX = 20.0;
		public double MANA = 100.0;
		public double MANA_MAX = 100.0;
		public double ATTACK = 1.0;
		public double DEFENCE = 1.0;
		public double CRITICAL_CHANCE = 5.0;
		public double CRITICAL_DAMAGE = 150.0;
		public double SPEED = 0.1;
		public double GOLD = 0;
		public String PLAYER_CLASS = "\"\"";
		public double CLASS_LEVEL = 1.0;
		public double SKILL_POINT = 0;
		public double STAT_POINT = 0;
		public double LEVEL = 1.0;
		public double HEALT = 20.0;
		public double page = 0;
		public double MANA_REGEN = 1.0;
		public double SKILL1_CD = 100.0;
		public double SKILL2_CD = 100.0;
		public double SKILL1_MAX_CD = 0;
		public double SKILL2_MAX_CD = 0;
		public boolean berserk_mode = false;
		public boolean paladin_shields = false;
		public boolean ranger_hitcooldown = false;
		public double Monk_Combo_CD = 0;
		public double ATTACK_SPEED = 0;
		public boolean werewolf_mode = false;

		@Override
		public CompoundTag serializeNBT(HolderLookup.Provider lookupProvider) {
			CompoundTag nbt = new CompoundTag();
			nbt.putDouble("XP", XP);
			nbt.putDouble("XP_NEXT_LEVEL", XP_NEXT_LEVEL);
			nbt.putDouble("HEALT_MAX", HEALT_MAX);
			nbt.putDouble("MANA", MANA);
			nbt.putDouble("MANA_MAX", MANA_MAX);
			nbt.putDouble("ATTACK", ATTACK);
			nbt.putDouble("DEFENCE", DEFENCE);
			nbt.putDouble("CRITICAL_CHANCE", CRITICAL_CHANCE);
			nbt.putDouble("CRITICAL_DAMAGE", CRITICAL_DAMAGE);
			nbt.putDouble("SPEED", SPEED);
			nbt.putDouble("GOLD", GOLD);
			nbt.putString("PLAYER_CLASS", PLAYER_CLASS);
			nbt.putDouble("CLASS_LEVEL", CLASS_LEVEL);
			nbt.putDouble("SKILL_POINT", SKILL_POINT);
			nbt.putDouble("STAT_POINT", STAT_POINT);
			nbt.putDouble("LEVEL", LEVEL);
			nbt.putDouble("HEALT", HEALT);
			nbt.putDouble("page", page);
			nbt.putDouble("MANA_REGEN", MANA_REGEN);
			nbt.putDouble("SKILL1_CD", SKILL1_CD);
			nbt.putDouble("SKILL2_CD", SKILL2_CD);
			nbt.putDouble("SKILL1_MAX_CD", SKILL1_MAX_CD);
			nbt.putDouble("SKILL2_MAX_CD", SKILL2_MAX_CD);
			nbt.putBoolean("berserk_mode", berserk_mode);
			nbt.putBoolean("paladin_shields", paladin_shields);
			nbt.putBoolean("ranger_hitcooldown", ranger_hitcooldown);
			nbt.putDouble("Monk_Combo_CD", Monk_Combo_CD);
			nbt.putDouble("ATTACK_SPEED", ATTACK_SPEED);
			nbt.putBoolean("werewolf_mode", werewolf_mode);
			return nbt;
		}

		@Override
		public void deserializeNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
			XP = nbt.getDouble("XP");
			XP_NEXT_LEVEL = nbt.getDouble("XP_NEXT_LEVEL");
			HEALT_MAX = nbt.getDouble("HEALT_MAX");
			MANA = nbt.getDouble("MANA");
			MANA_MAX = nbt.getDouble("MANA_MAX");
			ATTACK = nbt.getDouble("ATTACK");
			DEFENCE = nbt.getDouble("DEFENCE");
			CRITICAL_CHANCE = nbt.getDouble("CRITICAL_CHANCE");
			CRITICAL_DAMAGE = nbt.getDouble("CRITICAL_DAMAGE");
			SPEED = nbt.getDouble("SPEED");
			GOLD = nbt.getDouble("GOLD");
			PLAYER_CLASS = nbt.getString("PLAYER_CLASS");
			CLASS_LEVEL = nbt.getDouble("CLASS_LEVEL");
			SKILL_POINT = nbt.getDouble("SKILL_POINT");
			STAT_POINT = nbt.getDouble("STAT_POINT");
			LEVEL = nbt.getDouble("LEVEL");
			HEALT = nbt.getDouble("HEALT");
			page = nbt.getDouble("page");
			MANA_REGEN = nbt.getDouble("MANA_REGEN");
			SKILL1_CD = nbt.getDouble("SKILL1_CD");
			SKILL2_CD = nbt.getDouble("SKILL2_CD");
			SKILL1_MAX_CD = nbt.getDouble("SKILL1_MAX_CD");
			SKILL2_MAX_CD = nbt.getDouble("SKILL2_MAX_CD");
			berserk_mode = nbt.getBoolean("berserk_mode");
			paladin_shields = nbt.getBoolean("paladin_shields");
			ranger_hitcooldown = nbt.getBoolean("ranger_hitcooldown");
			Monk_Combo_CD = nbt.getDouble("Monk_Combo_CD");
			ATTACK_SPEED = nbt.getDouble("ATTACK_SPEED");
			werewolf_mode = nbt.getBoolean("werewolf_mode");
		}

		public void markSyncDirty() {
			_syncDirty = true;
		}
	}

	public record PlayerVariablesSyncMessage(PlayerVariables data, int player) implements CustomPacketPayload {
		public static final Type<PlayerVariablesSyncMessage> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EldervaleMod.MODID, "player_variables_sync"));
		public static final StreamCodec<RegistryFriendlyByteBuf, PlayerVariablesSyncMessage> STREAM_CODEC = StreamCodec.of((RegistryFriendlyByteBuf buffer, PlayerVariablesSyncMessage message) -> {
			buffer.writeInt(message.player());
			buffer.writeNbt(message.data().serializeNBT(buffer.registryAccess()));
		}, (RegistryFriendlyByteBuf buffer) -> {
			PlayerVariablesSyncMessage message = new PlayerVariablesSyncMessage(new PlayerVariables(), buffer.readInt());
			message.data.deserializeNBT(buffer.registryAccess(), buffer.readNbt());
			return message;
		});

		@Override
		public Type<PlayerVariablesSyncMessage> type() {
			return TYPE;
		}

		public static void handleData(final PlayerVariablesSyncMessage message, final IPayloadContext context) {
			if (context.flow() == PacketFlow.CLIENTBOUND && message.data != null) {
				context.enqueueWork(() -> {
					Entity player = context.player().level().getEntity(message.player);
					if (player == null)
						return;
					player.getData(PLAYER_VARIABLES).deserializeNBT(context.player().registryAccess(), message.data.serializeNBT(context.player().registryAccess()));
				}).exceptionally(e -> {
					context.connection().disconnect(Component.literal(e.getMessage()));
					return null;
				});
			}
		}
	}
}