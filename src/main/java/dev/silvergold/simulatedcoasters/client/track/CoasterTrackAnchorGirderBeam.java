/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackRenderer
 *  com.simibubi.create.foundation.render.SpecialModels
 *  dev.engine_room.flywheel.api.instance.Instance
 *  dev.engine_room.flywheel.api.instance.InstancerProvider
 *  dev.engine_room.flywheel.lib.instance.InstanceTypes
 *  dev.engine_room.flywheel.lib.instance.TransformedInstance
 *  dev.engine_room.flywheel.lib.model.baked.PartialModel
 *  dev.engine_room.flywheel.lib.transform.TransformStack
 *  net.createmod.catnip.render.CachedBuffers
 *  net.createmod.catnip.render.SuperByteBuffer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackRenderer;
import com.simibubi.create.foundation.render.SpecialModels;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorGirderModel;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorJunctionPlate;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class CoasterTrackAnchorGirderBeam {
    private CoasterTrackAnchorGirderBeam() {
    }

    @Nullable
    public static TransformedInstance[] createFlywheelGirders(InstancerProvider instancerProvider, Level level, BezierConnection bc, BlockPos anchorEndpoint, Vec3 visualOrigin) {
        if (!CoasterTrackAnchorJunctionPlate.shouldRenderGirderAt(level, anchorEndpoint, bc)) {
            return null;
        }
        CoasterTrackAnchorJunctionPlate.EndpointSpine frame = CoasterTrackAnchorJunctionPlate.spineAtEndpoint(bc, anchorEndpoint, level);
        if (frame == null) {
            return null;
        }
        BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
        Vec3 curveOrigin = Vec3.atLowerCornerOf((Vec3i)((Vec3i)p.bePositions.getFirst()));
        Vec3 anchorRel = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorEndpoint).subtract(curveOrigin);
        Vec3 plateRel = CoasterTrackAnchorJunctionPlate.junctionPlateCenterInHostRelativeSpace(frame);
        Vec3 dir = plateRel.subtract(anchorRel);
        double lenSq = dir.lengthSqr();
        if (lenSq < 1.0E-10) {
            return null;
        }
        double span = Math.sqrt(lenSq);
        if (span < 0.125) {
            return null;
        }
        dir = dir.scale(1.0 / span);
        int n = CoasterTrackGauge.anchorGirderHybridSegmentCount(span);
        double tierWorldHeight = span / (double)n;
        float scaleY = (float)(tierWorldHeight / 1.0);
        PartialModel girderPartial = CoasterTrackAnchorGirderModel.partialForAnchor(level, anchorEndpoint);
        TransformedInstance[] inst = new TransformedInstance[n];
        instancerProvider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk((PartialModel)girderPartial)).createInstances((Instance[])inst);
        PoseStack root = new PoseStack();
        TransformStack.of((PoseStack)root).translate(visualOrigin);
        Quaternionf q = CoasterTrackAnchorGirderBeam.girderOrientationAlongSpan(dir, frame);
        for (int i = 0; i < n; ++i) {
            PoseStack local = new PoseStack();
            TransformStack.of((PoseStack)local).translate(anchorRel);
            local.mulPose(new Matrix4f().rotation((Quaternionfc)q));
            TransformStack.of((PoseStack)local).translate(0.0f, (float)((double)i * tierWorldHeight), 0.0f);
            TransformStack.of((PoseStack)local).scale(1.0f, scaleY, 1.0f);
            TransformStack.of((PoseStack)local).translate(-0.5f, 0.0f, -0.5f);
            inst[i].setTransform(root).mul(local.last()).setChanged();
        }
        return inst;
    }

    public static void renderBerGirders(Level level, BezierConnection bc, BlockPos anchorEndpoint, BlockPos curveHostPos, PoseStack ms, VertexConsumer vb) {
        if (!CoasterTrackAnchorJunctionPlate.shouldRenderGirderAt(level, anchorEndpoint, bc)) {
            return;
        }
        CoasterTrackAnchorJunctionPlate.EndpointSpine frame = CoasterTrackAnchorJunctionPlate.spineAtEndpoint(bc, anchorEndpoint, level);
        if (frame == null) {
            return;
        }
        BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
        Vec3 curveOrigin = Vec3.atLowerCornerOf((Vec3i)curveHostPos);
        Vec3 anchorRel = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorEndpoint).subtract(curveOrigin);
        Vec3 plateRel = CoasterTrackAnchorJunctionPlate.junctionPlateCenterForVisualRoot(frame);
        Vec3 dir = plateRel.subtract(anchorRel);
        double lenSq = dir.lengthSqr();
        if (lenSq < 1.0E-10) {
            return;
        }
        double span = Math.sqrt(lenSq);
        if (span < 0.125) {
            return;
        }
        dir = dir.scale(1.0 / span);
        int n = CoasterTrackGauge.anchorGirderHybridSegmentCount(span);
        double tierWorldHeight = span / (double)n;
        float scaleY = (float)(tierWorldHeight / 1.0);
        PartialModel girderPartial = CoasterTrackAnchorGirderModel.partialForAnchor(level, anchorEndpoint);
        BlockState air = Blocks.AIR.defaultBlockState();
        int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)anchorEndpoint);
        Quaternionf q = CoasterTrackAnchorGirderBeam.girderOrientationAlongSpan(dir, frame);
        for (int i = 0; i < n; ++i) {
            PoseStack local = new PoseStack();
            TransformStack.of((PoseStack)local).translate(anchorRel);
            local.mulPose(new Matrix4f().rotation((Quaternionfc)q));
            TransformStack.of((PoseStack)local).translate(0.0f, (float)((double)i * tierWorldHeight), 0.0f);
            TransformStack.of((PoseStack)local).scale(1.0f, scaleY, 1.0f);
            TransformStack.of((PoseStack)local).translate(-0.5f, 0.0f, -0.5f);
            ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial((PartialModel)girderPartial, (BlockState)air).mulPose((Matrix4fc)local.last().pose())).mulNormal((Matrix3fc)local.last().normal())).light(light).renderInto(ms, vb);
        }
    }

    private static Quaternionf girderOrientationAlongSpan(Vec3 spanDir, CoasterTrackAnchorJunctionPlate.EndpointSpine frame) {
        double cos;
        Quaternionf yAligned = CoasterTrackAnchorGirderBeam.yAxisToDirection((float)spanDir.x, (float)spanDir.y, (float)spanDir.z);
        Vec3 angles = TrackRenderer.getModelAngles((Vec3)frame.lateral(), (Vec3)frame.chordIntoTrack());
        Quaternionf trackFrame = new Quaternionf().rotateY((float)angles.y).rotateX((float)angles.x).rotateZ((float)angles.z);
        Vector3f trackX = trackFrame.transform(new Vector3f(1.0f, 0.0f, 0.0f));
        Vector3f trackZ = trackFrame.transform(new Vector3f(0.0f, 0.0f, 1.0f));
        Vec3 axis = spanDir.normalize();
        Vec3 targetX = CoasterTrackAnchorGirderBeam.projectOntoPlane(new Vec3((double)trackX.x, (double)trackX.y, (double)trackX.z), axis);
        if (targetX.lengthSqr() < 1.0E-12) {
            targetX = CoasterTrackAnchorGirderBeam.projectOntoPlane(new Vec3((double)trackZ.x, (double)trackZ.y, (double)trackZ.z), axis);
        }
        if (targetX.lengthSqr() < 1.0E-12) {
            return yAligned;
        }
        targetX = targetX.normalize();
        Vector3f modelX = yAligned.transform(new Vector3f(1.0f, 0.0f, 0.0f));
        Vec3 currentX = CoasterTrackAnchorGirderBeam.projectOntoPlane(new Vec3((double)modelX.x, (double)modelX.y, (double)modelX.z), axis);
        if (currentX.lengthSqr() < 1.0E-12) {
            return yAligned;
        }
        double sin = axis.dot((currentX = currentX.normalize()).cross(targetX));
        float twist = (float)Math.atan2(sin, cos = currentX.dot(targetX));
        if (Math.abs(twist) < 1.0E-4f) {
            return yAligned;
        }
        return new Quaternionf().rotationAxis(twist, (float)axis.x, (float)axis.y, (float)axis.z).mul((Quaternionfc)yAligned);
    }

    private static Vec3 projectOntoPlane(Vec3 v, Vec3 planeNormal) {
        Vec3 n = planeNormal.normalize();
        return v.subtract(n.scale(v.dot(n)));
    }

    private static Quaternionf yAxisToDirection(float dx, float dy, float dz) {
        Vector3f from = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f to = new Vector3f(dx, dy, dz);
        if (to.lengthSquared() < 1.0E-12f) {
            return new Quaternionf();
        }
        to.normalize();
        float dot = from.dot((Vector3fc)to);
        if (dot > 0.9999f) {
            return new Quaternionf();
        }
        if (dot < -0.9999f) {
            return new Quaternionf().rotationAxis((float)Math.PI, 1.0f, 0.0f, 0.0f);
        }
        Vector3f axis = new Vector3f((Vector3fc)from).cross((Vector3fc)to);
        if (axis.lengthSquared() < 1.0E-12f) {
            return new Quaternionf();
        }
        axis.normalize();
        float angle = (float)Math.acos(Mth.clamp((float)dot, (float)-1.0f, (float)1.0f));
        return new Quaternionf().rotationAxis(angle, axis.x, axis.y, axis.z);
    }
}

