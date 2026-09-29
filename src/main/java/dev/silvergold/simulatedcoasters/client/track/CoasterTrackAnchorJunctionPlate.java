/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.BezierConnection$Segment
 *  com.simibubi.create.content.trains.track.TrackRenderer
 *  com.simibubi.create.foundation.render.SpecialModels
 *  dev.engine_room.flywheel.api.instance.Instance
 *  dev.engine_room.flywheel.api.instance.InstancerProvider
 *  dev.engine_room.flywheel.lib.instance.InstanceTypes
 *  dev.engine_room.flywheel.lib.instance.TransformedInstance
 *  dev.engine_room.flywheel.lib.model.baked.PartialModel
 *  dev.engine_room.flywheel.lib.transform.PoseTransformStack
 *  dev.engine_room.flywheel.lib.transform.TransformStack
 *  net.createmod.catnip.render.CachedBuffers
 *  net.createmod.catnip.render.SuperByteBuffer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4fc
 *  org.joml.Vector4f
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
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorJunctionModel;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

public final class CoasterTrackAnchorJunctionPlate {
    private CoasterTrackAnchorJunctionPlate() {
    }

    public static boolean shouldRenderGirderAt(Level level, BlockPos anchorPos, BezierConnection curve) {
        BezierConnection p;
        if (level == null || curve == null) {
            return false;
        }
        BezierConnection bezierConnection = p = curve.isPrimary() ? curve : curve.secondary();
        if (!CoasterTrackAnchorJunctionPlate.curveMeetsAnchor(p, anchorPos)) {
            return false;
        }
        List<BezierConnection> incident = CoasterTrackAnchorJunctionPlate.incidentCoasterCurvesAt(level, anchorPos);
        if (incident.isEmpty()) {
            return false;
        }
        if (incident.size() == 1) {
            return CoasterBezierHandleEdit.edgeKey(p).equals(CoasterBezierHandleEdit.edgeKey(incident.get(0)));
        }
        if (incident.size() == 2) {
            return CoasterTrackAnchorJunctionPlate.isWinningCurveAmongIncident(p, incident);
        }
        return false;
    }

    public static boolean shouldRenderJunctionTieAt(Level level, BlockPos anchorPos, BezierConnection curve) {
        return CoasterTrackAnchorJunctionPlate.shouldRenderGirderAt(level, anchorPos, curve);
    }

    public static boolean shouldRenderPlateAt(Level level, BlockPos anchorPos, BezierConnection curve) {
        BezierConnection p;
        if (level == null || curve == null) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
        if (anchor.hasChainLiftShaft()) {
            return false;
        }
        BezierConnection bezierConnection = p = curve.isPrimary() ? curve : curve.secondary();
        if (!CoasterTrackAnchorJunctionPlate.curveMeetsAnchor(p, anchorPos)) {
            return false;
        }
        List<BezierConnection> incident = CoasterTrackAnchorJunctionPlate.incidentCoasterCurvesAt(level, anchorPos);
        if (incident.size() != 1) {
            return false;
        }
        return CoasterBezierHandleEdit.edgeKey(p).equals(CoasterBezierHandleEdit.edgeKey(incident.get(0)));
    }

    public static long mixEndpointJunctionTopologySignature(Level level, BezierConnection bc, long hash) {
        if (level == null || bc == null) {
            return hash;
        }
        BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
        for (BlockPos ep : List.of((BlockPos)p.bePositions.getFirst(), (BlockPos)p.bePositions.getSecond())) {
            List<BezierConnection> incident = CoasterTrackAnchorJunctionPlate.incidentCoasterCurvesAt(level, ep);
            hash ^= ep.asLong();
            hash *= 1099511628211L;
            hash ^= (long)incident.size();
            hash *= 1099511628211L;
            BlockEntity blockEntity = level.getBlockEntity(ep);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
            hash ^= anchor.hasChainLiftShaft() ? 1L : 0L;
            hash *= 1099511628211L;
        }
        return hash;
    }

