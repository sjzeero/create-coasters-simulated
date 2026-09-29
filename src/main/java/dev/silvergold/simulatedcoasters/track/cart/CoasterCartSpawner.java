package dev.silvergold.simulatedcoasters.track.cart;

import com.simibubi.create.AllBlocks;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTangentOrientation;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;

public final class CoasterCartSpawner {
   private CoasterCartSpawner() {
   }

   @Nullable
   public static ServerSubLevel spawnMinimalContraption(ServerLevel level, Vec3 plotOriginWorld, @Nullable Quaterniond trackOrientation) {
      return spawnMinimalContraption(level, plotOriginWorld, trackOrientation, null);
   }

   @Nullable
   public static ServerSubLevel spawnMinimalContraption(
      ServerLevel level, Vec3 plotOriginWorld, @Nullable Quaterniond trackOrientation, @Nullable CoasterPathTrackFrame.GraphHit initialSnapHit
   ) {
      return spawnMinimalContraption(level, plotOriginWorld, trackOrientation, initialSnapHit, initialSnapHit != null);
   }

   @Nullable
   public static ServerSubLevel spawnMinimalContraption(
      ServerLevel level,
      Vec3 plotOriginWorld,
      @Nullable Quaterniond trackOrientation,
      @Nullable CoasterPathTrackFrame.GraphHit initialSnapHit,
      boolean applyInitialTrackSnap
   ) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         try {
            CoasterPathGraph graph = CoasterPathGraphManager.get(level);
            CoasterPathTangentOrientation.alignConnectedComponents(level, graph);
            Quaterniond orientation;
            if (trackOrientation == null) {
               orientation = CoasterPathTrackFrame.quaternionWorldFromTrackAt(level, graph, plotOriginWorld, 6.25);
            } else {
               orientation = new Quaterniond(trackOrientation);
            }

            Vec3 logicalPosition = CoasterCartLinkPlacement.bearingCenterWorld(plotOriginWorld, orientation);
            Pose3d pose = new Pose3d();
            pose.position().set(logicalPosition.x, logicalPosition.y, logicalPosition.z);
            pose.orientation().set(orientation);
            SubLevel sub = container.allocateNewSubLevel(pose);
            LevelPlot plot = sub.getPlot();
            ChunkPos center = plot.getCenterChunk();
            plot.newEmptyChunk(center);
            EmbeddedPlotLevelAccessor embedded = plot.getEmbeddedLevelAccessor();
            if (!embedded.setBlock(BlockPos.ZERO, ((CoasterCartBlock)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get()).defaultBlockState(), 3)) {
               ((ServerSubLevel)sub).markRemoved();
               return null;
            } else {
               BlockPos plotAnchor = plot.getCenterBlock();
               sub.logicalPose().rotationPoint().set((double)plotAnchor.getX() + 0.5, (double)plotAnchor.getY() + 0.5, (double)plotAnchor.getZ() + 0.5);
               sub.logicalPose().position().set(logicalPosition.x, logicalPosition.y, logicalPosition.z);
               sub.logicalPose().orientation().set(orientation);
               sub.updateLastPose();
               container.physicsSystem().getPipeline().onStatsChanged((ServerSubLevel)sub);
               if (applyInitialTrackSnap && initialSnapHit != null) {
                  CoasterCartTrackSnap.applyInitialSnapAfterSpawn((ServerSubLevel)sub, level, initialSnapHit);
               }

               return (ServerSubLevel)sub;
            }
         } catch (IllegalStateException var16) {
            return null;
         }
      } else {
         return null;
      }
   }

   @Nullable
   public static ServerSubLevel retrofitSubLevelAsCart(ServerSubLevel sub, Level level, BlockPos levelPlacePos) {
      if (CoasterCartResolve.isCartSubLevel(sub)) {
         return null;
      } else {
         BlockPos plotCell = CoasterCartResolve.plotLocalCellAt(level, sub, levelPlacePos);
         if (plotCell == null) {
            return null;
         } else {
            SubLevelContainer raw = SubLevelContainer.getContainer(sub.getLevel());
            if (raw instanceof ServerSubLevelContainer container) {
               EmbeddedPlotLevelAccessor embedded = sub.getPlot().getEmbeddedLevelAccessor();
               BlockState existing = embedded.getBlockState(plotCell);
               if (!existing.isAir() && !existing.canBeReplaced()) {
                  return null;
               } else {
                  ensurePlotChunk(sub, plotCell);
                  if (!embedded.setBlock(plotCell, ((CoasterCartBlock)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get()).defaultBlockState(), 3)) {
                     return null;
                  } else {
                     CoasterCartBearingTickCache.invalidate(sub);
                     CoasterCartIdIndex.invalidateDimension((ServerLevel)level);
                     container.physicsSystem().getPipeline().onStatsChanged(sub);
                     CoasterCartTrackSnap.markSpawnedDisengaged(sub);
                     CoasterCartTrainLinkConstraint.pruneDefunctLinks((ServerLevel)level, sub);
                     CoasterCartTrainLinkSync.broadcastCart(sub);
                     playSubLevelRetrofitPlaceSound(level, levelPlacePos);
                     return sub;
                  }
               }
            } else {
               return null;
            }
         }
      }
   }

   private static void playSubLevelRetrofitPlaceSound(Level level, BlockPos levelPos) {
      SoundType sound = AllBlocks.INDUSTRIAL_IRON_BLOCK.getDefaultState().getSoundType(level, levelPos, null);
      level.playSound(null, levelPos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch());
   }

   private static void ensurePlotChunk(ServerSubLevel sub, BlockPos plotLocal) {
      LevelPlot plot = sub.getPlot();
      BlockPos world = plotLocal.offset(plot.getCenterBlock());
      ChunkPos globalChunk = new ChunkPos(world);
      if (plot.getChunkHolder(plot.toLocal(globalChunk)) == null) {
         plot.newEmptyChunk(globalChunk);
      }
   }
}
