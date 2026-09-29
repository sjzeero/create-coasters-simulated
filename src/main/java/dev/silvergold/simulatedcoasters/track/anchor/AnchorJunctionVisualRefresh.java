/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  dev.engine_room.flywheel.lib.visualization.VisualizationHelper
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class AnchorJunctionVisualRefresh {
    private static final Set<BlockPos> PENDING_SEEDS = new LinkedHashSet<BlockPos>();
    @Nullable
    private static Set<BlockPos> followUpCluster = null;

    private AnchorJunctionVisualRefresh() {
    }

    public static void scheduleClusterRefresh(Level level, BlockPos ... seedAnchors) {
        if (level == null || !level.isClientSide()) {
            return;
        }
        for (BlockPos p : seedAnchors) {
            if (p == null || p.equals((Object)BlockPos.ZERO)) continue;
            PENDING_SEEDS.add(p.immutable());
        }
    }

    @Deprecated
    public static void refreshAround(Level level, BlockPos ... seedAnchors) {
        AnchorJunctionVisualRefresh.scheduleClusterRefresh(level, seedAnchors);
    }

    public static void clientTickPost(ClientLevel level) {
        if (followUpCluster != null) {
            AnchorJunctionVisualRefresh.queueClusterUpdates(level, followUpCluster);
            followUpCluster = null;
        }
        if (PENDING_SEEDS.isEmpty()) {
            return;
        }
        LinkedHashSet<BlockPos> seeds = new LinkedHashSet<BlockPos>(PENDING_SEEDS);
        PENDING_SEEDS.clear();
        Set<BlockPos> cluster = AnchorJunctionVisualRefresh.expandOneHop((Level)level, seeds);
        AnchorJunctionVisualRefresh.queueClusterUpdates(level, cluster);
        if (!cluster.isEmpty()) {
            followUpCluster = cluster;
        }
    }

    private static void queueClusterUpdates(ClientLevel level, Set<BlockPos> anchorEndpoints) {
        for (BlockPos anchorPos : anchorEndpoints) {
            BlockEntity blockEntity = level.getBlockEntity(anchorPos);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
            VisualizationHelper.queueUpdate((BlockEntity)anchor);
        }
    }

    private static Set<BlockPos> expandOneHop(Level level, Set<BlockPos> seedAnchors) {
        LinkedHashSet<BlockPos> out = new LinkedHashSet<BlockPos>();
        for (BlockPos seed : seedAnchors) {
            if (seed == null || seed.equals((Object)BlockPos.ZERO)) continue;
            out.add(seed.immutable());
            BlockEntity blockEntity = level.getBlockEntity(seed);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
            for (BezierConnection bc : anchor.getAnchorPeerCurvesView().values()) {
                if (bc == null) continue;
                BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
                out.add(((BlockPos)p.bePositions.getFirst()).immutable());
                out.add(((BlockPos)p.bePositions.getSecond()).immutable());
            }
        }
        return out;
    }
}

