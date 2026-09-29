/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent
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
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.rivet.RivetBlock;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.rivet.RivetHostKey;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlotScan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class RivetDisassemblyEnforcer {
    private RivetDisassemblyEnforcer() {
    }

    public static void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        ServerLevel level = event.getPhysicsSystem().getLevel();
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        for (ServerSubLevel sub : container.getAllSubLevels()) {
            if (sub.isRemoved()) continue;
            RivetDisassemblyEnforcer.enforceSingleRivet(sub);
            RivetDisassemblyEnforcer.popIfSelfHosted(sub);
        }
    }

    private static void popIfSelfHosted(ServerSubLevel sub) {
        if (!RivetHostKey.isRivetSubLevel((SubLevel)sub)) {
            return;
        }
        ServerLevel serverLevel = sub.getLevel();
        if (!(serverLevel instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = serverLevel;
        BlockEntity blockEntity = sub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO);
        if (!(blockEntity instanceof RivetBlockEntity)) {
            return;
        }
        RivetBlockEntity be = (RivetBlockEntity)blockEntity;
        if (level.getBlockState(be.hostPos()).isAir() || !be.hasInvalidHost(level, sub)) {
            return;
        }
        RivetBlockEntity.scheduleTeardownIfInvalidHost(level, sub.getUniqueId());
    }

    public static boolean enforceSingleRivet(ServerSubLevel sub) {
        Block rivet = (Block)SimulatedCoastersBlocks.RIVET.get();
        ServerLevelPlot plot = sub.getPlot();
        CoasterCartPlotScan.BearingScanResult scan = CoasterCartPlotScan.scanBearingCells((LevelPlot)plot, rivet);
        if (scan.bearingCount() <= 1) {
            return false;
        }
        BlockPos kept = RivetDisassemblyEnforcer.keptRivetWorldCell((LevelPlot)plot, rivet, scan.representative());
        if (kept == null) {
            return false;
        }
        BlockPos plotOrigin = plot.getCenterBlock();
        EmbeddedPlotLevelAccessor embedded = plot.getEmbeddedLevelAccessor();
        boolean[] removed = new boolean[]{false};
        CoasterCartPlotScan.forEachBearingWorldCell((LevelPlot)plot, rivet, mut -> {
            if (kept.equals(mut)) {
                return;
            }
            BlockState state = sub.getLevel().getBlockState((BlockPos)mut);
            if (!state.is(rivet)) {
                return;
            }
            BlockPos local = mut.immutable().subtract((Vec3i)plotOrigin);
            if (embedded.destroyBlock(local, true, null, 3)) {
                removed[0] = true;
            }
        });
        return removed[0];
    }

    public static void cleanupRivetBlocksAfterMove(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> movedSources) {
        RivetBlock rivet = (RivetBlock)((Object)SimulatedCoastersBlocks.RIVET.get());
        for (BlockPos src : movedSources) {
            BlockPos dst;
            BlockState state;
            if (src == null || !(state = level.getBlockState(dst = transform.apply(src))).is((Block)rivet) || RivetDisassemblyEnforcer.shouldRetainRivetBlockAt(level, dst, (Block)rivet)) continue;
            level.destroyBlock(dst, true, null, 3);
        }
    }

    private static boolean shouldRetainRivetBlockAt(ServerLevel level, BlockPos levelPos, Block rivet) {
        ServerSubLevel ssl;
        SubLevel sub = Sable.HELPER.getContaining((Level)level, (Vec3i)levelPos);
        if (!(sub instanceof ServerSubLevel) || (ssl = (ServerSubLevel)sub).isRemoved()) {
            return false;
        }
        CoasterCartPlotScan.BearingScanResult scan = CoasterCartPlotScan.scanBearingCells((LevelPlot)ssl.getPlot(), rivet);
        BlockPos kept = RivetDisassemblyEnforcer.keptRivetWorldCell((LevelPlot)ssl.getPlot(), rivet, scan.representative());
        return kept != null && kept.equals((Object)levelPos);
    }

    @Nullable
    private static BlockPos keptRivetWorldCell(LevelPlot plot, Block rivet, @Nullable BlockPos scanRepresentative) {
        if (plot.getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO).is(rivet)) {
            return plot.getCenterBlock();
        }
        return scanRepresentative;
    }
}

