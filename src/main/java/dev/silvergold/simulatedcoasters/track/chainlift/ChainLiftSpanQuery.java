package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.ChainLiftClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
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
   public static ChainLiftSpanQuery.ChainLiftSpanHit findAtGraphHit(Level level, CoasterPathTrackFrame.GraphHit hit, double partialTickForRender) {
      ChainLiftSpanQuery.ChainLiftSpanHit indexed = ChainLiftEdgeIndex.findAtGraphHit(level, hit, partialTickForRender);
      if (indexed != null) {
         return indexed;
      } else {
         for (CoasterAnchorpointBlockEntity anchor : ChainLiftClientIndex.snapshotAnchors()) {
            if (!anchor.isRemoved() && anchor.getLevel() == level) {
               CoasterPathGraphSpans.SpanPath path = ChainLiftSpanPathCache.resolve(anchor, partialTickForRender);
               if (path != null && graphHitOnSpan(path, hit)) {
                  return new ChainLiftSpanQuery.ChainLiftSpanHit(anchor, path);
               }
            }
         }

         return null;
      }
   }

   public static boolean graphHitOnSpan(CoasterPathGraphSpans.SpanPath path, CoasterPathTrackFrame.GraphHit hit) {
      return segmentContaining(path, hit) != null;
   }

   public static Vec3 chainWorldVelocityAtHit(
      Level level, CoasterPathGraphSpans.SpanPath path, CoasterPathTrackFrame.GraphHit hit, double signedChainBlocksPerSecond, double partialTickForRender
   ) {
      CoasterPathGraphSpans.PathSegment segment = segmentContaining(path, hit);
      if (segment == null) {
         return Vec3.ZERO;
      } else {
         Vec3 from = CoasterPathGraphManager.sampleEdgePosition(level, segment.edge(), segment.tStart(), partialTickForRender);
         Vec3 to = CoasterPathGraphManager.sampleEdgePosition(level, segment.edge(), segment.tEnd(), partialTickForRender);
         Vec3 delta = to.subtract(from);
         return delta.lengthSqr() < 1.0E-12 ? Vec3.ZERO : delta.normalize().scale(signedChainBlocksPerSecond);
      }
   }

   @Nullable
   private static CoasterPathGraphSpans.PathSegment segmentContaining(CoasterPathGraphSpans.SpanPath path, CoasterPathTrackFrame.GraphHit hit) {
      for (CoasterPathGraphSpans.PathSegment segment : path.segments()) {
         if (sameUndirectedEdge(segment.edge(), hit.edge())) {
            double lo = Math.min(segment.tStart(), segment.tEnd());
            double hi = Math.max(segment.tStart(), segment.tEnd());
            if (hit.t() >= lo - 1.0E-5 && hit.t() <= hi + 1.0E-5) {
               return segment;
            }
         }
      }

      return null;
   }

   private static boolean sameUndirectedEdge(CoasterPathEdge a, CoasterPathEdge b) {
      return a == b ? true : a.from().equals(b.from()) && a.to().equals(b.to()) || a.from().equals(b.to()) && a.to().equals(b.from());
   }

   public static record ChainLiftSpanHit(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path) {
   }
}
