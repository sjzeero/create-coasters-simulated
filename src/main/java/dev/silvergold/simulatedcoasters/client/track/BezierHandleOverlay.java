/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.simibubi.create.AllItems
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Vector3f
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleArmTextures;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleDragManager;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleEditMode;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleLiftTextures;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleOverlayRenderTypes;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleTangentTextures;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleTiltTextures;
import dev.silvergold.simulatedcoasters.client.track.CoasterAnchorClientSpace;
import dev.silvergold.simulatedcoasters.client.track.EndpointHandleTextures;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BezierHandleOverlay {
    private static final float LINE_ALPHA = 0.98f;
    private static final float BOX_ALPHA = 0.98f;

    private BezierHandleOverlay() {
    }

    public static void renderWrenchRangeCurves(Minecraft mc, PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation) {
        if (mc.player == null || mc.options.hideGui || mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }
        if (!AllItems.WRENCH.isIn(mc.player.getMainHandItem()) && !AllItems.WRENCH.isIn(mc.player.getOffhandItem())) {
            return;
        }
        if (!BezierHandleEditMode.isActive()) {
            return;
        }
        BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
        if (editAnchor == null) {
            return;
        }
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        ArrayList<PendingArmBillboard> pendingArmBillboards = new ArrayList<PendingArmBillboard>();
        ArrayList<PendingTangentBillboard> pendingTangentBillboards = new ArrayList<PendingTangentBillboard>();
        ArrayList<PendingLiftBillboard> pendingLiftBillboards = new ArrayList<PendingLiftBillboard>();
        ArrayList<PendingTiltBillboard> pendingTiltBillboards = new ArrayList<PendingTiltBillboard>();
        ArrayList<PendingOpenEndpointBillboard> pendingOpenEndpointBillboards = new ArrayList<PendingOpenEndpointBillboard>();
        HashSet<String> seen = new HashSet<String>();
        boolean draggingTangent = BezierHandleDragManager.isDraggingTangentHandle();
        boolean draggingTilt = BezierHandleDragManager.isDraggingTiltHandle();
        boolean draggingLift = BezierHandleDragManager.isDraggingLiftHandle();
        boolean draggingOpenEndpoint = BezierHandleDragManager.isDraggingOpenEndpointHandle();
        boolean isolateHandles = draggingTangent || draggingTilt || draggingLift || draggingOpenEndpoint;
        boolean drawTangentHandles = !isolateHandles || draggingTangent;
        boolean drawTiltHandle = !isolateHandles || draggingTilt;
        boolean drawLiftHandle = !isolateHandles || draggingLift;
        boolean drawOpenEndpointHandle = !isolateHandles || draggingOpenEndpoint;
        for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
            if (anchor.getLevel() != level || anchor.isRemoved() || !editAnchor.equals((Object)anchor.getBlockPos())) continue;
            BlockPos ap = anchor.getBlockPos();
            for (BezierConnection bcRaw : anchor.getAnchorPeerCurvesView().values()) {
                BezierConnection rootSource;
                BezierConnection bc;
                if (bcRaw == null) continue;
                BezierConnection bezierConnection = bc = bcRaw.isPrimary() ? bcRaw : bcRaw.secondary();
                if (!bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id) || !seen.add(BezierHandleOverlay.edgeKey(bc))) continue;
                String ek = BezierHandleOverlay.edgeKey(bc);
                BezierConnection axisSource = BezierHandleDragManager.previewCurveOrWorld(ek, bc);
                BezierConnection bezierConnection2 = rootSource = draggingTangent ? bc : axisSource;
                if (!drawTangentHandles) continue;
                BezierHandleOverlay.renderCurve(mc, pendingArmBillboards, pendingTangentBillboards, rootSource, axisSource, ek, ap);
            }
            if (drawTiltHandle && CoasterBezierHandleEdit.anchorEligibleForTiltEdit((Level)level, ap)) {
                BezierHandleOverlay.renderJointTiltHandle(anchor, ap, pendingTiltBillboards);
            }
            if (drawLiftHandle && CoasterBezierHandleEdit.anchorEligibleForLiftEdit((Level)level, ap)) {
                BezierHandleOverlay.renderAnchorLiftHandle(mc, ms, cameraPos, cameraRotation, anchor, ap, pendingLiftBillboards);
            }
            if (drawOpenEndpointHandle && anchor.legCount() == 1) {
                BezierHandleOverlay.renderOpenEndpointHandle(mc, (Level)level, ap, pendingArmBillboards, pendingOpenEndpointBillboards);
                continue;
            }
            if (!draggingTangent || anchor.legCount() != 1 || !BezierHandleDragManager.isDraggingTangentOnAnchor(ap)) continue;
            BezierHandleOverlay.renderOpenEndExtensionJunctionCapDuringTangentDrag(mc, (Level)level, ap, pendingArmBillboards);
        }
        BezierHandleOverlay.flushArmBillboardsByTexture(ms, buffer, cameraPos, cameraRotation, pendingArmBillboards);
        BezierHandleOverlay.flushTangentBillboardsByTexture(ms, buffer, cameraPos, cameraRotation, pendingTangentBillboards);
        BezierHandleOverlay.flushOpenEndpointBillboardsByTexture(ms, buffer, cameraPos, cameraRotation, pendingOpenEndpointBillboards);
        BezierHandleOverlay.flushLiftBillboardsByTexture(ms, buffer, cameraPos, cameraRotation, pendingLiftBillboards);
        BezierHandleOverlay.flushTiltHandlesByTexture(ms, buffer, cameraPos, cameraRotation, pendingTiltBillboards);
    }

    private static void flushArmBillboardsByTexture(PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation, List<PendingArmBillboard> pending) {
        for (ResourceLocation tex : new ResourceLocation[]{BezierHandleArmTextures.DEFAULT, BezierHandleArmTextures.HOVER, BezierHandleArmTextures.DRAGGING}) {
            VertexConsumer bb = null;
            HashSet<Long> junctionCapped = new HashSet<Long>();
            for (PendingArmBillboard p : pending) {
                long capKey;
                if (!p.texture().equals((Object)tex)) continue;
                if (bb == null) {
                    bb = buffer.getBuffer(BezierHandleOverlayRenderTypes.tangentHandleBillboard(tex));
                }
                BezierHandleOverlay.billboardArmStrip(ms, bb, cameraPos, cameraRotation, p.tailWorld(), p.tipWorld(), p.r(), p.g(), p.b(), p.a());
                if (!p.capTail() || !junctionCapped.add(capKey = BezierHandleOverlay.junctionCapKey(p.tailWorld()))) continue;
                BezierHandleOverlay.billboardArmJunctionCap(ms, bb, cameraPos, cameraRotation, p.tailWorld(), p.r(), p.g(), p.b(), p.a());
            }
        }
    }

    private static long junctionCapKey(Vec3 junctionWorld) {
        return BlockPos.containing((Position)junctionWorld).asLong();
    }

    private static void flushOpenEndpointBillboardsByTexture(PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation, List<PendingOpenEndpointBillboard> pending) {
        for (ResourceLocation tex : new ResourceLocation[]{EndpointHandleTextures.DEFAULT, EndpointHandleTextures.HOVER, EndpointHandleTextures.DRAGGING}) {
            VertexConsumer bb = null;
            for (PendingOpenEndpointBillboard p : pending) {
                if (!p.texture().equals((Object)tex)) continue;
                if (bb == null) {
                    bb = buffer.getBuffer(BezierHandleOverlayRenderTypes.tangentHandleBillboard(tex));
                }
                BezierHandleOverlay.billboardTangentQuad(ms, bb, cameraPos, cameraRotation, p.tipWorld(), p.r(), p.g(), p.b(), p.a());
            }
        }
    }

    private static void flushTangentBillboardsByTexture(PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation, List<PendingTangentBillboard> pending) {
        for (ResourceLocation tex : new ResourceLocation[]{BezierHandleTangentTextures.DEFAULT, BezierHandleTangentTextures.HOVER, BezierHandleTangentTextures.DRAGGING}) {
            VertexConsumer bb = null;
            for (PendingTangentBillboard p : pending) {
                if (!p.texture().equals((Object)tex)) continue;
                if (bb == null) {
                    bb = buffer.getBuffer(BezierHandleOverlayRenderTypes.tangentHandleBillboard(tex));
                }
                BezierHandleOverlay.billboardTangentQuad(ms, bb, cameraPos, cameraRotation, p.tipWorld(), p.r(), p.g(), p.b(), p.a());
            }
        }
    }

    private static void flushLiftBillboardsByTexture(PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation, List<PendingLiftBillboard> pending) {
        for (ResourceLocation tex : new ResourceLocation[]{BezierHandleLiftTextures.DEFAULT, BezierHandleLiftTextures.HOVER, BezierHandleLiftTextures.DRAGGING}) {
            VertexConsumer bb = null;
            for (PendingLiftBillboard p : pending) {
                if (!p.texture().equals((Object)tex)) continue;
                if (bb == null) {
                    bb = buffer.getBuffer(BezierHandleOverlayRenderTypes.tangentHandleBillboard(tex));
                }
                BezierHandleOverlay.billboardLiftQuad(ms, bb, cameraPos, cameraRotation, p.tipWorld(), p.normalWorld(), p.r(), p.g(), p.b(), p.a());
            }
        }
    }

    private static void flushTiltHandlesByTexture(PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation, List<PendingTiltBillboard> pending) {
        for (ResourceLocation tex : new ResourceLocation[]{BezierHandleTiltTextures.DEFAULT, BezierHandleTiltTextures.HOVER, BezierHandleTiltTextures.DRAGGING}) {
            VertexConsumer bb = null;
            for (PendingTiltBillboard p : pending) {
                if (!p.texture().equals((Object)tex)) continue;
                if (bb == null) {
                    bb = buffer.getBuffer(BezierHandleOverlayRenderTypes.tangentHandleBillboard(tex));
                }
                BezierHandleOverlay.tiltPlanarHandleQuad(ms, bb, cameraPos, cameraRotation, p.tipWorld(), p.normalWorld(), p.tangentIntoWorld(), p.r(), p.g(), p.b(), p.a());
            }
        }
    }

    private static void renderOpenEndpointHandle(Minecraft mc, Level level, BlockPos ap, List<PendingArmBillboard> pendingArmBillboards, List<PendingOpenEndpointBillboard> pendingOpenEndpointBillboards) {
        BlockEntity blockEntity = level.getBlockEntity(ap);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        BezierConnection bc = CoasterOpenEndExtension.primaryCoasterCurveAt(level, ap);
        if (bc == null) {
            return;
        }
        String ek = BezierHandleOverlay.edgeKey(bc);
        BezierConnection draw = BezierHandleDragManager.previewCurveOrWorld(ek, bc);
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 junction = BezierHandleDragManager.openEndpointJunctionRenderWorld(level, ap, draw, pt);
        Vec3 handle = BezierHandleDragManager.openEndpointHandleRenderWorld(level, ap, draw, pt);
        if (junction == null || handle == null) {
            return;
        }
        TangentBillboardStyle armSt = BezierHandleOverlay.openEndpointArmStyle(ap);
        pendingArmBillboards.add(new PendingArmBillboard(junction, handle, armSt.texture(), armSt.r(), armSt.g(), armSt.b(), armSt.a(), false));
        OpenEndpointBillboardStyle st = BezierHandleOverlay.openEndpointBillboardStyle(ap);
        pendingOpenEndpointBillboards.add(new PendingOpenEndpointBillboard(handle, st.texture(), st.r(), st.g(), st.b(), st.a()));
    }

    private static void renderOpenEndExtensionJunctionCapDuringTangentDrag(Minecraft mc, Level level, BlockPos ap, List<PendingArmBillboard> pendingArmBillboards) {
        BezierConnection bc = CoasterOpenEndExtension.primaryCoasterCurveAt(level, ap);
        if (bc == null) {
            return;
        }
        String ek = BezierHandleOverlay.edgeKey(bc);
        Vec3 tailPlot = BezierHandleDragManager.curveStartAtAnchor(bc, ap);
        if (tailPlot == null) {
            return;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)level);
        Vec3 tailWorld = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, ap, tailPlot, pt);
        TangentBillboardStyle armSt = BezierHandleOverlay.tangentArmStyle(mc, ek, ap);
        pendingArmBillboards.add(new PendingArmBillboard(tailWorld, tailWorld, armSt.texture(), armSt.r(), armSt.g(), armSt.b(), armSt.a(), true));
    }

    private static TangentBillboardStyle openEndpointArmStyle(BlockPos anchorBlock) {
        float a = 0.98f;
        if (BezierHandleDragManager.isDraggingOpenEndpointHandle()) {
            return new TangentBillboardStyle(BezierHandleArmTextures.DRAGGING, 1.0f, 1.0f, 1.0f, a);
        }
        if (BezierHandleDragManager.isHoveredOpenEndpointAnchor(anchorBlock)) {
            return new TangentBillboardStyle(BezierHandleArmTextures.HOVER, 1.0f, 1.0f, 1.0f, a);
        }
        return new TangentBillboardStyle(BezierHandleArmTextures.DEFAULT, 1.0f, 1.0f, 1.0f, a);
    }

    private static OpenEndpointBillboardStyle openEndpointBillboardStyle(BlockPos anchorBlock) {
        float a = 0.98f;
        if (BezierHandleDragManager.isDraggingOpenEndpointHandle()) {
            return new OpenEndpointBillboardStyle(EndpointHandleTextures.DRAGGING, 1.0f, 1.0f, 1.0f, a);
        }
        if (BezierHandleDragManager.isHoveredOpenEndpointAnchor(anchorBlock)) {
            return new OpenEndpointBillboardStyle(EndpointHandleTextures.HOVER, 1.0f, 1.0f, 1.0f, a);
        }
        return new OpenEndpointBillboardStyle(EndpointHandleTextures.DEFAULT, 1.0f, 1.0f, 1.0f, a);
    }

    private static void renderJointTiltHandle(CoasterAnchorpointBlockEntity anchor, BlockPos ap, List<PendingTiltBillboard> pendingTiltBillboards) {
        Vec3 tangentInto;
        Vec3 face;
        Vec3 center;
        Level level = anchor.getLevel();
        if (level == null) {
            return;
        }
        BezierHandleDragManager.AnchorFrameOverride override = BezierHandleDragManager.livePreviewedAnchorFrame(ap);
        if (override != null) {
            center = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, ap, override.center());
            face = CoasterAnchorClientSpace.toRenderDirection(level, ap, override.faceNormal());
            tangentInto = CoasterAnchorClientSpace.toRenderDirection(level, ap, override.tangentInto());
        } else {
            List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, anchor);
            if (incident.isEmpty()) {
                return;
            }
            Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), ap);
            if (t == null) {
                return;
            }
            center = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, ap, CoasterAnchorpointBlockEntity.worldCenter(level, ap));
            face = CoasterAnchorClientSpace.toRenderDirection(level, ap, CoasterTrackPlacement.anchorFaceNormalForAxis(level, ap, t));
            tangentInto = CoasterAnchorClientSpace.toRenderDirection(level, ap, t);
        }
        if (face.lengthSqr() < 1.0E-12 || tangentInto.lengthSqr() < 1.0E-12) {
            return;
        }
        face = face.normalize();
        tangentInto = tangentInto.normalize();
        Vec3 tip = center.add(face.scale(0.75));
        TiltBillboardStyle st = BezierHandleOverlay.tiltBillboardStyle(ap);
        pendingTiltBillboards.add(new PendingTiltBillboard(tip, face, tangentInto, st.texture(), st.r(), st.g(), st.b(), st.a()));
    }

    private static TiltBillboardStyle tiltBillboardStyle(BlockPos anchorBlock) {
        float a = 0.98f;
        if (BezierHandleDragManager.previewInvalidDrag() && BezierHandleDragManager.isDraggingTiltOnAnchor(anchorBlock)) {
            return new TiltBillboardStyle(BezierHandleTiltTextures.DEFAULT, 1.0f, 0.22f, 0.22f, a);
        }
        if (BezierHandleDragManager.isDraggingTiltOnAnchor(anchorBlock)) {
            return new TiltBillboardStyle(BezierHandleTiltTextures.DRAGGING, 1.0f, 1.0f, 1.0f, a);
        }
        if (BezierHandleDragManager.isHoveredTiltAnchor(anchorBlock)) {
            return new TiltBillboardStyle(BezierHandleTiltTextures.HOVER, 1.0f, 1.0f, 1.0f, a);
        }
        return new TiltBillboardStyle(BezierHandleTiltTextures.DEFAULT, 1.0f, 1.0f, 1.0f, a);
    }

    private static void renderAnchorLiftHandle(Minecraft mc, PoseStack ms, Vec3 cameraPos, Matrix4f cameraRotation, CoasterAnchorpointBlockEntity anchor, BlockPos ap, List<PendingLiftBillboard> pendingLiftBillboards) {
        Vec3 face;
        Vec3 center;
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        BezierHandleDragManager.AnchorFrameOverride override = BezierHandleDragManager.livePreviewedAnchorFrame(ap);
        if (override != null) {
            center = CoasterAnchorClientSpace.pathCenterlineRenderWorld((Level)level, ap, override.center());
            face = CoasterAnchorClientSpace.toRenderDirection((Level)level, ap, override.faceNormal());
        } else {
            List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves((Level)level, anchor);
            if (incident.isEmpty()) {
                return;
            }
            Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), ap);
            if (t == null) {
                return;
            }
            center = CoasterAnchorClientSpace.pathCenterlineRenderWorld((Level)level, ap, CoasterAnchorpointBlockEntity.worldCenter((Level)level, ap));
            face = CoasterAnchorClientSpace.toRenderDirection((Level)level, ap, CoasterTrackPlacement.anchorFaceNormalForAxis((Level)level, ap, t));
        }
        if (face.lengthSqr() < 1.0E-12) {
            return;
        }
        face = face.normalize();
        Vec3 tip = center.add(face.scale((double)1.4f));
        LiftBillboardStyle st = BezierHandleOverlay.liftBillboardStyle(ap);
        pendingLiftBillboards.add(new PendingLiftBillboard(tip, face, st.texture(), st.r(), st.g(), st.b(), st.a()));
    }

    private static LiftBillboardStyle liftBillboardStyle(BlockPos anchorBlock) {
        float a = 0.98f;
        if (BezierHandleDragManager.previewInvalidDrag() && BezierHandleDragManager.isDraggingLiftOnAnchor(anchorBlock)) {
            return new LiftBillboardStyle(BezierHandleLiftTextures.DEFAULT, 1.0f, 0.22f, 0.22f, a);
        }
        if (BezierHandleDragManager.isDraggingLiftOnAnchor(anchorBlock)) {
            return new LiftBillboardStyle(BezierHandleLiftTextures.DRAGGING, 1.0f, 1.0f, 1.0f, a);
        }
        if (BezierHandleDragManager.isHoveredLiftAnchor(anchorBlock)) {
            return new LiftBillboardStyle(BezierHandleLiftTextures.HOVER, 1.0f, 1.0f, 1.0f, a);
        }
        return new LiftBillboardStyle(BezierHandleLiftTextures.DEFAULT, 1.0f, 1.0f, 1.0f, a);
    }

    private static String edgeKey(BezierConnection bc) {
        long b;
        long a = ((BlockPos)bc.bePositions.getFirst()).asLong();
        return a <= (b = ((BlockPos)bc.bePositions.getSecond()).asLong()) ? a + ":" + b : b + ":" + a;
    }

    private static void renderCurve(Minecraft mc, List<PendingArmBillboard> pendingArmBillboards, List<PendingTangentBillboard> pendingTangentBillboards, BezierConnection rootBc, BezierConnection axisBc, String edgeKey, BlockPos transformAnchor) {
        ClientLevel level = mc.level;
        Vec3 e1 = (Vec3)rootBc.starts.getFirst();
        Vec3 e2 = (Vec3)rootBc.starts.getSecond();
        Vec3 f1 = BezierHandleOverlay.handleControlPointWorld(rootBc, axisBc, 0);
        Vec3 f2 = BezierHandleOverlay.handleControlPointWorld(rootBc, axisBc, 1);
        e1 = CoasterAnchorClientSpace.pathCenterlineRenderWorld((Level)level, transformAnchor, e1);
        e2 = CoasterAnchorClientSpace.pathCenterlineRenderWorld((Level)level, transformAnchor, e2);
        f1 = CoasterAnchorClientSpace.pathCenterlineRenderWorld((Level)level, transformAnchor, f1);
        f2 = CoasterAnchorClientSpace.pathCenterlineRenderWorld((Level)level, transformAnchor, f2);
        BlockPos tailAnchor = (BlockPos)rootBc.bePositions.getFirst();
        TangentBillboardStyle armSt = BezierHandleOverlay.tangentArmStyle(mc, edgeKey, tailAnchor);
        pendingArmBillboards.add(new PendingArmBillboard(e1, f1, armSt.texture(), armSt.r(), armSt.g(), armSt.b(), armSt.a(), !tailAnchor.equals((Object)transformAnchor)));
        TangentBillboardStyle st = BezierHandleOverlay.tangentBillboardStyle(mc, edgeKey, tailAnchor);
        pendingTangentBillboards.add(new PendingTangentBillboard(f1, st.texture(), st.r(), st.g(), st.b(), st.a()));
        tailAnchor = (BlockPos)rootBc.bePositions.getSecond();
        armSt = BezierHandleOverlay.tangentArmStyle(mc, edgeKey, tailAnchor);
        pendingArmBillboards.add(new PendingArmBillboard(e2, f2, armSt.texture(), armSt.r(), armSt.g(), armSt.b(), armSt.a(), !tailAnchor.equals((Object)transformAnchor)));
        st = BezierHandleOverlay.tangentBillboardStyle(mc, edgeKey, tailAnchor);
        pendingTangentBillboards.add(new PendingTangentBillboard(f2, st.texture(), st.r(), st.g(), st.b(), st.a()));
    }

    private static Vec3 handleControlPointWorld(BezierConnection rootBc, BezierConnection axisBc, int endpointIndex) {
        Vec3 start = endpointIndex == 0 ? (Vec3)rootBc.starts.getFirst() : (Vec3)rootBc.starts.getSecond();
        Vec3 axis = endpointIndex == 0 ? (Vec3)axisBc.axes.getFirst() : (Vec3)axisBc.axes.getSecond();
        double len = CoasterPeerCurveHandleLengths.getLengthAtEndpoint(axisBc, endpointIndex);
        if (axis.lengthSqr() < 1.0E-12) {
            return start;
        }
        return start.add(axis.normalize().scale(len));
    }

    private static TangentBillboardStyle tangentArmStyle(Minecraft mc, String edgeKey, BlockPos endpointBlock) {
        float a = 0.98f;
        if (BezierHandleDragManager.invalidTint(edgeKey)) {
            return new TangentBillboardStyle(BezierHandleArmTextures.DEFAULT, 1.0f, 0.22f, 0.22f, a);
        }
        if (BezierHandleDragManager.isDraggingTangentHandle()) {
            return new TangentBillboardStyle(BezierHandleArmTextures.DRAGGING, 1.0f, 1.0f, 1.0f, a);
        }
        if (BezierHandleDragManager.isHoveredEndpoint(mc, edgeKey, endpointBlock)) {
            return new TangentBillboardStyle(BezierHandleArmTextures.HOVER, 1.0f, 1.0f, 1.0f, a);
        }
        return new TangentBillboardStyle(BezierHandleArmTextures.DEFAULT, 1.0f, 1.0f, 1.0f, a);
    }

    private static TangentBillboardStyle tangentBillboardStyle(Minecraft mc, String edgeKey, BlockPos endpointBlock) {
        float a = 0.98f;
        if (BezierHandleDragManager.invalidTint(edgeKey)) {
            return new TangentBillboardStyle(BezierHandleTangentTextures.DEFAULT, 1.0f, 0.22f, 0.22f, a);
        }
        if (BezierHandleDragManager.isDraggingTangentHandle()) {
            return new TangentBillboardStyle(BezierHandleTangentTextures.DRAGGING, 1.0f, 1.0f, 1.0f, a);
        }
        if (BezierHandleDragManager.isHoveredEndpoint(mc, edgeKey, endpointBlock)) {
            return new TangentBillboardStyle(BezierHandleTangentTextures.HOVER, 1.0f, 1.0f, 1.0f, a);
        }
        return new TangentBillboardStyle(BezierHandleTangentTextures.DEFAULT, 1.0f, 1.0f, 1.0f, a);
    }

    private static void billboardArmJunctionCap(PoseStack ms, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 junctionWorld, float r, float g, float bl, float alpha) {
        float h = 0.087749995f;
        BezierHandleOverlay.billboardEyeSpaceQuad(ms, vb, cameraPos, cameraRotation, junctionWorld, h, r, g, bl, alpha);
    }

    private static void billboardTangentQuad(PoseStack ms, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 tipWorld, float r, float g, float bl, float alpha) {
        float h = 0.27f;
        BezierHandleOverlay.billboardEyeSpaceQuad(ms, vb, cameraPos, cameraRotation, tipWorld, h, r, g, bl, alpha);
    }

    private static void billboardEyeSpaceQuad(PoseStack ms, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 centerWorld, float eyeHalfExtent, float r, float g, float bl, float alpha) {
        Vec3 c = BezierHandleOverlay.worldToEye(cameraRotation, cameraPos, centerWorld);
        float cx = (float)c.x;
        float cy = (float)c.y;
        float cz = (float)c.z;
        Matrix4f mat = ms.last().pose();
        vb.addVertex(mat, cx - eyeHalfExtent, cy - eyeHalfExtent, cz).setUv(0.0f, 1.0f).setColor(r, g, bl, alpha);
        vb.addVertex(mat, cx + eyeHalfExtent, cy - eyeHalfExtent, cz).setUv(1.0f, 1.0f).setColor(r, g, bl, alpha);
        vb.addVertex(mat, cx + eyeHalfExtent, cy + eyeHalfExtent, cz).setUv(1.0f, 0.0f).setColor(r, g, bl, alpha);
        vb.addVertex(mat, cx - eyeHalfExtent, cy + eyeHalfExtent, cz).setUv(0.0f, 0.0f).setColor(r, g, bl, alpha);
    }

    private static void billboardArmStrip(PoseStack ms, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 tailWorld, Vec3 tipWorld, float r, float g, float bl, float alpha) {
        Vec3 ref;
        Vec3 delta = tipWorld.subtract(tailWorld);
        double len = delta.length();
        if (len < 1.0E-6) {
            return;
        }
        Vec3 axis = delta.scale(1.0 / len);
        Vec3 mid = tailWorld.add(tipWorld).scale(0.5);
        Vec3 view = cameraPos.subtract(mid);
        double vlen = Math.sqrt(view.lengthSqr());
        if (vlen < 1.0E-9) {
            return;
        }
        Vec3 right = (view = view.scale(1.0 / vlen)).cross(axis);
        if (right.lengthSqr() < 1.0E-10 && (right = (ref = Math.abs(axis.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0)).cross(axis)).lengthSqr() < 1.0E-12) {
            return;
        }
        right = right.normalize();
        float hw = 0.029249998f;
        Matrix4f mat = ms.last().pose();
        Vec3 tailL = tailWorld.subtract(right.scale((double)hw));
        Vec3 tailR = tailWorld.add(right.scale((double)hw));
        Vec3 tipL = tipWorld.subtract(right.scale((double)hw));
        Vec3 tipR = tipWorld.add(right.scale((double)hw));
        BezierHandleOverlay.emitArmVertex(mat, vb, cameraPos, cameraRotation, tailL, 0.0f, 1.0f, r, g, bl, alpha);
        BezierHandleOverlay.emitArmVertex(mat, vb, cameraPos, cameraRotation, tailR, 1.0f, 1.0f, r, g, bl, alpha);
        BezierHandleOverlay.emitArmVertex(mat, vb, cameraPos, cameraRotation, tipR, 1.0f, 0.0f, r, g, bl, alpha);
        BezierHandleOverlay.emitArmVertex(mat, vb, cameraPos, cameraRotation, tipL, 0.0f, 0.0f, r, g, bl, alpha);
    }

    private static void emitArmVertex(Matrix4f mat, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 world, float u, float v, float r, float g, float bl, float alpha) {
        Vec3 eye = BezierHandleOverlay.worldToEye(cameraRotation, cameraPos, world);
        vb.addVertex(mat, (float)eye.x, (float)eye.y, (float)eye.z).setUv(u, v).setColor(r, g, bl, alpha);
    }

    private static void billboardLiftQuad(PoseStack ms, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 tipWorld, Vec3 upWorld, float r, float g, float bl, float alpha) {
        Vec3 ref;
        Vec3 view = cameraPos.subtract(tipWorld);
        double vlen = Math.sqrt(view.lengthSqr());
        if (vlen < 1.0E-9) {
            return;
        }
        view = view.scale(1.0 / vlen);
        Vec3 up = upWorld;
        if (up.lengthSqr() < 1.0E-12) {
            return;
        }
        Vec3 right = view.cross(up = up.normalize());
        if (right.lengthSqr() < 1.0E-10 && (right = (ref = Math.abs(up.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0)).cross(up)).lengthSqr() < 1.0E-12) {
            return;
        }
        Vec3 forward = up.cross(right = right.normalize());
        double fl = Math.sqrt(forward.lengthSqr());
        if (fl < 1.0E-9) {
            return;
        }
        if ((forward = forward.scale(1.0 / fl)).dot(view) < 0.0) {
            right = right.scale(-1.0);
        }
        float scale = 0.9f;
        float hw = 0.35f * scale;
        float hh = 0.35f * scale;
        Matrix4f mat = ms.last().pose();
        BezierHandleOverlay.emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, -hw, -hh, 0.0f, 1.0f, r, g, bl, alpha);
        BezierHandleOverlay.emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, hw, -hh, 1.0f, 1.0f, r, g, bl, alpha);
        BezierHandleOverlay.emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, hw, hh, 1.0f, 0.0f, r, g, bl, alpha);
        BezierHandleOverlay.emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, -hw, hh, 0.0f, 0.0f, r, g, bl, alpha);
    }

    private static void tiltPlanarHandleQuad(PoseStack ms, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 tipWorld, Vec3 normalWorld, Vec3 tangentIntoWorld, float r, float g, float bl, float alpha) {
        Vec3 ref;
        Vec3 up = normalWorld;
        if (up.lengthSqr() < 1.0E-12) {
            return;
        }
        up = up.normalize();
        Vec3 t = tangentIntoWorld;
        if (t.lengthSqr() < 1.0E-12) {
            return;
        }
        Vec3 right = (t = t.normalize()).cross(up);
        if (right.lengthSqr() < 1.0E-10 && (right = (ref = Math.abs(up.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0)).cross(up)).lengthSqr() < 1.0E-12) {
            return;
        }
        right = right.normalize();
        float scale = 0.9f;
        float hw = 0.45f * scale;
        float hh = 0.45f * scale;
        Matrix4f mat = ms.last().pose();
        BezierHandleOverlay.emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, -hw, -hh, 0.0f, 1.0f, r, g, bl, alpha);
        BezierHandleOverlay.emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, hw, -hh, 1.0f, 1.0f, r, g, bl, alpha);
        BezierHandleOverlay.emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, hw, hh, 1.0f, 0.0f, r, g, bl, alpha);
        BezierHandleOverlay.emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, -hw, hh, 0.0f, 0.0f, r, g, bl, alpha);
    }

    private static void emitLiftBillboardVertex(Matrix4f mat, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 tip, Vec3 right, Vec3 up, float dx, float dy, float u, float v, float r, float g, float bl, float alpha) {
        Vec3 world = tip.add(right.scale((double)dx)).add(up.scale((double)dy));
        Vec3 eye = BezierHandleOverlay.worldToEye(cameraRotation, cameraPos, world);
        vb.addVertex(mat, (float)eye.x, (float)eye.y, (float)eye.z).setUv(u, v).setColor(r, g, bl, alpha);
    }

    private static Vec3 worldToEye(Matrix4f cameraRotation, Vec3 cameraPos, Vec3 world) {
        Vector3f v = new Vector3f((float)(world.x - cameraPos.x), (float)(world.y - cameraPos.y), (float)(world.z - cameraPos.z));
        cameraRotation.transformPosition(v);
        return new Vec3((double)v.x, (double)v.y, (double)v.z);
    }

    private record PendingArmBillboard(Vec3 tailWorld, Vec3 tipWorld, ResourceLocation texture, float r, float g, float b, float a, boolean capTail) {
    }

    private record PendingOpenEndpointBillboard(Vec3 tipWorld, ResourceLocation texture, float r, float g, float b, float a) {
    }

    private record PendingTangentBillboard(Vec3 tipWorld, ResourceLocation texture, float r, float g, float b, float a) {
    }

    private record PendingLiftBillboard(Vec3 tipWorld, Vec3 normalWorld, ResourceLocation texture, float r, float g, float b, float a) {
    }

    private record PendingTiltBillboard(Vec3 tipWorld, Vec3 normalWorld, Vec3 tangentIntoWorld, ResourceLocation texture, float r, float g, float b, float a) {
    }

    private record TangentBillboardStyle(ResourceLocation texture, float r, float g, float b, float a) {
    }

    private record OpenEndpointBillboardStyle(ResourceLocation texture, float r, float g, float b, float a) {
    }

    private record TiltBillboardStyle(ResourceLocation texture, float r, float g, float b, float a) {
    }

    private record LiftBillboardStyle(ResourceLocation texture, float r, float g, float b, float a) {
    }
}

