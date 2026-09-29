package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.content.decoration.girder.GirderBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.LevelTickAccess;

public final class CoasterAnchorGirderConnection {
   private CoasterAnchorGirderConnection() {
   }

   public static Direction mountFace(BlockState anchorState) {
      return ((Direction)anchorState.getValue(CoasterAnchorpointBlock.FACING)).getOpposite();
   }

   public static boolean acceptsGirderConnection(BlockState neighborState, Direction towardNeighbor) {
      return !(neighborState.getBlock() instanceof CoasterAnchorpointBlock) ? false : neighborState.getValue(CoasterAnchorpointBlock.FACING) == towardNeighbor;
   }

   public static void scheduleAdjacentGirderUpdates(Level level, BlockPos pos, LevelTickAccess<Block> blockTicks) {
      for (Direction d : Direction.values()) {
         BlockPos n = pos.relative(d);
         BlockState ns = level.getBlockState(n);
         Block var10 = ns.getBlock();
         if (var10 instanceof GirderBlock) {
            GirderBlock girder = (GirderBlock)var10;
            if (!blockTicks.hasScheduledTick(n, girder)) {
               level.scheduleTick(n, girder, 1);
            }
         }
      }
   }
}
