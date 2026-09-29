/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape
 *  dev.ryanhcode.sable.api.physics.collider.SableCollisionContext
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.ryanhcode.sable.api.physics.collider.SableCollisionContext;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class BalloonPlacement {
    public static final double STRING_TIE_Y_BLOCKS = 0.0625;
    public static final double LIFT_POINT_Y_BLOCKS = 0.9375;
    public static final double INITIAL_SPAWN_HEIGHT_BLOCKS = 2.5;

    private BalloonPlacement() {
    }

    public static BlockPos plotLatticeCell(SubLevel sub, BlockPos plotLocalCell) {
        return sub.getPlot().getCenterBlock().offset((Vec3i)plotLocalCell);
    }

    public static Vector3d hostAnchorPlot(SubLevel hostSub, BlockPos hostPlotLocal) {
        BlockPos lattice = BalloonPlacement.plotLatticeCell(hostSub, hostPlotLocal);
        VoxelShape shape = BalloonPlacement.hostAttachShape(hostSub, hostPlotLocal, lattice);
        if (shape.isEmpty()) {
            return new Vector3d((double)lattice.getX() + 0.5, (double)lattice.getY() + 1.0, (double)lattice.getZ() + 0.5);
        }
        AABB bounds = shape.bounds();
        return new Vector3d((double)lattice.getX() + (bounds.minX + bounds.maxX) * 0.5, (double)lattice.getY() + bounds.maxY, (double)lattice.getZ() + (bounds.minZ + bounds.maxZ) * 0.5);
    }

    private static VoxelShape hostAttachShape(SubLevel hostSub, BlockPos hostPlotLocal, BlockPos lattice) {
        BlockSubLevelCollisionShape extension;
        VoxelShape subShape;
        Block block;
        Level level = hostSub.getLevel();
        BlockState state = level.getBlockState(lattice);
        if (state.isAir()) {
            state = hostSub.getPlot().getEmbeddedLevelAccessor().getBlockState(hostPlotLocal);
        }
        if ((block = state.getBlock()) instanceof BlockSubLevelCollisionShape && !(subShape = (extension = (BlockSubLevelCollisionShape)block).getSubLevelCollisionShape((BlockGetter)level, state)).isEmpty()) {
            return subShape;
        }
        VoxelShape collision = state.getCollisionShape((BlockGetter)level, lattice, (CollisionContext)SableCollisionContext.get());
        if (!collision.isEmpty()) {
            return collision;
        }
        return state.getShape((BlockGetter)level, lattice);
    }

    public static Vector3d stringTiePlot(SubLevel balloonSub, BlockPos balloonPlotLocal) {
        BlockPos lattice = BalloonPlacement.plotLatticeCell(balloonSub, balloonPlotLocal);
        return new Vector3d((double)lattice.getX() + 0.5, (double)lattice.getY() + 0.0625, (double)lattice.getZ() + 0.5);
    }

    public static Vector3d liftPointPlot(SubLevel balloonSub, BlockPos balloonPlotLocal) {
        BlockPos lattice = BalloonPlacement.plotLatticeCell(balloonSub, balloonPlotLocal);
        return new Vector3d((double)lattice.getX() + 0.5, (double)lattice.getY() + 0.9375, (double)lattice.getZ() + 0.5);
    }

    public static boolean isValidHost(Level level, BlockPos clicked) {
        return BalloonPlacement.resolveHostSubLevel(level, clicked) != null;
    }

    @Nullable
    public static ServerSubLevel resolveHostSubLevel(Level level, BlockPos clicked) {
        SubLevel sub = CoasterCartResolve.findSubLevelAt(level, clicked);
        if (sub == null || sub.isRemoved() || BalloonResolve.isBalloonSubLevel(sub)) {
            return null;
        }
        if (!(sub instanceof ServerSubLevel)) {
            return null;
        }
        ServerSubLevel hostSub = (ServerSubLevel)sub;
        BlockPos plotLocal = CoasterCartResolve.plotLocalCellAt(level, sub, clicked);
        if (plotLocal == null) {
            return null;
        }
        if (hostSub.getPlot().getEmbeddedLevelAccessor().getBlockState(plotLocal).isAir()) {
            return null;
        }
        return hostSub;
    }

    @Nullable
    public static Vector3d hostAnchorWorld(ServerSubLevel hostSub, BlockPos hostPlotLocal) {
        if (hostSub.isRemoved()) {
            return null;
        }
        return hostSub.logicalPose().transformPosition((Vector3dc)BalloonPlacement.hostAnchorPlot((SubLevel)hostSub, hostPlotLocal), new Vector3d());
    }

    @Nullable
    public static Vector3d desiredStringWorld(ServerSubLevel hostSub, BlockPos hostPlotLocal) {
        Vector3d hostAnchor = BalloonPlacement.hostAnchorWorld(hostSub, hostPlotLocal);
        if (hostAnchor == null) {
            return null;
        }
        return new Vector3d((Vector3dc)hostAnchor).add(0.0, 2.5, 0.0);
    }

    @Nullable
    public static Vector3d desiredStringWorld(Level level, BlockPos clicked) {
        ServerSubLevel hostSub = BalloonPlacement.resolveHostSubLevel(level, clicked);
        if (hostSub == null) {
            return null;
        }
        BlockPos plotLocal = CoasterCartResolve.plotLocalCellAt(level, (SubLevel)hostSub, clicked);
        if (plotLocal == null) {
            return null;
        }
        return BalloonPlacement.desiredStringWorld(hostSub, plotLocal);
    }

    @Nullable
    public static ServerSubLevel resolveHostSub(Level level, UUID hostSubLevelId) {
        ServerSubLevel ssl;
        if (!(level instanceof ServerLevel)) {
            return null;
        }
        ServerLevel server = (ServerLevel)level;
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)server);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        SubLevel sub = container.getSubLevel(hostSubLevelId);
        return sub instanceof ServerSubLevel && !(ssl = (ServerSubLevel)sub).isRemoved() ? ssl : null;
    }
}

