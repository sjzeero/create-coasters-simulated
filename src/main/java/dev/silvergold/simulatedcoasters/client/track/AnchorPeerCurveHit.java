/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class AnchorPeerCurveHit {
    private static boolean active;
    private static BezierConnection curve;
    private static Vec3 vec;
    private static Vec3 angles;
    private static Vec3 direction;
    private static BlockPos focusAnchor;
    @Nullable
    private static BlockPos rasterBlockPos;

    private AnchorPeerCurveHit() {
    }

    public static void clear() {
        active = false;
        curve = null;
        vec = null;
        angles = null;
        direction = null;
        focusAnchor = null;
        rasterBlockPos = null;
    }

    public static void set(BezierConnection bc, Vec3 vecIn, Vec3 anglesIn, Vec3 directionIn, BlockPos focusAnchorIn, @Nullable BlockPos rasterBlockPosIn) {
        active = true;
        curve = bc;
        vec = vecIn;
        angles = anglesIn;
        direction = directionIn;
        focusAnchor = focusAnchorIn.immutable();
        rasterBlockPos = rasterBlockPosIn == null ? null : rasterBlockPosIn.immutable();
    }

    public static boolean isActive() {
        return active;
    }

    public static Vec3 vec() {
        return vec;
    }

    public static Vec3 angles() {
        return angles;
    }

    public static Vec3 direction() {
        return direction;
    }

    public static BezierConnection curve() {
        return curve;
    }

    public static BlockPos focusAnchor() {
        return focusAnchor;
    }

    @Nullable
    public static BlockPos rasterBlockPos() {
        return rasterBlockPos;
    }
}

