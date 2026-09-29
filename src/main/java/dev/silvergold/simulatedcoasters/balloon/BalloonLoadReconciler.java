/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostIndex;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostKey;
import dev.silvergold.simulatedcoasters.balloon.BalloonPlacement;
import dev.silvergold.simulatedcoasters.balloon.BalloonPoseAlignment;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import dev.silvergold.simulatedcoasters.balloon.BalloonTetherController;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BalloonLoadReconciler {
    private static final int INITIAL_DELAY_TICKS = 2;
    private static final int RETRY_TICKS = 40;
    private static final Map<ResourceKey<Level>, Integer> PENDING = new HashMap<ResourceKey<Level>, Integer>();

    private BalloonLoadReconciler() {
    }

    public static void schedule(ServerLevel level) {
        PENDING.put((ResourceKey<Level>)level.dimension(), 42);
    }

    public static void clear(ServerLevel level) {
        PENDING.remove(level.dimension());
    }

    public static void serverTick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<ResourceKey<Level>, Integer>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceKey<Level>, Integer> entry = it.next();
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                it.remove();
                continue;
            }
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft > 40) {
                entry.setValue(ticksLeft);
                continue;
            }
            BalloonLoadReconciler.reconcile(level);
            if (ticksLeft <= 0) {
                it.remove();
                continue;
            }
            entry.setValue(ticksLeft);
        }
    }

    public static void reconcile(ServerLevel level) {
        BalloonLoadReconciler.reconcile(level, true);
    }

    public static void reconcile(ServerLevel level, boolean snapPose) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        for (SubLevel rawSub : container.getAllSubLevels()) {
            ServerSubLevel balloonSub;
            if (!(rawSub instanceof ServerSubLevel) || (balloonSub = (ServerSubLevel)rawSub).isRemoved() || !BalloonResolve.isBalloonSubLevel((SubLevel)balloonSub)) continue;
            BalloonLoadReconciler.reconcileBalloon(level, container, balloonSub, snapPose);
        }
    }

    public static void reconcileBalloon(ServerLevel level, ServerSubLevel balloonSub) {
        BalloonLoadReconciler.reconcileBalloon(level, balloonSub, false);
    }

    public static void reconcileBalloon(ServerLevel level, ServerSubLevel balloonSub, boolean snapPose) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        BalloonLoadReconciler.reconcileBalloon(level, container, balloonSub, snapPose);
    }

    public static void reconcileBalloon(ServerLevel level, ServerSubLevelContainer container, ServerSubLevel balloonSub) {
        BalloonLoadReconciler.reconcileBalloon(level, container, balloonSub, false);
    }

    public static void reconcileBalloon(ServerLevel level, ServerSubLevelContainer container, ServerSubLevel balloonSub, boolean snapPose) {
        if (balloonSub.isRemoved() || !BalloonResolve.isBalloonSubLevel((SubLevel)balloonSub)) {
            return;
        }
        BlockEntity blockEntity = balloonSub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO);
        if (!(blockEntity instanceof BalloonBlockEntity)) {
            return;
        }
        BalloonBlockEntity be = (BalloonBlockEntity)blockEntity;
        BalloonHostIndex index = BalloonHostIndex.get(level);
        BalloonHostKey hostKey = BalloonHostKey.relocateHostKey(level, be, balloonSub);
        if (hostKey == null) {
            return;
        }
        if (!hostKey.equals(be.hostKey())) {
            be.rebindHost(hostKey);
        } else {
            be.refreshHostLattice(level, hostKey);
        }
        index.registerLoaded(be.hostKey(), balloonSub.getUniqueId());
        ServerSubLevel hostSub = BalloonPlacement.resolveHostSub((Level)level, be.hostSubLevelId());
        if (hostSub == null) {
            return;
        }
        if (snapPose) {
            BalloonPoseAlignment.alignToNominal(level, container, balloonSub, hostSub, hostKey.cell());
        }
        BalloonTetherController.register(balloonSub, be);
    }
}

