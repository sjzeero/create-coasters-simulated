/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackMaterial
 *  net.createmod.catnip.data.Couple
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
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
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphLookup;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathNode;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterHalfCirclePlacement {
    private static final double PERPENDICULAR_DOT_MAX = 0.08;
    private static final double ALIGN_DOT_MIN = 0.985;
    private static final double MIN_RADIUS_BLOCKS = 1.25;
    private static final double PARALLEL_PLANE_NORMAL_DOT = 0.85;
    private static final double RADIAL_FACING_DOT = 0.45;
    private static final double KAPPA_QUARTER = 1.3333333333333333 * Math.tan(0.39269908169872414);
    private static final double CONTINUITY_OPPOSITE_DOT_MAX = -0.985;
    private static final double INV_SQRT2 = 1.0 / Math.sqrt(2.0);

    private CoasterHalfCirclePlacement() {
    }

    @Nullable
    public static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildHalfCircle(Level level, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder) {
        if (anchorA.equals((Object)anchorB)) {
            return null;
        }
        if (!(level.getBlockEntity(anchorA) instanceof CoasterAnchorpointBlockEntity) || !(level.getBlockEntity(anchorB) instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        if (CoasterHalfCirclePlacement.existingPrimaryBetween(level, anchorA, anchorB) != null) {
            return null;
        }
        for (Roles roles : CoasterHalfCirclePlacement.collectRoleCandidates(level, anchorA, anchorB)) {
            @Nullable CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult built = CoasterHalfCirclePlacement.tryBuildWithRoles(level, roles, anchorA, anchorB, material, girder);
            if (built == null) continue;
            return built;
        }
        return null;
    }

    @Nullable
    private static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildWithRoles(Level level, Roles roles, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder) {
        List<BlockPos> involved = List.of(roles.outerA(), roles.cornerA(), roles.mid(), roles.cornerB(), roles.outerB());
        if (!CoasterHalfCirclePlacement.allAnchorsHaveDefaultTilt(level, involved)) {
            return null;
        }
        @Nullable HalfSolve solve = CoasterHalfCirclePlacement.solveHalf(level, roles);
        if (solve == null) {
            return null;
        }
        BezierConnection straightA = CoasterHalfCirclePlacement.buildStraightLeg(roles.outerA(), roles.cornerA(), solve.startOuterA(), solve.startCornerA(), material, girder, solve);
        BezierConnection quarter1 = CoasterHalfCirclePlacement.buildQuarter(roles.cornerA(), roles.mid(), solve.startCornerA(), solve.startMid(), solve.travel(), solve.dirAB(), solve, material, girder);
        BezierConnection quarter2 = CoasterHalfCirclePlacement.buildQuarter(roles.mid(), roles.cornerB(), solve.startMid(), solve.startCornerB(), solve.dirAB(), solve.travel().scale(-1.0), solve, material, girder);
        BezierConnection straightB = CoasterHalfCirclePlacement.buildStraightLeg(roles.outerB(), roles.cornerB(), solve.startOuterB(), solve.startCornerB(), material, girder, solve);
        if (straightA == null || quarter1 == null || quarter2 == null || straightB == null) {
            return null;
        }
        if (!(CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightA) && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(quarter1) && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(quarter2) && CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(straightB))) {
            return null;
        }
        List<BezierConnection> all = List.of(straightA, quarter1, quarter2, straightB);
        BezierConnection primary = null;
        ArrayList<BezierConnection> neighbors = new ArrayList<BezierConnection>(6);
        for (BezierConnection bc : all) {
            if (CoasterHalfCirclePlacement.isEdge(bc, anchorA, anchorB)) {
                primary = bc;
                continue;
            }
            neighbors.add(bc);
        }
        if (primary == null) {
            return null;
        }
        if (!CoasterHalfCirclePlacement.appendOuterContinuityAdjustments(level, neighbors, roles.outerA(), roles.cornerA(), straightA)) {
            return null;
        }
        if (!CoasterHalfCirclePlacement.appendOuterContinuityAdjustments(level, neighbors, roles.outerB(), roles.cornerB(), straightB)) {
            return null;
        }
        return new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(primary, List.of(), List.copyOf(neighbors));
    }

    private static List<Roles> collectRoleCandidates(Level level, BlockPos a, BlockPos b) {
        ArrayList<Roles> out = new ArrayList<Roles>(12);
        CoasterHalfCirclePlacement.addCandidate(out, CoasterHalfCirclePlacement.detectPlacingNearQuarter(level, a, b));
        CoasterHalfCirclePlacement.addCandidate(out, CoasterHalfCirclePlacement.detectPlacingNearQuarter(level, b, a));
        CoasterHalfCirclePlacement.addCandidate(out, CoasterHalfCirclePlacement.detectPlacingFarQuarter(level, a, b));
        CoasterHalfCirclePlacement.addCandidate(out, CoasterHalfCirclePlacement.detectPlacingFarQuarter(level, b, a));
        CoasterHalfCirclePlacement.addCandidate(out, CoasterHalfCirclePlacement.detectClosingOntoEmptyMid(level, a, b));
        CoasterHalfCirclePlacement.addCandidate(out, CoasterHalfCirclePlacement.detectClosingOntoEmptyMid(level, b, a));
        CoasterHalfCirclePlacement.addCandidate(out, CoasterHalfCirclePlacement.detectPlacingStraight(level, a, b));
        CoasterHalfCirclePlacement.addCandidate(out, CoasterHalfCirclePlacement.detectPlacingStraight(level, b, a));
        CoasterHalfCirclePlacement.addCandidate(out, CoasterHalfCirclePlacement.detectStraightOffQuarterWithFarParallel(level, a, b));
        CoasterHalfCirclePlacement.addCandidate(out, CoasterHalfCirclePlacement.detectStraightOffQuarterWithFarParallel(level, b, a));
        return out;
    }

    private static void addCandidate(List<Roles> out, @Nullable Roles roles) {
        if (roles == null) {
            return;
        }
        for (Roles existing : out) {
            if (!existing.equals(roles)) continue;
            return;
        }
        out.add(roles);
    }

    @Nullable
    private static Roles detectStraightOffQuarterWithFarParallel(Level level, BlockPos outer, BlockPos corner) {
        BlockPos outerB;
        BlockPos cornerB;
        BlockPos outerPeer;
        CoasterAnchorpointBlockEntity beOuter;
        BlockEntity blockEntity;
        block34: {
            block33: {
                blockEntity = level.getBlockEntity(outer);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block33;
                beOuter = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(corner);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block34;
            }
            return null;
        }
        CoasterAnchorpointBlockEntity beCorner = (CoasterAnchorpointBlockEntity)blockEntity;
        if (beOuter.legCount() > 1) {
            return null;
        }
        if (beOuter.legCount() == 1 && ((outerPeer = CoasterHalfCirclePlacement.solePeer(beOuter)) == null || outerPeer.equals((Object)corner))) {
            return null;
        }
        if (beCorner.legCount() != 1) {
            return null;
        }
        BlockPos mid = CoasterHalfCirclePlacement.solePeer(beCorner);
        if (mid == null || mid.equals((Object)outer)) {
            return null;
        }
        BlockEntity blockEntity2 = level.getBlockEntity(mid);
        if (!(blockEntity2 instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity beMid = (CoasterAnchorpointBlockEntity)blockEntity2;
        @Nullable BezierConnection quarter = CoasterHalfCirclePlacement.existingPrimaryBetween(level, mid, corner);
        if (quarter == null) {
            return null;
        }
        Vec3 intoQuarter = CoasterBezierHandleEdit.tangentIntoFromAnchor(quarter, corner);
        if (intoQuarter == null) {
            return null;
        }
        intoQuarter = intoQuarter.normalize();
        Vec3 cCorner = CoasterAnchorpointBlockEntity.worldCenter(level, corner);
        Vec3 cPeer = CoasterAnchorpointBlockEntity.worldCenter(level, mid);
        Vec3 toPeer = cPeer.subtract(cCorner);
        if (toPeer.lengthSqr() < 1.0E-8 || toPeer.normalize().dot(intoQuarter) < 0.45) {
            return null;
        }
        Vec3 alongStraight = intoQuarter.scale(-1.0);
        Vec3 cOuter = CoasterAnchorpointBlockEntity.worldCenter(level, outer);
        Vec3 delta = cOuter.subtract(cCorner);
        double len = delta.length();
        if (len < 0.5) {
            return null;
        }
        if (delta.scale(1.0 / len).dot(alongStraight) < 0.985) {
            return null;
        }
        Vec3 travelApproach = intoQuarter;
        if (beMid.legCount() == 2) {
            CoasterAnchorpointBlockEntity beCornerB;
            cornerB = CoasterHalfCirclePlacement.otherPeer(beMid, corner);
            if (cornerB == null || cornerB.equals((Object)outer)) {
                return null;
            }
            BlockEntity blockEntity3 = level.getBlockEntity(cornerB);
            if (!(blockEntity3 instanceof CoasterAnchorpointBlockEntity) || (beCornerB = (CoasterAnchorpointBlockEntity)blockEntity3).legCount() < 2) {
                return null;
            }
            outerB = CoasterHalfCirclePlacement.otherPeer(beCornerB, mid);
            if (outerB == null || outerB.equals((Object)outer) || outerB.equals((Object)corner) || outerB.equals((Object)mid)) {
                return null;
            }
            if (CoasterHalfCirclePlacement.existingPrimaryBetween(level, mid, cornerB) == null || CoasterHalfCirclePlacement.existingPrimaryBetween(level, cornerB, outerB) == null) {
                return null;
            }
        } else if (beMid.legCount() == 1) {
            CoasterAnchorpointBlockEntity beCornerB;
            Vec3 cM = CoasterAnchorpointBlockEntity.worldCenter(level, mid);
            Vec3 chord = cM.subtract(cCorner);
            double chordLen = chord.length();
            if (chordLen < 1.25 * Math.sqrt(2.0) * 0.5) {
                return null;
            }
            double chordDot = Math.abs(chord.normalize().dot(travelApproach));
            if (Math.abs(chordDot - INV_SQRT2) > 0.28) {
                return null;
            }
            double radius = chordLen / Math.sqrt(2.0);
            if (radius < 1.25) {
                return null;
            }
            Vec3 dirAB = chord.scale(1.0 / radius).subtract(travelApproach);
            if (dirAB.lengthSqr() < 1.0E-8 || Math.abs(dirAB.length() - 1.0) > 0.12) {
                return null;
            }
            if (Math.abs((dirAB = dirAB.normalize()).dot(travelApproach)) > 0.08) {
                return null;
            }
            Vec3 center = cM.subtract(travelApproach.scale(radius));
            double fitTol = Math.max(0.75, radius * 0.12);
            if (cCorner.distanceToSqr(center.subtract(dirAB.scale(radius))) > fitTol * fitTol) {
                return null;
            }
            Vec3 expectedB = center.add(dirAB.scale(radius));
            cornerB = CoasterHalfCirclePlacement.findParallelFarCorner(level, expectedB, travelApproach, fitTol, corner, mid, outer);
            if (cornerB == null) {
                return null;
            }
            BlockEntity blockEntity4 = level.getBlockEntity(cornerB);
            if (!(blockEntity4 instanceof CoasterAnchorpointBlockEntity) || (beCornerB = (CoasterAnchorpointBlockEntity)blockEntity4).legCount() != 1) {
                return null;
            }
            outerB = CoasterHalfCirclePlacement.solePeer(beCornerB);
            if (outerB == null || outerB.equals((Object)outer) || outerB.equals((Object)corner) || outerB.equals((Object)mid) || outerB.equals((Object)cornerB)) {
                return null;
            }
            if (CoasterHalfCirclePlacement.existingPrimaryBetween(level, cornerB, outerB) == null) {
                return null;
            }
            if (CoasterHalfCirclePlacement.existingPrimaryBetween(level, mid, cornerB) != null) {
                return null;
            }
        } else {
            return null;
        }
        return new Roles(outer.immutable(), corner.immutable(), mid.immutable(), cornerB.immutable(), outerB.immutable());
    }

    @Nullable
    private static Roles detectClosingOntoEmptyMid(Level level, BlockPos corner, BlockPos mid) {
        CoasterAnchorpointBlockEntity beCornerB;
        CoasterAnchorpointBlockEntity beCorner;
        BlockEntity blockEntity;
        block19: {
            block18: {
                blockEntity = level.getBlockEntity(corner);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block18;
                beCorner = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(mid);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block19;
            }
            return null;
        }
        CoasterAnchorpointBlockEntity beMid = (CoasterAnchorpointBlockEntity)blockEntity;
        if (beCorner.legCount() != 1 || beMid.legCount() != 0) {
            return null;
        }
        BlockPos outerA = CoasterHalfCirclePlacement.solePeer(beCorner);
        if (outerA == null || outerA.equals((Object)mid)) {
            return null;
        }
        Vec3 cOA = CoasterAnchorpointBlockEntity.worldCenter(level, outerA);
        Vec3 cA = CoasterAnchorpointBlockEntity.worldCenter(level, corner);
        Vec3 cM = CoasterAnchorpointBlockEntity.worldCenter(level, mid);
        Vec3 travel = cA.subtract(cOA);
        if (travel.lengthSqr() < 1.0E-8) {
            return null;
        }
        travel = travel.normalize();
        Vec3 chord = cM.subtract(cA);
        double chordLen = chord.length();
        if (chordLen < 1.25 * Math.sqrt(2.0) * 0.5) {
            return null;
        }
        double chordDotTravel = Math.abs(chord.normalize().dot(travel));
        if (Math.abs(chordDotTravel - INV_SQRT2) > 0.28) {
            return null;
        }
        double radius = chordLen / Math.sqrt(2.0);
        if (radius < 1.25) {
            return null;
        }
        Vec3 dirAB = chord.scale(1.0 / radius).subtract(travel);
        if (dirAB.lengthSqr() < 1.0E-8) {
            return null;
        }
        if (Math.abs(dirAB.length() - 1.0) > 0.12) {
            return null;
        }
        if (Math.abs((dirAB = dirAB.normalize()).dot(travel)) > 0.08) {
            return null;
        }
        Vec3 center = cM.subtract(travel.scale(radius));
        Vec3 expectedA = center.subtract(dirAB.scale(radius));
        double fitTol = Math.max(0.75, radius * 0.1);
        if (cA.distanceToSqr(expectedA) > fitTol * fitTol) {
            return null;
        }
        Vec3 expectedB = center.add(dirAB.scale(radius));
        @Nullable BlockPos cornerB = CoasterHalfCirclePlacement.findParallelFarCorner(level, expectedB, travel, fitTol, corner, mid, outerA);
        if (cornerB == null) {
            return null;
        }
        BlockEntity blockEntity2 = level.getBlockEntity(cornerB);
        if (!(blockEntity2 instanceof CoasterAnchorpointBlockEntity) || (beCornerB = (CoasterAnchorpointBlockEntity)blockEntity2).legCount() != 1) {
            return null;
        }
        BlockPos outerB = CoasterHalfCirclePlacement.solePeer(beCornerB);
        if (outerB == null || outerB.equals((Object)corner) || outerB.equals((Object)mid) || outerB.equals((Object)outerA) || outerB.equals((Object)cornerB)) {
            return null;
        }
        if (CoasterHalfCirclePlacement.existingPrimaryBetween(level, outerA, corner) == null || CoasterHalfCirclePlacement.existingPrimaryBetween(level, cornerB, outerB) == null) {
            return null;
        }
        if (CoasterHalfCirclePlacement.existingPrimaryBetween(level, mid, cornerB) != null) {
            return null;
        }
        return new Roles(outerA.immutable(), corner.immutable(), mid.immutable(), cornerB.immutable(), outerB.immutable());
    }

    @Nullable
    private static BlockPos findParallelFarCorner(Level level, Vec3 expectedCorner, Vec3 travel, double tol, BlockPos excludeCorner, BlockPos excludeMid, BlockPos excludeOuter) {
        Vec3 t;
        BlockPos outer;
        CoasterAnchorpointBlockEntity be;
        double d;
        double tolSqr = tol * tol;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
        for (CoasterPathNode node : graph.nodes()) {
            BlockEntity blockEntity;
            BlockPos p = node.pos();
            if (p.equals((Object)excludeCorner) || p.equals((Object)excludeMid) || p.equals((Object)excludeOuter) || (d = CoasterAnchorpointBlockEntity.worldCenter(level, p).distanceToSqr(expectedCorner)) > tolSqr || d >= bestDist || !((blockEntity = level.getBlockEntity(p)) instanceof CoasterAnchorpointBlockEntity) || (be = (CoasterAnchorpointBlockEntity)blockEntity).legCount() != 1 || (outer = CoasterHalfCirclePlacement.solePeer(be)) == null || outer.equals((Object)excludeCorner) || outer.equals((Object)excludeMid) || outer.equals((Object)excludeOuter) || outer.equals((Object)p) || (t = CoasterAnchorpointBlockEntity.worldCenter(level, p).subtract(CoasterAnchorpointBlockEntity.worldCenter(level, outer))).lengthSqr() < 1.0E-8 || t.normalize().dot(travel) < 0.985) continue;
            best = p.immutable();
            bestDist = d;
        }
        if (best != null) {
            return best;
        }
        BlockPos guess = BlockPos.containing((Position)expectedCorner);
        for (BlockPos p : BlockPos.betweenClosed((BlockPos)guess.offset(-4, -4, -4), (BlockPos)guess.offset(4, 4, 4))) {
            if (p.equals((Object)excludeCorner) || p.equals((Object)excludeMid) || p.equals((Object)excludeOuter) || (d = CoasterAnchorpointBlockEntity.worldCenter(level, p).distanceToSqr(expectedCorner)) > tolSqr || d >= bestDist || !((outer = level.getBlockEntity(p)) instanceof CoasterAnchorpointBlockEntity) || (be = (CoasterAnchorpointBlockEntity)outer).legCount() != 1 || (outer = CoasterHalfCirclePlacement.solePeer(be)) == null || outer.equals((Object)excludeCorner) || outer.equals((Object)excludeMid) || outer.equals((Object)excludeOuter) || outer.equals((Object)p) || (t = CoasterAnchorpointBlockEntity.worldCenter(level, p).subtract(CoasterAnchorpointBlockEntity.worldCenter(level, outer))).lengthSqr() < 1.0E-8 || t.normalize().dot(travel) < 0.985) continue;
            best = p.immutable();
            bestDist = d;
        }
        return best;
    }

    @Nullable
    private static Roles detectPlacingNearQuarter(Level level, BlockPos cornerA, BlockPos mid) {
        CoasterAnchorpointBlockEntity beCornerB;
        CoasterAnchorpointBlockEntity beCornerA;
        BlockEntity blockEntity;
        block10: {
            block9: {
                blockEntity = level.getBlockEntity(cornerA);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block9;
                beCornerA = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(mid);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block10;
            }
            return null;
        }
        CoasterAnchorpointBlockEntity beMid = (CoasterAnchorpointBlockEntity)blockEntity;
        if (beCornerA.legCount() != 1 || beMid.legCount() != 1) {
            return null;
        }
        BlockPos outerA = CoasterHalfCirclePlacement.solePeer(beCornerA);
        BlockPos cornerB = CoasterHalfCirclePlacement.solePeer(beMid);
        if (outerA == null || cornerB == null || outerA.equals((Object)mid) || cornerB.equals((Object)cornerA) || outerA.equals((Object)cornerB)) {
            return null;
        }
        BlockEntity blockEntity2 = level.getBlockEntity(cornerB);
        if (!(blockEntity2 instanceof CoasterAnchorpointBlockEntity) || (beCornerB = (CoasterAnchorpointBlockEntity)blockEntity2).legCount() < 2) {
            return null;
        }
        BlockPos outerB = CoasterHalfCirclePlacement.otherPeer(beCornerB, mid);
        if (outerB == null || outerB.equals((Object)cornerA) || outerB.equals((Object)outerA)) {
            return null;
        }
        if (!(level.getBlockEntity(outerB) instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        if (CoasterHalfCirclePlacement.existingPrimaryBetween(level, outerA, cornerA) == null || CoasterHalfCirclePlacement.existingPrimaryBetween(level, mid, cornerB) == null || CoasterHalfCirclePlacement.existingPrimaryBetween(level, cornerB, outerB) == null) {
            return null;
        }
        return new Roles(outerA.immutable(), cornerA.immutable(), mid.immutable(), cornerB.immutable(), outerB.immutable());
    }

    @Nullable
    private static Roles detectPlacingFarQuarter(Level level, BlockPos mid, BlockPos cornerB) {
        CoasterAnchorpointBlockEntity beCornerA;
        CoasterAnchorpointBlockEntity beMid;
        BlockEntity blockEntity;
        block10: {
            block9: {
                blockEntity = level.getBlockEntity(mid);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block9;
                beMid = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(cornerB);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block10;
            }
            return null;
        }
        CoasterAnchorpointBlockEntity beCornerB = (CoasterAnchorpointBlockEntity)blockEntity;
        if (beMid.legCount() != 1 || beCornerB.legCount() != 1) {
            return null;
        }
        BlockPos cornerA = CoasterHalfCirclePlacement.solePeer(beMid);
        BlockPos outerB = CoasterHalfCirclePlacement.solePeer(beCornerB);
        if (cornerA == null || outerB == null || cornerA.equals((Object)cornerB) || outerB.equals((Object)mid) || cornerA.equals((Object)outerB)) {
            return null;
        }
        BlockEntity blockEntity2 = level.getBlockEntity(cornerA);
        if (!(blockEntity2 instanceof CoasterAnchorpointBlockEntity) || (beCornerA = (CoasterAnchorpointBlockEntity)blockEntity2).legCount() < 2) {
            return null;
        }
        BlockPos outerA = CoasterHalfCirclePlacement.otherPeer(beCornerA, mid);
        if (outerA == null || outerA.equals((Object)cornerB) || outerA.equals((Object)outerB) || outerA.equals((Object)mid)) {
            return null;
        }
        if (!(level.getBlockEntity(outerA) instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        if (CoasterHalfCirclePlacement.existingPrimaryBetween(level, outerA, cornerA) == null || CoasterHalfCirclePlacement.existingPrimaryBetween(level, cornerA, mid) == null || CoasterHalfCirclePlacement.existingPrimaryBetween(level, cornerB, outerB) == null) {
            return null;
        }
        return new Roles(outerA.immutable(), cornerA.immutable(), mid.immutable(), cornerB.immutable(), outerB.immutable());
    }

    @Nullable
    private static Roles detectPlacingStraight(Level level, BlockPos outer, BlockPos corner) {
        CoasterAnchorpointBlockEntity beCornerB;
        CoasterAnchorpointBlockEntity beMid;
        BlockPos outerPeer;
        CoasterAnchorpointBlockEntity beOuter;
        BlockEntity blockEntity;
        block14: {
            block13: {
                blockEntity = level.getBlockEntity(outer);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block13;
                beOuter = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(corner);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block14;
            }
            return null;
        }
        CoasterAnchorpointBlockEntity beCorner = (CoasterAnchorpointBlockEntity)blockEntity;
        if (beOuter.legCount() > 1) {
            return null;
        }
        if (beOuter.legCount() == 1 && ((outerPeer = CoasterHalfCirclePlacement.solePeer(beOuter)) == null || outerPeer.equals((Object)corner))) {
            return null;
        }
        if (beCorner.legCount() != 1) {
            return null;
        }
        BlockPos mid = CoasterHalfCirclePlacement.solePeer(beCorner);
        if (mid == null || mid.equals((Object)outer)) {
            return null;
        }
        BlockEntity blockEntity2 = level.getBlockEntity(mid);
        if (!(blockEntity2 instanceof CoasterAnchorpointBlockEntity) || (beMid = (CoasterAnchorpointBlockEntity)blockEntity2).legCount() != 2) {
            return null;
        }
        BlockPos cornerB = CoasterHalfCirclePlacement.otherPeer(beMid, corner);
        if (cornerB == null || cornerB.equals((Object)outer)) {
            return null;
        }
        BlockEntity blockEntity3 = level.getBlockEntity(cornerB);
        if (!(blockEntity3 instanceof CoasterAnchorpointBlockEntity) || (beCornerB = (CoasterAnchorpointBlockEntity)blockEntity3).legCount() < 2) {
            return null;
        }
        BlockPos outerB = CoasterHalfCirclePlacement.otherPeer(beCornerB, mid);
        if (outerB == null || outerB.equals((Object)outer) || outerB.equals((Object)corner) || outerB.equals((Object)mid)) {
            return null;
        }
        if (!(level.getBlockEntity(outerB) instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        if (CoasterHalfCirclePlacement.existingPrimaryBetween(level, corner, mid) == null || CoasterHalfCirclePlacement.existingPrimaryBetween(level, mid, cornerB) == null || CoasterHalfCirclePlacement.existingPrimaryBetween(level, cornerB, outerB) == null) {
            return null;
        }
        return new Roles(outer.immutable(), corner.immutable(), mid.immutable(), cornerB.immutable(), outerB.immutable());
    }

    @Nullable
    private static HalfSolve solveHalf(Level level, Roles roles) {
        Vec3 cOA = CoasterAnchorpointBlockEntity.worldCenter(level, roles.outerA());
        Vec3 cA = CoasterAnchorpointBlockEntity.worldCenter(level, roles.cornerA());
        Vec3 cM = CoasterAnchorpointBlockEntity.worldCenter(level, roles.mid());
        Vec3 cB = CoasterAnchorpointBlockEntity.worldCenter(level, roles.cornerB());
        Vec3 cOB = CoasterAnchorpointBlockEntity.worldCenter(level, roles.outerB());
        Vec3 travelA = cA.subtract(cOA);
        Vec3 travelB = cB.subtract(cOB);
        if (travelA.lengthSqr() < 1.0E-8 || travelB.lengthSqr() < 1.0E-8) {
            return null;
        }
        if ((travelA = travelA.normalize()).dot(travelB = travelB.normalize()) < 0.985) {
            return null;
        }
        Vec3 diameter = cB.subtract(cA);
        double dist = diameter.length();
        if (dist < 1.25) {
            return null;
        }
        double radius = dist / 2.0;
        if (radius < 1.25) {
            return null;
        }
        Vec3 dirAB = diameter.scale(1.0 / dist);
        if (Math.abs(travelA.dot(dirAB)) > 0.08) {
            return null;
        }
        Vec3 center = cA.add(cB).scale(0.5);
        Vec3 expectedMid = center.add(travelA.scale(radius));
        double midTol = Math.max(1.0, radius * 0.15);
        if (cM.distanceToSqr(expectedMid) > midTol * midTol) {
            return null;
        }
        if (Math.abs(cM.subtract(center).length() - radius) > midTol) {
            return null;
        }
        Vec3 planeN = dirAB.cross(travelA);
        if (planeN.lengthSqr() < 1.0E-12) {
            return null;
        }
        planeN = planeN.normalize();
        List<BlockPos> involved = List.of(roles.outerA(), roles.cornerA(), roles.mid(), roles.cornerB(), roles.outerB());
        @Nullable Orientation orientation = CoasterHalfCirclePlacement.classifyOrientation(level, involved, planeN, center);
        if (orientation == null) {
            return null;
        }
        if (orientation.radial() && !CoasterHalfCirclePlacement.allAnchorsHaveDefaultLift(level, involved)) {
            return null;
        }
        if (orientation.radial()) {
            return new HalfSolve(orientation, radius, center, planeN, travelA, dirAB, cOA, cA, cM, cB, cOB, true);
        }
        Vec3 bOA = CoasterAnchorpointBlockEntity.worldCenterBase(level, roles.outerA());
        Vec3 bA = CoasterAnchorpointBlockEntity.worldCenterBase(level, roles.cornerA());
        Vec3 bM = CoasterAnchorpointBlockEntity.worldCenterBase(level, roles.mid());
        Vec3 bB = CoasterAnchorpointBlockEntity.worldCenterBase(level, roles.cornerB());
        Vec3 bOB = CoasterAnchorpointBlockEntity.worldCenterBase(level, roles.outerB());
        Vec3 tA = bA.subtract(bOA);
        Vec3 tB = bB.subtract(bOB);
        if (tA.lengthSqr() < 1.0E-8 || tB.lengthSqr() < 1.0E-8) {
            return null;
        }
        tA = tA.normalize();
        tB = tB.normalize();
        if (tA.dot(travelA) < 0.0) {
            tA = tA.scale(-1.0);
        }
        if (tB.dot(travelB) < 0.0) {
            tB = tB.scale(-1.0);
        }
        if (tA.dot(tB) < 0.985) {
            return null;
        }
        Vec3 baseDiam = bB.subtract(bA);
        double baseDist = baseDiam.length();
        double baseR = baseDist / 2.0;
        if (baseR < 1.25) {
            return null;
        }
        Vec3 baseDir = baseDiam.scale(1.0 / baseDist);
        if (Math.abs(tA.dot(baseDir)) > 0.08) {
            return null;
        }
        Vec3 baseCenter = bA.add(bB).scale(0.5);
        Vec3 baseExpectedMid = baseCenter.add(tA.scale(baseR));
        double baseTol = Math.max(1.0, baseR * 0.15);
        if (bM.distanceToSqr(baseExpectedMid) > baseTol * baseTol) {
            return null;
        }
        Vec3 baseN = baseDir.cross(tA);
        if (baseN.lengthSqr() < 1.0E-12) {
            return null;
        }
        if ((baseN = baseN.normalize()).dot(orientation.planeNormal()) < 0.0) {
            baseN = baseN.scale(-1.0);
        }
        return new HalfSolve(orientation, baseR, baseCenter, baseN, tA, baseDir, bOA, bA, bM, bB, bOB, false);
    }

    @Nullable
    private static Orientation classifyOrientation(Level level, List<BlockPos> anchors, Vec3 planeN, Vec3 circleCenter) {
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
            Vec3 toward = circleCenter.subtract(CoasterAnchorpointBlockEntity.worldCenterBase(level, pos));
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

    private static BezierConnection buildQuarter(BlockPos first, BlockPos second, Vec3 startFirst, Vec3 startSecond, Vec3 travelFirst, Vec3 travelSecond, HalfSolve solve, TrackMaterial material, boolean girder) {
        Vec3 axisFirst = travelFirst;
        Vec3 axisSecond = travelSecond.scale(-1.0);
        Vec3 normalFirst = CoasterHalfCirclePlacement.faceNormal(solve.orientation(), startFirst, solve.center(), axisFirst, solve.planeN());
        Vec3 normalSecond = CoasterHalfCirclePlacement.faceNormal(solve.orientation(), startSecond, solve.center(), axisSecond, solve.planeN());
        BezierConnection bc = CoasterHalfCirclePlacement.primaryCurve(first, second, startFirst, startSecond, axisFirst, axisSecond, normalFirst, normalSecond, material, girder);
        double handle = Math.max(1.0E-4, solve.radius() * KAPPA_QUARTER);
        CoasterPeerCurveHandleLengths.putCanonical(bc, handle, handle);
        return bc;
    }

    @Nullable
    private static BezierConnection buildStraightLeg(BlockPos outer, BlockPos corner, Vec3 startOuter, Vec3 startCorner, TrackMaterial material, boolean girder, HalfSolve solve) {
        Vec3 chord = startCorner.subtract(startOuter);
        if (chord.lengthSqr() < 1.0E-8) {
            return null;
        }
        Vec3 dir = chord.normalize();
        if (dir.dot(solve.travel()) < 0.0) {
            dir = dir.scale(-1.0);
        }
        Vec3 axisOuter = dir;
        Vec3 axisCorner = dir.scale(-1.0);
        Vec3 normalOuter = CoasterHalfCirclePlacement.faceNormal(solve.orientation(), startOuter, solve.center(), axisOuter, solve.planeN());
        Vec3 normalCorner = CoasterHalfCirclePlacement.faceNormal(solve.orientation(), startCorner, solve.center(), axisCorner, solve.planeN());
        BezierConnection bc = CoasterHalfCirclePlacement.primaryCurve(outer, corner, startOuter, startCorner, axisOuter, axisCorner, normalOuter, normalCorner, material, girder);
        double h = Math.max(1.0E-4, startOuter.distanceTo(startCorner) / 3.0);
        CoasterPeerCurveHandleLengths.putCanonical(bc, h, h);
        return bc;
    }

    private static Vec3 faceNormal(Orientation orientation, Vec3 start, Vec3 circleCenter, Vec3 axis, Vec3 planeN) {
        if (orientation.radial()) {
            Vec3 toward = circleCenter.subtract(start);
            toward = (toward = toward.subtract(planeN.scale(toward.dot(planeN)))).lengthSqr() < 1.0E-12 ? (orientation.towardCenter() ? planeN : planeN.scale(-1.0)) : toward.normalize();
            Vec3 face = orientation.towardCenter() ? toward : toward.scale(-1.0);
            return CoasterHalfCirclePlacement.orthonormalToAxis(face, axis);
        }
        return CoasterHalfCirclePlacement.orthonormalToAxis(orientation.parallelFaceUp(), axis);
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
            @Nullable BezierConnection existing = CoasterHalfCirclePlacement.existingPrimaryBetween(level, outer, peer);
            if (existing == null) {
                return false;
            }
            BezierConnection before = existing;
            BezierConnection adjusted = CoasterTrackPlacement.curveWithAxisAtEndpointPreservingPeerForJoin(level, existing, outer, intoBeyond);
            CoasterPeerCurveHandleLengths.carryLengthsToReplacement(before, adjusted);
            if (!CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(adjusted)) {
                return false;
            }
            Vec3 intoAdj = CoasterBezierHandleEdit.tangentIntoFromAnchor(adjusted, outer);
            if (intoAdj == null || intoApproach.normalize().dot(intoAdj.normalize()) > -0.985) {
                return false;
            }
            neighbors.add(adjusted);
        }
        return true;
    }

    private static boolean isEdge(BezierConnection bc, BlockPos a, BlockPos b) {
        BlockPos lo = (BlockPos)bc.bePositions.getFirst();
        BlockPos hi = (BlockPos)bc.bePositions.getSecond();
        return lo.equals((Object)a) && hi.equals((Object)b) || lo.equals((Object)b) && hi.equals((Object)a);
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
    private static BlockPos solePeer(CoasterAnchorpointBlockEntity be) {
        if (be.legCount() != 1) {
            return null;
        }
        return be.getAnchorPeerCurvesView().keySet().iterator().next().immutable();
    }

    @Nullable
    private static BlockPos otherPeer(CoasterAnchorpointBlockEntity be, BlockPos exclude) {
        BlockPos found = null;
        for (BlockPos peer : be.getAnchorPeerCurvesView().keySet()) {
            if (peer.equals((Object)exclude)) continue;
            if (found != null) {
                return null;
            }
            found = peer.immutable();
        }
        return found;
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
        Vec3 a = axis.lengthSqr() < 1.0E-12 ? axis : axis.normalize();
        Vec3 n = normal.subtract(a.scale(normal.dot(a)));
        if (n.lengthSqr() < 1.0E-12) {
            return a;
        }
        return n.normalize();
    }

    private record Roles(BlockPos outerA, BlockPos cornerA, BlockPos mid, BlockPos cornerB, BlockPos outerB) {
    }

    private record HalfSolve(Orientation orientation, double radius, Vec3 center, Vec3 planeN, Vec3 travel, Vec3 dirAB, Vec3 startOuterA, Vec3 startCornerA, Vec3 startMid, Vec3 startCornerB, Vec3 startOuterB, boolean useLiftedCenters) {
    }

    private record Orientation(boolean radial, boolean towardCenter, Vec3 planeNormal, Vec3 parallelFaceUp) {
    }
}