    private static List<BezierConnection> incidentCoasterCurvesAt(Level level, BlockPos anchorPos) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return List.of();
        }
        CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
        return CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, anchor);
    }

    private static boolean curveMeetsAnchor(BezierConnection primary, BlockPos anchorPos) {
        return ((BlockPos)primary.bePositions.getFirst()).equals((Object)anchorPos) || ((BlockPos)primary.bePositions.getSecond()).equals((Object)anchorPos);
    }

    private static boolean isWinningCurveAmongIncident(BezierConnection primary, List<BezierConnection> incident) {
        String winning = null;
        for (BezierConnection c : incident) {
            String k = CoasterBezierHandleEdit.edgeKey(c);
            if (winning != null && k.compareTo(winning) >= 0) continue;
            winning = k;
        }
        return winning != null && CoasterBezierHandleEdit.edgeKey(primary).equals(winning);
    }

    @Nullable
    public static EndpointSpine spineAtEndpoint(BezierConnection bc, BlockPos anchorPos, @Nullable Level level) {
        Vec3 chord;
        BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
        ArrayList<Vec3> pos = new ArrayList<Vec3>();
        ArrayList<Vec3> lat = new ArrayList<Vec3>();
        Vec3 prevLat = null;
        for (BezierConnection.Segment seg : p) {
            pos.add(seg.position);
            float segT = p.getSegmentT(seg.index);
            Vec3 currLat = CoasterBezierRailFrames.lateralAt(p, segT, seg.derivative, level);
            if (prevLat != null && currLat.dot(prevLat) < 0.0) {
                currLat = currLat.scale(-1.0);
            }
            lat.add(currLat);
            prevLat = currLat;
        }
        int n = pos.size();
        if (n < 2) {
            return null;
        }
        boolean atFirst = ((BlockPos)p.bePositions.getFirst()).equals((Object)anchorPos);
        Vec3 vec3 = chord = atFirst ? ((Vec3)pos.get(1)).subtract((Vec3)pos.get(0)) : ((Vec3)pos.get(n - 1)).subtract((Vec3)pos.get(n - 2));
        if (chord.lengthSqr() < 1.0E-12) {
            return null;
        }
        return new EndpointSpine(atFirst ? (Vec3)pos.getFirst() : (Vec3)pos.get(n - 1), atFirst ? (Vec3)lat.getFirst() : (Vec3)lat.getLast(), chord);
    }

    public static Vec3 junctionPlateCenterInHostRelativeSpace(EndpointSpine frame) {
        float modelAnchorY = -0.75390625f;
        Vec3 angles = TrackRenderer.getModelAngles((Vec3)frame.lateral(), (Vec3)frame.chordIntoTrack());
        PoseStack p = new PoseStack();
        ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of((PoseStack)p).translate(frame.spine())).rotateY((float)angles.y)).rotateX((float)angles.x)).rotateZ((float)angles.z)).translate(-0.5f, modelAnchorY, -0.5f);
        Vector4f h = new Vector4f(0.5f, 0.75f, 0.5f, 1.0f);
        p.last().pose().transform(h);
        return new Vec3((double)h.x, (double)h.y, (double)h.z).add(0.0, -0.1875, 0.0);
    }

    public static Vec3 junctionPlateCenterForVisualRoot(EndpointSpine frame) {
        return CoasterTrackAnchorJunctionPlate.junctionPlateCenterInHostRelativeSpace(frame).subtract(0.0, -0.1875, 0.0);
    }

    public static PoseStack platePoseBelowOrigin(EndpointSpine frame) {
        return CoasterTrackAnchorJunctionPlate.connectionPoseBelowOrigin(frame.spine(), frame.lateral(), frame.chordIntoTrack());
    }

    public static PoseStack connectionPoseBelowOrigin(Vec3 spine, Vec3 lateral, Vec3 inward) {
        float modelAnchorY = -0.75390625f;
        Vec3 angles = TrackRenderer.getModelAngles((Vec3)lateral, (Vec3)inward);
        PoseStack pose = new PoseStack();
        ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of((PoseStack)pose).translate(spine)).rotateY((float)angles.y)).rotateX((float)angles.x)).rotateZ((float)angles.z)).translate(-0.5f, modelAnchorY, -0.5f);
        return pose;
    }

    public static PoseStack spineTiePoseBelowOrigin(Vec3 spine, Vec3 lateral, Vec3 chordIntoTrack, float anchorExtraY, float localY) {
        float modelAnchorY = 0.24609375f + anchorExtraY;
        Vec3 angles = TrackRenderer.getModelAngles((Vec3)lateral, (Vec3)chordIntoTrack);
        PoseStack pose = new PoseStack();
        ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of((PoseStack)pose).translate(spine)).rotateY((float)angles.y)).rotateX((float)angles.x)).rotateZ((float)angles.z)).translate(-0.5f, modelAnchorY, -0.5f);
        TransformStack.of((PoseStack)pose).translate(0.5f, localY, 0.5f);
        TransformStack.of((PoseStack)pose).translate(-0.5f, 0.1315f, -0.25f);
        return pose;
    }

    public static PoseStack tiePoseBelowOrigin(EndpointSpine frame, float anchorExtraY, float localY) {
        return CoasterTrackAnchorJunctionPlate.spineTiePoseBelowOrigin(frame.spine(), frame.lateral(), frame.chordIntoTrack(), anchorExtraY, localY);
    }

    public static PoseStack midspanTiePoseBelowOrigin(Vec3 spine, Vec3 lateral, Vec3 chordIntoTrack) {
        return CoasterTrackAnchorJunctionPlate.spineTiePoseBelowOrigin(spine, lateral, chordIntoTrack, 0.0f, -0.1315f);
    }

    public static PoseStack openEndTiePoseBelowOrigin(EndpointSpine frame) {
        return CoasterTrackAnchorJunctionPlate.tiePoseBelowOrigin(frame, -1.0f, 0.8685f);
    }

    public static PoseStack junctionTiePoseBelowOrigin(EndpointSpine frame) {
        return CoasterTrackAnchorJunctionPlate.tiePoseBelowOrigin(frame, 0.0f, -0.1315f);
    }

    @Nullable
    public static TransformedInstance createFlywheelPlate(InstancerProvider instancerProvider, Level level, BezierConnection bc, BlockPos anchorEndpoint, Vec3 visualOrigin) {
        if (!CoasterTrackAnchorJunctionPlate.shouldRenderPlateAt(level, anchorEndpoint, bc)) {
            return null;
        }
        EndpointSpine frame = CoasterTrackAnchorJunctionPlate.spineAtEndpoint(bc, anchorEndpoint, level);
        if (frame == null) {
            return null;
        }
        TransformedInstance[] slot = new TransformedInstance[1];
        instancerProvider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk((PartialModel)CoasterTrackAnchorJunctionModel.TRACK_ANCHORPOINT_CONNECTION)).createInstances((Instance[])slot);
        TransformedInstance inst = slot[0];
        PoseStack root = new PoseStack();
        TransformStack.of((PoseStack)root).translate(visualOrigin.add(0.0, -0.1875, 0.0));
        PoseStack local = CoasterTrackAnchorJunctionPlate.platePoseBelowOrigin(frame);
        inst.setTransform(root).mul(local.last()).setChanged();
        return inst;
    }

    public static void renderBerPlate(Level level, BezierConnection bc, BlockPos anchorEndpoint, PoseStack ms, VertexConsumer vb, BlockPos curveHostPos) {
        if (!CoasterTrackAnchorJunctionPlate.shouldRenderPlateAt(level, anchorEndpoint, bc)) {
            return;
        }
        EndpointSpine frame = CoasterTrackAnchorJunctionPlate.spineAtEndpoint(bc, anchorEndpoint, level);
        if (frame == null) {
            return;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        int light = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)BlockPos.containing((Position)frame.spine()).offset((Vec3i)curveHostPos));
        PoseStack local = CoasterTrackAnchorJunctionPlate.platePoseBelowOrigin(frame);
        ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial((PartialModel)CoasterTrackAnchorJunctionModel.TRACK_ANCHORPOINT_CONNECTION, (BlockState)air).mulPose((Matrix4fc)local.last().pose())).mulNormal((Matrix3fc)local.last().normal())).light(light).renderInto(ms, vb);
    }

    public record EndpointSpine(Vec3 spine, Vec3 lateral, Vec3 chordIntoTrack) {
    }
}

