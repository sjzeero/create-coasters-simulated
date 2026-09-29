package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTraceEndpoints;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

final class CoasterCartTrackHitCache {
   private static final double FULL_SEARCH_MOVEMENT_BLOCKS = 0.35;
   private static final double FULL_SEARCH_MOVEMENT_DIST_SQ = 0.12249999999999998;
   private static final double ENDPOINT_T_MARGIN = 0.06;
   private static final double ENGAGED_T_REBUILD_EPS = 1.0E-5;
   private static final Map<ServerSubLevel, CoasterCartTrackHitCache.CartHitEntry> CART_HITS = Collections.synchronizedMap(new IdentityHashMap<>());
   private static final Map<ServerSubLevel, CoasterCartTrackHitCache.EngagedTickEntry> ENGAGED_TICK = Collections.synchronizedMap(new IdentityHashMap<>());
   private static final Map<ResourceKey<Level>, CoasterCartTrackHitCache.OpenEndTickEntry> OPEN_ENDS_BY_DIM = new HashMap<>();

   private CoasterCartTrackHitCache() {
   }

   static List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs(ServerLevel level, CoasterPathGraph graph) {
      if (graph.edges().isEmpty()) {
         return List.of();
      } else {
         long gameTime = level.getGameTime();
         int revision = graph.revision();
         ResourceKey<Level> dim = level.dimension();
         synchronized (OPEN_ENDS_BY_DIM) {
            CoasterCartTrackHitCache.OpenEndTickEntry cached = OPEN_ENDS_BY_DIM.get(dim);
            if (cached != null && cached.gameTime == gameTime && cached.graphRevision == revision) {
               return cached.refs;
            } else {
               List<CoasterPathTraceEndpoints.OpenEndRef> refs = CoasterPathTraceEndpoints.pathOpenEndRefs(
                  level, graph, CoasterPathTraceEndpoints.allEdgeIndices(graph)
               );
               OPEN_ENDS_BY_DIM.put(dim, new CoasterCartTrackHitCache.OpenEndTickEntry(gameTime, revision, refs));
               return refs;
            }
         }
      }
   }

   static CoasterCartTrackHitCache.EngagedTickFrame engagedTickFrame(
      ServerLevel level, CoasterPathGraph graph, ServerSubLevel cart, CoasterPathTrackFrame.GraphHit graphHit, boolean negateEdgeTangent, boolean subLevelTrack
   ) {
      long gameTime = level.getGameTime();
      int revision = graph.revision();
      CoasterCartTrackHitCache.EngagedTickEntry cached = ENGAGED_TICK.get(cart);
      if (cached != null && cached.gameTime == gameTime && cached.graphRevision == revision && cached.negateEdgeTangent == negateEdgeTangent) {
         if (!subLevelTrack) {
            return cached.frame;
         }

         if (sameEngagedHit(cached.frame.graphHit(), graphHit)) {
            return cached.frame;
         }
      }

      CoasterPathTrackFrame.TrackBasis basis = alignedTrackBasis(CoasterPathTrackFrame.trackBasisAtHit(level, graphHit), negateEdgeTangent);
      Vec3 trackUp = basis.up().lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : basis.up().normalize();
      Vec3 snapTarget = graphHit.point().add(trackUp.scale(0.3125));
      CoasterCartTrackHitCache.EngagedTickFrame frame = new CoasterCartTrackHitCache.EngagedTickFrame(
         graphHit, basis, new Vector3d(snapTarget.x, snapTarget.y, snapTarget.z)
      );
      ENGAGED_TICK.put(cart, new CoasterCartTrackHitCache.EngagedTickEntry(gameTime, revision, negateEdgeTangent, frame));
      return frame;
   }

   @Nullable
   static CoasterPathTrackFrame.GraphHit nearestGraphHit(ServerLevel level, CoasterPathGraph graph, ServerSubLevel cart, Vec3 bearingWorld, double maxDistSq) {
      return nearestGraphHit(level, graph, cart, bearingWorld, maxDistSq, new boolean[1]);
   }

