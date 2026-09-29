package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackRenderer;
import com.simibubi.create.content.trains.track.BezierConnection.GirderAngles;
import com.simibubi.create.content.trains.track.BezierConnection.Segment;
import com.simibubi.create.content.trains.track.BezierConnection.SegmentAngles;
import com.simibubi.create.content.trains.track.TrackMaterial.TrackModelHolder;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterCurveTintColors;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class AnchorPeerTrackCurveBerRender {
   private AnchorPeerTrackCurveBerRender() {
   }

   public static void render(CoasterAnchorpointBlockEntity anchor, PoseStack ms, MultiBufferSource buffer) {
      Level level = anchor.getLevel();
      if (level != null && !VisualizationManager.supportsVisualization(level)) {
         VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());

         for (Entry<BlockPos, BezierConnection> e : anchor.getAnchorPeerCurvesView().entrySet()) {
            BezierConnection bc = e.getValue();
            if (bc.isPrimary()) {
               if (CoasterTrackMaterials.COASTER.id.equals(bc.getMaterial().id)) {
                  renderCoasterCurve(level, bc, ms, vb, AnchorPeerTrackCurveBerRender.CurveBerMaterials.from(anchor, e.getKey()));
               } else {
                  TrackRenderer.renderBezierTurn(level, bc, ms, vb);
               }
            }
         }
      }
   }

   private static void renderCoasterCurve(
      Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb, AnchorPeerTrackCurveBerRender.CurveBerMaterials materials
   ) {
      ms.pushPose();
      BlockPos bePosition = (BlockPos)bc.bePositions.getFirst();
      BlockState air = Blocks.AIR.defaultBlockState();
      SegmentAngles segments = bc.getBakedSegments();

      for (int i = 1; i < segments.length; i++) {
         int light = LevelRenderer.getLightColor(level, segments.lightPosition[i].offset(bePosition));

         for (boolean first : Iterate.trueAndFalse) {
            Pose transform = (Pose)segments.railTransforms[i].get(first);
            renderPartial(ms, vb, air, first ? materials.leftRail() : materials.rightRail(), transform, light, materials.railColorMul());
         }
      }

      AnchorPeerTrackCurveBerRender.SpineWalk walk = collectSpineWalk(level, bc);
      renderOpenEndRailExtensions(level, bc, ms, vb, bePosition, air, materials, walk);
      renderGirder(level, bc, ms, vb, bePosition);
      renderJunctionAttachments(level, bc, ms, vb, bePosition);
      renderCoasterCenterBeams(level, bc, ms, vb, bePosition, air, materials);
      renderCoasterTies(level, bc, ms, vb, bePosition, air, materials, walk);
      ms.popPose();
   }

   private static void renderPartial(PoseStack ms, VertexConsumer vb, BlockState air, PartialModel partial, Pose transform, int light, int colorMul) {
      SuperByteBuffer buffer = ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(partial, air).mulPose(transform.pose())).mulNormal(transform.normal()))
         .light(light);
      if (colorMul != 0) {
         buffer.color(0xFF000000 | colorMul & 16777215);
      }

      buffer.renderInto(ms, vb);
   }

   private static void renderCoasterCenterBeams(
      Level level,
      BezierConnection bc,
      PoseStack ms,
      VertexConsumer vb,
      BlockPos bePosition,
      BlockState air,
      AnchorPeerTrackCurveBerRender.CurveBerMaterials materials
   ) {
      int segmentCount = bc.getSegmentCount();
      double railHalf = coasterRailHalfGauge();
      Couple<Vec3> previousOffsets = null;

      for (Segment segment : bc) {
         int i = segment.index;
         boolean end = i == 0 || i == segmentCount;
         Couple<Vec3> railOffsets = Couple.create(
            segment.position.add(segment.normal.scale(railHalf)), segment.position.subtract(segment.normal.scale(railHalf))
         );
         Vec3 railMiddle = ((Vec3)railOffsets.getFirst()).add((Vec3)railOffsets.getSecond()).scale(0.5);
         if (previousOffsets == null) {
            previousOffsets = railOffsets;
         } else {
            Vec3 prevMiddle = ((Vec3)previousOffsets.getFirst()).add((Vec3)previousOffsets.getSecond()).scale(0.5);
            Vec3 diff = railMiddle.subtract(prevMiddle);
            if (diff.lengthSqr() > 1.0E-20) {
               int light = LevelRenderer.getLightColor(level, BlockPos.containing(railMiddle).offset(bePosition));
               renderScaledRailMesh(
                  ms, vb, air, materials.centerBeam(), prevMiddle, diff, segment.normal, end, coasterCenterBeamModelAnchorY(), light, materials.beamColorMul()
               );
            }

            previousOffsets = railOffsets;
         }
      }
   }

   private static void renderJunctionAttachments(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb, BlockPos curveHostPos) {
      CoasterTrackAnchorJunctionPlate.renderBerPlate(level, bc, (BlockPos)bc.bePositions.getFirst(), ms, vb, curveHostPos);
      CoasterTrackAnchorJunctionPlate.renderBerPlate(level, bc, (BlockPos)bc.bePositions.getSecond(), ms, vb, curveHostPos);
      CoasterTrackAnchorGirderBeam.renderBerGirders(level, bc, (BlockPos)bc.bePositions.getFirst(), curveHostPos, ms, vb);
      CoasterTrackAnchorGirderBeam.renderBerGirders(level, bc, (BlockPos)bc.bePositions.getSecond(), curveHostPos, ms, vb);
   }

   private static void renderOpenEndRailExtensions(
      Level level,
      BezierConnection bc,
      PoseStack ms,
      VertexConsumer vb,
      BlockPos bePosition,
      BlockState air,
      AnchorPeerTrackCurveBerRender.CurveBerMaterials materials,
      AnchorPeerTrackCurveBerRender.SpineWalk walk
   ) {
      if (walk.firstPos != null && walk.firstTangent != null && walk.firstLateral != null) {
         double railHalf = coasterRailHalfGauge();
         if (CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, true)) {
            float ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, true);
            renderOpenEndRailExtension(level, ms, vb, bePosition, air, materials, walk.firstPos, walk.firstTangent, walk.firstLateral, true, ext, railHalf);
         }

         if (walk.lastPos != null && walk.lastTangent != null && walk.lastLateral != null && CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, false)) {
            float ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, false);
            renderOpenEndRailExtension(level, ms, vb, bePosition, air, materials, walk.lastPos, walk.lastTangent, walk.lastLateral, false, ext, railHalf);
         }
      }
   }

   private static void renderOpenEndRailExtension(
      Level level,
      PoseStack ms,
      VertexConsumer vb,
      BlockPos bePosition,
      BlockState air,
      AnchorPeerTrackCurveBerRender.CurveBerMaterials materials,
      Vec3 endpointPos,
      Vec3 endpointTangent,
      Vec3 endpointLateral,
      boolean atStart,
      float extensionBlocks,
      double railHalf
   ) {
      Vec3 tan = endpointTangent.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : endpointTangent.normalize();
      Vec3 lat = endpointLateral.lengthSqr() < 1.0E-14 ? new Vec3(1.0, 0.0, 0.0) : endpointLateral.normalize();
      Vec3 junctionLeft = endpointPos.add(lat.scale(railHalf));
      Vec3 junctionRight = endpointPos.subtract(lat.scale(railHalf));
      Vec3 tipLeft = atStart ? junctionLeft.subtract(tan.scale((double)extensionBlocks)) : junctionLeft.add(tan.scale((double)extensionBlocks));
      Vec3 tipRight = atStart ? junctionRight.subtract(tan.scale((double)extensionBlocks)) : junctionRight.add(tan.scale((double)extensionBlocks));
      Vec3 prevLeft = atStart ? tipLeft : junctionLeft;
      Vec3 currLeft = atStart ? junctionLeft : tipLeft;
      Vec3 prevRight = atStart ? tipRight : junctionRight;
      Vec3 currRight = atStart ? junctionRight : tipRight;
      int light = LevelRenderer.getLightColor(level, BlockPos.containing(junctionLeft).offset(bePosition));
      float anchorY = coasterRailModelAnchorY();
      renderScaledRailMesh(ms, vb, air, materials.leftRail(), prevLeft, currLeft.subtract(prevLeft), lat, true, anchorY, light, materials.railColorMul());
      renderScaledRailMesh(ms, vb, air, materials.rightRail(), prevRight, currRight.subtract(prevRight), lat, true, anchorY, light, materials.railColorMul());
   }

   private static void renderScaledRailMesh(
      PoseStack ms,
      VertexConsumer vb,
      BlockState air,
      PartialModel partial,
      Vec3 origin,
      Vec3 diff,
      Vec3 faceNormal,
      boolean endCap,
      float modelAnchorY,
      int light,
      int colorMul
   ) {
      if (!(diff.lengthSqr() < 1.0E-20)) {
         Vec3 angles = TrackRenderer.getModelAngles(faceNormal, diff);
         float scale = endCap ? 2.2F : 2.1F;
         PoseStack poseStack = new PoseStack();
         ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(poseStack).translate(origin))
                     .rotateY((float)angles.y))
                  .rotateX((float)angles.x))
               .rotateZ((float)angles.z))
            .translate(0.0F, modelAnchorY, -0.03125F)
            .scale(1.0F, 1.0F, (float)diff.length() * scale);
         SuperByteBuffer buffer = ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(partial, air).mulPose(poseStack.last().pose()))
               .mulNormal(poseStack.last().normal()))
            .light(light);
         if (colorMul != 0) {
            buffer.color(0xFF000000 | colorMul & 16777215);
         }

         buffer.renderInto(ms, vb);
      }
   }

   private static void renderGirder(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb, BlockPos tePosition) {
      if (bc.hasGirder) {
         BlockState air = Blocks.AIR.defaultBlockState();
         GirderAngles segment = bc.getBakedGirders();

         for (int i = 1; i < segment.length; i++) {
            int light = LevelRenderer.getLightColor(level, segment.lightPosition[i].offset(tePosition));

            for (boolean first : Iterate.trueAndFalse) {
               Pose beamTransform = (Pose)segment.beams[i].get(first);
               ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.GIRDER_SEGMENT_MIDDLE, air).mulPose(beamTransform.pose()))
                     .mulNormal(beamTransform.normal()))
                  .light(light)
                  .renderInto(ms, vb);

               for (boolean top : Iterate.trueAndFalse) {
                  Pose beamCapTransform = (Pose)((Couple)segment.beamCaps[i].get(top)).get(first);
                  ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(
                              top ? AllPartialModels.GIRDER_SEGMENT_TOP : AllPartialModels.GIRDER_SEGMENT_BOTTOM, air
                           )
                           .mulPose(beamCapTransform.pose()))
                        .mulNormal(beamCapTransform.normal()))
                     .light(light)
                     .renderInto(ms, vb);
               }
            }
         }
      }
   }

   private static void renderCoasterTies(
      Level level,
      BezierConnection bc,
      PoseStack ms,
      VertexConsumer vb,
      BlockPos bePosition,
      BlockState air,
      AnchorPeerTrackCurveBerRender.CurveBerMaterials materials,
      AnchorPeerTrackCurveBerRender.SpineWalk walk
   ) {
      if (walk.firstPos != null && walk.lastPos != null && !walk.spineSamples.isEmpty()) {
         double span = walk.spineSamples.getLast().arcLength();
         int gapCount = CoasterCurveRailInstancing.curveTieGapCount(bc);
         boolean openStart = CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, true);
         boolean openEnd = CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, false);
         renderEndpointTie(level, bc, ms, vb, bePosition, air, materials, walk.firstPos, walk.firstTangent, walk.firstLateral, openStart, true);

         for (int k = 1; k < gapCount; k++) {
            double arc = span * (double)k / (double)gapCount;
            renderMidspanTie(level, ms, vb, bePosition, air, materials, sampleAtArcLength(walk.spineSamples, arc));
         }

         renderEndpointTie(level, bc, ms, vb, bePosition, air, materials, walk.lastPos, walk.lastTangent, walk.lastLateral, openEnd, false);
      }
   }

   private static AnchorPeerTrackCurveBerRender.SpineWalk collectSpineWalk(Level level, BezierConnection bc) {
      AnchorPeerTrackCurveBerRender.SpineWalk walk = new AnchorPeerTrackCurveBerRender.SpineWalk();
      Vec3 prevPos = null;
      double arcLength = 0.0;

      for (Segment segment : bc) {
         Vec3 currPos = segment.position;
         Vec3 currTangent = segment.derivative;
         float segT = bc.getSegmentT(segment.index);
         Vec3 currLateral = CoasterBezierRailFrames.lateralAt(bc, segT, currTangent, level);
         if (walk.prevLateral != null && currLateral.dot(walk.prevLateral) < 0.0) {
            currLateral = currLateral.scale(-1.0);
         }

         if (prevPos == null) {
            walk.firstPos = currPos;
            walk.firstTangent = currTangent;
            walk.firstLateral = currLateral;
            walk.spineSamples.add(new AnchorPeerTrackCurveBerRender.SpineSample(currPos, currTangent, currLateral, segT, 0.0));
         } else {
            arcLength += currPos.distanceTo(prevPos);
            walk.spineSamples.add(new AnchorPeerTrackCurveBerRender.SpineSample(currPos, currTangent, currLateral, segT, arcLength));
         }

         walk.lastPos = currPos;
         walk.lastTangent = currTangent;
         walk.lastLateral = currLateral;
         walk.prevLateral = currLateral;
         prevPos = currPos;
      }

      return walk;
   }

   private static void renderMidspanTie(
      Level level,
      PoseStack ms,
      VertexConsumer vb,
      BlockPos bePosition,
      BlockState air,
      AnchorPeerTrackCurveBerRender.CurveBerMaterials materials,
      AnchorPeerTrackCurveBerRender.SpineSample sample
   ) {
      int light = LevelRenderer.getLightColor(level, BlockPos.containing(sample.pos).offset(bePosition));
      PoseStack tiePose = CoasterTrackAnchorJunctionPlate.midspanTiePoseBelowOrigin(sample.pos, sample.lateral, sample.tangent);
      renderPartial(ms, vb, air, materials.tie(), tiePose.last(), light, materials.beamColorMul());
   }

   private static void renderEndpointTie(
      Level level,
      BezierConnection bc,
      PoseStack ms,
      VertexConsumer vb,
      BlockPos bePosition,
      BlockState air,
      AnchorPeerTrackCurveBerRender.CurveBerMaterials materials,
      Vec3 endpointPos,
      Vec3 endpointTangent,
      Vec3 endpointLateral,
      boolean openEnd,
      boolean atStart
   ) {
      BlockPos anchor = atStart ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
      if (openEnd || CoasterTrackAnchorJunctionPlate.shouldRenderJunctionTieAt(level, anchor, bc)) {
         CoasterTrackAnchorJunctionPlate.EndpointSpine frame = CoasterTrackAnchorJunctionPlate.spineAtEndpoint(bc, anchor, level);
         PoseStack tiePose;
         if (frame != null) {
            tiePose = openEnd
               ? CoasterTrackAnchorJunctionPlate.openEndTiePoseBelowOrigin(frame)
               : CoasterTrackAnchorJunctionPlate.junctionTiePoseBelowOrigin(frame);
         } else {
            tiePose = CoasterTrackAnchorJunctionPlate.midspanTiePoseBelowOrigin(endpointPos, endpointLateral, endpointTangent);
         }

         int light = LevelRenderer.getLightColor(level, BlockPos.containing(endpointPos).offset(bePosition));
         renderPartial(ms, vb, air, materials.tie(), tiePose.last(), light, materials.beamColorMul());
      }
   }

   private static AnchorPeerTrackCurveBerRender.SpineSample sampleAtArcLength(List<AnchorPeerTrackCurveBerRender.SpineSample> samples, double arc) {
      if (samples.size() == 1) {
         return samples.getFirst();
      } else {
         arc = Mth.clamp(arc, 0.0, samples.getLast().arcLength());

         for (int i = 1; i < samples.size(); i++) {
            AnchorPeerTrackCurveBerRender.SpineSample prev = samples.get(i - 1);
            AnchorPeerTrackCurveBerRender.SpineSample curr = samples.get(i);
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
               return new AnchorPeerTrackCurveBerRender.SpineSample(pos, tangent, lateral, segT, arc);
            }
         }

         return samples.getLast();
      }
   }

   private static double coasterRailHalfGauge() {
      return 0.772F;
   }

   private static float coasterRailModelAnchorY() {
      return 0.24609375F;
   }

   private static float coasterCenterBeamModelAnchorY() {
      return coasterRailModelAnchorY() + -0.4375F;
   }

   private static record CurveBerMaterials(
      PartialModel leftRail, PartialModel rightRail, PartialModel tie, PartialModel centerBeam, int railColorMul, int beamColorMul
   ) {
      static AnchorPeerTrackCurveBerRender.CurveBerMaterials from(CoasterAnchorpointBlockEntity anchor, BlockPos peerKey) {
         TrackModelHolder holder = CoasterTrackMaterials.COASTER.getModelHolder();
         int rawRail = anchor.getPeerCurveRailDiffuseRgb(peerKey) & 16777215;
         boolean railDyed = CoasterTrackDyedBezierModels.hasDyedRailTint(rawRail);
         return new AnchorPeerTrackCurveBerRender.CurveBerMaterials(
            railDyed ? CoasterTrackDyedBezierModels.LEFT_SEGMENT_DYED : holder.leftSegment(),
            railDyed ? CoasterTrackDyedBezierModels.RIGHT_SEGMENT_DYED : holder.rightSegment(),
            holder.tie(),
            CoasterTrackCenterBeamModel.CENTER_BEAM_SEGMENT,
            rawRail == 0 ? 0 : CoasterCurveTintColors.vertexColorMultiplierRgb(rawRail),
            0
         );
      }
   }

   private static record SpineSample(Vec3 pos, Vec3 tangent, Vec3 lateral, float segT, double arcLength) {
   }

   private static final class SpineWalk {
      private final List<AnchorPeerTrackCurveBerRender.SpineSample> spineSamples = new ArrayList<>();
      private Vec3 firstPos;
      private Vec3 firstTangent;
      private Vec3 firstLateral;
      private Vec3 lastPos;
      private Vec3 lastTangent;
      private Vec3 lastLateral;
      private Vec3 prevLateral;
   }
}
