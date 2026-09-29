package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent.Load;
import net.neoforged.neoforge.event.level.LevelEvent.Unload;

@EventBusSubscriber(
   modid = "simulatedcoasters"
)
public final class CoasterCartTrainLinkHooks {
   private static final Set<ResourceKey<Level>> OBSERVER_REGISTERED = ConcurrentHashMap.newKeySet();
   private static final SubLevelObserver TRAIN_LINK_OBSERVER = new SubLevelObserver() {
      public void onSubLevelAdded(SubLevel subLevel) {
         if (subLevel instanceof ServerSubLevel serverCart) {
            ServerLevel var4 = serverCart.getLevel();
            if (var4 instanceof ServerLevel) {
               CoasterCartIdIndex.invalidateDimension(var4);
               var4.getServer().execute(() -> {
                  if (!serverCart.isRemoved() && CoasterCartResolve.isCartSubLevel(serverCart)) {
                     CoasterCartTrainLinkConstraint.tryRestoreLink(serverCart, var4);
                     CoasterCartTrainLinkSync.broadcastCart(serverCart);
                  }
               });
            }
         }
      }

      public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
         if (reason == SubLevelRemovalReason.REMOVED) {
            if (subLevel instanceof ServerSubLevel serverCart) {
               if (CoasterCartResolve.isCartSubLevel(serverCart)) {
                  CoasterCartTrainLinkConstraint.onCartRemoved(serverCart);
               }
            }
         }
      }
   };

   private CoasterCartTrainLinkHooks() {
   }

   @SubscribeEvent
   public static void onLevelLoad(Load event) {
      if (event.getLevel() instanceof ServerLevel sl) {
         SubLevelContainer c = SubLevelContainer.getContainer(sl);
         if (c instanceof ServerSubLevelContainer) {
            ServerSubLevelContainer ssc = (ServerSubLevelContainer)c;
            if (OBSERVER_REGISTERED.add(sl.dimension())) {
               ssc.addObserver(TRAIN_LINK_OBSERVER);
               CoasterCartTrainLinkConstraint.restorePersistedLinks(sl);
               sl.getServer().execute(() -> CoasterCartTrainLinkSync.broadcastAllCartsInDimension(sl));
            }
         }
      }
   }

   @SubscribeEvent
   public static void onLevelUnload(Unload event) {
      if (event.getLevel() instanceof ServerLevel sl) {
         CoasterCartTrainLinkConstraint.clearDimension(sl.dimension());
         CoasterCartBearingTickCache.clearDimension(sl);
         CoasterCartIdIndex.clearDimension(sl.dimension());
         OBSERVER_REGISTERED.remove(sl.dimension());
      }
   }
}
