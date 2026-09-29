package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class ChainLiftSpanEndcapRenderer {
   private static final double CHORD_ARC_EPS_BLOCKS = 0.12;

   private ChainLiftSpanEndcapRenderer() {
   }

   static void renderSpan(
      Level level,
      CoasterPathGraphSpans.SpanPath path,
      @Nullable ChainLiftRibbonMeshCache.CachedMesh mesh,
      double partialTickForRender,
      Vec3 cam,
      double maxRenderDistSq,
      PoseStack poseStack,
      BufferSource buffers
   ) {
      if (!path.segments().isEmpty()) {
         if (mesh != null && mesh.start() != null && mesh.end() != null) {
            renderEndcapFromFrame(level, mesh.start(), true, cam, maxRenderDistSq, poseStack, buffers);
            renderEndcapFromFrame(level, mesh.end(), false, cam, maxRenderDistSq, poseStack, buffers);
         } else {
            Double samplePartial = ChainLiftRibbonMeshCache.pathUsesSubLevelTrack(level, path) ? partialTickForRender : null;
            CoasterPathGraphSpans.SpanArcSampler sampler = CoasterPathGraphSpans.SpanArcSampler.of(level, path, samplePartial);
            double total = sampler.totalLength();
            if (!(total <= 1.0E-6)) {
               renderEndcap(level, frameAt(level, sampler, 0.0, true, samplePartial), true, cam, maxRenderDistSq, poseStack, buffers);
               renderEndcap(level, frameAt(level, sampler, total, false, samplePartial), false, cam, maxRenderDistSq, poseStack, buffers);
            }
         }
      }
   }

   @Nullable
   static ChainLiftRibbonMeshCache.RibbonEndcapFrame endcapFrameAt(
      Level level, CoasterPathGraphSpans.SpanArcSampler sampler, double arc, boolean atStart, @Nullable Double partialTickForRender
   ) {
      ChainLiftSpanEndcapRenderer.EndcapFrame frame = frameAt(level, sampler, arc, atStart, partialTickForRender);
      return frame == null ? null : new ChainLiftRibbonMeshCache.RibbonEndcapFrame(frame.spine, frame.lateral, frame.inward);
   }

   private static void renderEndcapFromFrame(
      Level level,
      ChainLiftRibbonMeshCache.RibbonEndcapFrame frame,
      boolean atStartAnchor,
      Vec3 cam,
      double maxRenderDistSq,
      PoseStack poseStack,
      BufferSource buffers
   ) {
      if (!(cam.distanceToSqr(frame.spine()) > maxRenderDistSq)) {
         PoseStack local = CoasterTrackAnchorJunctionPlate.connectionPoseBelowOrigin(frame.spine(), frame.lateral(), frame.chordIntoTrack());
         int light = LevelRenderer.getLightColor(level, BlockPos.containing(frame.spine()));
         SuperByteBuffer model = CachedBuffers.partial(endcapModel(atStartAnchor), Blocks.AIR.defaultBlockState());
         ((SuperByteBuffer)model.transform(local.last())).light(light).renderInto(poseStack, buffers.getBuffer(RenderType.cutout()));
      }
   }

   private static void renderEndcap(
      Level level,
      @Nullable ChainLiftSpanEndcapRenderer.EndcapFrame frame,
      boolean atStartAnchor,
      Vec3 cam,
      double maxRenderDistSq,
      PoseStack poseStack,
      BufferSource buffers
   ) {
      if (frame != null) {
         ChainLiftSpanEndcapRenderer.EndcapFrame render = frame.toRenderSpace(level);
         if (!(cam.distanceToSqr(render.spine) > maxRenderDistSq)) {
            PoseStack local = CoasterTrackAnchorJunctionPlate.connectionPoseBelowOrigin(render.spine, render.lateral, render.inward);
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(render.spine));
            SuperByteBuffer model = CachedBuffers.partial(endcapModel(atStartAnchor), Blocks.AIR.defaultBlockState());
            ((SuperByteBuffer)model.transform(local.last())).light(light).renderInto(poseStack, buffers.getBuffer(RenderType.cutout()));
         }
      }
   }

   private static PartialModel endcapModel(boolean atStartAnchor) {
      return atStartAnchor
         ? CoasterTrackAnchorJunctionModel.TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT
         : CoasterTrackAnchorJunctionModel.TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT_END;
   }

   @Nullable
   private static ChainLiftSpanEndcapRenderer.EndcapFrame frameAt(
      Level level, CoasterPathGraphSpans.SpanArcSampler sampler, double arc, boolean atStart, @Nullable Double partialTickForRender
   ) {
      CoasterPathTrackFrame.GraphHit hit = sampler.sampleGraphHit(level, arc, partialTickForRender);
      if (hit == null) {
         return null;
      } else {
         boolean clientSublevel = partialTickForRender != null && level.isClientSide() && CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge());
         if (clientSublevel) {
            BlockPos host = CoasterPathGraphManager.edgeSubLevelAnchor(level, hit.edge());
            return frameAtPlotLocal(level, sampler, hit, arc, atStart, partialTickForRender.floatValue(), host);
         } else {
            return frameAtWorld(level, sampler, hit, arc, atStart, partialTickForRender);
         }
      }
   }

   @Nullable
   private static ChainLiftSpanEndcapRenderer.EndcapFrame frameAtWorld(
      Level level,
      CoasterPathGraphSpans.SpanArcSampler sampler,
      CoasterPathTrackFrame.GraphHit hit,
      double arc,
      boolean atStart,
      @Nullable Double partialTickForRender
   ) {
      ChainLiftSpanEndcapRenderer.ChordSamples chord = chordSamples(level, sampler, hit, arc, atStart, partialTickForRender, false);
      if (chord == null) {
         return null;
      } else {
         Vec3 inward = trackTangentIntoSpanWorld(level, hit, chord.spanChord(), partialTickForRender);
         Vec3 lateral = trackLateralAtWorld(level, hit, inward, partialTickForRender);
         return lateral.lengthSqr() < 1.0E-12 ? null : ChainLiftSpanEndcapRenderer.EndcapFrame.world(chord.spine(), lateral.normalize(), inward);
      }
   }

   @Nullable
   private static ChainLiftSpanEndcapRenderer.EndcapFrame frameAtPlotLocal(
      Level level,
      CoasterPathGraphSpans.SpanArcSampler sampler,
      CoasterPathTrackFrame.GraphHit hit,
      double arc,
      boolean atStart,
      float partialTick,
      BlockPos host
   ) {
      ChainLiftSpanEndcapRenderer.ChordSamples chord = chordSamples(level, sampler, hit, arc, atStart, (double)partialTick, true);
      if (chord == null) {
         return null;
      } else {
         Vec3 inward = trackTangentIntoSpanPlotLocal(level, hit, chord.spanChord());
         Vec3 lateral = trackLateralAtPlotLocal(level, hit, inward);
         return lateral.lengthSqr() < 1.0E-12
            ? null
            : ChainLiftSpanEndcapRenderer.EndcapFrame.plotLocal(chord.spine(), lateral.normalize(), inward, host, partialTick);
      }
   }

   @Nullable
   private static ChainLiftSpanEndcapRenderer.ChordSamples chordSamples(
      Level level,
      CoasterPathGraphSpans.SpanArcSampler sampler,
      CoasterPathTrackFrame.GraphHit hit,
      double arc,
      boolean atStart,
      @Nullable Double partialTickForRender,
      boolean plotLocal
   ) {
      double total = sampler.totalLength();
      double eps = Math.max(0.001, Math.min(0.12, total * 0.25));
      double arcA = atStart ? arc : Math.max(0.0, arc - eps);
      double arcB = atStart ? Math.min(total, arc + eps) : arc;
      CoasterPathTrackFrame.GraphHit hitA = sampler.sampleGraphHit(level, arcA, partialTickForRender);
      CoasterPathTrackFrame.GraphHit hitB = sampler.sampleGraphHit(level, arcB, partialTickForRender);
      if (hitA != null && hitB != null) {
         Vec3 pA = plotLocal
            ? ChainLiftRibbonMeshCache.ribbonCenterPlotLocal(level, hitA)
            : ChainLiftRibbonMeshCache.ribbonCenterAt(level, hitA, partialTickForRender);
         Vec3 pB = plotLocal
            ? ChainLiftRibbonMeshCache.ribbonCenterPlotLocal(level, hitB)
            : ChainLiftRibbonMeshCache.ribbonCenterAt(level, hitB, partialTickForRender);
         Vec3 spanChord = atStart ? pB.subtract(pA) : pA.subtract(pB);
         if (spanChord.lengthSqr() < 1.0E-12) {
            return null;
         } else {
            Vec3 spine = plotLocal
               ? ChainLiftRibbonMeshCache.ribbonCenterPlotLocal(level, hit)
               : ChainLiftRibbonMeshCache.ribbonCenterAt(level, hit, partialTickForRender);
            return new ChainLiftSpanEndcapRenderer.ChordSamples(spine, spanChord);
         }
      } else {
         return null;
      }
   }

   private static Vec3 trackTangentIntoSpanWorld(Level level, CoasterPathTrackFrame.GraphHit hit, Vec3 spanChord, @Nullable Double partialTickForRender) {
      CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender);
      return alignForwardWithChord(basis.forward(), spanChord);
   }

   private static Vec3 trackTangentIntoSpanPlotLocal(Level level, CoasterPathTrackFrame.GraphHit hit, Vec3 spanChord) {
      CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit);
      return alignForwardWithChord(basis.forward(), spanChord);
   }

   private static Vec3 alignForwardWithChord(Vec3 forward, Vec3 spanChord) {
      if (forward.lengthSqr() < 1.0E-12) {
         return spanChord.normalize();
      } else {
         forward = forward.normalize();
         if (forward.dot(spanChord) < 0.0) {
            forward = forward.scale(-1.0);
         }

         return forward;
      }
   }

   private static Vec3 trackLateralAtWorld(Level level, CoasterPathTrackFrame.GraphHit hit, Vec3 unitTangentIntoSpan, @Nullable Double partialTickForRender) {
      BezierConnection bc = hit.edge().bezier();
      if (bc != null && CoasterBezierRailFrames.isCoaster(bc)) {
         float t = (float)Mth.clamp(hit.t(), 0.0, 1.0);
         return CoasterBezierRailFrames.lateralAt(bc, t, unitTangentIntoSpan, level);
      } else {
         return CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender).right();
      }
   }

   private static Vec3 trackLateralAtPlotLocal(Level level, CoasterPathTrackFrame.GraphHit hit, Vec3 unitTangentIntoSpan) {
      BezierConnection bc = hit.edge().bezier();
      if (bc != null && CoasterBezierRailFrames.isCoaster(bc)) {
         float t = (float)Mth.clamp(hit.t(), 0.0, 1.0);
         return CoasterBezierRailFrames.lateralAt(bc, t, unitTangentIntoSpan, level);
      } else {
         return CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit).right();
      }
   }

   private static record ChordSamples(Vec3 spine, Vec3 spanChord) {
   }

   private static record EndcapFrame(Vec3 spine, Vec3 lateral, Vec3 inward, @Nullable BlockPos sublevelHost, float sublevelPartialTick) {
      static ChainLiftSpanEndcapRenderer.EndcapFrame world(Vec3 spine, Vec3 lateral, Vec3 inward) {
         return new ChainLiftSpanEndcapRenderer.EndcapFrame(spine, lateral, inward, null, 0.0F);
      }

      static ChainLiftSpanEndcapRenderer.EndcapFrame plotLocal(Vec3 spine, Vec3 lateral, Vec3 inward, BlockPos host, float partialTick) {
         return new ChainLiftSpanEndcapRenderer.EndcapFrame(spine, lateral, inward, host, partialTick);
      }

      ChainLiftSpanEndcapRenderer.EndcapFrame toRenderSpace(Level level) {
         return this.sublevelHost == null
            ? this
            : world(
               CoasterAnchorClientSpace.toRenderWorld(level, this.sublevelHost, this.spine, this.sublevelPartialTick),
               CoasterAnchorClientSpace.toRenderDirection(level, this.sublevelHost, this.lateral, this.sublevelPartialTick),
               CoasterAnchorClientSpace.toRenderDirection(level, this.sublevelHost, this.inward, this.sublevelPartialTick)
            );
      }
   }
}
