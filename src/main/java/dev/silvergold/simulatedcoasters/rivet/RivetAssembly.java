/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.rivet.RivetAnchorKey;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.rivet.RivetHostIndex;
import dev.silvergold.simulatedcoasters.rivet.RivetHostKey;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class RivetAssembly {
    private RivetAssembly() {
    }

    public static void onHostBlocksMoved(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> movedSources, @Nullable UUID sourceSubLevelId) {
        RivetHostIndex index = RivetHostIndex.get(level);
        ServerLevel resultingLevel = transform.getLevel();
        for (BlockPos oldCell : movedSources) {
            if (oldCell == null) continue;
            BlockPos newCell = transform.apply(oldCell);
            RivetHostKey newHostKey = RivetHostKey.of((Level)resultingLevel, newCell);
            RivetAssembly.rebindAnchorsOnHostCell(index, level, oldCell, newHostKey, transform);
        }
        RivetAssembly.refreshAllRivetHostLocks(level);
    }

    private static void rebindAnchorsOnHostCell(RivetHostIndex index, ServerLevel level, BlockPos oldCell, RivetHostKey newHostKey, SubLevelAssemblyHelper.AssemblyTransform transform) {
        ArrayList<RivetAnchorKey> anchors = new ArrayList<RivetAnchorKey>(index.anchorsOnHostCell(oldCell));
        for (RivetAnchorKey anchor : anchors) {
            RivetAssembly.rebindAnchor(index, level, anchor, newHostKey, transform);
        }
    }

    private static void rebindAnchor(RivetHostIndex index, ServerLevel level, RivetAnchorKey anchor, RivetHostKey newHostKey, SubLevelAssemblyHelper.AssemblyTransform transform) {
        UUID rivetId = index.removeAnchor(anchor);
        if (rivetId == null) {
            return;
        }
        Direction newFace = transform.getRotation().rotate(anchor.face());
        RivetPlacement.Attachment attachment = RivetPlacement.rotateAttachment(anchor.face(), new RivetPlacement.Attachment(anchor.lx(), anchor.ly(), anchor.lz()), transform);
        RivetAnchorKey newAnchor = anchor.withHost(newHostKey).withFaceAndAttachment(newFace, attachment);
        index.register(newAnchor, rivetId);
        ServerSubLevel rivetSub = RivetAssembly.findRivetSubLevel(level, rivetId);
        if (rivetSub == null) {
            return;
        }
        RivetBlockEntity be = RivetAssembly.rivetBlockEntity(rivetSub);
        if (be == null) {
            return;
        }
        be.rebindHost(newHostKey, newFace, attachment);
        be.applyHostLock(rivetSub);
    }

    @Nullable
    private static ServerSubLevel findRivetSubLevel(ServerLevel level, UUID rivetId) {
        ServerSubLevel ssl;
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        SubLevel sub = container.getSubLevel(rivetId);
        if (sub instanceof ServerSubLevel && !(ssl = (ServerSubLevel)sub).isRemoved()) {
            return ssl;
        }
        return null;
    }

    @Nullable
    private static RivetBlockEntity rivetBlockEntity(ServerSubLevel rivetSub) {
        EmbeddedPlotLevelAccessor embedded = rivetSub.getPlot().getEmbeddedLevelAccessor();
        BlockEntity blockEntity = embedded.getBlockEntity(BlockPos.ZERO);
        if (blockEntity instanceof RivetBlockEntity) {
            RivetBlockEntity be = (RivetBlockEntity)blockEntity;
            return be;
        }
        return null;
    }

    public static void refreshAllRivetHostLocks(ServerLevel level) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        for (SubLevel sub : container.getAllSubLevels()) {
            RivetBlockEntity be;
            ServerSubLevel rivetSub;
            if (!(sub instanceof ServerSubLevel) || (rivetSub = (ServerSubLevel)sub).isRemoved() || !rivetSub.getPlot().getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO).is((Block)SimulatedCoastersBlocks.RIVET.get()) || (be = RivetAssembly.rivetBlockEntity(rivetSub)) == null) continue;
            be.applyHostLock(rivetSub);
        }
    }
}

