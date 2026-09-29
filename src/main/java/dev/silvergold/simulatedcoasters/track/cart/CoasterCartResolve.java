/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.companion.math.BoundingBox3d
 *  dev.ryanhcode.sable.companion.math.BoundingBox3dc
 *  dev.ryanhcode.sable.companion.math.BoundingBox3ic
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.plot.LevelPlot
 *  dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.chunk.LevelChunk
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.fml.loading.FMLEnvironment
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientMembership;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBearingTickCache;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartDuplicateEnforcer;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartIdIndex;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlotScan;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrackSnap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class CoasterCartResolve {
    private CoasterCartResolve() {
    }

    public static boolean isCartSubLevel(@Nullable SubLevel sub) {
        ServerSubLevel server;
        ServerLevel level;
        if (sub == null || sub.isRemoved()) {
            return false;
        }
        if (sub instanceof ServerSubLevel && (level = (server = (ServerSubLevel)sub).getLevel()) != null && !level.isClientSide()) {
            return CoasterCartBearingTickCache.scan(server).representative() != null;
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ClientMembership.isCart(sub);
        }
        return CoasterCartPlotScan.representativeBearingPlotPos(sub.getPlot(), (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get()) != null;
    }

    @Nullable
    public static SubLevel findCartSubLevelAt(Level level, BlockPos clicked) {
        SubLevel sub = Sable.HELPER.getContaining(level, (Vec3i)clicked);
        if (CoasterCartResolve.isCartSubLevel(sub) && CoasterCartResolve.isClickOnCartBearingBlock(level, sub, clicked)) {
            return sub;
        }
        BoundingBox3d query = new BoundingBox3d((double)clicked.getX() - 0.25, (double)clicked.getY() - 0.25, (double)clicked.getZ() - 0.25, (double)clicked.getX() + 1.25, (double)clicked.getY() + 1.25, (double)clicked.getZ() + 1.25);
        for (SubLevel candidate : Sable.HELPER.getAllIntersecting(level, (BoundingBox3dc)query)) {
            if (!CoasterCartResolve.isCartSubLevel(candidate) || !CoasterCartResolve.isClickOnCartBearingBlock(level, candidate, clicked)) continue;
            return candidate;
        }
        return null;
    }

    @Nullable
    public static SubLevel findSubLevelAt(Level level, BlockPos clicked) {
        SubLevel sub = Sable.HELPER.getContaining(level, (Vec3i)clicked);
        if (sub != null && !sub.isRemoved() && CoasterCartResolve.isClickOnSubLevelBlock(level, sub, clicked)) {
            return sub;
        }
        BoundingBox3d query = new BoundingBox3d((double)clicked.getX() - 0.25, (double)clicked.getY() - 0.25, (double)clicked.getZ() - 0.25, (double)clicked.getX() + 1.25, (double)clicked.getY() + 1.25, (double)clicked.getZ() + 1.25);
        for (SubLevel candidate : Sable.HELPER.getAllIntersecting(level, (BoundingBox3dc)query)) {
            if (candidate.isRemoved() || !CoasterCartResolve.isClickOnSubLevelBlock(level, candidate, clicked)) continue;
            return candidate;
        }
        return null;
    }

    @Nullable
    public static BlockPos plotLocalCellAt(Level level, SubLevel sub, BlockPos levelPos) {
        BlockPos center = sub.getPlot().getCenterBlock();
        if (Sable.HELPER.getContaining(level, (Vec3i)levelPos) == sub) {
            return levelPos.subtract((Vec3i)center);
        }
        Vector3d worldCenter = new Vector3d((double)levelPos.getX() + 0.5, (double)levelPos.getY() + 0.5, (double)levelPos.getZ() + 0.5);
        Vector3d plotLocal = sub.logicalPose().transformPositionInverse((Vector3dc)worldCenter, new Vector3d());
        if (!CoasterCartResolve.plotLocalPointInBounds(sub, plotLocal)) {
            return null;
        }
        return BlockPos.containing((double)plotLocal.x, (double)plotLocal.y, (double)plotLocal.z);
    }

    private static boolean isClickOnSubLevelBlock(Level level, SubLevel sub, BlockPos clicked) {
        if (Sable.HELPER.getContaining(level, (Vec3i)clicked) == sub) {
            return !level.getBlockState(clicked).isAir();
        }
        Vector3d worldCenter = new Vector3d((double)clicked.getX() + 0.5, (double)clicked.getY() + 0.5, (double)clicked.getZ() + 0.5);
        Vector3d plotLocal = sub.logicalPose().transformPositionInverse((Vector3dc)worldCenter, new Vector3d());
        if (!CoasterCartResolve.plotLocalPointInBounds(sub, plotLocal)) {
            return false;
        }
        BlockPos cell = BlockPos.containing((double)plotLocal.x, (double)plotLocal.y, (double)plotLocal.z);
        BlockState plotState = CoasterCartResolve.plotBlockStateAt(sub, cell);
        return plotState != null && !plotState.isAir();
    }

    private static boolean isClickOnCartBearingBlock(Level level, SubLevel cart, BlockPos clicked) {
        Block bearing = (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
        if (level.getBlockState(clicked).is(bearing) && Sable.HELPER.getContaining(level, (Vec3i)clicked) == cart) {
            return true;
        }
        Vector3d worldCenter = new Vector3d((double)clicked.getX() + 0.5, (double)clicked.getY() + 0.5, (double)clicked.getZ() + 0.5);
        Vector3d plotLocal = cart.logicalPose().transformPositionInverse((Vector3dc)worldCenter, new Vector3d());
        if (!CoasterCartResolve.plotLocalPointInBounds(cart, plotLocal)) {
            return false;
        }
        BlockPos cell = BlockPos.containing((double)plotLocal.x, (double)plotLocal.y, (double)plotLocal.z);
        BlockState plotState = CoasterCartResolve.plotBlockStateAt(cart, cell);
        return plotState != null && plotState.is(bearing);
    }

    private static boolean plotLocalPointInBounds(SubLevel sub, Vector3d plotLocal) {
        BoundingBox3ic bounds = sub.getPlot().getBoundingBox();
        return plotLocal.x >= (double)bounds.minX() && plotLocal.x <= (double)bounds.maxX() && plotLocal.y >= (double)bounds.minY() && plotLocal.y <= (double)bounds.maxY() && plotLocal.z >= (double)bounds.minZ() && plotLocal.z <= (double)bounds.maxZ();
    }

    @Nullable
    private static BlockState plotBlockStateAt(SubLevel sub, BlockPos plotLocalCell) {
        LevelChunk chunk;
        BlockPos world;
        ChunkPos globalChunk;
        BoundingBox3ic bounds = sub.getPlot().getBoundingBox();
        if (plotLocalCell.getX() < bounds.minX() || plotLocalCell.getX() > bounds.maxX() || plotLocalCell.getY() < bounds.minY() || plotLocalCell.getY() > bounds.maxY() || plotLocalCell.getZ() < bounds.minZ() || plotLocalCell.getZ() > bounds.maxZ()) {
            return null;
        }
        LevelPlot plot = sub.getPlot();
        if (!plot.contains(globalChunk = new ChunkPos(world = plotLocalCell.offset((Vec3i)plot.getCenterBlock())))) {
            return null;
        }
        PlotChunkHolder holder = plot.getChunkHolder(plot.toLocal(globalChunk));
        LevelChunk levelChunk = chunk = holder != null ? holder.getChunk() : null;
        if (chunk == null) {
            return null;
        }
        return chunk.getBlockState(world);
    }

    @Nullable
    public static SubLevel findCartById(Level level, UUID cartId) {
        SubLevelContainer raw;
        if (level instanceof ServerLevel) {
            ServerLevel server = (ServerLevel)level;
            if (!level.isClientSide()) {
                ServerSubLevel cart = CoasterCartIdIndex.find(server, cartId);
                if (cart == null || cart.isRemoved() || !CoasterCartResolve.isCartSubLevel((SubLevel)cart)) {
                    return null;
                }
                return cart;
            }
        }
        if ((raw = SubLevelContainer.getContainer((Level)level)) == null) {
            return null;
        }
        SubLevel sub = raw.getSubLevel(cartId);
        if (sub == null || sub.isRemoved() || !CoasterCartResolve.isCartSubLevel(sub)) {
            return null;
        }
        return sub;
    }

    @Nullable
    public static ServerSubLevel findCartById(ServerLevel level, UUID cartId) {
        ServerSubLevel server;
        SubLevel sub = CoasterCartResolve.findCartById((Level)level, cartId);
        return sub instanceof ServerSubLevel ? (server = (ServerSubLevel)sub) : null;
    }

    @Nullable
    public static Vector3d bearingPlotCenter(SubLevel cart) {
        ServerSubLevel server;
        ServerLevel level;
        BlockPos bearing = cart instanceof ServerSubLevel ? ((level = (server = (ServerSubLevel)cart).getLevel()) != null && !level.isClientSide() ? CoasterCartBearingTickCache.scan(server).representative() : CoasterCartTrackSnap.representativeBearingPlotPos(cart)) : CoasterCartTrackSnap.representativeBearingPlotPos(cart);
        if (bearing == null) {
            return null;
        }
        return new Vector3d((double)bearing.getX() + 0.5, (double)bearing.getY() + 0.5, (double)bearing.getZ() + 0.5);
    }

    @Nullable
    public static Vector3d cartCenterWorld(SubLevel cart) {
        Vector3d local = CoasterCartResolve.bearingPlotCenter(cart);
        if (local == null) {
            return null;
        }
        return cart.logicalPose().transformPosition((Vector3dc)local, new Vector3d());
    }

    public static void breakCartBlocksAfterAssemblyMove(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> movedSources) {
        CoasterCartDuplicateEnforcer.breakCartBlocksOnWorldGridAfterMove(level, transform, movedSources);
        CoasterCartIdIndex.invalidateDimension(level);
    }

    private static final class ClientMembership {
        private ClientMembership() {
        }

        static boolean isCart(SubLevel sub) {
            return CoasterCartClientMembership.isCart(sub);
        }
    }
}

