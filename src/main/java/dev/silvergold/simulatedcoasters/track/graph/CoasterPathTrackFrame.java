package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Quaterniond;

public final class CoasterPathTrackFrame {
   public static final double DEFAULT_MAX_LINK_SQ = 9216.0;
   private static final Vec3 WORLD_UP = new Vec3(0.0, 1.0, 0.0);
   private static final int BEZIER_COARSE_STEPS = 64;
   private static final int BEZIER_TERNARY_ITERS = 48;
   private static final double WARM_START_MAX_BEARING_MOVE_BLOCKS = 1.25;
   private static final double WARM_START_MAX_BEARING_MOVE_SQ = 1.5625;
   private static final double WARM_START_T_HALF_WIDTH_BASE = 0.045;
   private static final double WARM_START_T_HALF_WIDTH_PER_BLOCK = 0.03;
   private static final int WARM_TERNARY_ITERS = 28;
   private static final int WARM_QUICK_COARSE_STEPS = 12;
   private static final double WARM_WINDOW_EDGE_DIST_EPS = 1.0E-5;
   private static final double WARM_QUICK_COARSE_IMPROVE_EPS = 1.0E-4;
   private static final double ENDPOINT_T_MARGIN = 0.06;

   private CoasterPathTrackFrame() {
   }

   @Nullable
   public static Quaterniond quaternionWorldFromTrackAt(Level level, CoasterPathGraph graph, Vec3 worldPos, double maxDistSq) {
      CoasterPathTrackFrame.GraphHit hit = nearestGraphHit(level, graph, worldPos, maxDistSq);
      if (hit == null) {
         return null;
      } else {
         CoasterPathTrackFrame.TrackBasis basis = trackBasisAtHit(level, hit);
         return quaternionFromTrackBasis(basis);
      }
   }

   public static Quaterniond quaternionFromTrackBasis(CoasterPathTrackFrame.TrackBasis b) {
      Vec3 r = b.right();
      Vec3 u = b.up();
      Vec3 f = b.forward();
      Matrix3d m = new Matrix3d();
      m.setColumn(0, r.x, r.y, r.z);
      m.setColumn(1, u.x, u.y, u.z);
      m.setColumn(2, -f.x, -f.y, -f.z);
      return new Quaterniond().setFromNormalized(m);
   }

   @Nullable
   public static CoasterPathTrackFrame.GraphHit nearestGraphHit(Level level, CoasterPathGraph graph, Vec3 from, double maxDistSq) {
      return nearestGraphHit(level, graph, from, maxDistSq, null);
   }

   @Nullable
   public static CoasterPathTrackFrame.GraphHit nearestGraphHit(
      Level level, CoasterPathGraph graph, Vec3 from, double maxDistSq, @Nullable Double partialTickForRender
   ) {
      List<CoasterPathEdge> edges = graph.edges();
      if (edges.isEmpty()) {
         return null;
      } else {
         List<double[]> bounds = graph.ensureEdgeCenterlineBounds(level);
         double margin = Math.sqrt(maxDistSq) + 0.001;
         CoasterPathTrackFrame.GraphHit best = null;
         double bestD = Double.POSITIVE_INFINITY;

         for (int i = 0; i < edges.size(); i++) {
            if (pointInsideInflatedAabb(bounds.get(i), from, margin)) {
               CoasterPathEdge edge = edges.get(i);
               CoasterPathTrackFrame.GraphHit cand = edge.bezier() != null
                  ? closestHitOnBezierEdge(level, edge, from, partialTickForRender, true)
                  : closestHitOnStraightEdge(level, edge, from, partialTickForRender);
               double d = cand.point().distanceToSqr(from);
               if (d < bestD) {
                  bestD = d;
                  best = cand;
               }
            }
         }

         return best != null && !(bestD > maxDistSq) ? best : null;
      }
   }

   public static CoasterPathTrackFrame.GraphHit closestHitOnEdge(Level level, CoasterPathEdge edge, Vec3 from) {
      return closestHitOnEdge(level, edge, from, null);
   }

