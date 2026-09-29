package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterPathTraceEndpoints {
   private static final double BEZIER_HANDLE_MATCH_BLOCKS = 0.11;
   private static final double OPEN_END_TANGENT_DT = 0.001;
   public static final double OPEN_END_HANDOFF_JOIN_BLOCKS = 0.22;
   private static final double OPEN_END_HANDOFF_JOIN_DIST_SQ = 0.0484;
   private static final double OPEN_END_HANDOFF_MIN_TANGENT_ALIGN = 0.85;

   private CoasterPathTraceEndpoints() {
   }

   public static Set<Integer> allEdgeIndices(CoasterPathGraph graph) {
      List<CoasterPathEdge> edges = graph.edges();
      Set<Integer> out = new HashSet<>(Math.max(16, edges.size() * 4 / 3));

      for (int i = 0; i < edges.size(); i++) {
         out.add(i);
      }

      return out;
   }

   public static List<Vec3> pathEndpointWorldPositions(Level level, CoasterPathGraph graph, Set<Integer> edgeIndices) {
      List<CoasterPathTraceEndpoints.OpenEndRef> refs = pathOpenEndRefs(level, graph, edgeIndices);
      if (refs.isEmpty()) {
         return List.of();
      } else {
         List<Vec3> out = new ArrayList<>(refs.size());

         for (CoasterPathTraceEndpoints.OpenEndRef ref : refs) {
            out.add(ref.position());
         }

         out.sort((a, b) -> {
            int c = Double.compare(a.x, b.x);
            if (c != 0) {
               return c;
            } else {
               c = Double.compare(a.y, b.y);
               return c != 0 ? c : Double.compare(a.z, b.z);
            }
         });
         return List.copyOf(out);
      }
   }

   public static List<CoasterPathTraceEndpoints.OpenEndRef> pathOpenEndRefs(Level level, CoasterPathGraph graph, Set<Integer> edgeIndices) {
      if (edgeIndices.isEmpty()) {
         return List.of();
      } else {
         List<CoasterPathEdge> edges = graph.edges();
         Map<BlockPos, Integer> degree = new HashMap<>();
         List<CoasterPathTraceEndpoints.OpenEndRef> out = new ArrayList<>();
         Set<BlockPos> skipDegreeOneHandledBySelfEdge = new HashSet<>();

         for (int i : edgeIndices) {
            if (i >= 0 && i < edges.size()) {
               CoasterPathEdge e = edges.get(i);
               if (e.from().equals(e.to())) {
                  addSelfEdgeTraceEndpoints(level, i, e, out, skipDegreeOneHandledBySelfEdge, edges, edgeIndices);
               } else {
                  degree.merge(e.from(), 1, Integer::sum);
                  degree.merge(e.to(), 1, Integer::sum);
               }
            }
         }

         for (Entry<BlockPos, Integer> en : degree.entrySet()) {
            if (en.getValue() == 1) {
               BlockPos p = en.getKey();
               if (!skipDegreeOneHandledBySelfEdge.contains(p)) {
                  int incIdx = uniqueHighlightedEdgeIndexIncidentOnDistinctEndpoints(edges, edgeIndices, p);
                  if (incIdx >= 0) {
                     CoasterPathEdge inc = edges.get(incIdx);
                     if (p.equals(inc.from())) {
                        out.add(new CoasterPathTraceEndpoints.OpenEndRef(openCurveEndSample(level, inc, true), incIdx, true));
                     } else {
                        out.add(new CoasterPathTraceEndpoints.OpenEndRef(openCurveEndSample(level, inc, false), incIdx, false));
                     }
                  }
               }
            }
         }

         return List.copyOf(out);
      }
   }

   public static boolean shouldDisengageAtOpenEndHit(
      Level level,
      CoasterPathGraph graph,
      @Nullable CoasterPathTrackFrame.GraphHit hit,
      List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs,
      double hitMatchDistSq
   ) {
      if (hit != null && !openEndRefs.isEmpty()) {
         List<CoasterPathTraceEndpoints.OpenEndRef> atHit = new ArrayList<>();

         for (CoasterPathTraceEndpoints.OpenEndRef ref : openEndRefs) {
            if (ref.position().distanceToSqr(hit.point()) <= hitMatchDistSq) {
               atHit.add(ref);
            }
         }

         if (atHit.isEmpty()) {
            return false;
         } else {
            List<CoasterPathEdge> edges = graph.edges();

            for (CoasterPathTraceEndpoints.OpenEndRef a : atHit) {
               boolean hasHandoff = false;

               for (CoasterPathTraceEndpoints.OpenEndRef b : openEndRefs) {
                  if (a.edgeIndex() != b.edgeIndex() && !(a.position().distanceToSqr(b.position()) > 0.0484) && openEndsFormAlignedHandoff(level, edges, a, b)) {
                     hasHandoff = true;
                     break;
                  }
               }

               if (!hasHandoff) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean openEndsFormAlignedHandoff(
      Level level, List<CoasterPathEdge> edges, CoasterPathTraceEndpoints.OpenEndRef a, CoasterPathTraceEndpoints.OpenEndRef b
   ) {
      if (a.edgeIndex() >= 0 && a.edgeIndex() < edges.size() && b.edgeIndex() >= 0 && b.edgeIndex() < edges.size()) {
         Vec3 tA = openEndOutwardTangent(level, edges.get(a.edgeIndex()), a.atFrom());
         Vec3 tB = openEndOutwardTangent(level, edges.get(b.edgeIndex()), b.atFrom());
         if (!(tA.lengthSqr() < 1.0E-12) && !(tB.lengthSqr() < 1.0E-12)) {
            double align = Math.abs(tA.normalize().dot(tB.normalize()));
            return align >= 0.85;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static Vec3 openEndOutwardTangent(Level level, CoasterPathEdge edge, boolean atFrom) {
      Vec3 straightDir = straightDirection(level, edge);
      if (straightDir != null) {
         return atFrom ? straightDir.scale(-1.0) : straightDir;
      } else if (atFrom) {
         if (CoasterPathGraphManager.isOpenCurveEnd(level, edge, true)) {
            double ext = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, true);
            double t0 = -Math.min(0.001, ext);
            return diff(level, edge, t0, 0.0);
         } else {
            double t1 = Math.min(1.0, 0.001);
            return diff(level, edge, 0.0, t1);
         }
      } else if (CoasterPathGraphManager.isOpenCurveEnd(level, edge, false)) {
         double ext = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, false);
         double t1 = Math.min(1.001, 1.0 + ext);
         return diff(level, edge, 1.0, t1);
      } else {
         double t0 = Math.max(0.0, 0.999);
         return diff(level, edge, t0, 1.0);
      }
   }

   private static Vec3 diff(Level level, CoasterPathEdge edge, double t0, double t1) {
      Vec3 a = CoasterPathGraphManager.sampleEdgePosition(level, edge, t0);
      Vec3 b = CoasterPathGraphManager.sampleEdgePosition(level, edge, t1);
      Vec3 d = b.subtract(a);
      return d.lengthSqr() < 1.0E-14 ? Vec3.ZERO : d.normalize();
   }

   @Nullable
   private static Vec3 straightDirection(Level level, CoasterPathEdge edge) {
      if (edge.straightFromWorld() != null && edge.straightToWorld() != null) {
         Vec3 d = edge.straightToWorld().subtract(edge.straightFromWorld());
         if (d.lengthSqr() < 1.0E-14) {
            return null;
         } else {
            d = CoasterPathGraphManager.sublevelWorldDirection(level, CoasterPathGraphManager.edgeSubLevelAnchor(level, edge), d);
            return d.lengthSqr() < 1.0E-14 ? null : d.normalize();
         }
      } else {
         return null;
      }
   }

   private static Vec3 openCurveEndSample(Level level, CoasterPathEdge edge, boolean atFrom) {
      if (CoasterPathGraphManager.isOpenCurveEnd(level, edge, atFrom)) {
         double ext = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, atFrom);
         return CoasterPathGraphManager.sampleEdgePosition(level, edge, atFrom ? -ext : 1.0 + ext);
      } else {
         return CoasterPathGraphManager.sampleEdgePosition(level, edge, atFrom ? 0.0 : 1.0);
      }
   }

   private static int trackBezierConnectionCount(Level level, BlockPos p) {
      BlockState st = level.getBlockState(p);
      if (st.hasProperty(TrackBlock.HAS_BE) && (Boolean)st.getValue(TrackBlock.HAS_BE)) {
         return level.getBlockEntity(p) instanceof TrackBlockEntity tbe ? tbe.getConnections().size() : 0;
      } else {
         return 0;
      }
   }

   @Nullable
   private static Vec3 bezierHandleWorldNoOffset(Level level, BezierConnection bc, BlockPos host) {
      BlockPos a = (BlockPos)bc.bePositions.getFirst();
      BlockPos b = (BlockPos)bc.bePositions.getSecond();
      double t;
      if (a.equals(host)) {
         t = 0.0;
      } else {
         if (!b.equals(host)) {
            return null;
         }

         t = 1.0;
      }

      return CoasterPathGraphManager.isCoasterGraphBezier(bc) ? CoasterPathGraphManager.coasterBezierParametricSpineRaw(bc, t) : bc.getPosition(t);
   }

   private static void addSelfEdgeTraceEndpoints(
      Level level,
      int edgeIndex,
      CoasterPathEdge e,
      List<CoasterPathTraceEndpoints.OpenEndRef> out,
      Set<BlockPos> skipDegreeOneForBlock,
      List<CoasterPathEdge> allEdges,
      Set<Integer> edgeIndices
   ) {
      BlockPos p = e.from();
      if (e.bezier() == null && e.straightFromWorld() != null && e.straightToWorld() != null) {
         int nConn = trackBezierConnectionCount(level, p);
         if (nConn < 2) {
            if (nConn == 0) {
               out.add(new CoasterPathTraceEndpoints.OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0), edgeIndex, true));
               out.add(new CoasterPathTraceEndpoints.OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0), edgeIndex, false));
            } else if (!highlightedInterBlockStraightConnectsCurveCapToAnotherCurveTrack(level, allEdges, edgeIndices, p)) {
               if (level.getBlockEntity(p) instanceof TrackBlockEntity tbe) {
                  BezierConnection var20 = (BezierConnection)tbe.getConnections().values().iterator().next();
                  Vec3 handleRaw = bezierHandleWorldNoOffset(level, var20, p);
                  if (handleRaw == null) {
                     out.add(new CoasterPathTraceEndpoints.OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0), edgeIndex, true));
                     out.add(new CoasterPathTraceEndpoints.OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0), edgeIndex, false));
                  } else {
                     Vec3 w0 = e.straightFromWorld();
                     Vec3 w1 = e.straightToWorld();
                     double r2 = 0.0121;
                     double d0 = w0.distanceToSqr(handleRaw);
                     double d1 = w1.distanceToSqr(handleRaw);
                     skipDegreeOneForBlock.add(p);
                     if (d0 <= 0.0121 && d0 <= d1) {
                        out.add(new CoasterPathTraceEndpoints.OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0), edgeIndex, false));
                     } else if (d1 <= 0.0121 && d1 <= d0) {
                        out.add(new CoasterPathTraceEndpoints.OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0), edgeIndex, true));
                     } else if (d0 > d1) {
                        out.add(new CoasterPathTraceEndpoints.OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0), edgeIndex, true));
                     } else {
                        out.add(new CoasterPathTraceEndpoints.OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0), edgeIndex, false));
                     }
                  }
               }
            }
         }
      } else {
         out.add(new CoasterPathTraceEndpoints.OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0), edgeIndex, true));
         out.add(new CoasterPathTraceEndpoints.OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0), edgeIndex, false));
      }
   }

   private static boolean highlightedInterBlockStraightConnectsCurveCapToAnotherCurveTrack(
      Level level, List<CoasterPathEdge> allEdges, Set<Integer> edgeIndices, BlockPos p
   ) {
      for (int i : edgeIndices) {
         if (i >= 0 && i < allEdges.size()) {
            CoasterPathEdge e = allEdges.get(i);
            if (e.bezier() == null && !e.from().equals(e.to()) && e.straightFromWorld() != null && e.straightToWorld() != null) {
               BlockPos other;
               if (e.from().equals(p)) {
                  other = e.to();
               } else {
                  if (!e.to().equals(p)) {
                     continue;
                  }

                  other = e.from();
               }

               if (!other.equals(p) && trackBezierConnectionCount(level, other) >= 1) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private static int uniqueHighlightedEdgeIndexIncidentOnDistinctEndpoints(List<CoasterPathEdge> edges, Set<Integer> edgeIndices, BlockPos p) {
      int found = -1;

      for (int i : edgeIndices) {
         if (i >= 0 && i < edges.size()) {
            CoasterPathEdge e = edges.get(i);
            if (!e.from().equals(e.to()) && (e.from().equals(p) || e.to().equals(p))) {
               if (found >= 0) {
                  return -1;
               }

               found = i;
            }
         }
      }

      return found;
   }

   public static record OpenEndRef(Vec3 position, int edgeIndex, boolean atFrom) {
   }
}
