package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class RivetRemoval {
   private static final Set<UUID> REMOVING = ConcurrentHashMap.newKeySet();

   private RivetRemoval() {
   }

   public static boolean isRemoving(UUID rivetSubLevelId) {
      return REMOVING.contains(rivetSubLevelId);
   }

   public static void onHostBroken(ServerLevel level, ServerSubLevel rivetSub) {
      if (!rivetSub.isRemoved()) {
         if (REMOVING.add(rivetSub.getUniqueId())) {
            try {
               detachAndRemove(level, rivetSub, true);
            } finally {
               REMOVING.remove(rivetSub.getUniqueId());
            }
         }
      }
   }

   public static void onRivetBlockRemovedDuringMove(ServerLevel level, ServerSubLevel rivetSub) {
      if (!rivetSub.isRemoved() && !isRemoving(rivetSub.getUniqueId())) {
         RivetBlockEntity be = rivetBlockEntity(rivetSub);
         if (be != null) {
            be.removeWorldLock();
            RivetHostIndex.get(level).unregisterRivet(rivetSub.getUniqueId());
         }

         if (!rivetSub.isRemoved()) {
            rivetSub.markRemoved();
         }
      }
   }

   public static void onRivetBlockRemoved(ServerLevel level, ServerSubLevel rivetSub) {
      if (RivetAssemblyMoveContext.isActive()) {
         onRivetBlockRemovedDuringMove(level, rivetSub);
      } else if (!rivetSub.isRemoved() && !isRemoving(rivetSub.getUniqueId())) {
         detachAndRemove(level, rivetSub, false);
      }
   }

   private static void detachAndRemove(ServerLevel level, ServerSubLevel rivetSub, boolean breakRivetBlock) {
      if (!rivetSub.isRemoved()) {
         RivetBlockEntity be = rivetBlockEntity(rivetSub);
         if (be != null) {
            be.removeWorldLock();
            RivetHostIndex.get(level).unregisterRivet(rivetSub.getUniqueId());
         }

         RivetDisassembly.splitAttachedBlocks(level, rivetSub);
         RivetAssembly.refreshAllRivetHostLocks(level);
         if (breakRivetBlock) {
            BlockPos rivetCell = rivetSub.getPlot().getCenterBlock();
            BlockState rivetState = level.getBlockState(rivetCell);
            if (rivetState.is((Block)SimulatedCoastersBlocks.RIVET.get())) {
               BlockEntity blockEntity = level.getBlockEntity(rivetCell);
               RivetDrop.spawn(level, rivetCell, rivetState, blockEntity, null, ItemStack.EMPTY);
               level.setBlock(rivetCell, Blocks.AIR.defaultBlockState(), 3);
            }
         }

         if (!rivetSub.isRemoved()) {
            rivetSub.markRemoved();
         }
      }
   }

   @Nullable
   private static RivetBlockEntity rivetBlockEntity(ServerSubLevel rivetSub) {
      EmbeddedPlotLevelAccessor embedded = rivetSub.getPlot().getEmbeddedLevelAccessor();
      BlockEntity var3 = embedded.getBlockEntity(BlockPos.ZERO);
      return var3 instanceof RivetBlockEntity ? (RivetBlockEntity)var3 : null;
   }
}
