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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent.Load;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

@EventBusSubscriber(
   modid = "simulatedcoasters"
)
public final class CoasterPathSubLevelHooks {
   private static final Set<ResourceKey<Level>> SUB_LEVEL_OBSERVER_REGISTERED = ConcurrentHashMap.newKeySet();
   private static final SubLevelObserver COASTER_GRAPH_MAINTENANCE = new SubLevelObserver() {
      public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
         if (reason == SubLevelRemovalReason.REMOVED) {
            if (subLevel.getLevel() instanceof ServerLevel sl) {
               BoundingBox voxelBounds;
               try {
                  BoundingBox3ic bbox = subLevel.getPlot().getBoundingBox();
                  if (bbox == BoundingBox3i.EMPTY) {
                     return;
                  }

                  voxelBounds = bbox.toMojang();
               } catch (Exception var7) {
                  SimulatedCoasters.LOGGER.warn("Skipping coaster graph prune — could not resolve sub-level voxel bounds {}", subLevel.getUniqueId(), var7);
                  return;
               }

               CoasterPathSavedData data = CoasterPathSavedData.tryGet(sl);
               if (data != null) {
                  CoasterPathGraph graph = data.graphFor(sl.dimension());
                  CoasterPathGraphManager.invalidateServerTangentAlignment(sl.dimension());
                  graph.removeEdgesTouchingEndpointsIn(voxelBounds);
                  CoasterPathTangentOrientation.alignConnectedComponents(sl, graph);
                  CoasterPathGraphManager.markServerTangentAlignmentStaleResolved(sl.dimension());
                  data.setDirty();
                  CoasterPathSync.broadcastDimension(sl.getServer(), sl.dimension());
               }
            }
         }
      }
   };

   private CoasterPathSubLevelHooks() {
   }

   @SubscribeEvent
   public static void onLevelLoad(Load event) {
      if (event.getLevel() instanceof ServerLevel sl) {
         SubLevelContainer c = SubLevelContainer.getContainer(sl);
         if (c instanceof ServerSubLevelContainer) {
            ServerSubLevelContainer ssc = (ServerSubLevelContainer)c;
            if (SUB_LEVEL_OBSERVER_REGISTERED.add(sl.dimension())) {
               ssc.addObserver(COASTER_GRAPH_MAINTENANCE);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      if (event.getLevel() instanceof ServerLevel sl) {
         SubLevelContainer c = SubLevelContainer.getContainer(sl);
         if (c instanceof ServerSubLevelContainer) {
            SUB_LEVEL_OBSERVER_REGISTERED.remove(sl.dimension());
            CoasterPathAssemblyGraphDeferral.clearDimension(sl.dimension());
            CoasterTrackPlacementCommitDeferral.clearDimension(sl.dimension());
         }
      }
   }

   @SubscribeEvent
   public static void onChunkLoad(net.neoforged.neoforge.event.level.ChunkEvent.Load event) {
      if (event.getLevel() instanceof ServerLevel sl) {
         if (event.getChunk() instanceof LevelChunk chunk) {
            ChunkPos var7 = chunk.getPos();
            CoasterPathSavedData data = CoasterPathSavedData.tryGet(sl);
            if (data != null) {
               CoasterPathGraph graph = data.graphFor(sl.dimension());
               if (graph.removeGhostNodesMissingAnchors(sl, var7)) {
                  data.setDirty();
                  ChainLiftSpanPathCache.invalidateAll();
                  CoasterPathSync.scheduleBroadcastDimension(sl.getServer(), sl.dimension());
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void onServerTickPost(Post event) {
      CoasterTrackPlacementCommitDeferral.flushPending(event.getServer());
      CoasterPathAssemblyGraphDeferral.flushPending(event.getServer());
      CoasterPathSync.flushPendingBroadcasts(event.getServer());
   }
}
