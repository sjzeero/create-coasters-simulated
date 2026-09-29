/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.BezierConnection$Segment
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  net.createmod.catnip.math.VecHelper
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveOutlineSupport;
import dev.silvergold.simulatedcoasters.client.track.CoasterAnchorClientSpace;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterCurvePickGeometry;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public final class CoasterCurveSelectionOutlineRenderer {
    private static final float OUTLINE_R = 0.0f;
    private static final float OUTLINE_G = 0.0f;
    private static final float OUTLINE_B = 0.0f;
    private static final float OUTLINE_A = 0.4f;
    private static final double OUTLINE_HALF_WIDTH = CoasterCurvePickGeometry.curvePickCrossSection().bounds().getXsize() * 0.5 + 0.3;
    private static final double OUTLINE_HALF_HEIGHT = CoasterCurvePickGeometry.curvePickCrossSection().bounds().getYsize() * 0.5 - 0.1;
    private static final double[][] LOCAL_CORNERS = new double[][]{{-OUTLINE_HALF_WIDTH, -OUTLINE_HALF_HEIGHT}, {OUTLINE_HALF_WIDTH, -OUTLINE_HALF_HEIGHT}, {OUTLINE_HALF_WIDTH, OUTLINE_HALF_HEIGHT}, {-OUTLINE_HALF_WIDTH, OUTLINE_HALF_HEIGHT}};
    private static final double MAX_OUTLINE_SEGMENTS = 256.0;

    public static void render(PoseStack ms, MultiBufferSource buffer, Vec3 camera, Level level, BezierConnection bc, @Nullable BlockPos sublevelAnchor, float partialTick) {
        CoasterCurveSelectionOutlineRenderer.render(ms, buffer, camera, level, bc, sublevelAnchor, partialTick, 0.0f, 0.0f, 0.0f, 0.4f);
    }

    public static void render(PoseStack ms, MultiBufferSource buffer, Vec3 camera, Level level, BezierConnection bc, @Nullable BlockPos sublevelAnchor, float partialTick, float outlineR, float outlineG, float outlineB, float outlineA) {
        List<OutlineSample> samples;
        if (!AnchorPeerCurveOutlineSupport.hasSaneOutlineBounds(bc)) {
            return;
        }
        if (sublevelAnchor == null) {
            sublevelAnchor = CoasterCurveSelectionOutlineRenderer.sublevelHostForCurve(level, bc);
        }
        if ((samples = CoasterCurveSelectionOutlineRenderer.buildSamples(level, bc, sublevelAnchor, partialTick)).size() < 2) {
            return;
        }
        ms.pushPose();
        ms.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer lines = buffer.getBuffer(RenderType.lines());
        Matrix4f pose = ms.last().pose();
        PoseStack.Pose transform = ms.last();
        for (int i = 1; i < samples.size(); ++i) {
            OutlineSample prev = samples.get(i - 1);
            OutlineSample curr = samples.get(i);
            for (double[] corner : LOCAL_CORNERS) {
                Vec3 a = CoasterCurveSelectionOutlineRenderer.cornerWorld(prev, corner[0], corner[1]);
                Vec3 b = CoasterCurveSelectionOutlineRenderer.cornerWorld(curr, corner[0], corner[1]);
                CoasterCurveSelectionOutlineRenderer.drawLine(lines, pose, transform, a, b, outlineR, outlineG, outlineB, outlineA);
            }
        }
        CoasterCurveSelectionOutlineRenderer.drawEndCapRing(lines, pose, transform, samples.getFirst(), outlineR, outlineG, outlineB, outlineA);
        CoasterCurveSelectionOutlineRenderer.drawEndCapRing(lines, pose, transform, samples.getLast(), outlineR, outlineG, outlineB, outlineA);
        ms.popPose();
    }

    private CoasterCurveSelectionOutlineRenderer() {
    }

    @Nullable
    public static BlockPos sublevelHostForCurve(Level level, BezierConnection bc) {
        BlockPos first = (BlockPos)bc.bePositions.getFirst();
        if (CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, first)) {
            return first;
        }
        BlockPos second = (BlockPos)bc.bePositions.getSecond();
        if (CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, second)) {
            return second;
        }
        return null;
    }

    private static void drawEndCapRing(VertexConsumer lines, Matrix4f pose, PoseStack.Pose transform, OutlineSample sample, float outlineR, float outlineG, float outlineB, float outlineA) {
        for (int c = 0; c < LOCAL_CORNERS.length; ++c) {
            double[] a = LOCAL_CORNERS[c];
            double[] b = LOCAL_CORNERS[(c + 1) % LOCAL_CORNERS.length];
            CoasterCurveSelectionOutlineRenderer.drawLine(lines, pose, transform, CoasterCurveSelectionOutlineRenderer.cornerWorld(sample, a[0], a[1]), CoasterCurveSelectionOutlineRenderer.cornerWorld(sample, b[0], b[1]), outlineR, outlineG, outlineB, outlineA);
        }
    }

    private static Vec3 cornerWorld(OutlineSample sample, double lateral, double vertical) {
        return sample.center().add(sample.right().scale(lateral)).add(sample.up().scale(vertical));
    }

    private static void drawLine(VertexConsumer lines, Matrix4f pose, PoseStack.Pose transform, Vec3 a, Vec3 b, float outlineR, float outlineG, float outlineB, float outlineA) {
        float dx = (float)(b.x - a.x);
        float dy = (float)(b.y - a.y);
        float dz = (float)(b.z - a.z);
        float len = Mth.sqrt((float)(dx * dx + dy * dy + dz * dz));
        if (len < 1.0E-6f) {
            return;
        }
        lines.addVertex(pose, (float)a.x, (float)a.y, (float)a.z).setColor(outlineR, outlineG, outlineB, outlineA).setNormal(transform.copy(), dx /= len, dy /= len, dz /= len);
        lines.addVertex(pose, (float)b.x, (float)b.y, (float)b.z).setColor(outlineR, outlineG, outlineB, outlineA).setNormal(transform.copy(), dx, dy, dz);
    }

    private static List<OutlineSample> buildSamples(Level level, BezierConnection bc, @Nullable BlockPos sublevelAnchor, float partialTick) {
        @Nullable Pose3dc subRenderPose = sublevelAnchor == null ? null : CoasterAnchorClientSpace.renderPoseForAnchor(level, sublevelAnchor, partialTick);
        int actualSegCount = Math.min(bc.getSegmentCount(), 256);
        boolean useIterator = actualSegCount >= 16;
        int fallbackCount = Mth.clamp((int)Math.max(16, (int)(bc.getLength() * 2.0)), (int)8, (int)64);
        ArrayList<OutlineSample> samples = new ArrayList<OutlineSample>(useIterator ? actualSegCount + 3 : fallbackCount + 3);
        Vec3 prevLat = null;
        if (useIterator) {
            for (BezierConnection.Segment segment : bc) {
                float segT = bc.getSegmentT(segment.index);
                Vec3 lateral = CoasterBezierRailFrames.lateralAt(bc, segT, segment.derivative, level);
                if (prevLat != null && lateral.dot(prevLat) < 0.0) {
                    lateral = lateral.scale(-1.0);
                }
                prevLat = lateral;
                PlotFrame frame = CoasterCurveSelectionOutlineRenderer.plotFrameAtIteratorSample(bc, segment, segT, lateral, level);
                samples.add(CoasterCurveSelectionOutlineRenderer.toRenderSample(subRenderPose, frame));
            }
        } else {
            Vec3 end1 = (Vec3)bc.starts.getFirst();
            Vec3 end2 = (Vec3)bc.starts.getSecond();
            Vec3 finish1 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 0);
            Vec3 finish2 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 1);
            for (int i = 0; i <= fallbackCount; ++i) {
                float t = (float)i / (float)fallbackCount;
                Vec3 tan = VecHelper.bezierDerivative((Vec3)end1, (Vec3)end2, (Vec3)finish1, (Vec3)finish2, (float)t);
                if (tan.lengthSqr() < 1.0E-12) {
                    tan = CoasterBezierRailFrames.unitTangentAt(bc, t);
                }
                Vec3 lateral = CoasterBezierRailFrames.lateralAt(bc, t, tan, level);
                if (prevLat != null && lateral.dot(prevLat) < 0.0) {
                    lateral = lateral.scale(-1.0);
                }
                prevLat = lateral;
                PlotFrame frame = CoasterCurveSelectionOutlineRenderer.plotFrameAtParameter(VecHelper.bezier((Vec3)end1, (Vec3)end2, (Vec3)finish1, (Vec3)finish2, (float)t).add(CoasterCurveSelectionOutlineRenderer.interactionLiftOffset(bc, t, tan, lateral, level)), tan, lateral);
                samples.add(CoasterCurveSelectionOutlineRenderer.toRenderSample(subRenderPose, frame));
            }
        }
        CoasterCurveSelectionOutlineRenderer.appendOpenEndOutlineExtensions(level, bc, samples);
        return samples;
    }

    private static PlotFrame plotFrameAtIteratorSample(BezierConnection bc, BezierConnection.Segment segment, float segT, Vec3 lateral, Level level) {
        Vec3 plotCenter = CoasterCurveSelectionOutlineRenderer.iteratorSpinePlotLocal(bc, segment.position).add(CoasterCurveSelectionOutlineRenderer.interactionLiftOffset(bc, segT, segment.derivative, lateral, level));
        return CoasterCurveSelectionOutlineRenderer.plotFrameAtParameter(plotCenter, segment.derivative, lateral);
    }

    private static PlotFrame plotFrameAtParameter(Vec3 plotCenter, Vec3 tangent, Vec3 lateral) {
        Vec3 lat = lateral.normalize();
        Vec3 tan = tangent.lengthSqr() < 1.0E-12 ? lat : tangent.normalize();
        Vec3 upRaw = tan.cross(lat);
        Vec3 up = upRaw.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : upRaw.normalize();
        return new PlotFrame(plotCenter, lat, up);
    }

    private static Vec3 interactionLiftOffset(BezierConnection bc, float segT, Vec3 tangent, Vec3 lateral, Level level) {
        PlotFrame frame = CoasterCurveSelectionOutlineRenderer.plotFrameAtParameter(Vec3.ZERO, tangent, lateral);
        if (frame.up().lengthSqr() < 1.0E-12) {
            return CoasterBezierRailFrames.curveInteractionLiftOffset(bc, segT, level);
        }
        return frame.up().scale((double)CoasterTrackGauge.curveInteractionLift()).add(0.0, -0.125, 0.0);
    }

    private static Vec3 iteratorSpinePlotLocal(BezierConnection bc, Vec3 iteratorPosition) {
        return iteratorPosition.add(Vec3.atLowerCornerOf((Vec3i)((Vec3i)bc.bePositions.getFirst()))).add(0.0, -0.1875, 0.0);
    }

    private static OutlineSample toRenderSample(@Nullable Pose3dc subRenderPose, PlotFrame plot) {
        Vec3 center = CoasterAnchorClientSpace.toRenderWorld(subRenderPose, plot.center());
        Vec3 lat = CoasterAnchorClientSpace.toRenderDirection(subRenderPose, plot.right());
        Vec3 up = CoasterAnchorClientSpace.toRenderDirection(subRenderPose, plot.up());
        return new OutlineSample(center, lat, up);
    }

    private static void appendOpenEndOutlineExtensions(Level level, BezierConnection bc, List<OutlineSample> samples) {
        Vec3 chord;
        float ext;
        if (samples.size() < 2) {
            return;
        }
        OutlineSample start = samples.getFirst();
        OutlineSample end = samples.getLast();
        if (CoasterCurveSelectionOutlineRenderer.isOpenCurveEnd(level, bc, true)) {
            ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, true);
            chord = start.center().subtract(samples.get(1).center());
            if (chord.lengthSqr() > 1.0E-12) {
                samples.addFirst(new OutlineSample(start.center().add(chord.normalize().scale((double)ext)), start.right(), start.up()));
            }
        }
        if (CoasterCurveSelectionOutlineRenderer.isOpenCurveEnd(level, bc, false)) {
            ext = CoasterOpenEndExtension.extensionBlocksForCurveEndpoint(level, bc, false);
            chord = end.center().subtract(samples.get(samples.size() - 2).center());
            if (chord.lengthSqr() > 1.0E-12) {
                samples.add(new OutlineSample(end.center().add(chord.normalize().scale((double)ext)), end.right(), end.up()));
            }
        }
    }

    private static boolean isOpenCurveEnd(Level level, BezierConnection bc, boolean atFirstEndpoint) {
        BlockPos anchor = atFirstEndpoint ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
        BlockEntity blockEntity = level.getBlockEntity(anchor);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        return be.legCount() < 2;
    }

    private record OutlineSample(Vec3 center, Vec3 right, Vec3 up) {
    }

    private record PlotFrame(Vec3 center, Vec3 right, Vec3 up) {
    }
}

