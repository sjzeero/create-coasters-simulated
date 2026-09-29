package dev.silvergold.simulatedcoasters.client.track;

import net.minecraft.core.BlockPos;

public record AnchorPeerCurveMineTarget(BlockPos mined, BlockPos peer, boolean wrench) {
}
