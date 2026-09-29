package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class BalloonSpawner {
   private BalloonSpawner() {
   }

   @Nullable
   public static ServerSubLevel tryAttach(ServerLevel level, BlockPos hostCell) {
      if (!BalloonPlacement.isValidHost(level, hostCell)) {
         return null;
      } else {
         BalloonHostKey hostKey = BalloonHostKey.of(level, hostCell);
         if (hostKey == null) {
            return null;
         } else {
            if (BalloonHostIndex.get(level).hasBalloonAt(hostKey)) {
               ServerSubLevel existing = BalloonHostIndex.get(level).findByHost(level, hostKey);
               if (existing != null) {
                  return null;
               }

               BalloonHostIndex.get(level).unregisterHost(hostKey);
            }

            SubLevelContainer raw = SubLevelContainer.getContainer(level);
            if (raw instanceof ServerSubLevelContainer container) {
               ServerSubLevel hostSub = (ServerSubLevel)container.getSubLevel(hostKey.hostSubLevelId());
               if (hostSub != null && !hostSub.isRemoved()) {
                  Vector3d spawnAnchor = BalloonPlacement.desiredStringWorld(hostSub, hostKey.cell());
                  if (spawnAnchor == null) {
                     return null;
                  } else {
                     Pose3d pose = new Pose3d();
                     pose.position().set(spawnAnchor);
                     pose.orientation().set(new Quaterniond());

                     try {
                        SubLevel sub = container.allocateNewSubLevel(pose);
                        LevelPlot plot = sub.getPlot();
                        plot.newEmptyChunk(plot.getCenterChunk());
                        BlockState balloonState = ((RedBalloonBlock)SimulatedCoastersBlocks.RED_BALLOON.get()).defaultBlockState();
                        if (!plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, balloonState, 3)) {
                           ((ServerSubLevel)sub).markRemoved();
                           return null;
                        } else {
                           ServerSubLevel balloonSub = (ServerSubLevel)sub;
                           if (plot.getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof BalloonBlockEntity be) {
                              be.bindHost(hostKey);
                              BalloonPoseAlignment.alignToNominal(level, container, balloonSub, hostSub, hostKey.cell());
                              BalloonHostIndex.get(level).register(hostKey, balloonSub.getUniqueId());
                              BalloonTetherController.register(balloonSub, be);
                              return balloonSub;
                           } else {
                              balloonSub.markRemoved();
                              return null;
                           }
                        }
                     } catch (IllegalStateException var14) {
                        return null;
                     }
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         }
      }
   }
}
