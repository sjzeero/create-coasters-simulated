/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelObserver
 *  dev.ryanhcode.sable.companion.math.BoundingBox3i
 *  dev.ryanhcode.sable.companion.math.BoundingBox3ic
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.chunk.ChunkAccess
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.minecraft.world.level.levelgen.structure.BoundingBox
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.level.ChunkEvent$Load
 *  net.neoforged.neoforge.event.level.LevelEvent$Load
 *  net.neoforged.neoforge.event.level.LevelEvent$Unload
 *  net.neoforged.neoforge.event.tick.ServerTickEvent$Post
 */
package dev.silvergold.simulatedcoasters.track.graph;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacementCommitDeferral;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathAssemblyGraphDeferral;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathSavedData;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathSync;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTangentOrientation;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid="simulatedcoasters")
public final class CoasterPathSubLevelHooks {
    private static final Set<ResourceKey<Level>> SUB_LEVEL_OBSERVER_REGISTERED = ConcurrentHashMap.newKeySet();
    private static final SubLevelObserver COASTER_GRAPH_MAINTENANCE = new SubLevelObserver(){

        public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
            BoundingBox voxelBounds;
            if (reason != SubLevelRemovalReason.REMOVED) {
                return;
            }
            Level level = subLevel.getLevel();
            if (!(level instanceof ServerLevel)) {
                return;
            }
            ServerLevel sl = (ServerLevel)level;
            try {
                BoundingBox3ic bbox = subLevel.getPlot().getBoundingBox();
                if (bbox == BoundingBox3i.EMPTY) {
                    return;
                }
                voxelBounds = bbox.toMojang();
            }
            catch (Exception ex) {
                SimulatedCoasters.LOGGER.warn("Skipping coaster graph prune \u2014 could not resolve sub-level voxel bounds {}", (Object)subLevel.getUniqueId(), (Object)ex);
                return;
            }
            CoasterPathSavedData data = CoasterPathSavedData.tryGet(sl);
            if (data == null) {
                return;
            }
            CoasterPathGraph graph = data.graphFor((ResourceKey<Level>)sl.dimension());
            CoasterPathGraphManager.invalidateServerTangentAlignment((ResourceKey<Level>)sl.dimension());
            graph.removeEdgesTouchingEndpointsIn(voxelBounds);
            CoasterPathTangentOrientation.alignConnectedComponents((Level)sl, graph);
            CoasterPathGraphManager.markServerTangentAlignmentStaleResolved((ResourceKey<Level>)sl.dimension());
            data.setDirty();
            CoasterPathSync.broadcastDimension(sl.getServer(), (ResourceKey<Level>)sl.dimension());
        }
    };

    private CoasterPathSubLevelHooks() {
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)levelAccessor;
        ServerSubLevelContainer c = SubLevelContainer.getContainer((ServerLevel)sl);
        if (!(c instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer ssc = c;
        if (!SUB_LEVEL_OBSERVER_REGISTERED.add((ResourceKey<Level>)sl.dimension())) {
            return;
        }
        ssc.addObserver(COASTER_GRAPH_MAINTENANCE);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)levelAccessor;
        ServerSubLevelContainer c = SubLevelContainer.getContainer((ServerLevel)sl);
        if (!(c instanceof ServerSubLevelContainer)) {
            return;
        }
        SUB_LEVEL_OBSERVER_REGISTERED.remove(sl.dimension());
        CoasterPathAssemblyGraphDeferral.clearDimension((ResourceKey<Level>)sl.dimension());
        CoasterTrackPlacementCommitDeferral.clearDimension((ResourceKey<Level>)sl.dimension());
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)levelAccessor;
        ChunkAccess chunkAccess = event.getChunk();
        if (!(chunkAccess instanceof LevelChunk)) {
            return;
        }
        LevelChunk chunk = (LevelChunk)chunkAccess;
        ChunkPos pos = chunk.getPos();
        CoasterPathSavedData data = CoasterPathSavedData.tryGet(sl);
        if (data == null) {
            return;
        }
        CoasterPathGraph graph = data.graphFor((ResourceKey<Level>)sl.dimension());
        if (!graph.removeGhostNodesMissingAnchors(sl, pos)) {
            return;
        }
        data.setDirty();
        ChainLiftSpanPathCache.invalidateAll();
        CoasterPathSync.scheduleBroadcastDimension(sl.getServer(), (ResourceKey<Level>)sl.dimension());
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        CoasterTrackPlacementCommitDeferral.flushPending(event.getServer());
        CoasterPathAssemblyGraphDeferral.flushPending(event.getServer());
        CoasterPathSync.flushPendingBroadcasts(event.getServer());
    }
}

