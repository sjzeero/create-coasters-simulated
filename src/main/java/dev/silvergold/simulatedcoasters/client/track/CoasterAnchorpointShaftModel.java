package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.createmod.catnip.render.SuperByteBufferCache.Compartment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class CoasterAnchorpointShaftModel {
   public static final Compartment<BlockState> SHAFT_BODY = new Compartment();

   private CoasterAnchorpointShaftModel() {
   }

   public static void registerClientInit() {
      SuperByteBufferCache.getInstance().registerCompartment(SHAFT_BODY);
   }

   public static void renderBody(PoseStack ms, MultiBufferSource buffer, BlockState anchorState, Direction facing, Axis shaftAxis, int light) {
      SuperByteBuffer model = CachedBuffers.block(SHAFT_BODY, anchorState);
      applyShaftAxisAlignment(model, facing, shaftAxis);
      model.light(light).renderInto(ms, buffer.getBuffer(RenderType.cutout()));
   }

   private static void applyShaftAxisAlignment(SuperByteBuffer buf, Direction facing, Axis shaftAxis) {
      Vec3 modelShaftAxis = modelShaftAxisAfterFacing(facing);
      Vec3 shaftDir = axisUnit(shaftAxis);
      Vec3 faceNormal = Vec3.atLowerCornerOf(facing.getNormal());
      float degrees = (float)Math.toDegrees(signedAngleRadians(faceNormal, modelShaftAxis, shaftDir));
      if (!(Math.abs(degrees) < 0.001F)) {
         rotateCenteredDegrees(buf, degrees, facing);
      }
   }

   private static Vec3 modelShaftAxisAfterFacing(Direction facing) {
      int[] rotations = blockstateRotations(facing);
      BlockModelRotation rotation = BlockModelRotation.by(rotations[0], rotations[1]);
      Vector3f bore = new Vector3f(0.0F, 0.0F, 1.0F);
      Transformation transform = rotation.getRotation();
      transform.transformNormal(bore);
      return new Vec3((double)bore.x(), (double)bore.y(), (double)bore.z());
   }

   private static int[] blockstateRotations(Direction facing) {
      return switch (facing) {
         case DOWN -> new int[]{180, 0};
         case NORTH -> new int[]{90, 0};
         case SOUTH -> new int[]{270, 0};
         case WEST -> new int[]{90, 270};
         case EAST -> new int[]{90, 90};
         case UP -> new int[]{0, 0};
         default -> throw new MatchException(null, null);
      };
   }

   private static void rotateCenteredDegrees(SuperByteBuffer buf, float degrees, Direction axis) {
      if (!(Math.abs(degrees) < 0.001F)) {
         buf.rotateCentered((float)Math.toRadians((double)degrees), axis);
      }
   }

   private static Vec3 axisUnit(Axis axis) {
      return switch (axis) {
         case X -> new Vec3(1.0, 0.0, 0.0);
         case Y -> new Vec3(0.0, 1.0, 0.0);
         case Z -> new Vec3(0.0, 0.0, 1.0);
         default -> throw new MatchException(null, null);
      };
   }

   private static double signedAngleRadians(Vec3 axis, Vec3 from, Vec3 to) {
      Vec3 a = projectOntoPlane(from, axis);
      Vec3 b = projectOntoPlane(to, axis);
      if (!(a.lengthSqr() < 1.0E-12) && !(b.lengthSqr() < 1.0E-12)) {
         a = a.normalize();
         b = b.normalize();
         Vec3 n = axis.normalize();
         double sin = n.dot(a.cross(b));
         double cos = a.dot(b);
         return Math.atan2(sin, cos);
      } else {
         return 0.0;
      }
   }

   private static Vec3 projectOntoPlane(Vec3 v, Vec3 planeNormal) {
      Vec3 n = planeNormal.normalize();
      return v.subtract(n.scale(v.dot(n)));
   }
}
