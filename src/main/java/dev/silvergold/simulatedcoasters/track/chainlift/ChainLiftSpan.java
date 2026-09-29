package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphLookup;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftSpan {
   private ChainLiftSpan() {
   }

   @Nullable
   public static CoasterPathGraphSpans.SpanPath resolveEffective(
      Level level, BlockPos startAnchor, ChainLiftEndRef endRef, double committedLength, BlockPos firstHop, double partialTickForRender
   ) {
      if (committedLength <= 0.0) {
         return null;
      } else {
         CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
         CoasterPathGraphSpans.SpanPath ideal = resolveIdealPath(level, graph, startAnchor, endRef, partialTickForRender);
         return ideal != null
            ? CoasterPathGraphSpans.truncate(ideal, committedLength, level, partialTickForRender)
            : reachableForwardSpan(level, graph, startAnchor, firstHop, committedLength, partialTickForRender);
      }
   }

   public static BlockPos firstHopNode(CoasterPathGraphSpans.SpanPath path, BlockPos startAnchor) {
      if (path.segments().isEmpty()) {
         return BlockPos.ZERO;
      } else {
         CoasterPathGraphSpans.PathSegment first = path.segments().getFirst();
         CoasterPathEdge edge = first.edge();
         if (edge.from().equals(startAnchor)) {
            return edge.to();
         } else {
            return edge.to().equals(startAnchor) ? edge.from() : exitNodeAtT(edge, first.tStart());
         }
      }
   }

   public static List<ChainLiftCommittedSegment> segmentsFromPath(CoasterPathGraphSpans.SpanPath path) {
      List<ChainLiftCommittedSegment> out = new ArrayList<>(path.segments().size());

      for (CoasterPathGraphSpans.PathSegment segment : path.segments()) {
         out.add(ChainLiftCommittedSegment.from(segment));
      }

      return List.copyOf(out);
   }

   @Nullable
   public static CoasterPathGraphSpans.SpanPath rebindCommittedSpan(
      Level level, List<ChainLiftCommittedSegment> stored, double committedLength, double partialTickForRender
   ) {
      if (!stored.isEmpty() && !(committedLength <= 0.0)) {
         CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
         List<CoasterPathGraphSpans.PathSegment> segments = new ArrayList<>(stored.size());

         for (ChainLiftCommittedSegment storedSegment : stored) {
            CoasterPathEdge edge = findEdge(graph, storedSegment.edgeFrom(), storedSegment.edgeTo());
            if (edge == null) {
               return null;
            }

            segments.add(new CoasterPathGraphSpans.PathSegment(edge, storedSegment.tStart(), storedSegment.tEnd()));
         }

         double total = 0.0;

         for (CoasterPathGraphSpans.PathSegment segment : segments) {
            total += CoasterPathGraphSpans.arcLengthBetween(level, segment.edge(), segment.tStart(), segment.tEnd(), partialTickForRender);
         }

         return CoasterPathGraphSpans.truncate(new CoasterPathGraphSpans.SpanPath(List.copyOf(segments), total), committedLength, level, partialTickForRender);
      } else {
         return null;
      }
   }

   @Nullable
   public static CoasterPathGraphSpans.SpanPath resolveIdealPath(
      Level level, CoasterPathGraph graph, BlockPos startAnchor, ChainLiftEndRef endRef, double partialTickForRender
   ) {
      return switch (endRef.kind()) {
         case ANCHOR -> {
            BlockPos end = endRef.endAnchor();
            yield !end.equals(BlockPos.ZERO) && !end.equals(startAnchor)
               ? CoasterPathGraphSpans.spanPathBetweenAnchors(level, graph, startAnchor, end, partialTickForRender)
               : null;
         }
         case CURVE -> {
            CoasterPathEdge edge = findEdge(graph, endRef.edgeFrom(), endRef.edgeTo());
            if (edge == null) {
               yield null;
            } else {
               CoasterPathTrackFrame.GraphHit hit = new CoasterPathTrackFrame.GraphHit(Vec3.ZERO, edge, endRef.endT());
               yield CoasterPathGraphSpans.spanPathFromAnchor(level, graph, startAnchor, hit, partialTickForRender);
            }
         }
      };
   }

   @Nullable
   private static CoasterPathGraphSpans.SpanPath reachableForwardSpan(
      Level level, CoasterPathGraph graph, BlockPos startAnchor, BlockPos firstHop, double maxLength, double partialTickForRender
   ) {
      if (firstHop != null && !firstHop.equals(BlockPos.ZERO) && !firstHop.equals(startAnchor)) {
         List<CoasterPathGraphSpans.PathSegment> segments = new ArrayList<>();
         double walked = 0.0;
         BlockPos prev = startAnchor;
         BlockPos current = firstHop;
         int hops = 0;
         int maxHops = Math.max(4, graph.edges().size() + 1);

         while (current != null && walked < maxLength - 1.0E-9) {
            if (++hops > maxHops) {
               break;
            }

            CoasterPathEdge edge = findEdge(graph, prev, current);
            if (edge == null) {
               break;
            }

            double entryT = edge.from().equals(prev) ? 0.0 : 1.0;
            double exitT = edge.from().equals(current) ? 0.0 : 1.0;
            double fullSegLen = CoasterPathGraphSpans.arcLengthBetween(level, edge, entryT, exitT, partialTickForRender);
            if (fullSegLen < 1.0E-9) {
               break;
            }

            double remaining = maxLength - walked;
            if (!(fullSegLen <= remaining + 1.0E-9)) {
               double frac = remaining / fullSegLen;
               double cutT = Mth.lerp(frac, entryT, exitT);
               segments.add(new CoasterPathGraphSpans.PathSegment(edge, entryT, cutT));
               walked = maxLength;
               break;
            }

            segments.add(new CoasterPathGraphSpans.PathSegment(edge, entryT, exitT));
            walked += fullSegLen;
            BlockPos next = pickNextNode(graph, current, prev);
            if (next == null || next.equals(current) || next.equals(prev)) {
               break;
            }

            prev = current;
            current = next;
         }

         return segments.isEmpty() ? null : new CoasterPathGraphSpans.SpanPath(List.copyOf(segments), walked);
      } else {
         return null;
      }
   }

   private static BlockPos exitNodeAtT(CoasterPathEdge edge, double tStart) {
      return tStart <= 0.5 ? edge.to() : edge.from();
   }

   @Nullable
   private static BlockPos pickNextNode(CoasterPathGraph graph, BlockPos current, BlockPos prev) {
      BlockPos next = null;

      for (CoasterPathEdge edge : graph.edges()) {
         BlockPos candidate;
         if (edge.from().equals(current)) {
            candidate = edge.to();
         } else {
            if (!edge.to().equals(current)) {
               continue;
            }

            candidate = edge.from();
         }

         if (!candidate.equals(prev)) {
            if (next != null && !next.equals(candidate)) {
               return null;
            }

            next = candidate;
         }
      }

      return next;
   }

   @Nullable
   private static CoasterPathEdge findEdge(CoasterPathGraph graph, BlockPos from, BlockPos to) {
      if (!from.equals(BlockPos.ZERO) && !to.equals(BlockPos.ZERO)) {
         for (CoasterPathEdge edge : graph.edges()) {
            if (edge.from().equals(from) && edge.to().equals(to)) {
               return edge;
            }

            if (edge.from().equals(to) && edge.to().equals(from)) {
               return edge;
            }
         }

         return null;
      } else {
         return null;
      }
   }
}