   public static CoasterPathTrackFrame.GraphHit closestHitOnEdge(Level level, CoasterPathEdge edge, Vec3 from, @Nullable Double partialTickForRender) {
      return closestHitOnEdge(level, edge, from, partialTickForRender, true);
   }

   public static CoasterPathTrackFrame.GraphHit closestHitOnEdgeForEngagedSnap(Level level, CoasterPathEdge edge, Vec3 bearingWorld) {
      return closestHitOnEdgeForEngagedSnap(level, edge, bearingWorld, Double.NaN, null);
   }

   public static CoasterPathTrackFrame.GraphHit closestHitOnEdgeForEngagedSnap(
      Level level, CoasterPathEdge edge, Vec3 bearingWorld, double hintT, @Nullable Vec3 lastBearingWorld
   ) {
      if (CoasterPathGraphManager.edgeHostedOnSubLevel(level, edge)) {
         Vec3 bearingPlotLocal = CoasterPathGraphManager.bearingWorldInTrackHostPlotLocal(level, edge, bearingWorld);
         if (bearingPlotLocal != null) {
            return closestHitOnEngagedEdgeInTrackHostPlotLocal(level, edge, bearingPlotLocal, hintT, lastBearingWorld, bearingWorld);
         }
      }

      return closestHitOnEdge(level, edge, bearingWorld, null, false);
   }

   private static CoasterPathTrackFrame.GraphHit closestHitOnEngagedEdgeInTrackHostPlotLocal(
      Level level, CoasterPathEdge edge, Vec3 bearingPlotLocal, double hintT, @Nullable Vec3 lastBearingWorld, Vec3 bearingWorld
   ) {
      if (edge.bezier() != null) {
         if (CoasterPathGraphManager.isCoasterGraphBezier(edge.bezier())) {
            return closestHitOnCoasterBezierEdgePlotLocalEngaged(level, edge, bearingPlotLocal, hintT, lastBearingWorld, bearingWorld);
         } else {
            Vec3 bearingWorldFromPlot = CoasterPathGraphManager.trackHostPlotLocalToWorld(level, edge, bearingPlotLocal);
            return closestHitOnEdge(level, edge, bearingWorldFromPlot, null, false);
         }
      } else {
         return closestHitOnStraightEdgePlotLocalEngaged(level, edge, bearingPlotLocal);
      }
   }

