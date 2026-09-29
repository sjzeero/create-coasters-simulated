/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.ChainLiftClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftEdgeIndex;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftSpanQuery {
    public static final double T_EPS = 1.0E-5;

    private ChainLiftSpanQuery() {
    }

    @Nullable
    public static ChainLiftSpanHit findAtGraphHit(Level level, CoasterPathTrackFrame.GraphHit hit, double partialTickForRender) {
        @Nullable ChainLiftSpanHit indexed = ChainLiftEdgeIndex.findAtGraphHit(level, hit, partialTickForRender);
        if (indexed != null) {
            return indexed;
        }
        for (CoasterAnchorpointBlockEntity anchor : ChainLiftClientIndex.snapshotAnchors()) {
            CoasterPathGraphSpans.SpanPath path;
            if (anchor.isRemoved() || anchor.getLevel() != level || (path = ChainLiftSpanPathCache.resolve(anchor, partialTickForRender)) == null || !ChainLiftSpanQuery.graphHitOnSpan(path, hit)) continue;
            return new ChainLiftSpanHit(anchor, path);
        }
        return null;
    }

    public static boolean graphHitOnSpan(CoasterPathGraphSpans.SpanPath path, CoasterPathTrackFrame.GraphHit hit) {
        return ChainLiftSpanQuery.segmentContaining(path, hit) != null;
    }

    public static Vec3 chainWorldVelocityAtHit(Level level, CoasterPathGraphSpans.SpanPath path, CoasterPathTrackFrame.GraphHit hit, double signedChainBlocksPerSecond, double partialTickForRender) {
         @Nullable CoasterPathGraphSpans.PathSegment segment = ChainLiftSpanQuery.segmentContaining(path, hit);
        if (segment == null) {
            return Vec3.ZERO;
        }
        Vec3 from = CoasterPathGraphManager.sampleEdgePosition(level, segment.edge(), segment.tStart(), partialTickForRender);
        Vec3 to = CoasterPathGraphManager.sampleEdgePosition(level, segment.edge(), segment.tEnd(), partialTickForRender);
        Vec3 delta = to.subtract(from);
        if (delta.lengthSqr() < 1.0E-12) {
            return Vec3.ZERO;
        }
        return delta.normalize().scale(signedChainBlocksPerSecond);
    }

    @Nullable
    private static CoasterPathGraphSpans.PathSegment segmentContaining(CoasterPathGraphSpans.SpanPath path, CoasterPathTrackFrame.GraphHit hit) {
        for (CoasterPathGraphSpans.PathSegment segment : path.segments()) {
            if (!ChainLiftSpanQuery.sameUndirectedEdge(segment.edge(), hit.edge())) continue;
            double lo = Math.min(segment.tStart(), segment.tEnd());
            double hi = Math.max(segment.tStart(), segment.tEnd());
            if (!(hit.t() >= lo - 1.0E-5) || !(hit.t() <= hi + 1.0E-5)) continue;
            return segment;
        }
        return null;
    }

    private static boolean sameUndirectedEdge(CoasterPathEdge a, CoasterPathEdge b) {
        if (a == b) {
            return true;
        }
        return a.from().equals((Object)b.from()) && a.to().equals((Object)b.to()) || a.from().equals((Object)b.to()) && a.to().equals((Object)b.from());
    }

    public record ChainLiftSpanHit(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path) {
    }
}

