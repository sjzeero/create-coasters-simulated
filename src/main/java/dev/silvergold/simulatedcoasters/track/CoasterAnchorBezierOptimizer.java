/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackMaterial
 *  net.createmod.catnip.data.Couple
 *  net.createmod.catnip.math.VecHelper
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.CoasterAnchorCoplanarPlane;
import dev.silvergold.simulatedcoasters.track.CoasterBezierEndpointData;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterAnchorBezierOptimizer {
    private CoasterAnchorBezierOptimizer() {
    }

    @Nullable
    public static AnchorAnchorBuildResult buildAnchorAnchorPlacement(Level level, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder) {
        return CoasterAnchorBezierOptimizer.buildAnchorAnchorPlacement(level, anchorA, anchorB, material, girder, false);
    }

    @Nullable
    public static AnchorAnchorBuildResult buildAnchorAnchorPlacement(Level level, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder, boolean suppressNeighborSmoothing) {
        CoasterAnchorpointBlockEntity beLo;
        BlockEntity blockEntity;
        BlockPos hi;
        BlockPos lo;
        block5: {
            block4: {
                long bl;
                long al = anchorA.asLong();
                lo = al <= (bl = anchorB.asLong()) ? anchorA : anchorB;
                hi = al <= bl ? anchorB : anchorA;
                blockEntity = level.getBlockEntity(lo);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block4;
                beLo = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(hi);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block5;
            }
            return null;
        }
        CoasterAnchorpointBlockEntity beHi = (CoasterAnchorpointBlockEntity)blockEntity;
        CoasterAnchorpointBlockEntity.AnchorEndpointTowardPeer epLo = beLo.endpointTowardPeer(level, hi);
        CoasterAnchorpointBlockEntity.AnchorEndpointTowardPeer epHi = beHi.endpointTowardPeer(level, lo);
        if (epLo == null || epHi == null) {
            return null;
        }
        Vec3 startLo = CoasterTrackPlacement.resolvePlacementStartAtHub(level, lo, hi, epLo.worldStart());
        Vec3 startHi = CoasterTrackPlacement.resolvePlacementStartAtHub(level, hi, lo, epHi.worldStart());
        Vec3 axisLo = epLo.axisIntoCurve();
        Vec3 axisHi = epHi.axisIntoCurve();
        AxisOptimizationResult axes = CoasterAnchorBezierOptimizer.optimizeAxesForPlacement(level, lo, hi, startLo, startHi, axisLo, axisHi, epLo.normal(), epHi.normal(), girder, material, suppressNeighborSmoothing, true, true);
        Vec3 optAxisLo = axes.axisLo();
        Vec3 optAxisHi = axes.axisHi();
        LoHiAxes greenfieldAxes = CoasterAnchorBezierOptimizer.applyGreenfieldOpenEndAxisPass(level, lo, hi, beLo, beHi, startLo, startHi, optAxisLo, optAxisHi, epLo.normal(), epHi.normal(), girder, material, suppressNeighborSmoothing);
        optAxisLo = greenfieldAxes.lo();
        optAxisHi = greenfieldAxes.hi();
        BezierConnection bc = new BezierConnection(Couple.create((Object)lo, (Object)hi), Couple.create((Object)startLo, (Object)startHi), Couple.create((Object)optAxisLo.normalize(), (Object)optAxisHi.normalize()), Couple.create((Object)CoasterTrackPlacement.resolvePlacementNormalAtHub(level, lo, hi, optAxisLo), (Object)CoasterTrackPlacement.resolvePlacementNormalAtHub(level, hi, lo, optAxisHi)), true, girder, material);
        double chord = startLo.distanceTo(startHi);
        double h = Math.max(1.0E-4, chord / 3.0);
        CoasterPeerCurveHandleLengths.putCanonical(bc, h, h);
        List<NeighborJoinAxisWrite> writes = suppressNeighborSmoothing ? List.of() : CoasterAnchorBezierOptimizer.collectNeighborJoinWrites(level, axes.optAtLo(), axes.optAtHi(), lo, hi, material, girder);
        return new AnchorAnchorBuildResult(bc, writes);
    }

    @Nullable
    public static AnchorAnchorBuildResult buildAnchorToVirtualPeerPlacement(Level level, BlockPos anchorA, BlockPos virtualB, TrackMaterial material, boolean girder) {
        return CoasterAnchorBezierOptimizer.buildAnchorToVirtualPeerPlacement(level, anchorA, virtualB, material, girder, false);
    }

    @Nullable
    public static AnchorAnchorBuildResult buildAnchorToVirtualPeerPlacement(Level level, BlockPos anchorA, BlockPos virtualB, TrackMaterial material, boolean girder, boolean suppressNeighborSmoothing) {
        BlockEntity blockEntity = level.getBlockEntity(anchorA);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity beReal = (CoasterAnchorpointBlockEntity)blockEntity;
        long al = anchorA.asLong();
        long bl = virtualB.asLong();
        BlockPos lo = al <= bl ? anchorA : virtualB;
        BlockPos hi = al <= bl ? virtualB : anchorA;
        CoasterAnchorpointBlockEntity.AnchorEndpointTowardPeer epAtReal = beReal.endpointTowardPeer(level, virtualB);
        CoasterAnchorpointBlockEntity.AnchorEndpointTowardPeer epAtVirtual = CoasterAnchorpointBlockEntity.endpointTowardPeerAt(level, virtualB, anchorA);
        if (epAtReal == null || epAtVirtual == null) {
            return null;
        }
        CoasterAnchorpointBlockEntity.AnchorEndpointTowardPeer epLo = lo.equals((Object)anchorA) ? epAtReal : epAtVirtual;
        CoasterAnchorpointBlockEntity.AnchorEndpointTowardPeer epHi = hi.equals((Object)anchorA) ? epAtReal : epAtVirtual;
        Vec3 startLo = lo.equals((Object)anchorA) ? CoasterTrackPlacement.resolvePlacementStartAtHub(level, lo, hi, epLo.worldStart()) : epLo.worldStart();
        Vec3 startHi = hi.equals((Object)anchorA) ? CoasterTrackPlacement.resolvePlacementStartAtHub(level, hi, lo, epHi.worldStart()) : epHi.worldStart();
        Vec3 axisLo = epLo.axisIntoCurve();
        Vec3 axisHi = epHi.axisIntoCurve();
        AxisOptimizationResult axes = CoasterAnchorBezierOptimizer.optimizeAxesForPlacement(level, lo, hi, startLo, startHi, axisLo, axisHi, epLo.normal(), epHi.normal(), girder, material, suppressNeighborSmoothing, lo.equals((Object)anchorA), hi.equals((Object)anchorA));
        Vec3 optAxisLo = axes.axisLo();
        Vec3 optAxisHi = axes.axisHi();
        LoHiAxes greenfieldAxes = CoasterAnchorBezierOptimizer.applyGreenfieldOpenEndAxisPass(level, lo, hi, lo.equals((Object)anchorA) ? beReal : null, hi.equals((Object)anchorA) ? beReal : null, startLo, startHi, optAxisLo, optAxisHi, epLo.normal(), epHi.normal(), girder, material, suppressNeighborSmoothing);
        optAxisLo = greenfieldAxes.lo();
        optAxisHi = greenfieldAxes.hi();
        BezierConnection bc = new BezierConnection(Couple.create((Object)lo, (Object)hi), Couple.create((Object)startLo, (Object)startHi), Couple.create((Object)optAxisLo.normalize(), (Object)optAxisHi.normalize()), Couple.create((Object)(lo.equals((Object)anchorA) ? CoasterTrackPlacement.resolvePlacementNormalAtHub(level, lo, hi, optAxisLo) : CoasterTrackPlacement.anchorFaceNormalForAxis(level, lo, optAxisLo)), (Object)(hi.equals((Object)anchorA) ? CoasterTrackPlacement.resolvePlacementNormalAtHub(level, hi, lo, optAxisHi) : CoasterTrackPlacement.anchorFaceNormalForAxis(level, hi, optAxisHi))), true, girder, material);
        double chord = startLo.distanceTo(startHi);
        double h = Math.max(1.0E-4, chord / 3.0);
        CoasterPeerCurveHandleLengths.putCanonical(bc, h, h);
        List<NeighborJoinAxisWrite> writes = suppressNeighborSmoothing ? List.of() : CoasterAnchorBezierOptimizer.collectNeighborJoinWrites(level, axes.optAtLo(), axes.optAtHi(), lo, hi, material, girder);
        return new AnchorAnchorBuildResult(bc, writes);
    }

    @Nullable
    public static BezierConnection buildAnchorAnchorBezier(Level level, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder) {
        AnchorAnchorBuildResult r = CoasterAnchorBezierOptimizer.buildAnchorAnchorPlacement(level, anchorA, anchorB, material, girder);
        return r == null ? null : r.primary();
    }

    public static boolean isBuiltPlacementCurveValid(@Nullable BezierConnection bc) {
        double h1;
        double h0;
        if (bc == null) {
            return false;
        }
        Couple<Double> stored = CoasterPeerCurveHandleLengths.getCanonicalLengths(bc);
        if (stored != null) {
            BlockPos lo = CoasterPeerCurveHandleLengths.lexMin((BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond());
            boolean firstIsLo = ((BlockPos)bc.bePositions.getFirst()).equals((Object)lo);
            h0 = firstIsLo ? (Double)stored.getFirst() : (Double)stored.getSecond();
            h1 = firstIsLo ? (Double)stored.getSecond() : (Double)stored.getFirst();
        } else {
            h0 = h1 = CoasterAnchorBezierOptimizer.defaultHandleLength(bc);
        }
        return CoasterAnchorBezierOptimizer.handlesWithinReasonableScopeExplicit(bc, h0, h1) && CoasterAnchorBezierOptimizer.anchorCurveFreeOfOvershootLoopExplicit(bc, h0, h1);
    }

    public static boolean meetsAltSuppressMinCurveRadius(@Nullable BezierConnection bc) {
        if (bc == null) {
            return false;
        }
        double h = CoasterAnchorBezierOptimizer.defaultHandleLength(bc);
        return CoasterAnchorBezierOptimizer.meetsAltSuppressMinCurveRadiusExplicit(bc, h, h);
    }

    public static boolean meetsMinCurveRadiusWithHandleLengths(@Nullable BezierConnection bc, double handleLenAtEndpoint0, double handleLenAtEndpoint1) {
        return CoasterAnchorBezierOptimizer.meetsAltSuppressMinCurveRadiusExplicit(bc, handleLenAtEndpoint0, handleLenAtEndpoint1);
    }

    private static AxisOptimizationResult optimizeAxesForPlacement(Level level, BlockPos lo, BlockPos hi, Vec3 startLo, Vec3 startHi, Vec3 axisLo, Vec3 axisHi, Vec3 normalLo, Vec3 normalHi, boolean girder, TrackMaterial material, boolean suppressNeighborSmoothing, boolean neighborJoinAtLo, boolean neighborJoinAtHi) {
        AnchorJoinOptimization optAtHi;
        AnchorJoinOptimization optAtLo;
        boolean enforceMinRadius = suppressNeighborSmoothing;
        @Nullable Vec3 planeLo = CoasterAnchorCoplanarPlane.placementTangentPlaneNormal(level, lo, hi);
        @Nullable Vec3 planeHi = CoasterAnchorCoplanarPlane.placementTangentPlaneNormal(level, hi, lo);
        Vec3 optAxisHi = CoasterAnchorBezierOptimizer.optimizeAnchorAxis(level, lo, hi, startLo, startHi, axisLo, axisHi, CoasterTrackPlacement.anchorFaceNormalForAxis(level, lo, axisLo), null, girder, material, enforceMinRadius, planeLo);
        Vec3 optAxisLo = CoasterAnchorBezierOptimizer.optimizeAnchorAxis(level, hi, lo, startHi, startLo, axisHi, axisLo, CoasterTrackPlacement.anchorFaceNormalForAxis(level, hi, axisHi), null, girder, material, enforceMinRadius, planeHi);
        if (suppressNeighborSmoothing) {
            boolean hasLockHi;
            @Nullable Vec3 lockedLo = CoasterAnchorBezierOptimizer.lockedNewCurveAxisAtExistingJoin(level, lo, hi);
            @Nullable Vec3 lockedHi = CoasterAnchorBezierOptimizer.lockedNewCurveAxisAtExistingJoin(level, hi, lo);
            boolean hasLockLo = lockedLo != null;
            boolean bl = hasLockHi = lockedHi != null;
            if (hasLockLo) {
                optAxisLo = lockedLo;
            }
            if (hasLockHi) {
                optAxisHi = lockedHi;
            }
            if (hasLockLo && !hasLockHi) {
                optAxisHi = CoasterAnchorBezierOptimizer.optimizeAnchorAxis(level, lo, hi, startLo, startHi, optAxisLo, optAxisHi, CoasterTrackPlacement.anchorFaceNormalForAxis(level, lo, optAxisLo), null, girder, material, enforceMinRadius, planeLo);
            } else if (hasLockHi && !hasLockLo) {
                optAxisLo = CoasterAnchorBezierOptimizer.optimizeAnchorAxis(level, hi, lo, startHi, startLo, optAxisHi, optAxisLo, CoasterTrackPlacement.anchorFaceNormalForAxis(level, hi, optAxisHi), null, girder, material, enforceMinRadius, planeHi);
            }
            return new AxisOptimizationResult(optAxisLo, optAxisHi, null, null);
        }
        AnchorJoinOptimization anchorJoinOptimization = optAtLo = neighborJoinAtLo ? CoasterAnchorBezierOptimizer.optimizeNewAnchorPeerAxisFromNeighbor(level, lo, hi, startLo, normalLo, optAxisLo, hi, startHi, normalHi, optAxisHi, girder, material) : null;
        if (optAtLo != null) {
            optAxisLo = optAtLo.newAxisAtAnchor();
        }
        optAxisHi = CoasterAnchorBezierOptimizer.optimizeAnchorAxis(level, lo, hi, startLo, startHi, optAxisLo, optAxisHi, CoasterTrackPlacement.anchorFaceNormalForAxis(level, lo, optAxisLo), null, girder, material, false, planeLo);
        AnchorJoinOptimization anchorJoinOptimization2 = optAtHi = neighborJoinAtHi ? CoasterAnchorBezierOptimizer.optimizeNewAnchorPeerAxisFromNeighbor(level, hi, lo, startHi, normalHi, optAxisHi, lo, startLo, normalLo, optAxisLo, girder, material) : null;
        if (optAtHi != null) {
            optAxisHi = optAtHi.newAxisAtAnchor();
        }
        return new AxisOptimizationResult(optAxisLo, optAxisHi, optAtLo, optAtHi);
    }

    @Nullable
    private static Vec3 lockedNewCurveAxisAtExistingJoin(Level level, BlockPos anchorPos, BlockPos newPeerPos) {
        AnchorNeighborEndpoint existing = CoasterAnchorBezierOptimizer.firstNeighborEndpointForAnchorSmoothing(level, anchorPos, newPeerPos);
        if (existing == null) {
            return null;
        }
        BezierConnection curve = existing.curve().isPrimary() ? existing.curve() : existing.curve().secondary();
        Vec3 into = CoasterBezierHandleEdit.tangentIntoFromAnchor(curve, anchorPos);
        if (into == null || into.lengthSqr() < 1.0E-12) {
            return null;
        }
        return into.normalize().scale(-1.0);
    }

    private static List<NeighborJoinAxisWrite> collectNeighborJoinWrites(Level level, @Nullable AnchorJoinOptimization optAtLo, @Nullable AnchorJoinOptimization optAtHi, BlockPos lo, BlockPos hi, TrackMaterial material, boolean girder) {
        ArrayList<NeighborJoinAxisWrite> writes = new ArrayList<NeighborJoinAxisWrite>(4);
        if (optAtLo != null && optAtLo.existingAxis().lengthSqr() > 1.0E-12) {
            writes.add(new NeighborJoinAxisWrite(lo, optAtLo.remotePos(), optAtLo.existingAxis().normalize()));
        }
        if (optAtHi != null && optAtHi.existingAxis().lengthSqr() > 1.0E-12) {
            writes.add(new NeighborJoinAxisWrite(hi, optAtHi.remotePos(), optAtHi.existingAxis().normalize()));
        }
        return CoasterAnchorBezierOptimizer.appendOppositeOpenEndAxisWrites(level, writes, material, girder);
    }

    private static List<NeighborJoinAxisWrite> appendOppositeOpenEndAxisWrites(Level level, List<NeighborJoinAxisWrite> hubJoinWrites, TrackMaterial material, boolean girder) {
        if (hubJoinWrites.isEmpty()) {
            return List.of();
        }
        LinkedHashMap<String, NeighborJoinAxisWrite> merged = new LinkedHashMap<String, NeighborJoinAxisWrite>();
        for (NeighborJoinAxisWrite w : hubJoinWrites) {
            merged.putIfAbsent(CoasterAnchorBezierOptimizer.joinWriteKey(w.sharedAnchor(), w.remotePos()), w);
        }
        for (NeighborJoinAxisWrite hubWrite : hubJoinWrites) {
            Vec3 optimizedOpen;
            BezierConnection raw;
            String oppositeKey;
            CoasterAnchorpointBlockEntity beOpen;
            BlockPos hub = hubWrite.sharedAnchor();
            BlockPos openEnd = hubWrite.remotePos();
            BlockEntity blockEntity = level.getBlockEntity(openEnd);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity) || (beOpen = (CoasterAnchorpointBlockEntity)blockEntity).legCount() != 1 || merged.containsKey(oppositeKey = CoasterAnchorBezierOptimizer.joinWriteKey(openEnd, hub)) || (raw = beOpen.getAnchorPeerCurvesView().get(hub)) == null) continue;
            BezierConnection curve = raw.isPrimary() ? raw : raw.secondary();
            CoasterBezierEndpointData atHub = CoasterBezierEndpointData.at(curve, hub);
            CoasterBezierEndpointData atOpen = CoasterBezierEndpointData.at(curve, openEnd);
            if (atHub == null || atOpen == null || (optimizedOpen = CoasterAnchorBezierOptimizer.optimizeGreenfieldOpenEndAxis(level, hub, openEnd, atHub.start(), atOpen.start(), hubWrite.axisIntoCurveAtSharedAnchor(), atOpen.axis(), atOpen.normal(), girder, material, false)).lengthSqr() < 1.0E-12) continue;
            merged.put(oppositeKey, new NeighborJoinAxisWrite(openEnd, hub, optimizedOpen.normalize()));
        }
        return List.copyOf(merged.values());
    }

    private static String joinWriteKey(BlockPos endpoint, BlockPos peer) {
        return endpoint.asLong() + ":" + peer.asLong();
    }

    @Nullable
    private static AnchorJoinOptimization optimizeNewAnchorPeerAxisFromNeighbor(Level level, BlockPos anchorPos, BlockPos peerPos, Vec3 newAnchorStart, Vec3 newAnchorNormal, Vec3 newAnchorAxisFallback, BlockPos newRemotePos, Vec3 newRemoteStart, Vec3 newRemoteNormal, Vec3 newRemoteAxisIntoCurve, boolean girder, TrackMaterial material) {
        AnchorNeighborEndpoint existing = CoasterAnchorBezierOptimizer.firstNeighborEndpointForAnchorSmoothing(level, anchorPos, peerPos);
        if (existing == null) {
            return null;
        }
        Vec3 existingAnchorStub = newAnchorStart;
        Vec3 newAnchorStub = newAnchorStart;
        Vec3 existingAnchorFallback = existing.remoteStart().subtract(existingAnchorStub);
        existingAnchorFallback = existingAnchorFallback.lengthSqr() < 1.0E-8 ? existing.anchorAxis() : existingAnchorFallback.normalize();
        Vec3 newAnchorFallback = newRemoteStart.subtract(newAnchorStub);
        newAnchorFallback = newAnchorFallback.lengthSqr() < 1.0E-8 ? newAnchorAxisFallback : newAnchorFallback.normalize();
        @Nullable Vec3 hubPlane = CoasterAnchorCoplanarPlane.hubJoinPlaneNormal(level, anchorPos, existing.remotePos(), newRemotePos);
        AnchorPair pair = CoasterAnchorBezierOptimizer.optimizeAnchorPair(level, existing.remotePos(), newRemotePos, anchorPos, existing.remoteStart(), newRemoteStart, existing.remoteAxis(), newRemoteAxisIntoCurve, existing.remoteNormal(), newRemoteNormal, existingAnchorFallback, newAnchorFallback, existingAnchorStub, newAnchorStub, girder, material, hubPlane, null);
        return new AnchorJoinOptimization(pair.existingAxis(), pair.newAxis(), existing.remotePos(), existing.curve());
    }

    private static AnchorPair optimizeAnchorPair(Level level, BlockPos existingTrackPos, BlockPos newTrackPos, BlockPos anchorPos, Vec3 existingEnd, Vec3 newEnd, Vec3 existingAxisTrack, Vec3 newAxisTrack, Vec3 existingNormalTrack, Vec3 newNormalTrack, Vec3 existingAxisAnchorFallback, Vec3 newAxisAnchorFallback, Vec3 existingAnchorEnd, Vec3 newAnchorEnd, boolean girder, TrackMaterial material, @Nullable Vec3 coplanarPlaneNormal, @Nullable Vec3 lockedExistingAnchorAxisInto) {
        Vec3 baseExisting = existingEnd.subtract(existingAnchorEnd).normalize();
        Vec3 baseNew = newEnd.subtract(newAnchorEnd).normalize();
        if (lockedExistingAnchorAxisInto != null && lockedExistingAnchorAxisInto.lengthSqr() > 1.0E-12) {
            Vec3 axisExisting = CoasterAnchorCoplanarPlane.projectAxisOntoPlane(lockedExistingAnchorAxisInto, coplanarPlaneNormal, lockedExistingAnchorAxisInto);
            Vec3 axisNew = axisExisting.scale(-1.0);
            return new AnchorPair(axisExisting, axisNew);
        }
        Vec3 bestExisting = CoasterAnchorCoplanarPlane.projectAxisOntoPlane(existingAxisAnchorFallback, coplanarPlaneNormal, existingAxisAnchorFallback);
        Vec3 bestNew = CoasterAnchorCoplanarPlane.projectAxisOntoPlane(newAxisAnchorFallback, coplanarPlaneNormal, newAxisAnchorFallback);
        double best = Double.POSITIVE_INFINITY;
        double bestFallbackPenalty = Double.POSITIVE_INFINITY;
        double EPS = 1.0E-9;
        int maxEvals = Math.max(8, 40);
        List<Vec3> sharedAxes = CoasterAnchorBezierOptimizer.sharedAnchorAxisCandidates(existingEnd, newEnd, existingAnchorEnd, newAnchorEnd, existingAxisAnchorFallback, newAxisAnchorFallback, coplanarPlaneNormal);
        int evalCount = 0;
        for (Vec3 seed : sharedAxes) {
            for (int sign : new int[]{1, -1}) {
                boolean sameFallback;
                if (evalCount >= maxEvals) {
                    return new AnchorPair(CoasterAnchorCoplanarPlane.projectAxisOntoPlane(bestExisting, coplanarPlaneNormal, bestExisting), CoasterAnchorCoplanarPlane.projectAxisOntoPlane(bestNew, coplanarPlaneNormal, bestNew));
                }
                Vec3 axisExisting = CoasterAnchorCoplanarPlane.projectAxisOntoPlane(seed.scale((double)sign), coplanarPlaneNormal, seed.scale((double)sign));
                Vec3 axisNew = axisExisting.scale(-1.0);
                if (axisExisting.dot(baseExisting) < 0.35 || axisNew.dot(baseNew) < 0.35) continue;
                Vec3 existingAnchorNormal = CoasterTrackPlacement.anchorFaceNormalForAxis(level, anchorPos, axisExisting);
                Vec3 newAnchorNormal = CoasterTrackPlacement.anchorFaceNormalForAxis(level, anchorPos, axisNew);
                BezierConnection bcExisting = new BezierConnection(Couple.create((Object)existingTrackPos, (Object)anchorPos), Couple.create((Object)existingEnd, (Object)existingAnchorEnd), Couple.create((Object)existingAxisTrack, (Object)axisExisting), Couple.create((Object)existingNormalTrack, (Object)existingAnchorNormal), true, girder, material);
                BezierConnection bcNew = new BezierConnection(Couple.create((Object)newTrackPos, (Object)anchorPos), Couple.create((Object)newEnd, (Object)newAnchorEnd), Couple.create((Object)newAxisTrack, (Object)axisNew), Couple.create((Object)newNormalTrack, (Object)newAnchorNormal), true, girder, material);
                double hEx = CoasterAnchorBezierOptimizer.defaultHandleLength(bcExisting);
                double hNw = CoasterAnchorBezierOptimizer.defaultHandleLength(bcNew);
                if (!CoasterAnchorBezierOptimizer.handlesWithinReasonableScopeExplicit(bcExisting, hEx, hEx) || !CoasterAnchorBezierOptimizer.handlesWithinReasonableScopeExplicit(bcNew, hNw, hNw)) continue;
                ++evalCount;
                double fallbackPenalty = 1.0 - Math.abs(axisExisting.normalize().dot(existingAxisAnchorFallback.normalize())) + (1.0 - Math.abs(axisNew.normalize().dot(newAxisAnchorFallback.normalize())));
                double score = CoasterAnchorBezierOptimizer.optimizationSmoothnessScoreExplicit(bcExisting, hEx, hEx) + CoasterAnchorBezierOptimizer.optimizationSmoothnessScoreExplicit(bcNew, hNw, hNw);
                if (score + 1.0E-9 < best) {
                    best = score;
                    bestFallbackPenalty = fallbackPenalty;
                    bestExisting = axisExisting;
                    bestNew = axisNew;
                    continue;
                }
                if (!(Math.abs(score - best) <= 1.0E-9)) continue;
                boolean betterFallback = fallbackPenalty + 1.0E-9 < bestFallbackPenalty;
                boolean bl = sameFallback = Math.abs(fallbackPenalty - bestFallbackPenalty) <= 1.0E-9;
                if (!betterFallback && (!sameFallback || !CoasterAnchorBezierOptimizer.axisLexicographicallySmaller(axisExisting, bestExisting))) continue;
                best = score;
                bestFallbackPenalty = fallbackPenalty;
                bestExisting = axisExisting;
                bestNew = axisNew;
            }
        }
        return new AnchorPair(CoasterAnchorCoplanarPlane.projectAxisOntoPlane(bestExisting, coplanarPlaneNormal, bestExisting), CoasterAnchorCoplanarPlane.projectAxisOntoPlane(bestNew, coplanarPlaneNormal, bestNew));
    }

    private static LoHiAxes applyGreenfieldOpenEndAxisPass(Level level, BlockPos lo, BlockPos hi, @Nullable CoasterAnchorpointBlockEntity beLo, @Nullable CoasterAnchorpointBlockEntity beHi, Vec3 startLo, Vec3 startHi, Vec3 axisLo, Vec3 axisHi, Vec3 normalLo, Vec3 normalHi, boolean girder, TrackMaterial material, boolean enforceMinRadius) {
        if (beLo != null && beLo.legCount() == 0) {
            axisLo = CoasterAnchorBezierOptimizer.optimizeGreenfieldOpenEndAxis(level, hi, lo, startHi, startLo, axisHi, axisLo, normalLo, girder, material, enforceMinRadius);
        }
        if (beHi != null && beHi.legCount() == 0) {
            axisHi = CoasterAnchorBezierOptimizer.optimizeGreenfieldOpenEndAxis(level, lo, hi, startLo, startHi, axisLo, axisHi, normalHi, girder, material, enforceMinRadius);
        }
        return new LoHiAxes(axisLo, axisHi);
    }

    private static Vec3 optimizeGreenfieldOpenEndAxis(Level level, BlockPos hubPos, BlockPos openPos, Vec3 hubStart, Vec3 openStart, Vec3 hubAxisInto, Vec3 openAxisFallback, Vec3 openNormal, boolean girder, TrackMaterial material, boolean enforceMinRadius) {
        Vec3 towardHub = hubStart.subtract(openStart);
        if (towardHub.lengthSqr() < 1.0E-8) {
            return openAxisFallback;
        }
        towardHub = towardHub.normalize();
        @Nullable Vec3 approachPlane = CoasterAnchorCoplanarPlane.openEndApproachPlaneNormal(level, openPos, hubPos);
        List<Vec3> centers = CoasterAnchorBezierOptimizer.buildGreenfieldApproachSearchCenters(level, hubPos, openPos, hubStart, openStart, hubAxisInto, towardHub, openAxisFallback, approachPlane);
        Vec3 hubNormal = CoasterTrackPlacement.anchorFaceNormalForAxis(level, hubPos, hubAxisInto);
        Vec3 best = CoasterAnchorCoplanarPlane.projectAxisOntoPlane(openAxisFallback.lengthSqr() > 1.0E-10 ? openAxisFallback.normalize() : towardHub, approachPlane, towardHub);
        double bestScore = Double.POSITIVE_INFINITY;
        int budgetPerCenter = Math.max(8, 120 / Math.max(1, centers.size()));
        for (Vec3 center : centers) {
            AxisSearchResult found;
            if ((center = CoasterAnchorCoplanarPlane.projectAxisOntoPlane(center, approachPlane, center)).lengthSqr() < 1.0E-10 || center.dot(towardHub) < 0.2 || !((found = CoasterAnchorBezierOptimizer.searchEndpointAxisAroundCenter(level, hubPos, openPos, hubStart, openStart, hubAxisInto, hubNormal, openNormal, openAxisFallback, center, towardHub, girder, material, budgetPerCenter, enforceMinRadius, approachPlane)).score() < bestScore)) continue;
            bestScore = found.score();
            best = found.axis();
        }
        return CoasterAnchorCoplanarPlane.projectAxisOntoPlane(best, approachPlane, best);
    }

    private static List<Vec3> buildGreenfieldApproachSearchCenters(Level level, BlockPos hubPos, BlockPos openPos, Vec3 hubStart, Vec3 openStart, Vec3 hubAxisInto, Vec3 towardHub, Vec3 openAxisFallback, @Nullable Vec3 coplanarPlaneNormal) {
        AnchorNeighborEndpoint existing;
        LinkedHashMap<String, Vec3> uniq = new LinkedHashMap<String, Vec3>();
        CoasterAnchorBezierOptimizer.putSharedAxis(uniq, CoasterAnchorCoplanarPlane.projectAxisOntoPlane(towardHub, coplanarPlaneNormal, towardHub));
        if (openAxisFallback.lengthSqr() > 1.0E-10) {
            CoasterAnchorBezierOptimizer.putSharedAxis(uniq, CoasterAnchorCoplanarPlane.projectAxisOntoPlane(openAxisFallback, coplanarPlaneNormal, openAxisFallback));
        }
        if (hubAxisInto.lengthSqr() > 1.0E-10) {
            CoasterAnchorBezierOptimizer.putSharedAxis(uniq, CoasterAnchorCoplanarPlane.projectAxisOntoPlane(hubAxisInto.scale(-1.0), coplanarPlaneNormal, towardHub));
        }
        if ((existing = CoasterAnchorBezierOptimizer.firstNeighborEndpointForAnchorSmoothing(level, hubPos, openPos)) != null && existing.anchorAxis().lengthSqr() > 1.0E-10) {
            CoasterAnchorBezierOptimizer.putSharedAxis(uniq, CoasterAnchorCoplanarPlane.projectAxisOntoPlane(existing.anchorAxis().normalize().scale(-1.0), coplanarPlaneNormal, towardHub));
        }
        for (Vec3 c : CoasterAnchorBezierOptimizer.anchorAxisCandidates(hubStart, openStart, towardHub, coplanarPlaneNormal)) {
            CoasterAnchorBezierOptimizer.putSharedAxis(uniq, c);
        }
        return new ArrayList<Vec3>(uniq.values());
    }

    private static Vec3 optimizeAnchorAxis(Level level, BlockPos pos1, BlockPos pos2, Vec3 end1, Vec3 end2, Vec3 axis1, Vec3 fallbackAxis2, Vec3 normalAtPos1, @Nullable Vec3 normalAtPos2IfConstant, boolean girder, TrackMaterial material, boolean enforceMinRadius, @Nullable Vec3 coplanarPlaneNormal) {
        Vec3 chord = end1.subtract(end2);
        if (chord.lengthSqr() < 1.0E-8) {
            return CoasterAnchorCoplanarPlane.projectAxisOntoPlane(fallbackAxis2, coplanarPlaneNormal, fallbackAxis2);
        }
        Vec3 towardPos1 = chord.normalize();
        towardPos1 = CoasterAnchorCoplanarPlane.projectAxisOntoPlane(towardPos1, coplanarPlaneNormal, towardPos1);
        return CoasterAnchorBezierOptimizer.searchEndpointAxisAroundCenter(level, pos1, pos2, end1, end2, axis1, normalAtPos1, normalAtPos2IfConstant, fallbackAxis2, towardPos1, towardPos1, girder, material, Math.max(12, 120), enforceMinRadius, coplanarPlaneNormal).axis();
    }

    private static AxisSearchResult searchEndpointAxisAroundCenter(Level level, BlockPos pos1, BlockPos pos2, Vec3 end1, Vec3 end2, Vec3 axis1, Vec3 normalAtPos1, @Nullable Vec3 normalAtPos2IfConstant, Vec3 fallbackAxis2, Vec3 searchCenter, Vec3 forwardHemisphereAxis, boolean girder, TrackMaterial material, int maxEvals, boolean enforceMinRadius, @Nullable Vec3 coplanarPlaneNormal) {
        Vec3 axis1n = axis1.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axis1.normalize();
        Vec3 base = searchCenter.lengthSqr() < 1.0E-10 ? forwardHemisphereAxis : searchCenter.normalize();
        Vec3 forwardHem = CoasterAnchorCoplanarPlane.projectAxisOntoPlane(forwardHemisphereAxis, coplanarPlaneNormal, forwardHemisphereAxis);
        base = CoasterAnchorCoplanarPlane.projectAxisOntoPlane(base, coplanarPlaneNormal, forwardHem);
        Vec3 n1 = normalAtPos1.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : normalAtPos1.normalize();
        Vec3 upBasis = CoasterAnchorBezierOptimizer.normalAtPos2ForOptimizer(level, pos2, fallbackAxis2, normalAtPos2IfConstant);
        Vec3 up = upBasis.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : upBasis.normalize();
        Vec3 side = base.cross(up);
        if (side.lengthSqr() < 1.0E-8 && (side = base.cross(new Vec3(1.0, 0.0, 0.0))).lengthSqr() < 1.0E-8) {
            side = base.cross(new Vec3(0.0, 0.0, 1.0));
        }
        side = side.normalize();
        Vec3 basisUp = side.cross(base).normalize();
        Vec3 best = fallbackAxis2.lengthSqr() < 1.0E-10 ? base : CoasterAnchorCoplanarPlane.projectAxisOntoPlane(fallbackAxis2, coplanarPlaneNormal, base);
        Vec3 n2Fallback = CoasterAnchorBezierOptimizer.normalAtPos2ForOptimizer(level, pos2, fallbackAxis2, normalAtPos2IfConstant);
        n2Fallback = n2Fallback.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : n2Fallback.normalize();
        BezierConnection fallbackBc = new BezierConnection(Couple.create((Object)pos1, (Object)pos2), Couple.create((Object)end1, (Object)end2), Couple.create((Object)axis1n, (Object)best), Couple.create((Object)n1, (Object)n2Fallback), true, girder, material);
        double hf = CoasterAnchorBezierOptimizer.defaultHandleLength(fallbackBc);
        double bestScore = CoasterAnchorBezierOptimizer.handlesWithinReasonableScopeExplicit(fallbackBc, hf, hf) && (!enforceMinRadius || CoasterAnchorBezierOptimizer.meetsAltSuppressMinCurveRadiusExplicit(fallbackBc, hf, hf)) ? CoasterAnchorBezierOptimizer.optimizationSmoothnessScoreExplicit(fallbackBc, hf, hf) : Double.POSITIVE_INFINITY;
        int evalCount = 1;
        int ringsWithoutImprovement = 0;
        int maxRing = 8;
        double ringStepDeg = 11.25;
        block0: for (int ring = 0; ring <= 8; ++ring) {
            boolean improvedThisRing = false;
            double theta = Math.toRadians((double)ring * 11.25);
            double c = Math.cos(theta);
            double s = Math.sin(theta);
            int azSteps = ring == 0 ? 1 : 16 + ring * 4;
            for (int i = 0; i < azSteps; ++i) {
                double hc;
                if (evalCount >= maxEvals) break block0;
                double phi = Math.PI * 2 * (double)i / (double)azSteps;
                Vec3 radial = side.scale(Math.cos(phi)).add(basisUp.scale(Math.sin(phi)));
                Vec3 candidate = base.scale(c).add(radial.scale(s));
                if (candidate.lengthSqr() < 1.0E-8 || (candidate = CoasterAnchorCoplanarPlane.projectAxisOntoPlane(candidate, coplanarPlaneNormal, forwardHem)).lengthSqr() < 1.0E-8 || (candidate = candidate.normalize()).dot(forwardHem) < 0.2) continue;
                Vec3 n2Cand = CoasterAnchorBezierOptimizer.normalAtPos2ForOptimizer(level, pos2, candidate, normalAtPos2IfConstant);
                n2Cand = n2Cand.lengthSqr() < 1.0E-10 ? n2Fallback : n2Cand.normalize();
                BezierConnection bc = new BezierConnection(Couple.create((Object)pos1, (Object)pos2), Couple.create((Object)end1, (Object)end2), Couple.create((Object)axis1n, (Object)candidate), Couple.create((Object)n1, (Object)n2Cand), true, girder, material);
                if (!CoasterAnchorBezierOptimizer.handlesWithinReasonableScopeExplicit(bc, hc = CoasterAnchorBezierOptimizer.defaultHandleLength(bc), hc) || !CoasterAnchorBezierOptimizer.anchorCurveFreeOfOvershootLoopExplicit(bc, hc, hc) || enforceMinRadius && !CoasterAnchorBezierOptimizer.meetsAltSuppressMinCurveRadiusExplicit(bc, hc, hc)) continue;
                double score = CoasterAnchorBezierOptimizer.optimizationSmoothnessScoreExplicit(bc, hc, hc);
                ++evalCount;
                if (!(score < bestScore)) continue;
                bestScore = score;
                best = candidate;
                improvedThisRing = true;
            }
            int n = ringsWithoutImprovement = improvedThisRing ? 0 : ringsWithoutImprovement + 1;
            if (ring >= 3 && ringsWithoutImprovement >= 2) break;
        }
        if (Double.isInfinite(bestScore)) {
            double hb;
            Vec3 n2Chord = CoasterAnchorBezierOptimizer.normalAtPos2ForOptimizer(level, pos2, forwardHem, normalAtPos2IfConstant);
            n2Chord = n2Chord.lengthSqr() < 1.0E-10 ? n2Fallback : n2Chord.normalize();
            BezierConnection chordBc = new BezierConnection(Couple.create((Object)pos1, (Object)pos2), Couple.create((Object)end1, (Object)end2), Couple.create((Object)axis1n, (Object)forwardHem), Couple.create((Object)n1, (Object)n2Chord), true, girder, material);
            if (CoasterAnchorBezierOptimizer.handlesWithinReasonableScopeExplicit(chordBc, hb = CoasterAnchorBezierOptimizer.defaultHandleLength(chordBc), hb) && CoasterAnchorBezierOptimizer.anchorCurveFreeOfOvershootLoopExplicit(chordBc, hb, hb) && (!enforceMinRadius || CoasterAnchorBezierOptimizer.meetsAltSuppressMinCurveRadiusExplicit(chordBc, hb, hb))) {
                return new AxisSearchResult(forwardHem, CoasterAnchorBezierOptimizer.optimizationSmoothnessScoreExplicit(chordBc, hb, hb));
            }
        }
        return new AxisSearchResult(CoasterAnchorCoplanarPlane.projectAxisOntoPlane(best, coplanarPlaneNormal, best), bestScore);
    }

    private static double defaultHandleLength(BezierConnection bc) {
        return Math.max(1.0E-4, ((Vec3)bc.starts.getFirst()).distanceTo((Vec3)bc.starts.getSecond()) / 3.0);
    }

    private static Vec3 normalAtPos2ForOptimizer(Level level, BlockPos pos2, Vec3 axisIntoCurve, @Nullable Vec3 constantNormal) {
        if (constantNormal != null) {
            return constantNormal;
        }
        BlockState st = level.getBlockState(pos2);
        if (st.is((Block)SimulatedCoastersBlocks.COASTER_ANCHORPOINT.get())) {
            return CoasterTrackPlacement.anchorFaceNormalForAxis(level, pos2, axisIntoCurve);
        }
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 axis = axisIntoCurve.lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : axisIntoCurve.normalize();
        Vec3 n = up.subtract(axis.scale(axis.dot(up)));
        if (n.lengthSqr() < 1.0E-10) {
            n = axis.cross(new Vec3(1.0, 0.0, 0.0));
        }
        return n.normalize();
    }

    private static double optimizationSmoothnessScoreExplicit(BezierConnection bc, double handleLenFirst, double handleLenSecond) {
        int target = Math.max(8, 14);
        int samples = Math.min(Math.max(12, bc.getSegmentCount()), target);
        return CoasterAnchorBezierOptimizer.smoothnessScoreExplicit(bc, handleLenFirst, handleLenSecond, samples);
    }

    private static double smoothnessScoreExplicit(BezierConnection bc, double handleLenFirst, double handleLenSecond, int samples) {
        Vec3 e1 = (Vec3)bc.starts.getFirst();
        Vec3 e2 = (Vec3)bc.starts.getSecond();
        Vec3 a1 = ((Vec3)bc.axes.getFirst()).lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : ((Vec3)bc.axes.getFirst()).normalize();
        Vec3 a2 = ((Vec3)bc.axes.getSecond()).lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : ((Vec3)bc.axes.getSecond()).normalize();
        Vec3 f1 = e1.add(a1.scale(handleLenFirst));
        Vec3 f2 = e2.add(a2.scale(handleLenSecond));
        Vec3 prev = VecHelper.bezier((Vec3)e1, (Vec3)e2, (Vec3)f1, (Vec3)f2, (float)0.0f);
        Vec3 prevTan = null;
        double score = 0.0;
        for (int i = 1; i <= samples; ++i) {
            float t = (float)((double)i / (double)samples);
            Vec3 cur = VecHelper.bezier((Vec3)e1, (Vec3)e2, (Vec3)f1, (Vec3)f2, (float)t);
            Vec3 seg = cur.subtract(prev);
            if (seg.lengthSqr() < 1.0E-12) {
                prev = cur;
                continue;
            }
            Vec3 tan = seg.normalize();
            if (prevTan != null) {
                double dot = Mth.clamp((double)prevTan.dot(tan), (double)-1.0, (double)1.0);
                double bend = 1.0 - dot;
                score += bend * bend;
            }
            prevTan = tan;
            prev = cur;
        }
        return score;
    }

    private static boolean handlesWithinReasonableScopeExplicit(BezierConnection bc, double handleLenFirst, double handleLenSecond) {
        Vec3 e1 = (Vec3)bc.starts.getFirst();
        Vec3 e2 = (Vec3)bc.starts.getSecond();
        Vec3 chord = e2.subtract(e1);
        double chordLenSq = chord.lengthSqr();
        if (chordLenSq < 1.0E-10) {
            return true;
        }
        double chordLen = Math.sqrt(chordLenSq);
        Vec3 chordN = chord.scale(1.0 / chordLen);
        Vec3 a1 = ((Vec3)bc.axes.getFirst()).lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : ((Vec3)bc.axes.getFirst()).normalize();
        Vec3 a2 = ((Vec3)bc.axes.getSecond()).lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : ((Vec3)bc.axes.getSecond()).normalize();
        Vec3 f1 = e1.add(a1.scale(handleLenFirst));
        Vec3 f2 = e2.add(a2.scale(handleLenSecond));
        double maxPerp = chordLen * 0.85;
        if (CoasterAnchorBezierOptimizer.distancePointToLine(f1, e1, chordN) > maxPerp) {
            return false;
        }
        if (CoasterAnchorBezierOptimizer.distancePointToLine(f2, e1, chordN) > maxPerp) {
            return false;
        }
        double maxPast = 0.6;
        double t1 = f1.subtract(e1).dot(chordN) / chordLen;
        double t2 = f2.subtract(e1).dot(chordN) / chordLen;
        return !(t1 < -maxPast || t1 > 1.0 + maxPast || t2 < -maxPast || t2 > 1.0 + maxPast);
    }

    private static boolean bezierChordProjectionMonotonicExplicit(BezierConnection bc, double handleLenFirst, double handleLenSecond) {
        Vec3 e1 = (Vec3)bc.starts.getFirst();
        Vec3 e2 = (Vec3)bc.starts.getSecond();
        Vec3 chord = e2.subtract(e1);
        double chordLen = Math.sqrt(chord.lengthSqr());
        if (chordLen < 1.0E-8) {
            return true;
        }
        Vec3 chordN = chord.scale(1.0 / chordLen);
        Vec3 a1 = ((Vec3)bc.axes.getFirst()).lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : ((Vec3)bc.axes.getFirst()).normalize();
        Vec3 a2 = ((Vec3)bc.axes.getSecond()).lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : ((Vec3)bc.axes.getSecond()).normalize();
        Vec3 f1 = e1.add(a1.scale(handleLenFirst));
        Vec3 f2 = e2.add(a2.scale(handleLenSecond));
        int steps = Math.max(32, bc.getSegmentCount() * 3);
        double maxDrop = 0.035;
        double prevS = 0.0;
        for (int i = 0; i <= steps; ++i) {
            double t = (double)i / (double)steps;
            Vec3 p = VecHelper.bezier((Vec3)e1, (Vec3)e2, (Vec3)f1, (Vec3)f2, (float)((float)t));
            double s = p.subtract(e1).dot(chordN) / chordLen;
            if (i > 0 && s < prevS - maxDrop) {
                return false;
            }
            prevS = s;
        }
        return true;
    }

    private static boolean anchorCurveFreeOfOvershootLoopExplicit(BezierConnection bc, double h1, double h2) {
        return CoasterAnchorBezierOptimizer.handlesWithinReasonableScopeExplicit(bc, h1, h2) && CoasterAnchorBezierOptimizer.bezierChordProjectionMonotonicExplicit(bc, h1, h2);
    }

    private static boolean meetsAltSuppressMinCurveRadiusExplicit(BezierConnection bc, double handleLenFirst, double handleLenSecond) {
        double minRadius = 2.0;
        if (minRadius <= 0.0) {
            return true;
        }
        return CoasterAnchorBezierOptimizer.approximateMinEndRadiusExplicit(bc, handleLenFirst, handleLenSecond) >= minRadius;
    }

    private static double approximateMinEndRadiusExplicit(BezierConnection bc, double handleLenFirst, double handleLenSecond) {
        Vec3 e1 = (Vec3)bc.starts.getFirst();
        Vec3 e2 = (Vec3)bc.starts.getSecond();
        Vec3 a1 = ((Vec3)bc.axes.getFirst()).lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : ((Vec3)bc.axes.getFirst()).normalize();
        Vec3 a2 = ((Vec3)bc.axes.getSecond()).lengthSqr() < 1.0E-10 ? new Vec3(0.0, 1.0, 0.0) : ((Vec3)bc.axes.getSecond()).normalize();
        Vec3 f1 = e1.add(a1.scale(handleLenFirst));
        Vec3 f2 = e2.add(a2.scale(handleLenSecond));
        double endRegion = 0.25;
        int samples = 7;
        double minR = Double.POSITIVE_INFINITY;
        minR = Math.min(minR, CoasterAnchorBezierOptimizer.minRadiusAlongBezierSpan(e1, e2, f1, f2, 0.0, endRegion, samples));
        minR = Math.min(minR, CoasterAnchorBezierOptimizer.minRadiusAlongBezierSpan(e1, e2, f1, f2, 1.0 - endRegion, 1.0, samples));
        return minR;
    }

    private static double minRadiusAlongBezierSpan(Vec3 e1, Vec3 e2, Vec3 f1, Vec3 f2, double tStart, double tEnd, int samples) {
        if (samples < 3) {
            return Double.POSITIVE_INFINITY;
        }
        Vec3[] pts = new Vec3[samples];
        for (int i = 0; i < samples; ++i) {
            double t = tStart + (tEnd - tStart) * (double)i / (double)(samples - 1);
            pts[i] = VecHelper.bezier((Vec3)e1, (Vec3)e2, (Vec3)f1, (Vec3)f2, (float)((float)t));
        }
        double minR = Double.POSITIVE_INFINITY;
        for (int i = 0; i < samples - 2; ++i) {
            minR = Math.min(minR, CoasterAnchorBezierOptimizer.mengerRadius(pts[i], pts[i + 1], pts[i + 2]));
        }
        return minR;
    }

    private static double mengerRadius(Vec3 p0, Vec3 p1, Vec3 p2) {
        Vec3 edgeA = p1.subtract(p0);
        Vec3 edgeB = p2.subtract(p1);
        Vec3 span = p2.subtract(p0);
        double la = edgeA.length();
        double lb = edgeB.length();
        double lc = span.length();
        if (la < 1.0E-8 || lb < 1.0E-8 || lc < 1.0E-8) {
            return Double.POSITIVE_INFINITY;
        }
        double area2 = edgeA.cross(span).length();
        if (area2 < 1.0E-10) {
            return Double.POSITIVE_INFINITY;
        }
        return la * lb * lc / area2;
    }

    private static double distancePointToLine(Vec3 p, Vec3 linePoint, Vec3 lineDirUnit) {
        Vec3 d = p.subtract(linePoint);
        double along = d.dot(lineDirUnit);
        Vec3 perp = d.subtract(lineDirUnit.scale(along));
        return Math.sqrt(perp.lengthSqr());
    }

    @Nullable
    private static AnchorNeighborEndpoint firstNeighborEndpointForAnchorSmoothing(Level level, BlockPos anchorPos, BlockPos excludePeer) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
        BlockPos bestRemote = null;
        BezierConnection best = null;
        for (Map.Entry<BlockPos, BezierConnection> e : anchor.getAnchorPeerCurvesView().entrySet()) {
            BezierConnection c;
            BlockPos remote = e.getKey();
            if (remote.equals((Object)excludePeer) || (c = e.getValue()) == null || bestRemote != null && remote.asLong() >= bestRemote.asLong()) continue;
            bestRemote = remote.immutable();
            best = c;
        }
        if (best == null) {
            return null;
        }
        CoasterBezierEndpointData atAnchor = CoasterBezierEndpointData.at(best, anchorPos);
        CoasterBezierEndpointData atRemote = CoasterBezierEndpointData.at(best, bestRemote);
        if (atAnchor == null || atRemote == null) {
            return null;
        }
        return new AnchorNeighborEndpoint(bestRemote, atAnchor.start(), atAnchor.axis(), atAnchor.normal(), atRemote.start(), atRemote.axis(), atRemote.normal(), best);
    }

    private static boolean axisLexicographicallySmaller(Vec3 a, Vec3 b) {
        if (a.x != b.x) {
            return a.x < b.x;
        }
        if (a.y != b.y) {
            return a.y < b.y;
        }
        return a.z < b.z;
    }

    private static List<Vec3> sharedAnchorAxisCandidates(Vec3 existingEnd, Vec3 newEnd, Vec3 existingAnchorEnd, Vec3 newAnchorEnd, Vec3 existingFallback, Vec3 newFallback, @Nullable Vec3 coplanarPlaneNormal) {
        LinkedHashMap<String, Vec3> uniq = new LinkedHashMap<String, Vec3>();
        Vec3 seg = existingAnchorEnd.subtract(newAnchorEnd);
        if (seg.lengthSqr() > 1.0E-8) {
            CoasterAnchorBezierOptimizer.putSharedAxis(uniq, CoasterAnchorCoplanarPlane.projectAxisOntoPlane(seg.normalize(), coplanarPlaneNormal, seg.normalize()));
        }
        Vec3[] a = CoasterAnchorBezierOptimizer.anchorAxisCandidates(existingEnd, existingAnchorEnd, existingFallback, coplanarPlaneNormal);
        Vec3[] b = CoasterAnchorBezierOptimizer.anchorAxisCandidates(newEnd, newAnchorEnd, newFallback, coplanarPlaneNormal);
        for (Vec3 v : a) {
            CoasterAnchorBezierOptimizer.putSharedAxis(uniq, v);
        }
        for (Vec3 v : b) {
            CoasterAnchorBezierOptimizer.putSharedAxis(uniq, v);
        }
        return new ArrayList<Vec3>(uniq.values());
    }

    private static void putSharedAxis(LinkedHashMap<String, Vec3> uniq, Vec3 axis) {
        if (axis.lengthSqr() < 1.0E-8) {
            return;
        }
        Vec3 n = axis.normalize();
        double kx = Math.abs(n.x);
        double ky = Math.abs(n.y);
        double kz = Math.abs(n.z);
        String key = String.format("%.6f:%.6f:%.6f", kx, ky, kz);
        uniq.putIfAbsent(key, n);
    }

    private static Vec3[] anchorAxisCandidates(Vec3 trackEnd, Vec3 anchorEnd, Vec3 fallback, @Nullable Vec3 coplanarPlaneNormal) {
        Vec3 dir = trackEnd.subtract(anchorEnd);
        if (dir.lengthSqr() < 1.0E-8) {
            return new Vec3[]{CoasterAnchorCoplanarPlane.projectAxisOntoPlane(fallback, coplanarPlaneNormal, fallback)};
        }
        Vec3 base = CoasterAnchorCoplanarPlane.projectAxisOntoPlane(dir.normalize(), coplanarPlaneNormal, dir.normalize());
        Vec3 side = base.cross(new Vec3(0.0, 1.0, 0.0));
        if (side.lengthSqr() < 1.0E-8) {
            side = base.cross(new Vec3(1.0, 0.0, 0.0));
        }
        side = side.normalize();
        Vec3 up = side.cross(base).normalize();
        ArrayList<Vec3> out = new ArrayList<Vec3>();
        if (fallback.lengthSqr() > 1.0E-8) {
            out.add(CoasterAnchorCoplanarPlane.projectAxisOntoPlane(fallback, coplanarPlaneNormal, fallback));
        }
        out.add(base);
        for (int ring = 1; ring <= 4; ++ring) {
            double theta = Math.toRadians((double)ring * 10.0);
            double c = Math.cos(theta);
            double s = Math.sin(theta);
            int azSteps = 8 + ring * 2;
            for (int i = 0; i < azSteps; ++i) {
                double phi = Math.PI * 2 * (double)i / (double)azSteps;
                Vec3 radial = side.scale(Math.cos(phi)).add(up.scale(Math.sin(phi)));
                Vec3 v = base.scale(c).add(radial.scale(s));
                if (v.lengthSqr() < 1.0E-8) continue;
                out.add(CoasterAnchorCoplanarPlane.projectAxisOntoPlane(v, coplanarPlaneNormal, base));
            }
        }
        return out.toArray(new Vec3[0]);
    }

    public record AnchorAnchorBuildResult(BezierConnection primary, List<NeighborJoinAxisWrite> neighborJoinWrites, @Nullable List<BezierConnection> circularLoopNeighbors) {
        public AnchorAnchorBuildResult(BezierConnection primary, List<NeighborJoinAxisWrite> neighborJoinWrites) {
            this(primary, neighborJoinWrites, null);
        }
    }

    private record AxisOptimizationResult(Vec3 axisLo, Vec3 axisHi, @Nullable AnchorJoinOptimization optAtLo, @Nullable AnchorJoinOptimization optAtHi) {
    }

    private record LoHiAxes(Vec3 lo, Vec3 hi) {
    }

    public record AnchorJoinOptimization(Vec3 existingAxis, Vec3 newAxisAtAnchor, BlockPos remotePos, BezierConnection curve) {
    }

    public record AnchorNeighborEndpoint(BlockPos remotePos, Vec3 anchorStart, Vec3 anchorAxis, Vec3 anchorNormal, Vec3 remoteStart, Vec3 remoteAxis, Vec3 remoteNormal, BezierConnection curve) {
    }

    public record NeighborJoinAxisWrite(BlockPos sharedAnchor, BlockPos remotePos, Vec3 axisIntoCurveAtSharedAnchor) {
    }

    public record AnchorPair(Vec3 existingAxis, Vec3 newAxis) {
    }

    private record AxisSearchResult(Vec3 axis, double score) {
    }
}

