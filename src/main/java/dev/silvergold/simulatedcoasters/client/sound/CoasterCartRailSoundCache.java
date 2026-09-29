package dev.silvergold.simulatedcoasters.client.sound;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientFrameCache;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientMembership;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTraceEndpoints;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartRailSoundCache {
   private static List<Vec3> openEnds = List.of();
   private static List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs = List.of();
   private static final int OPEN_ENDS_REFRESH_INTERVAL_TICKS = 10;
   private static int openEndsCachedAtTick = Integer.MIN_VALUE;
   private static int openEndsCachedEdgeCount = -1;
   private static int trainCacheTick = Integer.MIN_VALUE;
   private static final Set<UUID> SOUND_LEADER_IDS = new HashSet<>();
   private static final Map<UUID, BlockPos> BEARING_PLOT_BY_CART = new HashMap<>();
   private static final Map<UUID, SubLevel> CART_SUB_BY_ID = new HashMap<>();

   private CoasterCartRailSoundCache() {
   }

   public static void beginClientTick(ClientLevel level) {
      CoasterCartClientMembership.reconcile(level);
      CoasterCartClientFrameCache.beginClientTick(level);
      refreshTrainSoundCache(level);
      CoasterPathGraph graph = CoasterPathGraphManagerClient.get(level);
      List<CoasterPathEdge> edges = graph.edges();
      int edgeCount = edges.size();
      int tick = (int)level.getGameTime();
      if (edges.isEmpty()) {
         openEnds = List.of();
         openEndRefs = List.of();
         openEndsCachedEdgeCount = 0;
         openEndsCachedAtTick = tick;
      } else {
         if (edgeCount != openEndsCachedEdgeCount || tick - openEndsCachedAtTick >= 10) {
            openEndsCachedEdgeCount = edgeCount;
            openEndsCachedAtTick = tick;
            Set<Integer> indices = CoasterPathTraceEndpoints.allEdgeIndices(graph);
            openEndRefs = CoasterPathTraceEndpoints.pathOpenEndRefs(level, graph, indices);
            openEnds = CoasterPathTraceEndpoints.pathEndpointWorldPositions(level, graph, indices);
         }
      }
   }

   public static boolean isSoundLeader(UUID cartId) {
      return SOUND_LEADER_IDS.contains(cartId);
   }

   public static Map<UUID, BlockPos> bearingPlotByCart() {
      return BEARING_PLOT_BY_CART;
   }

   public static Set<UUID> soundLeaderIds() {
      return SOUND_LEADER_IDS;
   }

   @Nullable
   public static SubLevel cartSubLevel(UUID cartId) {
      return CART_SUB_BY_ID.get(cartId);
   }

   public static List<Vec3> openPathEnds() {
      return openEnds;
   }

   public static List<CoasterPathTraceEndpoints.OpenEndRef> openPathEndRefs() {
      return openEndRefs;
   }

   private static void refreshTrainSoundCache(ClientLevel level) {
      int tick = (int)level.getGameTime();
      if (tick != trainCacheTick) {
         trainCacheTick = tick;
         SOUND_LEADER_IDS.clear();
         BEARING_PLOT_BY_CART.clear();
         CART_SUB_BY_ID.clear();
         Set<UUID> assigned = new HashSet<>();
         CoasterCartClientMembership.forEachCart(level, sub -> {
            BlockPos bearing = CoasterCartClientMembership.representativeBearingPlotPos(sub);
            if (bearing != null) {
               UUID id = sub.getUniqueId();
               BEARING_PLOT_BY_CART.put(id, bearing);
               CART_SUB_BY_ID.put(id, sub);
               if (!assigned.contains(id)) {
                  Set<UUID> train = CoasterCartTrainLinkConstraint.getTrainLinkedCartIds(sub);
                  UUID leader = null;

                  for (UUID member : train) {
                     if (leader == null || member.compareTo(leader) < 0) {
                        leader = member;
                     }
                  }

                  if (leader == null) {
                     leader = id;
                  }

                  SOUND_LEADER_IDS.add(leader);
                  assigned.addAll(train);
               }
            }
         });
      }
   }
}
