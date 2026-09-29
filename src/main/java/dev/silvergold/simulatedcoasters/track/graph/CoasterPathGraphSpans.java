package dev.silvergold.simulatedcoasters.track.graph;

import dev.silvergold.simulatedcoasters.Config;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterPathGraphSpans {
   private static final int ARC_SAMPLES_PER_UNIT = 16;

   private CoasterPathGraphSpans() {
   }

   @Nullable
   public static CoasterPathGraphSpans.SpanPath spanPathFromAnchor(
      Level level, CoasterPathGraph graph, BlockPos startAnchor, CoasterPathTrackFrame.GraphHit end, double partialTickForRender
   ) {
      if (end == null) {
         return null;
      } else {
         List<CoasterPathEdge> edges = graph.edges();
         int endIdx = indexOfEdge(edges, end.edge());
         if (endIdx < 0) {
            return null;
         } else {
            CoasterPathGraphSpans.SpanPath best = null;

            for (int i = 0; i < edges.size(); i++) {
               CoasterPathEdge startEdge = edges.get(i);
               double startT;
               if (startEdge.from().equals(startAnchor)) {
                  startT = 0.0;
               } else {
                  if (!startEdge.to().equals(startAnchor)) {
                     continue;
                  }

                  startT = 1.0;
               }

               CoasterPathGraphSpans.SpanPath path = resolveSpanPathBetween(level, edges, i, startT, endIdx, end.t(), partialTickForRender);
               if (path != null && (best == null || path.totalLength() < best.totalLength())) {
                  best = path;
               }
            }

            return best;
         }
      }
   }

   @Nullable
   public static CoasterPathGraphSpans.SpanPath spanPathBetweenAnchors(
      Level level, CoasterPathGraph graph, BlockPos startAnchor, BlockPos endAnchor, double partialTickForRender
   ) {
      if (startAnchor.equals(endAnchor)) {
         return null;
      } else {
         CoasterPathGraphSpans.SpanPath best = null;

         for (CoasterPathTrackFrame.GraphHit endHit : anchorJunctionHits(level, graph, endAnchor, partialTickForRender)) {
            CoasterPathGraphSpans.SpanPath path = spanPathFromAnchor(level, graph, startAnchor, endHit, partialTickForRender);
            if (path != null && (best == null || path.totalLength() < best.totalLength())) {
               best = path;
            }
         }

         return best;
      }
   }

   public static List<CoasterPathTrackFrame.GraphHit> anchorJunctionHits(Level level, CoasterPathGraph graph, BlockPos anchor, double partialTickForRender) {
      Vec3 center = CoasterPathGraphManager.sampleNodePosition(level, new CoasterPathNode(anchor, Vec3.ZERO), partialTickForRender);
      List<CoasterPathTrackFrame.GraphHit> hits = new ArrayList<>();

      for (CoasterPathEdge edge : graph.edges()) {
         double t;
         if (edge.from().equals(anchor)) {
            t = 0.0;
         } else {
            if (!edge.to().equals(anchor)) {
               continue;
            }

            t = 1.0;
         }

         hits.add(new CoasterPathTrackFrame.GraphHit(center, edge, t));
      }

      return hits;
   }

   @Nullable
   public static Double pathLengthFromAnchor(
      Level level, CoasterPathGraph graph, BlockPos startAnchor, CoasterPathTrackFrame.GraphHit end, double partialTickForRender
   ) {
      CoasterPathGraphSpans.SpanPath path = spanPathFromAnchor(level, graph, startAnchor, end, partialTickForRender);
      return path == null ? null : path.totalLength();
   }

   public static boolean isWithinChainLiftSpan(@Nullable Double pathLengthBlocks) {
      return pathLengthBlocks != null && pathLengthBlocks <= Config.maxChainLiftSpanBlocks();
   }

   public static double arcLengthBetween(Level level, CoasterPathEdge edge, double t0, double t1, double partialTickForRender) {
      return arcLengthBetweenInternal(level, edge, t0, t1, partialTickForRender);
   }

   public static CoasterPathGraphSpans.SpanPath truncate(CoasterPathGraphSpans.SpanPath path, double maxLength, Level level, double partialTickForRender) {
      if (!path.segments().isEmpty() && !(maxLength <= 0.0)) {
         if (path.totalLength() <= maxLength + 1.0E-9) {
            return path;
         } else {
            List<CoasterPathGraphSpans.PathSegment> out = new ArrayList<>();
            double walked = 0.0;

            for (CoasterPathGraphSpans.PathSegment segment : path.segments()) {
               double segLen = arcLengthBetweenInternal(level, segment.edge(), segment.tStart(), segment.tEnd(), partialTickForRender);
               if (!(walked + segLen <= maxLength + 1.0E-9)) {
                  double remaining = maxLength - walked;
                  if (!(remaining <= 1.0E-9)) {
                     double frac = segLen < 1.0E-9 ? 0.0 : remaining / segLen;
                     double cutT = Mth.lerp(frac, segment.tStart(), segment.tEnd());
                     out.add(new CoasterPathGraphSpans.PathSegment(segment.edge(), segment.tStart(), cutT));
                     walked = maxLength;
                  }
                  break;
               }

               out.add(segment);
               walked += segLen;
            }

            return new CoasterPathGraphSpans.SpanPath(List.copyOf(out), walked);
         }
      } else {
         return new CoasterPathGraphSpans.SpanPath(List.of(), 0.0);
      }
   }

   public static Vec3 samplePositionAtArcLength(Level level, CoasterPathGraphSpans.SpanPath path, double arcLength, double partialTickForRender) {
      CoasterPathTrackFrame.GraphHit hit = sampleGraphHitAtArcLength(level, path, arcLength, partialTickForRender);
      return hit == null ? Vec3.ZERO : hit.point();
   }

   @Nullable
   public static CoasterPathTrackFrame.GraphHit sampleGraphHitAtArcLength(
      Level level, CoasterPathGraphSpans.SpanPath path, double arcLength, double partialTickForRender
   ) {
      if (path.segments().isEmpty()) {
         return null;
      } else {
         double target = Mth.clamp(arcLength, 0.0, path.totalLength());
         double walked = 0.0;

         for (CoasterPathGraphSpans.PathSegment segment : path.segments()) {
            double segLen = arcLengthBetweenInternal(level, segment.edge(), segment.tStart(), segment.tEnd(), partialTickForRender);
            if (walked + segLen >= target - 1.0E-9) {
               double local = target - walked;
               double frac = segLen < 1.0E-9 ? 0.0 : local / segLen;
               double t = Mth.lerp(frac, segment.tStart(), segment.tEnd());
               Vec3 point = samplePositionForArcParameter(level, segment.edge(), t, partialTickForRender);
               return new CoasterPathTrackFrame.GraphHit(point, segment.edge(), t);
            }

            walked += segLen;
         }

         CoasterPathGraphSpans.PathSegment last = path.segments().getLast();
         Vec3 point = samplePositionForArcParameter(level, last.edge(), last.tEnd(), partialTickForRender);
         return new CoasterPathTrackFrame.GraphHit(point, last.edge(), last.tEnd());
      }
   }

   @Nullable
   private static CoasterPathGraphSpans.SpanPath resolveSpanPathBetween(
      Level level, List<CoasterPathEdge> edges, int startEdgeIdx, double startT, int endEdgeIdx, double endT, double partialTickForRender
   ) {
      CoasterPathEdge startEdge = edges.get(startEdgeIdx);
      CoasterPathEdge endEdge = edges.get(endEdgeIdx);
      if (startEdgeIdx == endEdgeIdx) {
         double len = arcLengthBetweenInternal(level, startEdge, startT, endT, partialTickForRender);
         return new CoasterPathGraphSpans.SpanPath(List.of(new CoasterPathGraphSpans.PathSegment(startEdge, startT, endT)), len);
      } else {
         BlockPos startAnchor = nodeAtT(startEdge, startT);
         double startExitT = startT <= 0.5 ? 1.0 : 0.0;
         BlockPos startExitNode = nodeAtT(startEdge, startExitT);
         double baseDist = arcLengthBetweenInternal(level, startEdge, startT, startExitT, partialTickForRender);
         PriorityQueue<CoasterPathGraphSpans.NodeDist> queue = new PriorityQueue<>(Comparator.comparingDouble(CoasterPathGraphSpans.NodeDist::distance));
         Map<BlockPos, Double> bestDist = new HashMap<>();
         Map<BlockPos, CoasterPathGraphSpans.Arrival> cameFrom = new HashMap<>();
         queue.add(new CoasterPathGraphSpans.NodeDist(startExitNode, baseDist));
         bestDist.put(startExitNode, baseDist);
         CoasterPathGraphSpans.EndCandidate bestEnd = null;

         while (!queue.isEmpty()) {
            CoasterPathGraphSpans.NodeDist current = queue.poll();
            double dist = current.distance();
            Double recorded = bestDist.get(current.node());
            if (recorded != null && !(dist > recorded + 1.0E-6)) {
               for (int edgeIdx = 0; edgeIdx < edges.size(); edgeIdx++) {
                  CoasterPathEdge edge = edges.get(edgeIdx);
                  if (!edge.from().equals(edge.to())) {
                     BlockPos nextNode;
                     double entryT;
                     double exitT;
                     if (edge.from().equals(current.node())) {
                        nextNode = edge.to();
                        entryT = 0.0;
                        exitT = 1.0;
                     } else {
                        if (!edge.to().equals(current.node())) {
                           continue;
                        }

                        nextNode = edge.from();
                        entryT = 1.0;
                        exitT = 0.0;
                     }

                     if (edgeIdx != startEdgeIdx || !nextNode.equals(startAnchor)) {
                        if (edgeIdx == endEdgeIdx) {
                           double candidate = dist + arcLengthBetweenInternal(level, edge, entryT, endT, partialTickForRender);
                           if (bestEnd == null || candidate < bestEnd.distance()) {
                              bestEnd = new CoasterPathGraphSpans.EndCandidate(candidate, current.node(), entryT);
                           }
                        } else {
                           double candidate = dist + arcLengthBetweenInternal(level, edge, entryT, exitT, partialTickForRender);
                           Double prev = bestDist.get(nextNode);
                           if (prev == null || candidate + 1.0E-6 < prev) {
                              bestDist.put(nextNode, candidate);
                              cameFrom.put(nextNode, new CoasterPathGraphSpans.Arrival(current.node(), edgeIdx, entryT, exitT));
                              queue.add(new CoasterPathGraphSpans.NodeDist(nextNode, candidate));
                           }
                        }
                     }
                  }
               }
            }
         }

         if (bestEnd == null) {
            return null;
         } else {
            List<CoasterPathGraphSpans.PathSegment> segments = new ArrayList<>();
            segments.add(new CoasterPathGraphSpans.PathSegment(endEdge, bestEnd.entryT(), endT));
            BlockPos node = bestEnd.entryNode();

            while (node != null && !node.equals(startExitNode)) {
               CoasterPathGraphSpans.Arrival arrival = cameFrom.get(node);
               if (arrival == null) {
                  return null;
               }

               segments.add(0, new CoasterPathGraphSpans.PathSegment(edges.get(arrival.edgeIdx()), arrival.entryT(), arrival.exitT()));
               node = arrival.prevNode();
            }

            segments.add(0, new CoasterPathGraphSpans.PathSegment(startEdge, startT, startExitT));
            return new CoasterPathGraphSpans.SpanPath(List.copyOf(segments), bestEnd.distance());
         }
      }
   }

   private static BlockPos nodeAtT(CoasterPathEdge edge, double t) {
      return t <= 0.5 ? edge.from() : edge.to();
   }

   private static double arcLengthBetweenInternal(Level level, CoasterPathEdge edge, double t0, double t1, @Nullable Double partialTickForRender) {
      double lo = Math.min(t0, t1);
      double hi = Math.max(t0, t1);
      if (hi - lo < 1.0E-9) {
         return 0.0;
      } else {
         int steps = Math.max(4, (int)Math.ceil((hi - lo) * 16.0));
         Vec3 prev = samplePositionForArcParameter(level, edge, lo, partialTickForRender);
         double sum = 0.0;

         for (int i = 1; i <= steps; i++) {
            double t = Mth.lerp((double)i / (double)steps, lo, hi);
            Vec3 cur = samplePositionForArcParameter(level, edge, t, partialTickForRender);
            sum += cur.distanceTo(prev);
            prev = cur;
         }

         return sum;
      }
   }

   private static Vec3 samplePositionForArcParameter(Level level, CoasterPathEdge edge, double t, @Nullable Double partialTickForRender) {
      return partialTickForRender != null && level.isClientSide() && CoasterPathGraphManager.edgeHostedOnSubLevel(level, edge)
         ? CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, t)
         : CoasterPathGraphManager.sampleEdgePosition(level, edge, t, partialTickForRender);
   }

   private static int indexOfEdge(List<CoasterPathEdge> edges, CoasterPathEdge target) {
      for (int i = 0; i < edges.size(); i++) {
         if (edges.get(i) == target) {
            return i;
         }
      }

      return -1;
   }

   private static record Arrival(BlockPos prevNode, int edgeIdx, double entryT, double exitT) {
   }

   private static record EndCandidate(double distance, BlockPos entryNode, double entryT) {
   }

   private static record NodeDist(BlockPos node, double distance) {
   }

   public static record PathSegment(CoasterPathEdge edge, double tStart, double tEnd) {
   }

   public static final class SpanArcSampler {
      private final CoasterPathGraphSpans.SpanPath path;
      private final double[] cumulativeEnds;

      private SpanArcSampler(CoasterPathGraphSpans.SpanPath path, double[] cumulativeEnds) {
         this.path = path;
         this.cumulativeEnds = cumulativeEnds;
      }

      public static CoasterPathGraphSpans.SpanArcSampler of(Level level, CoasterPathGraphSpans.SpanPath path, double partialTickForRender) {
         return of(level, path, Double.valueOf(partialTickForRender));
      }

      public static CoasterPathGraphSpans.SpanArcSampler of(Level level, CoasterPathGraphSpans.SpanPath path, @Nullable Double partialTickForRender) {
         List<CoasterPathGraphSpans.PathSegment> segments = path.segments();
         double[] cumulative = new double[segments.size() + 1];

         for (int i = 0; i < segments.size(); i++) {
            CoasterPathGraphSpans.PathSegment segment = segments.get(i);
            cumulative[i + 1] = cumulative[i]
               + CoasterPathGraphSpans.arcLengthBetweenInternal(level, segment.edge(), segment.tStart(), segment.tEnd(), partialTickForRender);
         }

         return new CoasterPathGraphSpans.SpanArcSampler(path, cumulative);
      }

      public CoasterPathGraphSpans.SpanPath path() {
         return this.path;
      }

      public double totalLength() {
         return this.cumulativeEnds.length == 0 ? 0.0 : this.cumulativeEnds[this.cumulativeEnds.length - 1];
      }

      @Nullable
      public CoasterPathTrackFrame.GraphHit sampleGraphHit(Level level, double arcLength, double partialTickForRender) {
         return this.sampleGraphHit(level, arcLength, Double.valueOf(partialTickForRender));
      }

      @Nullable
      public CoasterPathTrackFrame.GraphHit sampleGraphHit(Level level, double arcLength, @Nullable Double partialTickForRender) {
         if (this.path.segments().isEmpty()) {
            return null;
         } else {
            double target = Mth.clamp(arcLength, 0.0, this.totalLength());
            int segIdx = this.segmentIndexAt(target);
            CoasterPathGraphSpans.PathSegment segment = this.path.segments().get(segIdx);
            double segStart = this.cumulativeEnds[segIdx];
            double segLen = this.cumulativeEnds[segIdx + 1] - segStart;
            double frac = segLen < 1.0E-9 ? 0.0 : (target - segStart) / segLen;
            double t = Mth.lerp(frac, segment.tStart(), segment.tEnd());
            Vec3 point = CoasterPathGraphSpans.samplePositionForArcParameter(level, segment.edge(), t, partialTickForRender);
            return new CoasterPathTrackFrame.GraphHit(point, segment.edge(), t);
         }
      }

      private int segmentIndexAt(double arcLength) {
         int lo = 0;
         int hi = this.path.segments().size() - 1;

         while (lo < hi) {
            int mid = lo + hi + 1 >>> 1;
            if (this.cumulativeEnds[mid] <= arcLength + 1.0E-9) {
               lo = mid;
            } else {
               hi = mid - 1;
            }
         }

         return lo;
      }
   }

   public static record SpanPath(List<CoasterPathGraphSpans.PathSegment> segments, double totalLength) {
   }
}
