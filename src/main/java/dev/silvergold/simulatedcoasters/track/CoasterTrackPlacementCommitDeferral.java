package dev.silvergold.simulatedcoasters.track;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class CoasterTrackPlacementCommitDeferral {
   private static final Map<ResourceKey<Level>, List<Runnable>> PENDING = new ConcurrentHashMap<>();
   private static final Set<String> PENDING_EDGE_KEYS = ConcurrentHashMap.newKeySet();

   private CoasterTrackPlacementCommitDeferral() {
   }

   public static boolean isEdgeCommitPending(ServerLevel level, BlockPos anchorA, BlockPos anchorB) {
      return PENDING_EDGE_KEYS.contains(edgeKey(level, anchorA, anchorB));
   }

   public static void scheduleGeometryCommit(ServerLevel level, BlockPos anchorA, BlockPos anchorB, Runnable geometryCommit) {
      String key = edgeKey(level, anchorA, anchorB);
      if (PENDING_EDGE_KEYS.add(key)) {
         PENDING.computeIfAbsent(level.dimension(), dim -> new ArrayList<>()).add(() -> {
            try {
               geometryCommit.run();
            } finally {
               PENDING_EDGE_KEYS.remove(key);
            }
         });
      }
   }

   public static void flushPending(MinecraftServer server) {
      for (ServerLevel sl : server.getAllLevels()) {
         List<Runnable> work = PENDING.remove(sl.dimension());
         if (work != null && !work.isEmpty()) {
            for (Runnable r : work) {
               r.run();
            }
         }
      }
   }

   public static void clearDimension(ResourceKey<Level> dim) {
      PENDING.remove(dim);
   }

   private static String edgeKey(ServerLevel level, BlockPos a, BlockPos b) {
      long al = a.asLong();
      long bl = b.asLong();
      String edge = al <= bl ? al + "|" + bl : bl + "|" + al;
      return level.dimension().location() + "|" + edge;
   }
}
