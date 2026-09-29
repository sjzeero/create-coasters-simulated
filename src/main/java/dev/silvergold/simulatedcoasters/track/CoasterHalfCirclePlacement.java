package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphLookup;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathNode;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterHalfCirclePlacement {
   private static final double PERPENDICULAR_DOT_MAX = 0.08;
   private static final double ALIGN_DOT_MIN = 0.985;
   private static final double MIN_RADIUS_BLOCKS = 1.25;
   private static final double PARALLEL_PLANE_NORMAL_DOT = 0.85;
   private static final double RADIAL_FACING_DOT = 0.45;
   private static final double KAPPA_QUARTER = 1.3333333333333333 * Math.tan(Math.PI / 8);
   private static final double CONTINUITY_OPPOSITE_DOT_MAX = -0.985;
   private static final double INV_SQRT2 = 1.0 / Math.sqrt(2.0);

   private CoasterHalfCirclePlacement() {
   }

   @Nullable
   public static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildHalfCircle(
      Level level, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder
   ) {
      if (anchorA.equals(anchorB)) {
         return null;
      } else if (level.getBlockEntity(anchorA) instanceof CoasterAnchorpointBlockEntity
         && level.getBlockEntity(anchorB) instanceof CoasterAnchorpointBlockEntity) {
         if (existingPrimaryBetween(level, anchorA, anchorB) != null) {
            return null;
         } else {
            for (CoasterHalfCirclePlacement.Roles roles : collectRoleCandidates(level, anchorA, anchorB)) {
               CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult built = tryBuildWithRoles(level, roles, anchorA, anchorB, material, girder);
               if (built != null) {
                  return built;
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildWithRoles(
      Level level, CoasterHalfCirclePlacement.Roles roles, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder
   ) {
      List<BlockPos> involved = List.of(roles.outerA(), roles.cornerA(), roles.mid(), roles.cornerB(), roles.outerB());
      if (!allAnchorsHaveDefaultTilt(level, involved)) {
         return null;
      } else {
         CoasterHalfCirclePlacement.HalfSolve solve = solveHalf(level, roles);
         if (solve == null) {
            return null;
         } else {
            BezierConnection straightA = buildStraightLeg(roles.outerA(), roles.cornerA(), solve.startOuterA(), solve.startCornerA(), material, girder, solve);
            BezierConnection quarter1 = buildQuarter(
               roles.cornerA(), roles.mid(), solve.startCornerA(), solve.startMid(), solve.travel(), solve.dirAB(), solve, material, girder
            );
            BezierConnection quarter2 = buildQuarter(
               roles.mid(), roles.cornerB(), solve.startMid(), solve.startCornerB(), solve.dirAB(), solve.travel().scale(-1.0), solve, material, girder
            );
            BezierConnection straightB = buildStraightLeg(roles.outerB(), roles.cornerB(), solve.startOuterB(), solve.startCornerB(), material, girder, solve);
            if (straightA != null && quarter1 != null && quarter2 != null && straightB != null) {
               if (CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightA)
                  && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(quarter1)
                  && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(quarter2)
                  && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightB)) {
                  List<BezierConnection> all = List.of(straightA, quarter1, quarter2, straightB);
                  BezierConnection primary = null;
                  List<BezierConnection> neighbors = new ArrayList<>(6);

                  for (BezierConnection bc : all) {
                     if (isEdge(bc, anchorA, anchorB)) {
                        primary = bc;
                     } else {
                        neighbors.add(bc);
                     }
                  }

                  if (primary == null) {
                     return null;
                  } else if (!appendOuterContinuityAdjustments(level, neighbors, roles.outerA(), roles.cornerA(), straightA)) {
                     return null;
                  } else {
                     return !appendOuterContinuityAdjustments(level, neighbors, roles.outerB(), roles.cornerB(), straightB)
                        ? null
                        : new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(primary, List.of(), List.copyOf(neighbors));
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

   private static List<CoasterHalfCirclePlacement.Roles> collectRoleCandidates(Level level, BlockPos a, BlockPos b) {
      List<CoasterHalfCirclePlacement.Roles> out = new ArrayList<>(12);
      addCandidate(out, detectPlacingNearQuarter(level, a, b));
      addCandidate(out, detectPlacingNearQuarter(level, b, a));
      addCandidate(out, detectPlacingFarQuarter(level, a, b));
      addCandidate(out, detectPlacingFarQuarter(level, b, a));
      addCandidate(out, detectClosingOntoEmptyMid(level, a, b));
      addCandidate(out, detectClosingOntoEmptyMid(level, b, a));
      addCandidate(out, detectPlacingStraight(level, a, b));
      addCandidate(out, detectPlacingStraight(level, b, a));
      addCandidate(out, detectStraightOffQuarterWithFarParallel(level, a, b));
      addCandidate(out, detectStraightOffQuarterWithFarParallel(level, b, a));
      return out;
   }

   private static void addCandidate(List<CoasterHalfCirclePlacement.Roles> out, @Nullable CoasterHalfCirclePlacement.Roles roles) {
      if (roles != null) {
         for (CoasterHalfCirclePlacement.Roles existing : out) {
            if (existing.equals(roles)) {
               return;
            }
         }

         out.add(roles);
      }
   }

   @Nullable
   private static CoasterHalfCirclePlacement.Roles detectStraightOffQuarterWithFarParallel(Level level, BlockPos outer, BlockPos corner) {
      if (!(level.getBlockEntity(outer) instanceof CoasterAnchorpointBlockEntity beOuter)
         || !(level.getBlockEntity(corner) instanceof CoasterAnchorpointBlockEntity beCorner)) {
         return null;
      }

      if (beOuter.legCount() > 1) {
         return null;
      } else {
         if (beOuter.legCount() == 1) {
            BlockPos outerPeer = solePeer(beOuter);
            if (outerPeer == null || outerPeer.equals(corner)) {
               return null;
            }
         }

         if (beCorner.legCount() != 1) {
            return null;
         } else {
            BlockPos mid = solePeer(beCorner);
            if (mid == null || mid.equals(outer)) {
               return null;
            } else {
               if (!(level.getBlockEntity(mid) instanceof CoasterAnchorpointBlockEntity beMid)) {
                  return null;
               }

               BezierConnection quarter = existingPrimaryBetween(level, mid, corner);
               if (quarter == null) {
                  return null;
               } else {
                  Vec3 intoQuarter = CoasterBezierHandleEdit.tangentIntoFromAnchor(quarter, corner);
                  if (intoQuarter == null) {
                     return null;
                  } else {
                     intoQuarter = intoQuarter.normalize();
                     Vec3 cCorner = CoasterAnchorpointBlockEntity.worldCenter(level, corner);
                     Vec3 cPeer = CoasterAnchorpointBlockEntity.worldCenter(level, mid);
                     Vec3 toPeer = cPeer.subtract(cCorner);
                     if (!(toPeer.lengthSqr() < 1.0E-8) && !(toPeer.normalize().dot(intoQuarter) < 0.45)) {
                        Vec3 alongStraight = intoQuarter.scale(-1.0);
                        Vec3 cOuter = CoasterAnchorpointBlockEntity.worldCenter(level, outer);
                        Vec3 delta = cOuter.subtract(cCorner);
                        double len = delta.length();
                        if (len < 0.5) {
                           return null;
                        } else if (delta.scale(1.0 / len).dot(alongStraight) < 0.985) {
                           return null;
                        } else {
                           BlockPos cornerB;
                           BlockPos outerB;
                           if (beMid.legCount() == 2) {
                              cornerB = otherPeer(beMid, corner);
                              if (cornerB == null || cornerB.equals(outer)) {
                                 return null;
                              }

                              if (!(level.getBlockEntity(cornerB) instanceof CoasterAnchorpointBlockEntity beCornerB) || beCornerB.legCount() < 2) {
                                 return null;
                              }

                              outerB = otherPeer(beCornerB, mid);
                              if (outerB == null || outerB.equals(outer) || outerB.equals(corner) || outerB.equals(mid)) {
                                 return null;
                              }

                              if (existingPrimaryBetween(level, mid, cornerB) == null || existingPrimaryBetween(level, cornerB, outerB) == null) {
                                 return null;
                              }
                           } else {
                              if (beMid.legCount() != 1) {
                                 return null;
                              }

                              Vec3 cM = CoasterAnchorpointBlockEntity.worldCenter(level, mid);
                              Vec3 chord = cM.subtract(cCorner);
                              double chordLen = chord.length();
                              if (chordLen < 1.25 * Math.sqrt(2.0) * 0.5) {
                                 return null;
                              }

                              double chordDot = Math.abs(chord.normalize().dot(intoQuarter));
                              if (Math.abs(chordDot - INV_SQRT2) > 0.28) {
                                 return null;
                              }

                              double radius = chordLen / Math.sqrt(2.0);
                              if (radius < 1.25) {
                                 return null;
                              }

                              Vec3 dirAB = chord.scale(1.0 / radius).subtract(intoQuarter);
                              if (dirAB.lengthSqr() < 1.0E-8 || Math.abs(dirAB.length() - 1.0) > 0.12) {
                                 return null;
                              }

                              dirAB = dirAB.normalize();
                              if (Math.abs(dirAB.dot(intoQuarter)) > 0.08) {
                                 return null;
                              }

                              Vec3 center = cM.subtract(intoQuarter.scale(radius));
                              double fitTol = Math.max(0.75, radius * 0.12);
                              if (cCorner.distanceToSqr(center.subtract(dirAB.scale(radius))) > fitTol * fitTol) {
                                 return null;
                              }

                              Vec3 expectedB = center.add(dirAB.scale(radius));
                              cornerB = findParallelFarCorner(level, expectedB, intoQuarter, fitTol, corner, mid, outer);
                              if (cornerB == null) {
                                 return null;
                              }

                              if (!(level.getBlockEntity(cornerB) instanceof CoasterAnchorpointBlockEntity beCornerB) || beCornerB.legCount() != 1) {
                                 return null;
                              }

                              outerB = solePeer(beCornerB);
                              if (outerB == null || outerB.equals(outer) || outerB.equals(corner) || outerB.equals(mid) || outerB.equals(cornerB)) {
                                 return null;
                              }

                              if (existingPrimaryBetween(level, cornerB, outerB) == null) {
                                 return null;
                              }

                              if (existingPrimaryBetween(level, mid, cornerB) != null) {
                                 return null;
                              }
                           }

                           return new CoasterHalfCirclePlacement.Roles(
                              outer.immutable(), corner.immutable(), mid.immutable(), cornerB.immutable(), outerB.immutable()
                           );
                        }
                     } else {
                        return null;
                     }
                  }
               }
            }
         }
      }
   }

   @Nullable
   private static CoasterHalfCirclePlacement.Roles detectClosingOntoEmptyMid(Level level, BlockPos corner, BlockPos mid) {
      if (!(level.getBlockEntity(corner) instanceof CoasterAnchorpointBlockEntity beCorner)
         || !(level.getBlockEntity(mid) instanceof CoasterAnchorpointBlockEntity beMid)) {
         return null;
      }

      if (beCorner.legCount() == 1 && beMid.legCount() == 0) {
         BlockPos outerA = solePeer(beCorner);
         if (outerA != null && !outerA.equals(mid)) {
            Vec3 cOA = CoasterAnchorpointBlockEntity.worldCenter(level, outerA);
            Vec3 cA = CoasterAnchorpointBlockEntity.worldCenter(level, corner);
            Vec3 cM = CoasterAnchorpointBlockEntity.worldCenter(level, mid);
            Vec3 travel = cA.subtract(cOA);
            if (travel.lengthSqr() < 1.0E-8) {
               return null;
            } else {
               travel = travel.normalize();
               Vec3 chord = cM.subtract(cA);
               double chordLen = chord.length();
               if (chordLen < 1.25 * Math.sqrt(2.0) * 0.5) {
                  return null;
               } else {
                  double chordDotTravel = Math.abs(chord.normalize().dot(travel));
                  if (Math.abs(chordDotTravel - INV_SQRT2) > 0.28) {
                     return null;
                  } else {
                     double radius = chordLen / Math.sqrt(2.0);
                     if (radius < 1.25) {
                        return null;
                     } else {
                        Vec3 dirAB = chord.scale(1.0 / radius).subtract(travel);
                        if (dirAB.lengthSqr() < 1.0E-8) {
                           return null;
                        } else if (Math.abs(dirAB.length() - 1.0) > 0.12) {
                           return null;
                        } else {
                           dirAB = dirAB.normalize();
                           if (Math.abs(dirAB.dot(travel)) > 0.08) {
                              return null;
                           } else {
                              Vec3 center = cM.subtract(travel.scale(radius));
                              Vec3 expectedA = center.subtract(dirAB.scale(radius));
                              double fitTol = Math.max(0.75, radius * 0.1);
                              if (cA.distanceToSqr(expectedA) > fitTol * fitTol) {
                                 return null;
                              } else {
                                 Vec3 expectedB = center.add(dirAB.scale(radius));
                                 BlockPos cornerB = findParallelFarCorner(level, expectedB, travel, fitTol, corner, mid, outerA);
                                 if (cornerB == null) {
                                    return null;
                                 } else {
                                    if (level.getBlockEntity(cornerB) instanceof CoasterAnchorpointBlockEntity beCornerB && beCornerB.legCount() == 1) {
                                       BlockPos outerB = solePeer(beCornerB);
                                       if (outerB != null && !outerB.equals(corner) && !outerB.equals(mid) && !outerB.equals(outerA) && !outerB.equals(cornerB)
                                          )
                                        {
                                          if (existingPrimaryBetween(level, outerA, corner) != null && existingPrimaryBetween(level, cornerB, outerB) != null) {
                                             if (existingPrimaryBetween(level, mid, cornerB) != null) {
                                                return null;
                                             }

                                             return new CoasterHalfCirclePlacement.Roles(
                                                outerA.immutable(), corner.immutable(), mid.immutable(), cornerB.immutable(), outerB.immutable()
                                             );
                                          }

                                          return null;
                                       }

                                       return null;
                                    }

                                    return null;
                                 }
                              }
                           }
                        }
                     }
                  }
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
   private static BlockPos findParallelFarCorner(
      Level level, Vec3 expectedCorner, Vec3 travel, double tol, BlockPos excludeCorner, BlockPos excludeMid, BlockPos excludeOuter
   ) {
      double tolSqr = tol * tol;
      BlockPos best = null;
      double bestDist = Double.MAX_VALUE;
      CoasterPathGraph graph = CoasterPathGraphLookup.get(level);

      for (CoasterPathNode node : graph.nodes()) {
         BlockPos p = node.pos();
         if (!p.equals(excludeCorner) && !p.equals(excludeMid) && !p.equals(excludeOuter)) {
            double d = CoasterAnchorpointBlockEntity.worldCenter(level, p).distanceToSqr(expectedCorner);
            if (!(d > tolSqr) && !(d >= bestDist)) {
               BlockEntity outer = level.getBlockEntity(p);
               if (outer instanceof CoasterAnchorpointBlockEntity) {
                  CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)outer;
                  if (be.legCount() == 1) {
                     BlockPos outerx = solePeer(be);
                     if (outerx != null && !outerx.equals(excludeCorner) && !outerx.equals(excludeMid) && !outerx.equals(excludeOuter) && !outerx.equals(p)) {
                        Vec3 t = CoasterAnchorpointBlockEntity.worldCenter(level, p).subtract(CoasterAnchorpointBlockEntity.worldCenter(level, outerx));
                        if (!(t.lengthSqr() < 1.0E-8) && !(t.normalize().dot(travel) < 0.985)) {
                           best = p.immutable();
                           bestDist = d;
                        }
                     }
                  }
               }
            }
         }
      }

      if (best != null) {
         return best;
      } else {
         BlockPos guess = BlockPos.containing(expectedCorner);

         for (BlockPos p : BlockPos.betweenClosed(guess.offset(-4, -4, -4), guess.offset(4, 4, 4))) {
            if (!p.equals(excludeCorner) && !p.equals(excludeMid) && !p.equals(excludeOuter)) {
               double d = CoasterAnchorpointBlockEntity.worldCenter(level, p).distanceToSqr(expectedCorner);
               if (!(d > tolSqr) && !(d >= bestDist)) {
                  BlockEntity var28 = level.getBlockEntity(p);
                  if (var28 instanceof CoasterAnchorpointBlockEntity) {
                     CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)var28;
                     if (be.legCount() == 1) {
                        BlockPos outer = solePeer(be);
                        if (outer != null && !outer.equals(excludeCorner) && !outer.equals(excludeMid) && !outer.equals(excludeOuter) && !outer.equals(p)) {
                           Vec3 t = CoasterAnchorpointBlockEntity.worldCenter(level, p).subtract(CoasterAnchorpointBlockEntity.worldCenter(level, outer));
                           if (!(t.lengthSqr() < 1.0E-8) && !(t.normalize().dot(travel) < 0.985)) {
                              best = p.immutable();
                              bestDist = d;
                           }
                        }
                     }
                  }
               }
            }
         }

         return best;
      }
   }

   @Nullable
   private static CoasterHalfCirclePlacement.Roles detectPlacingNearQuarter(Level level, BlockPos cornerA, BlockPos mid) {
      if (!(level.getBlockEntity(cornerA) instanceof CoasterAnchorpointBlockEntity beCornerA)
         || !(level.getBlockEntity(mid) instanceof CoasterAnchorpointBlockEntity beMid)) {
         return null;
      }

      if (beCornerA.legCount() == 1 && beMid.legCount() == 1) {
         BlockPos outerA = solePeer(beCornerA);
         BlockPos cornerB = solePeer(beMid);
         if (outerA == null || cornerB == null || outerA.equals(mid) || cornerB.equals(cornerA) || outerA.equals(cornerB)) {
            return null;
         } else {
            if (level.getBlockEntity(cornerB) instanceof CoasterAnchorpointBlockEntity beCornerB && beCornerB.legCount() >= 2) {
               BlockPos outerB = otherPeer(beCornerB, mid);
               if (outerB != null && !outerB.equals(cornerA) && !outerB.equals(outerA)) {
                  if (!(level.getBlockEntity(outerB) instanceof CoasterAnchorpointBlockEntity)) {
                     return null;
                  }

                  if (existingPrimaryBetween(level, outerA, cornerA) != null
                     && existingPrimaryBetween(level, mid, cornerB) != null
                     && existingPrimaryBetween(level, cornerB, outerB) != null) {
                     return new CoasterHalfCirclePlacement.Roles(
                        outerA.immutable(), cornerA.immutable(), mid.immutable(), cornerB.immutable(), outerB.immutable()
                     );
                  }

                  return null;
               }

               return null;
            }

            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   private static CoasterHalfCirclePlacement.Roles detectPlacingFarQuarter(Level level, BlockPos mid, BlockPos cornerB) {
      if (!(level.getBlockEntity(mid) instanceof CoasterAnchorpointBlockEntity beMid)
         || !(level.getBlockEntity(cornerB) instanceof CoasterAnchorpointBlockEntity beCornerB)) {
         return null;
      }

      if (beMid.legCount() == 1 && beCornerB.legCount() == 1) {
         BlockPos cornerA = solePeer(beMid);
         BlockPos outerB = solePeer(beCornerB);
         if (cornerA == null || outerB == null || cornerA.equals(cornerB) || outerB.equals(mid) || cornerA.equals(outerB)) {
            return null;
         } else {
            if (level.getBlockEntity(cornerA) instanceof CoasterAnchorpointBlockEntity beCornerA && beCornerA.legCount() >= 2) {
               BlockPos outerA = otherPeer(beCornerA, mid);
               if (outerA != null && !outerA.equals(cornerB) && !outerA.equals(outerB) && !outerA.equals(mid)) {
                  if (!(level.getBlockEntity(outerA) instanceof CoasterAnchorpointBlockEntity)) {
                     return null;
                  }

                  if (existingPrimaryBetween(level, outerA, cornerA) != null
                     && existingPrimaryBetween(level, cornerA, mid) != null
                     && existingPrimaryBetween(level, cornerB, outerB) != null) {
                     return new CoasterHalfCirclePlacement.Roles(
                        outerA.immutable(), cornerA.immutable(), mid.immutable(), cornerB.immutable(), outerB.immutable()
                     );
                  }

                  return null;
               }

               return null;
            }

            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   private static CoasterHalfCirclePlacement.Roles detectPlacingStraight(Level level, BlockPos outer, BlockPos corner) {
      if (!(level.getBlockEntity(outer) instanceof CoasterAnchorpointBlockEntity beOuter)
         || !(level.getBlockEntity(corner) instanceof CoasterAnchorpointBlockEntity beCorner)) {
         return null;
      }

      if (beOuter.legCount() > 1) {
         return null;
      } else {
         if (beOuter.legCount() == 1) {
            BlockPos outerPeer = solePeer(beOuter);
            if (outerPeer == null || outerPeer.equals(corner)) {
               return null;
            }
         }

         if (beCorner.legCount() != 1) {
            return null;
         } else {
            BlockPos mid = solePeer(beCorner);
            if (mid == null || mid.equals(outer)) {
               return null;
            } else {
               if (level.getBlockEntity(mid) instanceof CoasterAnchorpointBlockEntity beMid && beMid.legCount() == 2) {
                  BlockPos cornerB = otherPeer(beMid, corner);
                  if (cornerB != null && !cornerB.equals(outer)) {
                     if (level.getBlockEntity(cornerB) instanceof CoasterAnchorpointBlockEntity beCornerB && beCornerB.legCount() >= 2) {
                        BlockPos outerB = otherPeer(beCornerB, mid);
                        if (outerB != null && !outerB.equals(outer) && !outerB.equals(corner) && !outerB.equals(mid)) {
                           if (!(level.getBlockEntity(outerB) instanceof CoasterAnchorpointBlockEntity)) {
                              return null;
                           }

                           if (existingPrimaryBetween(level, corner, mid) != null
                              && existingPrimaryBetween(level, mid, cornerB) != null
                              && existingPrimaryBetween(level, cornerB, outerB) != null) {
                              return new CoasterHalfCirclePlacement.Roles(
                                 outer.immutable(), corner.immutable(), mid.immutable(), cornerB.immutable(), outerB.immutable()
                              );
                           }

                           return null;
                        }

                        return null;
                     }

                     return null;
                  }

                  return null;
               }

               return null;
            }
         }
      }
   }

   @Nullable
   private static CoasterHalfCirclePlacement.HalfSolve solveHalf(Level level, CoasterHalfCirclePlacement.Roles roles) {
      Vec3 cOA = CoasterAnchorpointBlockEntity.worldCenter(level, roles.outerA());
      Vec3 cA = CoasterAnchorpointBlockEntity.worldCenter(level, roles.cornerA());
      Vec3 cM = CoasterAnchorpointBlockEntity.worldCenter(level, roles.mid());
      Vec3 cB = CoasterAnchorpointBlockEntity.worldCenter(level, roles.cornerB());
      Vec3 cOB = CoasterAnchorpointBlockEntity.worldCenter(level, roles.outerB());
      Vec3 travelA = cA.subtract(cOA);
      Vec3 travelB = cB.subtract(cOB);
      if (!(travelA.lengthSqr() < 1.0E-8) && !(travelB.lengthSqr() < 1.0E-8)) {
         travelA = travelA.normalize();
         travelB = travelB.normalize();
         if (travelA.dot(travelB) < 0.985) {
            return null;
         } else {
            Vec3 diameter = cB.subtract(cA);
            double dist = diameter.length();
            if (dist < 1.25) {
               return null;
            } else {
               double radius = dist / 2.0;
               if (radius < 1.25) {
                  return null;
               } else {
                  Vec3 dirAB = diameter.scale(1.0 / dist);
                  if (Math.abs(travelA.dot(dirAB)) > 0.08) {
                     return null;
                  } else {
                     Vec3 center = cA.add(cB).scale(0.5);
                     Vec3 expectedMid = center.add(travelA.scale(radius));
                     double midTol = Math.max(1.0, radius * 0.15);
                     if (cM.distanceToSqr(expectedMid) > midTol * midTol) {
                        return null;
                     } else if (Math.abs(cM.subtract(center).length() - radius) > midTol) {
                        return null;
                     } else {
                        Vec3 planeN = dirAB.cross(travelA);
                        if (planeN.lengthSqr() < 1.0E-12) {
                           return null;
                        } else {
                           planeN = planeN.normalize();
                           List<BlockPos> involved = List.of(roles.outerA(), roles.cornerA(), roles.mid(), roles.cornerB(), roles.outerB());
                           CoasterHalfCirclePlacement.Orientation orientation = classifyOrientation(level, involved, planeN, center);
                           if (orientation == null) {
                              return null;
                           } else if (orientation.radial() && !allAnchorsHaveDefaultLift(level, involved)) {
                              return null;
                           } else if (orientation.radial()) {
                              return new CoasterHalfCirclePlacement.HalfSolve(orientation, radius, center, planeN, travelA, dirAB, cOA, cA, cM, cB, cOB, true);
                           } else {
                              Vec3 bOA = CoasterAnchorpointBlockEntity.worldCenterBase(level, roles.outerA());
                              Vec3 bA = CoasterAnchorpointBlockEntity.worldCenterBase(level, roles.cornerA());
                              Vec3 bM = CoasterAnchorpointBlockEntity.worldCenterBase(level, roles.mid());
                              Vec3 bB = CoasterAnchorpointBlockEntity.worldCenterBase(level, roles.cornerB());
                              Vec3 bOB = CoasterAnchorpointBlockEntity.worldCenterBase(level, roles.outerB());
                              Vec3 tA = bA.subtract(bOA);
                              Vec3 tB = bB.subtract(bOB);
                              if (!(tA.lengthSqr() < 1.0E-8) && !(tB.lengthSqr() < 1.0E-8)) {
                                 tA = tA.normalize();
                                 tB = tB.normalize();
                                 if (tA.dot(travelA) < 0.0) {
                                    tA = tA.scale(-1.0);
                                 }

                                 if (tB.dot(travelB) < 0.0) {
                                    tB = tB.scale(-1.0);
                                 }

                                 if (tA.dot(tB) < 0.985) {
                                    return null;
                                 } else {
                                    Vec3 baseDiam = bB.subtract(bA);
                                    double baseDist = baseDiam.length();
                                    double baseR = baseDist / 2.0;
                                    if (baseR < 1.25) {
                                       return null;
                                    } else {
                                       Vec3 baseDir = baseDiam.scale(1.0 / baseDist);
                                       if (Math.abs(tA.dot(baseDir)) > 0.08) {
                                          return null;
                                       } else {
                                          Vec3 baseCenter = bA.add(bB).scale(0.5);
                                          Vec3 baseExpectedMid = baseCenter.add(tA.scale(baseR));
                                          double baseTol = Math.max(1.0, baseR * 0.15);
                                          if (bM.distanceToSqr(baseExpectedMid) > baseTol * baseTol) {
                                             return null;
                                          } else {
                                             Vec3 baseN = baseDir.cross(tA);
                                             if (baseN.lengthSqr() < 1.0E-12) {
                                                return null;
                                             } else {
                                                baseN = baseN.normalize();
                                                if (baseN.dot(orientation.planeNormal()) < 0.0) {
                                                   baseN = baseN.scale(-1.0);
                                                }

                                                return new CoasterHalfCirclePlacement.HalfSolve(
                                                   orientation, baseR, baseCenter, baseN, tA, baseDir, bOA, bA, bM, bB, bOB, false
                                                );
                                             }
                                          }
                                       }
                                    }
                                 }
                              } else {
                                 return null;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      } else {
         return null;
      }
   }

   @Nullable
   private static CoasterHalfCirclePlacement.Orientation classifyOrientation(Level level, List<BlockPos> anchors, Vec3 planeN, Vec3 circleCenter) {
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
            Vec3 toward = circleCenter.subtract(CoasterAnchorpointBlockEntity.worldCenterBase(level, pos));
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

         return new CoasterHalfCirclePlacement.Orientation(false, false, planeN, faceUp);
      } else {
         return radialCount == anchors.size() ? new CoasterHalfCirclePlacement.Orientation(true, radialSign > 0, planeN, Vec3.ZERO) : null;
      }
   }

   private static BezierConnection buildQuarter(
      BlockPos first,
      BlockPos second,
      Vec3 startFirst,
      Vec3 startSecond,
      Vec3 travelFirst,
      Vec3 travelSecond,
      CoasterHalfCirclePlacement.HalfSolve solve,
      TrackMaterial material,
      boolean girder
   ) {
      Vec3 axisSecond = travelSecond.scale(-1.0);
      Vec3 normalFirst = faceNormal(solve.orientation(), startFirst, solve.center(), travelFirst, solve.planeN());
      Vec3 normalSecond = faceNormal(solve.orientation(), startSecond, solve.center(), axisSecond, solve.planeN());
      BezierConnection bc = primaryCurve(first, second, startFirst, startSecond, travelFirst, axisSecond, normalFirst, normalSecond, material, girder);
      double handle = Math.max(1.0E-4, solve.radius() * KAPPA_QUARTER);
      CoasterPeerCurveHandleLengths.putCanonical(bc, handle, handle);
      return bc;
   }

   @Nullable
   private static BezierConnection buildStraightLeg(
      BlockPos outer, BlockPos corner, Vec3 startOuter, Vec3 startCorner, TrackMaterial material, boolean girder, CoasterHalfCirclePlacement.HalfSolve solve
   ) {
      Vec3 chord = startCorner.subtract(startOuter);
      if (chord.lengthSqr() < 1.0E-8) {
         return null;
      } else {
         Vec3 dir = chord.normalize();
         if (dir.dot(solve.travel()) < 0.0) {
            dir = dir.scale(-1.0);
         }

         Vec3 axisCorner = dir.scale(-1.0);
         Vec3 normalOuter = faceNormal(solve.orientation(), startOuter, solve.center(), dir, solve.planeN());
         Vec3 normalCorner = faceNormal(solve.orientation(), startCorner, solve.center(), axisCorner, solve.planeN());
         BezierConnection bc = primaryCurve(outer, corner, startOuter, startCorner, dir, axisCorner, normalOuter, normalCorner, material, girder);
         double h = Math.max(1.0E-4, startOuter.distanceTo(startCorner) / 3.0);
         CoasterPeerCurveHandleLengths.putCanonical(bc, h, h);
         return bc;
      }
   }

   private static Vec3 faceNormal(CoasterHalfCirclePlacement.Orientation orientation, Vec3 start, Vec3 circleCenter, Vec3 axis, Vec3 planeN) {
      if (orientation.radial()) {
         Vec3 toward = circleCenter.subtract(start);
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

   private static boolean appendOuterContinuityAdjustments(
      Level level, List<BezierConnection> neighbors, BlockPos outer, BlockPos corner, BezierConnection straightenedApproach
   ) {
      if (!(level.getBlockEntity(outer) instanceof CoasterAnchorpointBlockEntity be)) {
         return true;
      } else {
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
                  if (!CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(adjusted)) {
                     return false;
                  }

                  Vec3 intoAdj = CoasterBezierHandleEdit.tangentIntoFromAnchor(adjusted, outer);
                  if (intoAdj == null || intoApproach.normalize().dot(intoAdj.normalize()) > -0.985) {
                     return false;
                  }

                  neighbors.add(adjusted);
               }
            }

            return true;
         }
      }
   }

   private static boolean isEdge(BezierConnection bc, BlockPos a, BlockPos b) {
      BlockPos lo = (BlockPos)bc.bePositions.getFirst();
      BlockPos hi = (BlockPos)bc.bePositions.getSecond();
      return lo.equals(a) && hi.equals(b) || lo.equals(b) && hi.equals(a);
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
      Vec3 a = axis.lengthSqr() < 1.0E-12 ? axis : axis.normalize();
      Vec3 n = normal.subtract(a.scale(normal.dot(a)));
      return n.lengthSqr() < 1.0E-12 ? a : n.normalize();
   }

   private static record HalfSolve(
      CoasterHalfCirclePlacement.Orientation orientation,
      double radius,
      Vec3 center,
      Vec3 planeN,
      Vec3 travel,
      Vec3 dirAB,
      Vec3 startOuterA,
      Vec3 startCornerA,
      Vec3 startMid,
      Vec3 startCornerB,
      Vec3 startOuterB,
      boolean useLiftedCenters
   ) {
   }

   private static record Orientation(boolean radial, boolean towardCenter, Vec3 planeNormal, Vec3 parallelFaceUp) {
   }

   private static record Roles(BlockPos outerA, BlockPos cornerA, BlockPos mid, BlockPos cornerB, BlockPos outerB) {
   }
}
