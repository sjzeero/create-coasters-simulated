package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterCircularLoopPlacement {
   private static final double RADIUS_RELATIVE_TOL = 0.06;
   private static final double MIN_CIRCLE_RADIUS_BLOCKS = 1.25;
   private static final double PARALLEL_PLANE_NORMAL_DOT = 0.85;
   private static final double RADIAL_FACING_DOT = 0.45;
   private static final double MIN_ARC_RADIANS = Math.toRadians(35.0);
   private static final double MAX_ARC_RADIANS = Math.toRadians(145.0);
   private static final double COPLANAR_HEIGHT_TOL_BLOCKS = 0.35;

   private CoasterCircularLoopPlacement() {
   }

   @Nullable
   public static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildCircularLoop(
      Level level, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder
   ) {
      List<BlockPos> cycle = detectFourCycle(level, anchorA, anchorB);
      if (cycle == null) {
         return null;
      } else if (!allAnchorsHaveDefaultTilt(level, cycle)) {
         return null;
      } else {
         CoasterCircularLoopPlacement.CircleFit baseFit = fitCircle(level, cycle, false);
         if (baseFit == null) {
            return null;
         } else {
            CoasterCircularLoopPlacement.OrientationLayout orientation = classifyOrientation(level, cycle, baseFit);
            if (orientation == null) {
               return null;
            } else {
               CoasterCircularLoopPlacement.CircleFit fit;
               if (orientation.radial()) {
                  if (!allAnchorsHaveDefaultLift(level, cycle)) {
                     return null;
                  }

                  fit = fitCircle(level, cycle, true);
                  if (fit == null) {
                     return null;
                  }
               } else {
                  fit = baseFit;
               }

               List<BlockPos> ordered = orderAroundCircle(level, cycle, fit, orientation.radial());
               if (ordered.size() != 4) {
                  return null;
               } else {
                  List<BezierConnection> arcs = new ArrayList<>(4);

                  for (int i = 0; i < 4; i++) {
                     BlockPos from = ordered.get(i);
                     BlockPos to = ordered.get((i + 1) % 4);
                     BezierConnection arc = buildCircularArc(level, from, to, fit, orientation, material, girder);
                     if (arc == null || !CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(arc)) {
                        return null;
                     }

                     arcs.add(arc);
                  }

                  BezierConnection primary = null;
                  List<BezierConnection> neighbors = new ArrayList<>(3);

                  for (BezierConnection arc : arcs) {
                     BlockPos lo = (BlockPos)arc.bePositions.getFirst();
                     BlockPos hi = (BlockPos)arc.bePositions.getSecond();
                     boolean isClosing = lo.equals(anchorA) && hi.equals(anchorB) || lo.equals(anchorB) && hi.equals(anchorA);
                     if (isClosing) {
                        primary = arc;
                     } else {
                        neighbors.add(arc);
                     }
                  }

                  return primary != null && neighbors.size() == 3
                     ? new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(primary, List.of(), List.copyOf(neighbors))
                     : null;
               }
            }
         }
      }
   }

   private static boolean allAnchorsHaveDefaultTilt(Level level, List<BlockPos> cycle) {
      for (BlockPos pos : cycle) {
         if (!(level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity be)) {
            return false;
         }

         if (Math.abs(be.getJointBankRadians() - 0.0F) > 1.0E-4F) {
            return false;
         }
      }

      return true;
   }

   private static boolean allAnchorsHaveDefaultLift(Level level, List<BlockPos> cycle) {
      float def = 0.5F;

      for (BlockPos pos : cycle) {
         if (!(level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity be)) {
            return false;
         }

         if (Math.abs(be.getLiftBlocks() - def) > 0.001F) {
            return false;
         }
      }

      return true;
   }

   @Nullable
   public static List<BlockPos> detectFourCycle(Level level, BlockPos a, BlockPos b) {
      if (a.equals(b)) {
         return null;
      } else {
         if (!(level.getBlockEntity(a) instanceof CoasterAnchorpointBlockEntity beA) || !(level.getBlockEntity(b) instanceof CoasterAnchorpointBlockEntity beB)
            )
          {
            return null;
         }

         if (beA.legCount() != 1 || beB.legCount() != 1) {
            return null;
         } else if (!beA.getAnchorPeerCurvesView().containsKey(b) && !beB.getAnchorPeerCurvesView().containsKey(a)) {
            BlockPos p = solePeer(beA);
            BlockPos q = solePeer(beB);
            if (p == null || q == null || p.equals(q) || p.equals(b) || q.equals(a)) {
               return null;
            } else {
               if (!(level.getBlockEntity(p) instanceof CoasterAnchorpointBlockEntity beP)
                  || !(level.getBlockEntity(q) instanceof CoasterAnchorpointBlockEntity beQ)) {
                  return null;
               }

               if (beP.legCount() == 2 && beQ.legCount() == 2) {
                  BlockPos pOther = otherPeer(beP, a);
                  BlockPos qOther = otherPeer(beQ, b);
                  if (pOther == null || qOther == null) {
                     return null;
                  } else {
                     return pOther.equals(q) && qOther.equals(p) ? List.of(a.immutable(), p.immutable(), q.immutable(), b.immutable()) : null;
                  }
               } else {
                  return null;
               }
            }
         } else {
            return null;
         }
      }
   }

   @Nullable
   private static BlockPos solePeer(CoasterAnchorpointBlockEntity be) {
      return be.legCount() != 1 ? null : be.getAnchorPeerCurvesView().keySet().iterator().next().immutable();
   }

   @Nullable
   private static BlockPos otherPeer(CoasterAnchorpointBlockEntity be, BlockPos exclude) {
      BlockPos found = null;

      for (BlockPos peer : be.getAnchorPeerCurvesView().keySet()) {
         if (!peer.equals(exclude)) {
            if (found != null) {
               return null;
            }

            found = peer.immutable();
         }
      }

      return found;
   }

   @Nullable
   private static CoasterCircularLoopPlacement.CircleFit fitCircle(Level level, List<BlockPos> cycle, boolean useLiftedCenters) {
      Vec3[] pts = new Vec3[4];
      Vec3 sum = Vec3.ZERO;

      for (int i = 0; i < 4; i++) {
         pts[i] = anchorPointForFit(level, cycle.get(i), useLiftedCenters);
         sum = sum.add(pts[i]);
      }

      Vec3 centroid = sum.scale(0.25);
      Vec3 planeN = CoasterAnchorCoplanarPlane.planeNormalFromWorldCenters(pts[0], pts[1], pts[2]);
      if (planeN == null) {
         return null;
      } else {
         double maxHeight = 0.0;

         for (Vec3 p : pts) {
            maxHeight = Math.max(maxHeight, Math.abs(p.subtract(centroid).dot(planeN)));
         }

         if (maxHeight > 0.35) {
            return null;
         } else {
            Vec3 center = circumcenterInPlane(pts[0], pts[1], pts[2], planeN);
            if (center == null) {
               center = centroid;
            }

            center = center.subtract(planeN.scale(center.subtract(centroid).dot(planeN)));
            double r0 = pts[0].distanceTo(center);
            if (r0 < 1.25) {
               return null;
            } else {
               for (int i = 1; i < 4; i++) {
                  double ri = pts[i].distanceTo(center);
                  if (Math.abs(ri - r0) > 0.06 * r0) {
                     return null;
                  }
               }

               double radius = 0.0;

               for (Vec3 p : pts) {
                  radius += p.distanceTo(center);
               }

               radius *= 0.25;
               if (radius < 1.25) {
                  return null;
               } else {
                  Vec3 basisU = planeN.cross(Math.abs(planeN.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0));
                  if (basisU.lengthSqr() < 1.0E-12) {
                     return null;
                  } else {
                     basisU = basisU.normalize();
                     Vec3 basisV = planeN.cross(basisU).normalize();
                     return new CoasterCircularLoopPlacement.CircleFit(center, radius, planeN, basisU, basisV);
                  }
               }
            }
         }
      }
   }

   @Nullable
   private static Vec3 circumcenterInPlane(Vec3 a, Vec3 b, Vec3 c, Vec3 planeN) {
      Vec3 ab = b.subtract(a);
      Vec3 ac = c.subtract(a);
      Vec3 n = planeN.normalize();
      Vec3 abPerp = n.cross(ab);
      Vec3 acPerp = n.cross(ac);
      if (!(abPerp.lengthSqr() < 1.0E-12) && !(acPerp.lengthSqr() < 1.0E-12)) {
         abPerp = abPerp.normalize();
         acPerp = acPerp.normalize();
         Vec3 midAb = a.add(b).scale(0.5);
         Vec3 midAc = a.add(c).scale(0.5);
         Vec3 v = n.cross(abPerp).normalize();
         double dx = midAc.subtract(midAb).dot(abPerp);
         double dy = midAc.subtract(midAb).dot(v);
         double a12 = -acPerp.dot(abPerp);
         double a22 = -acPerp.dot(v);
         if (Math.abs(a22) < 1.0E-12) {
            return null;
         } else {
            double s = (dx * a22 - a12 * dy) / a22;
            return midAb.add(abPerp.scale(s));
         }
      } else {
         return null;
      }
   }

   @Nullable
   private static CoasterCircularLoopPlacement.OrientationLayout classifyOrientation(
      Level level, List<BlockPos> cycle, CoasterCircularLoopPlacement.CircleFit fit
   ) {
      int parallelSign = 0;
      int radialSign = 0;
      int parallelCount = 0;
      int radialCount = 0;

      for (BlockPos pos : cycle) {
         Vec3 up = CoasterAnchorpointBlockEntity.localUp(level, pos);
         double planeDot = up.dot(fit.planeNormal());
         if (Math.abs(planeDot) >= 0.85) {
            int s = planeDot >= 0.0 ? 1 : -1;
            if (parallelSign == 0) {
               parallelSign = s;
            } else if (parallelSign != s) {
               return null;
            }

            parallelCount++;
         } else {
            Vec3 towardCenter = inPlaneTowardCenter(level, pos, fit);
            if (towardCenter == null) {
               return null;
            }

            double radialDot = up.dot(towardCenter);
            if (Math.abs(radialDot) < 0.45) {
               return null;
            }

            int s = radialDot >= 0.0 ? 1 : -1;
            if (radialSign == 0) {
               radialSign = s;
            } else if (radialSign != s) {
               return null;
            }

            radialCount++;
         }
      }

      if (parallelCount == 4) {
         Vec3 faceUp = fit.planeNormal().normalize();
         if (parallelSign < 0) {
            faceUp = faceUp.scale(-1.0);
         }

         return new CoasterCircularLoopPlacement.OrientationLayout(false, false, faceUp);
      } else {
         return radialCount == 4 ? new CoasterCircularLoopPlacement.OrientationLayout(true, radialSign > 0, Vec3.ZERO) : null;
      }
   }

   @Nullable
   private static Vec3 inPlaneTowardCenter(Level level, BlockPos pos, CoasterCircularLoopPlacement.CircleFit fit) {
      Vec3 toward = fit.center().subtract(anchorPointForFit(level, pos, false));
      toward = toward.subtract(fit.planeNormal().scale(toward.dot(fit.planeNormal())));
      return toward.lengthSqr() < 1.0E-12 ? null : toward.normalize();
   }

   private static List<BlockPos> orderAroundCircle(Level level, List<BlockPos> cycle, CoasterCircularLoopPlacement.CircleFit fit, boolean useLiftedCenters) {
      record Angled(BlockPos pos, double angle) {
      }

      List<Angled> angled = new ArrayList<>(4);

      for (BlockPos pos : cycle) {
         angled.add(new Angled(pos, angleOnCircle(anchorPointForFit(level, pos, useLiftedCenters), fit)));
      }

      angled.sort(Comparator.comparingDouble(Angled::angle));
      List<BlockPos> ordered = new ArrayList<>(4);

      for (Angled a : angled) {
         ordered.add(a.pos());
      }

      return ordered;
   }

   private static Vec3 anchorPointForFit(Level level, BlockPos pos, boolean useLiftedCenters) {
      return useLiftedCenters ? CoasterAnchorpointBlockEntity.worldCenter(level, pos) : CoasterAnchorpointBlockEntity.worldCenterBase(level, pos);
   }

   @Nullable
   private static BezierConnection buildCircularArc(
      Level level,
      BlockPos from,
      BlockPos to,
      CoasterCircularLoopPlacement.CircleFit fit,
      CoasterCircularLoopPlacement.OrientationLayout orientation,
      TrackMaterial material,
      boolean girder
   ) {
      boolean useLifted = orientation.radial();
      double angFrom = angleOnCircle(anchorPointForFit(level, from, useLifted), fit);
      double angTo = angleOnCircle(anchorPointForFit(level, to, useLifted), fit);
      double alpha = angTo - angFrom;
      if (alpha <= 1.0E-6) {
         alpha += Math.PI * 2;
      }

      if (!(alpha < MIN_ARC_RADIANS) && !(alpha > MAX_ARC_RADIANS)) {
         Vec3 startFrom = pointOnCircle(fit, angFrom);
         Vec3 startTo = pointOnCircle(fit, angTo);
         Vec3 travelFrom = unitTangentCcW(fit, angFrom);
         Vec3 travelTo = unitTangentCcW(fit, angTo);
         if (!(travelFrom.lengthSqr() < 1.0E-12) && !(travelTo.lengthSqr() < 1.0E-12)) {
            Vec3 axisTo = travelTo.scale(-1.0);
            double handle = fit.radius() * 1.3333333333333333 * Math.tan(alpha * 0.25);
            handle = Math.max(1.0E-4, handle);
            Vec3 radialFrom = radialAtAngle(fit, angFrom);
            Vec3 radialTo = radialAtAngle(fit, angTo);
            Vec3 normalFrom;
            Vec3 normalTo;
            if (orientation.radial()) {
               Vec3 faceFrom = orientation.towardCenter() ? radialFrom.scale(-1.0) : radialFrom;
               Vec3 faceTo = orientation.towardCenter() ? radialTo.scale(-1.0) : radialTo;
               normalFrom = orthonormalToAxis(faceFrom, travelFrom);
               normalTo = orthonormalToAxis(faceTo, axisTo);
            } else {
               normalFrom = orthonormalToAxis(orientation.parallelFaceUp(), travelFrom);
               normalTo = orthonormalToAxis(orientation.parallelFaceUp(), axisTo);
            }

            long fl = from.asLong();
            long tl = to.asLong();
            BlockPos lo = fl <= tl ? from : to;
            BlockPos hi = fl <= tl ? to : from;
            Vec3 startLo = lo.equals(from) ? startFrom : startTo;
            Vec3 startHi = hi.equals(from) ? startFrom : startTo;
            Vec3 axisLo = lo.equals(from) ? travelFrom : axisTo;
            Vec3 axisHi = hi.equals(from) ? travelFrom : axisTo;
            Vec3 normalLo = lo.equals(from) ? normalFrom : normalTo;
            Vec3 normalHi = hi.equals(from) ? normalFrom : normalTo;
            BezierConnection bc = new BezierConnection(
               Couple.create(lo, hi), Couple.create(startLo, startHi), Couple.create(axisLo, axisHi), Couple.create(normalLo, normalHi), true, girder, material
            );
            CoasterPeerCurveHandleLengths.putCanonical(bc, handle, handle);
            return bc;
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static Vec3 pointOnCircle(CoasterCircularLoopPlacement.CircleFit fit, double angle) {
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      return fit.center().add(fit.basisU().scale(fit.radius() * c)).add(fit.basisV().scale(fit.radius() * s));
   }

   private static Vec3 radialAtAngle(CoasterCircularLoopPlacement.CircleFit fit, double angle) {
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      return fit.basisU().scale(c).add(fit.basisV().scale(s)).normalize();
   }

   private static Vec3 unitTangentCcW(CoasterCircularLoopPlacement.CircleFit fit, double angle) {
      double c = Math.cos(angle);
      double s = Math.sin(angle);
      Vec3 t = fit.basisU().scale(-s).add(fit.basisV().scale(c));
      return t.lengthSqr() < 1.0E-12 ? Vec3.ZERO : t.normalize();
   }

   private static double angleOnCircle(Vec3 worldPoint, CoasterCircularLoopPlacement.CircleFit fit) {
      Vec3 p = worldPoint.subtract(fit.center());
      return Math.atan2(p.dot(fit.basisV()), p.dot(fit.basisU()));
   }

   private static Vec3 orthonormalToAxis(Vec3 normal, Vec3 axis) {
      Vec3 a = axis.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : axis.normalize();
      Vec3 n = normal.subtract(a.scale(a.dot(normal)));
      if (n.lengthSqr() < 1.0E-12) {
         n = new Vec3(0.0, 1.0, 0.0).subtract(a.scale(a.dot(new Vec3(0.0, 1.0, 0.0))));
      }

      if (n.lengthSqr() < 1.0E-12) {
         n = a.cross(new Vec3(1.0, 0.0, 0.0));
      }

      if (n.lengthSqr() < 1.0E-12) {
         n = a.cross(new Vec3(0.0, 0.0, 1.0));
      }

      return n.normalize();
   }

   private static record CircleFit(Vec3 center, double radius, Vec3 planeNormal, Vec3 basisU, Vec3 basisV) {
   }

   private static record OrientationLayout(boolean radial, boolean towardCenter, Vec3 parallelFaceUp) {
   }
}
