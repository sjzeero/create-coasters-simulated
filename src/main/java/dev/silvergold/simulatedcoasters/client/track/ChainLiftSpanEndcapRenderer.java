/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  dev.engine_room.flywheel.lib.model.baked.PartialModel
 *  net.createmod.catnip.render.CachedBuffers
 *  net.createmod.catnip.render.SuperByteBuffer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.silvergold.simulatedcoasters.client.track.ChainLiftRibbonMeshCache;
import dev.silvergold.simulatedcoasters.client.track.CoasterAnchorClientSpace;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorJunctionModel;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorJunctionPlate;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class ChainLiftSpanEndcapRenderer {
    private static final double CHORD_ARC_EPS_BLOCKS = 0.12;

    private ChainLiftSpanEndcapRenderer() {
    }

    static void renderSpan(Level level, CoasterPathGraphSpans.SpanPath path, @Nullable ChainLiftRibbonMeshCache.CachedMesh mesh, double partialTickForRender, Vec3 cam, double maxRenderDistSq, PoseStack poseStack, MultiBufferSource.BufferSource buffers) {
        if (path.segments().isEmpty()) {
            return;
        }
        if (mesh != null && mesh.start() != null && mesh.end() != null) {
            ChainLiftSpanEndcapRenderer.renderEndcapFromFrame(level, mesh.start(), true, cam, maxRenderDistSq, poseStack, buffers);
            ChainLiftSpanEndcapRenderer.renderEndcapFromFrame(level, mesh.end(), false, cam, maxRenderDistSq, poseStack, buffers);
            return;
        }
        Double samplePartial = ChainLiftRibbonMeshCache.pathUsesSubLevelTrack(level, path) ? Double.valueOf(partialTickForRender) : null;
        CoasterPathGraphSpans.SpanArcSampler sampler = CoasterPathGraphSpans.SpanArcSampler.of(level, path, samplePartial);
        double total = sampler.totalLength();
        if (total <= 1.0E-6) {
            return;
        }
        ChainLiftSpanEndcapRenderer.renderEndcap(level, ChainLiftSpanEndcapRenderer.frameAt(level, sampler, 0.0, true, samplePartial), true, cam, maxRenderDistSq, poseStack, buffers);
        ChainLiftSpanEndcapRenderer.renderEndcap(level, ChainLiftSpanEndcapRenderer.frameAt(level, sampler, total, false, samplePartial), false, cam, maxRenderDistSq, poseStack, buffers);
    }

    @Nullable
    static ChainLiftRibbonMeshCache.RibbonEndcapFrame endcapFrameAt(Level level, CoasterPathGraphSpans.SpanArcSampler sampler, double arc, boolean atStart, @Nullable Double partialTickForRender) {
        @Nullable EndcapFrame frame = ChainLiftSpanEndcapRenderer.frameAt(level, sampler, arc, atStart, partialTickForRender);
        return frame == null ? null : new ChainLiftRibbonMeshCache.RibbonEndcapFrame(frame.spine, frame.lateral, frame.inward);
    }

    private static void renderEndcapFromFrame(Level level, ChainLiftRibbonMeshCache.RibbonEndcapFrame frame, boolean atStartAnchor, Vec3 cam, double maxRenderDistSq, PoseStack poseStack, MultiBufferSource.BufferSource buffers) {
        if (cam.distanceToSqr(frame.spine()) > maxRenderDistSq) {
            return;
        }
        PoseStack local = CoasterTrackAnchorJunctionPlate.connectionPoseBelowOrigin(frame.spine(), frame.lateral(), frame.chordIntoTrack());
        int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)BlockPos.containing((Position)frame.spine()));
        SuperByteBuffer model = CachedBuffers.partial((PartialModel)ChainLiftSpanEndcapRenderer.endcapModel(atStartAnchor), (BlockState)Blocks.AIR.defaultBlockState());
        ((SuperByteBuffer)model.transform(local.last())).light(light).renderInto(poseStack, buffers.getBuffer(RenderType.cutout()));
    }

    private static void renderEndcap(Level level, @Nullable EndcapFrame frame, boolean atStartAnchor, Vec3 cam, double maxRenderDistSq, PoseStack poseStack, MultiBufferSource.BufferSource buffers) {
        if (frame == null) {
            return;
        }
        EndcapFrame render = frame.toRenderSpace(level);
        if (cam.distanceToSqr(render.spine) > maxRenderDistSq) {
            return;
        }
        PoseStack local = CoasterTrackAnchorJunctionPlate.connectionPoseBelowOrigin(render.spine, render.lateral, render.inward);
        int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)BlockPos.containing((Position)render.spine));
        SuperByteBuffer model = CachedBuffers.partial((PartialModel)ChainLiftSpanEndcapRenderer.endcapModel(atStartAnchor), (BlockState)Blocks.AIR.defaultBlockState());
        ((SuperByteBuffer)model.transform(local.last())).light(light).renderInto(poseStack, buffers.getBuffer(RenderType.cutout()));
    }

    private static PartialModel endcapModel(boolean atStartAnchor) {
        return atStartAnchor ? CoasterTrackAnchorJunctionModel.TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT : CoasterTrackAnchorJunctionModel.TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT_END;
    }

    @Nullable
    private static EndcapFrame frameAt(Level level, CoasterPathGraphSpans.SpanArcSampler sampler, double arc, boolean atStart, @Nullable Double partialTickForRender) {
        boolean clientSublevel;
        @Nullable CoasterPathTrackFrame.GraphHit hit = sampler.sampleGraphHit(level, arc, partialTickForRender);
        if (hit == null) {
            return null;
        }
        boolean bl = clientSublevel = partialTickForRender != null && level.isClientSide() && CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge());
        if (clientSublevel) {
            BlockPos host = CoasterPathGraphManager.edgeSubLevelAnchor(level, hit.edge());
            return ChainLiftSpanEndcapRenderer.frameAtPlotLocal(level, sampler, hit, arc, atStart, partialTickForRender.floatValue(), host);
        }
        return ChainLiftSpanEndcapRenderer.frameAtWorld(level, sampler, hit, arc, atStart, partialTickForRender);
    }

    @Nullable
    private static EndcapFrame frameAtWorld(Level level, CoasterPathGraphSpans.SpanArcSampler sampler, CoasterPathTrackFrame.GraphHit hit, double arc, boolean atStart, @Nullable Double partialTickForRender) {
        @Nullable ChordSamples chord = ChainLiftSpanEndcapRenderer.chordSamples(level, sampler, hit, arc, atStart, partialTickForRender, false);
        if (chord == null) {
            return null;
        }
        Vec3 inward = ChainLiftSpanEndcapRenderer.trackTangentIntoSpanWorld(level, hit, chord.spanChord(), partialTickForRender);
        Vec3 lateral = ChainLiftSpanEndcapRenderer.trackLateralAtWorld(level, hit, inward, partialTickForRender);
        if (lateral.lengthSqr() < 1.0E-12) {
            return null;
        }
        return EndcapFrame.world(chord.spine(), lateral.normalize(), inward);
    }

    @Nullable
    private static EndcapFrame frameAtPlotLocal(Level level, CoasterPathGraphSpans.SpanArcSampler sampler, CoasterPathTrackFrame.GraphHit hit, double arc, boolean atStart, float partialTick, BlockPos host) {
        @Nullable ChordSamples chord = ChainLiftSpanEndcapRenderer.chordSamples(level, sampler, hit, arc, atStart, Double.valueOf(partialTick), true);
        if (chord == null) {
            return null;
        }
        Vec3 inward = ChainLiftSpanEndcapRenderer.trackTangentIntoSpanPlotLocal(level, hit, chord.spanChord());
        Vec3 lateral = ChainLiftSpanEndcapRenderer.trackLateralAtPlotLocal(level, hit, inward);
        if (lateral.lengthSqr() < 1.0E-12) {
            return null;
        }
        return EndcapFrame.plotLocal(chord.spine(), lateral.normalize(), inward, host, partialTick);
    }

    @Nullable
    private static ChordSamples chordSamples(Level level, CoasterPathGraphSpans.SpanArcSampler sampler, CoasterPathTrackFrame.GraphHit hit, double arc, boolean atStart, @Nullable Double partialTickForRender, boolean plotLocal) {
        Vec3 spanChord;
        double total = sampler.totalLength();
        double eps = Math.max(0.001, Math.min(0.12, total * 0.25));
        double arcA = atStart ? arc : Math.max(0.0, arc - eps);
        double arcB = atStart ? Math.min(total, arc + eps) : arc;
        @Nullable CoasterPathTrackFrame.GraphHit hitA = sampler.sampleGraphHit(level, arcA, partialTickForRender);
        @Nullable CoasterPathTrackFrame.GraphHit hitB = sampler.sampleGraphHit(level, arcB, partialTickForRender);
        if (hitA == null || hitB == null) {
            return null;
        }
        Vec3 pA = plotLocal ? ChainLiftRibbonMeshCache.ribbonCenterPlotLocal(level, hitA) : ChainLiftRibbonMeshCache.ribbonCenterAt(level, hitA, partialTickForRender);
        Vec3 pB = plotLocal ? ChainLiftRibbonMeshCache.ribbonCenterPlotLocal(level, hitB) : ChainLiftRibbonMeshCache.ribbonCenterAt(level, hitB, partialTickForRender);
        Vec3 vec3 = spanChord = atStart ? pB.subtract(pA) : pA.subtract(pB);
        if (spanChord.lengthSqr() < 1.0E-12) {
            return null;
        }
        Vec3 spine = plotLocal ? ChainLiftRibbonMeshCache.ribbonCenterPlotLocal(level, hit) : ChainLiftRibbonMeshCache.ribbonCenterAt(level, hit, partialTickForRender);
        return new ChordSamples(spine, spanChord);
    }

    private static Vec3 trackTangentIntoSpanWorld(Level level, CoasterPathTrackFrame.GraphHit hit, Vec3 spanChord, @Nullable Double partialTickForRender) {
        CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender);
        return ChainLiftSpanEndcapRenderer.alignForwardWithChord(basis.forward(), spanChord);
    }

    private static Vec3 trackTangentIntoSpanPlotLocal(Level level, CoasterPathTrackFrame.GraphHit hit, Vec3 spanChord) {
        CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit);
        return ChainLiftSpanEndcapRenderer.alignForwardWithChord(basis.forward(), spanChord);
    }

    private static Vec3 alignForwardWithChord(Vec3 forward, Vec3 spanChord) {
        if (forward.lengthSqr() < 1.0E-12) {
            return spanChord.normalize();
        }
        if ((forward = forward.normalize()).dot(spanChord) < 0.0) {
            forward = forward.scale(-1.0);
        }
        return forward;
    }

    private static Vec3 trackLateralAtWorld(Level level, CoasterPathTrackFrame.GraphHit hit, Vec3 unitTangentIntoSpan, @Nullable Double partialTickForRender) {
        BezierConnection bc = hit.edge().bezier();
        if (bc != null && CoasterBezierRailFrames.isCoaster(bc)) {
            float t = (float)Mth.clamp((double)hit.t(), (double)0.0, (double)1.0);
            return CoasterBezierRailFrames.lateralAt(bc, t, unitTangentIntoSpan, level);
        }
        return CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender).right();
    }

    private static Vec3 trackLateralAtPlotLocal(Level level, CoasterPathTrackFrame.GraphHit hit, Vec3 unitTangentIntoSpan) {
        BezierConnection bc = hit.edge().bezier();
        if (bc != null && CoasterBezierRailFrames.isCoaster(bc)) {
            float t = (float)Mth.clamp((double)hit.t(), (double)0.0, (double)1.0);
            return CoasterBezierRailFrames.lateralAt(bc, t, unitTangentIntoSpan, level);
        }
        return CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit).right();
    }

    private record EndcapFrame(Vec3 spine, Vec3 lateral, Vec3 inward, @Nullable BlockPos sublevelHost, float sublevelPartialTick) {
        static EndcapFrame world(Vec3 spine, Vec3 lateral, Vec3 inward) {
            return new EndcapFrame(spine, lateral, inward, null, 0.0f);
        }

        static EndcapFrame plotLocal(Vec3 spine, Vec3 lateral, Vec3 inward, BlockPos host, float partialTick) {
            return new EndcapFrame(spine, lateral, inward, host, partialTick);
        }

        EndcapFrame toRenderSpace(Level level) {
            if (this.sublevelHost == null) {
                return this;
            }
            return EndcapFrame.world(CoasterAnchorClientSpace.toRenderWorld(level, this.sublevelHost, this.spine, this.sublevelPartialTick), CoasterAnchorClientSpace.toRenderDirection(level, this.sublevelHost, this.lateral, this.sublevelPartialTick), CoasterAnchorClientSpace.toRenderDirection(level, this.sublevelHost, this.inward, this.sublevelPartialTick));
        }
    }

    private record ChordSamples(Vec3 spine, Vec3 spanChord) {
    }
}

