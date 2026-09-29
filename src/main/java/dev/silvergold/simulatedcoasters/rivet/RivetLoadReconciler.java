package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class RivetLoadReconciler {
   private static final int INITIAL_DELAY_TICKS = 2;
   private static final int RETRY_TICKS = 40;
   private static final Map<ResourceKey<Level>, Integer> PENDING = new HashMap<>();

   private RivetLoadReconciler() {
   }

   public static void schedule(ServerLevel level) {
      PENDING.put(level.dimension(), 42);
   }

   public static void clear(ServerLevel level) {
      PENDING.remove(level.dimension());
   }

   public static void serverTick(MinecraftServer server) {
      if (!PENDING.isEmpty()) {
         Iterator<Entry<ResourceKey<Level>, Integer>> it = PENDING.entrySet().iterator();

         while (it.hasNext()) {
            Entry<ResourceKey<Level>, Integer> entry = it.next();
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
               it.remove();
            } else {
               int ticksLeft = entry.getValue() - 1;
               if (ticksLeft > 40) {
                  entry.setValue(ticksLeft);
               } else {
                  reconcile(level);
                  if (ticksLeft <= 0) {
                     it.remove();
                  } else {
                     entry.setValue(ticksLeft);
                  }
               }
            }
         }
      }
   }

   public static void reconcile(ServerLevel level) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         RivetHostIndex index = RivetHostIndex.get(level);

         for (SubLevel rawSub : container.getAllSubLevels()) {
            if (rawSub instanceof ServerSubLevel) {
               ServerSubLevel rivetSub = (ServerSubLevel)rawSub;
               if (!rivetSub.isRemoved()) {
                  BlockEntity actualHost = rivetSub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO);
                  if (actualHost instanceof RivetBlockEntity) {
                     RivetBlockEntity be = (RivetBlockEntity)actualHost;
                     if (rivetSub.getPlot().getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO).is((Block)SimulatedCoastersBlocks.RIVET.get())) {
                        RivetHostKey actualHostx = RivetHostKey.of(level, be.hostPos());
                        if (!actualHostx.equals(be.hostKey())) {
                           be.rebindHost(actualHostx, be.face(), be.attachment());
                        }

                        index.registerLoadedRivet(be.anchorKey(), rivetSub.getUniqueId());
                        if (be.hasInvalidHost(level, rivetSub)) {
                           RivetRemoval.onHostBroken(level, rivetSub);
                        } else {
                           RivetPoseAlignment.alignToNominal(level, container, rivetSub, be);
                           be.applyHostLock(rivetSub);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
