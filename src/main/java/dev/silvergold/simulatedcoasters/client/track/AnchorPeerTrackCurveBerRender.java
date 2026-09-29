/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.simibubi.create.AllPartialModels
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.BezierConnection$GirderAngles
 *  com.simibubi.create.content.trains.track.BezierConnection$Segment
 *  com.simibubi.create.content.trains.track.BezierConnection$SegmentAngles
 *  com.simibubi.create.content.trains.track.TrackMaterial$TrackModelHolder
 *  com.simibubi.create.content.trains.track.TrackRenderer
 *  dev.engine_room.flywheel.api.visualization.VisualizationManager
 *  dev.engine_room.flywheel.lib.model.baked.PartialModel
 *  dev.engine_room.flywheel.lib.transform.PoseTransformStack
 *  dev.engine_room.flywheel.lib.transform.TransformStack
 *  net.createmod.catnip.data.Couple
 *  net.createmod.catnip.data.Iterate
 *  net.createmod.catnip.render.CachedBuffers
 *  net.createmod.catnip.render.SuperByteBuffer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4fc
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.track.CoasterCurveRailInstancing;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorGirderBeam;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorJunctionPlate;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackCenterBeamModel;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackDyedBezierModels;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterCurveTintColors;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;

public final class AnchorPeerTrackCurveBerRender {
    private AnchorPeerTrackCurveBerRender() {
    }

    public static void render(CoasterAnchorpointBlockEntity anchor, PoseStack ms, MultiBufferSource buffer) {
        Level level = anchor.getLevel();
        if (level == null || VisualizationManager.supportsVisualization((LevelAccessor)level)) {
            return;
        }
        VertexConsumer vb = buffer.getBuffer(RenderType.cutoutMipped());
        for (Map.Entry<BlockPos, BezierConnection> e : anchor.getAnchorPeerCurvesView().entrySet()) {
            BezierConnection bc = e.getValue();
            if (!bc.isPrimary()) continue;
            if (CoasterTrackMaterials.COASTER.id.equals((Object)bc.getMaterial().id)) {
                AnchorPeerTrackCurveBerRender.renderCoasterCurve(level, bc, ms, vb, CurveBerMaterials.from(anchor, e.getKey()));
                continue;
            }
            TrackRenderer.renderBezierTurn((Level)level, (BezierConnection)bc, (PoseStack)ms, (VertexConsumer)vb);
        }
    }

