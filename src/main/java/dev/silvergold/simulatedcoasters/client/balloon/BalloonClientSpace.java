package dev.silvergold.simulatedcoasters.client.balloon;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

@OnlyIn(Dist.CLIENT)
public final class BalloonClientSpace {
   private BalloonClientSpace() {
   }

   @Nullable
   public static SubLevel resolveHostSubLevel(Level level, BalloonBlockEntity be) {
      SubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container == null) {
         return null;
      } else {
         SubLevel byId = container.getSubLevel(be.hostSubLevelId());
         return byId != null && !byId.isRemoved() ? byId : null;
      }
   }

   public static Vec3 plotPointRenderWorld(SubLevel sub, Vector3d plotPoint, float partialTick) {
      return plotPointRenderWorld(sub, new Vec3(plotPoint.x, plotPoint.y, plotPoint.z), partialTick);
   }

   public static Vec3 plotPointRenderWorld(SubLevel sub, Vec3 plotPoint, float partialTick) {
      Pose3dc pose = renderPose(sub, partialTick);
      return pose == null ? plotPoint : pose.transformPosition(plotPoint);
   }

   private static Pose3dc renderPose(SubLevel sub, float partialTick) {
      return (Pose3dc)(sub instanceof ClientSubLevel clientSub ? clientSub.renderPose(partialTick) : sub.logicalPose());
   }
}
