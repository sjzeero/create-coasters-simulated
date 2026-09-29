package dev.silvergold.simulatedcoasters.track.chainlift;

import com.simibubi.create.content.contraptions.StructureTransform;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class ChainLiftShaftAxis {
   private static final double AXIS_MATCH_EPS = 0.25;

   private ChainLiftShaftAxis() {
   }

   public static Vec3 resolveFromEye(Level level, BlockPos anchor, Vec3 eyePosition) {
      BlockState state = level.getBlockState(anchor);
      Direction facing = state.hasProperty(CoasterAnchorpointBlock.FACING) ? (Direction)state.getValue(CoasterAnchorpointBlock.FACING) : Direction.UP;
      Vec3 faceNormal = Vec3.atLowerCornerOf(facing.getNormal()).normalize();
      Vec3[] inPlane = inPlaneAxes(facing);
      Vec3 center = Vec3.atCenterOf(anchor);
      Vec3 toViewer = eyePosition.subtract(center);
      Vec3 planar = toViewer.subtract(faceNormal.scale(toViewer.dot(faceNormal)));
      if (planar.lengthSqr() < 1.0E-8) {
         return inPlane[0];
      } else {
         Vec3 planarUnit = planar.normalize();
         double alongFirst = Math.abs(planarUnit.dot(inPlane[0]));
         double alongSecond = Math.abs(planarUnit.dot(inPlane[1]));
         Vec3 axis = alongFirst >= alongSecond ? inPlane[0] : inPlane[1];
         if (planarUnit.dot(axis) < 0.0) {
            axis = axis.scale(-1.0);
         }

         return axis;
      }
   }

   public static Axis toDominantAxis(Vec3 axis) {
      Vec3 n = axis.normalize();
      double ax = Math.abs(n.x);
      double ay = Math.abs(n.y);
      double az = Math.abs(n.z);
      if (ax >= ay && ax >= az) {
         return Axis.X;
      } else {
         return ay >= az ? Axis.Y : Axis.Z;
      }
   }

   public static Axis rotateStoredAxis(StructureTransform transform, Axis axis) {
      return toDominantAxis(transform.applyWithoutOffsetUncentered(unitVector(axis)));
   }

   private static Vec3 unitVector(Axis axis) {
      return switch (axis) {
         case X -> new Vec3(1.0, 0.0, 0.0);
         case Y -> new Vec3(0.0, 1.0, 0.0);
         case Z -> new Vec3(0.0, 0.0, 1.0);
         default -> throw new MatchException(null, null);
      };
   }

   public static boolean matchesSentAxis(Vec3 serverAxis, Vec3 clientAxis) {
      if (clientAxis.lengthSqr() < 1.0E-12) {
         return false;
      } else {
         Vec3 a = serverAxis.normalize();
         Vec3 b = clientAxis.normalize();
         return Math.abs(a.dot(b)) >= 0.75;
      }
   }

   public static boolean isValidForFacing(Vec3 axis, Direction facing) {
      if (axis.lengthSqr() < 1.0E-12) {
         return false;
      } else {
         Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal()).normalize();
         return Math.abs(axis.normalize().dot(normal)) < 0.01;
      }
   }

   public static Axis alternateAxis(Axis current, Direction facing) {
      Vec3[] inPlane = inPlaneAxes(facing);
      Axis first = toDominantAxis(inPlane[0]);
      Axis second = toDominantAxis(inPlane[1]);
      if (current == first) {
         return second;
      } else {
         return current == second ? first : second;
      }
   }

   private static Vec3[] inPlaneAxes(Direction facing) {
      return switch (facing.getAxis()) {
         case X -> new Vec3[]{new Vec3(0.0, 1.0, 0.0), new Vec3(0.0, 0.0, 1.0)};
         case Y -> new Vec3[]{new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0)};
         case Z -> new Vec3[]{new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0)};
         default -> throw new MatchException(null, null);
      };
   }
}
