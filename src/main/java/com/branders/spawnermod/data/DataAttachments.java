package com.branders.spawnermod.data;

import com.branders.spawnermod.config.Config;
import com.branders.spawnermod.SpawnerMod;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class DataAttachments {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
            .create(NeoForgeRegistries.ATTACHMENT_TYPES, SpawnerMod.MOD_ID);

    public static final Supplier<AttachmentType<Short>> SPAWNS = ATTACHMENT_TYPES.register(
            "spawns", () -> AttachmentType.builder(() -> (short) 0)
                    .sync(ByteBufCodecs.SHORT)
                    .serialize(Codec.SHORT)
                    .build());

    public static final Supplier<AttachmentType<Boolean>> ENABLED = ATTACHMENT_TYPES.register(
            "enabled", () -> AttachmentType.builder(() -> true)
                    .sync(ByteBufCodecs.BOOL)
                    .serialize(Codec.BOOL)
                    .build());

    public static final Supplier<AttachmentType<Short>> RANGE = ATTACHMENT_TYPES.register(
            "range", () -> AttachmentType.builder(() -> (short) (int) Config.DEFAULT_RANGE.get())
                    .sync(ByteBufCodecs.SHORT)
                    .serialize(Codec.SHORT)
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
