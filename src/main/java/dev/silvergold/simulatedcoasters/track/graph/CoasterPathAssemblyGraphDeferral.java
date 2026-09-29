/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 */
package dev.silvergold.simulatedcoasters.track.graph;

import dev.silvergold.simulatedcoasters.track.graph.CoasterTrackPropagator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class CoasterPathAssemblyGraphDeferral {
    private static final Map<ResourceKey<Level>, PendingAssembly> PENDING = new ConcurrentHashMap<ResourceKey<Level>, PendingAssembly>();

    private CoasterPathAssemblyGraphDeferral() {
    }

    public static void scheduleSubLevelBlockMove(ServerLevel level, BlockPos oldWorldPos, BlockPos newPos) {
        PendingAssembly p = PENDING.computeIfAbsent((ResourceKey<Level>)level.dimension(), dim -> new PendingAssembly());
        if (oldWorldPos != null) {
            p.vacatedWorldCells.add(oldWorldPos.immutable());
        }
        if (newPos != null) {
            p.plotDestinationCells.add(newPos.immutable());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void flushPending(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        for (ServerLevel sl : server.getAllLevels()) {
            ResourceKey dim = sl.dimension();
            PendingAssembly p = PENDING.get(dim);
            if (p == null || p.vacatedWorldCells.isEmpty() && p.plotDestinationCells.isEmpty()) continue;
            try {
                CoasterTrackPropagator.afterSubLevelBulkMove(sl, p.vacatedWorldCells, p.plotDestinationCells);
            }
            finally {
                p.vacatedWorldCells.clear();
                p.plotDestinationCells.clear();
            }
        }
    }

    public static void clearDimension(ResourceKey<Level> dim) {
        PENDING.remove(dim);
    }

    private static final class PendingAssembly {
        private final Set<BlockPos> vacatedWorldCells = ConcurrentHashMap.newKeySet();
        private final Set<BlockPos> plotDestinationCells = ConcurrentHashMap.newKeySet();

        private PendingAssembly() {
        }
    }
}

