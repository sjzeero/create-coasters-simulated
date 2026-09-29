/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackMaterial
 *  net.createmod.catnip.data.Couple
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.silvergold.simulatedcoasters.track.CoasterAnchorBezierOptimizer;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterQuarterCirclePlacement {
    private static final double PERPENDICULAR_DOT_MAX = 0.08;
    private static final double ALIGN_DOT_MIN = 0.985;
    private static final double MIN_RADIUS_BLOCKS = 1.25;
    private static final double MIN_OPEN_STRAIGHT_BLOCKS = 0.5;
    private static final double PARALLEL_PLANE_NORMAL_DOT = 0.85;
    private static final double RADIAL_FACING_DOT = 0.45;
    private static final double KAPPA_QUARTER = 1.3333333333333333 * Math.tan(0.39269908169872414);
    private static final double STRAIGHT_AXIS_DOT_MIN = 0.985;
    private static final double CONTINUITY_OPPOSITE_DOT_MAX = -0.985;
    private static final double CLASS_FLIP_COST = 100.0;

    private CoasterQuarterCirclePlacement() {
    }

    @Nullable
    public static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildQuarterCircle(Level level, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder) {
        @Nullable CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult fromStraight = CoasterQuarterCirclePlacement.tryBuildOpenQuarterFromStraight(level, anchorA, anchorB, material, girder);
        if (fromStraight != null) {
            return fromStraight;
        }
        @Nullable CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult fromQuarter = CoasterQuarterCirclePlacement.tryBuildOpenStraightFromQuarter(level, anchorA, anchorB, material, girder);
        if (fromQuarter != null) {
            return fromQuarter;
        }
        @Nullable CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult closing = CoasterQuarterCirclePlacement.tryBuildClosingQuarter(level, anchorA, anchorB, material, girder);
        @Nullable CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult completing = CoasterQuarterCirclePlacement.tryBuildStraightCompletingQuarter(level, anchorA, anchorB, material, girder);
        if (closing == null) {
            return completing;
        }
        if (completing == null) {
            return closing;
        }
        return CoasterQuarterCirclePlacement.preferLessDramaticRewrite(level, closing, completing);
    }

    @Nullable
    private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildOpenQuarterFromStraight(Level level, BlockPos a, BlockPos b, TrackMaterial material, boolean girder) {
        CoasterAnchorpointBlockEntity beA;
        BlockEntity blockEntity;
        block6: {
            block5: {
                blockEntity = level.getBlockEntity(a);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block5;
                beA = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(b);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block6;
            }
            return null;
        }
        CoasterAnchorpointBlockEntity beB = (CoasterAnchorpointBlockEntity)blockEntity;
        if (beA.getAnchorPeerCurvesView().containsKey(b) || beB.getAnchorPeerCurvesView().containsKey(a)) {
            return null;
        }
        @Nullable CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult ab = CoasterQuarterCirclePlacement.openQuarterFromStraightRoles(level, a, beA, b, beB, material, girder);
        if (ab != null) {
            return ab;
        }
        return CoasterQuarterCirclePlacement.openQuarterFromStraightRoles(level, b, beB, a, beA, material, girder);
    }

    @Nullable
    private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult openQuarterFromStraightRoles(Level level, BlockPos corner, CoasterAnchorpointBlockEntity beCorner, BlockPos open, CoasterAnchorpointBlockEntity beOpen, TrackMaterial material, boolean girder) {
        if (beCorner.legCount() != 1 || beOpen.legCount() != 0) {
            return null;
        }
        BlockPos outer = CoasterQuarterCirclePlacement.solePeer(beCorner);
        if (outer == null || outer.equals((Object)open)) {
            return null;
        }
        @Nullable BezierConnection approach = CoasterQuarterCirclePlacement.existingPrimaryBetween(level, outer, corner);
        if (approach == null || !CoasterQuarterCirclePlacement.looksStraight(approach)) {
            return null;
        }
        @Nullable QuarterSolve solve = CoasterQuarterCirclePlacement.solveQuarter(level, outer, corner, open, null);
        if (solve == null) {
            return null;
        }
        BezierConnection quarter = CoasterQuarterCirclePlacement.buildQuarterCurve(corner, open, solve, material, girder);
        if (quarter == null || !CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(quarter)) {
            return null;
        }
        Vec3 intoApproach = CoasterBezierHandleEdit.tangentIntoFromAnchor(approach, corner);
        Vec3 intoQuarter = CoasterBezierHandleEdit.tangentIntoFromAnchor(quarter, corner);
        if (intoApproach == null || intoQuarter == null) {
            return null;
        }
        if (intoApproach.normalize().dot(intoQuarter.normalize()) > -0.985) {
            return null;
        }
        return new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(quarter, List.of(), List.of());
    }

    @Nullable
    private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildOpenStraightFromQuarter(Level level, BlockPos a, BlockPos b, TrackMaterial material, boolean girder) {
        CoasterAnchorpointBlockEntity beA;
        BlockEntity blockEntity;
        block6: {
            block5: {
                blockEntity = level.getBlockEntity(a);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block5;
                beA = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(b);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block6;
            }
            return null;
        }
        CoasterAnchorpointBlockEntity beB = (CoasterAnchorpointBlockEntity)blockEntity;
        if (beA.getAnchorPeerCurvesView().containsKey(b) || beB.getAnchorPeerCurvesView().containsKey(a)) {
            return null;
        }
        @Nullable CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult ab = CoasterQuarterCirclePlacement.openStraightFromQuarterRoles(level, a, beA, b, beB, material, girder);
        if (ab != null) {
            return ab;
        }
        return CoasterQuarterCirclePlacement.openStraightFromQuarterRoles(level, b, beB, a, beA, material, girder);
    }

    @Nullable
    private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult openStraightFromQuarterRoles(Level level, BlockPos corner, CoasterAnchorpointBlockEntity beCorner, BlockPos open, CoasterAnchorpointBlockEntity beOpen, TrackMaterial material, boolean girder) {
        if (beCorner.legCount() != 1 || beOpen.legCount() != 0) {
            return null;
        }
        BlockPos otherCorner = CoasterQuarterCirclePlacement.solePeer(beCorner);
        if (otherCorner == null || otherCorner.equals((Object)open)) {
            return null;
        }
        if (!CoasterQuarterCirclePlacement.allAnchorsHaveDefaultTilt(level, List.of(corner, open))) {
            return null;
        }
        @Nullable BezierConnection quarter = CoasterQuarterCirclePlacement.existingPrimaryBetween(level, otherCorner, corner);
        if (quarter == null || !CoasterQuarterCirclePlacement.looksQuarter(quarter)) {
            return null;
        }
        Vec3 intoQuarter = CoasterBezierHandleEdit.tangentIntoFromAnchor(quarter, corner);
        if (intoQuarter == null) {
            return null;
        }
        intoQuarter = intoQuarter.normalize();
        Vec3 travel = intoQuarter.scale(-1.0);
        Vec3 startCorner = ((BlockPos)quarter.bePositions.getFirst()).equals((Object)corner) ? (Vec3)quarter.starts.getFirst() : (Vec3)quarter.starts.getSecond();
        Vec3 startOpen = CoasterAnchorpointBlockEntity.worldCenter(level, open);
        Vec3 delta = startOpen.subtract(startCorner);
        double len = delta.length();
        if (len < 0.5) {
            return null;
        }
        if (delta.scale(1.0 / len).dot(travel) < 0.985) {
            return null;
        }
        Vec3 axisCorner = travel;
        Vec3 axisOpen = travel.scale(-1.0);
        Vec3 nCornerStored = ((BlockPos)quarter.bePositions.getFirst()).equals((Object)corner) ? (Vec3)quarter.normals.getFirst() : (Vec3)quarter.normals.getSecond();
        Vec3 normalCorner = CoasterTrackPlacement.orthonormalFaceNormalForAxis(level, corner, nCornerStored, axisCorner);
        Vec3 normalOpen = CoasterTrackPlacement.anchorFaceNormalForAxis(level, open, axisOpen);
        BezierConnection straight = CoasterQuarterCirclePlacement.primaryCurve(corner, open, startCorner, startOpen, axisCorner, axisOpen, normalCorner, normalOpen, material, girder);
        double h = Math.max(1.0E-4, len / 3.0);
        CoasterPeerCurveHandleLengths.putCanonical(straight, h, h);
        if (!CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straight) || !CoasterQuarterCirclePlacement.looksStraight(straight)) {
            return null;
        }
        Vec3 intoStraight = CoasterBezierHandleEdit.tangentIntoFromAnchor(straight, corner);
        if (intoStraight == null || intoQuarter.dot(intoStraight.normalize()) > -0.985) {
            return null;
        }
        return new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(straight, List.of(), List.of());
    }

    private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult preferLessDramaticRewrite(Level level, CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult a, CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult b) {
        double db;
        double da = CoasterQuarterCirclePlacement.rewriteDrama(level, a);
        return da <= (db = CoasterQuarterCirclePlacement.rewriteDrama(level, b)) ? a : b;
    }

    private static double rewriteDrama(Level level, CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult result) {
        List<BezierConnection> neighbors = result.circularLoopNeighbors();
        if (neighbors == null || neighbors.isEmpty()) {
            return 0.0;
        }
        double drama = 0.0;
        for (BezierConnection after : neighbors) {
            @Nullable BezierConnection before = CoasterQuarterCirclePlacement.existingPrimaryBetween(level, (BlockPos)after.bePositions.getFirst(), (BlockPos)after.bePositions.getSecond());
            if (before == null) continue;
            drama += CoasterQuarterCirclePlacement.edgeRewriteDrama(before, after);
        }
        return drama;
    }

    private static double edgeRewriteDrama(BezierConnection before, BezierConnection after) {
        boolean nowStraight;
        boolean wasStraight = CoasterQuarterCirclePlacement.looksStraight(before);
        double drama = wasStraight != (nowStraight = CoasterQuarterCirclePlacement.looksStraight(after)) ? 100.0 : 0.0;
        Vec3 before0 = (Vec3)before.starts.getFirst();
        Vec3 before1 = (Vec3)before.starts.getSecond();
        Vec3 after0 = (Vec3)after.starts.getFirst();
        Vec3 after1 = (Vec3)after.starts.getSecond();
        boolean sameOrder = ((BlockPos)before.bePositions.getFirst()).equals(after.bePositions.getFirst());
        if (!sameOrder) {
            after0 = (Vec3)after.starts.getSecond();
            after1 = (Vec3)after.starts.getFirst();
        }
        drama += before0.distanceTo(after0) + before1.distanceTo(after1);
        Vec3 ax0b = (Vec3)before.axes.getFirst();
        Vec3 ax1b = (Vec3)before.axes.getSecond();
        Vec3 ax0a = sameOrder ? (Vec3)after.axes.getFirst() : (Vec3)after.axes.getSecond();
        Vec3 ax1a = sameOrder ? (Vec3)after.axes.getSecond() : (Vec3)after.axes.getFirst();
        drama += CoasterQuarterCirclePlacement.axisDelta(ax0b, ax0a) + CoasterQuarterCirclePlacement.axisDelta(ax1b, ax1a);
        Couple<Double> hb = CoasterPeerCurveHandleLengths.getCanonicalLengths(before);
        Couple<Double> ha = CoasterPeerCurveHandleLengths.getCanonicalLengths(after);
        if (hb != null && ha != null) {
            double chord = Math.max(0.001, before0.distanceTo(before1));
            drama += Math.abs((Double)ha.getFirst() - (Double)hb.getFirst()) / chord + Math.abs((Double)ha.getSecond() - (Double)hb.getSecond()) / chord;
        }
        return drama;
    }

    private static double axisDelta(Vec3 a, Vec3 b) {
        if (a.lengthSqr() < 1.0E-12 || b.lengthSqr() < 1.0E-12) {
            return 1.0;
        }
        return 1.0 - Math.abs(a.normalize().dot(b.normalize()));
    }

    private static boolean looksStraight(BezierConnection bc) {
        Vec3 chord = ((Vec3)bc.starts.getSecond()).subtract((Vec3)bc.starts.getFirst());
        if (chord.lengthSqr() < 1.0E-12) {
            return true;
        }
        chord = chord.normalize();
        Vec3 a0 = (Vec3)bc.axes.getFirst();
        Vec3 a1 = (Vec3)bc.axes.getSecond();
        if (a0.lengthSqr() < 1.0E-12 || a1.lengthSqr() < 1.0E-12) {
            return false;
        }
        a0 = a0.normalize();
        a1 = a1.normalize();
        return a0.dot(chord) >= 0.985 && a1.dot(chord.scale(-1.0)) >= 0.985;
    }

    private static boolean looksQuarter(BezierConnection bc) {
        if (CoasterQuarterCirclePlacement.looksStraight(bc)) {
            return false;
        }
        Vec3 a0 = (Vec3)bc.axes.getFirst();
        Vec3 a1 = (Vec3)bc.axes.getSecond();
        if (a0.lengthSqr() < 1.0E-12 || a1.lengthSqr() < 1.0E-12) {
            return false;
        }
        if (Math.abs((a0 = a0.normalize()).dot(a1 = a1.normalize())) > 0.08) {
            return false;
        }
        Vec3 chord = ((Vec3)bc.starts.getSecond()).subtract((Vec3)bc.starts.getFirst());
        double dist = chord.length();
        if (dist < 1.25 * Math.sqrt(2.0) * 0.5) {
            return false;
        }
        double radius = dist / Math.sqrt(2.0);
        if (radius < 1.25) {
            return false;
        }
        chord = chord.normalize();
        Vec3 sum = a0.subtract(a1);
        if (sum.lengthSqr() < 1.0E-8) {
            return false;
        }
        if (chord.dot(sum.normalize()) < 0.985) {
            return false;
        }
        Couple<Double> handles = CoasterPeerCurveHandleLengths.getCanonicalLengths(bc);
        if (handles != null) {
            double expected = radius * KAPPA_QUARTER;
            double tol = Math.max(0.2, expected * 0.25);
            if (Math.abs((Double)handles.getFirst() - expected) > tol || Math.abs((Double)handles.getSecond() - expected) > tol) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildClosingQuarter(Level level, BlockPos cornerA, BlockPos cornerB, TrackMaterial material, boolean girder) {
        CoasterAnchorpointBlockEntity beA;
        BlockEntity blockEntity;
        block12: {
            block11: {
                blockEntity = level.getBlockEntity(cornerA);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block11;
                beA = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(cornerB);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block12;
            }
            return null;
        }
        CoasterAnchorpointBlockEntity beB = (CoasterAnchorpointBlockEntity)blockEntity;
        if (beA.legCount() != 1 || beB.legCount() != 1) {
            return null;
        }
        if (beA.getAnchorPeerCurvesView().containsKey(cornerB) || beB.getAnchorPeerCurvesView().containsKey(cornerA)) {
            return null;
        }
        BlockPos outerA = CoasterQuarterCirclePlacement.solePeer(beA);
        BlockPos outerB = CoasterQuarterCirclePlacement.solePeer(beB);
        if (outerA == null || outerB == null || outerA.equals((Object)outerB) || outerA.equals((Object)cornerB) || outerB.equals((Object)cornerA)) {
            return null;
        }
        @Nullable QuarterSolve solve = CoasterQuarterCirclePlacement.solveQuarter(level, outerA, cornerA, cornerB, outerB);
        if (solve == null) {
            return null;
        }
        BezierConnection quarter = CoasterQuarterCirclePlacement.buildQuarterCurve(cornerA, cornerB, solve, material, girder);
        BezierConnection straightA = CoasterQuarterCirclePlacement.buildStraightLeg(level, outerA, cornerA, material, girder, solve);
        BezierConnection straightB = CoasterQuarterCirclePlacement.buildStraightLeg(level, outerB, cornerB, material, girder, solve);
        if (quarter == null || straightA == null || straightB == null) {
            return null;
        }
        if (!(CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(quarter) && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightA) && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightB))) {
            return null;
        }
        ArrayList<BezierConnection> neighbors = new ArrayList<BezierConnection>(4);
        neighbors.add(straightA);
        neighbors.add(straightB);
        if (!CoasterQuarterCirclePlacement.appendOuterContinuityAdjustments(level, neighbors, outerA, cornerA, straightA)) {
            return null;
        }
        if (!CoasterQuarterCirclePlacement.appendOuterContinuityAdjustments(level, neighbors, outerB, cornerB, straightB)) {
            return null;
        }
        return new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(quarter, List.of(), List.copyOf(neighbors));
    }

    @Nullable
    private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildStraightCompletingQuarter(Level level, BlockPos a, BlockPos b, TrackMaterial material, boolean girder) {
        CoasterAnchorpointBlockEntity beA;
        BlockEntity blockEntity;
        block13: {
            block12: {
                blockEntity = level.getBlockEntity(a);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block12;
                beA = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(b);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block13;
            }
            return null;
        }
        CoasterAnchorpointBlockEntity beB = (CoasterAnchorpointBlockEntity)blockEntity;
        if (beA.getAnchorPeerCurvesView().containsKey(b) || beB.getAnchorPeerCurvesView().containsKey(a)) {
            return null;
        }
        @Nullable CornerStraightRoles roles = CoasterQuarterCirclePlacement.resolveStraightOntoCornerRoles(beA, a, beB, b);
        if (roles == null) {
            return null;
        }
        BlockPos otherOuter = CoasterQuarterCirclePlacement.otherApproachPeer(level, roles.otherCorner(), roles.corner());
        if (otherOuter == null) {
            return null;
        }
        @Nullable QuarterSolve solve = CoasterQuarterCirclePlacement.solveQuarter(level, roles.outer(), roles.corner(), roles.otherCorner(), otherOuter);
        if (solve == null) {
            return null;
        }
        BezierConnection straightNew = CoasterQuarterCirclePlacement.buildStraightLeg(level, roles.outer(), roles.corner(), material, girder, solve);
        BezierConnection quarter = CoasterQuarterCirclePlacement.buildQuarterCurve(roles.corner(), roles.otherCorner(), solve, material, girder);
        if (straightNew == null || quarter == null) {
            return null;
        }
        if (!CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightNew) || !CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(quarter)) {
            return null;
        }
        BezierConnection straightOther = CoasterQuarterCirclePlacement.buildStraightLeg(level, otherOuter, roles.otherCorner(), material, girder, solve);
        if (straightOther == null || !CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightOther)) {
            return null;
        }
        ArrayList<BezierConnection> neighbors = new ArrayList<BezierConnection>(4);
        neighbors.add(quarter);
        neighbors.add(straightOther);
        if (!CoasterQuarterCirclePlacement.appendOuterContinuityAdjustments(level, neighbors, roles.outer(), roles.corner(), straightNew)) {
            return null;
        }
        if (!CoasterQuarterCirclePlacement.appendOuterContinuityAdjustments(level, neighbors, otherOuter, roles.otherCorner(), straightOther)) {
            return null;
        }
        return new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(straightNew, List.of(), List.copyOf(neighbors));
    }

    private static boolean appendOuterContinuityAdjustments(Level level, List<BezierConnection> neighbors, BlockPos outer, BlockPos corner, BezierConnection straightenedApproach) {
        BlockEntity blockEntity = level.getBlockEntity(outer);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return true;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        Vec3 intoApproach = CoasterBezierHandleEdit.tangentIntoFromAnchor(straightenedApproach, outer);
        if (intoApproach == null) {
            return false;
        }
        Vec3 intoBeyond = intoApproach.scale(-1.0);
        for (BlockPos peer : be.getAnchorPeerCurvesView().keySet()) {
            if (peer.equals((Object)corner)) continue;
            @Nullable BezierConnection existing = CoasterQuarterCirclePlacement.existingPrimaryBetween(level, outer, peer);
            if (existing == null) {
                return false;
            }
            BezierConnection before = existing;
            BezierConnection adjusted = CoasterTrackPlacement.curveWithAxisAtEndpointPreservingPeerForJoin(level, existing, outer, intoBeyond);
            CoasterPeerCurveHandleLengths.carryLengthsToReplacement(before, adjusted);
            if (!CoasterQuarterCirclePlacement.isAdjustedOuterCurveValid(adjusted)) {
                return false;
            }
            neighbors.add(adjusted);
        }
        return true;
    }

    private static boolean isAdjustedOuterCurveValid(BezierConnection bc) {
        return CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(bc);
    }

    @Nullable
    private static BezierConnection existingPrimaryBetween(Level level, BlockPos a, BlockPos b) {
        CoasterAnchorpointBlockEntity beB;
        CoasterAnchorpointBlockEntity beA;
        BlockEntity c;
        BlockEntity blockEntity = level.getBlockEntity(a);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity && (c = (beA = (CoasterAnchorpointBlockEntity)blockEntity).getAnchorPeerCurvesView().get(b)) != null) {
            return c.isPrimary() ? c : c.secondary();
        }
        c = level.getBlockEntity(b);
        if (c instanceof CoasterAnchorpointBlockEntity && (c = (beB = (CoasterAnchorpointBlockEntity)c).getAnchorPeerCurvesView().get(a)) != null) {
            return c.isPrimary() ? c : c.secondary();
        }
        return null;
    }

    @Nullable
    private static CornerStraightRoles resolveStraightOntoCornerRoles(CoasterAnchorpointBlockEntity beA, BlockPos a, CoasterAnchorpointBlockEntity beB, BlockPos b) {
        @Nullable CornerStraightRoles ab = CoasterQuarterCirclePlacement.rolesIfStraightOntoCorner(beA, a, beB, b);
        if (ab != null) {
            return ab;
        }
        return CoasterQuarterCirclePlacement.rolesIfStraightOntoCorner(beB, b, beA, a);
    }

    @Nullable
    private static CornerStraightRoles rolesIfStraightOntoCorner(CoasterAnchorpointBlockEntity beOuter, BlockPos outer, CoasterAnchorpointBlockEntity beCorner, BlockPos corner) {
        BlockPos outerPeer;
        if (beCorner.legCount() != 1) {
            return null;
        }
        if (beOuter.legCount() > 1) {
            return null;
        }
        if (beOuter.legCount() == 1 && ((outerPeer = CoasterQuarterCirclePlacement.solePeer(beOuter)) == null || outerPeer.equals((Object)corner))) {
            return null;
        }
        BlockPos otherCorner = CoasterQuarterCirclePlacement.solePeer(beCorner);
        if (otherCorner == null || otherCorner.equals((Object)outer)) {
            return null;
        }
        return new CornerStraightRoles(outer.immutable(), corner.immutable(), otherCorner.immutable());
    }

    @Nullable
    private static BlockPos otherApproachPeer(Level level, BlockPos otherCorner, BlockPos corner) {
        BlockEntity blockEntity = level.getBlockEntity(otherCorner);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        BlockPos found = null;
        for (BlockPos peer : be.getAnchorPeerCurvesView().keySet()) {
            if (peer.equals((Object)corner)) continue;
            if (found != null) {
                return null;
            }
            found = peer.immutable();
        }
        return found;
    }

    @Nullable
    private static QuarterSolve solveQuarter(Level level, BlockPos outerA, BlockPos cornerA, BlockPos cornerB, @Nullable BlockPos otherOuter) {
        Vec3 tB;
        Vec3 cornerCenter;
        double dist;
        Vec3 delta;
        Vec3 travelB;
        ArrayList<BlockPos> involved = new ArrayList<BlockPos>(4);
        involved.add(outerA);
        involved.add(cornerA);
        involved.add(cornerB);
        if (otherOuter != null) {
            involved.add(otherOuter);
        }
        if (!CoasterQuarterCirclePlacement.allAnchorsHaveDefaultTilt(level, involved)) {
            return null;
        }
        Vec3 cA = CoasterAnchorpointBlockEntity.worldCenter(level, cornerA);
        Vec3 cB = CoasterAnchorpointBlockEntity.worldCenter(level, cornerB);
        Vec3 cP = CoasterAnchorpointBlockEntity.worldCenter(level, outerA);
        Vec3 travelA = cA.subtract(cP);
        if (travelA.lengthSqr() < 1.0E-8) {
            return null;
        }
        travelA = travelA.normalize();
        if (otherOuter != null) {
            Vec3 cQ = CoasterAnchorpointBlockEntity.worldCenter(level, otherOuter);
            travelB = cQ.subtract(cB);
            if (travelB.lengthSqr() < 1.0E-8) {
                return null;
            }
            travelB = travelB.normalize();
        } else {
            delta = cB.subtract(cA);
            dist = delta.length();
            if (dist < 1.25 * Math.sqrt(2.0) * 0.5) {
                return null;
            }
            double r = dist / Math.sqrt(2.0);
            travelB = delta.scale(1.0 / r).subtract(travelA);
            if (travelB.lengthSqr() < 1.0E-8) {
                return null;
            }
            travelB = travelB.normalize();
        }
        if (Math.abs(travelA.dot(travelB)) > 0.08) {
            return null;
        }
        delta = cB.subtract(cA);
        dist = delta.length();
        if (dist < 1.25 * Math.sqrt(2.0) * 0.5) {
            return null;
        }
        Vec3 sum = travelA.add(travelB);
        if (sum.lengthSqr() < 1.0E-8) {
            return null;
        }
        sum = sum.normalize();
        if (delta.normalize().dot(sum) < 0.985) {
            return null;
        }
        double radius = dist / Math.sqrt(2.0);
        if (radius < 1.25) {
            return null;
        }
        Vec3 expectedB = cA.add(travelA.add(travelB).scale(radius));
        if (expectedB.distanceToSqr(cB) > radius * 0.06 * (radius * 0.06)) {
            return null;
        }
        Vec3 planeN = travelA.cross(travelB);
        if (planeN.lengthSqr() < 1.0E-12) {
            return null;
        }
        @Nullable Orientation orientation = CoasterQuarterCirclePlacement.classifyOrientation(level, involved, planeN = planeN.normalize(), cornerCenter = cA.add(travelB.scale(radius)));
        if (orientation == null) {
            return null;
        }
        if (orientation.radial() && !CoasterQuarterCirclePlacement.allAnchorsHaveDefaultLift(level, involved)) {
            return null;
        }
        if (orientation.radial()) {
            return new QuarterSolve(orientation, radius, planeN, cornerCenter, cA, cB, travelA, travelB, true);
        }
        Vec3 baseA = CoasterAnchorpointBlockEntity.worldCenterBase(level, cornerA);
        Vec3 baseB = CoasterAnchorpointBlockEntity.worldCenterBase(level, cornerB);
        Vec3 baseP = CoasterAnchorpointBlockEntity.worldCenterBase(level, outerA);
        Vec3 tA = baseA.subtract(baseP);
        if (tA.lengthSqr() < 1.0E-8) {
            return null;
        }
        if ((tA = tA.normalize()).dot(travelA) < 0.0) {
            tA = tA.scale(-1.0);
        }
        if (otherOuter != null) {
            Vec3 baseQ = CoasterAnchorpointBlockEntity.worldCenterBase(level, otherOuter);
            tB = baseQ.subtract(baseB);
            if (tB.lengthSqr() < 1.0E-8) {
                return null;
            }
            if ((tB = tB.normalize()).dot(travelB) < 0.0) {
                tB = tB.scale(-1.0);
            }
        } else {
            Vec3 d = baseB.subtract(baseA);
            double br = d.length() / Math.sqrt(2.0);
            if (br < 1.25) {
                return null;
            }
            tB = d.scale(1.0 / br).subtract(tA);
            if (tB.lengthSqr() < 1.0E-8) {
                return null;
            }
            tB = tB.normalize();
        }
        if (Math.abs(tA.dot(tB)) > 0.08) {
            return null;
        }
        double baseDist = baseB.distanceTo(baseA);
        double baseR = baseDist / Math.sqrt(2.0);
        if (baseR < 1.25) {
            return null;
        }
        Vec3 baseSum = tA.add(tB);
        if (baseSum.lengthSqr() < 1.0E-8) {
            return null;
        }
        if (baseB.subtract(baseA).normalize().dot(baseSum.normalize()) < 0.985) {
            return null;
        }
        Vec3 baseN = tA.cross(tB);
        if (baseN.lengthSqr() < 1.0E-12) {
            return null;
        }
        if ((baseN = baseN.normalize()).dot(orientation.planeNormal()) < 0.0) {
            baseN = baseN.scale(-1.0);
        }
        Vec3 baseCenter = baseA.add(tB.scale(baseR));
        return new QuarterSolve(orientation, baseR, baseN, baseCenter, baseA, baseB, tA, tB, false);
    }

    @Nullable
    private static BezierConnection buildQuarterCurve(BlockPos cornerA, BlockPos cornerB, QuarterSolve solve, TrackMaterial material, boolean girder) {
        Vec3 axisA = solve.travelA();
        Vec3 axisB = solve.travelB().scale(-1.0);
        Vec3 normalA = CoasterQuarterCirclePlacement.faceNormal(solve.orientation(), solve.startCornerA(), solve.cornerCenter(), axisA, solve.planeN());
        Vec3 normalB = CoasterQuarterCirclePlacement.faceNormal(solve.orientation(), solve.startCornerB(), solve.cornerCenter(), axisB, solve.planeN());
        BezierConnection quarter = CoasterQuarterCirclePlacement.primaryCurve(cornerA, cornerB, solve.startCornerA(), solve.startCornerB(), axisA, axisB, normalA, normalB, material, girder);
        double handle = Math.max(1.0E-4, solve.radius() * KAPPA_QUARTER);
        CoasterPeerCurveHandleLengths.putCanonical(quarter, handle, handle);
        return quarter;
    }

    @Nullable
    private static Orientation classifyOrientation(Level level, List<BlockPos> anchors, Vec3 planeN, Vec3 cornerCenter) {
        int parallelSign = 0;
        int radialSign = 0;
        int parallelCount = 0;
        int radialCount = 0;
        for (BlockPos pos : anchors) {
            int s;
            Vec3 up = CoasterAnchorpointBlockEntity.localUp(level, pos);
            double planeDot = up.dot(planeN);
            if (Math.abs(planeDot) >= 0.85) {
                int s2;
                int n = s2 = planeDot >= 0.0 ? 1 : -1;
                if (parallelSign == 0) {
                    parallelSign = s2;
                } else if (parallelSign != s2) {
                    return null;
                }
                ++parallelCount;
                continue;
            }
            Vec3 toward = cornerCenter.subtract(CoasterAnchorpointBlockEntity.worldCenterBase(level, pos));
            if ((toward = toward.subtract(planeN.scale(toward.dot(planeN)))).lengthSqr() < 1.0E-12) {
                return null;
            }
            double radialDot = up.dot(toward = toward.normalize());
            if (Math.abs(radialDot) < 0.45) {
                return null;
            }
            int n = s = radialDot >= 0.0 ? 1 : -1;
            if (radialSign == 0) {
                radialSign = s;
            } else if (radialSign != s) {
                return null;
            }
            ++radialCount;
        }
        if (parallelCount == anchors.size()) {
            Vec3 faceUp = planeN.normalize();
            if (parallelSign < 0) {
                faceUp = faceUp.scale(-1.0);
            }
            return new Orientation(false, false, planeN, faceUp);
        }
        if (radialCount == anchors.size()) {
            return new Orientation(true, radialSign > 0, planeN, Vec3.ZERO);
        }
        return null;
    }

    private static Vec3 faceNormal(Orientation orientation, Vec3 start, Vec3 cornerCenter, Vec3 axis, Vec3 planeN) {
        if (orientation.radial()) {
            Vec3 toward = cornerCenter.subtract(start);
            toward = (toward = toward.subtract(planeN.scale(toward.dot(planeN)))).lengthSqr() < 1.0E-12 ? (orientation.towardCenter() ? planeN : planeN.scale(-1.0)) : toward.normalize();
            Vec3 face = orientation.towardCenter() ? toward : toward.scale(-1.0);
            return CoasterQuarterCirclePlacement.orthonormalToAxis(face, axis);
        }
        return CoasterQuarterCirclePlacement.orthonormalToAxis(orientation.parallelFaceUp(), axis);
    }

    @Nullable
    private static BezierConnection buildStraightLeg(Level level, BlockPos a, BlockPos b, TrackMaterial material, boolean girder, QuarterSolve solve) {
        Vec3 dir;
        Vec3 startA = solve.useLiftedCenters() ? CoasterAnchorpointBlockEntity.worldCenter(level, a) : CoasterAnchorpointBlockEntity.worldCenterBase(level, a);
        Vec3 startB = solve.useLiftedCenters() ? CoasterAnchorpointBlockEntity.worldCenter(level, b) : CoasterAnchorpointBlockEntity.worldCenterBase(level, b);
        Vec3 chord = startB.subtract(startA);
        if (chord.lengthSqr() < 1.0E-8) {
            return null;
        }
        Vec3 axisA = dir = chord.normalize();
        Vec3 axisB = dir.scale(-1.0);
        Vec3 normalA = CoasterQuarterCirclePlacement.faceNormal(solve.orientation(), startA, solve.cornerCenter(), axisA, solve.planeN());
        Vec3 normalB = CoasterQuarterCirclePlacement.faceNormal(solve.orientation(), startB, solve.cornerCenter(), axisB, solve.planeN());
        BezierConnection bc = CoasterQuarterCirclePlacement.primaryCurve(a, b, startA, startB, axisA, axisB, normalA, normalB, material, girder);
        double h = Math.max(1.0E-4, startA.distanceTo(startB) / 3.0);
        CoasterPeerCurveHandleLengths.putCanonical(bc, h, h);
        return bc;
    }

    private static BezierConnection primaryCurve(BlockPos a, BlockPos b, Vec3 startA, Vec3 startB, Vec3 axisA, Vec3 axisB, Vec3 normalA, Vec3 normalB, TrackMaterial material, boolean girder) {
        long bl;
        long al = a.asLong();
        BlockPos lo = al <= (bl = b.asLong()) ? a : b;
        BlockPos hi = al <= bl ? b : a;
        Vec3 startLo = lo.equals((Object)a) ? startA : startB;
        Vec3 startHi = hi.equals((Object)a) ? startA : startB;
        Vec3 axisLo = lo.equals((Object)a) ? axisA : axisB;
        Vec3 axisHi = hi.equals((Object)a) ? axisA : axisB;
        Vec3 normalLo = lo.equals((Object)a) ? normalA : normalB;
        Vec3 normalHi = hi.equals((Object)a) ? normalA : normalB;
        return new BezierConnection(Couple.create((Object)lo, (Object)hi), Couple.create((Object)startLo, (Object)startHi), Couple.create((Object)axisLo.normalize(), (Object)axisHi.normalize()), Couple.create((Object)normalLo, (Object)normalHi), true, girder, material);
    }

    @Nullable
    private static BlockPos solePeer(CoasterAnchorpointBlockEntity be) {
        if (be.legCount() != 1) {
            return null;
        }
        return be.getAnchorPeerCurvesView().keySet().iterator().next().immutable();
    }

    private static boolean allAnchorsHaveDefaultTilt(Level level, List<BlockPos> anchors) {
        for (BlockPos pos : anchors) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
                return false;
            }
            CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
            if (!(Math.abs(be.getJointBankRadians() - 0.0f) > 1.0E-4f)) continue;
            return false;
        }
        return true;
    }

    private static boolean allAnchorsHaveDefaultLift(Level level, List<BlockPos> anchors) {
        float def = 0.5f;
        for (BlockPos pos : anchors) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
                return false;
            }
            CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
            if (!(Math.abs(be.getLiftBlocks() - def) > 0.001f)) continue;
            return false;
        }
        return true;
    }

    private static Vec3 orthonormalToAxis(Vec3 normal, Vec3 axis) {
        Vec3 a = axis.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : axis.normalize();
        Vec3 n = normal.subtract(a.scale(a.dot(normal)));
        if (n.lengthSqr() < 1.0E-12) {
            n = new Vec3(0.0, 1.0, 0.0).subtract(a.scale(a.dot(new Vec3(0.0, 1.0, 0.0))));
        }
        if (n.lengthSqr() < 1.0E-12) {
            n = a.cross(new Vec3(1.0, 0.0, 0.0));
        }
        if (n.lengthSqr() < 1.0E-12) {
            n = a.cross(new Vec3(0.0, 0.0, 1.0));
        }
        return n.normalize();
    }

    private record QuarterSolve(Orientation orientation, double radius, Vec3 planeN, Vec3 cornerCenter, Vec3 startCornerA, Vec3 startCornerB, Vec3 travelA, Vec3 travelB, boolean useLiftedCenters) {
    }

    private record CornerStraightRoles(BlockPos outer, BlockPos corner, BlockPos otherCorner) {
    }

    private record Orientation(boolean radial, boolean towardCenter, Vec3 planeNormal, Vec3 parallelFaceUp) {
    }
}

