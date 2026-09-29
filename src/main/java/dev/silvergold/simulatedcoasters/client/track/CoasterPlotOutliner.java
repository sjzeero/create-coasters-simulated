package dev.silvergold.simulatedcoasters.client.track;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CoasterPlotOutliner {
   public static final float LINE_WIDTH = 0.0625F;
   private static final double SHAFT_CAP_AXIS_HALF_DEPTH = 0.00390625;

   private CoasterPlotOutliner() {
   }

   public static void showShaftEndCaps(
      Outliner outliner, String keyPrefix, Vec3 plotCenter, Vec3 plotAxis, double halfLengthAlongAxis, double halfCross, int color
   ) {
      Vec3 axis = plotAxis.normalize();
      Vec3 plus = plotCenter.add(axis.scale(halfLengthAlongAxis));
      Vec3 minus = plotCenter.subtract(axis.scale(halfLengthAlongAxis));
      outliner.showAABB(Pair.of(keyPrefix, 0), endCapAabb(plus, axis, halfCross, 0.00390625)).colored(color).lineWidth(0.0625F);
      outliner.showAABB(Pair.of(keyPrefix, 1), endCapAabb(minus, axis, halfCross, 0.00390625)).colored(color).lineWidth(0.0625F);
   }

   public static void clearShaftEndCaps(Outliner outliner, String keyPrefix) {
      outliner.remove(Pair.of(keyPrefix, 0));
      outliner.remove(Pair.of(keyPrefix, 1));
   }

   private static AABB endCapAabb(Vec3 center, Vec3 axis, double halfCross, double halfDepthAlongAxis) {
      double ax = Math.abs(axis.x);
      double ay = Math.abs(axis.y);
      double az = Math.abs(axis.z);
      if (ax >= ay && ax >= az) {
         return new AABB(
            center.x - halfDepthAlongAxis,
            center.y - halfCross,
            center.z - halfCross,
            center.x + halfDepthAlongAxis,
            center.y + halfCross,
            center.z + halfCross
         );
      } else {
         return ay >= az
            ? new AABB(
               center.x - halfCross,
               center.y - halfDepthAlongAxis,
               center.z - halfCross,
               center.x + halfCross,
               center.y + halfDepthAlongAxis,
               center.z + halfCross
            )
            : new AABB(
               center.x - halfCross,
               center.y - halfCross,
               center.z - halfDepthAlongAxis,
               center.x + halfCross,
               center.y + halfCross,
               center.z + halfDepthAlongAxis
            );
      }
   }
}