    private static void renderCoasterCurve(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb, CurveBerMaterials materials) {
        ms.pushPose();
        BlockPos bePosition = (BlockPos)bc.bePositions.getFirst();
        BlockState air = Blocks.AIR.defaultBlockState();
        BezierConnection.SegmentAngles segments = bc.getBakedSegments();
        for (int i = 1; i < segments.length; ++i) {
            int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)segments.lightPosition[i].offset((Vec3i)bePosition));
            for (boolean first : Iterate.trueAndFalse) {
                PoseStack.Pose transform = (PoseStack.Pose)segments.railTransforms[i].get(first);
                AnchorPeerTrackCurveBerRender.renderPartial(ms, vb, air, first ? materials.leftRail() : materials.rightRail(), transform, light, materials.railColorMul());
            }
        }
        SpineWalk walk = AnchorPeerTrackCurveBerRender.collectSpineWalk(level, bc);
        AnchorPeerTrackCurveBerRender.renderOpenEndRailExtensions(level, bc, ms, vb, bePosition, air, materials, walk);
        AnchorPeerTrackCurveBerRender.renderGirder(level, bc, ms, vb, bePosition);
        AnchorPeerTrackCurveBerRender.renderJunctionAttachments(level, bc, ms, vb, bePosition);
        AnchorPeerTrackCurveBerRender.renderCoasterCenterBeams(level, bc, ms, vb, bePosition, air, materials);
        AnchorPeerTrackCurveBerRender.renderCoasterTies(level, bc, ms, vb, bePosition, air, materials, walk);
        ms.popPose();
    }

    private static void renderPartial(PoseStack ms, VertexConsumer vb, BlockState air, PartialModel partial, PoseStack.Pose transform, int light, int colorMul) {
        SuperByteBuffer buffer = ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial((PartialModel)partial, (BlockState)air).mulPose((Matrix4fc)transform.pose())).mulNormal((Matrix3fc)transform.normal())).light(light);
        if (colorMul != 0) {
            buffer.color(0xFF000000 | colorMul & 0xFFFFFF);
        }
        buffer.renderInto(ms, vb);
    }

    private static void renderCoasterCenterBeams(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb, BlockPos bePosition, BlockState air, CurveBerMaterials materials) {
        int segmentCount = bc.getSegmentCount();
        double railHalf = AnchorPeerTrackCurveBerRender.coasterRailHalfGauge();
        Couple previousOffsets = null;
        for (BezierConnection.Segment segment : bc) {
            int i = segment.index;
            boolean end = i == 0 || i == segmentCount;
            Couple railOffsets = Couple.create((Object)segment.position.add(segment.normal.scale(railHalf)), (Object)segment.position.subtract(segment.normal.scale(railHalf)));
            Vec3 railMiddle = ((Vec3)railOffsets.getFirst()).add((Vec3)railOffsets.getSecond()).scale(0.5);
            if (previousOffsets == null) {
                previousOffsets = railOffsets;
                continue;
            }
            Vec3 prevMiddle = ((Vec3)previousOffsets.getFirst()).add((Vec3)previousOffsets.getSecond()).scale(0.5);
            Vec3 diff = railMiddle.subtract(prevMiddle);
            if (diff.lengthSqr() > 1.0E-20) {
                int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)BlockPos.containing((Position)railMiddle).offset((Vec3i)bePosition));
                AnchorPeerTrackCurveBerRender.renderScaledRailMesh(ms, vb, air, materials.centerBeam(), prevMiddle, diff, segment.normal, end, AnchorPeerTrackCurveBerRender.coasterCenterBeamModelAnchorY(), light, materials.beamColorMul());
            }
            previousOffsets = railOffsets;
        }
    }

    private static void renderJunctionAttachments(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb, BlockPos curveHostPos) {
        CoasterTrackAnchorJunctionPlate.renderBerPlate(level, bc, (BlockPos)bc.bePositions.getFirst(), ms, vb, curveHostPos);
        CoasterTrackAnchorJunctionPlate.renderBerPlate(level, bc, (BlockPos)bc.bePositions.getSecond(), ms, vb, curveHostPos);
        CoasterTrackAnchorGirderBeam.renderBerGirders(level, bc, (BlockPos)bc.bePositions.getFirst(), curveHostPos, ms, vb);
        CoasterTrackAnchorGirderBeam.renderBerGirders(level, bc, (BlockPos)bc.bePositions.getSecond(), curveHostPos, ms, vb);
    }

    private static void renderOpenEndRailExtensions(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb, BlockPos bePosition, BlockState air, CurveBerMaterials materials, SpineWalk walk) {
        float ext;
        if (walk.firstPos == null || walk.firstTangent == null || walk.firstLateral == null) {
            return;
        }
        double railHalf = AnchorPeerTrackCurveBerRender.coasterRailHalfGauge();
        if (CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, true)) {
            ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, true);
            AnchorPeerTrackCurveBerRender.renderOpenEndRailExtension(level, ms, vb, bePosition, air, materials, walk.firstPos, walk.firstTangent, walk.firstLateral, true, ext, railHalf);
        }
        if (walk.lastPos != null && walk.lastTangent != null && walk.lastLateral != null && CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, false)) {
            ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, false);
            AnchorPeerTrackCurveBerRender.renderOpenEndRailExtension(level, ms, vb, bePosition, air, materials, walk.lastPos, walk.lastTangent, walk.lastLateral, false, ext, railHalf);
        }
    }

    private static void renderOpenEndRailExtension(Level level, PoseStack ms, VertexConsumer vb, BlockPos bePosition, BlockState air, CurveBerMaterials materials, Vec3 endpointPos, Vec3 endpointTangent, Vec3 endpointLateral, boolean atStart, float extensionBlocks, double railHalf) {
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
        int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)BlockPos.containing((Position)junctionLeft).offset((Vec3i)bePosition));
        float anchorY = AnchorPeerTrackCurveBerRender.coasterRailModelAnchorY();
        AnchorPeerTrackCurveBerRender.renderScaledRailMesh(ms, vb, air, materials.leftRail(), prevLeft, currLeft.subtract(prevLeft), lat, true, anchorY, light, materials.railColorMul());
        AnchorPeerTrackCurveBerRender.renderScaledRailMesh(ms, vb, air, materials.rightRail(), prevRight, currRight.subtract(prevRight), lat, true, anchorY, light, materials.railColorMul());
    }

    private static void renderScaledRailMesh(PoseStack ms, VertexConsumer vb, BlockState air, PartialModel partial, Vec3 origin, Vec3 diff, Vec3 faceNormal, boolean endCap, float modelAnchorY, int light, int colorMul) {
        if (diff.lengthSqr() < 1.0E-20) {
            return;
        }
        Vec3 angles = TrackRenderer.getModelAngles((Vec3)faceNormal, (Vec3)diff);
        float scale = endCap ? 2.2f : 2.1f;
        PoseStack poseStack = new PoseStack();
        ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of((PoseStack)poseStack).translate(origin)).rotateY((float)angles.y)).rotateX((float)angles.x)).rotateZ((float)angles.z)).translate(0.0f, modelAnchorY, -0.03125f).scale(1.0f, 1.0f, (float)diff.length() * scale);
        SuperByteBuffer buffer = ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial((PartialModel)partial, (BlockState)air).mulPose((Matrix4fc)poseStack.last().pose())).mulNormal((Matrix3fc)poseStack.last().normal())).light(light);
        if (colorMul != 0) {
            buffer.color(0xFF000000 | colorMul & 0xFFFFFF);
        }
        buffer.renderInto(ms, vb);
    }

    private static void renderGirder(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb, BlockPos tePosition) {
        if (!bc.hasGirder) {
            return;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        BezierConnection.GirderAngles segment = bc.getBakedGirders();
        for (int i = 1; i < segment.length; ++i) {
            int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)segment.lightPosition[i].offset((Vec3i)tePosition));
            for (boolean first : Iterate.trueAndFalse) {
                PoseStack.Pose beamTransform = (PoseStack.Pose)segment.beams[i].get(first);
                ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial((PartialModel)AllPartialModels.GIRDER_SEGMENT_MIDDLE, (BlockState)air).mulPose((Matrix4fc)beamTransform.pose())).mulNormal((Matrix3fc)beamTransform.normal())).light(light).renderInto(ms, vb);
                for (boolean top : Iterate.trueAndFalse) {
                    PoseStack.Pose beamCapTransform = (PoseStack.Pose)((Couple)segment.beamCaps[i].get(top)).get(first);
                    ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial((PartialModel)(top ? AllPartialModels.GIRDER_SEGMENT_TOP : AllPartialModels.GIRDER_SEGMENT_BOTTOM), (BlockState)air).mulPose((Matrix4fc)beamCapTransform.pose())).mulNormal((Matrix3fc)beamCapTransform.normal())).light(light).renderInto(ms, vb);
                }
            }
        }
    }

    private static void renderCoasterTies(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb, BlockPos bePosition, BlockState air, CurveBerMaterials materials, SpineWalk walk) {
        if (walk.firstPos == null || walk.lastPos == null || walk.spineSamples.isEmpty()) {
            return;
        }
        double span = walk.spineSamples.getLast().arcLength();
        int gapCount = CoasterCurveRailInstancing.curveTieGapCount(bc);
        boolean openStart = CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, true);
        boolean openEnd = CoasterCurveRailInstancing.isOpenCurveEnd(level, bc, false);
        AnchorPeerTrackCurveBerRender.renderEndpointTie(level, bc, ms, vb, bePosition, air, materials, walk.firstPos, walk.firstTangent, walk.firstLateral, openStart, true);
        for (int k = 1; k < gapCount; ++k) {
            double arc = span * (double)k / (double)gapCount;
            AnchorPeerTrackCurveBerRender.renderMidspanTie(level, ms, vb, bePosition, air, materials, AnchorPeerTrackCurveBerRender.sampleAtArcLength(walk.spineSamples, arc));
        }
        AnchorPeerTrackCurveBerRender.renderEndpointTie(level, bc, ms, vb, bePosition, air, materials, walk.lastPos, walk.lastTangent, walk.lastLateral, openEnd, false);
    }

    private static SpineWalk collectSpineWalk(Level level, BezierConnection bc) {
        SpineWalk walk = new SpineWalk();
        Vec3 prevPos = null;
        double arcLength = 0.0;
        for (BezierConnection.Segment segment : bc) {
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
                walk.spineSamples.add(new SpineSample(currPos, currTangent, currLateral, segT, 0.0));
            } else {
                walk.spineSamples.add(new SpineSample(currPos, currTangent, currLateral, segT, arcLength += currPos.distanceTo(prevPos)));
            }
            walk.lastPos = currPos;
            walk.lastTangent = currTangent;
            walk.lastLateral = currLateral;
            walk.prevLateral = currLateral;
            prevPos = currPos;
        }
        return walk;
    }

    private static void renderMidspanTie(Level level, PoseStack ms, VertexConsumer vb, BlockPos bePosition, BlockState air, CurveBerMaterials materials, SpineSample sample) {
        int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)BlockPos.containing((Position)sample.pos).offset((Vec3i)bePosition));
        PoseStack tiePose = CoasterTrackAnchorJunctionPlate.midspanTiePoseBelowOrigin(sample.pos, sample.lateral, sample.tangent);
        AnchorPeerTrackCurveBerRender.renderPartial(ms, vb, air, materials.tie(), tiePose.last(), light, materials.beamColorMul());
    }

    private static void renderEndpointTie(Level level, BezierConnection bc, PoseStack ms, VertexConsumer vb, BlockPos bePosition, BlockState air, CurveBerMaterials materials, Vec3 endpointPos, Vec3 endpointTangent, Vec3 endpointLateral, boolean openEnd, boolean atStart) {
        BlockPos anchor;
        BlockPos blockPos = anchor = atStart ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
        if (!openEnd && !CoasterTrackAnchorJunctionPlate.shouldRenderJunctionTieAt(level, anchor, bc)) {
            return;
        }
        CoasterTrackAnchorJunctionPlate.EndpointSpine frame = CoasterTrackAnchorJunctionPlate.spineAtEndpoint(bc, anchor, level);
        PoseStack tiePose = frame != null ? (openEnd ? CoasterTrackAnchorJunctionPlate.openEndTiePoseBelowOrigin(frame) : CoasterTrackAnchorJunctionPlate.junctionTiePoseBelowOrigin(frame)) : CoasterTrackAnchorJunctionPlate.midspanTiePoseBelowOrigin(endpointPos, endpointLateral, endpointTangent);
        int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)BlockPos.containing((Position)endpointPos).offset((Vec3i)bePosition));
        AnchorPeerTrackCurveBerRender.renderPartial(ms, vb, air, materials.tie(), tiePose.last(), light, materials.beamColorMul());
    }

    private static SpineSample sampleAtArcLength(List<SpineSample> samples, double arc) {
        if (samples.size() == 1) {
            return samples.getFirst();
        }
        arc = Mth.clamp((double)arc, (double)0.0, (double)samples.getLast().arcLength());
        for (int i = 1; i < samples.size(); ++i) {
            SpineSample prev = samples.get(i - 1);
            SpineSample curr = samples.get(i);
            if (!(arc <= curr.arcLength + 1.0E-9)) continue;
            double segLen = curr.arcLength - prev.arcLength;
            double f = segLen < 1.0E-12 ? 0.0 : (arc - prev.arcLength) / segLen;
            Vec3 pos = prev.pos.lerp(curr.pos, f);
            Vec3 tangent = prev.tangent.lerp(curr.tangent, f);
            Vec3 lateral = prev.lateral.lerp(curr.lateral, f);
            if (lateral.dot(prev.lateral) < 0.0) {
                lateral = lateral.scale(-1.0);
            }
            tangent = tangent.lengthSqr() < 1.0E-14 ? curr.tangent : tangent.normalize();
            lateral = lateral.lengthSqr() < 1.0E-14 ? curr.lateral : lateral.normalize();
            float segT = Mth.lerp((float)((float)f), (float)prev.segT, (float)curr.segT);
            return new SpineSample(pos, tangent, lateral, segT, arc);
        }
        return samples.getLast();
    }

    private static double coasterRailHalfGauge() {
        return 0.772f;
    }

    private static float coasterRailModelAnchorY() {
        return 0.24609375f;
    }

    private static float coasterCenterBeamModelAnchorY() {
        return AnchorPeerTrackCurveBerRender.coasterRailModelAnchorY() + -0.4375f;
    }

    private record CurveBerMaterials(PartialModel leftRail, PartialModel rightRail, PartialModel tie, PartialModel centerBeam, int railColorMul, int beamColorMul) {
        static CurveBerMaterials from(CoasterAnchorpointBlockEntity anchor, BlockPos peerKey) {
            TrackMaterial.TrackModelHolder holder = CoasterTrackMaterials.COASTER.getModelHolder();
            int rawRail = anchor.getPeerCurveRailDiffuseRgb(peerKey) & 0xFFFFFF;
            boolean railDyed = CoasterTrackDyedBezierModels.hasDyedRailTint(rawRail);
            return new CurveBerMaterials(railDyed ? CoasterTrackDyedBezierModels.LEFT_SEGMENT_DYED : holder.leftSegment(), railDyed ? CoasterTrackDyedBezierModels.RIGHT_SEGMENT_DYED : holder.rightSegment(), holder.tie(), CoasterTrackCenterBeamModel.CENTER_BEAM_SEGMENT, rawRail == 0 ? 0 : CoasterCurveTintColors.vertexColorMultiplierRgb(rawRail), 0);
        }
    }

    private static final class SpineWalk {
        private final List<SpineSample> spineSamples = new ArrayList<SpineSample>();
        private Vec3 firstPos;
        private Vec3 firstTangent;
        private Vec3 firstLateral;
        private Vec3 lastPos;
        private Vec3 lastTangent;
        private Vec3 lastLateral;
        private Vec3 prevLateral;

        private SpineWalk() {
        }
    }

    private record SpineSample(Vec3 pos, Vec3 tangent, Vec3 lateral, float segT, double arcLength) {
    }
}

