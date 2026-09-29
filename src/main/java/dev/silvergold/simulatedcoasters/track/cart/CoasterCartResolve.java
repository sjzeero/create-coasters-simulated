package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientMembership;
import java.util.UUID;
import net.minecraft.core.BlockPos;
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

public final class CoasterCartResolve {
   private CoasterCartResolve() {
   }

   public static boolean isCartSubLevel(@Nullable SubLevel sub) {
      if (sub != null && !sub.isRemoved()) {
         if (sub instanceof ServerSubLevel server) {
            ServerLevel level = server.getLevel();
            if (level != null && !level.isClientSide()) {
               return CoasterCartBearingTickCache.scan(server).representative() != null;
            }
         }

         return FMLEnvironment.dist == Dist.CLIENT
            ? CoasterCartResolve.ClientMembership.isCart(sub)
            : CoasterCartPlotScan.representativeBearingPlotPos(sub.getPlot(), (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get()) != null;
      } else {
         return false;
      }
   }

   @Nullable
   public static SubLevel findCartSubLevelAt(Level level, BlockPos clicked) {
      SubLevel sub = Sable.HELPER.getContaining(level, clicked);
      if (isCartSubLevel(sub) && isClickOnCartBearingBlock(level, sub, clicked)) {
         return sub;
      } else {
         BoundingBox3d query = new BoundingBox3d(
            (double)clicked.getX() - 0.25,
            (double)clicked.getY() - 0.25,
            (double)clicked.getZ() - 0.25,
            (double)clicked.getX() + 1.25,
            (double)clicked.getY() + 1.25,
            (double)clicked.getZ() + 1.25
         );

         for (SubLevel candidate : Sable.HELPER.getAllIntersecting(level, query)) {
            if (isCartSubLevel(candidate) && isClickOnCartBearingBlock(level, candidate, clicked)) {
               return candidate;
            }
         }

         return null;
      }
   }

   @Nullable
   public static SubLevel findSubLevelAt(Level level, BlockPos clicked) {
      SubLevel sub = Sable.HELPER.getContaining(level, clicked);
      if (sub != null && !sub.isRemoved() && isClickOnSubLevelBlock(level, sub, clicked)) {
         return sub;
      } else {
         BoundingBox3d query = new BoundingBox3d(
            (double)clicked.getX() - 0.25,
            (double)clicked.getY() - 0.25,
            (double)clicked.getZ() - 0.25,
            (double)clicked.getX() + 1.25,
            (double)clicked.getY() + 1.25,
            (double)clicked.getZ() + 1.25
         );

         for (SubLevel candidate : Sable.HELPER.getAllIntersecting(level, query)) {
            if (!candidate.isRemoved() && isClickOnSubLevelBlock(level, candidate, clicked)) {
               return candidate;
            }
         }

         return null;
      }
   }

   @Nullable
   public static BlockPos plotLocalCellAt(Level level, SubLevel sub, BlockPos levelPos) {
      BlockPos center = sub.getPlot().getCenterBlock();
      if (Sable.HELPER.getContaining(level, levelPos) == sub) {
         return levelPos.subtract(center);
      } else {
         Vector3d worldCenter = new Vector3d((double)levelPos.getX() + 0.5, (double)levelPos.getY() + 0.5, (double)levelPos.getZ() + 0.5);
         Vector3d plotLocal = sub.logicalPose().transformPositionInverse(worldCenter, new Vector3d());
         return !plotLocalPointInBounds(sub, plotLocal) ? null : BlockPos.containing(plotLocal.x, plotLocal.y, plotLocal.z);
      }
   }

   private static boolean isClickOnSubLevelBlock(Level level, SubLevel sub, BlockPos clicked) {
      if (Sable.HELPER.getContaining(level, clicked) == sub) {
         return !level.getBlockState(clicked).isAir();
      } else {
         Vector3d worldCenter = new Vector3d((double)clicked.getX() + 0.5, (double)clicked.getY() + 0.5, (double)clicked.getZ() + 0.5);
         Vector3d plotLocal = sub.logicalPose().transformPositionInverse(worldCenter, new Vector3d());
         if (!plotLocalPointInBounds(sub, plotLocal)) {
            return false;
         } else {
            BlockPos cell = BlockPos.containing(plotLocal.x, plotLocal.y, plotLocal.z);
            BlockState plotState = plotBlockStateAt(sub, cell);
            return plotState != null && !plotState.isAir();
         }
      }
   }

   private static boolean isClickOnCartBearingBlock(Level level, SubLevel cart, BlockPos clicked) {
      Block bearing = (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
      if (level.getBlockState(clicked).is(bearing) && Sable.HELPER.getContaining(level, clicked) == cart) {
         return true;
      } else {
         Vector3d worldCenter = new Vector3d((double)clicked.getX() + 0.5, (double)clicked.getY() + 0.5, (double)clicked.getZ() + 0.5);
         Vector3d plotLocal = cart.logicalPose().transformPositionInverse(worldCenter, new Vector3d());
         if (!plotLocalPointInBounds(cart, plotLocal)) {
            return false;
         } else {
            BlockPos cell = BlockPos.containing(plotLocal.x, plotLocal.y, plotLocal.z);
            BlockState plotState = plotBlockStateAt(cart, cell);
            return plotState != null && plotState.is(bearing);
         }
      }
   }

   private static boolean plotLocalPointInBounds(SubLevel sub, Vector3d plotLocal) {
      BoundingBox3ic bounds = sub.getPlot().getBoundingBox();
      return plotLocal.x >= (double)bounds.minX()
         && plotLocal.x <= (double)bounds.maxX()
         && plotLocal.y >= (double)bounds.minY()
         && plotLocal.y <= (double)bounds.maxY()
         && plotLocal.z >= (double)bounds.minZ()
         && plotLocal.z <= (double)bounds.maxZ();
   }

   @Nullable
   private static BlockState plotBlockStateAt(SubLevel sub, BlockPos plotLocalCell) {
      BoundingBox3ic bounds = sub.getPlot().getBoundingBox();
      if (plotLocalCell.getX() >= bounds.minX()
         && plotLocalCell.getX() <= bounds.maxX()
         && plotLocalCell.getY() >= bounds.minY()
         && plotLocalCell.getY() <= bounds.maxY()
         && plotLocalCell.getZ() >= bounds.minZ()
         && plotLocalCell.getZ() <= bounds.maxZ()) {
         LevelPlot plot = sub.getPlot();
         BlockPos world = plotLocalCell.offset(plot.getCenterBlock());
         ChunkPos globalChunk = new ChunkPos(world);
         if (!plot.contains(globalChunk)) {
            return null;
         } else {
            PlotChunkHolder holder = plot.getChunkHolder(plot.toLocal(globalChunk));
            LevelChunk chunk = holder != null ? holder.getChunk() : null;
            return chunk == null ? null : chunk.getBlockState(world);
         }
      } else {
         return null;
      }
   }

   @Nullable
   public static SubLevel findCartById(Level level, UUID cartId) {
      if (level instanceof ServerLevel server && !level.isClientSide()) {
         ServerSubLevel cart = CoasterCartIdIndex.find(server, cartId);
         if (cart != null && !cart.isRemoved() && isCartSubLevel(cart)) {
            return cart;
         }

         return null;
      }

      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw == null) {
         return null;
      } else {
         SubLevel sub = raw.getSubLevel(cartId);
         return sub != null && !sub.isRemoved() && isCartSubLevel(sub) ? sub : null;
      }
   }

   @Nullable
   public static ServerSubLevel findCartById(ServerLevel level, UUID cartId) {
      return findCartById((Level)level, cartId) instanceof ServerSubLevel server ? server : null;
   }

   @Nullable
   public static Vector3d bearingPlotCenter(SubLevel cart) {
      BlockPos bearing;
      if (cart instanceof ServerSubLevel server) {
         ServerLevel level = server.getLevel();
         if (level != null && !level.isClientSide()) {
            bearing = CoasterCartBearingTickCache.scan(server).representative();
         } else {
            bearing = CoasterCartTrackSnap.representativeBearingPlotPos(cart);
         }
      } else {
         bearing = CoasterCartTrackSnap.representativeBearingPlotPos(cart);
      }

      return bearing == null ? null : new Vector3d((double)bearing.getX() + 0.5, (double)bearing.getY() + 0.5, (double)bearing.getZ() + 0.5);
   }

   @Nullable
   public static Vector3d cartCenterWorld(SubLevel cart) {
      Vector3d local = bearingPlotCenter(cart);
      return local == null ? null : cart.logicalPose().transformPosition(local, new Vector3d());
   }

   public static void breakCartBlocksAfterAssemblyMove(ServerLevel level, AssemblyTransform transform, Iterable<BlockPos> movedSources) {
      CoasterCartDuplicateEnforcer.breakCartBlocksOnWorldGridAfterMove(level, transform, movedSources);
      CoasterCartIdIndex.invalidateDimension(level);
   }

   private static final class ClientMembership {
      static boolean isCart(SubLevel sub) {
         return CoasterCartClientMembership.isCart(sub);
      }
   }
}
