package com.branders.spawnermod.item;

import com.branders.spawnermod.config.Config;
import com.branders.spawnermod.gui.SpawnerSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class SpawnerKeyItem extends Item {
    private static final Component TOOL_TIP = Component.translatable("tooltip.spawnermod.spawner_key_disabled")
            .setStyle(Style.EMPTY.withColor(0xff0000));

    public SpawnerKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (!Config.ALLOW_SPAWNER_CONFIG.get())
            tooltipComponents.add(TOOL_TIP);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!Config.ALLOW_SPAWNER_CONFIG.get())
            return InteractionResult.FAIL;

        Level level = context.getLevel();
        if (!level.isClientSide)
            return InteractionResult.FAIL;

        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner))
            return InteractionResult.FAIL;

        openSpawnerGui(spawner);
        return super.useOn(context);
    }

    @OnlyIn(Dist.CLIENT)
    private void openSpawnerGui(SpawnerBlockEntity spawner) {
        Minecraft.getInstance().setScreen(new SpawnerSettingsScreen(spawner));
    }
}
