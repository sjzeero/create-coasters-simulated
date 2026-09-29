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
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.context.BlockPlaceContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.collider.SableCollisionContext;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class RivetPlacementCollision {
    private static final double SAT_MIN_OVERLAP_SQ = 0.05;
    private static final double QUERY_EXPAND = 0.36602540380000004;
    private static final LevelReusedVectors SCRATCH_VECTORS = new LevelReusedVectors();
    private static final List<OrientedBoundingBox3d> PLACED_BOX_SCRATCH = new ArrayList<OrientedBoundingBox3d>(8);
    private static final List<SubLevel> INTERSECTING_SUBS_SCRATCH = new ArrayList<SubLevel>(8);
    private static final List<double[]> CLIPPED_BOX_SCRATCH = new ArrayList<double[]>(8);
    private static final Quaterniond IDENTITY = new Quaterniond();

    private RivetPlacementCollision() {
    }

    public static boolean hasShapeObstruction(BlockPlaceContext context, BlockState placing) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        VoxelShape placingShape = RivetPlacementCollision.subLevelCollisionShape((BlockGetter)level, placing);
        if (placingShape.isEmpty()) {
            return false;
        }
        SubLevel placementSub = Sable.HELPER.getContaining(level, (Vec3i)clicked);
        Pose3d placementPose = RivetPlacementCollision.placementPose(placementSub);
        List<OrientedBoundingBox3d> placedBoxes = RivetPlacementCollision.buildPlacedObbs(clicked, placingShape, placementPose, SCRATCH_VECTORS);
        if (placedBoxes.isEmpty()) {
            return false;
        }
        BoundingBox3d worldCell = new BoundingBox3d(clicked);
        if (placementSub != null) {
            worldCell.transform((Pose3dc)placementSub.logicalPose(), worldCell);
        }
        BoundingBox3d queryBounds = worldCell.expand(0.36602540380000004, new BoundingBox3d());
        List<OrientedBoundingBox3d> placedSnapshot = List.copyOf(placedBoxes);
        INTERSECTING_SUBS_SCRATCH.clear();
        for (SubLevel sub : Sable.HELPER.getAllIntersecting(level, (BoundingBox3dc)queryBounds)) {
            INTERSECTING_SUBS_SCRATCH.add(sub);
        }
        for (SubLevel sub : INTERSECTING_SUBS_SCRATCH) {
            if (sub == placementSub || !RivetPlacementCollision.intersectsPlotBlocks(level, context, sub, (BoundingBox3dc)queryBounds, placedSnapshot, SCRATCH_VECTORS)) continue;
            return true;
        }
        return RivetPlacementCollision.intersectsWorldBlocks(level, context, clicked, (BoundingBox3dc)queryBounds, placedSnapshot, SCRATCH_VECTORS);
    }

    private static Pose3d placementPose(SubLevel placementSub) {
        Pose3d pose = new Pose3d();
        if (placementSub != null) {
            pose.set((Pose3dc)placementSub.logicalPose());
        }
        return pose;
    }

    private static List<OrientedBoundingBox3d> buildPlacedObbs(BlockPos clicked, VoxelShape shape, Pose3d placementPose, LevelReusedVectors sink) {
        PLACED_BOX_SCRATCH.clear();
        Quaterniond orientation = placementPose.orientation();
        for (double[] slice : RivetPlacementCollision.clippedBoxes(shape)) {
            PLACED_BOX_SCRATCH.add(RivetPlacementCollision.obbForPlotBlockBox(clicked, slice, placementPose, (Quaterniondc)orientation, sink));
        }
        return PLACED_BOX_SCRATCH;
    }

    private static boolean intersectsPlotBlocks(Level level, BlockPlaceContext context, SubLevel sub, BoundingBox3dc queryBounds, List<OrientedBoundingBox3d> placedBoxes, LevelReusedVectors sink) {
        Pose3d spacePose = new Pose3d();
        spacePose.set((Pose3dc)sub.logicalPose());
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
                    if (RivetPlacementCollision.obstacleIgnored(level, context, plotPos) || (shape = RivetPlacementCollision.subLevelCollisionShape((BlockGetter)level, level.getBlockState(plotPos))).isEmpty()) continue;
                    for (double[] slice : RivetPlacementCollision.clippedBoxes(shape)) {
                        OrientedBoundingBox3d obstacle = RivetPlacementCollision.obbForPlotBlockBox(plotPos, slice, spacePose, (Quaterniondc)spaceOrientation, sink);
                        if (!RivetPlacementCollision.intersectsAny(obstacle, placedBoxes)) continue;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean intersectsWorldBlocks(Level level, BlockPlaceContext context, BlockPos clicked, BoundingBox3dc queryBounds, List<OrientedBoundingBox3d> placedBoxes, LevelReusedVectors sink) {
        int minX = Mth.floor((double)queryBounds.minX());
        int minY = Mth.floor((double)queryBounds.minY());
        int minZ = Mth.floor((double)queryBounds.minZ());
        int maxX = Mth.floor((double)queryBounds.maxX());
        int maxY = Mth.floor((double)queryBounds.maxY());
        int maxZ = Mth.floor((double)queryBounds.maxZ());
        for (int x = minX; x <= maxX; ++x) {
            for (int y = minY; y <= maxY; ++y) {
                for (int z = minZ; z <= maxZ; ++z) {
                    VoxelShape shape;
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals((Object)clicked) || RivetPlacementCollision.obstacleIgnored(level, context, pos) || Sable.HELPER.getContaining(level, (Vec3i)pos) != null || (shape = RivetPlacementCollision.subLevelCollisionShape((BlockGetter)level, level.getBlockState(pos))).isEmpty()) continue;
                    for (double[] slice : RivetPlacementCollision.clippedBoxes(shape)) {
                        Vector3d center = sink.center.set((double)pos.getX() + (slice[0] + slice[3]) * 0.5, (double)pos.getY() + (slice[1] + slice[4]) * 0.5, (double)pos.getZ() + (slice[2] + slice[5]) * 0.5);
                        OrientedBoundingBox3d obstacle = new OrientedBoundingBox3d((Vector3dc)center, (Vector3dc)new Vector3d(slice[3] - slice[0], slice[4] - slice[1], slice[5] - slice[2]), (Quaterniondc)IDENTITY, sink);
                        if (!RivetPlacementCollision.intersectsAny(obstacle, placedBoxes)) continue;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean obstacleIgnored(Level level, BlockPlaceContext context, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced(context);
    }

    private static OrientedBoundingBox3d obbForPlotBlockBox(BlockPos plotPos, double[] slice, Pose3d spacePose, Quaterniondc spaceOrientation, LevelReusedVectors sink) {
        Vector3d center = sink.center.set((double)plotPos.getX() + (slice[0] + slice[3]) * 0.5, (double)plotPos.getY() + (slice[1] + slice[4]) * 0.5, (double)plotPos.getZ() + (slice[2] + slice[5]) * 0.5);
        spacePose.transformPosition(center);
        return new OrientedBoundingBox3d((Vector3dc)center, (Vector3dc)new Vector3d(slice[3] - slice[0], slice[4] - slice[1], slice[5] - slice[2]), spaceOrientation, sink);
    }

    private static boolean intersectsAny(OrientedBoundingBox3d obstacle, List<OrientedBoundingBox3d> placedBoxes) {
        for (OrientedBoundingBox3d placed : placedBoxes) {
            if (!(OrientedBoundingBox3d.sat((OrientedBoundingBox3d)obstacle, (OrientedBoundingBox3d)placed).lengthSquared() > 0.05)) continue;
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

