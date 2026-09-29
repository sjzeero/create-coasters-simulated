package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.physics.collider.SableCollisionContext;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

public final class RivetPlacementCollision {
   private static final double SAT_MIN_OVERLAP_SQ = 0.05;
   private static final double QUERY_EXPAND = 0.36602540380000004;
   private static final LevelReusedVectors SCRATCH_VECTORS = new LevelReusedVectors();
   private static final List<OrientedBoundingBox3d> PLACED_BOX_SCRATCH = new ArrayList<>(8);
   private static final List<SubLevel> INTERSECTING_SUBS_SCRATCH = new ArrayList<>(8);
   private static final List<double[]> CLIPPED_BOX_SCRATCH = new ArrayList<>(8);
   private static final Quaterniond IDENTITY = new Quaterniond();

   private RivetPlacementCollision() {
   }

   public static boolean hasShapeObstruction(BlockPlaceContext context, BlockState placing) {
      Level level = context.getLevel();
      BlockPos clicked = context.getClickedPos();
      VoxelShape placingShape = subLevelCollisionShape(level, placing);
      if (placingShape.isEmpty()) {
         return false;
      } else {
         SubLevel placementSub = Sable.HELPER.getContaining(level, clicked);
         Pose3d placementPose = placementPose(placementSub);
         List<OrientedBoundingBox3d> placedBoxes = buildPlacedObbs(clicked, placingShape, placementPose, SCRATCH_VECTORS);
         if (placedBoxes.isEmpty()) {
            return false;
         } else {
            BoundingBox3d worldCell = new BoundingBox3d(clicked);
            if (placementSub != null) {
               worldCell.transform(placementSub.logicalPose(), worldCell);
            }

            BoundingBox3d queryBounds = worldCell.expand(0.36602540380000004, new BoundingBox3d());
            List<OrientedBoundingBox3d> placedSnapshot = List.copyOf(placedBoxes);
            INTERSECTING_SUBS_SCRATCH.clear();

            for (SubLevel sub : Sable.HELPER.getAllIntersecting(level, queryBounds)) {
               INTERSECTING_SUBS_SCRATCH.add(sub);
            }

            for (SubLevel sub : INTERSECTING_SUBS_SCRATCH) {
               if (sub != placementSub && intersectsPlotBlocks(level, context, sub, queryBounds, placedSnapshot, SCRATCH_VECTORS)) {
                  return true;
               }
            }

            return intersectsWorldBlocks(level, context, clicked, queryBounds, placedSnapshot, SCRATCH_VECTORS);
         }
      }
   }

   private static Pose3d placementPose(SubLevel placementSub) {
      Pose3d pose = new Pose3d();
      if (placementSub != null) {
         pose.set(placementSub.logicalPose());
      }

      return pose;
   }

   private static List<OrientedBoundingBox3d> buildPlacedObbs(BlockPos clicked, VoxelShape shape, Pose3d placementPose, LevelReusedVectors sink) {
      PLACED_BOX_SCRATCH.clear();
      Quaterniond orientation = placementPose.orientation();

      for (double[] slice : clippedBoxes(shape)) {
         PLACED_BOX_SCRATCH.add(obbForPlotBlockBox(clicked, slice, placementPose, orientation, sink));
      }

      return PLACED_BOX_SCRATCH;
   }

