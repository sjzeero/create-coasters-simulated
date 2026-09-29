package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;

@OnlyIn(Dist.CLIENT)
public final class CoasterAnchorClientSpace {
   private CoasterAnchorClientSpace() {
   }

   @Nullable
   public static Pose3dc renderPoseForAnchor(Level level, BlockPos anchorHost, float partialTick) {
      SubLevel sub = Sable.HELPER.getContaining(level, anchorHost);
      if (sub == null) {
         return null;
      } else {
         return (Pose3dc)(sub instanceof ClientSubLevel clientSub ? clientSub.renderPose(partialTick) : sub.logicalPose());
      }
   }

   public static Vec3 toRenderWorld(Level level, BlockPos anchorHost, Vec3 plotLocal) {
      return toRenderWorld(renderPoseForAnchor(level, anchorHost, currentPartialTick(level)), plotLocal);
   }

   public static Vec3 toRenderWorld(Level level, BlockPos anchorHost, Vec3 plotLocal, float partialTick) {
      return toRenderWorld(renderPoseForAnchor(level, anchorHost, partialTick), plotLocal);
   }

   public static Vec3 toRenderWorld(@Nullable Pose3dc renderPose, Vec3 plotLocal) {
      return renderPose == null ? plotLocal : renderPose.transformPosition(plotLocal);
   }

   public static Vec3 toPlotLocal(Level level, BlockPos anchorHost, Vec3 renderWorld) {
      return toPlotLocal(renderPoseForAnchor(level, anchorHost, currentPartialTick(level)), renderWorld);
   }

   public static Vec3 toPlotLocal(Level level, BlockPos anchorHost, Vec3 renderWorld, float partialTick) {
      return toPlotLocal(renderPoseForAnchor(level, anchorHost, partialTick), renderWorld);
   }

   public static Vec3 toPlotLocal(@Nullable Pose3dc renderPose, Vec3 renderWorld) {
      return renderPose == null ? renderWorld : renderPose.transformPositionInverse(renderWorld);
   }

   public static Vec3 toRenderDirection(Level level, BlockPos anchorHost, Vec3 plotLocalDir) {
      return toRenderDirection(renderPoseForAnchor(level, anchorHost, currentPartialTick(level)), plotLocalDir);
   }

   public static Vec3 toRenderDirection(Level level, BlockPos anchorHost, Vec3 plotLocalDir, float partialTick) {
      return toRenderDirection(renderPoseForAnchor(level, anchorHost, partialTick), plotLocalDir);
   }

   public static Vec3 toRenderDirection(@Nullable Pose3dc renderPose, Vec3 plotLocalDir) {
      if (renderPose != null && !(plotLocalDir.lengthSqr() < 1.0E-12)) {
         Vector3d v = new Vector3d(plotLocalDir.x, plotLocalDir.y, plotLocalDir.z);
         renderPose.orientation().transform(v);
         return new Vec3(v.x, v.y, v.z);
      } else {
         return plotLocalDir;
      }
   }

   public static void premultiplyRenderPose(PoseStack poseStack, Pose3dc renderPose) {
      Matrix4d baked = new Matrix4d();
      renderPose.bakeIntoMatrix(baked);
      Matrix4f renderF = new Matrix4f(baked);
      Pose entry = poseStack.last();
      Matrix4f world = new Matrix4f(renderF).mul(entry.pose());
      entry.pose().set(world);
      Matrix3f rot = new Matrix3f(renderF);
      rot.mul(entry.normal());
      entry.normal().set(rot);
   }

   public static Vec3 toPlotLocalDirection(Level level, BlockPos anchorHost, Vec3 renderWorldDir) {
      if (renderWorldDir.lengthSqr() < 1.0E-12) {
         return renderWorldDir;
      } else {
         Vec3 refWorld = toRenderWorld(level, anchorHost, CoasterAnchorpointBlockEntity.worldCenter(level, anchorHost));
         Vec3 plotBase = toPlotLocal(level, anchorHost, refWorld);
         Vec3 plotTip = toPlotLocal(level, anchorHost, refWorld.add(renderWorldDir));
         return plotTip.subtract(plotBase);
      }
   }

   public static Vec3 anchorCenterRenderWorld(Level level, BlockPos anchorPos) {
      return toRenderWorld(level, anchorPos, CoasterAnchorpointBlockEntity.worldCenter(level, anchorPos));
   }

   public static Vec3 anchorCenterRenderWorld(Level level, BlockPos anchorPos, float partialTick) {
      return toRenderWorld(level, anchorPos, CoasterAnchorpointBlockEntity.worldCenter(level, anchorPos), partialTick);
   }

   public static Vec3 anchorShaftHostRenderWorld(Level level, BlockPos anchorPos, float partialTick) {
      return toRenderWorld(level, anchorPos, Vec3.atCenterOf(anchorPos), partialTick);
   }

   public static Vec3 anchorShaftHostRenderWorld(Level level, BlockPos anchorPos) {
      return anchorShaftHostRenderWorld(level, anchorPos, currentPartialTick(level));
   }

   public static boolean anchorHostsWithinPreviewSpan(Level level, BlockPos anchorA, BlockPos anchorB) {
      return anchorCenterRenderWorld(level, anchorA).distanceToSqr(anchorCenterRenderWorld(level, anchorB))
         <= (double)CoasterTrackGauge.maxCoasterCurveEndpointSpanBlocksSq();
   }

   public static Vec3 pathCenterlineRenderWorld(Level level, BlockPos anchorHost, Vec3 plotLocal) {
      return toRenderWorld(level, anchorHost, CoasterPathGraphManager.pathCenterlineWorld(plotLocal));
   }

   public static Vec3 pathCenterlineRenderWorld(Level level, BlockPos anchorHost, Vec3 plotLocal, float partialTick) {
      return toRenderWorld(level, anchorHost, CoasterPathGraphManager.pathCenterlineWorld(plotLocal), partialTick);
   }

   private static float currentPartialTick(Level level) {
      return level.isClientSide() ? AnimationTickHolder.getPartialTicks(level) : 0.0F;
   }
}
