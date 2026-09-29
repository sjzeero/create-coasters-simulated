package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.BezierConnection.Segment;
import dev.engine_room.flywheel.lib.instance.ColoredLitOverlayInstance;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.track.bent.BentRailInstance;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterCurveTintColors;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterCurveRailInstancing {
   private CoasterCurveRailInstancing() {
   }

   public static boolean useSplineRails(BezierConnection bc) {
      return bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id);
   }

   public static int curveTieGapCount(BezierConnection bc) {
      return curveTieGapCount(curveSpanLengthBlocks(bc));
   }

   static int curveTieGapCount(double spanLengthBlocks) {
      return spanLengthBlocks < 1.0E-8 ? 1 : Math.max(1, (int)Math.round(spanLengthBlocks / 1.0));
   }

   public static int curveTieInstanceCount(BezierConnection bc) {
      return curveTieGapCount(bc) + 1;
   }

   private static double curveSpanLengthBlocks(BezierConnection bc) {
      return bc.getLength();
   }

   public static int openEndRailExtensionSlotCount(@Nullable Level level, BezierConnection bc) {
      if (useSplineRails(bc) && level != null) {
         int slots = 0;
         if (isOpenCurveEnd(level, bc, true)) {
            slots++;
         }

         if (isOpenCurveEnd(level, bc, false)) {
            slots++;
         }

         return slots;
      } else {
         return 0;
      }
   }

   public static boolean isOpenCurveEnd(Level level, BezierConnection bc, boolean atFirstEndpoint) {
      if (!useSplineRails(bc)) {
         return false;
      } else {
         BlockPos anchor = atFirstEndpoint ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
         return level.getBlockEntity(anchor) instanceof CoasterAnchorpointBlockEntity be ? be.legCount() < 2 : false;
      }
   }

   public static void apply(
      Vec3 visualPos,
      BezierConnection bc,
      TransformedInstance[] ties,
      @Nullable BentRailInstance[] bentLeft,
      @Nullable BentRailInstance[] bentRight,
      @Nullable BentRailInstance[] bentCenterBeam,
      int railDiffuseRgbPacked,
      int beamDiffuseRgbPacked,
      @Nullable Level level
   ) {
      double railHalf = 0.772F;
      float modelAnchorY = 0.24609375F;
      float centerBeamAnchorY = modelAnchorY + -0.4375F;
      Vec3 runtimeWorldYOffset = new Vec3(0.0, -0.1875, 0.0);
      int rawRail = railDiffuseRgbPacked & 16777215;
      int rawBeam = beamDiffuseRgbPacked & 16777215;
      int railMul = rawRail == 0 ? 0 : CoasterCurveTintColors.vertexColorMultiplierRgb(rawRail);
      int beamMul = rawBeam == 0 ? 0 : CoasterCurveTintColors.beamVertexColorMultiplierRgb(rawBeam);
      PoseStack rootPose = new PoseStack();
      TransformStack.of(rootPose).translate(visualPos.add(runtimeWorldYOffset));
      List<CoasterCurveRailInstancing.SpineSample> spineSamples = new ArrayList<>();
      Vec3 prevPos = null;
      Vec3 prevTangent = null;
      Vec3 prevLateral = null;
      Vec3 firstPos = null;
      Vec3 firstTangent = null;
      Vec3 firstLateral = null;
      Vec3 lastPos = null;
      Vec3 lastTangent = null;
      Vec3 lastLateral = null;
      double arcLength = 0.0;
      int modelIndex = 0;

      for (Segment segment : bc) {
         Vec3 currPos = segment.position;
         Vec3 currTangent = segment.derivative;
         float segT = bc.getSegmentT(segment.index);
         Vec3 currLateral = CoasterBezierRailFrames.lateralAt(bc, segT, currTangent, level);
         if (prevLateral != null && currLateral.dot(prevLateral) < 0.0) {
            currLateral = currLateral.scale(-1.0);
         }

         if (prevPos == null) {
            firstPos = currPos;
            firstTangent = currTangent;
            firstLateral = currLateral;
            spineSamples.add(new CoasterCurveRailInstancing.SpineSample(currPos, currTangent, currLateral, segT, 0.0));
            prevPos = currPos;
            prevTangent = currTangent;
            prevLateral = currLateral;
         } else {
            arcLength += currPos.distanceTo(prevPos);
            if (bentLeft != null && modelIndex < bentLeft.length) {
               Vec3 prevSpine = visualPos.add(prevPos).add(prevLateral.scale(railHalf)).add(runtimeWorldYOffset);
               Vec3 currSpine = visualPos.add(currPos).add(currLateral.scale(railHalf)).add(runtimeWorldYOffset);
               bentLeft[modelIndex].setSegment(prevSpine, currSpine, prevTangent, currTangent, prevLateral, currLateral).setModelAnchorY(modelAnchorY);
               if (railMul != 0) {
                  applyRgbMultiplier(bentLeft[modelIndex], railMul);
               }

               bentLeft[modelIndex].setChanged();
            }

            if (bentRight != null && modelIndex < bentRight.length) {
               Vec3 prevSpine = visualPos.add(prevPos).subtract(prevLateral.scale(railHalf)).add(runtimeWorldYOffset);
               Vec3 currSpine = visualPos.add(currPos).subtract(currLateral.scale(railHalf)).add(runtimeWorldYOffset);
               bentRight[modelIndex].setSegment(prevSpine, currSpine, prevTangent, currTangent, prevLateral, currLateral).setModelAnchorY(modelAnchorY);
               if (railMul != 0) {
                  applyRgbMultiplier(bentRight[modelIndex], railMul);
               }

               bentRight[modelIndex].setChanged();
            }

            if (bentCenterBeam != null && modelIndex < bentCenterBeam.length) {
               Vec3 prevSpine = visualPos.add(prevPos).add(runtimeWorldYOffset);
               Vec3 currSpine = visualPos.add(currPos).add(runtimeWorldYOffset);
               bentCenterBeam[modelIndex]
                  .setSegment(prevSpine, currSpine, prevTangent, currTangent, prevLateral, currLateral)
                  .setModelAnchorY(centerBeamAnchorY);
               if (beamMul != 0) {
                  applyRgbMultiplier(bentCenterBeam[modelIndex], beamMul);
               }

               bentCenterBeam[modelIndex].setChanged();
            }

            spineSamples.add(new CoasterCurveRailInstancing.SpineSample(currPos, currTangent, currLateral, segT, arcLength));
            lastPos = currPos;
            lastTangent = currTangent;
            lastLateral = currLateral;
            prevPos = currPos;
            prevTangent = currTangent;
            prevLateral = currLateral;
            modelIndex++;
         }
      }

      int extSlot = modelIndex;
      if (level != null && firstPos != null && firstTangent != null && firstLateral != null && isOpenCurveEnd(level, bc, true)) {
         float ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, true);
         extSlot = modelIndex + 1;
         applyOpenEndRailExtension(
            bentLeft, bentRight, modelIndex, visualPos, runtimeWorldYOffset, railHalf, modelAnchorY, railMul, firstPos, firstTangent, firstLateral, true, ext
         );
      }

      if (level != null && lastPos != null && lastTangent != null && lastLateral != null && isOpenCurveEnd(level, bc, false)) {
         float ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, false);
         applyOpenEndRailExtension(
            bentLeft, bentRight, extSlot++, visualPos, runtimeWorldYOffset, railHalf, modelAnchorY, railMul, lastPos, lastTangent, lastLateral, false, ext
         );
      }

      zeroUnusedBentRails(bentLeft, extSlot, railMul);
      zeroUnusedBentRails(bentRight, extSlot, railMul);
      if (firstPos != null
         && firstTangent != null
         && firstLateral != null
         && lastPos != null
         && lastTangent != null
         && lastLateral != null
         && !spineSamples.isEmpty()) {
         applyCurveTies(ties, rootPose, bc, level, beamMul, spineSamples, firstPos, firstTangent, firstLateral, lastPos, lastTangent, lastLateral);
      }
   }

   private static void applyCurveTies(
      TransformedInstance[] ties,
      PoseStack rootPose,
      BezierConnection bc,
      @Nullable Level level,
      int beamMul,
      List<CoasterCurveRailInstancing.SpineSample> spineSamples,
      Vec3 firstPos,
      Vec3 firstTangent,
      Vec3 firstLateral,
      Vec3 lastPos,
      Vec3 lastTangent,
      Vec3 lastLateral
   ) {
      double span = spineSamples.getLast().arcLength();
      int gapCount = curveTieGapCount(span);
      int tieCount = gapCount + 1;
      boolean openStart = level != null && isOpenCurveEnd(level, bc, true);
      boolean openEnd = level != null && isOpenCurveEnd(level, bc, false);
      placeEndpointTie(ties, 0, rootPose, bc, level, beamMul, firstPos, firstTangent, firstLateral, openStart, true);

      for (int k = 1; k < gapCount; k++) {
         double arc = span * (double)k / (double)gapCount;
         CoasterCurveRailInstancing.SpineSample sample = sampleAtArcLength(spineSamples, arc);
         placeMidspanTie(ties, k, rootPose, beamMul, sample);
      }

      placeEndpointTie(ties, tieCount - 1, rootPose, bc, level, beamMul, lastPos, lastTangent, lastLateral, openEnd, false);

      for (int i = tieCount; i < ties.length; i++) {
         zeroTie(ties[i], beamMul);
      }
   }

   private static CoasterCurveRailInstancing.SpineSample sampleAtArcLength(List<CoasterCurveRailInstancing.SpineSample> samples, double arc) {
      if (samples.size() == 1) {
         return samples.getFirst();
      } else {
         arc = Mth.clamp(arc, 0.0, samples.getLast().arcLength());

         for (int i = 1; i < samples.size(); i++) {
            CoasterCurveRailInstancing.SpineSample prev = samples.get(i - 1);
            CoasterCurveRailInstancing.SpineSample curr = samples.get(i);
            if (arc <= curr.arcLength + 1.0E-9) {
               double segLen = curr.arcLength - prev.arcLength;
               double f = segLen < 1.0E-12 ? 0.0 : (arc - prev.arcLength) / segLen;
               Vec3 pos = prev.pos.lerp(curr.pos, f);
               Vec3 tangent = prev.tangent.lerp(curr.tangent, f);
               Vec3 lateral = prev.lateral.lerp(curr.lateral, f);
               if (lateral.dot(prev.lateral) < 0.0) {
                  lateral = lateral.scale(-1.0);
               }

               if (tangent.lengthSqr() < 1.0E-14) {
                  tangent = curr.tangent;
               } else {
                  tangent = tangent.normalize();
               }

               if (lateral.lengthSqr() < 1.0E-14) {
                  lateral = curr.lateral;
               } else {
                  lateral = lateral.normalize();
               }

               float segT = Mth.lerp((float)f, prev.segT, curr.segT);
               return new CoasterCurveRailInstancing.SpineSample(pos, tangent, lateral, segT, arc);
            }
         }

         return samples.getLast();
      }
   }

   private static void placeMidspanTie(TransformedInstance[] ties, int slot, PoseStack rootPose, int beamMul, CoasterCurveRailInstancing.SpineSample sample) {
      if (slot >= 0 && slot < ties.length) {
         PoseStack tieStack = CoasterTrackAnchorJunctionPlate.midspanTiePoseBelowOrigin(sample.pos, sample.lateral, sample.tangent);
         ties[slot].setTransform(rootPose).mul(tieStack.last()).setChanged();
         if (beamMul != 0) {
            TransformedInstance var7 = ties[slot];
            if (var7 instanceof ColoredLitOverlayInstance) {
               applyRgbMultiplier(var7, beamMul);
            }
         }
      }
   }

   private static void placeEndpointTie(
      TransformedInstance[] ties,
      int slot,
      PoseStack rootPose,
      BezierConnection bc,
      @Nullable Level level,
      int beamMul,
      Vec3 endpointPos,
      Vec3 endpointTangent,
      Vec3 endpointLateral,
      boolean openEnd,
      boolean atStart
   ) {
      if (slot >= 0 && slot < ties.length) {
         if (level != null) {
            BlockPos anchor = atStart ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
            if (!openEnd && !CoasterTrackAnchorJunctionPlate.shouldRenderJunctionTieAt(level, anchor, bc)) {
               zeroTie(ties[slot], beamMul);
               return;
            }

            CoasterTrackAnchorJunctionPlate.EndpointSpine frame = CoasterTrackAnchorJunctionPlate.spineAtEndpoint(bc, anchor, level);
            if (frame != null) {
               PoseStack tieStack = openEnd
                  ? CoasterTrackAnchorJunctionPlate.openEndTiePoseBelowOrigin(frame)
                  : CoasterTrackAnchorJunctionPlate.junctionTiePoseBelowOrigin(frame);
               ties[slot].setTransform(rootPose).mul(tieStack.last()).setChanged();
               if (beamMul != 0) {
                  TransformedInstance var15 = ties[slot];
                  if (var15 instanceof ColoredLitOverlayInstance) {
                     applyRgbMultiplier(var15, beamMul);
                  }
               }

               return;
            }
         }

         PoseStack tieStack = CoasterTrackAnchorJunctionPlate.midspanTiePoseBelowOrigin(endpointPos, endpointLateral, endpointTangent);
         ties[slot].setTransform(rootPose).mul(tieStack.last()).setChanged();
         if (beamMul != 0) {
            TransformedInstance tieStackx = ties[slot];
            if (tieStackx instanceof ColoredLitOverlayInstance) {
               applyRgbMultiplier(tieStackx, beamMul);
            }
         }
      }
   }

   private static void zeroTie(TransformedInstance tie, int beamMul) {
      tie.setZeroTransform();
      if (beamMul != 0 && tie instanceof ColoredLitOverlayInstance) {
         applyRgbMultiplier(tie, beamMul);
      }

      tie.setChanged();
   }

   private static void applyOpenEndRailExtension(
      @Nullable BentRailInstance[] bentLeft,
      @Nullable BentRailInstance[] bentRight,
      int slot,
      Vec3 visualPos,
      Vec3 runtimeWorldYOffset,
      double railHalf,
      float modelAnchorY,
      int railMul,
      Vec3 endpointPos,
      Vec3 endpointTangent,
      Vec3 endpointLateral,
      boolean atStart,
      float extensionBlocks
   ) {
      Vec3 tan = endpointTangent.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : endpointTangent.normalize();
      double extLen = (double)extensionBlocks;
      Vec3 lat = endpointLateral.lengthSqr() < 1.0E-14 ? new Vec3(1.0, 0.0, 0.0) : endpointLateral.normalize();
      Vec3 junctionLeft = visualPos.add(endpointPos).add(lat.scale(railHalf)).add(runtimeWorldYOffset);
      Vec3 junctionRight = visualPos.add(endpointPos).subtract(lat.scale(railHalf)).add(runtimeWorldYOffset);
      Vec3 tipLeft = atStart ? junctionLeft.subtract(tan.scale(extLen)) : junctionLeft.add(tan.scale(extLen));
      Vec3 tipRight = atStart ? junctionRight.subtract(tan.scale(extLen)) : junctionRight.add(tan.scale(extLen));
      Vec3 prevLeft = atStart ? tipLeft : junctionLeft;
      Vec3 currLeft = atStart ? junctionLeft : tipLeft;
      Vec3 prevRight = atStart ? tipRight : junctionRight;
      Vec3 currRight = atStart ? junctionRight : tipRight;
      if (bentLeft != null && slot < bentLeft.length) {
         bentLeft[slot].setSegment(prevLeft, currLeft, tan, tan, lat, lat).setModelAnchorY(modelAnchorY);
         if (railMul != 0) {
            applyRgbMultiplier(bentLeft[slot], railMul);
         }

         bentLeft[slot].setChanged();
      }

      if (bentRight != null && slot < bentRight.length) {
         bentRight[slot].setSegment(prevRight, currRight, tan, tan, lat, lat).setModelAnchorY(modelAnchorY);
         if (railMul != 0) {
            applyRgbMultiplier(bentRight[slot], railMul);
         }

         bentRight[slot].setChanged();
      }
   }

   private static void zeroUnusedBentRails(@Nullable BentRailInstance[] rails, int fromIndex, int railMul) {
      if (rails != null) {
         for (int i = fromIndex; i < rails.length; i++) {
            rails[i].setZeroTransform();
            if (railMul != 0) {
               applyRgbMultiplier(rails[i], railMul);
            }

            rails[i].setChanged();
         }
      }
   }

   private static void applyRgbMultiplier(ColoredLitOverlayInstance inst, int rgbPacked) {
      inst.red = (byte)(rgbPacked >> 16 & 0xFF);
      inst.green = (byte)(rgbPacked >> 8 & 0xFF);
      inst.blue = (byte)(rgbPacked & 0xFF);
      inst.alpha = -1;
   }

   private static record SpineSample(Vec3 pos, Vec3 tangent, Vec3 lateral, float segT, double arcLength) {
   }
}
