/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.createmod.catnip.data.Pair
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.track;

import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class SplineOrientedBoxOutlineRenderer {
    public static final int EDGE_COUNT = 12;
    public static final float LINE_WIDTH = 0.0625f;
    private static final double HALF_LENGTH = 0.375;
    private static final double HALF_WIDTH = 0.22;
    private static final double HALF_HEIGHT = 0.125;
    private static final int[][] EDGES = new int[][]{{0, 1}, {0, 2}, {1, 3}, {2, 3}, {4, 5}, {4, 6}, {5, 7}, {6, 7}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

    private SplineOrientedBoxOutlineRenderer() {
    }

    public static void show(Outliner outliner, String keyPrefix, Vec3 center, CoasterPathTrackFrame.TrackBasis basis, int color) {
        Vec3 right = basis.right();
        Vec3 up = basis.up();
        Vec3 forward = basis.forward();
        if (right.lengthSqr() < 1.0E-14 || up.lengthSqr() < 1.0E-14 || forward.lengthSqr() < 1.0E-14) {
            return;
        }
        Vec3[] corners = new Vec3[8];
        for (int sx = 0; sx < 2; ++sx) {
            for (int sy = 0; sy < 2; ++sy) {
                for (int sz = 0; sz < 2; ++sz) {
                    double fx = sx == 0 ? -1.0 : 1.0;
                    double fy = sy == 0 ? -1.0 : 1.0;
                    double fz = sz == 0 ? -1.0 : 1.0;
                    int idx = sx | sy << 1 | sz << 2;
                    corners[idx] = center.add(right.scale(fx * 0.22)).add(up.scale(fy * 0.125)).add(forward.scale(fz * 0.375));
                }
            }
        }
        for (int i = 0; i < 12; ++i) {
            int[] e = EDGES[i];
            outliner.showLine((Object)Pair.of((Object)keyPrefix, (Object)i), corners[e[0]], corners[e[1]]).colored(color).lineWidth(0.0625f);
        }
    }

    public static void clear(Outliner outliner, String keyPrefix) {
        for (int i = 0; i < 12; ++i) {
            outliner.remove((Object)Pair.of((Object)keyPrefix, (Object)i));
        }
    }
}

