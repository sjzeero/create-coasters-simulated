package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CoasterTrackPropagator {
   private static final int MAX_COMPONENT = 16384;
   private static int batchDepth;
   private static final Set<BlockPos> batchSeeds = new LinkedHashSet<>();
   private static final Set<BlockPos> batchBlockSync = new LinkedHashSet<>();
   private static final Set<BlockPos> batchVisualStateSync = new LinkedHashSet<>();
   private static final List<BezierConnection> batchFakeTrackRemoves = new ArrayList<>();
   private static final List<BezierConnection> batchFakeTrackAdds = new ArrayList<>();

   private CoasterTrackPropagator() {
   }

   public static boolean isBatching() {
      return batchDepth > 0;
   }

   public static boolean scheduleBlockSyncIfBatching(BlockPos pos) {
      if (batchDepth > 0 && pos != null) {
         batchBlockSync.add(pos.immutable());
         return true;
      } else {
         return false;
      }
   }

   public static boolean scheduleVisualBlockStateSync(BlockPos pos) {
      if (batchDepth > 0 && pos != null) {
         batchVisualStateSync.add(pos.immutable());
         return true;
      } else {
         return false;
      }
   }

   public static void manageFakeTracks(ServerLevel level, BezierConnection bc, boolean remove) {
      if (bc.isPrimary()) {
         if (batchDepth > 0) {
            if (remove) {
               batchFakeTrackRemoves.add(bc);
            } else {
               batchFakeTrackAdds.add(bc);
            }
         } else {
            AnchorPeerFakeTracks.manageFakeTracksAlong(level, bc, remove);
         }
      }
   }

   public static void runBatchUpdate(ServerLevel level, Runnable work) {
      batchDepth++;

      try {
         work.run();
      } finally {
         batchDepth--;
         if (batchDepth == 0) {
            try {
               if (!batchSeeds.isEmpty()) {
                  Set<BlockPos> seeds = new LinkedHashSet<>(batchSeeds);
                  batchSeeds.clear();
                  updateRegion(level, seeds);
               }
            } finally {
               flushBatchDeferredWork(level);
            }
         }
      }
   }

   private static void flushBatchDeferredWork(ServerLevel level) {
      for (BezierConnection bc : batchFakeTrackRemoves) {
         AnchorPeerFakeTracks.manageFakeTracksAlong(level, bc, true);
      }

      batchFakeTrackRemoves.clear();

      for (BezierConnection bc : batchFakeTrackAdds) {
         AnchorPeerFakeTracks.manageFakeTracksAlong(level, bc, false);
      }

      batchFakeTrackAdds.clear();

      for (BlockPos p : batchVisualStateSync) {
         if (level.getBlockEntity(p) instanceof CoasterAnchorpointBlockEntity be) {
            be.syncAnchorVisualBlockStateDeferred();
         }
      }

      batchVisualStateSync.clear();

      for (BlockPos px : batchBlockSync) {
         if (level.getBlockEntity(px) instanceof CoasterAnchorpointBlockEntity be) {
            be.notifyUpdate();
         }
      }

      batchBlockSync.clear();
   }

   public static void touch(ServerLevel level, Collection<BlockPos> positions) {
      Set<BlockPos> seeds = collectAnchorSeeds(level, positions);
      if (!seeds.isEmpty()) {
         if (batchDepth > 0) {
            batchSeeds.addAll(seeds);
         } else {
            updateRegion(level, seeds);
         }
      }
   }

   public static void afterSubLevelBulkMove(ServerLevel level, Set<BlockPos> vacatedWorldCells, Set<BlockPos> placedCells) {
      if (!vacatedWorldCells.isEmpty() || !placedCells.isEmpty()) {
         Set<BlockPos> seeds = new HashSet<>();

         for (BlockPos p : placedCells) {
            if (p != null && level.getBlockState(p).getBlock() instanceof CoasterAnchorpointBlock) {
               seeds.add(p.immutable());
            }
         }

         for (BlockPos old : vacatedWorldCells) {
            if (old != null) {
               BlockPos immutableOld = old.immutable();
               BlockPos.betweenClosedStream(immutableOld.offset(-1, -1, -1), immutableOld.offset(1, 1, 1)).forEach(n -> {
                  if (!n.equals(immutableOld)) {
                     if (level.getBlockState(n).getBlock() instanceof CoasterAnchorpointBlock) {
                        seeds.add(n.immutable());
                     }
                  }
               });
            }
         }

         Set<BlockPos> region = new HashSet<>();

         for (BlockPos v : vacatedWorldCells) {
            if (v != null) {
               region.add(v.immutable());
            }
         }

         region.addAll(unionComponents(level, seeds));
         if (!region.isEmpty()) {
            updateRegionWithFixedRegion(level, region);
         }
      }
   }

   private static void updateRegionWithFixedRegion(ServerLevel level, Set<BlockPos> region) {
      CoasterPathSavedData data = CoasterPathSavedData.get(level.getServer());
      CoasterPathGraph graph = data.graphFor(level.dimension());
      graph.removeEdgesTouching(region);
      graph.removeNodesIn(region);
      rebuildGraphInRegion(level, graph, region);
      graph.pruneOrphanNodes();
      data.setDirty();
      ChainLiftSpanPathCache.invalidateAll();
      CoasterPathSync.scheduleBroadcastDimension(level.getServer(), level.dimension());
   }

   private static void updateRegion(ServerLevel level, Collection<BlockPos> seeds) {
      Set<BlockPos> region = regionFromSeeds(level, seeds);
      if (!region.isEmpty()) {
         updateRegionWithFixedRegion(level, region);
      }
   }

   private static Set<BlockPos> regionFromSeeds(ServerLevel level, Collection<BlockPos> seeds) {
      Set<BlockPos> region = collectAnchorSeeds(level, seeds);
      if (region.isEmpty()) {
         return region;
      } else {
         Set<BlockPos> expanded = new LinkedHashSet<>(region);

         for (BlockPos p : region) {
            BlockEntity var7 = level.getBlockEntity(p);
            if (var7 instanceof CoasterAnchorpointBlockEntity) {
               CoasterAnchorpointBlockEntity abe = (CoasterAnchorpointBlockEntity)var7;

               for (BlockPos peer : abe.getAnchorPeerCurvesView().keySet()) {
                  expanded.add(peer.immutable());
               }
            }
         }

         return expanded;
      }
   }

   private static Set<BlockPos> collectAnchorSeeds(ServerLevel level, Collection<BlockPos> positions) {
      Set<BlockPos> seeds = new LinkedHashSet<>();

      for (BlockPos p : positions) {
         if (p != null && level.getBlockState(p).getBlock() instanceof CoasterAnchorpointBlock) {
            seeds.add(p.immutable());
         }
      }

      return seeds;
   }

   private static Set<BlockPos> unionComponents(ServerLevel level, Collection<BlockPos> seeds) {
      Set<BlockPos> out = new HashSet<>();
      ArrayDeque<BlockPos> q = new ArrayDeque<>();

      for (BlockPos s : seeds) {
         if (s != null && level.getBlockState(s).getBlock() instanceof CoasterAnchorpointBlock) {
            q.add(s);
         }
      }

      while (!q.isEmpty() && out.size() < 16384) {
         BlockPos p = q.removeFirst();
         if (out.add(p)) {
            BlockEntity var6 = level.getBlockEntity(p);
            if (var6 instanceof CoasterAnchorpointBlockEntity) {
               CoasterAnchorpointBlockEntity abe = (CoasterAnchorpointBlockEntity)var6;

               for (BlockPos peer : abe.getAnchorPeerCurvesView().keySet()) {
                  q.addLast(peer);
               }
            }
         }
      }

      return out;
   }

   private static void rebuildGraphInRegion(ServerLevel level, CoasterPathGraph graph, Set<BlockPos> region) {
      Set<String> seen = new HashSet<>();

      for (BlockPos p : region) {
         BlockEntity var7 = level.getBlockEntity(p);
         if (var7 instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity abe = (CoasterAnchorpointBlockEntity)var7;

            for (BezierConnection bc : abe.getAnchorPeerCurvesView().values()) {
               if (bc != null) {
                  BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
                  BlockPos from = (BlockPos)primary.bePositions.getFirst();
                  BlockPos to = (BlockPos)primary.bePositions.getSecond();
                  if (isCurveEndpoint(level, from) && isCurveEndpoint(level, to) && (region.contains(from) || region.contains(to))) {
                     String k = edgeKey(from, to);
                     if (seen.add(k)) {
                        graph.upsertNode(new CoasterPathNode(from, (Vec3)primary.normals.getFirst()));
                        graph.upsertNode(new CoasterPathNode(to, (Vec3)primary.normals.getSecond()));
                        graph.addEdge(CoasterPathEdge.curve(from, to, primary, endpointRollDeg(level, from), endpointRollDeg(level, to)));
                     }
                  }
               }
            }
         }
      }

      CoasterPathTangentOrientation.alignComponentsTouchingRegion(level, graph, region);
   }

   private static boolean isCurveEndpoint(Level level, BlockPos pos) {
      BlockState st = level.getBlockState(pos);
      return st.getBlock() instanceof CoasterAnchorpointBlock;
   }

   private static double endpointRollDeg(ServerLevel level, BlockPos anchorPos) {
      return level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity abe ? Math.toDegrees((double)abe.getJointBankRadians()) : 0.0;
   }

   private static String edgeKey(BlockPos a, BlockPos b) {
      long al = a.asLong();
      long bl = b.asLong();
      return al <= bl ? al + "|" + bl : bl + "|" + al;
   }
}
