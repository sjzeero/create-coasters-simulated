package dev.silvergold.simulatedcoasters.track.graph;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record CoasterPathNode(BlockPos pos, Vec3 normal) {
}
