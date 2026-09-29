package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackRenderer;
import com.simibubi.create.foundation.render.SpecialModels;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CoasterTrackAnchorGirderBeam {
   private CoasterTrackAnchorGirderBeam() {
   }

   @Nullable
   public static TransformedInstance[] createFlywheelGirders(
      InstancerProvider instancerProvider, Level level, BezierConnection bc, BlockPos anchorEndpoint, Vec3 visualOrigin
   ) {
      if (!CoasterTrackAnchorJunctionPlate.shouldRenderGirderAt(level, anchorEndpoint, bc)) {
         return null;
      } else {
         CoasterTrackAnchorJunctionPlate.EndpointSpine frame = CoasterTrackAnchorJunctionPlate.spineAtEndpoint(bc, anchorEndpoint, level);
         if (frame == null) {
            return null;
         } else {
            BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
            Vec3 curveOrigin = Vec3.atLowerCornerOf((Vec3i)p.bePositions.getFirst());
            Vec3 anchorRel = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorEndpoint).subtract(curveOrigin);
            Vec3 plateRel = CoasterTrackAnchorJunctionPlate.junctionPlateCenterInHostRelativeSpace(frame);
            Vec3 dir = plateRel.subtract(anchorRel);
            double lenSq = dir.lengthSqr();
            if (lenSq < 1.0E-10) {
               return null;
            } else {
               double span = Math.sqrt(lenSq);
               if (span < 0.125) {
                  return null;
               } else {
                  dir = dir.scale(1.0 / span);
                  int n = CoasterTrackGauge.anchorGirderHybridSegmentCount(span);
                  double tierWorldHeight = span / (double)n;
                  float scaleY = (float)(tierWorldHeight / 1.0);
                  PartialModel girderPartial = CoasterTrackAnchorGirderModel.partialForAnchor(level, anchorEndpoint);
                  TransformedInstance[] inst = new TransformedInstance[n];
                  instancerProvider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(girderPartial)).createInstances(inst);
                  PoseStack root = new PoseStack();
                  TransformStack.of(root).translate(visualOrigin);
                  Quaternionf q = girderOrientationAlongSpan(dir, frame);

                  for (int i = 0; i < n; i++) {
                     PoseStack local = new PoseStack();
                     TransformStack.of(local).translate(anchorRel);
                     local.mulPose(new Matrix4f().rotation(q));
                     TransformStack.of(local).translate(0.0F, (float)((double)i * tierWorldHeight), 0.0F);
                     TransformStack.of(local).scale(1.0F, scaleY, 1.0F);
                     TransformStack.of(local).translate(-0.5F, 0.0F, -0.5F);
                     inst[i].setTransform(root).mul(local.last()).setChanged();
                  }

                  return inst;
               }
            }
         }
      }
   }

   public static void renderBerGirders(Level level, BezierConnection bc, BlockPos anchorEndpoint, BlockPos curveHostPos, PoseStack ms, VertexConsumer vb) {
      if (CoasterTrackAnchorJunctionPlate.shouldRenderGirderAt(level, anchorEndpoint, bc)) {
         CoasterTrackAnchorJunctionPlate.EndpointSpine frame = CoasterTrackAnchorJunctionPlate.spineAtEndpoint(bc, anchorEndpoint, level);
         if (frame != null) {
            if (!bc.isPrimary()) {
               bc.secondary();
            }

            Vec3 curveOrigin = Vec3.atLowerCornerOf(curveHostPos);
            Vec3 anchorRel = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorEndpoint).subtract(curveOrigin);
            Vec3 plateRel = CoasterTrackAnchorJunctionPlate.junctionPlateCenterForVisualRoot(frame);
            Vec3 dir = plateRel.subtract(anchorRel);
            double lenSq = dir.lengthSqr();
            if (!(lenSq < 1.0E-10)) {
               double span = Math.sqrt(lenSq);
               if (!(span < 0.125)) {
                  dir = dir.scale(1.0 / span);
                  int n = CoasterTrackGauge.anchorGirderHybridSegmentCount(span);
                  double tierWorldHeight = span / (double)n;
                  float scaleY = (float)(tierWorldHeight / 1.0);
                  PartialModel girderPartial = CoasterTrackAnchorGirderModel.partialForAnchor(level, anchorEndpoint);
                  BlockState air = Blocks.AIR.defaultBlockState();
                  int light = LevelRenderer.getLightColor(level, anchorEndpoint);
                  Quaternionf q = girderOrientationAlongSpan(dir, frame);

                  for (int i = 0; i < n; i++) {
                     PoseStack local = new PoseStack();
                     TransformStack.of(local).translate(anchorRel);
                     local.mulPose(new Matrix4f().rotation(q));
                     TransformStack.of(local).translate(0.0F, (float)((double)i * tierWorldHeight), 0.0F);
                     TransformStack.of(local).scale(1.0F, scaleY, 1.0F);
                     TransformStack.of(local).translate(-0.5F, 0.0F, -0.5F);
                     ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(girderPartial, air).mulPose(local.last().pose()))
                           .mulNormal(local.last().normal()))
                        .light(light)
                        .renderInto(ms, vb);
                  }
               }
            }
         }
      }
   }

   private static Quaternionf girderOrientationAlongSpan(Vec3 spanDir, CoasterTrackAnchorJunctionPlate.EndpointSpine frame) {
      Quaternionf yAligned = yAxisToDirection((float)spanDir.x, (float)spanDir.y, (float)spanDir.z);
      Vec3 angles = TrackRenderer.getModelAngles(frame.lateral(), frame.chordIntoTrack());
      Quaternionf trackFrame = new Quaternionf().rotateY((float)angles.y).rotateX((float)angles.x).rotateZ((float)angles.z);
      Vector3f trackX = trackFrame.transform(new Vector3f(1.0F, 0.0F, 0.0F));
      Vector3f trackZ = trackFrame.transform(new Vector3f(0.0F, 0.0F, 1.0F));
      Vec3 axis = spanDir.normalize();
      Vec3 targetX = projectOntoPlane(new Vec3((double)trackX.x, (double)trackX.y, (double)trackX.z), axis);
      if (targetX.lengthSqr() < 1.0E-12) {
         targetX = projectOntoPlane(new Vec3((double)trackZ.x, (double)trackZ.y, (double)trackZ.z), axis);
      }

      if (targetX.lengthSqr() < 1.0E-12) {
         return yAligned;
      } else {
         targetX = targetX.normalize();
         Vector3f modelX = yAligned.transform(new Vector3f(1.0F, 0.0F, 0.0F));
         Vec3 currentX = projectOntoPlane(new Vec3((double)modelX.x, (double)modelX.y, (double)modelX.z), axis);
         if (currentX.lengthSqr() < 1.0E-12) {
            return yAligned;
         } else {
            currentX = currentX.normalize();
            double sin = axis.dot(currentX.cross(targetX));
            double cos = currentX.dot(targetX);
            float twist = (float)Math.atan2(sin, cos);
            return Math.abs(twist) < 1.0E-4F ? yAligned : new Quaternionf().rotationAxis(twist, (float)axis.x, (float)axis.y, (float)axis.z).mul(yAligned);
         }
      }
   }

   private static Vec3 projectOntoPlane(Vec3 v, Vec3 planeNormal) {
      Vec3 n = planeNormal.normalize();
      return v.subtract(n.scale(v.dot(n)));
   }

   private static Quaternionf yAxisToDirection(float dx, float dy, float dz) {
      Vector3f from = new Vector3f(0.0F, 1.0F, 0.0F);
      Vector3f to = new Vector3f(dx, dy, dz);
      if (to.lengthSquared() < 1.0E-12F) {
         return new Quaternionf();
      } else {
         to.normalize();
         float dot = from.dot(to);
         if (dot > 0.9999F) {
            return new Quaternionf();
         } else if (dot < -0.9999F) {
            return new Quaternionf().rotationAxis((float) Math.PI, 1.0F, 0.0F, 0.0F);
         } else {
            Vector3f axis = new Vector3f(from).cross(to);
            if (axis.lengthSquared() < 1.0E-12F) {
               return new Quaternionf();
            } else {
               axis.normalize();
               float angle = (float)Math.acos((double)Mth.clamp(dot, -1.0F, 1.0F));
               return new Quaternionf().rotationAxis(angle, axis.x, axis.y, axis.z);
            }
         }
      }
   }
}
