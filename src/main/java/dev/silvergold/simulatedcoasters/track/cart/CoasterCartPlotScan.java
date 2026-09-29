package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

public final class CoasterCartPlotScan {
   private CoasterCartPlotScan() {
   }

   @Nullable
   public static BlockPos representativeBearingPlotPos(LevelPlot plot, Block bearingBlock) {
      return scanBearingCells(plot, bearingBlock).representative();
   }

   public static CoasterCartPlotScan.BearingScanResult scanBearingCells(LevelPlot plot, Block bearingBlock) {
      MutableBlockPos mut = new MutableBlockPos();
      BlockPos best = null;
      int count = 0;
      List<BlockPos> cells = null;

      for (PlotChunkHolder holder : plot.getLoadedChunks()) {
         LevelChunk chunk = holder.getChunk();
         if (chunk != null) {
            int chunkBaseX = chunk.getPos().getMinBlockX();
            int chunkBaseZ = chunk.getPos().getMinBlockZ();

            for (int sectionIndex = 0; sectionIndex < chunk.getSectionsCount(); sectionIndex++) {
               LevelChunkSection section = chunk.getSection(sectionIndex);
               if (section != null && !section.hasOnlyAir()) {
                  int minBlockY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(sectionIndex));

                  for (int lx = 0; lx < 16; lx++) {
                     for (int ly = 0; ly < 16; ly++) {
                        for (int lz = 0; lz < 16; lz++) {
                           if (section.getBlockState(lx, ly, lz).is(bearingBlock)) {
                              count++;
                              mut.set(chunkBaseX + lx, minBlockY + ly, chunkBaseZ + lz);
                              BlockPos cur = mut.immutable();
                              if (cells == null) {
                                 cells = new ArrayList<>(4);
                              }

                              cells.add(cur);
                              if (best == null || compareYXZ(cur, best) < 0) {
                                 best = cur;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return best == null ? CoasterCartPlotScan.BearingScanResult.empty() : new CoasterCartPlotScan.BearingScanResult(best, count, List.copyOf(cells));
   }

   private static int compareYXZ(BlockPos a, BlockPos b) {
      if (a.getY() != b.getY()) {
         return Integer.compare(a.getY(), b.getY());
      } else {
         return a.getX() != b.getX() ? Integer.compare(a.getX(), b.getX()) : Integer.compare(a.getZ(), b.getZ());
      }
   }

   public static void forEachBearingWorldCell(LevelPlot plot, Block bearingBlock, Consumer<MutableBlockPos> visitor) {
      MutableBlockPos mut = new MutableBlockPos();

      for (PlotChunkHolder holder : plot.getLoadedChunks()) {
         LevelChunk chunk = holder.getChunk();
         if (chunk != null) {
            int chunkBaseX = chunk.getPos().getMinBlockX();
            int chunkBaseZ = chunk.getPos().getMinBlockZ();

            for (int sectionIndex = 0; sectionIndex < chunk.getSectionsCount(); sectionIndex++) {
               LevelChunkSection section = chunk.getSection(sectionIndex);
               if (section != null && !section.hasOnlyAir()) {
                  int minBlockY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(sectionIndex));

                  for (int lx = 0; lx < 16; lx++) {
                     for (int ly = 0; ly < 16; ly++) {
                        for (int lz = 0; lz < 16; lz++) {
                           if (section.getBlockState(lx, ly, lz).is(bearingBlock)) {
                              mut.set(chunkBaseX + lx, minBlockY + ly, chunkBaseZ + lz);
                              visitor.accept(mut);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static record BearingScanResult(@Nullable BlockPos representative, int bearingCount, List<BlockPos> cells) {
      public static CoasterCartPlotScan.BearingScanResult empty() {
         return new CoasterCartPlotScan.BearingScanResult(null, 0, List.of());
      }
   }
}
