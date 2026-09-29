/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record CoasterBezierEndpointData(Vec3 start, Vec3 axis, Vec3 normal) {
    @Nullable
    public static CoasterBezierEndpointData at(BezierConnection bc, BlockPos endpoint) {
        if (endpoint.equals(bc.bePositions.getFirst())) {
            return new CoasterBezierEndpointData((Vec3)bc.starts.getFirst(), ((Vec3)bc.axes.getFirst()).normalize(), ((Vec3)bc.normals.getFirst()).normalize());
        }
        if (endpoint.equals(bc.bePositions.getSecond())) {
            return new CoasterBezierEndpointData((Vec3)bc.starts.getSecond(), ((Vec3)bc.axes.getSecond()).normalize(), ((Vec3)bc.normals.getSecond()).normalize());
        }
        return null;
    }
}

