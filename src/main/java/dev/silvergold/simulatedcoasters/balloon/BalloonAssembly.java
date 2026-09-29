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
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostIndex;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostKey;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import dev.silvergold.simulatedcoasters.balloon.BalloonTetherController;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class BalloonAssembly {
    private BalloonAssembly() {
    }

    public static void onHostBlocksMoved(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> movedSources) {
        BalloonAssembly.onHostBlocksMoved(level, transform, movedSources, null);
    }

    public static void onHostBlocksMoved(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> movedSources, @Nullable UUID sourceSubLevelId) {
        ServerLevel resultingLevel = transform.getLevel();
        HashSet<BlockPos> movedLattice = new HashSet<BlockPos>();
        for (BlockPos oldCell : movedSources) {
            if (oldCell == null) continue;
            movedLattice.add(oldCell);
        }
        if (movedLattice.isEmpty()) {
            return;
        }
        BalloonAssembly.rebindBalloonsOnMovedLattice(resultingLevel, transform, movedLattice);
        if (sourceSubLevelId != null) {
            BalloonHostIndex index = BalloonHostIndex.get(level);
            for (Map.Entry<BalloonHostKey, UUID> entry : new ArrayList<Map.Entry<BalloonHostKey, UUID>>(index.entries())) {
                BlockPos lattice;
                BalloonBlockEntity be;
                ServerSubLevel balloon;
                if (!entry.getKey().hostSubLevelId().equals(sourceSubLevelId) || (balloon = BalloonResolve.findBalloonById(resultingLevel, entry.getValue())) == null || (be = BalloonAssembly.balloonBlockEntity(balloon)) == null || (lattice = be.hostLatticePos()) == null || !movedLattice.contains(lattice)) continue;
                BalloonAssembly.rebindBalloonOnHostLattice(resultingLevel, balloon, be, transform.apply(lattice));
            }
        }
        BalloonAssembly.refreshAllBalloonHosts(resultingLevel);
    }

    private static void rebindBalloonsOnMovedLattice(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Set<BlockPos> movedLattice) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        for (SubLevel sub : container.getAllSubLevels()) {
            BlockPos lattice;
            BalloonBlockEntity be;
            ServerSubLevel balloonSub;
            if (!(sub instanceof ServerSubLevel) || (balloonSub = (ServerSubLevel)sub).isRemoved() || !BalloonResolve.isBalloonSubLevel((SubLevel)balloonSub) || (be = BalloonAssembly.balloonBlockEntity(balloonSub)) == null || (lattice = be.hostLatticePos()) == null || !movedLattice.contains(lattice)) continue;
            BalloonAssembly.rebindBalloonOnHostLattice(level, balloonSub, be, transform.apply(lattice));
        }
    }

    public static void refreshAllBalloonHosts(ServerLevel level) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        for (SubLevel sub : container.getAllSubLevels()) {
            BalloonHostKey atHost;
            BalloonBlockEntity be;
            ServerSubLevel balloonSub;
            if (!(sub instanceof ServerSubLevel) || (balloonSub = (ServerSubLevel)sub).isRemoved() || !BalloonResolve.isBalloonSubLevel((SubLevel)balloonSub) || (be = BalloonAssembly.balloonBlockEntity(balloonSub)) == null || (atHost = BalloonHostKey.relocateHostKey(level, be, balloonSub)) == null) continue;
            if (!atHost.equals(be.hostKey())) {
                BalloonAssembly.rebindBalloon(level, balloonSub, be, atHost);
                continue;
            }
            be.refreshHostLattice(level, atHost);
            BalloonTetherController.register(balloonSub, be);
        }
    }

    private static void rebindBalloonOnHostLattice(ServerLevel level, UUID balloonId, BlockPos newLatticeCell) {
        ServerSubLevel balloon = BalloonResolve.findBalloonById(level, balloonId);
        if (balloon == null) {
            return;
        }
        BalloonBlockEntity be = BalloonAssembly.balloonBlockEntity(balloon);
        if (be == null) {
            return;
        }
        BalloonAssembly.rebindBalloonOnHostLattice(level, balloon, be, newLatticeCell);
    }

    private static void rebindBalloonOnHostLattice(ServerLevel level, ServerSubLevel balloon, BalloonBlockEntity be, BlockPos newLatticeCell) {
        BalloonHostKey newHost = BalloonHostKey.of((Level)level, newLatticeCell);
        if (newHost == null) {
            return;
        }
        BalloonAssembly.rebindBalloon(level, balloon, be, newHost);
    }

    private static void rebindBalloon(ServerLevel level, ServerSubLevel balloonSub, BalloonBlockEntity be, BalloonHostKey newHost) {
        BalloonHostKey oldHost = be.hostKey();
        if (oldHost.equals(newHost)) {
            be.refreshHostLattice(level, newHost);
            BalloonTetherController.register(balloonSub, be);
            return;
        }
        BalloonHostIndex.get(level).rebindHost(oldHost, newHost, balloonSub.getUniqueId());
        be.rebindHost(newHost);
        BalloonTetherController.register(balloonSub, be);
    }

    @Nullable
    private static BalloonBlockEntity balloonBlockEntity(ServerSubLevel balloonSub) {
        EmbeddedPlotLevelAccessor embedded = balloonSub.getPlot().getEmbeddedLevelAccessor();
        BlockEntity blockEntity = embedded.getBlockEntity(BlockPos.ZERO);
        if (blockEntity instanceof BalloonBlockEntity) {
            BalloonBlockEntity be = (BalloonBlockEntity)blockEntity;
            return be;
        }
        return null;
    }
}

