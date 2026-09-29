/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.decoration.girder.GirderBlock
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.ticks.LevelTickAccess
 */
package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.content.decoration.girder.GirderBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.ticks.LevelTickAccess;

public final class CoasterAnchorGirderConnection {
    private CoasterAnchorGirderConnection() {
    }

    public static Direction mountFace(BlockState anchorState) {
        return ((Direction)anchorState.getValue((Property)CoasterAnchorpointBlock.FACING)).getOpposite();
    }

    public static boolean acceptsGirderConnection(BlockState neighborState, Direction towardNeighbor) {
        if (!(neighborState.getBlock() instanceof CoasterAnchorpointBlock)) {
            return false;
        }
        return neighborState.getValue((Property)CoasterAnchorpointBlock.FACING) == towardNeighbor;
    }

    public static void scheduleAdjacentGirderUpdates(Level level, BlockPos pos, LevelTickAccess<Block> blockTicks) {
        for (Direction d : Direction.values()) {
            GirderBlock girder;
            BlockPos n = pos.relative(d);
            BlockState ns = level.getBlockState(n);
            Block block = ns.getBlock();
            if (!(block instanceof GirderBlock) || blockTicks.hasScheduledTick(n, (Object)(girder = (GirderBlock)block))) continue;
            level.scheduleTick(n, (Block)girder, 1);
        }
    }
}

