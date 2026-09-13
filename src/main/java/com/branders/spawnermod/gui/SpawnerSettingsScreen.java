package com.branders.spawnermod.gui;

import com.branders.spawnermod.config.Config;
import com.branders.spawnermod.SpawnerMod;
import com.branders.spawnermod.event.EventHandler;
import com.branders.spawnermod.networking.packet.SyncSpawnerPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import static com.branders.spawnermod.config.SpawnerSettings.*;

/**
 * Spawner GUI config screen. Renders the background and all the buttons. It
 * communicates with the spawner block by sending a network package with data
 * from the GUI elements. Packet data is verified server-side.
 *
 * @author Anders <Branders> Blomqvist
 */
@OnlyIn(Dist.CLIENT)
public class SpawnerSettingsScreen extends Screen {
    private static final Component TITLE_TEXT = Component.translatable("gui.spawnermod.spawner_config_screen_title");
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(SpawnerMod.MOD_ID,
            "textures/gui/spawner_config_screen.png");
    private static final int BACKGROUND_WIDTH = 178;
    private static final int BACKGROUND_HEIGHT = 177;
    private static final int BUTTON_WIDTH = 108;
    private static final ResourceLocation SPAWNS_ICON = ResourceLocation.fromNamespaceAndPath(SpawnerMod.MOD_ID,
            "textures/gui/spawner_config_screen_icon_spawns.png");

    private final SpawnerBlockEntity spawner;
    private boolean enabled;
    private CountSettings COUNT;
    private short count;
    private SpeedSettings SPEED;
    private short speed;
    private RangeSettings RANGE;
    private short range;

    public SpawnerSettingsScreen(SpawnerBlockEntity spawner) {
        super(TITLE_TEXT);

        this.spawner = spawner;
    }

    /**
     * Fetch spawner nbt data. Instantiate Count, Speed, and Range settings groups.
     * All current values are fetched and sent to the server when save is pressed,
     * even if values aren't changed. The server checks received values against
     * its own instances of the SettingsGroups. Any values that don't have a place
     * are ignored. If you set nbt data on the server directly, that data will appear
     * here as custom, and will be ignored unless changed.
     */
    @Override
    protected void init() {
        boolean alive = EventHandler.isSpawnerAlive(spawner);
        CompoundTag tag = spawner.getSpawner().save(new CompoundTag());

        COUNT = new CountSettings();
        count = tag.getShort("SpawnCount");
        var countButton = Button.builder(COUNT.getLabel(count), this::handleCountButtonPressed)
                .tooltip(Tooltip.create(Component.translatable("button.count.tooltip")))
                .bounds((width - BUTTON_WIDTH) / 2, 55, BUTTON_WIDTH, 20).build();
        addRenderableWidget(countButton);
        countButton.active = !Config.DISABLE_COUNT_CONFIG.get() && alive;

        SPEED = new SpeedSettings();
        speed = tag.getShort("MinSpawnDelay"); // We use MinSpawnDelay because of a race that happens with Delay.
        var speedButton = Button.builder(SPEED.getLabel(speed) , this::handleSpeedButtonPressed)
                .tooltip(Tooltip.create(Component.translatable("button.speed.tooltip")))
                .bounds((width - BUTTON_WIDTH) / 2, 80, BUTTON_WIDTH, 20).build();
        addRenderableWidget(speedButton);
        speedButton.active = !Config.DISABLE_SPEED_CONFIG.get() && alive;

        RANGE = new RangeSettings();
        range = EventHandler.getSpawnerRange(spawner);
        var rangeButton = Button.builder(RANGE.getLabel(range), this::handleRangeButtonPressed)
                .tooltip(Tooltip.create(Component.translatable("button.range.tooltip")))
                .bounds((width - BUTTON_WIDTH) / 2, 105, BUTTON_WIDTH, 20).build();
        addRenderableWidget(rangeButton);
        rangeButton.active = !Config.DISABLE_RANGE_CONFIG.get() && alive;

        enabled = EventHandler.isSpawnerEnabled(spawner);
        var toggleButton = Button.builder(getToggleLabel(), this::handleToggleButtonPressed)
                .bounds((width - BUTTON_WIDTH) / 2, 130, BUTTON_WIDTH, 20).build();
        addRenderableWidget(toggleButton);
        toggleButton.active = alive;

        var saveButton = Button.builder(Component.translatable("button.save"), this::handleSaveButtonPressed)
                .bounds((width - BACKGROUND_WIDTH) / 2, 190, BACKGROUND_WIDTH, 20).build();
        addRenderableWidget(saveButton);
        saveButton.active = alive;

        var cancelButton = Button.builder(Component.translatable("button.cancel"), button -> this.onClose())
                .bounds((width - BACKGROUND_WIDTH) / 2, 215, BACKGROUND_WIDTH, 20).build();
        addRenderableWidget(cancelButton);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int bgX = (width - BACKGROUND_WIDTH) / 2;
        guiGraphics.blit(BACKGROUND, bgX, 5, 0, 0, BACKGROUND_WIDTH, BACKGROUND_HEIGHT, BACKGROUND_WIDTH, BACKGROUND_HEIGHT);

        // Render spawner title text. Centered, for my sanity.
        int titleX = (width - font.width(TITLE_TEXT)) / 2;
        guiGraphics.drawString(this.font, TITLE_TEXT, titleX, 33, 0xFFD964);

        // Currently this doesn't get updated if the config changes on the server.
        // How best can I fix this without needing an entirely new sync packet?
        if (Config.LIMIT_SPAWNER_SPAWNS.get()) {
            short remaining = EventHandler.getRemainingSpawns(spawner);
            guiGraphics.blit(SPAWNS_ICON, width / 2 - 7 + 101, 23, 0, 0, 14, 14, 14, 14);
            guiGraphics.drawString(this.font, String.valueOf(remaining),  width / 2 + 114, 27, 0xFFFFFF);
        }
    }

    private void handleCountButtonPressed(Button button) {
        var next = COUNT.getNext(count);
        count = (short) next.key();
        button.setMessage(COUNT.getLabel(count));
    }

    private void handleSpeedButtonPressed(Button button) {
        var next = SPEED.getNext(speed);
        speed = (short) next.key();
        button.setMessage(SPEED.getLabel(speed));
    }

    private void handleRangeButtonPressed(Button button) {
        var next = RANGE.getNext(range);
        range = (short) next.key();
        button.setMessage(RANGE.getLabel(range));
    }

    private MutableComponent getToggleLabel() {
        return Component.translatable(enabled ? "button.toggle.enabled" : "button.toggle.disabled");
    }

    private void handleToggleButtonPressed(Button button) {
        enabled = !enabled;
        button.setMessage(getToggleLabel());
    }

    /**
     * Send a payload with the updated state. Any data sent here will be checked for validity sever-side.
     * The block position is sent to facilitate the server finding the spawner to update.
     * Close the window.
     */
    private void handleSaveButtonPressed(Button button) {
        PacketDistributor.sendToServer(new SyncSpawnerPacket(
                spawner.getBlockPos(), enabled, count, speed, range));
        this.onClose();
    }
}
