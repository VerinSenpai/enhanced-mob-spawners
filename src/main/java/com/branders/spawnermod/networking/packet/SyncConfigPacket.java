package com.branders.spawnermod.networking.packet;

import com.branders.spawnermod.SpawnerMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncConfigPacket(CompoundTag tag) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncConfigPacket> TYPE = new CustomPacketPayload
            .Type<>(ResourceLocation.fromNamespaceAndPath(SpawnerMod.MOD_ID, "sync_config_packet"));

    public static final StreamCodec<ByteBuf, SyncConfigPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.COMPOUND_TAG, SyncConfigPacket::tag,
            SyncConfigPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
