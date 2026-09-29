package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class AnchorJunctionVisualRefresh {
   private static final Set<BlockPos> PENDING_SEEDS = new LinkedHashSet<>();
   @Nullable
   private static Set<BlockPos> followUpCluster = null;

   private AnchorJunctionVisualRefresh() {
   }

   public static void scheduleClusterRefresh(Level level, BlockPos... seedAnchors) {
      if (level != null && level.isClientSide()) {
         for (BlockPos p : seedAnchors) {
            if (p != null && !p.equals(BlockPos.ZERO)) {
               PENDING_SEEDS.add(p.immutable());
            }
         }
      }
   }

   @Deprecated
   public static void refreshAround(Level level, BlockPos... seedAnchors) {
      scheduleClusterRefresh(level, seedAnchors);
   }

   public static void clientTickPost(ClientLevel level) {
      if (followUpCluster != null) {
         queueClusterUpdates(level, followUpCluster);
         followUpCluster = null;
      }

      if (!PENDING_SEEDS.isEmpty()) {
         Set<BlockPos> seeds = new LinkedHashSet<>(PENDING_SEEDS);
         PENDING_SEEDS.clear();
         Set<BlockPos> cluster = expandOneHop(level, seeds);
         queueClusterUpdates(level, cluster);
         if (!cluster.isEmpty()) {
            followUpCluster = cluster;
         }
      }
   }

   private static void queueClusterUpdates(ClientLevel level, Set<BlockPos> anchorEndpoints) {
      for (BlockPos anchorPos : anchorEndpoints) {
         if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity anchor) {
            VisualizationHelper.queueUpdate(anchor);
         }
      }
   }

   private static Set<BlockPos> expandOneHop(Level level, Set<BlockPos> seedAnchors) {
      Set<BlockPos> out = new LinkedHashSet<>();

      for (BlockPos seed : seedAnchors) {
         if (seed != null && !seed.equals(BlockPos.ZERO)) {
            out.add(seed.immutable());
            BlockEntity var6 = level.getBlockEntity(seed);
            if (var6 instanceof CoasterAnchorpointBlockEntity) {
               CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)var6;

               for (BezierConnection bc : anchor.getAnchorPeerCurvesView().values()) {
                  if (bc != null) {
                     BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
                     out.add(((BlockPos)p.bePositions.getFirst()).immutable());
                     out.add(((BlockPos)p.bePositions.getSecond()).immutable());
                  }
               }
            }
         }
      }

      return out;
   }
}
