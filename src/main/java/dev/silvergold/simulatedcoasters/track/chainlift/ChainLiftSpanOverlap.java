package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.ChainLiftClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
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
      for (CoasterAnchorpointBlockEntity existing : chainLiftAnchors(level)) {
         if (!existing.getBlockPos().equals(newStartAnchor)) {
            CoasterPathGraphSpans.SpanPath existingPath = resolveExistingPath(existing, partialTickForRender);
            if (existingPath != null && spansOverlap(candidate, existingPath)) {
               return true;
            }
         }
      }

      return false;
   }

   public static boolean spansOverlap(CoasterPathGraphSpans.SpanPath a, CoasterPathGraphSpans.SpanPath b) {
      for (CoasterPathGraphSpans.PathSegment segA : a.segments()) {
         for (CoasterPathGraphSpans.PathSegment segB : b.segments()) {
            if (segmentsOverlapOnEdge(segA, segB)) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean segmentsOverlapOnEdge(CoasterPathGraphSpans.PathSegment a, CoasterPathGraphSpans.PathSegment b) {
      if (!sameUndirectedEdge(a.edge(), b.edge())) {
         return false;
      } else {
         double loA = Math.min(a.tStart(), a.tEnd());
         double hiA = Math.max(a.tStart(), a.tEnd());
         double loB = Math.min(b.tStart(), b.tEnd());
         double hiB = Math.max(b.tStart(), b.tEnd());
         return Math.max(loA, loB) + 1.0E-5 < Math.min(hiA, hiB);
      }
   }

   private static boolean sameUndirectedEdge(CoasterPathEdge a, CoasterPathEdge b) {
      return a == b ? true : a.from().equals(b.from()) && a.to().equals(b.to()) || a.from().equals(b.to()) && a.to().equals(b.from());
   }

   static List<CoasterAnchorpointBlockEntity> chainLiftAnchors(Level level) {
      List<CoasterAnchorpointBlockEntity> out = new ArrayList<>();

      for (CoasterAnchorpointBlockEntity be : ChainLiftClientIndex.snapshotAnchors()) {
         if (!be.isRemoved() && be.getLevel() == level) {
            out.add(be);
         }
      }

      return out;
   }

   @Nullable
   private static CoasterPathGraphSpans.SpanPath resolveExistingPath(CoasterAnchorpointBlockEntity existing, double partialTickForRender) {
      return ChainLiftSpanPathCache.resolve(existing, partialTickForRender);
   }
}
