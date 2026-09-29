package dev.silvergold.simulatedcoasters.mixin;

import dev.silvergold.simulatedcoasters.balloon.BalloonHostIndex;
import dev.silvergold.simulatedcoasters.balloon.BalloonIntegrity;
import dev.silvergold.simulatedcoasters.rivet.RivetAssemblyMoveContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LevelChunk.class})
public class LevelChunkBalloonHostMixin {
   @Shadow
   @Final
   Level level;

   @Inject(
      method = {"setBlockState"},
      at = {@At("RETURN")}
   )
   private void simulatedcoasters$balloonPlotBlockChanged(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
      if (!this.level.isClientSide() && this.level instanceof ServerLevel serverLevel) {
         if (!RivetAssemblyMoveContext.isActive()) {
            BlockState previous = (BlockState)cir.getReturnValue();
            if (previous != null) {
               if (!previous.isAir() && state.isAir()) {
                  BalloonHostIndex.get(serverLevel).breakBalloonsOnHostCell(serverLevel, pos);
               }

               if (!state.isAir() && previous != state) {
                  BalloonIntegrity.onPlotBlockChanged(serverLevel, pos, state);
               }
            }
         }
      }
   }
}
