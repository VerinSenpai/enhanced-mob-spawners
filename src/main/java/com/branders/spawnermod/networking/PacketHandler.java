package com.branders.spawnermod.networking;

import com.branders.spawnermod.config.Config;
import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.data.DataAttachments;
import com.branders.spawnermod.event.EventHandler;
import com.branders.spawnermod.item.SpawnerKeyItem;
import com.branders.spawnermod.networking.packet.SyncConfigPacket;
import com.branders.spawnermod.networking.packet.SyncSpawnerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.branders.spawnermod.config.SpawnerSettings.*;

public class PacketHandler {
    public static void serverHandleSpawnerSync(final SyncSpawnerPacket payload, final IPayloadContext context) {
        SpawnerMod.LOGGER.info(payload.toString());

        context.enqueueWork(() -> {
            if (!Config.ALLOW_SPAWNER_CONFIG.get())
                return;

            Player player = context.player();
            ItemStack itemStack = player.getMainHandItem();

            if (!(itemStack.getItem() instanceof SpawnerKeyItem))
                return;

            BlockPos pos = payload.pos();
            // This handler is run exclusively by the server.
            // If for some reason this is something other than ServerLevel, cry.
            ServerLevel level = (ServerLevel) player.level();
            if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner))
                return;

            if (!EventHandler.isSpawnerAlive(spawner))
                return;

            BaseSpawner logic = spawner.getSpawner();
            CompoundTag tag = logic.save(new CompoundTag());

            var count = new CountSettings().get(payload.count());
            if (count != null) {
                tag.putShort("SpawnCount", (short) count.key());
                tag.putShort("MaxNearbyEntities", (short) count.pair());
            }

            var speed = new SpeedSettings().get(payload.speed());
            if (speed != null) {
                tag.putShort("MinSpawnDelay", (short) speed.key());
                tag.putShort("MaxSpawnDelay", (short) speed.pair());
            }

            boolean enabled = payload.enabled();
            var range = new RangeSettings().get(payload.range());
            if (range != null) {
                boolean active = enabled && !level.hasNeighborSignal(pos);
                tag.putShort("RequiredPlayerRange", active ? (short) range.key() : 0);

                if (range.key() == Config.DEFAULT_RANGE.get())
                    spawner.removeData(DataAttachments.RANGE);
                else
                    spawner.setData(DataAttachments.RANGE, (short) range.key());
            }

            spawner.setData(DataAttachments.ENABLED, enabled);
            logic.load(level, pos, tag);
            spawner.setChanged();

            itemStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("spawnermod.networking.spawner_sync_failed", e.getMessage()));
            return null;
        });
    }

    public static void clientHandleConfigSync(final SyncConfigPacket payload, final IPayloadContext context) {
        SpawnerMod.LOGGER.info(payload.toString());

        context.enqueueWork(() -> {
            CompoundTag tag = payload.tag();
            Config.DEFAULT_RANGE.set((int) tag.getShort("defaultRange"));
            Config.LIMIT_SPAWNER_SPAWNS.set(tag.getBoolean("limitSpawns"));
            Config.SPAWNER_SPAWNS_LIMIT.set((int) tag.getShort("spawnLimit"));
            Config.ALLOW_SPAWNER_CONFIG.set(tag.getBoolean("allowConfig"));
            Config.DISABLE_COUNT_CONFIG.set(tag.getBoolean("disableCount"));
            Config.DISABLE_SPEED_CONFIG.set(tag.getBoolean("disableSpeed"));
            Config.DISABLE_RANGE_CONFIG.set(tag.getBoolean("disableRange"));
        }).exceptionally(e -> {
            context.disconnect(Component.translatable("spawnermod.networking.spawner_sync_failed", e.getMessage()));
            return null;
        });
    }
}
