package dev.silvergold.simulatedcoasters.track.graph;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class CoasterPathAssemblyGraphDeferral {
   private static final Map<ResourceKey<Level>, CoasterPathAssemblyGraphDeferral.PendingAssembly> PENDING = new ConcurrentHashMap<>();

   private CoasterPathAssemblyGraphDeferral() {
   }

   public static void scheduleSubLevelBlockMove(ServerLevel level, BlockPos oldWorldPos, BlockPos newPos) {
      CoasterPathAssemblyGraphDeferral.PendingAssembly p = PENDING.computeIfAbsent(
         level.dimension(), dim -> new CoasterPathAssemblyGraphDeferral.PendingAssembly()
      );
      if (oldWorldPos != null) {
         p.vacatedWorldCells.add(oldWorldPos.immutable());
      }

      if (newPos != null) {
         p.plotDestinationCells.add(newPos.immutable());
      }
   }

   public static void flushPending(MinecraftServer server) {
      if (!PENDING.isEmpty()) {
         for (ServerLevel sl : server.getAllLevels()) {
            ResourceKey<Level> dim = sl.dimension();
            CoasterPathAssemblyGraphDeferral.PendingAssembly p = PENDING.get(dim);
            if (p != null && (!p.vacatedWorldCells.isEmpty() || !p.plotDestinationCells.isEmpty())) {
               try {
                  CoasterTrackPropagator.afterSubLevelBulkMove(sl, p.vacatedWorldCells, p.plotDestinationCells);
               } finally {
                  p.vacatedWorldCells.clear();
                  p.plotDestinationCells.clear();
               }
            }
         }
      }
   }

   public static void clearDimension(ResourceKey<Level> dim) {
      PENDING.remove(dim);
   }

   private static final class PendingAssembly {
      private final Set<BlockPos> vacatedWorldCells = ConcurrentHashMap.newKeySet();
      private final Set<BlockPos> plotDestinationCells = ConcurrentHashMap.newKeySet();
   }
}
