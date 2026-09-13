package com.branders.spawnermod.networking.packet;

import com.branders.spawnermod.SpawnerMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncSpawnerPacket(BlockPos pos, boolean enabled, short count, short speed, short range) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSpawnerPacket> TYPE = new CustomPacketPayload
            .Type<>(ResourceLocation.fromNamespaceAndPath(SpawnerMod.MOD_ID, "sync_spawner_packet"));

    public static final StreamCodec<ByteBuf, SyncSpawnerPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            SyncSpawnerPacket::pos,
            ByteBufCodecs.BOOL,
            SyncSpawnerPacket::enabled,
            ByteBufCodecs.SHORT,
            SyncSpawnerPacket::count,
            ByteBufCodecs.SHORT,
            SyncSpawnerPacket::speed,
            ByteBufCodecs.SHORT,
            SyncSpawnerPacket::range,
            SyncSpawnerPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