   private static CoasterPathTrackFrame.GraphHit closestHitOnStraightEdgePlotLocalEngaged(Level level, CoasterPathEdge edge, Vec3 bearingPlotLocal) {
      Vec3 p0 = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, 0.0);
      Vec3 p1 = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, 1.0);
      Vec3 ab = p1.subtract(p0);
      double ab2 = ab.lengthSqr();
      if (ab2 < 1.0E-16) {
         Vec3 point = CoasterPathGraphManager.sampleEdgePosition(level, edge, 0.0);
         return new CoasterPathTrackFrame.GraphHit(point, edge, 0.0);
      } else {
         double t = bearingPlotLocal.subtract(p0).dot(ab) / ab2;
         t = Mth.clamp(t, 0.0, 1.0);
         Vec3 point = CoasterPathGraphManager.sampleEdgePosition(level, edge, t);
         return new CoasterPathTrackFrame.GraphHit(point, edge, t);
      }
   }

   private static CoasterPathTrackFrame.GraphHit closestHitOnCoasterBezierEdgePlotLocalEngaged(
      Level level, CoasterPathEdge edge, Vec3 bearingPlotLocal, double hintT, @Nullable Vec3 lastBearingWorld, Vec3 bearingWorld
   ) {
      CoasterBezierEdgeSamplingContext bezierContext = CoasterBezierEdgeSamplingContext.forCoasterEdge(level, edge);
      if (bezierContext == null) {
         Vec3 bearingWorldFromPlot = CoasterPathGraphManager.trackHostPlotLocalToWorld(level, edge, bearingPlotLocal);
         return closestHitOnEdge(level, edge, bearingWorldFromPlot, null, false);
      } else {
         Double warmT = tryWarmStartCoasterBezierPlotLocal(level, edge, bearingPlotLocal, bezierContext, hintT, lastBearingWorld, bearingWorld);
         if (warmT != null) {
            Vec3 point = CoasterPathGraphManager.sampleEdgePosition(level, edge, warmT);
            return new CoasterPathTrackFrame.GraphHit(point, edge, warmT);
         } else {
            return closestHitOnCoasterBezierEdgePlotLocalEngagedFull(level, edge, bearingPlotLocal, bezierContext);
         }
      }
   }

   @Nullable
   private static Double tryWarmStartCoasterBezierPlotLocal(
      Level level,
      CoasterPathEdge edge,
      Vec3 bearingPlotLocal,
      CoasterBezierEdgeSamplingContext bezierContext,
      double hintT,
      @Nullable Vec3 lastBearingWorld,
      Vec3 bearingWorld
   ) {
      if (!Double.isFinite(hintT)) {
         return null;
      } else {
         double tLo = bezierContext.tLo();
         double tHi = bezierContext.tHi();
         if (nearBezierSearchEndpoint(hintT, tLo, tHi)) {
            return null;
         } else if (lastBearingWorld != null && bearingWorld.distanceToSqr(lastBearingWorld) > 1.5625) {
            return null;
         } else {
            double bearingMove = lastBearingWorld != null ? Math.sqrt(bearingWorld.distanceToSqr(lastBearingWorld)) : 0.0;
            double halfWidth = 0.045 + bearingMove * 0.03;
            double lo = Math.max(tLo, hintT - halfWidth);
            double hi = Math.min(tHi, hintT + halfWidth);
            if (hi - lo < 1.0E-9) {
               return null;
            } else {
               double warmT = ternaryMinimizeDistancePlotLocal(level, edge, bearingPlotLocal, lo, hi, 28, bezierContext);
               double warmD = plotLocalBankedDistSq(level, edge, warmT, bearingPlotLocal, bezierContext);
               double dLo = plotLocalBankedDistSq(level, edge, lo, bearingPlotLocal, bezierContext);
               double dHi = plotLocalBankedDistSq(level, edge, hi, bearingPlotLocal, bezierContext);
               if (!(dLo + 1.0E-5 < warmD) && !(dHi + 1.0E-5 < warmD)) {
                  double quickBestT = warmT;
                  double quickBestD = warmD;

                  for (int i = 0; i <= 12; i++) {
                     double ti = Mth.lerp((double)i / 12.0, tLo, tHi);
                     double d = plotLocalBankedDistSq(level, edge, ti, bearingPlotLocal, bezierContext);
                     if (d < quickBestD) {
                        quickBestD = d;
                        quickBestT = ti;
                     }
                  }

                  if (quickBestD + 1.0E-4 < warmD) {
                     return null;
                  } else {
                     return !(quickBestT < lo - halfWidth * 0.25) && !(quickBestT > hi + halfWidth * 0.25) ? warmT : null;
                  }
               } else {
                  return null;
               }
            }
         }
      }
   }

   private static CoasterPathTrackFrame.GraphHit closestHitOnCoasterBezierEdgePlotLocalEngagedFull(
      Level level, CoasterPathEdge edge, Vec3 bearingPlotLocal, CoasterBezierEdgeSamplingContext bezierContext
   ) {
      double tLo = bezierContext.tLo();
      double tHi = bezierContext.tHi();
      int n = 64;
      double[] d = new double[65];

      for (int i = 0; i <= 64; i++) {
         double ti = Mth.lerp((double)i / 64.0, tLo, tHi);
         d[i] = plotLocalBankedDistSq(level, edge, ti, bearingPlotLocal, bezierContext);
      }

      double bestT = tLo;
      double bestD = d[0];

      for (int i = 1; i <= 64; i++) {
         if (d[i] < bestD) {
            bestD = d[i];
            bestT = Mth.lerp((double)i / 64.0, tLo, tHi);
         }
      }

      for (int ix = 0; ix <= 64; ix++) {
         boolean localMin = (ix == 0 || d[ix] <= d[ix - 1]) && (ix == 64 || d[ix] <= d[ix + 1]);
         if (localMin) {
            double lo = ix == 0 ? tLo : Mth.lerp((double)(ix - 1) / 64.0, tLo, tHi);
            double hi = ix == 64 ? tHi : Mth.lerp((double)(ix + 1) / 64.0, tLo, tHi);
            double tRef = ternaryMinimizeDistancePlotLocal(level, edge, bearingPlotLocal, lo, hi, 48, bezierContext);
            double dRef = plotLocalBankedDistSq(level, edge, tRef, bearingPlotLocal, bezierContext);
            if (dRef < bestD) {
               bestD = dRef;
               bestT = tRef;
            }
         }
      }

      Vec3 point = CoasterPathGraphManager.sampleEdgePosition(level, edge, bestT);
      return new CoasterPathTrackFrame.GraphHit(point, edge, bestT);
   }

   private static double plotLocalBankedDistSq(
      Level level, CoasterPathEdge edge, double t, Vec3 bearingPlotLocal, CoasterBezierEdgeSamplingContext bezierContext
   ) {
      return CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, t, bezierContext).distanceToSqr(bearingPlotLocal);
   }

   private static boolean nearBezierSearchEndpoint(double t, double tLo, double tHi) {
      double span = Math.max(tHi - tLo, 1.0E-9);
      double margin = Math.max(0.06, 0.08 * span);
      return t <= tLo + margin || t >= tHi - margin;
   }

   private static double ternaryMinimizeDistancePlotLocal(
      Level level, CoasterPathEdge edge, Vec3 fromPlotLocal, double lo, double hi, int iters, CoasterBezierEdgeSamplingContext bezierContext
   ) {
      for (int k = 0; k < iters && hi - lo > 1.0E-9; k++) {
         double m1 = lo + (hi - lo) / 3.0;
         double m2 = hi - (hi - lo) / 3.0;
         double d1 = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, m1, bezierContext).distanceToSqr(fromPlotLocal);
         double d2 = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, m2, bezierContext).distanceToSqr(fromPlotLocal);
         if (d1 < d2) {
            hi = m2;
         } else {
            lo = m1;
         }
      }

      return (lo + hi) * 0.5;
   }

   private static CoasterPathTrackFrame.GraphHit closestHitOnEdge(
      Level level, CoasterPathEdge edge, Vec3 from, @Nullable Double partialTickForRender, boolean spineOnlyForDistance
   ) {
      return edge.bezier() != null
         ? closestHitOnBezierEdge(level, edge, from, partialTickForRender, spineOnlyForDistance)
         : closestHitOnStraightEdge(level, edge, from, partialTickForRender);
   }

   @Nullable
   public static CoasterPathTrackFrame.GraphHit nearestGraphHit(Level level, List<CoasterPathEdge> edges, Vec3 from, double maxDistSq) {
      return nearestGraphHit(level, edges, from, maxDistSq, null);
   }

   @Nullable
   public static CoasterPathTrackFrame.GraphHit nearestGraphHit(
      Level level, List<CoasterPathEdge> edges, Vec3 from, double maxDistSq, @Nullable Double partialTickForRender
   ) {
      CoasterPathTrackFrame.GraphHit best = null;
      double bestD = Double.POSITIVE_INFINITY;

      for (CoasterPathEdge edge : edges) {
         CoasterPathTrackFrame.GraphHit cand = edge.bezier() != null
            ? closestHitOnBezierEdge(level, edge, from, partialTickForRender, true)
            : closestHitOnStraightEdge(level, edge, from, partialTickForRender);
         double d = cand.point().distanceToSqr(from);
         if (d < bestD) {
            bestD = d;
            best = cand;
         }
      }

      return best != null && !(bestD > maxDistSq) ? best : null;
   }

   private static boolean pointInsideInflatedAabb(double[] b, Vec3 p, double margin) {
      return p.x >= b[0] - margin && p.x <= b[3] + margin && p.y >= b[1] - margin && p.y <= b[4] + margin && p.z >= b[2] - margin && p.z <= b[5] + margin;
   }

   public static CoasterPathTrackFrame.TrackBasis trackBasisAtHit(Level level, CoasterPathTrackFrame.GraphHit hit) {
      return trackBasisAtHit(level, hit, null);
   }

   public static CoasterPathTrackFrame.TrackBasis trackBasisAtHit(Level level, CoasterPathTrackFrame.GraphHit hit, float partialTick) {
      return trackBasisAtHit(level, hit, (double)partialTick);
   }

   public static CoasterPathTrackFrame.TrackBasis trackBasisAtHit(Level level, CoasterPathTrackFrame.GraphHit hit, @Nullable Double partialTickForRender) {
      BlockPos host = CoasterPathGraphManager.edgeSubLevelAnchor(level, hit.edge());
      CoasterPathTrackFrame.TrackBasis plot = trackBasisPlotLocalAtHit(level, hit);
      return partialTickForRender == null && !CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, host)
         ? plot
         : transformTrackBasisToWorld(level, host, plot, partialTickForRender);
   }

   public static CoasterPathTrackFrame.TrackBasis trackBasisPlotLocalAtHit(Level level, CoasterPathTrackFrame.GraphHit hit) {
      CoasterPathEdge edge = hit.edge();
      double t = hit.t();
      Vec3 forwardPlotLocal = plotLocalTangentAt(level, edge, t);
      Vec3 forward = forwardPlotLocal.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : forwardPlotLocal.normalize();
      forward = forward.scale((double)edge.tangentSign());
      Vec3 upHint;
      if (edge.bezier() != null) {
         BezierConnection bc = edge.bezier();
         if (bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
            float faceT = (float)Mth.clamp(t, 0.0, 1.0);
            Vec3 geomTanPlot = forwardPlotLocal.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : forwardPlotLocal.normalize();
            upHint = CoasterBezierRailFrames.faceNormalAt(bc, faceT, geomTanPlot, level);
         } else {
            upHint = bc.getNormal(t);
         }

         if (upHint.lengthSqr() < 1.0E-14) {
            upHint = WORLD_UP;
         }
      } else {
         upHint = WORLD_UP;
      }

      Vec3 upCandidate = upHint.subtract(forward.scale(forward.dot(upHint)));
      if (upCandidate.lengthSqr() < 1.0E-12) {
         Vec3 alt = new Vec3(1.0, 0.0, 0.0);
         upCandidate = alt.subtract(forward.scale(forward.dot(alt)));
         if (upCandidate.lengthSqr() < 1.0E-12) {
            upCandidate = new Vec3(0.0, 0.0, 1.0).subtract(forward.scale(forward.dot(new Vec3(0.0, 0.0, 1.0))));
         }
      }

      upCandidate = upCandidate.normalize();
      Vec3 right = forward.cross(upCandidate);
      if (right.lengthSqr() < 1.0E-12) {
         right = forward.cross(new Vec3(1.0, 0.0, 0.0));
         if (right.lengthSqr() < 1.0E-12) {
            right = forward.cross(new Vec3(0.0, 0.0, 1.0));
         }
      }

      right = right.normalize();
      Vec3 up = right.cross(forward).normalize();
      return new CoasterPathTrackFrame.TrackBasis(forward, right, up);
   }

   private static CoasterPathTrackFrame.TrackBasis transformTrackBasisToWorld(
      Level level, BlockPos host, CoasterPathTrackFrame.TrackBasis plot, @Nullable Double partialTickForRender
   ) {
      Vec3 forward = CoasterPathGraphManager.sublevelWorldDirection(level, host, plot.forward(), partialTickForRender);
      Vec3 right = CoasterPathGraphManager.sublevelWorldDirection(level, host, plot.right(), partialTickForRender);
      Vec3 up = CoasterPathGraphManager.sublevelWorldDirection(level, host, plot.up(), partialTickForRender);
      return new CoasterPathTrackFrame.TrackBasis(forward, right, up);
   }

   public static Vec3 pathAnchorForSnappedBlock(Level level, CoasterPathTrackFrame.GraphHit hit) {
      return pathAnchorForSnappedBlock(level, hit, (Double)null);
   }

   public static Vec3 pathAnchorForSnappedBlock(Level level, CoasterPathTrackFrame.GraphHit hit, float partialTick) {
      return pathAnchorForSnappedBlock(level, hit, (double)partialTick);
   }

   private static Vec3 pathAnchorForSnappedBlock(Level level, CoasterPathTrackFrame.GraphHit hit, @Nullable Double partialTickForRender) {
      Vec3 p = partialTickForRender == null ? hit.point() : CoasterPathGraphManager.sampleEdgePosition(level, hit.edge(), hit.t(), partialTickForRender);
      CoasterPathTrackFrame.TrackBasis basis = trackBasisAtHit(level, hit, partialTickForRender);
      Vec3 up = basis.up();
      return up.lengthSqr() < 1.0E-12 ? p.add(0.0, 0.3125, 0.0) : p.add(up.normalize().scale(0.3125));
   }

   public static Vec3 pathAnchorForSnappedBlockAtSpine(Level level, CoasterPathTrackFrame.GraphHit basisHit, Vec3 spineWorld) {
      CoasterPathTrackFrame.TrackBasis basis = trackBasisAtHit(level, basisHit);
      Vec3 up = basis.up();
      return up.lengthSqr() < 1.0E-12 ? spineWorld.add(0.0, 0.3125, 0.0) : spineWorld.add(up.normalize().scale(0.3125));
   }

   private static CoasterPathTrackFrame.GraphHit closestHitOnStraightEdge(Level level, CoasterPathEdge edge, Vec3 from, @Nullable Double partialTickForRender) {
      Vec3 p0 = CoasterPathGraphManager.sampleEdgePosition(level, edge, 0.0, partialTickForRender);
      Vec3 p1 = CoasterPathGraphManager.sampleEdgePosition(level, edge, 1.0, partialTickForRender);
      Vec3 ab = p1.subtract(p0);
      double ab2 = ab.lengthSqr();
      if (ab2 < 1.0E-16) {
         return new CoasterPathTrackFrame.GraphHit(p0, edge, 0.0);
      } else {
         double t = from.subtract(p0).dot(ab) / ab2;
         t = Mth.clamp(t, 0.0, 1.0);
         Vec3 point = p0.add(ab.scale(t));
         return new CoasterPathTrackFrame.GraphHit(point, edge, t);
      }
   }

   private static CoasterPathTrackFrame.GraphHit closestHitOnBezierEdge(
      Level level, CoasterPathEdge edge, Vec3 from, @Nullable Double partialTickForRender, boolean spineOnlyForDistance
   ) {
      double[] tRange = CoasterPathGraphManager.isCoasterGraphBezier(edge.bezier())
         ? CoasterPathGraphManager.coasterBezierSampleTRange(level, edge)
         : new double[]{0.0, 1.0};
      double tLo = tRange[0];
      double tHi = tRange[1];
      int n = 64;
      double[] d = new double[65];

      for (int i = 0; i <= 64; i++) {
         double ti = Mth.lerp((double)i / 64.0, tLo, tHi);
         d[i] = sampleForDistance(level, edge, ti, partialTickForRender, spineOnlyForDistance).distanceToSqr(from);
      }

      double bestT = tLo;
      double bestD = d[0];

      for (int i = 1; i <= 64; i++) {
         if (d[i] < bestD) {
            bestD = d[i];
            bestT = Mth.lerp((double)i / 64.0, tLo, tHi);
         }
      }

      for (int ix = 0; ix <= 64; ix++) {
         boolean localMin = (ix == 0 || d[ix] <= d[ix - 1]) && (ix == 64 || d[ix] <= d[ix + 1]);
         if (localMin) {
            double lo = ix == 0 ? tLo : Mth.lerp((double)(ix - 1) / 64.0, tLo, tHi);
            double hi = ix == 64 ? tHi : Mth.lerp((double)(ix + 1) / 64.0, tLo, tHi);
            double tRef = ternaryMinimizeDistance(level, edge, from, lo, hi, 48, partialTickForRender, spineOnlyForDistance);
            double dRef = sampleForDistance(level, edge, tRef, partialTickForRender, spineOnlyForDistance).distanceToSqr(from);
            if (dRef < bestD) {
               bestD = dRef;
               bestT = tRef;
            }
         }
      }

      Vec3 point = CoasterPathGraphManager.sampleEdgePosition(level, edge, bestT, partialTickForRender);
      return new CoasterPathTrackFrame.GraphHit(point, edge, bestT);
   }

   private static Vec3 sampleForDistance(Level level, CoasterPathEdge edge, double t, @Nullable Double partialTickForRender, boolean spineOnlyForDistance) {
      return !spineOnlyForDistance
         ? CoasterPathGraphManager.sampleEdgePosition(level, edge, t, partialTickForRender)
         : edgePositionForNearestSearch(level, edge, t, partialTickForRender);
   }

   private static Vec3 straightForward(CoasterPathEdge edge) {
      if (edge.straightFromWorld() != null && edge.straightToWorld() != null) {
         Vec3 d = edge.straightToWorld().subtract(edge.straightFromWorld());
         return d.lengthSqr() < 1.0E-14 ? null : d;
      } else {
         return null;
      }
   }

   private static Vec3 plotLocalTangentAt(Level level, CoasterPathEdge edge, double t) {
      Vec3 straight = straightForward(edge);
      if (straight != null) {
         return straight;
      } else {
         BezierConnection bc = edge.bezier();
         if (bc == null) {
            return new Vec3(0.0, 0.0, 1.0);
         } else if (CoasterPathGraphManager.isCoasterGraphBezier(bc)) {
            return CoasterPathGraphManager.plotLocalCenterlineTangentAlongIncreasingT(level, edge, t);
         } else {
            double dt = 1.0E-4;
            double tm = t - dt;
            double tp = t + dt;
            if (tm < 0.0) {
               tm = 0.0;
               tp = Math.min(1.0, 2.0 * dt);
            } else if (tp > 1.0) {
               tp = 1.0;
               tm = Math.max(0.0, 1.0 - 2.0 * dt);
            }

            Vec3 pM = bc.getPosition(tm).add(CoasterPathGraphManager.PATH_CENTER_OFFSET);
            Vec3 pP = bc.getPosition(tp).add(CoasterPathGraphManager.PATH_CENTER_OFFSET);
            Vec3 d = pP.subtract(pM);
            return d.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : d;
         }
      }
   }

   private static Vec3 edgePositionForNearestSearch(Level level, CoasterPathEdge edge, double t, @Nullable Double partialTickForRender) {
      return CoasterPathGraphManager.edgeHostedOnSubLevel(level, edge)
         ? CoasterPathGraphManager.sampleEdgePosition(level, edge, t, partialTickForRender)
         : CoasterPathGraphManager.sampleEdgePositionForNearestQuery(level, edge, t, partialTickForRender);
   }

   private static double ternaryMinimizeDistance(
      Level level, CoasterPathEdge edge, Vec3 from, double lo, double hi, int iters, @Nullable Double partialTickForRender, boolean spineOnlyForDistance
   ) {
      for (int k = 0; k < iters && hi - lo > 1.0E-9; k++) {
         double m1 = lo + (hi - lo) / 3.0;
         double m2 = hi - (hi - lo) / 3.0;
         double d1 = sampleForDistance(level, edge, m1, partialTickForRender, spineOnlyForDistance).distanceToSqr(from);
         double d2 = sampleForDistance(level, edge, m2, partialTickForRender, spineOnlyForDistance).distanceToSqr(from);
         if (d1 < d2) {
            hi = m2;
         } else {
            lo = m1;
         }
      }

      return (lo + hi) * 0.5;
   }

   public static record GraphHit(Vec3 point, CoasterPathEdge edge, double t) {
   }

   public static record TrackBasis(Vec3 forward, Vec3 right, Vec3 up) {
   }
}
