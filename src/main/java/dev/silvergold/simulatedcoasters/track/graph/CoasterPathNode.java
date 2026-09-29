/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.phys.Vec3
 */
package dev.silvergold.simulatedcoasters.track.graph;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record CoasterPathNode(BlockPos pos, Vec3 normal) {
}