   private static boolean intersectsPlotBlocks(
      Level level, BlockPlaceContext context, SubLevel sub, BoundingBox3dc queryBounds, List<OrientedBoundingBox3d> placedBoxes, LevelReusedVectors sink
   ) {
      Pose3d spacePose = new Pose3d();
      spacePose.set(sub.logicalPose());
      BoundingBox3d plotQuery = new BoundingBox3d(
         queryBounds.minX(), queryBounds.minY(), queryBounds.minZ(), queryBounds.maxX(), queryBounds.maxY(), queryBounds.maxZ()
      );
      plotQuery.transformInverse(spacePose, plotQuery);
      plotQuery.expand(0.36602540380000004, plotQuery);
      int minX = Mth.floor(plotQuery.minX());
      int minY = Mth.floor(plotQuery.minY());
      int minZ = Mth.floor(plotQuery.minZ());
      int maxX = Mth.floor(plotQuery.maxX());
      int maxY = Mth.floor(plotQuery.maxY());
      int maxZ = Mth.floor(plotQuery.maxZ());
      Quaterniond spaceOrientation = spacePose.orientation();

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               BlockPos plotPos = new BlockPos(x, y, z);
               if (!obstacleIgnored(level, context, plotPos)) {
                  VoxelShape shape = subLevelCollisionShape(level, level.getBlockState(plotPos));
                  if (!shape.isEmpty()) {
                     for (double[] slice : clippedBoxes(shape)) {
                        OrientedBoundingBox3d obstacle = obbForPlotBlockBox(plotPos, slice, spacePose, spaceOrientation, sink);
                        if (intersectsAny(obstacle, placedBoxes)) {
                           return true;
                        }
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private static boolean intersectsWorldBlocks(
      Level level, BlockPlaceContext context, BlockPos clicked, BoundingBox3dc queryBounds, List<OrientedBoundingBox3d> placedBoxes, LevelReusedVectors sink
   ) {
      int minX = Mth.floor(queryBounds.minX());
      int minY = Mth.floor(queryBounds.minY());
      int minZ = Mth.floor(queryBounds.minZ());
      int maxX = Mth.floor(queryBounds.maxX());
      int maxY = Mth.floor(queryBounds.maxY());
      int maxZ = Mth.floor(queryBounds.maxZ());

      for (int x = minX; x <= maxX; x++) {
         for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
               BlockPos pos = new BlockPos(x, y, z);
               if (!pos.equals(clicked) && !obstacleIgnored(level, context, pos) && Sable.HELPER.getContaining(level, pos) == null) {
                  VoxelShape shape = subLevelCollisionShape(level, level.getBlockState(pos));
                  if (!shape.isEmpty()) {
                     for (double[] slice : clippedBoxes(shape)) {
                        Vector3d center = sink.center
                           .set(
                              (double)pos.getX() + (slice[0] + slice[3]) * 0.5,
                              (double)pos.getY() + (slice[1] + slice[4]) * 0.5,
                              (double)pos.getZ() + (slice[2] + slice[5]) * 0.5
                           );
                        OrientedBoundingBox3d obstacle = new OrientedBoundingBox3d(
                           center, new Vector3d(slice[3] - slice[0], slice[4] - slice[1], slice[5] - slice[2]), IDENTITY, sink
                        );
                        if (intersectsAny(obstacle, placedBoxes)) {
                           return true;
                        }
                     }
                  }
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

   private static OrientedBoundingBox3d obbForPlotBlockBox(
      BlockPos plotPos, double[] slice, Pose3d spacePose, Quaterniondc spaceOrientation, LevelReusedVectors sink
   ) {
      Vector3d center = sink.center
         .set(
            (double)plotPos.getX() + (slice[0] + slice[3]) * 0.5,
            (double)plotPos.getY() + (slice[1] + slice[4]) * 0.5,
            (double)plotPos.getZ() + (slice[2] + slice[5]) * 0.5
         );
      spacePose.transformPosition(center);
      return new OrientedBoundingBox3d(center, new Vector3d(slice[3] - slice[0], slice[4] - slice[1], slice[5] - slice[2]), spaceOrientation, sink);
   }

   private static boolean intersectsAny(OrientedBoundingBox3d obstacle, List<OrientedBoundingBox3d> placedBoxes) {
      for (OrientedBoundingBox3d placed : placedBoxes) {
         if (OrientedBoundingBox3d.sat(obstacle, placed).lengthSquared() > 0.05) {
            return true;
         }
      }

      return false;
   }

   private static VoxelShape subLevelCollisionShape(BlockGetter getter, BlockState state) {
      return state.getBlock() instanceof BlockSubLevelCollisionShape extension
         ? extension.getSubLevelCollisionShape(getter, state)
         : state.getCollisionShape(getter, BlockPos.ZERO, SableCollisionContext.get());
   }

   private static List<double[]> clippedBoxes(VoxelShape shape) {
      CLIPPED_BOX_SCRATCH.clear();
      shape.forAllBoxes(
         (minX, minY, minZ, maxX, maxY, maxZ) -> CLIPPED_BOX_SCRATCH.add(
               new double[]{Math.max(minX, 0.0), Math.max(minY, 0.0), Math.max(minZ, 0.0), Math.min(maxX, 1.0), Math.min(maxY, 1.0), Math.min(maxZ, 1.0)}
            )
      );
      return List.copyOf(CLIPPED_BOX_SCRATCH);
   }
}
