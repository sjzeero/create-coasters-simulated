/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.ChainLiftClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftSpanOverlap {
    private static final double T_EPS = 1.0E-5;

    private ChainLiftSpanOverlap() {
    }

    public static boolean overlapsAnyExisting(Level level, CoasterPathGraphSpans.SpanPath candidate, BlockPos newStartAnchor, double partialTickForRender) {
        for (CoasterAnchorpointBlockEntity existing : ChainLiftSpanOverlap.chainLiftAnchors(level)) {
            CoasterPathGraphSpans.SpanPath existingPath;
            if (existing.getBlockPos().equals((Object)newStartAnchor) || (existingPath = ChainLiftSpanOverlap.resolveExistingPath(existing, partialTickForRender)) == null || !ChainLiftSpanOverlap.spansOverlap(candidate, existingPath)) continue;
            return true;
        }
        return false;
    }

    public static boolean spansOverlap(CoasterPathGraphSpans.SpanPath a, CoasterPathGraphSpans.SpanPath b) {
        for (CoasterPathGraphSpans.PathSegment segA : a.segments()) {
            for (CoasterPathGraphSpans.PathSegment segB : b.segments()) {
                if (!ChainLiftSpanOverlap.segmentsOverlapOnEdge(segA, segB)) continue;
                return true;
            }
        }
        return false;
    }

    private static boolean segmentsOverlapOnEdge(CoasterPathGraphSpans.PathSegment a, CoasterPathGraphSpans.PathSegment b) {
        if (!ChainLiftSpanOverlap.sameUndirectedEdge(a.edge(), b.edge())) {
            return false;
        }
        double loA = Math.min(a.tStart(), a.tEnd());
        double hiA = Math.max(a.tStart(), a.tEnd());
        double loB = Math.min(b.tStart(), b.tEnd());
        double hiB = Math.max(b.tStart(), b.tEnd());
        return Math.max(loA, loB) + 1.0E-5 < Math.min(hiA, hiB);
    }

    private static boolean sameUndirectedEdge(CoasterPathEdge a, CoasterPathEdge b) {
        if (a == b) {
            return true;
        }
        return a.from().equals((Object)b.from()) && a.to().equals((Object)b.to()) || a.from().equals((Object)b.to()) && a.to().equals((Object)b.from());
    }

    static List<CoasterAnchorpointBlockEntity> chainLiftAnchors(Level level) {
        ArrayList<CoasterAnchorpointBlockEntity> out = new ArrayList<CoasterAnchorpointBlockEntity>();
        for (CoasterAnchorpointBlockEntity be : ChainLiftClientIndex.snapshotAnchors()) {
            if (be.isRemoved() || be.getLevel() != level) continue;
            out.add(be);
        }
        return out;
    }

    @Nullable
    private static CoasterPathGraphSpans.SpanPath resolveExistingPath(CoasterAnchorpointBlockEntity existing, double partialTickForRender) {
        return ChainLiftSpanPathCache.resolve(existing, partialTickForRender);
    }
}

