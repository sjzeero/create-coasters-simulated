/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllBlocks
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor
 *  dev.ryanhcode.sable.sublevel.plot.LevelPlot
 *  dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelReader
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 */
package dev.silvergold.simulatedcoasters.track.cart;

import com.simibubi.create.AllBlocks;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBearingTickCache;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartIdIndex;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrackSnap;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkSync;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTangentOrientation;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;

public final class CoasterCartSpawner {
    private CoasterCartSpawner() {
    }

    @Nullable
    public static ServerSubLevel spawnMinimalContraption(ServerLevel level, Vec3 plotOriginWorld, @Nullable Quaterniond trackOrientation) {
        return CoasterCartSpawner.spawnMinimalContraption(level, plotOriginWorld, trackOrientation, null);
    }

    @Nullable
    public static ServerSubLevel spawnMinimalContraption(ServerLevel level, Vec3 plotOriginWorld, @Nullable Quaterniond trackOrientation, @Nullable CoasterPathTrackFrame.GraphHit initialSnapHit) {
        return CoasterCartSpawner.spawnMinimalContraption(level, plotOriginWorld, trackOrientation, initialSnapHit, initialSnapHit != null);
    }

    @Nullable
    public static ServerSubLevel spawnMinimalContraption(ServerLevel level, Vec3 plotOriginWorld, @Nullable Quaterniond trackOrientation, @Nullable CoasterPathTrackFrame.GraphHit initialSnapHit, boolean applyInitialTrackSnap) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        try {
            CoasterPathGraph graph = CoasterPathGraphManager.get(level);
            CoasterPathTangentOrientation.alignConnectedComponents((Level)level, graph);
            Quaterniond orientation = trackOrientation;
            orientation = orientation == null ? CoasterPathTrackFrame.quaternionWorldFromTrackAt((Level)level, graph, plotOriginWorld, 6.25) : new Quaterniond((Quaterniondc)orientation);
            Vec3 logicalPosition = CoasterCartLinkPlacement.bearingCenterWorld(plotOriginWorld, orientation);
            Pose3d pose = new Pose3d();
            pose.position().set(logicalPosition.x, logicalPosition.y, logicalPosition.z);
            pose.orientation().set((Quaterniondc)orientation);
            SubLevel sub = container.allocateNewSubLevel(pose);
            LevelPlot plot = sub.getPlot();
            ChunkPos center = plot.getCenterChunk();
            plot.newEmptyChunk(center);
            EmbeddedPlotLevelAccessor embedded = plot.getEmbeddedLevelAccessor();
            if (!embedded.setBlock(BlockPos.ZERO, ((CoasterCartBlock)((Object)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get())).defaultBlockState(), 3)) {
                ((ServerSubLevel)sub).markRemoved();
                return null;
            }
            BlockPos plotAnchor = plot.getCenterBlock();
            sub.logicalPose().rotationPoint().set((double)plotAnchor.getX() + 0.5, (double)plotAnchor.getY() + 0.5, (double)plotAnchor.getZ() + 0.5);
            sub.logicalPose().position().set(logicalPosition.x, logicalPosition.y, logicalPosition.z);
            sub.logicalPose().orientation().set((Quaterniondc)orientation);
            sub.updateLastPose();
            container.physicsSystem().getPipeline().onStatsChanged((ServerSubLevel)sub);
            if (applyInitialTrackSnap && initialSnapHit != null) {
                CoasterCartTrackSnap.applyInitialSnapAfterSpawn((ServerSubLevel)sub, level, initialSnapHit);
            }
            return (ServerSubLevel)sub;
        }
        catch (IllegalStateException ex) {
            return null;
        }
    }

    @Nullable
    public static ServerSubLevel retrofitSubLevelAsCart(ServerSubLevel sub, Level level, BlockPos levelPlacePos) {
        if (CoasterCartResolve.isCartSubLevel((SubLevel)sub)) {
            return null;
        }
        BlockPos plotCell = CoasterCartResolve.plotLocalCellAt(level, (SubLevel)sub, levelPlacePos);
        if (plotCell == null) {
            return null;
        }
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)sub.getLevel());
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        EmbeddedPlotLevelAccessor embedded = sub.getPlot().getEmbeddedLevelAccessor();
        BlockState existing = embedded.getBlockState(plotCell);
        if (!existing.isAir() && !existing.canBeReplaced()) {
            return null;
        }
        CoasterCartSpawner.ensurePlotChunk(sub, plotCell);
        if (!embedded.setBlock(plotCell, ((CoasterCartBlock)((Object)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get())).defaultBlockState(), 3)) {
            return null;
        }
        CoasterCartBearingTickCache.invalidate(sub);
        CoasterCartIdIndex.invalidateDimension((ServerLevel)level);
        container.physicsSystem().getPipeline().onStatsChanged(sub);
        CoasterCartTrackSnap.markSpawnedDisengaged(sub);
        CoasterCartTrainLinkConstraint.pruneDefunctLinks((ServerLevel)level, sub);
        CoasterCartTrainLinkSync.broadcastCart(sub);
        CoasterCartSpawner.playSubLevelRetrofitPlaceSound(level, levelPlacePos);
        return sub;
    }

    private static void playSubLevelRetrofitPlaceSound(Level level, BlockPos levelPos) {
        SoundType sound = AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState().getSoundType((LevelReader)level, levelPos, null);
        level.playSound(null, levelPos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch());
    }

    private static void ensurePlotChunk(ServerSubLevel sub, BlockPos plotLocal) {
        BlockPos world;
        ChunkPos globalChunk;
        ServerLevelPlot plot = sub.getPlot();
        if (plot.getChunkHolder(plot.toLocal(globalChunk = new ChunkPos(world = plotLocal.offset((Vec3i)plot.getCenterBlock())))) == null) {
            plot.newEmptyChunk(globalChunk);
        }
    }
}

