package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class BalloonAssembly {
   private BalloonAssembly() {
   }

   public static void onHostBlocksMoved(ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> movedSources) {
      onHostBlocksMoved(level, transform, movedSources, null);
   }

   public static void onHostBlocksMoved(ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> movedSources, @Nullable UUID sourceSubLevelId) {
      ServerLevel resultingLevel = transform.getLevel();
      Set<BlockPos> movedLattice = new HashSet<>();

      for (BlockPos oldCell : movedSources) {
         if (oldCell != null) {
            movedLattice.add(oldCell);
         }
      }

      if (!movedLattice.isEmpty()) {
         rebindBalloonsOnMovedLattice(resultingLevel, transform, movedLattice);
         if (sourceSubLevelId != null) {
            BalloonHostIndex index = BalloonHostIndex.get(level);

            for (Entry<BalloonHostKey, UUID> entry : new ArrayList<>(index.entries())) {
               if (entry.getKey().hostSubLevelId().equals(sourceSubLevelId)) {
                  ServerSubLevel balloon = BalloonResolve.findBalloonById(resultingLevel, entry.getValue());
                  if (balloon != null) {
                     BalloonBlockEntity be = balloonBlockEntity(balloon);
                     if (be != null) {
                        BlockPos lattice = be.hostLatticePos();
                        if (lattice != null && movedLattice.contains(lattice)) {
                           rebindBalloonOnHostLattice(resultingLevel, balloon, be, transform.apply(lattice));
                        }
                     }
                  }
               }
            }
         }

         refreshAllBalloonHosts(resultingLevel);
      }
   }

   private static void rebindBalloonsOnMovedLattice(ServerLevel level, AssemblyTransform transform, Set<BlockPos> movedLattice) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         for (SubLevel sub : container.getAllSubLevels()) {
            if (sub instanceof ServerSubLevel) {
               ServerSubLevel balloonSub = (ServerSubLevel)sub;
               if (!balloonSub.isRemoved() && BalloonResolve.isBalloonSubLevel(balloonSub)) {
                  BalloonBlockEntity be = balloonBlockEntity(balloonSub);
                  if (be != null) {
                     BlockPos lattice = be.hostLatticePos();
                     if (lattice != null && movedLattice.contains(lattice)) {
                        rebindBalloonOnHostLattice(level, balloonSub, be, transform.apply(lattice));
                     }
                  }
               }
            }
         }
      }
   }

   public static void refreshAllBalloonHosts(ServerLevel level) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         for (SubLevel sub : container.getAllSubLevels()) {
            if (sub instanceof ServerSubLevel) {
               ServerSubLevel balloonSub = (ServerSubLevel)sub;
               if (!balloonSub.isRemoved() && BalloonResolve.isBalloonSubLevel(balloonSub)) {
                  BalloonBlockEntity be = balloonBlockEntity(balloonSub);
                  if (be != null) {
                     BalloonHostKey atHost = BalloonHostKey.relocateHostKey(level, be, balloonSub);
                     if (atHost != null) {
                        if (!atHost.equals(be.hostKey())) {
                           rebindBalloon(level, balloonSub, be, atHost);
                        } else {
                           be.refreshHostLattice(level, atHost);
                           BalloonTetherController.register(balloonSub, be);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void rebindBalloonOnHostLattice(ServerLevel level, UUID balloonId, BlockPos newLatticeCell) {
      ServerSubLevel balloon = BalloonResolve.findBalloonById(level, balloonId);
      if (balloon != null) {
         BalloonBlockEntity be = balloonBlockEntity(balloon);
         if (be != null) {
            rebindBalloonOnHostLattice(level, balloon, be, newLatticeCell);
         }
      }
   }

   private static void rebindBalloonOnHostLattice(ServerLevel level, ServerSubLevel balloon, BalloonBlockEntity be, BlockPos newLatticeCell) {
      BalloonHostKey newHost = BalloonHostKey.of(level, newLatticeCell);
      if (newHost != null) {
         rebindBalloon(level, balloon, be, newHost);
      }
   }

   private static void rebindBalloon(ServerLevel level, ServerSubLevel balloonSub, BalloonBlockEntity be, BalloonHostKey newHost) {
      BalloonHostKey oldHost = be.hostKey();
      if (oldHost.equals(newHost)) {
         be.refreshHostLattice(level, newHost);
         BalloonTetherController.register(balloonSub, be);
      } else {
         BalloonHostIndex.get(level).rebindHost(oldHost, newHost, balloonSub.getUniqueId());
         be.rebindHost(newHost);
         BalloonTetherController.register(balloonSub, be);
      }
   }

   @Nullable
   private static BalloonBlockEntity balloonBlockEntity(ServerSubLevel balloonSub) {
      EmbeddedPlotLevelAccessor embedded = balloonSub.getPlot().getEmbeddedLevelAccessor();
      BlockEntity var3 = embedded.getBlockEntity(BlockPos.ZERO);
      return var3 instanceof BalloonBlockEntity ? (BalloonBlockEntity)var3 : null;
   }
}
