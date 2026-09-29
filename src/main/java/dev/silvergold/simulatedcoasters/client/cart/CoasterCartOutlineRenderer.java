/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.createmod.catnip.data.Pair
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 */
package dev.silvergold.simulatedcoasters.client.cart;

import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartOutlineRenderer {
    public static final float OUTLINE_LINE_WIDTH = 0.03125f;
    public static final int EDGE_COUNT = 12;
    private static final double[][] UNIT_CUBE_EDGES = new double[][]{{0.0, 0.0, 0.0, 1.0, 0.0, 0.0}, {0.0, 0.0, 0.0, 0.0, 0.0, 1.0}, {1.0, 0.0, 0.0, 1.0, 0.0, 1.0}, {0.0, 0.0, 1.0, 1.0, 0.0, 1.0}, {0.0, 1.0, 0.0, 1.0, 1.0, 0.0}, {0.0, 1.0, 0.0, 0.0, 1.0, 1.0}, {1.0, 1.0, 0.0, 1.0, 1.0, 1.0}, {0.0, 1.0, 1.0, 1.0, 1.0, 1.0}, {0.0, 0.0, 0.0, 0.0, 1.0, 0.0}, {1.0, 0.0, 0.0, 1.0, 1.0, 0.0}, {0.0, 0.0, 1.0, 0.0, 1.0, 1.0}, {1.0, 0.0, 1.0, 1.0, 1.0, 1.0}};

    private CoasterCartOutlineRenderer() {
    }

    public static void showRotatedUnitCubeOutline(Outliner outliner, String keyPrefix, Vec3 plotOriginWorld, Quaterniondc orientation, int color) {
        Vector3d scratch = new Vector3d();
        for (int i = 0; i < 12; ++i) {
            double[] e = UNIT_CUBE_EDGES[i];
            Vec3 start = CoasterCartOutlineRenderer.plotLocalToWorld(plotOriginWorld, orientation, e[0], e[1], e[2], scratch);
            Vec3 end = CoasterCartOutlineRenderer.plotLocalToWorld(plotOriginWorld, orientation, e[3], e[4], e[5], scratch);
            outliner.showLine((Object)Pair.of((Object)keyPrefix, (Object)i), start, end).colored(color).lineWidth(0.03125f);
        }
    }

    public static void clear(Outliner outliner, String keyPrefix) {
        for (int i = 0; i < 12; ++i) {
            outliner.remove((Object)Pair.of((Object)keyPrefix, (Object)i));
        }
    }

    private static Vec3 plotLocalToWorld(Vec3 plotOriginWorld, Quaterniondc orientation, double lx, double ly, double lz, Vector3d scratch) {
        scratch.set(lx, ly, lz);
        orientation.transform(scratch);
        return new Vec3(plotOriginWorld.x + scratch.x, plotOriginWorld.y + scratch.y, plotOriginWorld.z + scratch.z);
    }
}

