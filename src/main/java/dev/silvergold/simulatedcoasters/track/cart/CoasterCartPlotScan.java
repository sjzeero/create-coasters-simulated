/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.plot.LevelPlot
 *  dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.SectionPos
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.chunk.LevelChunkSection
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

public final class CoasterCartPlotScan {
    private CoasterCartPlotScan() {
    }

    @Nullable
    public static BlockPos representativeBearingPlotPos(LevelPlot plot, Block bearingBlock) {
        return CoasterCartPlotScan.scanBearingCells(plot, bearingBlock).representative();
    }

    public static BearingScanResult scanBearingCells(LevelPlot plot, Block bearingBlock) {
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        int count = 0;
        ArrayList<BlockPos> cells = null;
        for (PlotChunkHolder holder : plot.getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            if (chunk == null) continue;
            int chunkBaseX = chunk.getPos().getMinBlockX();
            int chunkBaseZ = chunk.getPos().getMinBlockZ();
            for (int sectionIndex = 0; sectionIndex < chunk.getSectionsCount(); ++sectionIndex) {
                LevelChunkSection section = chunk.getSection(sectionIndex);
                if (section == null || section.hasOnlyAir()) continue;
                int minBlockY = SectionPos.sectionToBlockCoord((int)chunk.getSectionYFromSectionIndex(sectionIndex));
                for (int lx = 0; lx < 16; ++lx) {
                    for (int ly = 0; ly < 16; ++ly) {
                        for (int lz = 0; lz < 16; ++lz) {
                            if (!section.getBlockState(lx, ly, lz).is(bearingBlock)) continue;
                            ++count;
                            mut.set(chunkBaseX + lx, minBlockY + ly, chunkBaseZ + lz);
                            BlockPos cur = mut.immutable();
                            if (cells == null) {
                                cells = new ArrayList<BlockPos>(4);
                            }
                            cells.add(cur);
                            if (best != null && CoasterCartPlotScan.compareYXZ(cur, best) >= 0) continue;
                            best = cur;
                        }
                    }
                }
            }
        }
        if (best == null) {
            return BearingScanResult.empty();
        }
        return new BearingScanResult(best, count, List.copyOf(cells));
    }

    private static int compareYXZ(BlockPos a, BlockPos b) {
        if (a.getY() != b.getY()) {
            return Integer.compare(a.getY(), b.getY());
        }
        if (a.getX() != b.getX()) {
            return Integer.compare(a.getX(), b.getX());
        }
        return Integer.compare(a.getZ(), b.getZ());
    }

    public static void forEachBearingWorldCell(LevelPlot plot, Block bearingBlock, Consumer<BlockPos.MutableBlockPos> visitor) {
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        for (PlotChunkHolder holder : plot.getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            if (chunk == null) continue;
            int chunkBaseX = chunk.getPos().getMinBlockX();
            int chunkBaseZ = chunk.getPos().getMinBlockZ();
            for (int sectionIndex = 0; sectionIndex < chunk.getSectionsCount(); ++sectionIndex) {
                LevelChunkSection section = chunk.getSection(sectionIndex);
                if (section == null || section.hasOnlyAir()) continue;
                int minBlockY = SectionPos.sectionToBlockCoord((int)chunk.getSectionYFromSectionIndex(sectionIndex));
                for (int lx = 0; lx < 16; ++lx) {
                    for (int ly = 0; ly < 16; ++ly) {
                        for (int lz = 0; lz < 16; ++lz) {
                            if (!section.getBlockState(lx, ly, lz).is(bearingBlock)) continue;
                            mut.set(chunkBaseX + lx, minBlockY + ly, chunkBaseZ + lz);
                            visitor.accept(mut);
                        }
                    }
                }
            }
        }
    }

    public record BearingScanResult(@Nullable BlockPos representative, int bearingCount, List<BlockPos> cells) {
        public static BearingScanResult empty() {
            return new BearingScanResult(null, 0, List.of());
        }
    }
}

