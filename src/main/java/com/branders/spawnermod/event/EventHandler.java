package com.branders.spawnermod.event;

import com.branders.spawnermod.config.Config;
import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.config.SpawnerSettings;
import com.branders.spawnermod.config.SpawnerSettings.CountSettings;
import com.branders.spawnermod.config.SpawnerSettings.SpeedSettings;
import com.branders.spawnermod.data.DataAttachments;
import com.branders.spawnermod.data.DataComponents;
import com.branders.spawnermod.item.ModItems;
import com.branders.spawnermod.item.SpawnerKeyItem;
import com.branders.spawnermod.networking.packet.SyncConfigPacket;
import com.branders.spawnermod.networking.PacketHandler;
import com.branders.spawnermod.networking.packet.SyncSpawnerPacket;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

@EventBusSubscriber
public class EventHandler {
    /**
     * Register spawner sync packet and server-side handler.
     */
    @SubscribeEvent
    private static void registerPayloadHandlersEvent(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1")
                .optional();

        registrar.playToServer(
                SyncSpawnerPacket.TYPE,
                SyncSpawnerPacket.STREAM_CODEC,
                PacketHandler::serverHandleSpawnerSync
        );
        registrar.playToClient(
                SyncConfigPacket.TYPE,
                SyncConfigPacket.STREAM_CODEC,
                PacketHandler::clientHandleConfigSync
        );
    }

    @SubscribeEvent
    private static void onModConfigReloadingEvent(ModConfigEvent.Reloading event) {
        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER)
            return;

