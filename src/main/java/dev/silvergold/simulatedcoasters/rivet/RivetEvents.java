package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import net.neoforged.neoforge.event.level.BlockEvent.NeighborNotifyEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Load;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;

@EventBusSubscriber(
   modid = "simulatedcoasters"
)
public final class RivetEvents {
   private static final Set<Level> OBSERVER_REGISTERED = ConcurrentHashMap.newKeySet();
   private static final SubLevelObserver RIVET_REMOVAL = new SubLevelObserver() {
      public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
         if (reason == SubLevelRemovalReason.REMOVED) {
            if (subLevel.getLevel() instanceof ServerLevel level) {
               RivetHostIndex.get(level).unregisterRivet(subLevel.getUniqueId());
            }
         }
      }
   };

   private RivetEvents() {
   }

   @SubscribeEvent
   public static void onLevelLoad(Load event) {
      if (event.getLevel() instanceof ServerLevel level) {
         SubLevelContainer raw = SubLevelContainer.getContainer(level);
         if (raw instanceof ServerSubLevelContainer) {
            ServerSubLevelContainer container = (ServerSubLevelContainer)raw;
            if (OBSERVER_REGISTERED.add(level)) {
               container.addObserver(RIVET_REMOVAL);
               RivetLoadReconciler.schedule(level);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      OBSERVER_REGISTERED.remove(event.getLevel());
      if (event.getLevel() instanceof ServerLevel level) {
         RivetLoadReconciler.clear(level);
      }
   }

   @SubscribeEvent
   public static void onServerTickPost(Post event) {
      RivetLoadReconciler.serverTick(event.getServer());
   }

   @SubscribeEvent
   public static void onHostBroken(BreakEvent event) {
      if (!RivetAssemblyMoveContext.isActive()) {
         if (event.getLevel() instanceof ServerLevel level) {
            RivetHostIndex.get(level).breakRivetsOnHostCell(level, event.getPos());
         }
      }
   }

   @SubscribeEvent
   public static void onBlockPlaced(EntityPlaceEvent event) {
      if (!RivetAssemblyMoveContext.isActive()) {
         if (event.getLevel() instanceof ServerLevel level) {
            RivetHostIndex.get(level).breakRivetsOnHostCell(level, event.getPos());
            refreshRivetWorldLock(level, event.getPos());
         }
      }
   }

   @SubscribeEvent
   public static void onHostCellEmptied(NeighborNotifyEvent event) {
      if (!RivetAssemblyMoveContext.isActive()) {
         if (event.getLevel() instanceof ServerLevel level) {
            if (level.getBlockState(event.getPos()).isAir()) {
               RivetHostIndex.get(level).breakRivetsOnHostCell(level, event.getPos());
            }
         }
      }
   }

   @SubscribeEvent
   public static void onRivetPlotBlockBroken(BreakEvent event) {
      if (!event.getState().is((Block)SimulatedCoastersBlocks.RIVET.get())) {
         refreshRivetWorldLock(event.getLevel(), event.getPos());
      }
   }

   private static void refreshRivetWorldLock(LevelAccessor level, BlockPos changedPos) {
      if (level instanceof ServerLevel serverLevel) {
         if (!(Sable.HELPER.getContaining(serverLevel, changedPos) instanceof ServerSubLevel ssl) || ssl.isRemoved()) {
            return;
         }

         if (ssl.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof RivetBlockEntity be) {
            be.applyHostLock(ssl);
            return;
         }
      }
   }
}
