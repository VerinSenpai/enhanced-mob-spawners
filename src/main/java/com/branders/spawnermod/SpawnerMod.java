package com.branders.spawnermod;

import com.branders.spawnermod.config.Config;
import com.branders.spawnermod.data.DataAttachments;
import com.branders.spawnermod.data.DataComponents;
import com.branders.spawnermod.item.ModItems;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;

/**
 * Small mod adding more functionality to the Mob Spawner for Minecraft Fabric
 * 1.20.1 forge version ported to 1.21.1 neoforge with fabric additions.
 * Updated by Verin.
 * @author Anders <Branders> Blomqvist
 */
@Mod(SpawnerMod.MOD_ID)
public class SpawnerMod {
    public static final String MOD_ID = "spawnermod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SpawnerMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        DataAttachments.register(modEventBus);
        DataComponents.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
    }
}
