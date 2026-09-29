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
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import dev.silvergold.simulatedcoasters.balloon.BalloonRemoval;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class BalloonCascadePop {
    private static final int CASCADE_DELAY_TICKS = 3;
    private static final List<Pending> PENDING = new ArrayList<Pending>();

    private BalloonCascadePop() {
    }

    public static void onBalloonPopped(ServerLevel level, @Nullable UUID hostSubLevelId, Vector3d poppedPos, UUID poppedBalloonId) {
        if (hostSubLevelId == null) {
            return;
        }
        ServerSubLevel next = BalloonCascadePop.nearestOtherBalloonOnHost(level, hostSubLevelId, poppedPos, poppedBalloonId);
        if (next != null) {
            BalloonCascadePop.schedule((ResourceKey<Level>)level.dimension(), next.getUniqueId());
        }
    }

    private static void schedule(ResourceKey<Level> dimension, UUID balloonId) {
        for (Pending p : PENDING) {
            if (!p.dimension.equals(dimension) || !p.balloonId.equals(balloonId)) continue;
            return;
        }
        PENDING.add(new Pending(dimension, balloonId, 3));
    }

    private static boolean isScheduled(ResourceKey<Level> dimension, UUID balloonId) {
        for (Pending p : PENDING) {
            if (!p.dimension.equals(dimension) || !p.balloonId.equals(balloonId)) continue;
            return true;
        }
        return false;
    }

    public static void clearDimension(ResourceKey<Level> dimension) {
        PENDING.removeIf(p -> p.dimension.equals((Object)dimension));
    }

    public static void serverTick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        ArrayList<Runnable> due = new ArrayList<Runnable>();
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            ServerSubLevel balloon;
            Pending p = it.next();
            if (--p.ticksLeft > 0) continue;
            it.remove();
            ServerLevel level = server.getLevel(p.dimension);
            if (level == null || (balloon = BalloonResolve.findBalloonById(level, p.balloonId)) == null) continue;
            due.add(() -> BalloonRemoval.pop(level, balloon));
        }
        for (Runnable r : due) {
            r.run();
        }
    }

    @Nullable
    private static ServerSubLevel nearestOtherBalloonOnHost(ServerLevel level, UUID hostSubLevelId, Vector3d fromPos, UUID excludeBalloonId) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        ServerSubLevel best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (SubLevel sub : container.getAllSubLevels()) {
            double distSq;
            UUID host;
            ServerSubLevel balloon;
            if (!(sub instanceof ServerSubLevel) || (balloon = (ServerSubLevel)sub).isRemoved() || balloon.getUniqueId().equals(excludeBalloonId) || !BalloonResolve.isBalloonSubLevel((SubLevel)balloon) || BalloonRemoval.isRemoving(balloon.getUniqueId()) || BalloonCascadePop.isScheduled((ResourceKey<Level>)level.dimension(), balloon.getUniqueId()) || (host = BalloonCascadePop.hostSubLevelIdOf(level, balloon)) == null || !host.equals(hostSubLevelId) || !((distSq = balloon.logicalPose().position().distanceSquared((Vector3dc)fromPos)) < bestDistSq)) continue;
            bestDistSq = distSq;
            best = balloon;
        }
        return best;
    }

    @Nullable
    public static UUID hostSubLevelIdOf(ServerLevel level, ServerSubLevel balloon) {
        BlockEntity blockEntity = balloon.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO);
        if (!(blockEntity instanceof BalloonBlockEntity)) {
            return null;
        }
        BalloonBlockEntity be = (BalloonBlockEntity)blockEntity;
        ServerSubLevel host = be.resolveHostSubLevel(level);
        return host != null ? host.getUniqueId() : be.hostSubLevelId();
    }

    private static final class Pending {
        final ResourceKey<Level> dimension;
        final UUID balloonId;
        int ticksLeft;

        Pending(ResourceKey<Level> dimension, UUID balloonId, int ticksLeft) {
            this.dimension = dimension;
            this.balloonId = balloonId;
            this.ticksLeft = ticksLeft;
        }
    }
}

