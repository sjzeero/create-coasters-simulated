/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper
 *  dev.ryanhcode.sable.companion.math.BoundingBox3i
 *  dev.ryanhcode.sable.companion.math.BoundingBox3ic
 *  dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.plot.LevelPlot
 *  dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder
 *  it.unimi.dsi.fastutil.objects.ObjectArrayList
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.rivet.RivetBlock;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class RivetDisassembly {
    private RivetDisassembly() {
    }

    @Nullable
    public static ServerSubLevel splitAttachedBlocks(ServerLevel level, ServerSubLevel rivetSub) {
        if (rivetSub.isRemoved()) {
            return null;
        }
        ObjectArrayList<BlockPos> blocks = RivetDisassembly.collectAttachedBlocks(level, (LevelPlot)rivetSub.getPlot());
        if (blocks.isEmpty()) {
            return null;
        }
        if (RivetDisassembly.totalMass(level, blocks) <= 0.0) {
            SimulatedCoasters.LOGGER.debug("Rivet {} attachments are weightless \u2014 popping {} block(s) instead of assembling", (Object)rivetSub.getUniqueId(), (Object)blocks.size());
            RivetDisassembly.popAttachedBlocks(level, blocks);
            return null;
        }
        BoundingBox3i bounds = BoundingBox3i.from(blocks);
        if (bounds == null) {
            return null;
        }
        return SubLevelAssemblyHelper.assembleBlocks((ServerLevel)level, (BlockPos)((BlockPos)blocks.getFirst()), blocks, (BoundingBox3ic)bounds);
    }

    private static double totalMass(ServerLevel level, ObjectArrayList<BlockPos> blocks) {
        double mass = 0.0;
        for (BlockPos pos : blocks) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            mass += PhysicsBlockPropertyHelper.getMass((BlockGetter)level, (BlockPos)pos, (BlockState)state);
        }
        return mass;
    }

    private static void popAttachedBlocks(ServerLevel level, ObjectArrayList<BlockPos> blocks) {
        for (BlockPos pos : blocks) {
            if (level.getBlockState(pos).isAir()) continue;
            level.destroyBlock(pos, true);
        }
    }

    private static ObjectArrayList<BlockPos> collectAttachedBlocks(ServerLevel level, LevelPlot plot) {
        ObjectArrayList blocks = new ObjectArrayList();
        RivetBlock rivet = (RivetBlock)((Object)SimulatedCoastersBlocks.RIVET.get());
        BlockPos rivetCell = plot.getCenterBlock();
        for (PlotChunkHolder chunk : plot.getLoadedChunks()) {
            BoundingBox3ic localChunkBounds = chunk.getBoundingBox();
            if (localChunkBounds == null || localChunkBounds == BoundingBox3i.EMPTY) continue;
            for (int x = localChunkBounds.minX(); x <= localChunkBounds.maxX(); ++x) {
                for (int y = localChunkBounds.minY(); y <= localChunkBounds.maxY(); ++y) {
                    for (int z = localChunkBounds.minZ(); z <= localChunkBounds.maxZ(); ++z) {
                        BlockState state;
                        BlockPos pos = new BlockPos(x + chunk.getPos().getMinBlockX(), y, z + chunk.getPos().getMinBlockZ());
                        if (pos.equals((Object)rivetCell) || (state = level.getBlockState(pos)).isAir() || state.is((Block)rivet)) continue;
                        blocks.add((Object)pos.immutable());
                    }
                }
            }
        }
        return blocks;
    }
}

