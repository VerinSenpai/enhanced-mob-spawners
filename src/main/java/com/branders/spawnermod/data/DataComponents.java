package com.branders.spawnermod.data;

import com.branders.spawnermod.SpawnerMod;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class DataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SpawnerMod.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Short>> SPAWNS = DATA_COMPONENT_TYPES.registerComponentType(
            "spawns", builder -> builder
                    .persistent(Codec.SHORT)
                    .networkSynchronized(ByteBufCodecs.SHORT)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Short>> COUNT = DATA_COMPONENT_TYPES.registerComponentType(
            "count", builder -> builder
                    .persistent(Codec.SHORT)
                    .networkSynchronized(ByteBufCodecs.SHORT)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Short>> SPEED = DATA_COMPONENT_TYPES.registerComponentType(
            "speed", builder -> builder
                    .persistent(Codec.SHORT)
                    .networkSynchronized(ByteBufCodecs.SHORT)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Short>> RANGE = DATA_COMPONENT_TYPES.registerComponentType(
            "range", builder -> builder
                    .persistent(Codec.SHORT)
                    .networkSynchronized(ByteBufCodecs.SHORT)
    );

    public static void register(IEventBus eventBus) {
        DATA_COMPONENT_TYPES.register(eventBus);
    }
}