   @Nullable
   static CoasterPathTrackFrame.GraphHit nearestGraphHit(
      ServerLevel level, CoasterPathGraph graph, ServerSubLevel cart, Vec3 bearingWorld, double maxDistSq, boolean[] outSubLevelTrack
   ) {
      if (graph.edges().isEmpty()) {
         return null;
      } else {
         long gameTime = level.getGameTime();
         int revision = graph.revision();
         CoasterCartTrackHitCache.EngagedTickEntry engaged = ENGAGED_TICK.get(cart);
         if (engaged != null && engaged.gameTime == gameTime && engaged.graphRevision == revision) {
            CoasterCartTrackHitCache.CartHitEntry cartEntry = CART_HITS.get(cart);
            boolean isSub = cartEntry != null && cartEntry.graphRevision() == revision && cartEntry.subLevelTrack();
            if (!isSub) {
               outSubLevelTrack[0] = false;
               return engaged.frame.graphHit();
            } else {
               CoasterPathTrackFrame.GraphHit refined = refineEngagedHit(level, graph, engaged.frame.graphHit(), bearingWorld, maxDistSq, cart);
               outSubLevelTrack[0] = true;
               if (refined != null) {
                  putCartHit(cart, refined, graph, gameTime, bearingWorld, true);
                  return refined;
               } else {
                  return engaged.frame.graphHit();
               }
            }
         } else {
            CoasterCartTrackHitCache.CartHitEntry cached = CART_HITS.get(cart);
            if (cached != null && cached.graphRevision == revision) {
               if (cached.gameTime == gameTime) {
                  CoasterPathTrackFrame.GraphHit refined = refineOnCachedEdge(level, graph, cached, bearingWorld, maxDistSq);
                  if (refined != null) {
                     outSubLevelTrack[0] = cached.subLevelTrack();
                     return refined;
                  }
               } else if (!cached.subLevelTrack() && bearingWorld.distanceToSqr(cached.bearingX(), cached.bearingY(), cached.bearingZ()) <= 0.12249999999999998
                  )
                {
                  CoasterPathTrackFrame.GraphHit refined = refineOnCachedEdge(level, graph, cached, bearingWorld, maxDistSq);
                  if (refined != null && !nearEdgeEndpoint(refined.t())) {
                     putCartHit(cart, refined, graph, gameTime, bearingWorld, false);
                     outSubLevelTrack[0] = false;
                     return refined;
                  }
               }
            }

            CoasterPathTrackFrame.GraphHit hit = CoasterPathTrackFrame.nearestGraphHit(level, graph, bearingWorld, maxDistSq);
            if (hit != null) {
               boolean isSub = CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge());
               putCartHit(cart, hit, graph, gameTime, bearingWorld, isSub);
               outSubLevelTrack[0] = isSub;
            } else {
               clearCartHit(cart);
               outSubLevelTrack[0] = false;
            }

            return hit;
         }
      }
   }

   static void remove(ServerSubLevel cart) {
      clearCartHit(cart);
      clearEngagedTick(cart);
   }

   static void clearEngagedTick(ServerSubLevel cart) {
      ENGAGED_TICK.remove(cart);
   }

   private static void clearCartHit(ServerSubLevel cart) {
      CART_HITS.remove(cart);
   }

   private static CoasterPathTrackFrame.TrackBasis alignedTrackBasis(CoasterPathTrackFrame.TrackBasis raw, boolean negateEdgeTangent) {
      return !negateEdgeTangent ? raw : new CoasterPathTrackFrame.TrackBasis(raw.forward().scale(-1.0), raw.right().scale(-1.0), raw.up());
   }

   private static void putCartHit(
      ServerSubLevel cart, CoasterPathTrackFrame.GraphHit hit, CoasterPathGraph graph, long gameTime, Vec3 bearingWorld, boolean subLevelTrack
   ) {
      int edgeIndex = graph.indexOfEdge(hit.edge());
      if (edgeIndex >= 0) {
         CART_HITS.put(
            cart,
            new CoasterCartTrackHitCache.CartHitEntry(
               edgeIndex, hit.t(), graph.revision(), gameTime, bearingWorld.x, bearingWorld.y, bearingWorld.z, subLevelTrack
            )
         );
      }
   }

   @Nullable
   private static CoasterPathTrackFrame.GraphHit refineEngagedHit(
      Level level, CoasterPathGraph graph, CoasterPathTrackFrame.GraphHit engagedHit, Vec3 bearingWorld, double maxDistSq, ServerSubLevel cart
   ) {
      int edgeIndex = graph.indexOfEdge(engagedHit.edge());
      if (edgeIndex < 0) {
         return null;
      } else {
         CoasterCartTrackHitCache.CartHitEntry prior = CART_HITS.get(cart);
         CoasterCartTrackHitCache.CartHitEntry seed = prior != null && prior.edgeIndex() == edgeIndex && prior.graphRevision() == graph.revision()
            ? prior
            : new CoasterCartTrackHitCache.CartHitEntry(edgeIndex, engagedHit.t(), graph.revision(), 0L, bearingWorld.x, bearingWorld.y, bearingWorld.z, true);
         return refineOnCachedEdge(level, graph, seed, bearingWorld, maxDistSq);
      }
   }

   @Nullable
   private static CoasterPathTrackFrame.GraphHit refineOnCachedEdge(
      Level level, CoasterPathGraph graph, CoasterCartTrackHitCache.CartHitEntry cached, Vec3 bearingWorld, double maxDistSq
   ) {
      List<CoasterPathEdge> edges = graph.edges();
      if (cached.edgeIndex() >= 0 && cached.edgeIndex() < edges.size()) {
         CoasterPathEdge edge = edges.get(cached.edgeIndex());
         Vec3 lastBearing = cached.bearingX() == 0.0 && cached.bearingY() == 0.0 && cached.bearingZ() == 0.0
            ? null
            : new Vec3(cached.bearingX(), cached.bearingY(), cached.bearingZ());
         CoasterPathTrackFrame.GraphHit hit = cached.subLevelTrack()
            ? CoasterPathTrackFrame.closestHitOnEdgeForEngagedSnap(level, edge, bearingWorld, cached.t(), lastBearing)
            : CoasterPathTrackFrame.closestHitOnEdge(level, edge, bearingWorld);
         return hit.point().distanceToSqr(bearingWorld) > maxDistSq ? null : hit;
      } else {
         return null;
      }
   }

   private static boolean sameEngagedHit(CoasterPathTrackFrame.GraphHit a, CoasterPathTrackFrame.GraphHit b) {
      return a.edge() != b.edge() ? false : Math.abs(a.t() - b.t()) <= 1.0E-5;
   }

   private static boolean nearEdgeEndpoint(double t) {
      return t <= 0.06 || t >= 0.94;
   }

   private static record CartHitEntry(
      int edgeIndex, double t, int graphRevision, long gameTime, double bearingX, double bearingY, double bearingZ, boolean subLevelTrack
   ) {
   }

   private static record EngagedTickEntry(long gameTime, int graphRevision, boolean negateEdgeTangent, CoasterCartTrackHitCache.EngagedTickFrame frame) {
   }

   static record EngagedTickFrame(CoasterPathTrackFrame.GraphHit graphHit, CoasterPathTrackFrame.TrackBasis trackBasis, Vector3d snapTargetWorld) {
   }

   private static record OpenEndTickEntry(long gameTime, int graphRevision, List<CoasterPathTraceEndpoints.OpenEndRef> refs) {
   }
}
