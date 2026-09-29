/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyReturnValue
 *  com.simibubi.create.content.decoration.girder.GirderBlock
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BooleanProperty
 *  net.minecraft.world.level.block.state.properties.Property
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.decoration.girder.GirderBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorGirderConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={GirderBlock.class})
public class GirderBlockCoasterAnchorMixin {
    @ModifyReturnValue(method={"updateState"}, at={@At(value="RETURN")})
    private static BlockState simulatedcoasters$connectGirderArmsToAnchor(BlockState returned, LevelAccessor level, BlockPos pos, BlockState state, Direction d) {
        BlockState sideState = level.getBlockState(pos.relative(d));
        if (!CoasterAnchorGirderConnection.acceptsGirderConnection(sideState, d)) {
            return returned;
        }
        Direction.Axis axis = d.getAxis();
        BooleanProperty updateProperty = axis == Direction.Axis.X ? GirderBlock.X : (axis == Direction.Axis.Z ? GirderBlock.Z : (d == Direction.UP ? GirderBlock.TOP : GirderBlock.BOTTOM));
        return (BlockState)returned.setValue((Property)updateProperty, (Comparable)Boolean.valueOf(true));
    }

    @ModifyReturnValue(method={"updateVerticalProperty"}, at={@At(value="RETURN")})
    private static BlockState simulatedcoasters$connectGirderCapsToAnchor(BlockState returned, LevelAccessor level, BlockPos pos, BlockState state, Property<Boolean> updateProperty, BlockState sideState, Direction d) {
        if (CoasterAnchorGirderConnection.acceptsGirderConnection(sideState, d)) {
            return (BlockState)returned.setValue(updateProperty, (Comparable)Boolean.valueOf(true));
        }
        return returned;
    }

    @Inject(method={"isConnected"}, at={@At(value="RETURN")}, cancellable=true)
    private static void simulatedcoasters$girderFlangeOnAnchor(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        BlockState neighbor = world.getBlockState(pos.relative(side));
        if (CoasterAnchorGirderConnection.acceptsGirderConnection(neighbor, side)) {
            cir.setReturnValue((Object)true);
        }
    }
}

