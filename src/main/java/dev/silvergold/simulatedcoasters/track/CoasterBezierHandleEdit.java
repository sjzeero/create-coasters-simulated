package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterTrackPropagator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterBezierHandleEdit {
   private static final double OPPOSITE_INCIDENT_TANGENT_DOT = -0.65;

   private CoasterBezierHandleEdit() {
   }

   @Nullable
   static Vec3 jointBankSideForAnchor(Level level, BlockPos anchorPos, Vec3 tangentUnit, Vec3 geometricFaceNormal) {
      Vec3 side = tangentUnit.cross(geometricFaceNormal);
      if (side.lengthSqr() >= 1.0E-12) {
         return side.normalize();
      } else {
         Vec3 ref = CoasterTrackPlacement.anchorFacingPerpendicularToAxis(level, anchorPos, tangentUnit);
         side = tangentUnit.cross(ref);
         return side.lengthSqr() < 1.0E-12 ? null : side.normalize();
      }
   }

   static Vec3 bankRotatedFaceNormal(Vec3 geometricFaceNormal, Vec3 side, double theta) {
      return geometricFaceNormal.scale(Math.cos(theta)).add(side.scale(Math.sin(theta))).normalize();
   }

   public static CoasterBezierHandleEdit.PreviewResult computePreview(
      Level level, BlockPos primaryHost, BlockPos remoteEnd, int endpointIndex, Vec3 handleOffsetWorld
   ) {
      BezierConnection bc = fetchPrimary(level, primaryHost, remoteEnd);
      if (bc == null || !bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
         return CoasterBezierHandleEdit.PreviewResult.invalid();
      } else if (endpointIndex != 0 && endpointIndex != 1) {
         return CoasterBezierHandleEdit.PreviewResult.invalid();
      } else {
         double newLen = Math.sqrt(handleOffsetWorld.lengthSqr());
         if (newLen < 1.0E-8) {
            return CoasterBezierHandleEdit.PreviewResult.invalid();
         } else {
            newLen = Math.min(newLen, CoasterPeerCurveHandleLengths.maxHandleLengthForEdge(level, bc));
            Vec3 axisStored = handleOffsetWorld.scale(1.0 / Math.sqrt(handleOffsetWorld.lengthSqr()));
            BlockPos ep = endpointIndex == 0 ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
            BlockPos oppositeOnLeg = endpointIndex == 0 ? (BlockPos)bc.bePositions.getSecond() : (BlockPos)bc.bePositions.getFirst();
            CoasterBezierHandleEdit.HandleAxisClampSpec clampSpec = handleAxisClampReference(level, ep, oppositeOnLeg);
            if (clampSpec != null) {
               axisStored = clampUnitVectorWithinCone(axisStored, clampSpec.reference(), (double)clampSpec.maxAwayRadians());
            }

            double at0 = endpointIndex == 0 ? newLen : CoasterPeerCurveHandleLengths.getLengthAtEndpoint(bc, 0);
            double at1 = endpointIndex == 1 ? newLen : CoasterPeerCurveHandleLengths.getLengthAtEndpoint(bc, 1);
            if (!(level.getBlockEntity(ep) instanceof CoasterAnchorpointBlockEntity be)) {
               return CoasterBezierHandleEdit.PreviewResult.invalid();
            } else {
               String var34 = edgeKey(bc);
               BezierConnection neighbor = null;
               Vec3 tEditedBefore = tangentIntoFromAnchor(bc, ep);

               for (BezierConnection inc : incidentCoasterPrimariesAtAnchor(level, be)) {
                  if (!edgeKey(inc).equals(var34)) {
                     Vec3 tN = tangentIntoFromAnchor(inc, ep);
                     if (tN != null && tEditedBefore != null) {
                        Vec3 u0 = tEditedBefore.normalize();
                        Vec3 u1 = tN.normalize();
                        if (u0.dot(u1) < -0.65) {
                           neighbor = inc;
                           break;
                        }
                     }
                  }
               }

               BezierConnection edited = CoasterTrackPlacement.curveWithAxisAtEndpointForEdit(level, bc, ep, axisStored);
               BlockPos lo = CoasterPeerCurveHandleLengths.lexMin((BlockPos)edited.bePositions.getFirst(), (BlockPos)edited.bePositions.getSecond());
               double hLo = ((BlockPos)edited.bePositions.getFirst()).equals(lo) ? at0 : at1;
               double hHi = ((BlockPos)edited.bePositions.getFirst()).equals(lo) ? at1 : at0;
               CoasterPeerCurveHandleLengths.putPreviewCanonical(edited, hLo, hHi);
               List<BezierConnection> all = new ArrayList<>();
               all.add(edited);
               if (neighbor != null) {
                  Vec3 intoNeighbor = axisStored.scale(-1.0);
                  BezierConnection neighborEdited = CoasterTrackPlacement.curveWithAxisAtEndpointForEdit(level, neighbor, ep, intoNeighbor);
                  CoasterPeerCurveHandleLengths.copyEdgeLengths(neighbor, neighborEdited);
                  all.add(neighborEdited);
               }

               repinLiftedStartsOnBatchAnchors(level, all);

               for (BezierConnection c : all) {
                  if (edgeKey(c).equals(var34)) {
                     double cLo = ((BlockPos)c.bePositions.getFirst()).equals(lo) ? at0 : at1;
                     double cHi = ((BlockPos)c.bePositions.getFirst()).equals(lo) ? at1 : at0;
                     CoasterPeerCurveHandleLengths.putPreviewCanonical(c, cLo, cHi);
                  }
               }

               all.sort(Comparator.comparing(CoasterBezierHandleEdit::edgeKey));
               boolean ok = true;

               for (BezierConnection cx : all) {
                  if (!CoasterTrackPlacement.validateEditedCoasterCurve(level, cx, all)) {
                     ok = false;
                     break;
                  }
               }

               if (ok && !previewCurvesAtAnchorMeetMinRadius(ep, all)) {
                  ok = false;
               }

               BezierConnection editedOut = edited;

               for (BezierConnection cxx : all) {
                  if (edgeKey(cxx).equals(var34)) {
                     editedOut = cxx;
                     break;
                  }
               }

               return new CoasterBezierHandleEdit.PreviewResult(editedOut, List.copyOf(all), ok);
            }
         }
      }
   }

   public static CoasterBezierHandleEdit.TangentGuideSnapResult snapBezierTangentVirtualTargetWithGuidePlanes(
      Level level, BlockPos primaryHost, BlockPos remoteEnd, int endpointIndex, Vec3 virtualTargetWorld, boolean snapActive, double snapDistanceBlocks
   ) {
      if (snapActive && !(snapDistanceBlocks <= 0.0) && (endpointIndex == 0 || endpointIndex == 1)) {
         BezierConnection bc = fetchPrimary(level, primaryHost, remoteEnd);
         if (bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
            BlockPos p0 = (BlockPos)bc.bePositions.getFirst();
            BlockPos p1 = (BlockPos)bc.bePositions.getSecond();
            BlockPos edited = endpointIndex == 0 ? p0 : p1;
            BlockPos opposite = endpointIndex == 0 ? p1 : p0;
            Vec3 a = CoasterAnchorpointBlockEntity.worldCenter(level, edited);
            Vec3 b = CoasterAnchorpointBlockEntity.worldCenter(level, opposite);
            CoasterBezierHandleEdit.ChordGuideAxes setA = chordGuideAxesForChordWorld(b.subtract(a));
            if (setA == null) {
               return new CoasterBezierHandleEdit.TangentGuideSnapResult(virtualTargetWorld, 0);
            } else {
               Vec3 relA = virtualTargetWorld.subtract(a);
               double dAt = relA.dot(setA.nTall);
               double dAf = relA.dot(setA.nFlat);
               boolean nearAt = Math.abs(dAt) < snapDistanceBlocks;
               boolean nearAf = Math.abs(dAf) < snapDistanceBlocks;
               if (nearAt && nearAf) {
                  return new CoasterBezierHandleEdit.TangentGuideSnapResult(a.add(setA.u.scale(relA.dot(setA.u))), 2);
               } else {
                  CoasterBezierHandleEdit.ChordGuideAxes setB = null;
                  double dBt = 0.0;
                  double dBf = 0.0;
                  boolean nearBt = false;
                  boolean nearBf = false;
                  if (level.getBlockEntity(edited) instanceof CoasterAnchorpointBlockEntity beHub) {
                     List<BezierConnection> hubInc = incidentPrimaryCoasterCurves(level, beHub);
                     if (hubInc.size() >= 2) {
                        BlockPos r0 = otherEndpointBlock(hubInc.get(0), edited);
                        BlockPos r1 = otherEndpointBlock(hubInc.get(1), edited);
                        if (!r0.equals(r1)) {
                           Vec3 w0 = CoasterAnchorpointBlockEntity.worldCenter(level, r0);
                           Vec3 w1 = CoasterAnchorpointBlockEntity.worldCenter(level, r1);
                           setB = chordGuideAxesForChordWorld(w1.subtract(w0));
                           if (setB != null) {
                              dBt = relA.dot(setB.nTall);
                              dBf = relA.dot(setB.nFlat);
                              nearBt = Math.abs(dBt) < snapDistanceBlocks;
                              nearBf = Math.abs(dBf) < snapDistanceBlocks;
                              if (nearBt && nearBf) {
                                 return new CoasterBezierHandleEdit.TangentGuideSnapResult(a.add(setB.u.scale(relA.dot(setB.u))), 2);
                              }
                           }
                        }
                     }
                  }

                  Double bestAbs = null;
                  Vec3 bestN = null;
                  double bestD = 0.0;
                  if (nearAt) {
                     bestAbs = Math.abs(dAt);
                     bestN = setA.nTall;
                     bestD = dAt;
                  }

                  if (nearAf && (bestAbs == null || Math.abs(dAf) < bestAbs)) {
                     bestAbs = Math.abs(dAf);
                     bestN = setA.nFlat;
                     bestD = dAf;
                  }

                  if (setB != null) {
                     if (nearBt && (bestAbs == null || Math.abs(dBt) < bestAbs)) {
                        bestAbs = Math.abs(dBt);
                        bestN = setB.nTall;
                        bestD = dBt;
                     }

                     if (nearBf && (bestAbs == null || Math.abs(dBf) < bestAbs)) {
                        bestAbs = Math.abs(dBf);
                        bestN = setB.nFlat;
                        bestD = dBf;
                     }
                  }

                  return bestN != null
                     ? new CoasterBezierHandleEdit.TangentGuideSnapResult(virtualTargetWorld.subtract(bestN.scale(bestD)), 1)
                     : new CoasterBezierHandleEdit.TangentGuideSnapResult(virtualTargetWorld, 0);
               }
            }
         } else {
            return new CoasterBezierHandleEdit.TangentGuideSnapResult(virtualTargetWorld, 0);
         }
      } else {
         return new CoasterBezierHandleEdit.TangentGuideSnapResult(virtualTargetWorld, 0);
      }
   }

   private static BlockPos otherEndpointBlock(BezierConnection c, BlockPos anchor) {
      return ((BlockPos)c.bePositions.getFirst()).equals(anchor) ? (BlockPos)c.bePositions.getSecond() : (BlockPos)c.bePositions.getFirst();
   }

   @Nullable
   private static CoasterBezierHandleEdit.ChordGuideAxes chordGuideAxesForChordWorld(Vec3 chordVectorWorld) {
      if (chordVectorWorld.lengthSqr() < 1.0E-10) {
         return null;
      } else {
         Vec3 u = chordVectorWorld.normalize();
         Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
         Vec3 nTallRaw = u.cross(worldUp);
         Vec3 nTall = nTallRaw.lengthSqr() >= 1.0E-12 ? nTallRaw.normalize() : u.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
         Vec3 nFlatRaw = nTall.cross(u);
         if (nFlatRaw.lengthSqr() < 1.0E-12) {
            return null;
         } else {
            Vec3 nFlat = nFlatRaw.normalize();
            return new CoasterBezierHandleEdit.ChordGuideAxes(u, nTall, nFlat);
         }
      }
   }

   @Nullable
   private static CoasterBezierHandleEdit.HandleAxisClampSpec handleAxisClampReference(Level level, BlockPos edited, BlockPos oppositeOnLeg) {
      Vec3 a = CoasterAnchorpointBlockEntity.worldCenter(level, edited);
      Vec3 b = CoasterAnchorpointBlockEntity.worldCenter(level, oppositeOnLeg);
      Vec3 legDir = b.subtract(a);
      if (legDir.lengthSqr() < 1.0E-10) {
         return null;
      } else {
         legDir = legDir.normalize();
         if (level.getBlockEntity(edited) instanceof CoasterAnchorpointBlockEntity beHub) {
            List<BezierConnection> hubInc = incidentPrimaryCoasterCurves(level, beHub);
            if (hubInc.size() >= 2) {
               BlockPos r0 = otherEndpointBlock(hubInc.get(0), edited);
               BlockPos r1 = otherEndpointBlock(hubInc.get(1), edited);
               if (!r0.equals(r1)) {
                  Vec3 w0 = CoasterAnchorpointBlockEntity.worldCenter(level, r0);
                  Vec3 w1 = CoasterAnchorpointBlockEntity.worldCenter(level, r1);
                  CoasterBezierHandleEdit.ChordGuideAxes setB = chordGuideAxesForChordWorld(w1.subtract(w0));
                  if (setB != null) {
                     Vec3 ref = setB.u;
                     if (ref.dot(legDir) < 0.0) {
                        ref = ref.scale(-1.0);
                     }

                     return new CoasterBezierHandleEdit.HandleAxisClampSpec(ref, CoasterTrackGauge.BEZIER_HANDLE_HUB_MAX_AXIS_AWAY_RADIANS);
                  }
               }
            }
         }

         return new CoasterBezierHandleEdit.HandleAxisClampSpec(legDir, CoasterTrackGauge.BEZIER_HANDLE_MAX_AXIS_AWAY_RADIANS);
      }
   }

   private static Vec3 clampUnitVectorWithinCone(Vec3 axis, Vec3 reference, double maxAngleRadians) {
      axis = axis.normalize();
      reference = reference.normalize();
      double maxDot = Math.cos(maxAngleRadians);
      double dot = axis.dot(reference);
      if (dot >= maxDot) {
         return axis;
      } else {
         Vec3 perp = axis.subtract(reference.scale(dot));
         double perpLenSqr = perp.lengthSqr();
         if (perpLenSqr < 1.0E-12) {
            perp = reference.cross(new Vec3(0.0, 1.0, 0.0));
            if (perp.lengthSqr() < 1.0E-12) {
               perp = reference.cross(new Vec3(1.0, 0.0, 0.0));
            }

            perp = perp.normalize();
         } else {
            perp = perp.scale(1.0 / Math.sqrt(perpLenSqr));
         }

         double sinMax = Math.sin(maxAngleRadians);
         double cosMax = Math.cos(maxAngleRadians);
         return reference.scale(cosMax).add(perp.scale(sinMax));
      }
   }

   public static Vec3 anchorLiftDirectionFromCanonicalTangent(Level level, BlockPos anchorPos, Vec3 canonicalTangentInto) {
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
         Vec3 t0 = canonicalTangentInto.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : canonicalTangentInto.normalize();
         Vec3 n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t0);
         Vec3 side = jointBankSideForAnchor(level, anchorPos, t0, n0);
         return side == null
            ? CoasterTrackPlacement.anchorFaceNormalForAxis(level, anchorPos, t0)
            : bankRotatedFaceNormal(n0, side, (double)be.getJointBankRadians()).normalize();
      } else {
         return CoasterAnchorpointBlockEntity.localUp(level, anchorPos);
      }
   }

   public static Vec3 liftedWorldCenterFromCanonicalTangent(Level level, BlockPos anchorPos, Vec3 canonicalTangentInto) {
      Vec3 base = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
         float lift = be.getLiftBlocks();
         return Math.abs(lift) < 1.0E-9F ? base : base.add(anchorLiftDirectionFromCanonicalTangent(level, anchorPos, canonicalTangentInto).scale((double)lift));
      } else {
         return base;
      }
   }

   @Nullable
   static Vec3 canonicalLiftTangentAtHub(Level level, BlockPos anchorPos, List<BezierConnection> batch) {
      if (!(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be)) {
         return null;
      } else {
         Map<String, BezierConnection> var9 = new HashMap<>();

         for (BezierConnection c : batch) {
            if (((BlockPos)c.bePositions.getFirst()).equals(anchorPos) || ((BlockPos)c.bePositions.getSecond()).equals(anchorPos)) {
               var9.put(edgeKey(c), c);
            }
         }

         List<BezierConnection> ordered = new ArrayList<>();

         for (BezierConnection committed : incidentPrimaryCoasterCurves(level, be)) {
            BezierConnection use = var9.getOrDefault(edgeKey(committed), committed);
            ordered.add(use);
         }

         if (ordered.isEmpty()) {
            ordered.addAll(var9.values());
            ordered.sort(
               Comparator.comparingLong(
                  cx -> {
                     BlockPos remote = ((BlockPos)cx.bePositions.getFirst()).equals(anchorPos)
                        ? (BlockPos)cx.bePositions.getSecond()
                        : (BlockPos)cx.bePositions.getFirst();
                     return remote.asLong();
                  }
               )
            );
         }

         return ordered.isEmpty() ? null : tangentIntoFromAnchor(ordered.get(0), anchorPos);
      }
   }

   public static void repinLiftedStartsOnBatchAnchors(Level level, List<BezierConnection> batch) {
      if (!batch.isEmpty()) {
         Set<BlockPos> touchedAnchors = new HashSet<>();

         for (BezierConnection c : batch) {
            if (level.getBlockState((BlockPos)c.bePositions.getFirst()).getBlock() instanceof CoasterAnchorpointBlock) {
               touchedAnchors.add((BlockPos)c.bePositions.getFirst());
            }

            if (level.getBlockState((BlockPos)c.bePositions.getSecond()).getBlock() instanceof CoasterAnchorpointBlock) {
               touchedAnchors.add((BlockPos)c.bePositions.getSecond());
            }
         }

         Set<String> batchKeys = new HashSet<>();

         for (BezierConnection c : batch) {
            batchKeys.add(edgeKey(c));
         }

         for (BlockPos anchorPos : touchedAnchors) {
            BlockEntity canonicalTangent = level.getBlockEntity(anchorPos);
            if (canonicalTangent instanceof CoasterAnchorpointBlockEntity) {
               CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)canonicalTangent;
               if (!(Math.abs(be.getLiftBlocks()) < 1.0E-9F)) {
                  for (BezierConnection inc : incidentPrimaryCoasterCurves(level, be)) {
                     String k = edgeKey(inc);
                     if (!batchKeys.contains(k)) {
                        batch.add(inc);
                        batchKeys.add(k);
                     }
                  }
               }
            }
         }

         for (BlockPos anchorPosx : touchedAnchors) {
            BlockEntity var20 = level.getBlockEntity(anchorPosx);
            if (var20 instanceof CoasterAnchorpointBlockEntity) {
               CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)var20;
               if (!(Math.abs(be.getLiftBlocks()) < 1.0E-9F)) {
                  Vec3 canonicalTangent = canonicalLiftTangentAtHub(level, anchorPosx, batch);
                  if (canonicalTangent != null) {
                     Vec3 newStart = liftedWorldCenterFromCanonicalTangent(level, anchorPosx, canonicalTangent);

                     for (int i = 0; i < batch.size(); i++) {
                        BezierConnection c = batch.get(i);
                        if (((BlockPos)c.bePositions.getFirst()).equals(anchorPosx) || ((BlockPos)c.bePositions.getSecond()).equals(anchorPosx)) {
                           BezierConnection repinned = CoasterTrackPlacement.curveWithStartAtAnchorEndpoint(level, c, anchorPosx, newStart);
                           CoasterPeerCurveHandleLengths.carryLengthsToReplacement(c, repinned);
                           batch.set(i, repinned);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static Vec3 storedAxisAtEndpoint(BezierConnection bc, BlockPos endpoint) {
      if (((BlockPos)bc.bePositions.getFirst()).equals(endpoint)) {
         return (Vec3)bc.axes.getFirst();
      } else {
         return ((BlockPos)bc.bePositions.getSecond()).equals(endpoint) ? (Vec3)bc.axes.getSecond() : Vec3.ZERO;
      }
   }

   @Nullable
   public static Vec3 tangentIntoFromAnchor(BezierConnection bc, BlockPos anchorPos) {
      if (((BlockPos)bc.bePositions.getFirst()).equals(anchorPos)) {
         Vec3 a = (Vec3)bc.axes.getFirst();
         return a.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : a.normalize();
      } else if (((BlockPos)bc.bePositions.getSecond()).equals(anchorPos)) {
         Vec3 a = (Vec3)bc.axes.getSecond();
         return a.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : a.normalize();
      } else {
         return null;
      }
   }

   public static String edgeKey(BezierConnection bc) {
      long a = ((BlockPos)bc.bePositions.getFirst()).asLong();
      long b = ((BlockPos)bc.bePositions.getSecond()).asLong();
      return a <= b ? a + ":" + b : b + ":" + a;
   }

   @Nullable
   public static BezierConnection fetchPrimary(Level level, BlockPos host, BlockPos remote) {
      if (level.getBlockEntity(host) instanceof CoasterAnchorpointBlockEntity ape) {
         BezierConnection c = ape.getAnchorPeerCurvesView().get(remote);
         return c != null && c.isPrimary() ? c : null;
      } else {
         return null;
      }
   }

   public static void commit(ServerPlayer player, BlockPos primaryHost, BlockPos remoteEnd, int endpointIndex, Vec3 handleOffsetWorld) {
      ServerLevel level = player.serverLevel();
      int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
      CoasterBezierHandleEdit.PreviewResult pr = computePreview(level, primaryHost, remoteEnd, endpointIndex, handleOffsetWorld);
      if (pr.valid() && pr.edited() != null) {
         BlockPos anchorPos = endpointIndex == 0 ? (BlockPos)pr.edited().bePositions.getFirst() : (BlockPos)pr.edited().bePositions.getSecond();
         if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity) {
            if (player.canInteractWithBlock(anchorPos, (double)maxRange)) {
               applyPreviewResult(level, pr);
            }
         }
      } else {
         if (pr.edited() != null) {
            player.displayClientMessage(Component.translatable("simulatedcoasters.track.bezier_handle_radius_invalid"), true);
         }
      }
   }

   private static void applyPreviewResult(ServerLevel level, CoasterBezierHandleEdit.PreviewResult pr) {
      CoasterTrackPropagator.runBatchUpdate(level, () -> {
         Set<String> done = new HashSet<>();

         for (BezierConnection curve : pr.allUpdated()) {
            if (curve.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
               commitOne(level, curve, done);
            }
         }
      });
   }

   private static void commitOne(ServerLevel level, BezierConnection primaryCurve, Set<String> done) {
      String k = edgeKey(primaryCurve);
      if (done.add(k)) {
         CoasterPeerCurveHandleLengths.commitPreviewLengths(primaryCurve);
         BlockPos a = (BlockPos)primaryCurve.bePositions.getFirst();
         BlockPos b = (BlockPos)primaryCurve.bePositions.getSecond();

         for (BlockPos host : List.of(a, b)) {
            if (level.getBlockEntity(host) instanceof CoasterAnchorpointBlockEntity ape) {
               BlockPos peer = host.equals(a) ? b : a;
               if (ape.getAnchorPeerCurvesView().containsKey(peer)) {
                  ape.putAnchorPeerCurve(level, peer, primaryCurve);
                  return;
               }
            }
         }
      }
   }

   public static boolean anchorEligibleForTiltEdit(Level level, BlockPos anchorPos) {
      return level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be ? !incidentPrimaryCoasterCurves(level, be).isEmpty() : false;
   }

   public static List<BezierConnection> incidentPrimaryCoasterCurves(Level level, CoasterAnchorpointBlockEntity be) {
      List<BezierConnection> out = new ArrayList<>();
      BlockPos anchorPos = be.getBlockPos();

      for (BezierConnection c : be.getAnchorPeerCurvesView().values()) {
         if (c != null) {
            BezierConnection p = c.isPrimary() ? c : c.secondary();
            if (p.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
               out.add(p);
            }
         }
      }

      out.sort(Comparator.comparingLong(cx -> {
         BlockPos remote = ((BlockPos)cx.bePositions.getFirst()).equals(anchorPos) ? (BlockPos)cx.bePositions.getSecond() : (BlockPos)cx.bePositions.getFirst();
         return remote.asLong();
      }));
      return out;
   }

   public static List<BezierConnection> incidentCoasterPrimariesAtAnchor(Level level, CoasterAnchorpointBlockEntity be) {
      return incidentPrimaryCoasterCurves(level, be);
   }

   @Nullable
   public static Double computeJointBankRadiansFromVirtualTarget(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
         List<BezierConnection> incident = incidentPrimaryCoasterCurves(level, be);
         if (incident.isEmpty()) {
            return null;
         } else {
            BezierConnection c0 = incident.get(0);
            Vec3 t = tangentIntoFromAnchor(c0, anchorPos);
            if (t == null) {
               return null;
            } else {
               t = t.normalize();
               Vec3 center = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
               Vec3 n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t);
               Vec3 side = jointBankSideForAnchor(level, anchorPos, t, n0);
               if (side == null) {
                  return null;
               } else {
                  Vec3 raw = virtualTargetWorld.subtract(center);
                  Vec3 inPlane = raw.subtract(t.scale(raw.dot(t)));
                  if (inPlane.lengthSqr() < 1.0E-14) {
                     return 0.0;
                  } else {
                     inPlane = inPlane.normalize();
                     return clampJointBankRadians(level, anchorPos, Math.atan2(inPlane.dot(side), inPlane.dot(n0)));
                  }
               }
            }
         }
      } else {
         return null;
      }
   }

   public static double clampJointBankRadians(double thetaRad) {
      return (double)Mth.clamp((float)thetaRad, (float) (-Math.PI / 2), (float) (Math.PI / 2));
   }

   public static double clampJointBankRadians(Level level, BlockPos anchorPos, double thetaRad) {
      return clampJointBankRadiansToNeighborEndpointBanks(level, anchorPos, clampJointBankRadians(thetaRad));
   }

   private static Vec3 storedFaceNormalAtEndpoint(BezierConnection bc, BlockPos endpoint) {
      if (((BlockPos)bc.bePositions.getFirst()).equals(endpoint)) {
         return (Vec3)bc.normals.getFirst();
      } else {
         return ((BlockPos)bc.bePositions.getSecond()).equals(endpoint) ? (Vec3)bc.normals.getSecond() : Vec3.ZERO;
      }
   }

   @Nullable
   static Double jointBankRadiansFromStoredEndpointNormal(Level level, BlockPos anchorPos, BezierConnection bc) {
      Vec3 t = tangentIntoFromAnchor(bc, anchorPos);
      if (t == null) {
         return null;
      } else {
         t = t.normalize();
         Vec3 stored = storedFaceNormalAtEndpoint(bc, anchorPos);
         if (stored.lengthSqr() < 1.0E-12) {
            return null;
         } else {
            stored = stored.normalize();
            Vec3 n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t);
            Vec3 side = jointBankSideForAnchor(level, anchorPos, t, n0);
            if (side == null) {
               return null;
            } else {
               Vec3 inPlane = stored.subtract(t.scale(stored.dot(t)));
               if (inPlane.lengthSqr() < 1.0E-14) {
                  return 0.0;
               } else {
                  inPlane = inPlane.normalize();
                  return Math.atan2(inPlane.dot(side), inPlane.dot(n0));
               }
            }
         }
      }
   }

   private static double clampJointBankRadiansToNeighborEndpointBanks(Level level, BlockPos anchorPos, double thetaRad) {
      if (!(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be)) {
         return thetaRad;
      } else {
         List<BezierConnection> incident = incidentPrimaryCoasterCurves(level, be);
         if (incident.isEmpty()) {
            return thetaRad;
         } else {
            double hardLo = (float) (-Math.PI / 2);
            double hardHi = (float) (Math.PI / 2);
            double allowedLo = hardLo;
            double allowedHi = hardHi;
            double minDot = Math.cos((float) (Math.PI / 2));
            boolean hairpinSharedBank = false;
            Vec3 n0Canon = null;
            Vec3 sideCanon = null;
            if (incident.size() == 2) {
               Vec3 t0 = tangentIntoFromAnchor(incident.get(0), anchorPos);
               Vec3 t1 = tangentIntoFromAnchor(incident.get(1), anchorPos);
               if (t0 != null && t1 != null) {
                  t0 = t0.normalize();
                  t1 = t1.normalize();
                  if (t0.dot(t1) < -0.65) {
                     hairpinSharedBank = true;
                     n0Canon = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t0);
                     sideCanon = jointBankSideForAnchor(level, anchorPos, t0, n0Canon);
                  }
               }
            }

            boolean anyConstraint = false;

            for (BezierConnection c : incident) {
               BlockPos peer = ((BlockPos)c.bePositions.getFirst()).equals(anchorPos)
                  ? (BlockPos)c.bePositions.getSecond()
                  : (BlockPos)c.bePositions.getFirst();
               Vec3 nRef = storedFaceNormalAtEndpoint(c, peer);
               if (!(nRef.lengthSqr() < 1.0E-12)) {
                  nRef = nRef.normalize();
                  Vec3 t = tangentIntoFromAnchor(c, anchorPos);
                  if (t != null) {
                     t = t.normalize();
                     Vec3 n0;
                     Vec3 side;
                     if (hairpinSharedBank && n0Canon != null && sideCanon != null) {
                        n0 = n0Canon;
                        side = sideCanon;
                     } else {
                        n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t);
                        side = jointBankSideForAnchor(level, anchorPos, t, n0);
                     }

                     if (side != null) {
                        double a = n0.dot(nRef);
                        double b = side.dot(nRef);
                        double[] interval = thetaIntervalForBankDotConstraint(a, b, minDot, hardLo, hardHi);
                        if (interval != null) {
                           anyConstraint = true;
                           allowedLo = Math.max(allowedLo, interval[0]);
                           allowedHi = Math.min(allowedHi, interval[1]);
                        }
                     }
                  }
               }
            }

            return anyConstraint && !(allowedLo > allowedHi) ? (double)Mth.clamp((float)thetaRad, (float)allowedLo, (float)allowedHi) : thetaRad;
         }
      }
   }

   @Nullable
   private static double[] thetaIntervalForBankDotConstraint(double a, double b, double minDot, double hardLo, double hardHi) {
      double r = Math.sqrt(a * a + b * b);
      if (r < 1.0E-8) {
         return null;
      } else {
         float ratio = Mth.clamp((float)(minDot / r), -1.0F, 1.0F);
         if (ratio >= 1.0F) {
            return new double[]{hardLo, hardHi};
         } else {
            double half = Math.acos((double)ratio);
            double phi = Math.atan2(b, a);
            return new double[]{Math.max(hardLo, phi - half), Math.min(hardHi, phi + half)};
         }
      }
   }

   public static double snapJointBankRadiansToStep(double thetaRad, double stepRad) {
      return (double)Math.round(thetaRad / stepRad) * stepRad;
   }

   public static double snapJointBankRadiansToNearestTiltSnapStep(double thetaRad) {
      return clampJointBankRadians(snapJointBankRadiansToStep(thetaRad, (float) (Math.PI / 12)));
   }

   public static double snapJointBankRadiansToNearestTiltSnapStep(Level level, BlockPos anchorPos, double thetaRad) {
      return clampJointBankRadians(level, anchorPos, snapJointBankRadiansToStep(thetaRad, (float) (Math.PI / 12)));
   }

   @Nullable
   public static Vec3 tiltVirtualTargetWorldForJointBankRadians(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld, double jointBankRadians) {
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
         List<BezierConnection> incident = incidentPrimaryCoasterCurves(level, be);
         if (incident.isEmpty()) {
            return null;
         } else {
            BezierConnection c0 = incident.get(0);
            Vec3 t = tangentIntoFromAnchor(c0, anchorPos);
            if (t == null) {
               return null;
            } else {
               t = t.normalize();
               Vec3 center = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
               Vec3 n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t);
               Vec3 side = jointBankSideForAnchor(level, anchorPos, t, n0);
               if (side == null) {
                  return null;
               } else {
                  Vec3 raw = virtualTargetWorld.subtract(center);
                  double rawDotT = raw.dot(t);
                  Vec3 inPlane = raw.subtract(t.scale(rawDotT));
                  double magSq = inPlane.lengthSqr();
                  if (magSq < 1.0E-14) {
                     return virtualTargetWorld;
                  } else {
                     double mag = Math.sqrt(magSq);
                     Vec3 dirDesired = bankRotatedFaceNormal(n0, side, jointBankRadians);
                     return center.add(t.scale(rawDotT)).add(dirDesired.scale(mag));
                  }
               }
            }
         }
      } else {
         return null;
      }
   }

   public static Vec3 jointBankFaceNormalAfterGeometric(Level level, BlockPos anchorPos, Vec3 axisIntoCurve, Vec3 geometricFaceNormal, double theta) {
      Vec3 axis = axisIntoCurve.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axisIntoCurve.normalize();
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
         List<BezierConnection> incident = incidentPrimaryCoasterCurves(level, be);
         if (incident.size() == 2) {
            Vec3 t0 = tangentIntoFromAnchor(incident.get(0), anchorPos);
            Vec3 t1 = tangentIntoFromAnchor(incident.get(1), anchorPos);
            if (t0 != null && t1 != null) {
               t0 = t0.normalize();
               t1 = t1.normalize();
               if (t0.dot(t1) < -0.65) {
                  Vec3 n0Canon = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t0);
                  Vec3 sideCanon = jointBankSideForAnchor(level, anchorPos, t0, n0Canon);
                  if (sideCanon != null) {
                     Vec3 nShared = bankRotatedFaceNormal(n0Canon, sideCanon, theta);
                     return CoasterTrackPlacement.orthonormalFaceNormalForAxis(level, anchorPos, nShared, axis);
                  }
               }
            }
         }

         Vec3 side = jointBankSideForAnchor(level, anchorPos, axis, geometricFaceNormal);
         Vec3 n = side == null ? geometricFaceNormal : bankRotatedFaceNormal(geometricFaceNormal, side, theta);
         return CoasterTrackPlacement.orthonormalFaceNormalForAxis(level, anchorPos, n, axis);
      } else {
         return CoasterTrackPlacement.orthonormalFaceNormalForAxis(level, anchorPos, geometricFaceNormal, axis);
      }
   }

   public static CoasterBezierHandleEdit.PreviewResult computeTiltPreview(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
      Double thetaObj = computeJointBankRadiansFromVirtualTarget(level, anchorPos, virtualTargetWorld);
      if (thetaObj == null) {
         return CoasterBezierHandleEdit.PreviewResult.invalid();
      } else {
         double theta = thetaObj;
         if (!(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity)) {
            return CoasterBezierHandleEdit.PreviewResult.invalid();
         } else {
            List<BezierConnection> incident = incidentPrimaryCoasterCurves(level, (CoasterAnchorpointBlockEntity)level.getBlockEntity(anchorPos));
            if (incident.isEmpty()) {
               return CoasterBezierHandleEdit.PreviewResult.invalid();
            } else {
               BezierConnection c0 = incident.get(0);
               Vec3 t0 = tangentIntoFromAnchor(c0, anchorPos);
               if (t0 == null) {
                  return CoasterBezierHandleEdit.PreviewResult.invalid();
               } else {
                  t0 = t0.normalize();
                  Vec3 n0Canonical = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t0);
                  Vec3 sideCanonical = jointBankSideForAnchor(level, anchorPos, t0, n0Canonical);
                  if (sideCanonical == null) {
                     return CoasterBezierHandleEdit.PreviewResult.invalid();
                  } else {
                     Vec3 canonicalLiftDir = bankRotatedFaceNormal(n0Canonical, sideCanonical, theta);
                     float lift = ((CoasterAnchorpointBlockEntity)level.getBlockEntity(anchorPos)).getLiftBlocks();
                     Vec3 baseCenter = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
                     Vec3 newStart = Math.abs(lift) < 1.0E-9F ? baseCenter : baseCenter.add(canonicalLiftDir.scale((double)lift));
                     List<BezierConnection> all = new ArrayList<>();
                     if (incident.size() == 1) {
                        BezierConnection u = CoasterTrackPlacement.curveWithStartAndReferenceNormalAtAnchorEndpoint(
                           level, c0, anchorPos, newStart, canonicalLiftDir
                        );
                        CoasterPeerCurveHandleLengths.copyEdgeLengths(c0, u);
                        all.add(u);
                     } else {
                        BezierConnection c1 = incident.get(1);
                        Vec3 t1 = tangentIntoFromAnchor(c1, anchorPos);
                        if (t1 == null) {
                           return CoasterBezierHandleEdit.PreviewResult.invalid();
                        }

                        t1 = t1.normalize();
                        if (t0.dot(t1) < -0.65) {
                           Vec3 nRef = canonicalLiftDir;

                           for (BezierConnection c : incident) {
                              BezierConnection u = CoasterTrackPlacement.curveWithStartAndReferenceNormalAtAnchorEndpoint(level, c, anchorPos, newStart, nRef);
                              CoasterPeerCurveHandleLengths.copyEdgeLengths(c, u);
                              all.add(u);
                           }
                        } else {
                           for (BezierConnection c : incident) {
                              Vec3 t = tangentIntoFromAnchor(c, anchorPos);
                              if (t == null) {
                                 return CoasterBezierHandleEdit.PreviewResult.invalid();
                              }

                              t = t.normalize();
                              Vec3 n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t);
                              Vec3 side = jointBankSideForAnchor(level, anchorPos, t, n0);
                              if (side == null) {
                                 return CoasterBezierHandleEdit.PreviewResult.invalid();
                              }

                              Vec3 nRef = bankRotatedFaceNormal(n0, side, theta);
                              BezierConnection u = CoasterTrackPlacement.curveWithStartAndReferenceNormalAtAnchorEndpoint(level, c, anchorPos, newStart, nRef);
                              CoasterPeerCurveHandleLengths.copyEdgeLengths(c, u);
                              all.add(u);
                           }
                        }
                     }

                     boolean ok = true;

                     for (BezierConnection c : all) {
                        if (!CoasterTrackPlacement.validateEditedCoasterCurve(level, c, all)) {
                           ok = false;
                           break;
                        }
                     }

                     if (ok && !previewCurvesAtAnchorMeetMinRadius(anchorPos, all)) {
                        ok = false;
                     }

                     return new CoasterBezierHandleEdit.PreviewResult(all.get(0), List.copyOf(all), ok);
                  }
               }
            }
         }
      }
   }

   public static void commitTilt(ServerPlayer player, BlockPos anchorPos, Vec3 virtualTargetWorld) {
      ServerLevel level = player.serverLevel();
      int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
      if (player.canInteractWithBlock(anchorPos, (double)maxRange)) {
         CoasterBezierHandleEdit.PreviewResult pr = computeTiltPreview(level, anchorPos, virtualTargetWorld);
         if (pr.valid() && pr.edited() != null) {
            Double thetaObj = computeJointBankRadiansFromVirtualTarget(level, anchorPos, virtualTargetWorld);
            if (thetaObj != null) {
               if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
                  float var14 = (float)clampJointBankRadians(level, anchorPos, thetaObj);
                  be.setJointBankRadians(var14);
                  List<BezierConnection> refreshed = new ArrayList<>();

                  for (BezierConnection c : incidentPrimaryCoasterCurves(level, be)) {
                     Vec3 axis = storedAxisAtEndpoint(c, anchorPos);
                     BezierConnection r = CoasterTrackPlacement.curveWithAxisAtEndpointForEdit(level, c, anchorPos, axis);
                     CoasterPeerCurveHandleLengths.copyEdgeLengths(c, r);
                     refreshed.add(r);
                  }

                  refreshed.sort(Comparator.comparing(CoasterBezierHandleEdit::edgeKey));
                  boolean ok = true;

                  for (BezierConnection c : refreshed) {
                     if (!CoasterTrackPlacement.validateEditedCoasterCurve(level, c, refreshed)) {
                        ok = false;
                        break;
                     }
                  }

                  if (ok && !previewCurvesAtAnchorMeetMinRadius(anchorPos, refreshed)) {
                     ok = false;
                  }

                  if (!ok) {
                     be.resetJointBank();
                     player.displayClientMessage(Component.translatable("simulatedcoasters.track.bezier_tilt_invalid"), true);
                  } else {
                     applyPreviewResult(level, new CoasterBezierHandleEdit.PreviewResult((BezierConnection)refreshed.get(0), List.copyOf(refreshed), true));
                     displayJointBankDegreesActionBarSuccess(player, (double)be.getJointBankRadians());
                  }
               }
            }
         }
      }
   }

   public static boolean anchorEligibleForLiftEdit(Level level, BlockPos anchorPos) {
      return level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be ? !incidentPrimaryCoasterCurves(level, be).isEmpty() : false;
   }

   @Nullable
   public static Float computeLiftBlocksFromVirtualTarget(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
      if (!(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity)) {
         return null;
      } else {
         Vec3 base = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
         Vec3 dir = CoasterAnchorpointBlockEntity.anchorLiftDirection(level, anchorPos);
         double along = virtualTargetWorld.subtract(base).dot(dir);
         double lift = along - 1.4F;
         if (lift < 0.25) {
            lift = 0.25;
         }

         if (lift > 2.5) {
            lift = 2.5;
         }

         return (float)lift;
      }
   }

   public static float snapLiftBlocksToNearestHalf(float liftBlocks) {
      float clamped = Mth.clamp(liftBlocks, 0.25F, 2.5F);
      return clamped < 0.5F ? 0.25F : Mth.clamp((float)Math.round(clamped * 2.0F) / 2.0F, 0.25F, 2.5F);
   }

   @Nullable
   public static Vec3 liftVirtualTargetWorldForStoredLift(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld, float liftBlocks) {
      if (!(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity)) {
         return null;
      } else {
         Vec3 base = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
         Vec3 dir = CoasterAnchorpointBlockEntity.anchorLiftDirection(level, anchorPos);
         if (dir.lengthSqr() < 1.0E-20) {
            return virtualTargetWorld;
         } else {
            dir = dir.normalize();
            double desiredAlong = (double)(liftBlocks + 1.4F);
            double rawAlong = virtualTargetWorld.subtract(base).dot(dir);
            return virtualTargetWorld.add(dir.scale(desiredAlong - rawAlong));
         }
      }
   }

   public static CoasterBezierHandleEdit.PreviewResult computeLiftPreview(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
      Float liftObj = computeLiftBlocksFromVirtualTarget(level, anchorPos, virtualTargetWorld);
      if (liftObj == null) {
         return CoasterBezierHandleEdit.PreviewResult.invalid();
      } else if (!(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be)) {
         return CoasterBezierHandleEdit.PreviewResult.invalid();
      } else {
         List<BezierConnection> incident = incidentPrimaryCoasterCurves(level, be);
         if (incident.isEmpty()) {
            return CoasterBezierHandleEdit.PreviewResult.invalid();
         } else {
            Vec3 newStart = CoasterAnchorpointBlockEntity.worldCenterWithLift(level, anchorPos, liftObj);
            List<BezierConnection> all = new ArrayList<>();

            for (BezierConnection c : incident) {
               Vec3 axis = storedAxisAtEndpoint(c, anchorPos);
               BezierConnection u = CoasterTrackPlacement.curveWithStartAndAxisAtEndpointForEdit(level, c, anchorPos, newStart, axis);
               CoasterPeerCurveHandleLengths.copyEdgeLengths(c, u);
               all.add(u);
            }

            boolean ok = true;

            for (BezierConnection c : all) {
               if (!CoasterTrackPlacement.validateEditedCoasterCurve(level, c, all)) {
                  ok = false;
                  break;
               }
            }

            if (ok && !previewCurvesAtAnchorMeetMinRadius(anchorPos, all)) {
               ok = false;
            }

            return new CoasterBezierHandleEdit.PreviewResult(all.get(0), List.copyOf(all), ok);
         }
      }
   }

   public static void commitLift(ServerPlayer player, BlockPos anchorPos, Vec3 virtualTargetWorld) {
      ServerLevel level = player.serverLevel();
      int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
      if (player.canInteractWithBlock(anchorPos, (double)maxRange)) {
         if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
            Float liftObj = computeLiftBlocksFromVirtualTarget(level, anchorPos, virtualTargetWorld);
            if (liftObj != null) {
               float oldStored = be.getStoredLiftBlocks();
               float newLift = liftObj;
               be.setLiftBlocks(newLift);
               List<BezierConnection> refreshed = new ArrayList<>();

               for (BezierConnection c : incidentPrimaryCoasterCurves(level, be)) {
                  Vec3 axis = storedAxisAtEndpoint(c, anchorPos);
                  BezierConnection r = CoasterTrackPlacement.curveWithAxisAtEndpointForEdit(level, c, anchorPos, axis);
                  CoasterPeerCurveHandleLengths.copyEdgeLengths(c, r);
                  refreshed.add(r);
               }

               refreshed.sort(Comparator.comparing(CoasterBezierHandleEdit::edgeKey));
               boolean ok = true;

               for (BezierConnection c : refreshed) {
                  if (!CoasterTrackPlacement.validateEditedCoasterCurve(level, c, refreshed)) {
                     ok = false;
                     break;
                  }
               }

               if (ok && !previewCurvesAtAnchorMeetMinRadius(anchorPos, refreshed)) {
                  ok = false;
               }

               if (ok && !refreshed.isEmpty()) {
                  applyPreviewResult(level, new CoasterBezierHandleEdit.PreviewResult(refreshed.get(0), List.copyOf(refreshed), true));
                  displayLiftMetersActionBarSuccess(player, be.getLiftBlocks());
               } else {
                  if (Float.isNaN(oldStored)) {
                     be.clearLift();
                  } else {
                     be.setLiftBlocks(oldStored);
                  }

                  player.displayClientMessage(Component.translatable("simulatedcoasters.track.bezier_lift_invalid"), true);
               }
            }
         }
      }
   }

   private static boolean previewCurvesAtAnchorMeetMinRadius(BlockPos anchor, List<BezierConnection> previewBatch) {
      boolean anyAtAnchor = false;

      for (BezierConnection c : previewBatch) {
         if (((BlockPos)c.bePositions.getFirst()).equals(anchor) || ((BlockPos)c.bePositions.getSecond()).equals(anchor)) {
            anyAtAnchor = true;
            double h0 = CoasterPeerCurveHandleLengths.getLengthAtEndpoint(c, 0);
            double h1 = CoasterPeerCurveHandleLengths.getLengthAtEndpoint(c, 1);
            if (!CoasterAnchorBezierOptimizer.meetsMinCurveRadiusWithHandleLengths(c, h0, h1)) {
               return false;
            }
         }
      }

      return anyAtAnchor;
   }

   public static MutableComponent liftMetersComponent(float liftBlocks) {
      return Component.translatable("simulatedcoasters.track.bezier_edit_lift_meters", new Object[]{formatLiftMetersReadout(liftBlocks)});
   }

   public static MutableComponent jointBankDegreesComponent(double jointBankRadians) {
      return Component.translatable("simulatedcoasters.track.bezier_edit_tilt_degrees", new Object[]{formatJointBankDegreesReadout(jointBankRadians)});
   }

   public static String formatLiftMetersReadout(float blocks) {
      String s = String.format(Locale.ROOT, "%.2f", blocks);
      int dot = s.indexOf(46);
      int end = s.length();

      while (end > dot + 2 && s.charAt(end - 1) == '0') {
         end--;
      }

      return s.substring(0, end);
   }

   public static String formatJointBankDegreesReadout(double jointBankRadians) {
      double deg = jointBankRadians * 180.0F / (float)Math.PI;
      if (Math.abs(deg) < 0.02) {
         return "0";
      } else {
         String s = String.format(Locale.ROOT, "%.2f", deg);
         int dot = s.indexOf(46);
         int end = s.length();

         while (end > dot + 2 && s.charAt(end - 1) == '0') {
            end--;
         }

         return s.substring(0, end);
      }
   }

   private static void displayJointBankDegreesActionBarSuccess(ServerPlayer player, double jointBankRadians) {
      player.displayClientMessage(jointBankDegreesComponent(jointBankRadians).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(16768341))), true);
   }

   private static void displayLiftMetersActionBarSuccess(ServerPlayer player, float liftBlocks) {
      player.displayClientMessage(liftMetersComponent(liftBlocks).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(16768341))), true);
   }

   public static boolean anchorEligibleForOpenEndExtensionEdit(Level level, BlockPos anchorPos) {
      return CoasterOpenEndExtension.isOpenEndAnchor(level, anchorPos);
   }

   @Nullable
   public static Float computeOpenEndExtensionFromVirtualTarget(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld, boolean applyCenterSnap) {
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
         List<BezierConnection> incident = incidentPrimaryCoasterCurves(level, be);
         if (incident.size() != 1) {
            return null;
         } else {
            BezierConnection bc = incident.get(0);
            return CoasterOpenEndExtension.extensionBlocksFromVirtualTarget(level, anchorPos, bc, virtualTargetWorld, applyCenterSnap);
         }
      } else {
         return null;
      }
   }

   @Nullable
   public static Float computeOpenEndExtensionFromVirtualTarget(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
      return computeOpenEndExtensionFromVirtualTarget(level, anchorPos, virtualTargetWorld, true);
   }

   public static CoasterBezierHandleEdit.OpenEndExtensionPreviewResult computeOpenEndExtensionPreview(
      Level level, BlockPos anchorPos, Vec3 virtualTargetWorld, boolean applyCenterSnap
   ) {
      Float ext = computeOpenEndExtensionFromVirtualTarget(level, anchorPos, virtualTargetWorld, applyCenterSnap);
      return ext == null
         ? CoasterBezierHandleEdit.OpenEndExtensionPreviewResult.invalid()
         : new CoasterBezierHandleEdit.OpenEndExtensionPreviewResult(ext, true);
   }

   public static CoasterBezierHandleEdit.OpenEndExtensionPreviewResult computeOpenEndExtensionPreview(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
      return computeOpenEndExtensionPreview(level, anchorPos, virtualTargetWorld, true);
   }

   public static void commitOpenEndExtension(ServerPlayer player, BlockPos anchorPos, Vec3 virtualTargetWorld) {
      ServerLevel level = player.serverLevel();
      int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
      if (player.canInteractWithBlock(anchorPos, (double)maxRange)) {
         CoasterBezierHandleEdit.OpenEndExtensionPreviewResult pr = computeOpenEndExtensionPreview(level, anchorPos, virtualTargetWorld, false);
         if (pr.valid()) {
            if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be) {
               if (be.legCount() == 1) {
                  be.setOpenEndExtensionBlocks(pr.extensionBlocks());
                  CoasterTrackPropagator.touch(level, List.of(anchorPos));
                  VisualizationHelper.queueUpdate(be);
                  displayOpenEndExtensionActionBarSuccess(player, pr.extensionBlocks());
               }
            }
         }
      }
   }

   public static MutableComponent openEndExtensionBlocksComponent(float blocks) {
      return Component.literal(formatOpenEndExtensionReadout(blocks));
   }

   public static String formatOpenEndExtensionReadout(float blocks) {
      return formatLiftMetersReadout(blocks) + " m";
   }

   private static void displayOpenEndExtensionActionBarSuccess(ServerPlayer player, float extensionBlocks) {
      player.displayClientMessage(openEndExtensionBlocksComponent(extensionBlocks).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(16768341))), true);
   }

   private static record ChordGuideAxes(Vec3 u, Vec3 nTall, Vec3 nFlat) {
   }

   private static record HandleAxisClampSpec(Vec3 reference, float maxAwayRadians) {
   }

   public static record OpenEndExtensionPreviewResult(float extensionBlocks, boolean valid) {
      public static CoasterBezierHandleEdit.OpenEndExtensionPreviewResult invalid() {
         return new CoasterBezierHandleEdit.OpenEndExtensionPreviewResult(0.5F, false);
      }
   }

   public static record PreviewResult(BezierConnection edited, List<BezierConnection> allUpdated, boolean valid) {
      public static CoasterBezierHandleEdit.PreviewResult invalid() {
         return new CoasterBezierHandleEdit.PreviewResult(null, List.of(), false);
      }

      public List<BezierConnection> curvesIncidentTo(BlockPos anchor) {
         if (anchor != null && !this.allUpdated.isEmpty()) {
            List<BezierConnection> out = new ArrayList<>(2);

            for (BezierConnection c : this.allUpdated) {
               if (((BlockPos)c.bePositions.getFirst()).equals(anchor) || ((BlockPos)c.bePositions.getSecond()).equals(anchor)) {
                  out.add(c);
               }
            }

            return List.copyOf(out);
         } else {
            return List.of();
         }
      }
   }

   public static record TangentGuideSnapResult(Vec3 snappedVirtualTarget, int guideAlignmentAxes) {
   }
}
