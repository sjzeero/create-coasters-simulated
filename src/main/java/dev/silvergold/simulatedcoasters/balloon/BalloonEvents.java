package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.LevelEvent.Load;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;
import net.neoforged.neoforge.event.tick.ServerTickEvent.Post;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
   modid = "simulatedcoasters"
)
public final class BalloonEvents {
   private static final Set<Level> OBSERVER_REGISTERED = ConcurrentHashMap.newKeySet();
   private static final SubLevelObserver BALLOON_OBSERVER = new SubLevelObserver() {
      public void onSubLevelAdded(SubLevel subLevel) {
         if (subLevel instanceof ServerSubLevel server) {
            if (subLevel.getLevel() instanceof ServerLevel level) {
               UUID splitFromId = server.getSplitFromSubLevel();
               if (splitFromId != null) {
                  ServerSubLevel splitFrom = BalloonEvents.subLevelById(level, splitFromId);
                  if (splitFrom != null && BalloonResolve.isBalloonSubLevel(splitFrom)) {
                     BalloonRemoval.pop(level, splitFrom);
                  } else {
                     BalloonAssembly.refreshAllBalloonHosts(level);
                  }
               }
            }
         }
      }

      public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
         if (reason == SubLevelRemovalReason.REMOVED) {
            if (subLevel.getLevel() instanceof ServerLevel level) {
               BalloonHostIndex.get(level).breakBalloonsOnHostSubLevel(level, subLevel.getUniqueId());
               BalloonHostIndex.get(level).unregisterBalloon(subLevel.getUniqueId());
               BalloonTetherController.unregister(subLevel.getUniqueId());
            }
         }
      }
   };

   private BalloonEvents() {
   }

   @SubscribeEvent
   public static void onLevelLoad(Load event) {
      if (event.getLevel() instanceof ServerLevel level) {
         SubLevelContainer raw = SubLevelContainer.getContainer(level);
         if (raw instanceof ServerSubLevelContainer) {
            ServerSubLevelContainer container = (ServerSubLevelContainer)raw;
            if (OBSERVER_REGISTERED.add(level)) {
               container.addObserver(BALLOON_OBSERVER);
               BalloonLoadReconciler.schedule(level);
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      OBSERVER_REGISTERED.remove(event.getLevel());
      if (event.getLevel() instanceof ServerLevel level) {
         BalloonLoadReconciler.clear(level);
         BalloonCascadePop.clearDimension(level.dimension());
      }
   }

   @SubscribeEvent
   public static void onServerTickPost(Post event) {
      BalloonLoadReconciler.serverTick(event.getServer());
      BalloonCascadePop.serverTick(event.getServer());
   }

   @SubscribeEvent
   public static void onHostBroken(BreakEvent event) {
      if (event.getLevel() instanceof ServerLevel level) {
         BalloonHostIndex.get(level).breakBalloonsOnHostCell(level, event.getPos());
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGH
   )
   public static void onProjectileImpact(ProjectileImpactEvent event) {
      if (event.getProjectile().level() instanceof ServerLevel level) {
         HitResult hit = event.getRayTraceResult();
         if (hit.getType() == Type.BLOCK) {
            ServerSubLevel balloon = BalloonResolve.findBalloonAtImpact(level, (BlockHitResult)hit);
            if (balloon != null) {
               BalloonRemoval.pop(level, balloon);
               event.setCanceled(true);
            }
         }
      }
   }

   @Nullable
   private static ServerSubLevel subLevelById(ServerLevel level, UUID id) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         if (container.getSubLevel(id) instanceof ServerSubLevel server && !server.isRemoved()) {
            return server;
         }

         return null;
      } else {
         return null;
      }
   }
}
