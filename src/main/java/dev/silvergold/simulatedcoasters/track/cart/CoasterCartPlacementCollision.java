/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape
 *  dev.ryanhcode.sable.api.math.LevelReusedVectors
 *  dev.ryanhcode.sable.api.math.OrientedBoundingBox3d
 *  dev.ryanhcode.sable.api.physics.collider.SableCollisionContext
 *  dev.ryanhcode.sable.companion.math.BoundingBox3d
 *  dev.ryanhcode.sable.companion.math.BoundingBox3dc
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.ClientSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.collider.SableCollisionContext;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class CoasterCartPlacementCollision {
    private static final double SAT_MIN_OVERLAP_SQ = 0.05;
    private static final double QUERY_EXPAND = 0.36602540380000004;
    private static final double CLIENT_POSE_EPS = 0.001;
    private static final LevelReusedVectors SCRATCH_VECTORS = new LevelReusedVectors();
    private static final List<OrientedBoundingBox3d> KIT_BOX_SCRATCH = new ArrayList<OrientedBoundingBox3d>(4);
    private static final List<SubLevel> INTERSECTING_SUBS_SCRATCH = new ArrayList<SubLevel>(4);
    private static final List<double[]> CLIPPED_BOX_SCRATCH = new ArrayList<double[]>(8);
    private static final Quaterniond IDENTITY = new Quaterniond();
    private static long clientCacheTick = Long.MIN_VALUE;
    private static double clientCacheOx;
    private static double clientCacheOy;
    private static double clientCacheOz;
    private static double clientCacheQx;
    private static double clientCacheQy;
    private static double clientCacheQz;
    private static double clientCacheQw;
    private static boolean clientCacheObstructed;

    private CoasterCartPlacementCollision() {
    }

    public static boolean hasSubLevelObstruction(Level level, Vec3 plotOriginWorld, Quaterniond orientation) {
        return CoasterCartPlacementCollision.hasSubLevelObstruction(level, plotOriginWorld, orientation, null);
    }

    public static boolean hasSubLevelObstruction(Level level, Vec3 plotOriginWorld, Quaterniond orientation, @Nullable Float partialTickForRender) {
        long tick;
        if (level.isClientSide() && (tick = level.getGameTime()) == clientCacheTick && CoasterCartPlacementCollision.sameCachedPose(plotOriginWorld, orientation)) {
            return clientCacheObstructed;
        }
        Pose3d plotPose = CoasterCartPlacementCollision.plotPose(plotOriginWorld, orientation);
        List<OrientedBoundingBox3d> kitBoxes = CoasterCartPlacementCollision.buildProposedKitObbs(level, plotPose, SCRATCH_VECTORS);
        if (kitBoxes.isEmpty()) {
            CoasterCartPlacementCollision.storeClientCache(level, plotOriginWorld, orientation, false);
            return false;
        }
        BoundingBox3d worldBounds = new BoundingBox3d(0.0, 0.0, 0.0, 1.0, 1.0, 1.0).transform((Pose3dc)plotPose, new BoundingBox3d());
        BoundingBox3d queryBounds = worldBounds.expand(0.36602540380000004, new BoundingBox3d());
        boolean obstructed = CoasterCartPlacementCollision.intersectsBlocksInQuery(level, (BoundingBox3dc)queryBounds, List.copyOf(kitBoxes), partialTickForRender, SCRATCH_VECTORS);
        CoasterCartPlacementCollision.storeClientCache(level, plotOriginWorld, orientation, obstructed);
        return obstructed;
    }

    private static void storeClientCache(Level level, Vec3 origin, Quaterniond orientation, boolean obstructed) {
        if (!level.isClientSide()) {
            return;
        }
        clientCacheTick = level.getGameTime();
        clientCacheOx = origin.x;
        clientCacheOy = origin.y;
        clientCacheOz = origin.z;
        clientCacheQx = orientation.x;
        clientCacheQy = orientation.y;
        clientCacheQz = orientation.z;
        clientCacheQw = orientation.w;
        clientCacheObstructed = obstructed;
    }

    private static boolean sameCachedPose(Vec3 origin, Quaterniond orientation) {
        return Math.abs(origin.x - clientCacheOx) < 0.001 && Math.abs(origin.y - clientCacheOy) < 0.001 && Math.abs(origin.z - clientCacheOz) < 0.001 && Math.abs(orientation.x - clientCacheQx) < 0.001 && Math.abs(orientation.y - clientCacheQy) < 0.001 && Math.abs(orientation.z - clientCacheQz) < 0.001 && Math.abs(orientation.w - clientCacheQw) < 0.001;
    }

    private static Pose3d plotPose(Vec3 plotOriginWorld, Quaterniond orientation) {
        Pose3d pose = new Pose3d();
        pose.position().set(plotOriginWorld.x, plotOriginWorld.y, plotOriginWorld.z);
        pose.orientation().set((Quaterniondc)orientation);
        return pose;
    }

    private static Pose3d collisionPose(SubLevel subLevel, @Nullable Float partialTickForRender) {
        Pose3d pose = new Pose3d();
        if (partialTickForRender != null && subLevel instanceof ClientSubLevel) {
            ClientSubLevel client = (ClientSubLevel)subLevel;
            client.lastPose().lerp((Pose3dc)client.logicalPose(), (double)partialTickForRender.floatValue(), pose);
        } else {
            pose.set((Pose3dc)subLevel.logicalPose());
        }
        return pose;
    }

    private static List<OrientedBoundingBox3d> buildProposedKitObbs(Level level, Pose3d plotPose, LevelReusedVectors sink) {
        BlockState bearingState = ((CoasterCartBlock)((Object)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get())).defaultBlockState();
        VoxelShape shape = CoasterCartPlacementCollision.subLevelCollisionShape((BlockGetter)level, bearingState);
        KIT_BOX_SCRATCH.clear();
        if (shape.isEmpty()) {
            return KIT_BOX_SCRATCH;
        }
        Quaterniond plotOrientation = plotPose.orientation();
        for (double[] slice : CoasterCartPlacementCollision.clippedBoxes(shape)) {
            KIT_BOX_SCRATCH.add(CoasterCartPlacementCollision.obbForBodyCellBox(slice, plotPose, (Quaterniondc)plotOrientation, sink));
        }
        return KIT_BOX_SCRATCH;
    }

    private static OrientedBoundingBox3d obbForBodyCellBox(double[] slice, Pose3d plotPose, Quaterniondc plotOrientation, LevelReusedVectors sink) {
        Vector3d center = sink.center.set((slice[0] + slice[3]) * 0.5, (slice[1] + slice[4]) * 0.5, (slice[2] + slice[5]) * 0.5);
        plotPose.transformPosition(center);
        return new OrientedBoundingBox3d((Vector3dc)center, (Vector3dc)new Vector3d(slice[3] - slice[0], slice[4] - slice[1], slice[5] - slice[2]), plotOrientation, sink);
    }

    private static boolean intersectsBlocksInQuery(Level level, BoundingBox3dc queryBounds, List<OrientedBoundingBox3d> kitBoxes, @Nullable Float partialTickForRender, LevelReusedVectors sink) {
        INTERSECTING_SUBS_SCRATCH.clear();
        for (SubLevel sub : Sable.HELPER.getAllIntersecting(level, queryBounds)) {
            INTERSECTING_SUBS_SCRATCH.add(sub);
        }
        for (SubLevel sub : INTERSECTING_SUBS_SCRATCH) {
            if (!CoasterCartPlacementCollision.intersectsPlotBlocksInQuery(level, sub, queryBounds, kitBoxes, partialTickForRender, sink)) continue;
            return true;
        }
        return CoasterCartPlacementCollision.intersectsWorldBlocksInQuery(level, queryBounds, kitBoxes, sink);
    }

    private static boolean intersectsPlotBlocksInQuery(Level level, SubLevel sub, BoundingBox3dc queryBounds, List<OrientedBoundingBox3d> kitBoxes, @Nullable Float partialTickForRender, LevelReusedVectors sink) {
        Pose3d spacePose = CoasterCartPlacementCollision.collisionPose(sub, partialTickForRender);
        BoundingBox3d plotQuery = new BoundingBox3d(queryBounds.minX(), queryBounds.minY(), queryBounds.minZ(), queryBounds.maxX(), queryBounds.maxY(), queryBounds.maxZ());
        plotQuery.transformInverse((Pose3dc)spacePose, plotQuery);
        plotQuery.expand(0.36602540380000004, plotQuery);
        int minX = Mth.floor((double)plotQuery.minX());
        int minY = Mth.floor((double)plotQuery.minY());
        int minZ = Mth.floor((double)plotQuery.minZ());
        int maxX = Mth.floor((double)plotQuery.maxX());
        int maxY = Mth.floor((double)plotQuery.maxY());
        int maxZ = Mth.floor((double)plotQuery.maxZ());
        Quaterniond spaceOrientation = spacePose.orientation();
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    VoxelShape shape;
                    BlockPos plotPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(plotPos);
                    if (state.isAir() || (shape = CoasterCartPlacementCollision.subLevelCollisionShape((BlockGetter)level, state)).isEmpty()) continue;
                    for (double[] slice : CoasterCartPlacementCollision.clippedBoxes(shape)) {
                        OrientedBoundingBox3d obstacle = CoasterCartPlacementCollision.obbForPlotBlockBox(plotPos, slice, spacePose, (Quaterniondc)spaceOrientation, sink);
                        if (!CoasterCartPlacementCollision.intersectsAnyKit(obstacle, kitBoxes)) continue;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean intersectsWorldBlocksInQuery(Level level, BoundingBox3dc queryBounds, List<OrientedBoundingBox3d> kitBoxes, LevelReusedVectors sink) {
        int minX = Mth.floor((double)queryBounds.minX());
        int minY = Mth.floor((double)queryBounds.minY());
        int minZ = Mth.floor((double)queryBounds.minZ());
        int maxX = Mth.floor((double)queryBounds.maxX());
        int maxY = Mth.floor((double)queryBounds.maxY());
        int maxZ = Mth.floor((double)queryBounds.maxZ());
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    if (!CoasterCartPlacementCollision.intersectsWorldBlockAt(level, new BlockPos(x, y, z), kitBoxes, sink)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private static OrientedBoundingBox3d obbForPlotBlockBox(BlockPos plotPos, double[] slice, Pose3d spacePose, Quaterniondc spaceOrientation, LevelReusedVectors sink) {
        Vector3d center = sink.center.set((double)plotPos.getX() + (slice[0] + slice[3]) * 0.5, (double)plotPos.getY() + (slice[1] + slice[4]) * 0.5, (double)plotPos.getZ() + (slice[2] + slice[5]) * 0.5);
        spacePose.transformPosition(center);
        return new OrientedBoundingBox3d((Vector3dc)center, (Vector3dc)new Vector3d(slice[3] - slice[0], slice[4] - slice[1], slice[5] - slice[2]), spaceOrientation, sink);
    }

    private static boolean intersectsWorldBlockAt(Level level, BlockPos pos, List<OrientedBoundingBox3d> kitBoxes, LevelReusedVectors sink) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        VoxelShape shape = CoasterCartPlacementCollision.subLevelCollisionShape((BlockGetter)level, state);
        if (shape.isEmpty()) {
            return false;
        }
        for (double[] slice : CoasterCartPlacementCollision.clippedBoxes(shape)) {
            Vector3d center = sink.center.set((double)pos.getX() + (slice[0] + slice[3]) * 0.5, (double)pos.getY() + (slice[1] + slice[4]) * 0.5, (double)pos.getZ() + (slice[2] + slice[5]) * 0.5);
            OrientedBoundingBox3d obstacle = new OrientedBoundingBox3d((Vector3dc)center, (Vector3dc)new Vector3d(slice[3] - slice[0], slice[4] - slice[1], slice[5] - slice[2]), (Quaterniondc)IDENTITY, sink);
            if (!CoasterCartPlacementCollision.intersectsAnyKit(obstacle, kitBoxes)) continue;
            return true;
        }
        return false;
    }

    private static boolean intersectsAnyKit(OrientedBoundingBox3d obstacle, List<OrientedBoundingBox3d> kitBoxes) {
        for (OrientedBoundingBox3d kit : kitBoxes) {
            if (!(OrientedBoundingBox3d.sat((OrientedBoundingBox3d)obstacle, (OrientedBoundingBox3d)kit).lengthSquared() > 0.05)) continue;
            return true;
        }
        return false;
    }

    private static VoxelShape subLevelCollisionShape(BlockGetter getter, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof BlockSubLevelCollisionShape) {
            BlockSubLevelCollisionShape extension = (BlockSubLevelCollisionShape)block;
            return extension.getSubLevelCollisionShape(getter, state);
        }
        return state.getCollisionShape(getter, BlockPos.ZERO, (CollisionContext)SableCollisionContext.get());
    }

    private static List<double[]> clippedBoxes(VoxelShape shape) {
        CLIPPED_BOX_SCRATCH.clear();
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> CLIPPED_BOX_SCRATCH.add(new double[]{Math.max(minX, 0.0), Math.max(minY, 0.0), Math.max(minZ, 0.0), Math.min(maxX, 1.0), Math.min(maxY, 1.0), Math.min(maxZ, 1.0)}));
        return List.copyOf(CLIPPED_BOX_SCRATCH);
    }
}

