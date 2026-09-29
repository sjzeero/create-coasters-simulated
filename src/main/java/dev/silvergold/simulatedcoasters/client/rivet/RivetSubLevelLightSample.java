package dev.silvergold.simulatedcoasters.client.rivet;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class RivetSubLevelLightSample {
   private RivetSubLevelLightSample() {
   }

   public static BlockPos probeCell(ClientLevel level, ClientSubLevel rivetSub, BlockState rivetState, @Nullable RivetBlockEntity be) {
      if (be != null) {
         Pose3dc hostPose = hostRenderPose(level, be);
         Vec3 airPlot = Vec3.atCenterOf(be.hostPos().relative(be.face()));
         Vec3 world = hostPose == null ? airPlot : hostPose.transformPosition(airPlot);
         return BlockPos.containing(world);
      } else {
         Pose3dc rivetPose = rivetSub.renderPose();
         Vec3 anchorPlot = RivetPlacement.rivetOutwardAnchorPlot(BlockPos.ZERO);
         double plateOut = 0.14250000000000002;
         Vec3 world = rivetPose.transformPosition(new Vec3(anchorPlot.x, anchorPlot.y + plateOut, anchorPlot.z));
         return BlockPos.containing(world);
      }
   }

   @Nullable
   private static Pose3dc hostRenderPose(ClientLevel level, RivetBlockEntity be) {
      UUID hostSubLevelId = be.hostKey().hostSubLevelId();
      if (hostSubLevelId == null) {
         return null;
      } else {
         SubLevelContainer raw = SubLevelContainer.getContainer(level);
         if (raw == null) {
            return null;
         } else {
            SubLevel hostSub = raw.getSubLevel(hostSubLevelId);
            if (hostSub instanceof ClientSubLevel clientHost) {
               return clientHost.renderPose();
            } else {
               return hostSub != null ? hostSub.logicalPose() : null;
            }
         }
      }
   }
}
