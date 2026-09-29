package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterQuarterCirclePlacement {
   private static final double PERPENDICULAR_DOT_MAX = 0.08;
   private static final double ALIGN_DOT_MIN = 0.985;
   private static final double MIN_RADIUS_BLOCKS = 1.25;
   private static final double MIN_OPEN_STRAIGHT_BLOCKS = 0.5;
   private static final double PARALLEL_PLANE_NORMAL_DOT = 0.85;
   private static final double RADIAL_FACING_DOT = 0.45;
   private static final double KAPPA_QUARTER = 1.3333333333333333 * Math.tan(Math.PI / 8);
   private static final double STRAIGHT_AXIS_DOT_MIN = 0.985;
   private static final double CONTINUITY_OPPOSITE_DOT_MAX = -0.985;
   private static final double CLASS_FLIP_COST = 100.0;

   private CoasterQuarterCirclePlacement() {
   }

   @Nullable
   public static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildQuarterCircle(
      Level level, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder
   ) {
      CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult fromStraight = tryBuildOpenQuarterFromStraight(level, anchorA, anchorB, material, girder);
      if (fromStraight != null) {
         return fromStraight;
      } else {
         CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult fromQuarter = tryBuildOpenStraightFromQuarter(level, anchorA, anchorB, material, girder);
         if (fromQuarter != null) {
            return fromQuarter;
         } else {
            CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult closing = tryBuildClosingQuarter(level, anchorA, anchorB, material, girder);
            CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult completing = tryBuildStraightCompletingQuarter(level, anchorA, anchorB, material, girder);
            if (closing == null) {
               return completing;
            } else {
               return completing == null ? closing : preferLessDramaticRewrite(level, closing, completing);
            }
         }
      }
   }

   @Nullable
   private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildOpenQuarterFromStraight(
      Level level, BlockPos a, BlockPos b, TrackMaterial material, boolean girder
   ) {
      if (!(level.getBlockEntity(a) instanceof CoasterAnchorpointBlockEntity beA) || !(level.getBlockEntity(b) instanceof CoasterAnchorpointBlockEntity beB)) {
         return null;
      }

      if (!beA.getAnchorPeerCurvesView().containsKey(b) && !beB.getAnchorPeerCurvesView().containsKey(a)) {
         CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult ab = openQuarterFromStraightRoles(level, a, beA, b, beB, material, girder);
         return ab != null ? ab : openQuarterFromStraightRoles(level, b, beB, a, beA, material, girder);
      } else {
         return null;
      }
   }

   @Nullable
   private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult openQuarterFromStraightRoles(
      Level level,
      BlockPos corner,
      CoasterAnchorpointBlockEntity beCorner,
      BlockPos open,
      CoasterAnchorpointBlockEntity beOpen,
      TrackMaterial material,
      boolean girder
   ) {
      if (beCorner.legCount() == 1 && beOpen.legCount() == 0) {
         BlockPos outer = solePeer(beCorner);
         if (outer != null && !outer.equals(open)) {
            BezierConnection approach = existingPrimaryBetween(level, outer, corner);
            if (approach != null && looksStraight(approach)) {
               CoasterQuarterCirclePlacement.QuarterSolve solve = solveQuarter(level, outer, corner, open, null);
               if (solve == null) {
                  return null;
               } else {
                  BezierConnection quarter = buildQuarterCurve(corner, open, solve, material, girder);
                  if (quarter != null && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(quarter)) {
                     Vec3 intoApproach = CoasterBezierHandleEdit.tangentIntoFromAnchor(approach, corner);
                     Vec3 intoQuarter = CoasterBezierHandleEdit.tangentIntoFromAnchor(quarter, corner);
                     if (intoApproach != null && intoQuarter != null) {
                        return intoApproach.normalize().dot(intoQuarter.normalize()) > -0.985
                           ? null
                           : new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(quarter, List.of(), List.of());
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               }
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildOpenStraightFromQuarter(
      Level level, BlockPos a, BlockPos b, TrackMaterial material, boolean girder
   ) {
      if (!(level.getBlockEntity(a) instanceof CoasterAnchorpointBlockEntity beA) || !(level.getBlockEntity(b) instanceof CoasterAnchorpointBlockEntity beB)) {
         return null;
      }

      if (!beA.getAnchorPeerCurvesView().containsKey(b) && !beB.getAnchorPeerCurvesView().containsKey(a)) {
         CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult ab = openStraightFromQuarterRoles(level, a, beA, b, beB, material, girder);
         return ab != null ? ab : openStraightFromQuarterRoles(level, b, beB, a, beA, material, girder);
      } else {
         return null;
      }
   }

   @Nullable
   private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult openStraightFromQuarterRoles(
      Level level,
      BlockPos corner,
      CoasterAnchorpointBlockEntity beCorner,
      BlockPos open,
      CoasterAnchorpointBlockEntity beOpen,
      TrackMaterial material,
      boolean girder
   ) {
      if (beCorner.legCount() == 1 && beOpen.legCount() == 0) {
         BlockPos otherCorner = solePeer(beCorner);
         if (otherCorner == null || otherCorner.equals(open)) {
            return null;
         } else if (!allAnchorsHaveDefaultTilt(level, List.of(corner, open))) {
            return null;
         } else {
            BezierConnection quarter = existingPrimaryBetween(level, otherCorner, corner);
            if (quarter != null && looksQuarter(quarter)) {
               Vec3 intoQuarter = CoasterBezierHandleEdit.tangentIntoFromAnchor(quarter, corner);
               if (intoQuarter == null) {
                  return null;
               } else {
                  intoQuarter = intoQuarter.normalize();
                  Vec3 travel = intoQuarter.scale(-1.0);
                  Vec3 startCorner = ((BlockPos)quarter.bePositions.getFirst()).equals(corner)
                     ? (Vec3)quarter.starts.getFirst()
                     : (Vec3)quarter.starts.getSecond();
                  Vec3 startOpen = CoasterAnchorpointBlockEntity.worldCenter(level, open);
                  Vec3 delta = startOpen.subtract(startCorner);
                  double len = delta.length();
                  if (len < 0.5) {
                     return null;
                  } else if (delta.scale(1.0 / len).dot(travel) < 0.985) {
                     return null;
                  } else {
                     Vec3 axisOpen = travel.scale(-1.0);
                     Vec3 nCornerStored = ((BlockPos)quarter.bePositions.getFirst()).equals(corner)
                        ? (Vec3)quarter.normals.getFirst()
                        : (Vec3)quarter.normals.getSecond();
                     Vec3 normalCorner = CoasterTrackPlacement.orthonormalFaceNormalForAxis(level, corner, nCornerStored, travel);
                     Vec3 normalOpen = CoasterTrackPlacement.anchorFaceNormalForAxis(level, open, axisOpen);
                     BezierConnection straight = primaryCurve(
                        corner, open, startCorner, startOpen, travel, axisOpen, normalCorner, normalOpen, material, girder
                     );
                     double h = Math.max(1.0E-4, len / 3.0);
                     CoasterPeerCurveHandleLengths.putCanonical(straight, h, h);
                     if (CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straight) && looksStraight(straight)) {
                        Vec3 intoStraight = CoasterBezierHandleEdit.tangentIntoFromAnchor(straight, corner);
                        return intoStraight != null && !(intoQuarter.dot(intoStraight.normalize()) > -0.985)
                           ? new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(straight, List.of(), List.of())
                           : null;
                     } else {
                        return null;
                     }
                  }
               }
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult preferLessDramaticRewrite(
      Level level, CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult a, CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult b
   ) {
      double da = rewriteDrama(level, a);
      double db = rewriteDrama(level, b);
      return da <= db ? a : b;
   }

   private static double rewriteDrama(Level level, CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult result) {
      List<BezierConnection> neighbors = result.circularLoopNeighbors();
      if (neighbors != null && !neighbors.isEmpty()) {
         double drama = 0.0;

         for (BezierConnection after : neighbors) {
            BezierConnection before = existingPrimaryBetween(level, (BlockPos)after.bePositions.getFirst(), (BlockPos)after.bePositions.getSecond());
            if (before != null) {
               drama += edgeRewriteDrama(before, after);
            }
         }

         return drama;
      } else {
         return 0.0;
      }
   }

   private static double edgeRewriteDrama(BezierConnection before, BezierConnection after) {
      boolean wasStraight = looksStraight(before);
      boolean nowStraight = looksStraight(after);
      double drama = wasStraight != nowStraight ? 100.0 : 0.0;
      Vec3 before0 = (Vec3)before.starts.getFirst();
      Vec3 before1 = (Vec3)before.starts.getSecond();
      Vec3 after0 = (Vec3)after.starts.getFirst();
      Vec3 after1 = (Vec3)after.starts.getSecond();
      boolean sameOrder = ((BlockPos)before.bePositions.getFirst()).equals(after.bePositions.getFirst());
      if (!sameOrder) {
         after0 = (Vec3)after.starts.getSecond();
         after1 = (Vec3)after.starts.getFirst();
      }

      drama += before0.distanceTo(after0) + before1.distanceTo(after1);
      Vec3 ax0b = (Vec3)before.axes.getFirst();
      Vec3 ax1b = (Vec3)before.axes.getSecond();
      Vec3 ax0a = sameOrder ? (Vec3)after.axes.getFirst() : (Vec3)after.axes.getSecond();
      Vec3 ax1a = sameOrder ? (Vec3)after.axes.getSecond() : (Vec3)after.axes.getFirst();
      drama += axisDelta(ax0b, ax0a) + axisDelta(ax1b, ax1a);
      Couple<Double> hb = CoasterPeerCurveHandleLengths.getCanonicalLengths(before);
      Couple<Double> ha = CoasterPeerCurveHandleLengths.getCanonicalLengths(after);
      if (hb != null && ha != null) {
         double chord = Math.max(0.001, before0.distanceTo(before1));
         drama += Math.abs((Double)ha.getFirst() - (Double)hb.getFirst()) / chord + Math.abs((Double)ha.getSecond() - (Double)hb.getSecond()) / chord;
      }

      return drama;
   }

   private static double axisDelta(Vec3 a, Vec3 b) {
      return !(a.lengthSqr() < 1.0E-12) && !(b.lengthSqr() < 1.0E-12) ? 1.0 - Math.abs(a.normalize().dot(b.normalize())) : 1.0;
   }

   private static boolean looksStraight(BezierConnection bc) {
      Vec3 chord = ((Vec3)bc.starts.getSecond()).subtract((Vec3)bc.starts.getFirst());
      if (chord.lengthSqr() < 1.0E-12) {
         return true;
      } else {
         chord = chord.normalize();
         Vec3 a0 = (Vec3)bc.axes.getFirst();
         Vec3 a1 = (Vec3)bc.axes.getSecond();
         if (!(a0.lengthSqr() < 1.0E-12) && !(a1.lengthSqr() < 1.0E-12)) {
            a0 = a0.normalize();
            a1 = a1.normalize();
            return a0.dot(chord) >= 0.985 && a1.dot(chord.scale(-1.0)) >= 0.985;
         } else {
            return false;
         }
      }
   }

   private static boolean looksQuarter(BezierConnection bc) {
      if (looksStraight(bc)) {
         return false;
      } else {
         Vec3 a0 = (Vec3)bc.axes.getFirst();
         Vec3 a1 = (Vec3)bc.axes.getSecond();
         if (!(a0.lengthSqr() < 1.0E-12) && !(a1.lengthSqr() < 1.0E-12)) {
            a0 = a0.normalize();
            a1 = a1.normalize();
            if (Math.abs(a0.dot(a1)) > 0.08) {
               return false;
            } else {
               Vec3 chord = ((Vec3)bc.starts.getSecond()).subtract((Vec3)bc.starts.getFirst());
               double dist = chord.length();
               if (dist < 1.25 * Math.sqrt(2.0) * 0.5) {
                  return false;
               } else {
                  double radius = dist / Math.sqrt(2.0);
                  if (radius < 1.25) {
                     return false;
                  } else {
                     chord = chord.normalize();
                     Vec3 sum = a0.subtract(a1);
                     if (sum.lengthSqr() < 1.0E-8) {
                        return false;
                     } else if (chord.dot(sum.normalize()) < 0.985) {
                        return false;
                     } else {
                        Couple<Double> handles = CoasterPeerCurveHandleLengths.getCanonicalLengths(bc);
                        if (handles != null) {
                           double expected = radius * KAPPA_QUARTER;
                           double tol = Math.max(0.2, expected * 0.25);
                           if (Math.abs((Double)handles.getFirst() - expected) > tol || Math.abs((Double)handles.getSecond() - expected) > tol) {
                              return false;
                           }
                        }

                        return true;
                     }
                  }
               }
            }
         } else {
            return false;
         }
      }
   }

   @Nullable
   private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildClosingQuarter(
      Level level, BlockPos cornerA, BlockPos cornerB, TrackMaterial material, boolean girder
   ) {
      if (!(level.getBlockEntity(cornerA) instanceof CoasterAnchorpointBlockEntity beA)
         || !(level.getBlockEntity(cornerB) instanceof CoasterAnchorpointBlockEntity beB)) {
         return null;
      }

      if (beA.legCount() != 1 || beB.legCount() != 1) {
         return null;
      } else if (!beA.getAnchorPeerCurvesView().containsKey(cornerB) && !beB.getAnchorPeerCurvesView().containsKey(cornerA)) {
         BlockPos outerA = solePeer(beA);
         BlockPos outerB = solePeer(beB);
         if (outerA != null && outerB != null && !outerA.equals(outerB) && !outerA.equals(cornerB) && !outerB.equals(cornerA)) {
            CoasterQuarterCirclePlacement.QuarterSolve solve = solveQuarter(level, outerA, cornerA, cornerB, outerB);
            if (solve == null) {
               return null;
            } else {
               BezierConnection quarter = buildQuarterCurve(cornerA, cornerB, solve, material, girder);
               BezierConnection straightA = buildStraightLeg(level, outerA, cornerA, material, girder, solve);
               BezierConnection straightB = buildStraightLeg(level, outerB, cornerB, material, girder, solve);
               if (quarter == null || straightA == null || straightB == null) {
                  return null;
               } else if (CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(quarter)
                  && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightA)
                  && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightB)) {
                  List<BezierConnection> neighbors = new ArrayList<>(4);
                  neighbors.add(straightA);
                  neighbors.add(straightB);
                  if (!appendOuterContinuityAdjustments(level, neighbors, outerA, cornerA, straightA)) {
                     return null;
                  } else {
                     return !appendOuterContinuityAdjustments(level, neighbors, outerB, cornerB, straightB)
                        ? null
                        : new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(quarter, List.of(), List.copyOf(neighbors));
                  }
               } else {
                  return null;
               }
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildStraightCompletingQuarter(
      Level level, BlockPos a, BlockPos b, TrackMaterial material, boolean girder
   ) {
      if (!(level.getBlockEntity(a) instanceof CoasterAnchorpointBlockEntity beA) || !(level.getBlockEntity(b) instanceof CoasterAnchorpointBlockEntity beB)) {
         return null;
      }

      if (!beA.getAnchorPeerCurvesView().containsKey(b) && !beB.getAnchorPeerCurvesView().containsKey(a)) {
         CoasterQuarterCirclePlacement.CornerStraightRoles roles = resolveStraightOntoCornerRoles(beA, a, beB, b);
         if (roles == null) {
            return null;
         } else {
            BlockPos otherOuter = otherApproachPeer(level, roles.otherCorner(), roles.corner());
            if (otherOuter == null) {
               return null;
            } else {
               CoasterQuarterCirclePlacement.QuarterSolve solve = solveQuarter(level, roles.outer(), roles.corner(), roles.otherCorner(), otherOuter);
               if (solve == null) {
                  return null;
               } else {
                  BezierConnection straightNew = buildStraightLeg(level, roles.outer(), roles.corner(), material, girder, solve);
                  BezierConnection quarter = buildQuarterCurve(roles.corner(), roles.otherCorner(), solve, material, girder);
                  if (straightNew == null || quarter == null) {
                     return null;
                  } else if (CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightNew)
                     && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(quarter)) {
                     BezierConnection straightOther = buildStraightLeg(level, otherOuter, roles.otherCorner(), material, girder, solve);
                     if (straightOther != null && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightOther)) {
                        List<BezierConnection> neighbors = new ArrayList<>(4);
                        neighbors.add(quarter);
                        neighbors.add(straightOther);
                        if (!appendOuterContinuityAdjustments(level, neighbors, roles.outer(), roles.corner(), straightNew)) {
                           return null;
                        } else {
                           return !appendOuterContinuityAdjustments(level, neighbors, otherOuter, roles.otherCorner(), straightOther)
                              ? null
                              : new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(straightNew, List.of(), List.copyOf(neighbors));
                        }
                     } else {
                        return null;
                     }
                  } else {
                     return null;
                  }
               }
            }
         }
      } else {
         return null;
      }
   }

   private static boolean appendOuterContinuityAdjustments(
      Level level, List<BezierConnection> neighbors, BlockPos outer, BlockPos corner, BezierConnection straightenedApproach
   ) {
      if (level.getBlockEntity(outer) instanceof CoasterAnchorpointBlockEntity be) {
         Vec3 intoApproach = CoasterBezierHandleEdit.tangentIntoFromAnchor(straightenedApproach, outer);
         if (intoApproach == null) {
            return false;
         } else {
            Vec3 intoBeyond = intoApproach.scale(-1.0);

            for (BlockPos peer : be.getAnchorPeerCurvesView().keySet()) {
               if (!peer.equals(corner)) {
                  BezierConnection existing = existingPrimaryBetween(level, outer, peer);
                  if (existing == null) {
                     return false;
                  }

                  BezierConnection adjusted = CoasterTrackPlacement.curveWithAxisAtEndpointPreservingPeerForJoin(level, existing, outer, intoBeyond);
                  CoasterPeerCurveHandleLengths.carryLengthsToReplacement(existing, adjusted);
                  if (!isAdjustedOuterCurveValid(adjusted)) {
                     return false;
                  }

                  neighbors.add(adjusted);
               }
            }

            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean isAdjustedOuterCurveValid(BezierConnection bc) {
      return CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(bc);
   }

   @Nullable
   private static BezierConnection existingPrimaryBetween(Level level, BlockPos a, BlockPos b) {
      if (level.getBlockEntity(a) instanceof CoasterAnchorpointBlockEntity beA) {
         BezierConnection c = beA.getAnchorPeerCurvesView().get(b);
         if (c != null) {
            return c.isPrimary() ? c : c.secondary();
         }
      }

      if (level.getBlockEntity(b) instanceof CoasterAnchorpointBlockEntity beB) {
         BezierConnection c = beB.getAnchorPeerCurvesView().get(a);
         if (c != null) {
            return c.isPrimary() ? c : c.secondary();
         }
      }

      return null;
   }

   @Nullable
   private static CoasterQuarterCirclePlacement.CornerStraightRoles resolveStraightOntoCornerRoles(
      CoasterAnchorpointBlockEntity beA, BlockPos a, CoasterAnchorpointBlockEntity beB, BlockPos b
   ) {
      CoasterQuarterCirclePlacement.CornerStraightRoles ab = rolesIfStraightOntoCorner(beA, a, beB, b);
      return ab != null ? ab : rolesIfStraightOntoCorner(beB, b, beA, a);
   }

   @Nullable
   private static CoasterQuarterCirclePlacement.CornerStraightRoles rolesIfStraightOntoCorner(
      CoasterAnchorpointBlockEntity beOuter, BlockPos outer, CoasterAnchorpointBlockEntity beCorner, BlockPos corner
   ) {
      if (beCorner.legCount() != 1) {
         return null;
      } else if (beOuter.legCount() > 1) {
         return null;
      } else {
         if (beOuter.legCount() == 1) {
            BlockPos outerPeer = solePeer(beOuter);
            if (outerPeer == null || outerPeer.equals(corner)) {
               return null;
            }
         }

         BlockPos otherCorner = solePeer(beCorner);
         return otherCorner != null && !otherCorner.equals(outer)
            ? new CoasterQuarterCirclePlacement.CornerStraightRoles(outer.immutable(), corner.immutable(), otherCorner.immutable())
            : null;
      }
   }

   @Nullable
   private static BlockPos otherApproachPeer(Level level, BlockPos otherCorner, BlockPos corner) {
      if (level.getBlockEntity(otherCorner) instanceof CoasterAnchorpointBlockEntity be) {
         BlockPos var7 = null;

         for (BlockPos peer : be.getAnchorPeerCurvesView().keySet()) {
            if (!peer.equals(corner)) {
               if (var7 != null) {
                  return null;
               }

               var7 = peer.immutable();
            }
         }

         return var7;
      } else {
         return null;
      }
   }

   @Nullable
   private static CoasterQuarterCirclePlacement.QuarterSolve solveQuarter(
      Level level, BlockPos outerA, BlockPos cornerA, BlockPos cornerB, @Nullable BlockPos otherOuter
   ) {
      List<BlockPos> involved = new ArrayList<>(4);
      involved.add(outerA);
      involved.add(cornerA);
      involved.add(cornerB);
      if (otherOuter != null) {
         involved.add(otherOuter);
      }

      if (!allAnchorsHaveDefaultTilt(level, involved)) {
         return null;
      } else {
         Vec3 cA = CoasterAnchorpointBlockEntity.worldCenter(level, cornerA);
         Vec3 cB = CoasterAnchorpointBlockEntity.worldCenter(level, cornerB);
         Vec3 cP = CoasterAnchorpointBlockEntity.worldCenter(level, outerA);
         Vec3 travelA = cA.subtract(cP);
         if (travelA.lengthSqr() < 1.0E-8) {
            return null;
         } else {
            travelA = travelA.normalize();
            Vec3 travelB;
            if (otherOuter != null) {
               Vec3 cQ = CoasterAnchorpointBlockEntity.worldCenter(level, otherOuter);
               travelB = cQ.subtract(cB);
               if (travelB.lengthSqr() < 1.0E-8) {
                  return null;
               }

               travelB = travelB.normalize();
            } else {
               Vec3 delta = cB.subtract(cA);
               double dist = delta.length();
               if (dist < 1.25 * Math.sqrt(2.0) * 0.5) {
                  return null;
               }

               double r = dist / Math.sqrt(2.0);
               travelB = delta.scale(1.0 / r).subtract(travelA);
               if (travelB.lengthSqr() < 1.0E-8) {
                  return null;
               }

               travelB = travelB.normalize();
            }

            if (Math.abs(travelA.dot(travelB)) > 0.08) {
               return null;
            } else {
               Vec3 deltax = cB.subtract(cA);
               double distx = deltax.length();
               if (distx < 1.25 * Math.sqrt(2.0) * 0.5) {
                  return null;
               } else {
                  Vec3 sum = travelA.add(travelB);
                  if (sum.lengthSqr() < 1.0E-8) {
                     return null;
                  } else {
                     sum = sum.normalize();
                     if (deltax.normalize().dot(sum) < 0.985) {
                        return null;
                     } else {
                        double radius = distx / Math.sqrt(2.0);
                        if (radius < 1.25) {
                           return null;
                        } else {
                           Vec3 expectedB = cA.add(travelA.add(travelB).scale(radius));
                           if (expectedB.distanceToSqr(cB) > radius * 0.06 * radius * 0.06) {
                              return null;
                           } else {
                              Vec3 planeN = travelA.cross(travelB);
                              if (planeN.lengthSqr() < 1.0E-12) {
                                 return null;
                              } else {
                                 planeN = planeN.normalize();
                                 Vec3 cornerCenter = cA.add(travelB.scale(radius));
                                 CoasterQuarterCirclePlacement.Orientation orientation = classifyOrientation(level, involved, planeN, cornerCenter);
                                 if (orientation == null) {
                                    return null;
                                 } else if (orientation.radial() && !allAnchorsHaveDefaultLift(level, involved)) {
                                    return null;
                                 } else if (orientation.radial()) {
                                    return new CoasterQuarterCirclePlacement.QuarterSolve(
                                       orientation, radius, planeN, cornerCenter, cA, cB, travelA, travelB, true
                                    );
                                 } else {
                                    Vec3 baseA = CoasterAnchorpointBlockEntity.worldCenterBase(level, cornerA);
                                    Vec3 baseB = CoasterAnchorpointBlockEntity.worldCenterBase(level, cornerB);
                                    Vec3 baseP = CoasterAnchorpointBlockEntity.worldCenterBase(level, outerA);
                                    Vec3 tA = baseA.subtract(baseP);
                                    if (tA.lengthSqr() < 1.0E-8) {
                                       return null;
                                    } else {
                                       tA = tA.normalize();
                                       if (tA.dot(travelA) < 0.0) {
                                          tA = tA.scale(-1.0);
                                       }

                                       Vec3 tB;
                                       if (otherOuter != null) {
                                          Vec3 baseQ = CoasterAnchorpointBlockEntity.worldCenterBase(level, otherOuter);
                                          tB = baseQ.subtract(baseB);
                                          if (tB.lengthSqr() < 1.0E-8) {
                                             return null;
                                          }

                                          tB = tB.normalize();
                                          if (tB.dot(travelB) < 0.0) {
                                             tB = tB.scale(-1.0);
                                          }
                                       } else {
                                          Vec3 d = baseB.subtract(baseA);
                                          double br = d.length() / Math.sqrt(2.0);
                                          if (br < 1.25) {
                                             return null;
                                          }

                                          tB = d.scale(1.0 / br).subtract(tA);
                                          if (tB.lengthSqr() < 1.0E-8) {
                                             return null;
                                          }

                                          tB = tB.normalize();
                                       }

                                       if (Math.abs(tA.dot(tB)) > 0.08) {
                                          return null;
                                       } else {
                                          double baseDist = baseB.distanceTo(baseA);
                                          double baseR = baseDist / Math.sqrt(2.0);
                                          if (baseR < 1.25) {
                                             return null;
                                          } else {
                                             Vec3 baseSum = tA.add(tB);
                                             if (baseSum.lengthSqr() < 1.0E-8) {
                                                return null;
                                             } else if (baseB.subtract(baseA).normalize().dot(baseSum.normalize()) < 0.985) {
                                                return null;
                                             } else {
                                                Vec3 baseN = tA.cross(tB);
                                                if (baseN.lengthSqr() < 1.0E-12) {
                                                   return null;
                                                } else {
                                                   baseN = baseN.normalize();
                                                   if (baseN.dot(orientation.planeNormal()) < 0.0) {
                                                      baseN = baseN.scale(-1.0);
                                                   }

                                                   Vec3 baseCenter = baseA.add(tB.scale(baseR));
                                                   return new CoasterQuarterCirclePlacement.QuarterSolve(
                                                      orientation, baseR, baseN, baseCenter, baseA, baseB, tA, tB, false
                                                   );
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Nullable
   private static BezierConnection buildQuarterCurve(
      BlockPos cornerA, BlockPos cornerB, CoasterQuarterCirclePlacement.QuarterSolve solve, TrackMaterial material, boolean girder
   ) {
      Vec3 axisA = solve.travelA();
      Vec3 axisB = solve.travelB().scale(-1.0);
      Vec3 normalA = faceNormal(solve.orientation(), solve.startCornerA(), solve.cornerCenter(), axisA, solve.planeN());
      Vec3 normalB = faceNormal(solve.orientation(), solve.startCornerB(), solve.cornerCenter(), axisB, solve.planeN());
      BezierConnection quarter = primaryCurve(cornerA, cornerB, solve.startCornerA(), solve.startCornerB(), axisA, axisB, normalA, normalB, material, girder);
      double handle = Math.max(1.0E-4, solve.radius() * KAPPA_QUARTER);
      CoasterPeerCurveHandleLengths.putCanonical(quarter, handle, handle);
      return quarter;
   }

   @Nullable
   private static CoasterQuarterCirclePlacement.Orientation classifyOrientation(Level level, List<BlockPos> anchors, Vec3 planeN, Vec3 cornerCenter) {
      int parallelSign = 0;
      int radialSign = 0;
      int parallelCount = 0;
      int radialCount = 0;

      for (BlockPos pos : anchors) {
         Vec3 up = CoasterAnchorpointBlockEntity.localUp(level, pos);
         double planeDot = up.dot(planeN);
         if (Math.abs(planeDot) >= 0.85) {
            int s = planeDot >= 0.0 ? 1 : -1;
            if (parallelSign == 0) {
               parallelSign = s;
            } else if (parallelSign != s) {
               return null;
            }

            parallelCount++;
         } else {
            Vec3 toward = cornerCenter.subtract(CoasterAnchorpointBlockEntity.worldCenterBase(level, pos));
            toward = toward.subtract(planeN.scale(toward.dot(planeN)));
            if (toward.lengthSqr() < 1.0E-12) {
               return null;
            }

            toward = toward.normalize();
            double radialDot = up.dot(toward);
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

      if (parallelCount == anchors.size()) {
         Vec3 faceUp = planeN.normalize();
         if (parallelSign < 0) {
            faceUp = faceUp.scale(-1.0);
         }

         return new CoasterQuarterCirclePlacement.Orientation(false, false, planeN, faceUp);
      } else {
         return radialCount == anchors.size() ? new CoasterQuarterCirclePlacement.Orientation(true, radialSign > 0, planeN, Vec3.ZERO) : null;
      }
   }

   private static Vec3 faceNormal(CoasterQuarterCirclePlacement.Orientation orientation, Vec3 start, Vec3 cornerCenter, Vec3 axis, Vec3 planeN) {
      if (orientation.radial()) {
         Vec3 toward = cornerCenter.subtract(start);
         toward = toward.subtract(planeN.scale(toward.dot(planeN)));
         if (toward.lengthSqr() < 1.0E-12) {
            toward = orientation.towardCenter() ? planeN : planeN.scale(-1.0);
         } else {
            toward = toward.normalize();
         }

         Vec3 face = orientation.towardCenter() ? toward : toward.scale(-1.0);
         return orthonormalToAxis(face, axis);
      } else {
         return orthonormalToAxis(orientation.parallelFaceUp(), axis);
      }
   }

   @Nullable
   private static BezierConnection buildStraightLeg(
      Level level, BlockPos a, BlockPos b, TrackMaterial material, boolean girder, CoasterQuarterCirclePlacement.QuarterSolve solve
   ) {
      Vec3 startA = solve.useLiftedCenters() ? CoasterAnchorpointBlockEntity.worldCenter(level, a) : CoasterAnchorpointBlockEntity.worldCenterBase(level, a);
      Vec3 startB = solve.useLiftedCenters() ? CoasterAnchorpointBlockEntity.worldCenter(level, b) : CoasterAnchorpointBlockEntity.worldCenterBase(level, b);
      Vec3 chord = startB.subtract(startA);
      if (chord.lengthSqr() < 1.0E-8) {
         return null;
      } else {
         Vec3 dir = chord.normalize();
         Vec3 axisB = dir.scale(-1.0);
         Vec3 normalA = faceNormal(solve.orientation(), startA, solve.cornerCenter(), dir, solve.planeN());
         Vec3 normalB = faceNormal(solve.orientation(), startB, solve.cornerCenter(), axisB, solve.planeN());
         BezierConnection bc = primaryCurve(a, b, startA, startB, dir, axisB, normalA, normalB, material, girder);
         double h = Math.max(1.0E-4, startA.distanceTo(startB) / 3.0);
         CoasterPeerCurveHandleLengths.putCanonical(bc, h, h);
         return bc;
      }
   }

   private static BezierConnection primaryCurve(
      BlockPos a, BlockPos b, Vec3 startA, Vec3 startB, Vec3 axisA, Vec3 axisB, Vec3 normalA, Vec3 normalB, TrackMaterial material, boolean girder
   ) {
      long al = a.asLong();
      long bl = b.asLong();
      BlockPos lo = al <= bl ? a : b;
      BlockPos hi = al <= bl ? b : a;
      Vec3 startLo = lo.equals(a) ? startA : startB;
      Vec3 startHi = hi.equals(a) ? startA : startB;
      Vec3 axisLo = lo.equals(a) ? axisA : axisB;
      Vec3 axisHi = hi.equals(a) ? axisA : axisB;
      Vec3 normalLo = lo.equals(a) ? normalA : normalB;
      Vec3 normalHi = hi.equals(a) ? normalA : normalB;
      return new BezierConnection(
         Couple.create(lo, hi),
         Couple.create(startLo, startHi),
         Couple.create(axisLo.normalize(), axisHi.normalize()),
         Couple.create(normalLo, normalHi),
         true,
         girder,
         material
      );
   }

   @Nullable
   private static BlockPos solePeer(CoasterAnchorpointBlockEntity be) {
      return be.legCount() != 1 ? null : be.getAnchorPeerCurvesView().keySet().iterator().next().immutable();
   }

   private static boolean allAnchorsHaveDefaultTilt(Level level, List<BlockPos> anchors) {
      for (BlockPos pos : anchors) {
         if (!(level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity be)) {
            return false;
         }

         if (Math.abs(be.getJointBankRadians() - 0.0F) > 1.0E-4F) {
            return false;
         }
      }

      return true;
   }

   private static boolean allAnchorsHaveDefaultLift(Level level, List<BlockPos> anchors) {
      float def = 0.5F;

      for (BlockPos pos : anchors) {
         if (!(level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity be)) {
            return false;
         }

         if (Math.abs(be.getLiftBlocks() - def) > 0.001F) {
            return false;
         }
      }

      return true;
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

   private static record CornerStraightRoles(BlockPos outer, BlockPos corner, BlockPos otherCorner) {
   }

   private static record Orientation(boolean radial, boolean towardCenter, Vec3 planeNormal, Vec3 parallelFaceUp) {
   }

   private static record QuarterSolve(
      CoasterQuarterCirclePlacement.Orientation orientation,
      double radius,
      Vec3 planeN,
      Vec3 cornerCenter,
      Vec3 startCornerA,
      Vec3 startCornerB,
      Vec3 travelA,
      Vec3 travelB,
      boolean useLiftedCenters
   ) {
   }
}
