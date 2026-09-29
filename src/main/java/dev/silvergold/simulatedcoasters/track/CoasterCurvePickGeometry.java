/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.phys.shapes.VoxelShape
 */
package dev.silvergold.simulatedcoasters.track;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CoasterCurvePickGeometry {
    private static final VoxelShape CURVE_PICK_CROSS_SECTION = Block.box((double)-1.0, (double)0.0, (double)-1.0, (double)17.0, (double)16.0, (double)17.0);

    private CoasterCurvePickGeometry() {
    }

    public static VoxelShape curvePickCrossSection() {
        return CURVE_PICK_CROSS_SECTION;
    }

    public static float curvePickCrossSectionLocalOriginY() {
        return (float)(CoasterCurvePickGeometry.curvePickCrossSection().bounds().getYsize() * -0.5);
    }
}

