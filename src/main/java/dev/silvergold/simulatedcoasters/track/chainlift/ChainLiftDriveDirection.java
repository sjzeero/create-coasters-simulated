package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftDriveDirection {
   private static final double SPAN_DIR_SAMPLE_BLOCKS = 0.25;
   private static final double PARALLEL_EPS = 1.0E-4;

   private ChainLiftDriveDirection() {
   }

   public static Vec3 beltMotionAtShaftTop(BlockState anchorState, Axis shaftAxis, float signedRpm) {
      Vec3 axisUnit = positiveAxisUnit(shaftAxis);
      Vec3 crownRadial = crownRadial(axisUnit, anchorState);
      Vec3 tangent = crownRadial.cross(axisUnit);
      if (tangent.lengthSqr() < 1.0E-12) {
         return Vec3.ZERO;
      } else {
         double sign = signedRpm == 0.0F ? 1.0 : (double)Math.signum(signedRpm);
         return tangent.normalize().scale(sign);
      }
   }

   @Nullable
   public static Vec3 spanTraversalDirection(Level level, CoasterPathGraphSpans.SpanPath path, double partialTickForRender) {
      double total = path.totalLength();
      if (total <= 1.0E-6) {
         return null;
      } else {
         Vec3 start = CoasterPathGraphSpans.samplePositionAtArcLength(level, path, 0.0, partialTickForRender);
         double sample = Math.min(0.25, total * 0.5);
         Vec3 ahead = CoasterPathGraphSpans.samplePositionAtArcLength(level, path, sample, partialTickForRender);
         Vec3 delta = ahead.subtract(start);
         return delta.lengthSqr() < 1.0E-12 ? null : delta.normalize();
      }
   }

   public static int computeTravelSign(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path, double partialTickForRender) {
      Level level = anchor.getLevel();
      Axis shaftAxis = anchor.getChainLiftShaftAxis();
      if (level != null && shaftAxis != null) {
         Vec3 belt = beltMotionAtShaftTop(anchor.getBlockState(), shaftAxis, 1.0F);
         Vec3 spanDir = spanTraversalDirection(level, path, partialTickForRender);
         if (!(belt.lengthSqr() < 1.0E-12) && spanDir != null) {
            return belt.dot(spanDir) >= 0.0 ? 1 : -1;
         } else {
            return 1;
         }
      } else {
         return 1;
      }
   }

   public static double signedChainBlocksPerSecond(
      CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path, double partialTickForRender, float maxShaftRpm, double blocksPerSecondAtMaxRpm
   ) {
      float rpm = anchor.getSpeed();
      double magnitude = blocksPerSecondAtMaxRpm * (double)Math.abs(rpm) / (double)maxShaftRpm;
      if (magnitude <= 1.0E-9) {
         return 0.0;
      } else {
         int travelSign = anchor.getChainLiftTravelSign();
         if (travelSign != 0) {
            return magnitude * (double)Math.signum(rpm) * (double)travelSign;
         } else {
            Level level = anchor.getLevel();
            Axis shaftAxis = anchor.getChainLiftShaftAxis();
            if (level != null && shaftAxis != null) {
               Vec3 belt = beltMotionAtShaftTop(anchor.getBlockState(), shaftAxis, rpm);
               Vec3 spanDir = spanTraversalDirection(level, path, partialTickForRender);
               if (!(belt.lengthSqr() < 1.0E-12) && spanDir != null) {
                  double alignment = belt.dot(spanDir);
                  return Math.abs(alignment) < 1.0E-4 ? magnitude * (double)Math.signum(rpm) : magnitude * Math.signum(alignment);
               } else {
                  return magnitude * (double)Math.signum(rpm);
               }
            } else {
               return magnitude * (double)Math.signum(rpm);
            }
         }
      }
   }

   private static Vec3 positiveAxisUnit(Axis axis) {
      return switch (axis) {
         case X -> new Vec3(1.0, 0.0, 0.0);
         case Y -> new Vec3(0.0, 1.0, 0.0);
         case Z -> new Vec3(0.0, 0.0, 1.0);
         default -> throw new MatchException(null, null);
      };
   }

   private static Vec3 crownRadial(Vec3 axisUnit, BlockState anchorState) {
      Vec3 upRef = new Vec3(0.0, 1.0, 0.0);
      if (Math.abs(axisUnit.dot(upRef)) > 0.9999) {
         Direction facing = (Direction)anchorState.getValue(CoasterAnchorpointBlock.FACING);
         upRef = Vec3.atLowerCornerOf(facing.getNormal());
      }

      Vec3 radial = upRef.subtract(axisUnit.scale(upRef.dot(axisUnit)));
      if (radial.lengthSqr() < 1.0E-12) {
         Direction facing = (Direction)anchorState.getValue(CoasterAnchorpointBlock.FACING);
         Vec3 face = Vec3.atLowerCornerOf(facing.getNormal());
         radial = face.subtract(axisUnit.scale(face.dot(axisUnit)));
      }

      return radial.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : radial.normalize();
   }
}
