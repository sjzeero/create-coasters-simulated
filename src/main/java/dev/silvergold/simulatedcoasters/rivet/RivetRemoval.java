/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.rivet.RivetAssembly;
import dev.silvergold.simulatedcoasters.rivet.RivetAssemblyMoveContext;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.rivet.RivetDisassembly;
import dev.silvergold.simulatedcoasters.rivet.RivetDrop;
import dev.silvergold.simulatedcoasters.rivet.RivetHostIndex;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class RivetRemoval {
    private static final Set<UUID> REMOVING = ConcurrentHashMap.newKeySet();

    private RivetRemoval() {
    }

    public static boolean isRemoving(UUID rivetSubLevelId) {
        return REMOVING.contains(rivetSubLevelId);
    }

    public static void onHostBroken(ServerLevel level, ServerSubLevel rivetSub) {
        if (rivetSub.isRemoved()) {
            return;
        }
        if (!REMOVING.add(rivetSub.getUniqueId())) {
            return;
        }
        try {
            RivetRemoval.detachAndRemove(level, rivetSub, true);
        }
        finally {
            REMOVING.remove(rivetSub.getUniqueId());
        }
    }

    public static void onRivetBlockRemovedDuringMove(ServerLevel level, ServerSubLevel rivetSub) {
        if (rivetSub.isRemoved() || RivetRemoval.isRemoving(rivetSub.getUniqueId())) {
            return;
        }
        RivetBlockEntity be = RivetRemoval.rivetBlockEntity(rivetSub);
        if (be != null) {
            be.removeWorldLock();
            RivetHostIndex.get(level).unregisterRivet(rivetSub.getUniqueId());
        }
        if (!rivetSub.isRemoved()) {
            rivetSub.markRemoved();
        }
    }

    public static void onRivetBlockRemoved(ServerLevel level, ServerSubLevel rivetSub) {
        if (RivetAssemblyMoveContext.isActive()) {
            RivetRemoval.onRivetBlockRemovedDuringMove(level, rivetSub);
            return;
        }
        if (rivetSub.isRemoved() || RivetRemoval.isRemoving(rivetSub.getUniqueId())) {
            return;
        }
        RivetRemoval.detachAndRemove(level, rivetSub, false);
    }

    private static void detachAndRemove(ServerLevel level, ServerSubLevel rivetSub, boolean breakRivetBlock) {
        BlockPos rivetCell;
        BlockState rivetState;
        if (rivetSub.isRemoved()) {
            return;
        }
        RivetBlockEntity be = RivetRemoval.rivetBlockEntity(rivetSub);
        if (be != null) {
            be.removeWorldLock();
            RivetHostIndex.get(level).unregisterRivet(rivetSub.getUniqueId());
        }
        RivetDisassembly.splitAttachedBlocks(level, rivetSub);
        RivetAssembly.refreshAllRivetHostLocks(level);
        if (breakRivetBlock && (rivetState = level.getBlockState(rivetCell = rivetSub.getPlot().getCenterBlock())).is((Block)SimulatedCoastersBlocks.RIVET.get())) {
            BlockEntity blockEntity = level.getBlockEntity(rivetCell);
            RivetDrop.spawn(level, rivetCell, rivetState, blockEntity, null, ItemStack.EMPTY);
            level.setBlock(rivetCell, Blocks.AIR.defaultBlockState(), 3);
        }
        if (!rivetSub.isRemoved()) {
            rivetSub.markRemoved();
        }
    }

    @Nullable
    private static RivetBlockEntity rivetBlockEntity(ServerSubLevel rivetSub) {
        EmbeddedPlotLevelAccessor embedded = rivetSub.getPlot().getEmbeddedLevelAccessor();
        BlockEntity blockEntity = embedded.getBlockEntity(BlockPos.ZERO);
        if (blockEntity instanceof RivetBlockEntity) {
            RivetBlockEntity rivetBe = (RivetBlockEntity)blockEntity;
            return rivetBe;
        }
        return null;
    }
}

