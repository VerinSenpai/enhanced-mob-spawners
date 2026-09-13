package com.branders.spawnermod.item;

import com.branders.spawnermod.SpawnerMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SpawnerMod.MOD_ID);

    public static final DeferredItem<Item> SPAWNER_KEY = ITEMS.register("spawner_key",
            () -> new SpawnerKeyItem(new Item.Properties()
                    .durability(3)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