        CompoundTag tag = new CompoundTag();
        tag.putShort("defaultRange", (short) (int) Config.DEFAULT_RANGE.get());
        tag.putBoolean("limitSpawns", Config.LIMIT_SPAWNER_SPAWNS.get());
        tag.putShort("spawnLimit", (short) (int) Config.SPAWNER_SPAWNS_LIMIT.get());
        tag.putBoolean("allowConfig", Config.ALLOW_SPAWNER_CONFIG.get());
        tag.putBoolean("disableCount", Config.DISABLE_COUNT_CONFIG.get());
        tag.putBoolean("disableSpeed", Config.DISABLE_SPEED_CONFIG.get());
        tag.putBoolean("disableRange", Config.DISABLE_RANGE_CONFIG.get());
        PacketDistributor.sendToAllPlayers(new SyncConfigPacket(tag));
    }

    /**
     * Add the spawner key to the creative search tab.
     */
    @SubscribeEvent
    private static void onBuildCreativeModeTabContentsEvent(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SEARCH)
            event.accept(ModItems.SPAWNER_KEY.get());
    }

    /**
     * 	Change hardness for Spawner Block.
     * 	It calculates a new break speed for the custom hardness. If the custom hardness
     * 	is the same as Spawner default it will calculate the same break speed as vanilla.
     *
     *    @implNote The wiki tick is at 20 but it gave wrong results when new and original
     * 	hardness was the same. Example: newHardness = 5.0 should give 0.95 seconds for a
     * 	diamond pick but it gave 3.1 seconds.
     *
     *    @see <a href="https://minecraft.fandom.com/wiki/Breaking"></a>
     */
    @SubscribeEvent
    private static void onBreakSpeedEvent(PlayerEvent.BreakSpeed event) {
        if (event.getState().getBlock() instanceof SpawnerBlock) {
            float newHardness = Config.SPAWNER_HARDNESS.get();
            float originalHardness = 5.0f;

            // First we calculate how many seconds it will take with new hardness and
            // original break speed. We want to solve for break speed later.
            float dmg = event.getOriginalSpeed() / newHardness;
            dmg /= 100;
            float ticks = Math.round(1 / dmg);
            float seconds = ticks / 64f;

            // now do it reverse and insert original hardness
            float ticks2 = seconds * 64f;
            float dmg2 = 1 / ticks2;
            dmg2 *= 100;
            int newBreakSpeed = Math.round(dmg2 * originalHardness);

            event.setNewSpeed(newBreakSpeed);
        }
    }

    /**
     * Apply remaining spawns and saved settings as a tooltip on the spawner item.
     */
    @SubscribeEvent
    private static void onItemTooltipEvent(ItemTooltipEvent event) {
        ItemStack itemStack = event.getItemStack();
        if (!itemStack.is(Items.SPAWNER))
            return;

        var tooltip = event.getToolTip();

        if (Config.LIMIT_SPAWNER_SPAWNS.get()) {
            short remaining = getRemainingSpawns(itemStack);
            tooltip.add(Component.translatable("tooltip.spawnermod.spawns_remaining", remaining));

            // Don't apply the remaining tooltips if the spawner is dead.
            if (remaining == 0)
                return;
        }

        short count = itemStack.getOrDefault(DataComponents.COUNT, (short) 4);
        tooltip.add(new CountSettings().getLabel(count));

        short speed = itemStack.getOrDefault(DataComponents.SPEED, (short) 200);
        tooltip.add(new SpeedSettings().getLabel(speed));

        short range = itemStack.getOrDefault(DataComponents.RANGE, (short) (int) Config.DEFAULT_RANGE.get());
        tooltip.add(new SpawnerSettings.RangeSettings().getLabel(range));
    }

    /**
     * Handle disabling the spawner if a redstone signal is received.
     * This is accomplished by setting the RequiredPlayerRange to 0.
     */
    @SubscribeEvent
    private static void onNeighborNotifyEvent(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;

        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner))
            return;

        if (isSpawnerAlive(spawner) && isSpawnerEnabled(spawner)) {
            BaseSpawner logic = spawner.getSpawner();
            CompoundTag tag = logic.save(new CompoundTag());
            if (level.hasNeighborSignal(pos))
                tag.putShort("RequiredPlayerRange", (short) 0);
            else
                tag.putShort("RequiredPlayerRange", getSpawnerRange(spawner));

            logic.load(level, pos, tag);
            spawner.setChanged();
        }
    }

    /**
     * Ensure the spawner is only dropped if silk touch is enabled.
     * Transfer spawner data to the item on drop.
     */
    @SubscribeEvent
    private static void onBlockDropsEvent(BlockDropsEvent event) {
        if (!(event.getBlockEntity() instanceof SpawnerBlockEntity spawner))
            return;

        List<ItemEntity> drops = event.getDrops();
        if (drops.isEmpty())
            return;

        if (Config.DISABLE_SILK_TOUCH.get()) {
            RandomSource random = event.getLevel().random;
            int size = random.nextInt(15) + random.nextInt(15);
            event.setDroppedExperience(size);
            drops.clear();
            return;
        }

        BaseSpawner logic = spawner.getSpawner();
        CompoundTag tag = logic.save(new CompoundTag());
        short spawns = spawner.getData(DataAttachments.SPAWNS);
        short count = tag.getShort("SpawnCount");
        short speed = tag.getShort("MinSpawnDelay");

        ItemStack itemStack = drops.getFirst().getItem();
        itemStack.set(DataComponents.SPAWNS, spawns);
        itemStack.set(DataComponents.COUNT, count);
        itemStack.set(DataComponents.SPEED, speed);

        // We don't save the default range to the spawner in case the config is changed.
        // When the spawner tooltip is viewed, the spawner is placed, or settings are observed
        // the current default will be fetched and displayed.
        short range = getSpawnerRange(spawner);
        if (range != Config.DEFAULT_RANGE.get())
            itemStack.set(DataComponents.RANGE, range);

        if (Config.CAN_REMOVE_SPAWNER_EGGS.get())
            emptySpawner(spawner);
    }

    /**
     * Transfer spawns, range, speed, and count settings to the spawner block from the item (if present).
     */
    @SubscribeEvent
    private static void onEntityPlaceEvent(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player player))
            return;

        if (!(level.getBlockEntity(event.getPos()) instanceof SpawnerBlockEntity spawner))
            return;

        ItemStack itemStack = player.getMainHandItem();
        BaseSpawner logic = spawner.getSpawner();
        CompoundTag tag = logic.save(new CompoundTag());

        short spawns = itemStack.getOrDefault(DataComponents.SPAWNS, (short) 0);
        spawner.setData(DataAttachments.SPAWNS, spawns);

        if (itemStack.has(DataComponents.COUNT)) {
            var countOption = new CountSettings().get(itemStack.get(DataComponents.COUNT));
            tag.putShort("SpawnCount", (short) countOption.key());
            tag.putShort("MaxNearbyEntities", (short) countOption.pair());
        }

        short currentSpeed = itemStack.getOrDefault(DataComponents.SPEED, (short) 200);
        var speedOption = new SpeedSettings().get(currentSpeed);
        tag.putShort("MinSpawnDelay", (short) speedOption.key());
        tag.putShort("MaxSpawnDelay", (short) speedOption.pair());

        short defaultRange = (short) (int) Config.DEFAULT_RANGE.get();
        short currentRange =  itemStack.getOrDefault(DataComponents.RANGE, defaultRange);
        tag.putShort("RequiredPlayerRange", currentRange);

        if (currentRange != defaultRange)
            spawner.setData(DataAttachments.RANGE, currentRange);

        logic.load(level, spawner.getBlockPos(), tag);
        spawner.setChanged();
    }

    @SubscribeEvent
    private static void onRightClickBlockEvent(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;

        Item item = event.getItemStack().getItem();
        if (item instanceof BlockItem || item instanceof SpawnerKeyItem || event.getHand() == InteractionHand.OFF_HAND)
            return;

        if (!(level.getBlockEntity(event.getPos()) instanceof SpawnerBlockEntity spawner))
            return;

        if (Config.LOG_ITEM_ID_ON_RIGHT_CLICK.get())
            SpawnerMod.LOGGER.info("Right clicked with item id: {}", item);

        if (Config.CAN_REMOVE_SPAWNER_EGGS.get())
            emptySpawner(spawner);
    }

    /**
     * If limited spawns is enabled, create and tick down a spawner_remaining field on the spawner.
     * If spawns is 0, call killSpawner which will set RequiredPlayerRange to 0.
     * This enforces spawner death because the range is transferred to and from the spawner item stack
     * only when the range value is not default. In this case, 0 is not default.
     */
    @SubscribeEvent
    private static void onFinalizeSpawnEvent(FinalizeSpawnEvent event) {
        if (!Config.LIMIT_SPAWNER_SPAWNS.get())
            return;

        Either<BlockEntity, Entity> spawnerSource = event.getSpawner();
        if (spawnerSource == null)
            return;

        spawnerSource.ifLeft(blockEntity -> {
            if (!(blockEntity instanceof SpawnerBlockEntity spawner))
                return;

            short remaining = getRemainingSpawns(spawner);

            if (remaining == 1)
                killSpawner(event.getLevel().getLevel(), spawner);

            spawner.setData(DataAttachments.SPAWNS, (short) (spawner.getData(DataAttachments.SPAWNS) + 1));
        });
    }

    /**
     * Maybe possibly just a teeny tiny bit drop a spawn egg when a creature dies.
     * Configurable to only drop if a player kills the creature.
     */
    @SubscribeEvent
    private static void onLivingDeathEvent(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer) && Config.EGG_DROPS_REQUIRE_PLAYER.get())
            return;

        Entity entity = event.getEntity();
        Level level = entity.level();

        if (level.isClientSide)
            return;

        if (level.random.nextFloat() > Config.EGG_DROP_CHANCE.get() / 100f)
            return;

        EntityType<?> entityType = entity.getType();
        String entityString = EntityType.getKey(entityType).toString();
        trySpawnEgg(level, entityString, entity.xOld, entity.yOld, entity.zOld);
    }

    /**
     * Kill the spawner dead. Spawner death is checked by using getRemainingSpawns.
     * Spawner is disabled by setting RequriedPlayerRange to 0.
     * Also play the item burn sound effect.
     */
    private static void killSpawner(ServerLevel level, SpawnerBlockEntity spawner) {
        BaseSpawner logic = spawner.getSpawner();
        CompoundTag tag = logic.save(new CompoundTag());
        tag.putShort("RequiredPlayerRange", (short) 0);
        logic.load(level, spawner.getBlockPos(), tag);
        level.playSound(null, spawner.getBlockPos(), SoundEvents.GENERIC_BURN, SoundSource.BLOCKS);
    }

    /**
     * Lookup a spawn egg using a provided entity id and spawn said spawn egg at a given location.
     */
    private static boolean trySpawnEgg(Level level, String entityString, double posX, double posY, double posZ) {
        SpawnEggItem egg = getSpawnEgg(entityString);
        if (egg == null || Config.EXCLUDED_EGGS.get().contains(egg.toString()))
            return false;

        ItemEntity entityItem = new ItemEntity(level, posX, posY, posZ, new ItemStack(egg));
        entityItem.setDefaultPickUpDelay();
        level.addFreshEntity(entityItem);
        return true;
    }

    /**
     * Fetch the SpawnEggItem by looking through the registries for a matching key.
     */
    private static SpawnEggItem getSpawnEgg(String entityString) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!BuiltInRegistries.ENTITY_TYPE.getKey(type).toString().equals(entityString))
                continue;

            return SpawnEggItem.byId(type);
        }

        SpawnerMod.LOGGER.warn("egg not found for entity {}!", entityString);
        return null;
    }

    /**
     * Fetch the entity within a spawner. This is done by reading SpawnData and parsing/reading the entity ID.
     * Returns null if a spawner contains no spawn data, an entity is not found, or the spawner is empty.
     */
    private static String getEntityFromSpawner(BaseSpawner logic) {
        // Get entity ResourceLocation string from spawner by creating an empty compound which we make our
        // spawner logic write to. We can then access what type of entity id the spawner has inside
        CompoundTag nbt = logic.save(new CompoundTag());
        Tag tag = nbt.get("SpawnData");

        if (tag == null) {
            SpawnerMod.LOGGER.warn("spawner contained no spawn data! {}", logic);
            return null;
        }

        // Leave if the spawner does not contain an entity
        String entityString = tag.toString();
        if(!entityString.contains("\""))
            return null;

        // Strips the string
        // Example: {id: "minecraft:xxx_xx"} --> minecraft:xxx_xx
        entityString = entityString.substring(entityString.indexOf("\"") + 1);
        entityString = entityString.substring(0, entityString.indexOf("\""));

        // Leave if the spawner does not contain an egg
        if(entityString.equalsIgnoreCase(EntityType.getKey(EntityType.AREA_EFFECT_CLOUD).toString()))
            return null;

        return entityString;
    }

    /**
     * Remove the spawn egg from the spawner. This is actually done by setting the spawning entity to AREA_EFFECT_CLOUD
     * and creating a new spawn egg with a fly-out position outside the spawner.
     */
    private static void emptySpawner(SpawnerBlockEntity spawner) {
        BaseSpawner logic = spawner.getSpawner();
        if (!(spawner.getLevel() instanceof Level level))
            return;

        String entityString = getEntityFromSpawner(logic);
        if (entityString == null)
            return;

        BlockPos pos = spawner.getBlockPos();
        // Get random fly-out position offsets
        double posX = (double)(level.random.nextFloat() * 0.7F) + (double)0.15F + pos.getX();
        double posY = (double)(level.random.nextFloat() * 0.7F) + (double)0.06F + 0.6D + pos.getY();
        double posZ = (double)(level.random.nextFloat() * 0.7F) + (double)0.15F + pos.getZ();

        if (!trySpawnEgg(level, entityString, posX, posY, posZ))
            return;

        // Replace the entity inside the spawner with default entity
        logic.setEntityId(EntityType.AREA_EFFECT_CLOUD, level, level.random, pos);
        spawner.setChanged();
        BlockState blockstate = level.getBlockState(pos);
        level.sendBlockUpdated(pos, blockstate, blockstate, 3);
    }

    /**
     * Fetch the range of a spawner block entity.
     * If the spawner does not have a custom range set, returns the configured default.
     */
    public static short getSpawnerRange(SpawnerBlockEntity spawner) {
        if (spawner.hasData(DataAttachments.RANGE))
            return spawner.getData(DataAttachments.RANGE);
        return (short) (int) Config.DEFAULT_RANGE.get();
    }

    /**
     * Read the remaining spawns of a spawner block entity. If the limit is lower than the last set
     * value, returns the current limit.
     */
    public static short getRemainingSpawns(SpawnerBlockEntity spawner) {
        short limit = (short) (int) Config.SPAWNER_SPAWNS_LIMIT.get();
        short current = spawner.getData(DataAttachments.SPAWNS);
        return (short) Math.min((limit - current), 0);
    }

    /**
     * Read the remaining spawns of a spawner item stack. If the limit is lower than the last set
     * value, returns the current limit.
     */
    private static short getRemainingSpawns(ItemStack itemStack) {
        short limit = (short) (int) Config.SPAWNER_SPAWNS_LIMIT.get();
        short current = itemStack.getOrDefault(DataComponents.SPAWNS, (short) 0);
        return (short) Math.min((limit - current), 0);
    }

    /**
     * Is spawner spawns limited? If so, does this spawner still have spawns?
     */
    public static boolean isSpawnerAlive(SpawnerBlockEntity spawner) {
        return !Config.LIMIT_SPAWNER_SPAWNS.get() || getRemainingSpawns(spawner) > 0;
    }

    /**
     * The enabled flag on the spawner block is only used to check if the current 0 range
     * condition is because the spawner is disabled. Spawner functionality is actually disabled
     * by setting PlayerRequiredRange to 0.
     */
    public static boolean isSpawnerEnabled(SpawnerBlockEntity spawner) {
        return spawner.getData(DataAttachments.ENABLED);
    }
}
