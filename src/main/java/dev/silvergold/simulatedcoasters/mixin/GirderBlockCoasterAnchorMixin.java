package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.decoration.girder.GirderBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorGirderConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({GirderBlock.class})
public class GirderBlockCoasterAnchorMixin {
   @ModifyReturnValue(
      method = {"updateState"},
      at = {@At("RETURN")}
   )
   private static BlockState simulatedcoasters$connectGirderArmsToAnchor(BlockState returned, LevelAccessor level, BlockPos pos, BlockState state, Direction d) {
      BlockState sideState = level.getBlockState(pos.relative(d));
      if (!CoasterAnchorGirderConnection.acceptsGirderConnection(sideState, d)) {
         return returned;
      } else {
         Axis axis = d.getAxis();
         Property<Boolean> updateProperty = axis == Axis.X
            ? GirderBlock.X
            : (axis == Axis.Z ? GirderBlock.Z : (d == Direction.UP ? GirderBlock.TOP : GirderBlock.BOTTOM));
         return (BlockState)returned.setValue(updateProperty, true);
      }
   }

   @ModifyReturnValue(
      method = {"updateVerticalProperty"},
      at = {@At("RETURN")}
   )
   private static BlockState simulatedcoasters$connectGirderCapsToAnchor(
      BlockState returned, LevelAccessor level, BlockPos pos, BlockState state, Property<Boolean> updateProperty, BlockState sideState, Direction d
   ) {
      return CoasterAnchorGirderConnection.acceptsGirderConnection(sideState, d) ? (BlockState)returned.setValue(updateProperty, true) : returned;
   }

   @Inject(
      method = {"isConnected"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private static void simulatedcoasters$girderFlangeOnAnchor(
      BlockAndTintGetter world, BlockPos pos, BlockState state, Direction side, CallbackInfoReturnable<Boolean> cir
   ) {
      if (!cir.getReturnValueZ()) {
         BlockState neighbor = world.getBlockState(pos.relative(side));
         if (CoasterAnchorGirderConnection.acceptsGirderConnection(neighbor, side)) {
            cir.setReturnValue(true);
         }
      }
   }
}
