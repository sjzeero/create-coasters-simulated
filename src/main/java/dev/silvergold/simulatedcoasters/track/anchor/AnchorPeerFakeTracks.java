/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllBlocks
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.FakeTrackBlock
 *  com.simibubi.create.foundation.block.ProperWaterloggedBlock
 *  net.createmod.catnip.data.Pair
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.material.Fluid
 *  net.minecraft.world.level.material.FluidState
 *  net.minecraft.world.level.material.Fluids
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.FakeTrackBlock;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class AnchorPeerFakeTracks {
    private AnchorPeerFakeTracks() {
    }

    public static boolean rasterContains(BezierConnection bc, BlockPos worldPos) {
        BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
        BlockPos base = (BlockPos)primary.bePositions.getFirst();
        Map yLevels = primary.rasterise();
        for (Map.Entry entry : yLevels.entrySet()) {
            double yValue = (Double)entry.getValue();
            int floor = Mth.floor((double)yValue);
            BlockPos targetPos = new BlockPos(((Integer)((Pair)entry.getKey()).getFirst()).intValue(), floor, ((Integer)((Pair)entry.getKey()).getSecond()).intValue()).offset((Vec3i)base).above(1);
            if (!targetPos.equals((Object)worldPos)) continue;
            return true;
        }
        return false;
    }

    @Nullable
    public static BlockPos blockPosForCurveHit(BezierConnection bc, Vec3 worldHit) {
        BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
        Map yLevels = primary.rasterise();
        if (yLevels.isEmpty()) {
            return null;
        }
        BlockPos base = (BlockPos)primary.bePositions.getFirst();
        BlockPos bestPos = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Map.Entry entry : yLevels.entrySet()) {
            double yValue = (Double)entry.getValue();
            int floor = Mth.floor((double)yValue);
            BlockPos targetPos = new BlockPos(((Integer)((Pair)entry.getKey()).getFirst()).intValue(), floor, ((Integer)((Pair)entry.getKey()).getSecond()).intValue()).offset((Vec3i)base).above(1);
            AABB box = AnchorPeerFakeTracks.unitBlockBounds(targetPos);
            double d = AnchorPeerFakeTracks.distSqPointToAabb(worldHit, box);
            if (!(d < bestDistSq)) continue;
            bestDistSq = d;
            bestPos = targetPos;
        }
        return bestPos;
    }

    @Nullable
    public static BezierConnection resolvePrimaryCoasterCurveAtFakeCell(Level level, BlockPos fakeCell, Vec3 worldHit) {
        List<BezierConnection> curves = AnchorPeerFakeTracks.hostedPrimaryCoasterCurvesContainingCell(level, fakeCell);
        if (curves.isEmpty()) {
            return null;
        }
        if (curves.size() == 1) {
            return curves.get(0);
        }
        BezierConnection best = null;
        double bestD = Double.MAX_VALUE;
        for (BezierConnection bc : curves) {
            double d = AnchorPeerFakeTracks.distanceSqToCoasterCurveInteraction(bc, worldHit, level);
            if (!(d < bestD)) continue;
            bestD = d;
            best = bc;
        }
        return best;
    }

    public static double distanceSqToCoasterCurveInteraction(BezierConnection bc, Vec3 worldHit, @Nullable Level level) {
        BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
        int samples = Math.max(48, primary.getSegmentCount() * 4);
        double best = Double.MAX_VALUE;
        for (int i = 0; i <= samples; ++i) {
            double d;
            double t = (double)i / (double)samples;
            Vec3 p = primary.getPosition(t);
            if (level != null && primary.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
                p = p.add(CoasterBezierRailFrames.curveInteractionLiftOffset(primary, (float)t, level));
            }
            if (!((d = p.distanceToSqr(worldHit)) < best)) continue;
            best = d;
        }
        return Math.min(best, primary.getBounds().inflate(0.75).distanceToSqr(worldHit));
    }

    public static BlockPos nearestEndpointBlockForHit(Vec3 worldHit, BlockPos a, BlockPos b) {
        double db;
        double da = AnchorPeerFakeTracks.distSqPointToAabb(worldHit, AnchorPeerFakeTracks.unitBlockBounds(a));
        return da <= (db = AnchorPeerFakeTracks.distSqPointToAabb(worldHit, AnchorPeerFakeTracks.unitBlockBounds(b))) ? a : b;
    }

    public static Vec3 clampInteractionInsideCell(Vec3 rawWorld, BlockPos fakeTrackCell) {
        AABB b = AnchorPeerFakeTracks.unitBlockBounds(fakeTrackCell);
        double eps = 0.03125;
        double x = Mth.clamp((double)rawWorld.x, (double)(b.minX + eps), (double)(b.maxX - eps));
        double y = Mth.clamp((double)rawWorld.y, (double)(b.minY + eps), (double)(b.maxY - eps));
        double z = Mth.clamp((double)rawWorld.z, (double)(b.minZ + eps), (double)(b.maxZ - eps));
        return new Vec3(x, y, z);
    }

    private static AABB unitBlockBounds(BlockPos p) {
        return new AABB((double)p.getX(), (double)p.getY(), (double)p.getZ(), (double)p.getX() + 1.0, (double)p.getY() + 1.0, (double)p.getZ() + 1.0);
    }

    private static double distSqPointToAabb(Vec3 p, AABB aabb) {
        double x = Mth.clamp((double)p.x, (double)aabb.minX, (double)aabb.maxX);
        double y = Mth.clamp((double)p.y, (double)aabb.minY, (double)aabb.maxY);
        double z = Mth.clamp((double)p.z, (double)aabb.minZ, (double)aabb.maxZ);
        double dx = p.x - x;
        double dy = p.y - y;
        double dz = p.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public static boolean isManagedFakeTrack(Level level, BlockPos pos) {
        if (!AllBlocks.FAKE_TRACK.has(level.getBlockState(pos))) {
            return false;
        }
        return AnchorPeerFakeTracks.rasterScanHostedCurvesForCell(level, pos);
    }

    public static List<BezierConnection> hostedPrimaryCoasterCurvesContainingCell(Level level, BlockPos fakeCell) {
        ArrayList<BezierConnection> out = new ArrayList<BezierConnection>();
        HashSet<String> seenEdges = new HashSet<String>();
        int bx = fakeCell.getX() >> 4;
        int bz = fakeCell.getZ() >> 4;
        for (int cox = -2; cox <= 2; ++cox) {
            for (int coz = -2; coz <= 2; ++coz) {
                int cx = bx + cox;
                int cz = bz + coz;
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof CoasterAnchorpointBlockEntity)) continue;
                    CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)be;
                    for (BezierConnection raw : anchor.getAnchorPeerCurvesView().values()) {
                        BezierConnection curve;
                        if (raw == null) continue;
                        BezierConnection bezierConnection = curve = raw.isPrimary() ? raw : raw.secondary();
                        if (!curve.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id) || !AnchorPeerFakeTracks.rasterContains(curve, fakeCell) || !seenEdges.add(AnchorPeerFakeTracks.edgeKey(curve))) continue;
                        out.add(curve);
                    }
                }
            }
        }
        return out;
    }

    @Nullable
    public static BlockPos nearestCoasterAnchorPosForFakeTrackCell(Level level, BlockPos fakeCell, Vec3 referenceWorld) {
        List<BezierConnection> curves;
        BezierConnection primary = AnchorPeerFakeTracks.resolvePrimaryCoasterCurveAtFakeCell(level, fakeCell, referenceWorld);
        List<BezierConnection> list = curves = primary != null ? List.of(primary) : AnchorPeerFakeTracks.hostedPrimaryCoasterCurvesContainingCell(level, fakeCell);
        if (curves.isEmpty()) {
            return null;
        }
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        HashSet<Long> seenAnchors = new HashSet<Long>();
        for (BezierConnection bc : curves) {
            BlockPos a = (BlockPos)bc.bePositions.getFirst();
            BlockPos b = (BlockPos)bc.bePositions.getSecond();
            for (BlockPos ep : List.of(a, b)) {
                Vec3 c;
                double d;
                long key;
                if (!(level.getBlockEntity(ep) instanceof CoasterAnchorpointBlockEntity) || !seenAnchors.add(key = ep.asLong()) || !((d = referenceWorld.distanceToSqr(c = CoasterAnchorpointBlockEntity.worldCenter(level, ep))) < bestD)) continue;
                bestD = d;
                best = ep.immutable();
            }
        }
        return best;
    }

    private static String edgeKey(BezierConnection bc) {
        long b;
        long a = ((BlockPos)bc.bePositions.getFirst()).asLong();
        return a <= (b = ((BlockPos)bc.bePositions.getSecond()).asLong()) ? a + ":" + b : b + ":" + a;
    }

    public static boolean isManagedFakeTrackAt(ServerLevel level, BlockPos pos) {
        if (!AllBlocks.FAKE_TRACK.has(level.getBlockState(pos))) {
            return false;
        }
        return AnchorPeerFakeTracks.rasterScanHostedCurvesForCell((Level)level, pos);
    }

    private static boolean rasterScanHostedCurvesForCell(Level level, BlockPos pos) {
        int bx = pos.getX() >> 4;
        int bz = pos.getZ() >> 4;
        for (int cox = -2; cox <= 2; ++cox) {
            for (int coz = -2; coz <= 2; ++coz) {
                int cx = bx + cox;
                int cz = bz + coz;
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof CoasterAnchorpointBlockEntity)) continue;
                    CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)be;
                    for (BezierConnection raw : anchor.getAnchorPeerCurvesView().values()) {
                        BezierConnection curve;
                        if (raw == null || !AnchorPeerFakeTracks.rasterContains(curve = raw.isPrimary() ? raw : raw.secondary(), pos)) continue;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static void manageFakeTracksAlong(ServerLevel level, BezierConnection bc, boolean remove) {
        if (!bc.isPrimary()) {
            return;
        }
        Map yLevels = bc.rasterise();
        for (Map.Entry entry : yLevels.entrySet()) {
            double yValue = (Double)entry.getValue();
            int floor = Mth.floor((double)yValue);
            BlockPos targetPos = new BlockPos(((Integer)((Pair)entry.getKey()).getFirst()).intValue(), floor, ((Integer)((Pair)entry.getKey()).getSecond()).intValue()).offset((Vec3i)bc.bePositions.getFirst()).above(1);
            if (!level.isLoaded(targetPos)) continue;
            BlockState stateAtPos = level.getBlockState(targetPos);
            boolean present = AllBlocks.FAKE_TRACK.has(stateAtPos);
            if (remove) {
                if (!present) continue;
                level.removeBlock(targetPos, false);
                continue;
            }
            FluidState fluidState = stateAtPos.getFluidState();
            if (!fluidState.isEmpty() && !fluidState.isSourceOfType((Fluid)Fluids.WATER)) continue;
            if (!present && stateAtPos.canBeReplaced()) {
                level.setBlock(targetPos, ProperWaterloggedBlock.withWater((LevelAccessor)level, (BlockState)AllBlocks.FAKE_TRACK.getDefaultState(), (BlockPos)targetPos), 3);
            }
            FakeTrackBlock.keepAlive((LevelAccessor)level, (BlockPos)targetPos);
        }
    }

    static void keepAliveAlong(ServerLevel level, BezierConnection bc) {
        AnchorPeerFakeTracks.manageFakeTracksAlong(level, bc, false);
    }
}

