/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.engine_room.flywheel.lib.visualization.VisualizationHelper
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.silvergold.simulatedcoasters.mixin;

import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorJunctionVisualRefresh;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={CoasterAnchorpointBlockEntity.class})
public class CoasterAnchorpointBlockEntityClientMixin {
    @Inject(method={"read"}, at={@At(value="TAIL")})
    private void simulatedcoasters$scheduleJunctionClusterRefreshAfterRead(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket, CallbackInfo ci) {
        CoasterAnchorpointBlockEntity self = (CoasterAnchorpointBlockEntity)((Object)this);
        if (self.getLevel() == null || !self.getLevel().isClientSide()) {
            return;
        }
        AnchorJunctionVisualRefresh.scheduleClusterRefresh(self.getLevel(), self.getBlockPos());
        VisualizationHelper.queueUpdate((BlockEntity)self);
    }
}

