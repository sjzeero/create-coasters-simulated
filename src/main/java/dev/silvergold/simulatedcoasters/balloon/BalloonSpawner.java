/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.plot.LevelPlot
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostIndex;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostKey;
import dev.silvergold.simulatedcoasters.balloon.BalloonPlacement;
import dev.silvergold.simulatedcoasters.balloon.BalloonPoseAlignment;
import dev.silvergold.simulatedcoasters.balloon.BalloonTetherController;
import dev.silvergold.simulatedcoasters.balloon.RedBalloonBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class BalloonSpawner {
    private BalloonSpawner() {
    }

    @Nullable
    public static ServerSubLevel tryAttach(ServerLevel level, BlockPos hostCell) {
        ServerSubLevelContainer raw;
        if (!BalloonPlacement.isValidHost((Level)level, hostCell)) {
            return null;
        }
        BalloonHostKey hostKey = BalloonHostKey.of((Level)level, hostCell);
        if (hostKey == null) {
            return null;
        }
        if (BalloonHostIndex.get(level).hasBalloonAt(hostKey)) {
            ServerSubLevel existing = BalloonHostIndex.get(level).findByHost(level, hostKey);
            if (existing != null) {
                return null;
            }
            BalloonHostIndex.get(level).unregisterHost(hostKey);
        }
        if (!((raw = SubLevelContainer.getContainer((ServerLevel)level)) instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        ServerSubLevel hostSub = (ServerSubLevel)container.getSubLevel(hostKey.hostSubLevelId());
        if (hostSub == null || hostSub.isRemoved()) {
            return null;
        }
        Vector3d spawnAnchor = BalloonPlacement.desiredStringWorld(hostSub, hostKey.cell());
        if (spawnAnchor == null) {
            return null;
        }
        Pose3d pose = new Pose3d();
        pose.position().set((Vector3dc)spawnAnchor);
        pose.orientation().set((Quaterniondc)new Quaterniond());
        try {
            SubLevel sub = container.allocateNewSubLevel(pose);
            LevelPlot plot = sub.getPlot();
            plot.newEmptyChunk(plot.getCenterChunk());
            BlockState balloonState = ((RedBalloonBlock)((Object)SimulatedCoastersBlocks.RED_BALLOON.get())).defaultBlockState();
            if (!plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, balloonState, 3)) {
                ((ServerSubLevel)sub).markRemoved();
                return null;
            }
            ServerSubLevel balloonSub = (ServerSubLevel)sub;
            BlockEntity blockEntity = plot.getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO);
            if (!(blockEntity instanceof BalloonBlockEntity)) {
                balloonSub.markRemoved();
                return null;
            }
            BalloonBlockEntity be = (BalloonBlockEntity)blockEntity;
            be.bindHost(hostKey);
            BalloonPoseAlignment.alignToNominal(level, container, balloonSub, hostSub, hostKey.cell());
            BalloonHostIndex.get(level).register(hostKey, balloonSub.getUniqueId());
            BalloonTetherController.register(balloonSub, be);
            return balloonSub;
        }
        catch (IllegalStateException ex) {
            return null;
        }
    }
}

