package dev.silvergold.simulatedcoasters.track;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CoasterCurvePickGeometry {
   private static final VoxelShape CURVE_PICK_CROSS_SECTION = Block.box(-1.0, 0.0, -1.0, 17.0, 16.0, 17.0);

   private CoasterCurvePickGeometry() {
   }

   public static VoxelShape curvePickCrossSection() {
      return CURVE_PICK_CROSS_SECTION;
   }

   public static float curvePickCrossSectionLocalOriginY() {
      return (float)(curvePickCrossSection().bounds().getYsize() * -0.5);
   }
}
