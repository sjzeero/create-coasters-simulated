package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.ryanhcode.sable.api.physics.collider.SableCollisionContext;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class BalloonPlacement {
   public static final double STRING_TIE_Y_BLOCKS = 0.0625;
   public static final double LIFT_POINT_Y_BLOCKS = 0.9375;
   public static final double INITIAL_SPAWN_HEIGHT_BLOCKS = 2.5;

   private BalloonPlacement() {
   }

   public static BlockPos plotLatticeCell(SubLevel sub, BlockPos plotLocalCell) {
      return sub.getPlot().getCenterBlock().offset(plotLocalCell);
   }

   public static Vector3d hostAnchorPlot(SubLevel hostSub, BlockPos hostPlotLocal) {
      BlockPos lattice = plotLatticeCell(hostSub, hostPlotLocal);
      VoxelShape shape = hostAttachShape(hostSub, hostPlotLocal, lattice);
      if (shape.isEmpty()) {
         return new Vector3d((double)lattice.getX() + 0.5, (double)lattice.getY() + 1.0, (double)lattice.getZ() + 0.5);
      } else {
         AABB bounds = shape.bounds();
         return new Vector3d(
            (double)lattice.getX() + (bounds.minX + bounds.maxX) * 0.5,
            (double)lattice.getY() + bounds.maxY,
            (double)lattice.getZ() + (bounds.minZ + bounds.maxZ) * 0.5
         );
      }
   }

   private static VoxelShape hostAttachShape(SubLevel hostSub, BlockPos hostPlotLocal, BlockPos lattice) {
      Level level = hostSub.getLevel();
      BlockState state = level.getBlockState(lattice);
      if (state.isAir()) {
         state = hostSub.getPlot().getEmbeddedLevelAccessor().getBlockState(hostPlotLocal);
      }

      if (state.getBlock() instanceof BlockSubLevelCollisionShape extension) {
         VoxelShape subShape = extension.getSubLevelCollisionShape(level, state);
         if (!subShape.isEmpty()) {
            return subShape;
         }
      }

      VoxelShape collision = state.getCollisionShape(level, lattice, SableCollisionContext.get());
      return !collision.isEmpty() ? collision : state.getShape(level, lattice);
   }

   public static Vector3d stringTiePlot(SubLevel balloonSub, BlockPos balloonPlotLocal) {
      BlockPos lattice = plotLatticeCell(balloonSub, balloonPlotLocal);
      return new Vector3d((double)lattice.getX() + 0.5, (double)lattice.getY() + 0.0625, (double)lattice.getZ() + 0.5);
   }

   public static Vector3d liftPointPlot(SubLevel balloonSub, BlockPos balloonPlotLocal) {
      BlockPos lattice = plotLatticeCell(balloonSub, balloonPlotLocal);
      return new Vector3d((double)lattice.getX() + 0.5, (double)lattice.getY() + 0.9375, (double)lattice.getZ() + 0.5);
   }

   public static boolean isValidHost(Level level, BlockPos clicked) {
      return resolveHostSubLevel(level, clicked) != null;
   }

   @Nullable
   public static ServerSubLevel resolveHostSubLevel(Level level, BlockPos clicked) {
      SubLevel sub = CoasterCartResolve.findSubLevelAt(level, clicked);
      if (sub == null || sub.isRemoved() || BalloonResolve.isBalloonSubLevel(sub)) {
         return null;
      } else if (sub instanceof ServerSubLevel hostSub) {
         BlockPos plotLocal = CoasterCartResolve.plotLocalCellAt(level, sub, clicked);
         if (plotLocal == null) {
            return null;
         } else {
            return hostSub.getPlot().getEmbeddedLevelAccessor().getBlockState(plotLocal).isAir() ? null : hostSub;
         }
      } else {
         return null;
      }
   }

   @Nullable
   public static Vector3d hostAnchorWorld(ServerSubLevel hostSub, BlockPos hostPlotLocal) {
      return hostSub.isRemoved() ? null : hostSub.logicalPose().transformPosition(hostAnchorPlot(hostSub, hostPlotLocal), new Vector3d());
   }

   @Nullable
   public static Vector3d desiredStringWorld(ServerSubLevel hostSub, BlockPos hostPlotLocal) {
      Vector3d hostAnchor = hostAnchorWorld(hostSub, hostPlotLocal);
      return hostAnchor == null ? null : new Vector3d(hostAnchor).add(0.0, 2.5, 0.0);
   }

   @Nullable
   public static Vector3d desiredStringWorld(Level level, BlockPos clicked) {
      ServerSubLevel hostSub = resolveHostSubLevel(level, clicked);
      if (hostSub == null) {
         return null;
      } else {
         BlockPos plotLocal = CoasterCartResolve.plotLocalCellAt(level, hostSub, clicked);
         return plotLocal == null ? null : desiredStringWorld(hostSub, plotLocal);
      }
   }

   @Nullable
   public static ServerSubLevel resolveHostSub(Level level, UUID hostSubLevelId) {
      if (level instanceof ServerLevel server) {
         ServerSubLevelContainer raw = SubLevelContainer.getContainer(server);
         if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
         } else {
            if (raw.getSubLevel(hostSubLevelId) instanceof ServerSubLevel ssl && !ssl.isRemoved()) {
               return ssl;
            }

            return null;
         }
      } else {
         return null;
      }
   }
}
