package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public final class BalloonIntegrity {
   private BalloonIntegrity() {
   }

   public static void onPlotBlockChanged(ServerLevel level, BlockPos latticePos, BlockState newState) {
      if (!newState.isAir()) {
         if (!(Sable.HELPER.getContaining(level, latticePos) instanceof ServerSubLevel balloon) || balloon.isRemoved()) {
            return;
         }

         if (BalloonResolve.isBalloonSubLevel(balloon)) {
            BlockPos plotLocal = latticePos.subtract(balloon.getPlot().getCenterBlock());
            if (!plotLocal.equals(BalloonHostKey.BALLOON_BLOCK_CELL) || !BalloonResolve.isBalloonBlockState(newState)) {
               BalloonRemoval.pop(level, balloon);
            }
         }
      }
   }
}
