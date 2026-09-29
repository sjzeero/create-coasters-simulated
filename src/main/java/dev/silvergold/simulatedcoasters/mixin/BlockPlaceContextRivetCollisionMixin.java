/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  org.spongepowered.asm.mixin.Mixin
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={BlockPlaceContext.class})
public abstract class BlockPlaceContextRivetCollisionMixin {
    @WrapMethod(method={"canPlace"})
    private boolean simulatedcoasters$bypassSableUnitCollisionOnRivet(Operation<Boolean> original) {
        BlockPlaceContext self = (BlockPlaceContext)this;
        if (!RivetPlacement.shouldBypassSablePlacementCollision(self)) {
            return (Boolean)original.call(new Object[0]);
        }
        BlockPos clicked = self.getClickedPos();
        return self.replacingClickedOnBlock() || self.getLevel().getBlockState(clicked).canBeReplaced(self);
    }
}

