package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class BalloonLoadReconciler {
   private static final int INITIAL_DELAY_TICKS = 2;
   private static final int RETRY_TICKS = 40;
   private static final Map<ResourceKey<Level>, Integer> PENDING = new HashMap<>();

   private BalloonLoadReconciler() {
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
      reconcile(level, true);
   }

   public static void reconcile(ServerLevel level, boolean snapPose) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         for (SubLevel rawSub : container.getAllSubLevels()) {
            if (rawSub instanceof ServerSubLevel) {
               ServerSubLevel balloonSub = (ServerSubLevel)rawSub;
               if (!balloonSub.isRemoved() && BalloonResolve.isBalloonSubLevel(balloonSub)) {
                  reconcileBalloon(level, container, balloonSub, snapPose);
               }
            }
         }
      }
   }

   public static void reconcileBalloon(ServerLevel level, ServerSubLevel balloonSub) {
      reconcileBalloon(level, balloonSub, false);
   }

   public static void reconcileBalloon(ServerLevel level, ServerSubLevel balloonSub, boolean snapPose) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         reconcileBalloon(level, container, balloonSub, snapPose);
      }
   }

   public static void reconcileBalloon(ServerLevel level, ServerSubLevelContainer container, ServerSubLevel balloonSub) {
      reconcileBalloon(level, container, balloonSub, false);
   }

   public static void reconcileBalloon(ServerLevel level, ServerSubLevelContainer container, ServerSubLevel balloonSub, boolean snapPose) {
      if (!balloonSub.isRemoved() && BalloonResolve.isBalloonSubLevel(balloonSub)) {
         if (balloonSub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof BalloonBlockEntity be) {
            BalloonHostIndex var8 = BalloonHostIndex.get(level);
            BalloonHostKey hostKey = BalloonHostKey.relocateHostKey(level, be, balloonSub);
            if (hostKey != null) {
               if (!hostKey.equals(be.hostKey())) {
                  be.rebindHost(hostKey);
               } else {
                  be.refreshHostLattice(level, hostKey);
               }

               var8.registerLoaded(be.hostKey(), balloonSub.getUniqueId());
               ServerSubLevel hostSub = BalloonPlacement.resolveHostSub(level, be.hostSubLevelId());
               if (hostSub != null) {
                  if (snapPose) {
                     BalloonPoseAlignment.alignToNominal(level, container, balloonSub, hostSub, hostKey.cell());
                  }

                  BalloonTetherController.register(balloonSub, be);
               }
            }
         }
      }
   }
}
