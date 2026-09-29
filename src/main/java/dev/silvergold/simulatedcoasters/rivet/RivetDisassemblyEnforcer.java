package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlotScan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class RivetDisassemblyEnforcer {
   private RivetDisassemblyEnforcer() {
   }

   private static final java.util.Map<ServerSubLevel, Long> LAST_RIVET_CHECK = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
   private static final java.util.Map<ServerSubLevel, Long> LAST_SELF_HOST_CHECK = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

   public static void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
      ServerLevel level = event.getPhysicsSystem().getLevel();
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         long gameTime = level.getGameTime();
         for (ServerSubLevel sub : container.getAllSubLevels()) {
            if (!sub.isRemoved()) {
               Long lastCheck = LAST_RIVET_CHECK.get(sub);
               if (lastCheck == null || (gameTime - lastCheck >= 30L || gameTime < lastCheck)) {
                  LAST_RIVET_CHECK.put(sub, gameTime);
                  enforceSingleRivet(sub);
               }
               Long lastPop = LAST_SELF_HOST_CHECK.get(sub);
               if (lastPop == null || (gameTime - lastPop >= 20L || gameTime < lastPop)) {
                  LAST_SELF_HOST_CHECK.put(sub, gameTime);
                  popIfSelfHosted(sub);
               }
            }
         }
      }
   }

   private static void popIfSelfHosted(ServerSubLevel sub) {
      if (RivetHostKey.isRivetSubLevel(sub)) {
         ServerLevel be = sub.getLevel();
         if (be instanceof ServerLevel) {
            if (sub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof RivetBlockEntity bex) {
               if (!be.getBlockState(bex.hostPos()).isAir() && bex.hasInvalidHost(be, sub)) {
                  RivetBlockEntity.scheduleTeardownIfInvalidHost(be, sub.getUniqueId());
               }
            }
         }
      }
   }

   public static boolean enforceSingleRivet(ServerSubLevel sub) {
      Block rivet = (Block)SimulatedCoastersBlocks.RIVET.get();
      LevelPlot plot = sub.getPlot();
      CoasterCartPlotScan.BearingScanResult scan = CoasterCartPlotScan.scanBearingCells(plot, rivet);
      if (scan.bearingCount() <= 1) {
         return false;
      } else {
         BlockPos kept = keptRivetWorldCell(plot, rivet, scan.representative());
         if (kept == null) {
            return false;
         } else {
            BlockPos plotOrigin = plot.getCenterBlock();
            EmbeddedPlotLevelAccessor embedded = plot.getEmbeddedLevelAccessor();
            boolean[] removed = new boolean[]{false};
            CoasterCartPlotScan.forEachBearingWorldCell(plot, rivet, mut -> {
               if (!kept.equals(mut)) {
                  BlockState state = sub.getLevel().getBlockState(mut);
                  if (state.is(rivet)) {
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
   }

   public static void cleanupRivetBlocksAfterMove(ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> movedSources) {
      RivetBlock rivet = (RivetBlock)SimulatedCoastersBlocks.RIVET.get();

      for (BlockPos src : movedSources) {
         if (src != null) {
            BlockPos dst = transform.apply(src);
            BlockState state = level.getBlockState(dst);
            if (state.is(rivet) && !shouldRetainRivetBlockAt(level, dst, rivet)) {
               level.destroyBlock(dst, true, null, 3);
            }
         }
      }
   }

   private static boolean shouldRetainRivetBlockAt(ServerLevel level, BlockPos levelPos, Block rivet) {
      if (Sable.HELPER.getContaining(level, levelPos) instanceof ServerSubLevel ssl && !ssl.isRemoved()) {
         CoasterCartPlotScan.BearingScanResult scan = CoasterCartPlotScan.scanBearingCells(ssl.getPlot(), rivet);
         BlockPos kept = keptRivetWorldCell(ssl.getPlot(), rivet, scan.representative());
         return kept != null && kept.equals(levelPos);
      }

      return false;
   }

   @Nullable
   private static BlockPos keptRivetWorldCell(LevelPlot plot, Block rivet, @Nullable BlockPos scanRepresentative) {
      return plot.getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO).is(rivet) ? plot.getCenterBlock() : scanRepresentative;
   }
}
