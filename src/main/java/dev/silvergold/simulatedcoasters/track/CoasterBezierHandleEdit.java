/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  dev.engine_room.flywheel.lib.visualization.VisualizationHelper
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.network.chat.TextColor
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterAnchorBezierOptimizer;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterTrackPropagator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
        }
        Vec3 ref = CoasterTrackPlacement.anchorFacingPerpendicularToAxis(level, anchorPos, tangentUnit);
        side = tangentUnit.cross(ref);
        if (side.lengthSqr() < 1.0E-12) {
            return null;
        }
        return side.normalize();
    }

    static Vec3 bankRotatedFaceNormal(Vec3 geometricFaceNormal, Vec3 side, double theta) {
        return geometricFaceNormal.scale(Math.cos(theta)).add(side.scale(Math.sin(theta))).normalize();
    }

    /*
     * WARNING - void declaration
     */
    public static PreviewResult computePreview(Level level, BlockPos primaryHost, BlockPos remoteEnd, int endpointIndex, Vec3 handleOffsetWorld) {
        void var28_33;
        BlockPos oppositeOnLeg;
        BezierConnection bc = CoasterBezierHandleEdit.fetchPrimary(level, primaryHost, remoteEnd);
        if (bc == null || !bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return PreviewResult.invalid();
        }
        if (endpointIndex != 0 && endpointIndex != 1) {
            return PreviewResult.invalid();
        }
        double newLen = Math.sqrt(handleOffsetWorld.lengthSqr());
        if (newLen < 1.0E-8) {
            return PreviewResult.invalid();
        }
        newLen = Math.min(newLen, CoasterPeerCurveHandleLengths.maxHandleLengthForEdge(level, bc));
        Vec3 axisStored = handleOffsetWorld.scale(1.0 / Math.sqrt(handleOffsetWorld.lengthSqr()));
        BlockPos ep = endpointIndex == 0 ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
        HandleAxisClampSpec clampSpec = CoasterBezierHandleEdit.handleAxisClampReference(level, ep, oppositeOnLeg = endpointIndex == 0 ? (BlockPos)bc.bePositions.getSecond() : (BlockPos)bc.bePositions.getFirst());
        if (clampSpec != null) {
            axisStored = CoasterBezierHandleEdit.clampUnitVectorWithinCone(axisStored, clampSpec.reference(), clampSpec.maxAwayRadians());
        }
        double at0 = endpointIndex == 0 ? newLen : CoasterPeerCurveHandleLengths.getLengthAtEndpoint(bc, 0);
        double at1 = endpointIndex == 1 ? newLen : CoasterPeerCurveHandleLengths.getLengthAtEndpoint(bc, 1);
        BlockEntity blockEntity = level.getBlockEntity(ep);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return PreviewResult.invalid();
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        String draggedKey = CoasterBezierHandleEdit.edgeKey(bc);
        BezierConnection neighbor = null;
        Vec3 tEditedBefore = CoasterBezierHandleEdit.tangentIntoFromAnchor(bc, ep);
        for (BezierConnection inc : CoasterBezierHandleEdit.incidentCoasterPrimariesAtAnchor(level, be)) {
            Vec3 u1;
            Vec3 u0;
            Vec3 tN;
            if (CoasterBezierHandleEdit.edgeKey(inc).equals(draggedKey) || (tN = CoasterBezierHandleEdit.tangentIntoFromAnchor(inc, ep)) == null || tEditedBefore == null || !((u0 = tEditedBefore.normalize()).dot(u1 = tN.normalize()) < -0.65)) continue;
            neighbor = inc;
            break;
        }
        BezierConnection edited = CoasterTrackPlacement.curveWithAxisAtEndpointForEdit(level, bc, ep, axisStored);
        BlockPos lo = CoasterPeerCurveHandleLengths.lexMin((BlockPos)edited.bePositions.getFirst(), (BlockPos)edited.bePositions.getSecond());
        double hLo = ((BlockPos)edited.bePositions.getFirst()).equals((Object)lo) ? at0 : at1;
        double hHi = ((BlockPos)edited.bePositions.getFirst()).equals((Object)lo) ? at1 : at0;
        CoasterPeerCurveHandleLengths.putPreviewCanonical(edited, hLo, hHi);
        ArrayList<BezierConnection> all = new ArrayList<BezierConnection>();
        all.add(edited);
        if (neighbor != null) {
            Vec3 intoNeighbor = axisStored.scale(-1.0);
            BezierConnection bezierConnection = CoasterTrackPlacement.curveWithAxisAtEndpointForEdit(level, neighbor, ep, intoNeighbor);
            CoasterPeerCurveHandleLengths.copyEdgeLengths(neighbor, bezierConnection);
            all.add(bezierConnection);
        }
        CoasterBezierHandleEdit.repinLiftedStartsOnBatchAnchors(level, all);
        for (BezierConnection bezierConnection : all) {
            if (!CoasterBezierHandleEdit.edgeKey(bezierConnection).equals(draggedKey)) continue;
            double cLo = ((BlockPos)bezierConnection.bePositions.getFirst()).equals((Object)lo) ? at0 : at1;
            double cHi = ((BlockPos)bezierConnection.bePositions.getFirst()).equals((Object)lo) ? at1 : at0;
            CoasterPeerCurveHandleLengths.putPreviewCanonical(bezierConnection, cLo, cHi);
        }
        all.sort(Comparator.comparing(CoasterBezierHandleEdit::edgeKey));
        boolean ok = true;
        for (BezierConnection c : all) {
            if (CoasterTrackPlacement.validateEditedCoasterCurve(level, c, all)) continue;
            ok = false;
            break;
        }
        if (ok && !CoasterBezierHandleEdit.previewCurvesAtAnchorMeetMinRadius(ep, all)) {
            ok = false;
        }
        BezierConnection bezierConnection = edited;
        for (BezierConnection c : all) {
            if (!CoasterBezierHandleEdit.edgeKey(c).equals(draggedKey)) continue;
            BezierConnection bezierConnection2 = c;
            break;
        }
        return new PreviewResult((BezierConnection)var28_33, List.copyOf(all), ok);
    }

    public static TangentGuideSnapResult snapBezierTangentVirtualTargetWithGuidePlanes(Level level, BlockPos primaryHost, BlockPos remoteEnd, int endpointIndex, Vec3 virtualTargetWorld, boolean snapActive, double snapDistanceBlocks) {
        BlockPos r1;
        BlockPos r0;
        CoasterAnchorpointBlockEntity beHub;
        List<BezierConnection> hubInc;
        boolean nearAf;
        if (!snapActive || snapDistanceBlocks <= 0.0 || endpointIndex != 0 && endpointIndex != 1) {
            return new TangentGuideSnapResult(virtualTargetWorld, 0);
        }
        BezierConnection bc = CoasterBezierHandleEdit.fetchPrimary(level, primaryHost, remoteEnd);
        if (bc == null || !bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return new TangentGuideSnapResult(virtualTargetWorld, 0);
        }
        BlockPos p0 = (BlockPos)bc.bePositions.getFirst();
        BlockPos p1 = (BlockPos)bc.bePositions.getSecond();
        BlockPos edited = endpointIndex == 0 ? p0 : p1;
        BlockPos opposite = endpointIndex == 0 ? p1 : p0;
        Vec3 a = CoasterAnchorpointBlockEntity.worldCenter(level, edited);
        Vec3 b = CoasterAnchorpointBlockEntity.worldCenter(level, opposite);
        ChordGuideAxes setA = CoasterBezierHandleEdit.chordGuideAxesForChordWorld(b.subtract(a));
        if (setA == null) {
            return new TangentGuideSnapResult(virtualTargetWorld, 0);
        }
        double t = snapDistanceBlocks;
        Vec3 relA = virtualTargetWorld.subtract(a);
        double dAt = relA.dot(setA.nTall);
        double dAf = relA.dot(setA.nFlat);
        boolean nearAt = Math.abs(dAt) < t;
        boolean bl = nearAf = Math.abs(dAf) < t;
        if (nearAt && nearAf) {
            return new TangentGuideSnapResult(a.add(setA.u.scale(relA.dot(setA.u))), 2);
        }
        ChordGuideAxes setB = null;
        double dBt = 0.0;
        double dBf = 0.0;
        boolean nearBt = false;
        boolean nearBf = false;
        BlockEntity blockEntity = level.getBlockEntity(edited);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity && (hubInc = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, beHub = (CoasterAnchorpointBlockEntity)blockEntity)).size() >= 2 && !(r0 = CoasterBezierHandleEdit.otherEndpointBlock(hubInc.get(0), edited)).equals((Object)(r1 = CoasterBezierHandleEdit.otherEndpointBlock(hubInc.get(1), edited)))) {
            Vec3 w0 = CoasterAnchorpointBlockEntity.worldCenter(level, r0);
            Vec3 w1 = CoasterAnchorpointBlockEntity.worldCenter(level, r1);
            setB = CoasterBezierHandleEdit.chordGuideAxesForChordWorld(w1.subtract(w0));
            if (setB != null) {
                dBt = relA.dot(setB.nTall);
                dBf = relA.dot(setB.nFlat);
                nearBt = Math.abs(dBt) < t;
                boolean bl2 = nearBf = Math.abs(dBf) < t;
                if (nearBt && nearBf) {
                    return new TangentGuideSnapResult(a.add(setB.u.scale(relA.dot(setB.u))), 2);
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
        if (bestN != null) {
            return new TangentGuideSnapResult(virtualTargetWorld.subtract(bestN.scale(bestD)), 1);
        }
        return new TangentGuideSnapResult(virtualTargetWorld, 0);
    }

    private static BlockPos otherEndpointBlock(BezierConnection c, BlockPos anchor) {
        return ((BlockPos)c.bePositions.getFirst()).equals((Object)anchor) ? (BlockPos)c.bePositions.getSecond() : (BlockPos)c.bePositions.getFirst();
    }

    @Nullable
    private static ChordGuideAxes chordGuideAxesForChordWorld(Vec3 chordVectorWorld) {
        Vec3 worldUp;
        if (chordVectorWorld.lengthSqr() < 1.0E-10) {
            return null;
        }
        Vec3 u = chordVectorWorld.normalize();
        Vec3 nTallRaw = u.cross(worldUp = new Vec3(0.0, 1.0, 0.0));
        Vec3 nTall = nTallRaw.lengthSqr() >= 1.0E-12 ? nTallRaw.normalize() : u.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
        Vec3 nFlatRaw = nTall.cross(u);
        if (nFlatRaw.lengthSqr() < 1.0E-12) {
            return null;
        }
        Vec3 nFlat = nFlatRaw.normalize();
        return new ChordGuideAxes(u, nTall, nFlat);
    }

    @Nullable
    private static HandleAxisClampSpec handleAxisClampReference(Level level, BlockPos edited, BlockPos oppositeOnLeg) {
        BlockPos r1;
        BlockPos r0;
        CoasterAnchorpointBlockEntity beHub;
        List<BezierConnection> hubInc;
        Vec3 a = CoasterAnchorpointBlockEntity.worldCenter(level, edited);
        Vec3 b = CoasterAnchorpointBlockEntity.worldCenter(level, oppositeOnLeg);
        Vec3 legDir = b.subtract(a);
        if (legDir.lengthSqr() < 1.0E-10) {
            return null;
        }
        legDir = legDir.normalize();
        BlockEntity blockEntity = level.getBlockEntity(edited);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity && (hubInc = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, beHub = (CoasterAnchorpointBlockEntity)blockEntity)).size() >= 2 && !(r0 = CoasterBezierHandleEdit.otherEndpointBlock(hubInc.get(0), edited)).equals((Object)(r1 = CoasterBezierHandleEdit.otherEndpointBlock(hubInc.get(1), edited)))) {
            Vec3 w0 = CoasterAnchorpointBlockEntity.worldCenter(level, r0);
            Vec3 w1 = CoasterAnchorpointBlockEntity.worldCenter(level, r1);
            ChordGuideAxes setB = CoasterBezierHandleEdit.chordGuideAxesForChordWorld(w1.subtract(w0));
            if (setB != null) {
                Vec3 ref = setB.u;
                if (ref.dot(legDir) < 0.0) {
                    ref = ref.scale(-1.0);
                }
                return new HandleAxisClampSpec(ref, CoasterTrackGauge.BEZIER_HANDLE_HUB_MAX_AXIS_AWAY_RADIANS);
            }
        }
        return new HandleAxisClampSpec(legDir, CoasterTrackGauge.BEZIER_HANDLE_MAX_AXIS_AWAY_RADIANS);
    }

    private static Vec3 clampUnitVectorWithinCone(Vec3 axis, Vec3 reference, double maxAngleRadians) {
        axis = axis.normalize();
        reference = reference.normalize();
        double maxDot = Math.cos(maxAngleRadians);
        double dot = axis.dot(reference);
        if (dot >= maxDot) {
            return axis;
        }
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

    public static Vec3 anchorLiftDirectionFromCanonicalTangent(Level level, BlockPos anchorPos, Vec3 canonicalTangentInto) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return CoasterAnchorpointBlockEntity.localUp(level, anchorPos);
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        Vec3 t0 = canonicalTangentInto.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : canonicalTangentInto.normalize();
        Vec3 n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t0);
        Vec3 side = CoasterBezierHandleEdit.jointBankSideForAnchor(level, anchorPos, t0, n0);
        if (side == null) {
            return CoasterTrackPlacement.anchorFaceNormalForAxis(level, anchorPos, t0);
        }
        return CoasterBezierHandleEdit.bankRotatedFaceNormal(n0, side, be.getJointBankRadians()).normalize();
    }

    public static Vec3 liftedWorldCenterFromCanonicalTangent(Level level, BlockPos anchorPos, Vec3 canonicalTangentInto) {
        Vec3 base = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return base;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        float lift = be.getLiftBlocks();
        if (Math.abs(lift) < 1.0E-9f) {
            return base;
        }
        return base.add(CoasterBezierHandleEdit.anchorLiftDirectionFromCanonicalTangent(level, anchorPos, canonicalTangentInto).scale((double)lift));
    }

    @Nullable
    static Vec3 canonicalLiftTangentAtHub(Level level, BlockPos anchorPos, List<BezierConnection> batch) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        HashMap<String, BezierConnection> batchAtHub = new HashMap<String, BezierConnection>();
        for (BezierConnection c2 : batch) {
            if (!((BlockPos)c2.bePositions.getFirst()).equals((Object)anchorPos) && !((BlockPos)c2.bePositions.getSecond()).equals((Object)anchorPos)) continue;
            batchAtHub.put(CoasterBezierHandleEdit.edgeKey(c2), c2);
        }
        ArrayList<Object> ordered = new ArrayList<Object>();
        for (BezierConnection committed : CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be)) {
            BezierConnection use = batchAtHub.getOrDefault(CoasterBezierHandleEdit.edgeKey(committed), committed);
            ordered.add(use);
        }
        if (ordered.isEmpty()) {
            ordered.addAll(batchAtHub.values());
            ordered.sort(Comparator.comparingLong(c -> {
                BlockPos remote = ((BlockPos)c.bePositions.getFirst()).equals((Object)anchorPos) ? (BlockPos)c.bePositions.getSecond() : (BlockPos)c.bePositions.getFirst();
                return remote.asLong();
            }));
        }
        if (ordered.isEmpty()) {
            return null;
        }
        return CoasterBezierHandleEdit.tangentIntoFromAnchor((BezierConnection)ordered.get(0), anchorPos);
    }

    public static void repinLiftedStartsOnBatchAnchors(Level level, List<BezierConnection> batch) {
        CoasterAnchorpointBlockEntity be;
        Object object;
        if (batch.isEmpty()) {
            return;
        }
        HashSet<BlockPos> touchedAnchors = new HashSet<BlockPos>();
        for (BezierConnection c : batch) {
            if (level.getBlockState((BlockPos)c.bePositions.getFirst()).getBlock() instanceof CoasterAnchorpointBlock) {
                touchedAnchors.add((BlockPos)c.bePositions.getFirst());
            }
            if (!(level.getBlockState((BlockPos)c.bePositions.getSecond()).getBlock() instanceof CoasterAnchorpointBlock)) continue;
            touchedAnchors.add((BlockPos)c.bePositions.getSecond());
        }
        HashSet<String> batchKeys = new HashSet<String>();
        for (BezierConnection c : batch) {
            batchKeys.add(CoasterBezierHandleEdit.edgeKey(c));
        }
        for (BlockPos anchorPos : touchedAnchors) {
            object = level.getBlockEntity(anchorPos);
            if (!(object instanceof CoasterAnchorpointBlockEntity) || Math.abs((be = (CoasterAnchorpointBlockEntity)((Object)object)).getLiftBlocks()) < 1.0E-9f) continue;
            for (BezierConnection inc : CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be)) {
                String k = CoasterBezierHandleEdit.edgeKey(inc);
                if (batchKeys.contains(k)) continue;
                batch.add(inc);
                batchKeys.add(k);
            }
        }
        for (BlockPos anchorPos : touchedAnchors) {
            Vec3 canonicalTangent;
            object = level.getBlockEntity(anchorPos);
            if (!(object instanceof CoasterAnchorpointBlockEntity) || Math.abs((be = (CoasterAnchorpointBlockEntity)((Object)object)).getLiftBlocks()) < 1.0E-9f || (canonicalTangent = CoasterBezierHandleEdit.canonicalLiftTangentAtHub(level, anchorPos, batch)) == null) continue;
            Vec3 newStart = CoasterBezierHandleEdit.liftedWorldCenterFromCanonicalTangent(level, anchorPos, canonicalTangent);
            for (int i = 0; i < batch.size(); ++i) {
                BezierConnection c = batch.get(i);
                if (!((BlockPos)c.bePositions.getFirst()).equals((Object)anchorPos) && !((BlockPos)c.bePositions.getSecond()).equals((Object)anchorPos)) continue;
                BezierConnection repinned = CoasterTrackPlacement.curveWithStartAtAnchorEndpoint(level, c, anchorPos, newStart);
                CoasterPeerCurveHandleLengths.carryLengthsToReplacement(c, repinned);
                batch.set(i, repinned);
            }
        }
    }

    private static Vec3 storedAxisAtEndpoint(BezierConnection bc, BlockPos endpoint) {
        if (((BlockPos)bc.bePositions.getFirst()).equals((Object)endpoint)) {
            return (Vec3)bc.axes.getFirst();
        }
        if (((BlockPos)bc.bePositions.getSecond()).equals((Object)endpoint)) {
            return (Vec3)bc.axes.getSecond();
        }
        return Vec3.ZERO;
    }

    @Nullable
    public static Vec3 tangentIntoFromAnchor(BezierConnection bc, BlockPos anchorPos) {
        if (((BlockPos)bc.bePositions.getFirst()).equals((Object)anchorPos)) {
            Vec3 a = (Vec3)bc.axes.getFirst();
            return a.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : a.normalize();
        }
        if (((BlockPos)bc.bePositions.getSecond()).equals((Object)anchorPos)) {
            Vec3 a = (Vec3)bc.axes.getSecond();
            return a.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : a.normalize();
        }
        return null;
    }

    public static String edgeKey(BezierConnection bc) {
        long b;
        long a = ((BlockPos)bc.bePositions.getFirst()).asLong();
        return a <= (b = ((BlockPos)bc.bePositions.getSecond()).asLong()) ? a + ":" + b : b + ":" + a;
    }

    @Nullable
    public static BezierConnection fetchPrimary(Level level, BlockPos host, BlockPos remote) {
        BlockEntity be = level.getBlockEntity(host);
        if (be instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity ape = (CoasterAnchorpointBlockEntity)be;
            BezierConnection c = ape.getAnchorPeerCurvesView().get(remote);
            if (c != null && c.isPrimary()) {
                return c;
            }
            return null;
        }
        return null;
    }

    public static void commit(ServerPlayer player, BlockPos primaryHost, BlockPos remoteEnd, int endpointIndex, Vec3 handleOffsetWorld) {
        BlockPos anchorPos;
        ServerLevel level = player.serverLevel();
        int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
        PreviewResult pr = CoasterBezierHandleEdit.computePreview((Level)level, primaryHost, remoteEnd, endpointIndex, handleOffsetWorld);
        if (!pr.valid() || pr.edited() == null) {
            if (pr.edited() != null) {
                player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.track.bezier_handle_radius_invalid"), true);
            }
            return;
        }
        BlockPos blockPos = anchorPos = endpointIndex == 0 ? (BlockPos)pr.edited().bePositions.getFirst() : (BlockPos)pr.edited().bePositions.getSecond();
        if (!(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity)) {
            return;
        }
        if (!player.canInteractWithBlock(anchorPos, (double)maxRange)) {
            return;
        }
        CoasterBezierHandleEdit.applyPreviewResult(level, pr);
    }

    private static void applyPreviewResult(ServerLevel level, PreviewResult pr) {
        CoasterTrackPropagator.runBatchUpdate(level, () -> {
            HashSet<String> done = new HashSet<String>();
            for (BezierConnection curve : pr.allUpdated()) {
                if (!curve.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) continue;
                CoasterBezierHandleEdit.commitOne(level, curve, done);
            }
        });
    }

    private static void commitOne(ServerLevel level, BezierConnection primaryCurve, Set<String> done) {
        String k = CoasterBezierHandleEdit.edgeKey(primaryCurve);
        if (!done.add(k)) {
            return;
        }
        CoasterPeerCurveHandleLengths.commitPreviewLengths(primaryCurve);
        BlockPos a = (BlockPos)primaryCurve.bePositions.getFirst();
        BlockPos b = (BlockPos)primaryCurve.bePositions.getSecond();
        for (BlockPos host : List.of(a, b)) {
            BlockPos peer;
            BlockEntity blockEntity = level.getBlockEntity(host);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity ape = (CoasterAnchorpointBlockEntity)blockEntity;
            BlockPos blockPos = peer = host.equals((Object)a) ? b : a;
            if (!ape.getAnchorPeerCurvesView().containsKey(peer)) continue;
            ape.putAnchorPeerCurve(level, peer, primaryCurve);
            return;
        }
    }

    public static boolean anchorEligibleForTiltEdit(Level level, BlockPos anchorPos) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        return !CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be).isEmpty();
    }

    public static List<BezierConnection> incidentPrimaryCoasterCurves(Level level, CoasterAnchorpointBlockEntity be) {
        ArrayList<BezierConnection> out = new ArrayList<BezierConnection>();
        BlockPos anchorPos = be.getBlockPos();
        for (BezierConnection c2 : be.getAnchorPeerCurvesView().values()) {
            BezierConnection p;
            if (c2 == null) continue;
            BezierConnection bezierConnection = p = c2.isPrimary() ? c2 : c2.secondary();
            if (!p.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) continue;
            out.add(p);
        }
        out.sort(Comparator.comparingLong(c -> {
            BlockPos remote = ((BlockPos)c.bePositions.getFirst()).equals((Object)anchorPos) ? (BlockPos)c.bePositions.getSecond() : (BlockPos)c.bePositions.getFirst();
            return remote.asLong();
        }));
        return out;
    }

    public static List<BezierConnection> incidentCoasterPrimariesAtAnchor(Level level, CoasterAnchorpointBlockEntity be) {
        return CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
    }

    @Nullable
    public static Double computeJointBankRadiansFromVirtualTarget(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
        if (incident.isEmpty()) {
            return null;
        }
        BezierConnection c0 = incident.get(0);
        Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(c0, anchorPos);
        if (t == null) {
            return null;
        }
        t = t.normalize();
        Vec3 center = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
        Vec3 n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t);
        Vec3 side = CoasterBezierHandleEdit.jointBankSideForAnchor(level, anchorPos, t, n0);
        if (side == null) {
            return null;
        }
        Vec3 raw = virtualTargetWorld.subtract(center);
        Vec3 inPlane = raw.subtract(t.scale(raw.dot(t)));
        if (inPlane.lengthSqr() < 1.0E-14) {
            return 0.0;
        }
        inPlane = inPlane.normalize();
        return CoasterBezierHandleEdit.clampJointBankRadians(level, anchorPos, Math.atan2(inPlane.dot(side), inPlane.dot(n0)));
    }

    public static double clampJointBankRadians(double thetaRad) {
        return Mth.clamp((float)((float)thetaRad), (float)-1.5707964f, (float)1.5707964f);
    }

    public static double clampJointBankRadians(Level level, BlockPos anchorPos, double thetaRad) {
        return CoasterBezierHandleEdit.clampJointBankRadiansToNeighborEndpointBanks(level, anchorPos, CoasterBezierHandleEdit.clampJointBankRadians(thetaRad));
    }

    private static Vec3 storedFaceNormalAtEndpoint(BezierConnection bc, BlockPos endpoint) {
        if (((BlockPos)bc.bePositions.getFirst()).equals((Object)endpoint)) {
            return (Vec3)bc.normals.getFirst();
        }
        if (((BlockPos)bc.bePositions.getSecond()).equals((Object)endpoint)) {
            return (Vec3)bc.normals.getSecond();
        }
        return Vec3.ZERO;
    }

    @Nullable
    static Double jointBankRadiansFromStoredEndpointNormal(Level level, BlockPos anchorPos, BezierConnection bc) {
        Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(bc, anchorPos);
        if (t == null) {
            return null;
        }
        t = t.normalize();
        Vec3 stored = CoasterBezierHandleEdit.storedFaceNormalAtEndpoint(bc, anchorPos);
        if (stored.lengthSqr() < 1.0E-12) {
            return null;
        }
        stored = stored.normalize();
        Vec3 n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t);
        Vec3 side = CoasterBezierHandleEdit.jointBankSideForAnchor(level, anchorPos, t, n0);
        if (side == null) {
            return null;
        }
        Vec3 inPlane = stored.subtract(t.scale(stored.dot(t)));
        if (inPlane.lengthSqr() < 1.0E-14) {
            return 0.0;
        }
        inPlane = inPlane.normalize();
        return Math.atan2(inPlane.dot(side), inPlane.dot(n0));
    }

    private static double clampJointBankRadiansToNeighborEndpointBanks(Level level, BlockPos anchorPos, double thetaRad) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return thetaRad;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
        if (incident.isEmpty()) {
            return thetaRad;
        }
        double hardLo = -1.5707963705062866;
        double hardHi = 1.5707963705062866;
        double allowedLo = hardLo;
        double allowedHi = hardHi;
        double minDot = Math.cos(1.5707963705062866);
        boolean hairpinSharedBank = false;
        Vec3 n0Canon = null;
        Vec3 sideCanon = null;
        if (incident.size() == 2) {
            Vec3 t0 = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), anchorPos);
            Vec3 t1 = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(1), anchorPos);
            if (t0 != null && t1 != null && (t0 = t0.normalize()).dot(t1 = t1.normalize()) < -0.65) {
                hairpinSharedBank = true;
                n0Canon = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t0);
                sideCanon = CoasterBezierHandleEdit.jointBankSideForAnchor(level, anchorPos, t0, n0Canon);
            }
        }
        boolean anyConstraint = false;
        for (BezierConnection c : incident) {
            double b;
            double a;
            double[] interval;
            Vec3 side;
            Vec3 n0;
            BlockPos peer;
            Vec3 nRef = CoasterBezierHandleEdit.storedFaceNormalAtEndpoint(c, peer = ((BlockPos)c.bePositions.getFirst()).equals((Object)anchorPos) ? (BlockPos)c.bePositions.getSecond() : (BlockPos)c.bePositions.getFirst());
            if (nRef.lengthSqr() < 1.0E-12) continue;
            nRef = nRef.normalize();
            Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(c, anchorPos);
            if (t == null) continue;
            t = t.normalize();
            if (hairpinSharedBank && n0Canon != null && sideCanon != null) {
                n0 = n0Canon;
                side = sideCanon;
            } else {
                n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t);
                side = CoasterBezierHandleEdit.jointBankSideForAnchor(level, anchorPos, t, n0);
            }
            if (side == null || (interval = CoasterBezierHandleEdit.thetaIntervalForBankDotConstraint(a = n0.dot(nRef), b = side.dot(nRef), minDot, hardLo, hardHi)) == null) continue;
            anyConstraint = true;
            allowedLo = Math.max(allowedLo, interval[0]);
            allowedHi = Math.min(allowedHi, interval[1]);
        }
        if (!anyConstraint || allowedLo > allowedHi) {
            return thetaRad;
        }
        return Mth.clamp((float)((float)thetaRad), (float)((float)allowedLo), (float)((float)allowedHi));
    }

    @Nullable
    private static double[] thetaIntervalForBankDotConstraint(double a, double b, double minDot, double hardLo, double hardHi) {
        double r = Math.sqrt(a * a + b * b);
        if (r < 1.0E-8) {
            return null;
        }
        float ratio = Mth.clamp((float)((float)(minDot / r)), (float)-1.0f, (float)1.0f);
        if (ratio >= 1.0f) {
            return new double[]{hardLo, hardHi};
        }
        double half = Math.acos(ratio);
        double phi = Math.atan2(b, a);
        return new double[]{Math.max(hardLo, phi - half), Math.min(hardHi, phi + half)};
    }

    public static double snapJointBankRadiansToStep(double thetaRad, double stepRad) {
        return (double)Math.round(thetaRad / stepRad) * stepRad;
    }

    public static double snapJointBankRadiansToNearestTiltSnapStep(double thetaRad) {
        return CoasterBezierHandleEdit.clampJointBankRadians(CoasterBezierHandleEdit.snapJointBankRadiansToStep(thetaRad, 0.2617993950843811));
    }

    public static double snapJointBankRadiansToNearestTiltSnapStep(Level level, BlockPos anchorPos, double thetaRad) {
        return CoasterBezierHandleEdit.clampJointBankRadians(level, anchorPos, CoasterBezierHandleEdit.snapJointBankRadiansToStep(thetaRad, 0.2617993950843811));
    }

    @Nullable
    public static Vec3 tiltVirtualTargetWorldForJointBankRadians(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld, double jointBankRadians) {
        double rawDotT;
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
        if (incident.isEmpty()) {
            return null;
        }
        BezierConnection c0 = incident.get(0);
        Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(c0, anchorPos);
        if (t == null) {
            return null;
        }
        t = t.normalize();
        Vec3 center = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
        Vec3 n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t);
        Vec3 side = CoasterBezierHandleEdit.jointBankSideForAnchor(level, anchorPos, t, n0);
        if (side == null) {
            return null;
        }
        Vec3 raw = virtualTargetWorld.subtract(center);
        Vec3 inPlane = raw.subtract(t.scale(rawDotT = raw.dot(t)));
        double magSq = inPlane.lengthSqr();
        if (magSq < 1.0E-14) {
            return virtualTargetWorld;
        }
        double mag = Math.sqrt(magSq);
        Vec3 dirDesired = CoasterBezierHandleEdit.bankRotatedFaceNormal(n0, side, jointBankRadians);
        return center.add(t.scale(rawDotT)).add(dirDesired.scale(mag));
    }

    public static Vec3 jointBankFaceNormalAfterGeometric(Level level, BlockPos anchorPos, Vec3 axisIntoCurve, Vec3 geometricFaceNormal, double theta) {
        Vec3 side;
        Vec3 axis = axisIntoCurve.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axisIntoCurve.normalize();
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return CoasterTrackPlacement.orthonormalFaceNormalForAxis(level, anchorPos, geometricFaceNormal, axis);
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        Vec3 t = axis;
        List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
        if (incident.size() == 2) {
            Vec3 n0Canon;
            Vec3 sideCanon;
            Vec3 t0 = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), anchorPos);
            Vec3 t1 = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(1), anchorPos);
            if (t0 != null && t1 != null && (t0 = t0.normalize()).dot(t1 = t1.normalize()) < -0.65 && (sideCanon = CoasterBezierHandleEdit.jointBankSideForAnchor(level, anchorPos, t0, n0Canon = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t0))) != null) {
                Vec3 nShared = CoasterBezierHandleEdit.bankRotatedFaceNormal(n0Canon, sideCanon, theta);
                return CoasterTrackPlacement.orthonormalFaceNormalForAxis(level, anchorPos, nShared, axis);
            }
        }
        Vec3 n = (side = CoasterBezierHandleEdit.jointBankSideForAnchor(level, anchorPos, t, geometricFaceNormal)) == null ? geometricFaceNormal : CoasterBezierHandleEdit.bankRotatedFaceNormal(geometricFaceNormal, side, theta);
        return CoasterTrackPlacement.orthonormalFaceNormalForAxis(level, anchorPos, n, axis);
    }

    public static PreviewResult computeTiltPreview(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
        Vec3 n0Canonical;
        Double thetaObj = CoasterBezierHandleEdit.computeJointBankRadiansFromVirtualTarget(level, anchorPos, virtualTargetWorld);
        if (thetaObj == null) {
            return PreviewResult.invalid();
        }
        double theta = thetaObj;
        if (!(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity)) {
            return PreviewResult.invalid();
        }
        List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, (CoasterAnchorpointBlockEntity)level.getBlockEntity(anchorPos));
        if (incident.isEmpty()) {
            return PreviewResult.invalid();
        }
        BezierConnection c0 = incident.get(0);
        Vec3 t0 = CoasterBezierHandleEdit.tangentIntoFromAnchor(c0, anchorPos);
        if (t0 == null) {
            return PreviewResult.invalid();
        }
        Vec3 sideCanonical = CoasterBezierHandleEdit.jointBankSideForAnchor(level, anchorPos, t0 = t0.normalize(), n0Canonical = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t0));
        if (sideCanonical == null) {
            return PreviewResult.invalid();
        }
        Vec3 canonicalLiftDir = CoasterBezierHandleEdit.bankRotatedFaceNormal(n0Canonical, sideCanonical, theta);
        float lift = ((CoasterAnchorpointBlockEntity)level.getBlockEntity(anchorPos)).getLiftBlocks();
        Vec3 baseCenter = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
        Vec3 newStart = Math.abs(lift) < 1.0E-9f ? baseCenter : baseCenter.add(canonicalLiftDir.scale((double)lift));
        ArrayList<BezierConnection> all = new ArrayList<BezierConnection>();
        if (incident.size() == 1) {
            BezierConnection u = CoasterTrackPlacement.curveWithStartAndReferenceNormalAtAnchorEndpoint(level, c0, anchorPos, newStart, canonicalLiftDir);
            CoasterPeerCurveHandleLengths.copyEdgeLengths(c0, u);
            all.add(u);
        } else {
            BezierConnection c1 = incident.get(1);
            Vec3 t1 = CoasterBezierHandleEdit.tangentIntoFromAnchor(c1, anchorPos);
            if (t1 == null) {
                return PreviewResult.invalid();
            }
            if (t0.dot(t1 = t1.normalize()) < -0.65) {
                Vec3 nRef = canonicalLiftDir;
                for (BezierConnection c : incident) {
                    BezierConnection u = CoasterTrackPlacement.curveWithStartAndReferenceNormalAtAnchorEndpoint(level, c, anchorPos, newStart, nRef);
                    CoasterPeerCurveHandleLengths.copyEdgeLengths(c, u);
                    all.add(u);
                }
            } else {
                for (BezierConnection c : incident) {
                    Vec3 n0;
                    Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(c, anchorPos);
                    if (t == null) {
                        return PreviewResult.invalid();
                    }
                    Vec3 side = CoasterBezierHandleEdit.jointBankSideForAnchor(level, anchorPos, t = t.normalize(), n0 = CoasterTrackPlacement.anchorGeometricFaceNormal(level, anchorPos, t));
                    if (side == null) {
                        return PreviewResult.invalid();
                    }
                    Vec3 nRef = CoasterBezierHandleEdit.bankRotatedFaceNormal(n0, side, theta);
                    BezierConnection u = CoasterTrackPlacement.curveWithStartAndReferenceNormalAtAnchorEndpoint(level, c, anchorPos, newStart, nRef);
                    CoasterPeerCurveHandleLengths.copyEdgeLengths(c, u);
                    all.add(u);
                }
            }
        }
        boolean ok = true;
        for (BezierConnection c : all) {
            if (CoasterTrackPlacement.validateEditedCoasterCurve(level, c, all)) continue;
            ok = false;
            break;
        }
        if (ok && !CoasterBezierHandleEdit.previewCurvesAtAnchorMeetMinRadius(anchorPos, all)) {
            ok = false;
        }
        return new PreviewResult((BezierConnection)all.get(0), List.copyOf(all), ok);
    }

    public static void commitTilt(ServerPlayer player, BlockPos anchorPos, Vec3 virtualTargetWorld) {
        ServerLevel level = player.serverLevel();
        int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
        if (!player.canInteractWithBlock(anchorPos, (double)maxRange)) {
            return;
        }
        PreviewResult pr = CoasterBezierHandleEdit.computeTiltPreview((Level)level, anchorPos, virtualTargetWorld);
        if (!pr.valid() || pr.edited() == null) {
            return;
        }
        Double thetaObj = CoasterBezierHandleEdit.computeJointBankRadiansFromVirtualTarget((Level)level, anchorPos, virtualTargetWorld);
        if (thetaObj == null) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        float theta = (float)CoasterBezierHandleEdit.clampJointBankRadians((Level)level, anchorPos, thetaObj);
        be.setJointBankRadians(theta);
        ArrayList<BezierConnection> refreshed = new ArrayList<BezierConnection>();
        for (BezierConnection c : CoasterBezierHandleEdit.incidentPrimaryCoasterCurves((Level)level, be)) {
            Vec3 axis = CoasterBezierHandleEdit.storedAxisAtEndpoint(c, anchorPos);
            BezierConnection r = CoasterTrackPlacement.curveWithAxisAtEndpointForEdit((Level)level, c, anchorPos, axis);
            CoasterPeerCurveHandleLengths.copyEdgeLengths(c, r);
            refreshed.add(r);
        }
        refreshed.sort(Comparator.comparing(CoasterBezierHandleEdit::edgeKey));
        boolean ok = true;
        for (BezierConnection c : refreshed) {
            if (CoasterTrackPlacement.validateEditedCoasterCurve((Level)level, c, refreshed)) continue;
            ok = false;
            break;
        }
        if (ok && !CoasterBezierHandleEdit.previewCurvesAtAnchorMeetMinRadius(anchorPos, refreshed)) {
            ok = false;
        }
        if (!ok) {
            be.resetJointBank();
            player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.track.bezier_tilt_invalid"), true);
            return;
        }
        CoasterBezierHandleEdit.applyPreviewResult(level, new PreviewResult((BezierConnection)refreshed.get(0), List.copyOf(refreshed), true));
        CoasterBezierHandleEdit.displayJointBankDegreesActionBarSuccess(player, be.getJointBankRadians());
    }

    public static boolean anchorEligibleForLiftEdit(Level level, BlockPos anchorPos) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        return !CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be).isEmpty();
    }

    @Nullable
    public static Float computeLiftBlocksFromVirtualTarget(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
        if (!(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        Vec3 base = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
        Vec3 dir = CoasterAnchorpointBlockEntity.anchorLiftDirection(level, anchorPos);
        double along = virtualTargetWorld.subtract(base).dot(dir);
        double lift = along - (double)1.4f;
        if (lift < 0.25) {
            lift = 0.25;
        }
        if (lift > 2.5) {
            lift = 2.5;
        }
        return Float.valueOf((float)lift);
    }

    public static float snapLiftBlocksToNearestHalf(float liftBlocks) {
        float clamped = Mth.clamp((float)liftBlocks, (float)0.25f, (float)2.5f);
        if (clamped < 0.5f) {
            return 0.25f;
        }
        return Mth.clamp((float)((float)Math.round(clamped * 2.0f) / 2.0f), (float)0.25f, (float)2.5f);
    }

    @Nullable
    public static Vec3 liftVirtualTargetWorldForStoredLift(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld, float liftBlocks) {
        if (!(level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        Vec3 base = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorPos);
        Vec3 dir = CoasterAnchorpointBlockEntity.anchorLiftDirection(level, anchorPos);
        if (dir.lengthSqr() < 1.0E-20) {
            return virtualTargetWorld;
        }
        dir = dir.normalize();
        double desiredAlong = liftBlocks + 1.4f;
        double rawAlong = virtualTargetWorld.subtract(base).dot(dir);
        return virtualTargetWorld.add(dir.scale(desiredAlong - rawAlong));
    }

    public static PreviewResult computeLiftPreview(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
        Float liftObj = CoasterBezierHandleEdit.computeLiftBlocksFromVirtualTarget(level, anchorPos, virtualTargetWorld);
        if (liftObj == null) {
            return PreviewResult.invalid();
        }
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return PreviewResult.invalid();
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
        if (incident.isEmpty()) {
            return PreviewResult.invalid();
        }
        Vec3 newStart = CoasterAnchorpointBlockEntity.worldCenterWithLift(level, anchorPos, liftObj.floatValue());
        ArrayList<BezierConnection> all = new ArrayList<BezierConnection>();
        for (BezierConnection c : incident) {
            Vec3 axis = CoasterBezierHandleEdit.storedAxisAtEndpoint(c, anchorPos);
            BezierConnection u = CoasterTrackPlacement.curveWithStartAndAxisAtEndpointForEdit(level, c, anchorPos, newStart, axis);
            CoasterPeerCurveHandleLengths.copyEdgeLengths(c, u);
            all.add(u);
        }
        boolean ok = true;
        for (BezierConnection c : all) {
            if (CoasterTrackPlacement.validateEditedCoasterCurve(level, c, all)) continue;
            ok = false;
            break;
        }
        if (ok && !CoasterBezierHandleEdit.previewCurvesAtAnchorMeetMinRadius(anchorPos, all)) {
            ok = false;
        }
        return new PreviewResult((BezierConnection)all.get(0), List.copyOf(all), ok);
    }

    public static void commitLift(ServerPlayer player, BlockPos anchorPos, Vec3 virtualTargetWorld) {
        ServerLevel level = player.serverLevel();
        int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
        if (!player.canInteractWithBlock(anchorPos, (double)maxRange)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        Float liftObj = CoasterBezierHandleEdit.computeLiftBlocksFromVirtualTarget((Level)level, anchorPos, virtualTargetWorld);
        if (liftObj == null) {
            return;
        }
        float oldStored = be.getStoredLiftBlocks();
        float newLift = liftObj.floatValue();
        be.setLiftBlocks(newLift);
        ArrayList<BezierConnection> refreshed = new ArrayList<BezierConnection>();
        for (BezierConnection c : CoasterBezierHandleEdit.incidentPrimaryCoasterCurves((Level)level, be)) {
            Vec3 axis = CoasterBezierHandleEdit.storedAxisAtEndpoint(c, anchorPos);
            BezierConnection r = CoasterTrackPlacement.curveWithAxisAtEndpointForEdit((Level)level, c, anchorPos, axis);
            CoasterPeerCurveHandleLengths.copyEdgeLengths(c, r);
            refreshed.add(r);
        }
        refreshed.sort(Comparator.comparing(CoasterBezierHandleEdit::edgeKey));
        boolean ok = true;
        for (BezierConnection c : refreshed) {
            if (CoasterTrackPlacement.validateEditedCoasterCurve((Level)level, c, refreshed)) continue;
            ok = false;
            break;
        }
        if (ok && !CoasterBezierHandleEdit.previewCurvesAtAnchorMeetMinRadius(anchorPos, refreshed)) {
            ok = false;
        }
        if (!ok || refreshed.isEmpty()) {
            if (Float.isNaN(oldStored)) {
                be.clearLift();
            } else {
                be.setLiftBlocks(oldStored);
            }
            player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.track.bezier_lift_invalid"), true);
            return;
        }
        CoasterBezierHandleEdit.applyPreviewResult(level, new PreviewResult((BezierConnection)refreshed.get(0), List.copyOf(refreshed), true));
        CoasterBezierHandleEdit.displayLiftMetersActionBarSuccess(player, be.getLiftBlocks());
    }

    private static boolean previewCurvesAtAnchorMeetMinRadius(BlockPos anchor, List<BezierConnection> previewBatch) {
        boolean anyAtAnchor = false;
        for (BezierConnection c : previewBatch) {
            double h1;
            if (!((BlockPos)c.bePositions.getFirst()).equals((Object)anchor) && !((BlockPos)c.bePositions.getSecond()).equals((Object)anchor)) continue;
            anyAtAnchor = true;
            double h0 = CoasterPeerCurveHandleLengths.getLengthAtEndpoint(c, 0);
            if (CoasterAnchorBezierOptimizer.meetsMinCurveRadiusWithHandleLengths(c, h0, h1 = CoasterPeerCurveHandleLengths.getLengthAtEndpoint(c, 1))) continue;
            return false;
        }
        return anyAtAnchor;
    }

    public static MutableComponent liftMetersComponent(float liftBlocks) {
        return Component.translatable((String)"simulatedcoasters.track.bezier_edit_lift_meters", (Object[])new Object[]{CoasterBezierHandleEdit.formatLiftMetersReadout(liftBlocks)});
    }

    public static MutableComponent jointBankDegreesComponent(double jointBankRadians) {
        return Component.translatable((String)"simulatedcoasters.track.bezier_edit_tilt_degrees", (Object[])new Object[]{CoasterBezierHandleEdit.formatJointBankDegreesReadout(jointBankRadians)});
    }

    public static String formatLiftMetersReadout(float blocks) {
        int end;
        String s = String.format(Locale.ROOT, "%.2f", Float.valueOf(blocks));
        int dot = s.indexOf(46);
        for (end = s.length(); end > dot + 2 && s.charAt(end - 1) == '0'; --end) {
        }
        return s.substring(0, end);
    }

    public static String formatJointBankDegreesReadout(double jointBankRadians) {
        int end;
        double deg = jointBankRadians * 57.2957763671875;
        if (Math.abs(deg) < 0.02) {
            return "0";
        }
        String s = String.format(Locale.ROOT, "%.2f", deg);
        int dot = s.indexOf(46);
        for (end = s.length(); end > dot + 2 && s.charAt(end - 1) == '0'; --end) {
        }
        return s.substring(0, end);
    }

    private static void displayJointBankDegreesActionBarSuccess(ServerPlayer player, double jointBankRadians) {
        player.displayClientMessage((Component)CoasterBezierHandleEdit.jointBankDegreesComponent(jointBankRadians).withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)0xFFDD55))), true);
    }

    private static void displayLiftMetersActionBarSuccess(ServerPlayer player, float liftBlocks) {
        player.displayClientMessage((Component)CoasterBezierHandleEdit.liftMetersComponent(liftBlocks).withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)0xFFDD55))), true);
    }

    public static boolean anchorEligibleForOpenEndExtensionEdit(Level level, BlockPos anchorPos) {
        return CoasterOpenEndExtension.isOpenEndAnchor(level, anchorPos);
    }

    @Nullable
    public static Float computeOpenEndExtensionFromVirtualTarget(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld, boolean applyCenterSnap) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, be);
        if (incident.size() != 1) {
            return null;
        }
        BezierConnection bc = incident.get(0);
        return CoasterOpenEndExtension.extensionBlocksFromVirtualTarget(level, anchorPos, bc, virtualTargetWorld, applyCenterSnap);
    }

    @Nullable
    public static Float computeOpenEndExtensionFromVirtualTarget(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
        return CoasterBezierHandleEdit.computeOpenEndExtensionFromVirtualTarget(level, anchorPos, virtualTargetWorld, true);
    }

    public static OpenEndExtensionPreviewResult computeOpenEndExtensionPreview(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld, boolean applyCenterSnap) {
        Float ext = CoasterBezierHandleEdit.computeOpenEndExtensionFromVirtualTarget(level, anchorPos, virtualTargetWorld, applyCenterSnap);
        if (ext == null) {
            return OpenEndExtensionPreviewResult.invalid();
        }
        return new OpenEndExtensionPreviewResult(ext.floatValue(), true);
    }

    public static OpenEndExtensionPreviewResult computeOpenEndExtensionPreview(Level level, BlockPos anchorPos, Vec3 virtualTargetWorld) {
        return CoasterBezierHandleEdit.computeOpenEndExtensionPreview(level, anchorPos, virtualTargetWorld, true);
    }

    public static void commitOpenEndExtension(ServerPlayer player, BlockPos anchorPos, Vec3 virtualTargetWorld) {
        ServerLevel level = player.serverLevel();
        int maxRange = CoasterTrackGauge.maxCoasterCurvePacketInteractionRangeBlocks();
        if (!player.canInteractWithBlock(anchorPos, (double)maxRange)) {
            return;
        }
        OpenEndExtensionPreviewResult pr = CoasterBezierHandleEdit.computeOpenEndExtensionPreview((Level)level, anchorPos, virtualTargetWorld, false);
        if (!pr.valid()) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        if (be.legCount() != 1) {
            return;
        }
        be.setOpenEndExtensionBlocks(pr.extensionBlocks());
        CoasterTrackPropagator.touch(level, List.of(anchorPos));
        VisualizationHelper.queueUpdate((BlockEntity)be);
        CoasterBezierHandleEdit.displayOpenEndExtensionActionBarSuccess(player, pr.extensionBlocks());
    }

    public static MutableComponent openEndExtensionBlocksComponent(float blocks) {
        return Component.literal((String)CoasterBezierHandleEdit.formatOpenEndExtensionReadout(blocks));
    }

    public static String formatOpenEndExtensionReadout(float blocks) {
        return CoasterBezierHandleEdit.formatLiftMetersReadout(blocks) + " m";
    }

    private static void displayOpenEndExtensionActionBarSuccess(ServerPlayer player, float extensionBlocks) {
        player.displayClientMessage((Component)CoasterBezierHandleEdit.openEndExtensionBlocksComponent(extensionBlocks).withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)0xFFDD55))), true);
    }

    public record PreviewResult(BezierConnection edited, List<BezierConnection> allUpdated, boolean valid) {
        public static PreviewResult invalid() {
            return new PreviewResult(null, List.of(), false);
        }

        public List<BezierConnection> curvesIncidentTo(BlockPos anchor) {
            if (anchor == null || this.allUpdated.isEmpty()) {
                return List.of();
            }
            ArrayList<BezierConnection> out = new ArrayList<BezierConnection>(2);
            for (BezierConnection c : this.allUpdated) {
                if (!((BlockPos)c.bePositions.getFirst()).equals((Object)anchor) && !((BlockPos)c.bePositions.getSecond()).equals((Object)anchor)) continue;
                out.add(c);
            }
            return List.copyOf(out);
        }
    }

    private record HandleAxisClampSpec(Vec3 reference, float maxAwayRadians) {
    }

    public record TangentGuideSnapResult(Vec3 snappedVirtualTarget, int guideAlignmentAxes) {
    }

    private record ChordGuideAxes(Vec3 u, Vec3 nTall, Vec3 nFlat) {
    }

    public record OpenEndExtensionPreviewResult(float extensionBlocks, boolean valid) {
        public static OpenEndExtensionPreviewResult invalid() {
            return new OpenEndExtensionPreviewResult(0.5f, false);
        }
    }
}

