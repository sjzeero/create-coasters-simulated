package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class CoasterCartDuplicateEnforcer {
   private CoasterCartDuplicateEnforcer() {
   }

   static boolean enforceSingleCart(ServerSubLevel sub) {
      LevelPlot plot = sub.getPlot();
      Block bearingBlock = (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
      CoasterCartPlotScan.BearingScanResult scan = CoasterCartBearingTickCache.scan(sub);
      BlockPos kept = scan.representative();
      if (kept == null) {
         return false;
      } else if (scan.bearingCount() <= 1) {
         return false;
      } else {
         BlockPos plotOrigin = plot.getCenterBlock();
         EmbeddedPlotLevelAccessor embedded = plot.getEmbeddedLevelAccessor();
         boolean[] removed = new boolean[]{false};
         CoasterCartPlotScan.forEachBearingWorldCell(plot, bearingBlock, mut -> {
            if (!kept.equals(mut)) {
               BlockState state = sub.getLevel().getBlockState(mut);
               if (state.is(bearingBlock)) {
                  BlockPos local = mut.immutable().subtract(plotOrigin);
                  if (embedded.destroyBlock(local, true, null, 3)) {
                     removed[0] = true;
                  }
               }
            }
         });
         return removed[0];
      }
   }

   static void breakCartBlocksOnWorldGridAfterMove(ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> movedSources) {
      Block bearingBlock = (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();

      for (BlockPos src : movedSources) {
         if (src != null) {
            BlockPos dst = transform.apply(src);
            BlockState state = level.getBlockState(dst);
            if (state.is(bearingBlock) && !shouldRetainCartBlockAt(level, dst, bearingBlock)) {
               level.destroyBlock(dst, true, null, 3);
            }
         }
      }
   }

   private static boolean shouldRetainCartBlockAt(ServerLevel level, BlockPos levelPos, Block bearingBlock) {
      SubLevel sub = Sable.HELPER.getContaining(level, levelPos);
      if (sub != null && CoasterCartResolve.isCartSubLevel(sub)) {
         BlockPos kept = sub instanceof ServerSubLevel server
            ? CoasterCartBearingTickCache.scan(server).representative()
            : CoasterCartPlotScan.representativeBearingPlotPos(sub.getPlot(), bearingBlock);
         return kept != null && kept.equals(levelPos);
      } else {
         return false;
      }
   }
}
