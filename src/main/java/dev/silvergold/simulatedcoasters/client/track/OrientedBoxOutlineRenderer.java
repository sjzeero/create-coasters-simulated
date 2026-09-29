package dev.silvergold.simulatedcoasters.client.track;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class OrientedBoxOutlineRenderer {
   public static final int EDGE_COUNT = 12;
   public static final int END_CAP_EDGE_COUNT = 8;
   public static final float LINE_WIDTH = 0.0625F;
   private static final int[][] EDGES = new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}, {4, 5}, {4, 6}, {5, 7}, {6, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

   private OrientedBoxOutlineRenderer() {
   }

   public static void showEndCaps(
      Outliner outliner,
      String keyPrefix,
      Vec3 center,
      Vec3 axisRight,
      Vec3 axisUp,
      Vec3 axisForward,
      double halfRight,
      double halfUp,
      double halfForward,
      int color
   ) {
      Vec3[] corners = computeCorners(center, axisRight, axisUp, axisForward, halfRight, halfUp, halfForward);
      if (corners != null) {
         for (int i = 0; i < 8; i++) {
            int[] e = EDGES[i];
            outliner.showLine(Pair.of(keyPrefix, i), corners[e[0]], corners[e[1]]).colored(color).lineWidth(0.0625F);
         }
      }
   }

   public static void show(
      Outliner outliner,
      String keyPrefix,
      Vec3 center,
      Vec3 axisRight,
      Vec3 axisUp,
      Vec3 axisForward,
      double halfRight,
      double halfUp,
      double halfForward,
      int color
   ) {
      Vec3[] corners = computeCorners(center, axisRight, axisUp, axisForward, halfRight, halfUp, halfForward);
      if (corners != null) {
         for (int i = 0; i < 12; i++) {
            int[] e = EDGES[i];
            outliner.showLine(Pair.of(keyPrefix, i), corners[e[0]], corners[e[1]]).colored(color).lineWidth(0.0625F);
         }
      }
   }

   public static void clear(Outliner outliner, String keyPrefix) {
      for (int i = 0; i < 12; i++) {
         outliner.remove(Pair.of(keyPrefix, i));
      }
   }

   public static void clearEndCaps(Outliner outliner, String keyPrefix) {
      for (int i = 0; i < 12; i++) {
         outliner.remove(Pair.of(keyPrefix, i));
      }
   }

   @Nullable
   private static Vec3[] computeCorners(Vec3 center, Vec3 axisRight, Vec3 axisUp, Vec3 axisForward, double halfRight, double halfUp, double halfForward) {
      if (!(axisRight.lengthSqr() < 1.0E-14) && !(axisUp.lengthSqr() < 1.0E-14) && !(axisForward.lengthSqr() < 1.0E-14)) {
         Vec3[] corners = new Vec3[8];

         for (int sx = 0; sx < 2; sx++) {
            for (int sy = 0; sy < 2; sy++) {
               for (int sz = 0; sz < 2; sz++) {
                  double fx = sx == 0 ? -1.0 : 1.0;
                  double fy = sy == 0 ? -1.0 : 1.0;
                  double fz = sz == 0 ? -1.0 : 1.0;
                  int idx = sx | sy << 1 | sz << 2;
                  corners[idx] = center.add(axisRight.scale(fx * halfRight)).add(axisUp.scale(fy * halfUp)).add(axisForward.scale(fz * halfForward));
               }
            }
         }

         return corners;
      } else {
         return null;
      }
   }
}
