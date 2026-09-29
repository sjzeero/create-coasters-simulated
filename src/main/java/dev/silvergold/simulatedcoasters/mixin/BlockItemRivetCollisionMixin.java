/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacementCollision;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value={BlockItem.class})
public abstract class BlockItemRivetCollisionMixin {
    @Shadow
    protected abstract boolean mustSurvive();

    @WrapMethod(method={"canPlace(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"})
    private boolean simulatedcoasters$rivetShapeAwareCanPlace(BlockPlaceContext context, BlockState state, Operation<Boolean> original) {
        if (!RivetPlacement.shouldBypassSablePlacementCollision(context)) {
            return (Boolean)original.call(new Object[]{context, state});
        }
        if (this.mustSurvive() && !state.canSurvive((LevelReader)context.getLevel(), context.getClickedPos())) {
            return false;
        }
        return !RivetPlacementCollision.hasShapeObstruction(context, state);
    }
}

