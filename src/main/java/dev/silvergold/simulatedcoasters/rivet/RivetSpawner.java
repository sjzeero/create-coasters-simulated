package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;

public final class RivetSpawner {
   private RivetSpawner() {
   }

   @Nullable
   public static ServerSubLevel tryPlace(ServerLevel level, RivetPlacement.Resolved placement) {
      if (!RivetPlacement.isValidPlacement(level, placement)) {
         return null;
      } else {
         RivetAnchorKey anchor = RivetAnchorKey.from(level, placement);
         if (RivetHostIndex.get(level).hasRivetAt(anchor)) {
            return null;
         } else {
            RivetHostKey hostKey = anchor.host();
            SubLevelContainer raw = SubLevelContainer.getContainer(level);
            if (raw instanceof ServerSubLevelContainer container) {
               Vec3 faceAnchor = RivetPlacement.faceAnchorWorld(level, placement);
               Quaterniond orientation = RivetPlacement.worldOrientation(level, placement);
               Pose3d pose = new Pose3d();
               pose.position().set(faceAnchor.x, faceAnchor.y, faceAnchor.z);
               pose.orientation().set(orientation);

               try {
                  SubLevel sub = container.allocateNewSubLevel(pose);
                  LevelPlot plot = sub.getPlot();
                  plot.newEmptyChunk(plot.getCenterChunk());
                  BlockState rivetState = (BlockState)((RivetBlock)SimulatedCoastersBlocks.RIVET.get())
                     .defaultBlockState()
                     .setValue(RivetBlock.FACING, Direction.UP);
                  if (!plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, rivetState, 3)) {
                     ((ServerSubLevel)sub).markRemoved();
                     return null;
                  } else {
                     ServerSubLevel serverSub = (ServerSubLevel)sub;
                     serverSub.updateMergedMassData(1.0F);
                     if (plot.getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof RivetBlockEntity be) {
                        be.bindHost(hostKey, placement.face(), RivetPlacement.attachment(placement), placement.yRotationDegrees());
                        RivetPoseAlignment.alignToNominal(level, container, serverSub, be);
                        be.applyHostLock(serverSub);
                        RivetHostIndex.get(level).register(anchor, serverSub.getUniqueId());
                        return serverSub;
                     } else {
                        serverSub.markRemoved();
                        return null;
                     }
                  }
               } catch (IllegalStateException var15) {
                  return null;
               }
            } else {
               return null;
            }
         }
      }
   }
}
