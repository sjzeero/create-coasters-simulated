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
 *  net.minecraft.core.Direction
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.rivet.RivetAnchorKey;
import dev.silvergold.simulatedcoasters.rivet.RivetBlock;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.rivet.RivetHostIndex;
import dev.silvergold.simulatedcoasters.rivet.RivetHostKey;
import dev.silvergold.simulatedcoasters.rivet.RivetPlacement;
import dev.silvergold.simulatedcoasters.rivet.RivetPoseAlignment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;

public final class RivetSpawner {
    private RivetSpawner() {
    }

    @Nullable
    public static ServerSubLevel tryPlace(ServerLevel level, RivetPlacement.Resolved placement) {
        if (!RivetPlacement.isValidPlacement((Level)level, placement)) {
            return null;
        }
        RivetAnchorKey anchor = RivetAnchorKey.from((Level)level, placement);
        if (RivetHostIndex.get(level).hasRivetAt(anchor)) {
            return null;
        }
        RivetHostKey hostKey = anchor.host();
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        Vec3 faceAnchor = RivetPlacement.faceAnchorWorld((Level)level, placement);
        Quaterniond orientation = RivetPlacement.worldOrientation((Level)level, placement);
        Pose3d pose = new Pose3d();
        pose.position().set(faceAnchor.x, faceAnchor.y, faceAnchor.z);
        pose.orientation().set((Quaterniondc)orientation);
        try {
            SubLevel sub = container.allocateNewSubLevel(pose);
            LevelPlot plot = sub.getPlot();
            plot.newEmptyChunk(plot.getCenterChunk());
            BlockState rivetState = (BlockState)((RivetBlock)((Object)SimulatedCoastersBlocks.RIVET.get())).defaultBlockState().setValue((Property)RivetBlock.FACING, (Comparable)Direction.UP);
            if (!plot.getEmbeddedLevelAccessor().setBlock(BlockPos.ZERO, rivetState, 3)) {
                ((ServerSubLevel)sub).markRemoved();
                return null;
            }
            ServerSubLevel serverSub = (ServerSubLevel)sub;
            serverSub.updateMergedMassData(1.0f);
            BlockEntity blockEntity = plot.getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO);
            if (!(blockEntity instanceof RivetBlockEntity)) {
                serverSub.markRemoved();
                return null;
            }
            RivetBlockEntity be = (RivetBlockEntity)blockEntity;
            be.bindHost(hostKey, placement.face(), RivetPlacement.attachment(placement), placement.yRotationDegrees());
            RivetPoseAlignment.alignToNominal(level, container, serverSub, be);
            be.applyHostLock(serverSub);
            RivetHostIndex.get(level).register(anchor, serverSub.getUniqueId());
            return serverSub;
        }
        catch (IllegalStateException ex) {
            return null;
        }
    }
}

