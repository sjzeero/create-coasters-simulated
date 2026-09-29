/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathNode;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathSavedData;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathSync;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTangentOrientation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CoasterTrackPropagator {
    private static final int MAX_COMPONENT = 16384;
    private static int batchDepth;
    private static final Set<BlockPos> batchSeeds;
    private static final Set<BlockPos> batchBlockSync;
    private static final Set<BlockPos> batchVisualStateSync;
    private static final List<BezierConnection> batchFakeTrackRemoves;
    private static final List<BezierConnection> batchFakeTrackAdds;

    private CoasterTrackPropagator() {
    }

    public static boolean isBatching() {
        return batchDepth > 0;
    }

    public static boolean scheduleBlockSyncIfBatching(BlockPos pos) {
        if (batchDepth <= 0 || pos == null) {
            return false;
        }
        batchBlockSync.add(pos.immutable());
        return true;
    }

    public static boolean scheduleVisualBlockStateSync(BlockPos pos) {
        if (batchDepth <= 0 || pos == null) {
            return false;
        }
        batchVisualStateSync.add(pos.immutable());
        return true;
    }

    public static void manageFakeTracks(ServerLevel level, BezierConnection bc, boolean remove) {
        if (!bc.isPrimary()) {
            return;
        }
        if (batchDepth > 0) {
            if (remove) {
                batchFakeTrackRemoves.add(bc);
            } else {
                batchFakeTrackAdds.add(bc);
            }
            return;
        }
        AnchorPeerFakeTracks.manageFakeTracksAlong(level, bc, remove);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void runBatchUpdate(ServerLevel level, Runnable work) {
        ++batchDepth;
        try {
            work.run();
        }
        finally {
            if (--batchDepth == 0) {
                try {
                    if (!batchSeeds.isEmpty()) {
                        LinkedHashSet<BlockPos> seeds = new LinkedHashSet<BlockPos>(batchSeeds);
                        batchSeeds.clear();
                        CoasterTrackPropagator.updateRegion(level, seeds);
                    }
                }
                finally {
                    CoasterTrackPropagator.flushBatchDeferredWork(level);
                }
            }
        }
    }

    private static void flushBatchDeferredWork(ServerLevel level) {
        CoasterAnchorpointBlockEntity be;
        BlockEntity blockEntity;
        for (BezierConnection bc : batchFakeTrackRemoves) {
            AnchorPeerFakeTracks.manageFakeTracksAlong(level, bc, true);
        }
        batchFakeTrackRemoves.clear();
        for (BezierConnection bc : batchFakeTrackAdds) {
            AnchorPeerFakeTracks.manageFakeTracksAlong(level, bc, false);
        }
        batchFakeTrackAdds.clear();
        for (BlockPos p : batchVisualStateSync) {
            blockEntity = level.getBlockEntity(p);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) continue;
            be = (CoasterAnchorpointBlockEntity)blockEntity;
            be.syncAnchorVisualBlockStateDeferred();
        }
        batchVisualStateSync.clear();
        for (BlockPos p : batchBlockSync) {
            blockEntity = level.getBlockEntity(p);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) continue;
            be = (CoasterAnchorpointBlockEntity)blockEntity;
            be.notifyUpdate();
        }
        batchBlockSync.clear();
    }

    public static void touch(ServerLevel level, Collection<BlockPos> positions) {
        Set<BlockPos> seeds = CoasterTrackPropagator.collectAnchorSeeds(level, positions);
        if (seeds.isEmpty()) {
            return;
        }
        if (batchDepth > 0) {
            batchSeeds.addAll(seeds);
            return;
        }
        CoasterTrackPropagator.updateRegion(level, seeds);
    }

    public static void afterSubLevelBulkMove(ServerLevel level, Set<BlockPos> vacatedWorldCells, Set<BlockPos> placedCells) {
        if (vacatedWorldCells.isEmpty() && placedCells.isEmpty()) {
            return;
        }
        HashSet<BlockPos> seeds = new HashSet<BlockPos>();
        for (BlockPos p : placedCells) {
            if (p == null || !(level.getBlockState(p).getBlock() instanceof CoasterAnchorpointBlock)) continue;
            seeds.add(p.immutable());
        }
        for (BlockPos old : vacatedWorldCells) {
            if (old == null) continue;
            BlockPos immutableOld = old.immutable();
            BlockPos.betweenClosedStream((BlockPos)immutableOld.offset(-1, -1, -1), (BlockPos)immutableOld.offset(1, 1, 1)).forEach(n -> {
                if (n.equals((Object)immutableOld)) {
                    return;
                }
                if (level.getBlockState(n).getBlock() instanceof CoasterAnchorpointBlock) {
                    seeds.add(n.immutable());
                }
            });
        }
        HashSet<BlockPos> region = new HashSet<BlockPos>();
        for (BlockPos v : vacatedWorldCells) {
            if (v == null) continue;
            region.add(v.immutable());
        }
        region.addAll(CoasterTrackPropagator.unionComponents(level, seeds));
        if (region.isEmpty()) {
            return;
        }
        CoasterTrackPropagator.updateRegionWithFixedRegion(level, region);
    }

    private static void updateRegionWithFixedRegion(ServerLevel level, Set<BlockPos> region) {
        CoasterPathSavedData data = CoasterPathSavedData.get(level.getServer());
        CoasterPathGraph graph = data.graphFor((ResourceKey<Level>)level.dimension());
        graph.removeEdgesTouching(region);
        graph.removeNodesIn(region);
        CoasterTrackPropagator.rebuildGraphInRegion(level, graph, region);
        graph.pruneOrphanNodes();
        data.setDirty();
        ChainLiftSpanPathCache.invalidateAll();
        CoasterPathSync.scheduleBroadcastDimension(level.getServer(), (ResourceKey<Level>)level.dimension());
    }

    private static void updateRegion(ServerLevel level, Collection<BlockPos> seeds) {
        Set<BlockPos> region = CoasterTrackPropagator.regionFromSeeds(level, seeds);
        if (region.isEmpty()) {
            return;
        }
        CoasterTrackPropagator.updateRegionWithFixedRegion(level, region);
    }

    private static Set<BlockPos> regionFromSeeds(ServerLevel level, Collection<BlockPos> seeds) {
        Set<BlockPos> region = CoasterTrackPropagator.collectAnchorSeeds(level, seeds);
        if (region.isEmpty()) {
            return region;
        }
        LinkedHashSet<BlockPos> expanded = new LinkedHashSet<BlockPos>(region);
        for (BlockPos p : region) {
            BlockEntity blockEntity = level.getBlockEntity(p);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity abe = (CoasterAnchorpointBlockEntity)blockEntity;
            for (BlockPos peer : abe.getAnchorPeerCurvesView().keySet()) {
                expanded.add(peer.immutable());
            }
        }
        return expanded;
    }

    private static Set<BlockPos> collectAnchorSeeds(ServerLevel level, Collection<BlockPos> positions) {
        LinkedHashSet<BlockPos> seeds = new LinkedHashSet<BlockPos>();
        for (BlockPos p : positions) {
            if (p == null || !(level.getBlockState(p).getBlock() instanceof CoasterAnchorpointBlock)) continue;
            seeds.add(p.immutable());
        }
        return seeds;
    }

    private static Set<BlockPos> unionComponents(ServerLevel level, Collection<BlockPos> seeds) {
        HashSet<BlockPos> out = new HashSet<BlockPos>();
        ArrayDeque<BlockPos> q = new ArrayDeque<BlockPos>();
        for (BlockPos s : seeds) {
            if (s == null || !(level.getBlockState(s).getBlock() instanceof CoasterAnchorpointBlock)) continue;
            q.add(s);
        }
        while (!q.isEmpty() && out.size() < 16384) {
            Object object;
            BlockPos p = (BlockPos)q.removeFirst();
            if (!out.add(p) || !((object = level.getBlockEntity(p)) instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity abe = (CoasterAnchorpointBlockEntity)((Object)object);
            object = abe.getAnchorPeerCurvesView().keySet().iterator();
            while (object.hasNext()) {
                BlockPos peer = (BlockPos)object.next();
                q.addLast(peer);
            }
        }
        return out;
    }

    private static void rebuildGraphInRegion(ServerLevel level, CoasterPathGraph graph, Set<BlockPos> region) {
        HashSet<String> seen = new HashSet<String>();
        for (BlockPos p : region) {
            BlockEntity blockEntity = level.getBlockEntity(p);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity abe = (CoasterAnchorpointBlockEntity)blockEntity;
            for (BezierConnection bc : abe.getAnchorPeerCurvesView().values()) {
                String k;
                if (bc == null) continue;
                BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
                BlockPos from = (BlockPos)primary.bePositions.getFirst();
                BlockPos to = (BlockPos)primary.bePositions.getSecond();
                if (!CoasterTrackPropagator.isCurveEndpoint((Level)level, from) || !CoasterTrackPropagator.isCurveEndpoint((Level)level, to) || !region.contains(from) && !region.contains(to) || !seen.add(k = CoasterTrackPropagator.edgeKey(from, to))) continue;
                graph.upsertNode(new CoasterPathNode(from, (Vec3)primary.normals.getFirst()));
                graph.upsertNode(new CoasterPathNode(to, (Vec3)primary.normals.getSecond()));
                graph.addEdge(CoasterPathEdge.curve(from, to, primary, CoasterTrackPropagator.endpointRollDeg(level, from), CoasterTrackPropagator.endpointRollDeg(level, to)));
            }
        }
        CoasterPathTangentOrientation.alignComponentsTouchingRegion((Level)level, graph, region);
    }

    private static boolean isCurveEndpoint(Level level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        return st.getBlock() instanceof CoasterAnchorpointBlock;
    }

    private static double endpointRollDeg(ServerLevel level, BlockPos anchorPos) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return 0.0;
        }
        CoasterAnchorpointBlockEntity abe = (CoasterAnchorpointBlockEntity)blockEntity;
        return Math.toDegrees(abe.getJointBankRadians());
    }

    private static String edgeKey(BlockPos a, BlockPos b) {
        long bl;
        long al = a.asLong();
        return al <= (bl = b.asLong()) ? al + "|" + bl : bl + "|" + al;
    }

    static {
        batchSeeds = new LinkedHashSet<BlockPos>();
        batchBlockSync = new LinkedHashSet<BlockPos>();
        batchVisualStateSync = new LinkedHashSet<BlockPos>();
        batchFakeTrackRemoves = new ArrayList<BezierConnection>();
        batchFakeTrackAdds = new ArrayList<BezierConnection>();
    }
}

