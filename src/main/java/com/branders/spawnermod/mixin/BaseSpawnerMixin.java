package com.branders.spawnermod.mixin;

import com.branders.spawnermod.config.Config;
import com.branders.spawnermod.data.DataAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin {
    @Inject(method = "isNearPlayer", at = @At("RETURN"), cancellable = true)
    private void isNearPlayer(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!((level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)))
            return;
        
        if (level.hasNeighborSignal(pos) || spawner.getData(DataAttachments.ENABLED))
            cir.setReturnValue(false);

        cir.setReturnValue(
                level.hasNearbyAlivePlayer(
                        (double)pos.getX() + 0.5,
                        (double)pos.getY() + 0.5,
                        (double)pos.getZ() + 0.5,
                        (double)Config.DEFAULT_RANGE.get())
        );
    }
}