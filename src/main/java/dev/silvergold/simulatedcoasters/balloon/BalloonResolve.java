package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class BalloonResolve {
   private BalloonResolve() {
   }

   public static boolean isBalloonSubLevel(SubLevel sub) {
      return sub.getPlot().getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO).is((Block)SimulatedCoastersBlocks.RED_BALLOON.get());
   }

   public static boolean targetsBalloonCell(Level level, BlockPos worldCell) {
      SubLevel sub = Sable.HELPER.getContaining(level, worldCell);
      if (sub == null || sub.isRemoved()) {
         return false;
      } else {
         return !isBalloonSubLevel(sub) ? false : sub.getPlot().getCenterBlock().equals(worldCell);
      }
   }

   @Nullable
   public static ServerSubLevel findBalloonAt(ServerLevel level, BlockPos worldCell) {
      if (!(Sable.HELPER.getContaining(level, worldCell) instanceof ServerSubLevel server) || server.isRemoved()) {
         return null;
      }

      if (!isBalloonSubLevel(server)) {
         return null;
      } else {
         return !server.getPlot().getCenterBlock().equals(worldCell) ? null : server;
      }
   }

   @Nullable
   public static ServerSubLevel findBalloonAtImpact(ServerLevel level, BlockHitResult hit) {
      ServerSubLevel balloon = findBalloonAt(level, hit.getBlockPos());
      if (balloon != null) {
         return balloon;
      } else {
         balloon = findBalloonAt(level, BlockPos.containing(hit.getLocation()));
         if (balloon != null) {
            return balloon;
         } else {
            if (Sable.HELPER.getContaining(level, hit.getLocation()) instanceof ServerSubLevel server && !server.isRemoved() && isBalloonSubLevel(server)) {
               return server;
            }

            return null;
         }
      }
   }

   public static boolean isBalloonBlockState(BlockState state) {
      return state.is((Block)SimulatedCoastersBlocks.RED_BALLOON.get());
   }

   @Nullable
   public static ServerSubLevel findBalloonById(ServerLevel level, UUID id) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         if (container.getSubLevel(id) instanceof ServerSubLevel server && !server.isRemoved() && isBalloonSubLevel(server)) {
            return server;
         }

         return null;
      } else {
         return null;
      }
   }
}
