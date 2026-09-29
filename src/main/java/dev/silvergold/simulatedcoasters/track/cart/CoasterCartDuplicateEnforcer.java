/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor
 *  dev.ryanhcode.sable.sublevel.plot.LevelPlot
 *  dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBearingTickCache;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlotScan;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

final class CoasterCartDuplicateEnforcer {
    private CoasterCartDuplicateEnforcer() {
    }

    static boolean enforceSingleCart(ServerSubLevel sub) {
        ServerLevelPlot plot = sub.getPlot();
        Block bearingBlock = (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
        CoasterCartPlotScan.BearingScanResult scan = CoasterCartBearingTickCache.scan(sub);
        BlockPos kept = scan.representative();
        if (kept == null) {
            return false;
        }
        if (scan.bearingCount() <= 1) {
            return false;
        }
        BlockPos plotOrigin = plot.getCenterBlock();
        EmbeddedPlotLevelAccessor embedded = plot.getEmbeddedLevelAccessor();
        boolean[] removed = new boolean[]{false};
        CoasterCartPlotScan.forEachBearingWorldCell((LevelPlot)plot, bearingBlock, mut -> {
            if (kept.equals(mut)) {
                return;
            }
            BlockState state = sub.getLevel().getBlockState((BlockPos)mut);
            if (!state.is(bearingBlock)) {
                return;
            }
            BlockPos local = mut.immutable().subtract((Vec3i)plotOrigin);
            if (embedded.destroyBlock(local, true, null, 3)) {
                removed[0] = true;
            }
        });
        return removed[0];
    }

    static void breakCartBlocksOnWorldGridAfterMove(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> movedSources) {
        Block bearingBlock = (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
        for (BlockPos src : movedSources) {
            BlockPos dst;
            BlockState state;
            if (src == null || !(state = level.getBlockState(dst = transform.apply(src))).is(bearingBlock) || CoasterCartDuplicateEnforcer.shouldRetainCartBlockAt(level, dst, bearingBlock)) continue;
            level.destroyBlock(dst, true, null, 3);
        }
    }

    private static boolean shouldRetainCartBlockAt(ServerLevel level, BlockPos levelPos, Block bearingBlock) {
        BlockPos blockPos;
        SubLevel sub = Sable.HELPER.getContaining((Level)level, (Vec3i)levelPos);
        if (sub == null || !CoasterCartResolve.isCartSubLevel(sub)) {
            return false;
        }
        if (sub instanceof ServerSubLevel) {
            ServerSubLevel server = (ServerSubLevel)sub;
            blockPos = CoasterCartBearingTickCache.scan(server).representative();
        } else {
            blockPos = CoasterCartPlotScan.representativeBearingPlotPos(sub.getPlot(), bearingBlock);
        }
        BlockPos kept = blockPos;
        return kept != null && kept.equals((Object)levelPos);
    }
}

