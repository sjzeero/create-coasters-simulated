/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelObserver
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.level.LevelEvent$Load
 *  net.neoforged.neoforge.event.level.LevelEvent$Unload
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBearingTickCache;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartIdIndex;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkSync;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid="simulatedcoasters")
public final class CoasterCartTrainLinkHooks {
    private static final Set<ResourceKey<Level>> OBSERVER_REGISTERED = ConcurrentHashMap.newKeySet();
    private static final SubLevelObserver TRAIN_LINK_OBSERVER = new SubLevelObserver(){

        public void onSubLevelAdded(SubLevel subLevel) {
            if (!(subLevel instanceof ServerSubLevel)) {
                return;
            }
            ServerSubLevel serverCart = (ServerSubLevel)subLevel;
            ServerLevel serverLevel = serverCart.getLevel();
            if (!(serverLevel instanceof ServerLevel)) {
                return;
            }
            ServerLevel sl = serverLevel;
            CoasterCartIdIndex.invalidateDimension(sl);
            sl.getServer().execute(() -> {
                if (serverCart.isRemoved() || !CoasterCartResolve.isCartSubLevel((SubLevel)serverCart)) {
                    return;
                }
                CoasterCartTrainLinkConstraint.tryRestoreLink(serverCart, sl);
                CoasterCartTrainLinkSync.broadcastCart(serverCart);
            });
        }

        public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
            if (reason != SubLevelRemovalReason.REMOVED) {
                return;
            }
            if (!(subLevel instanceof ServerSubLevel)) {
                return;
            }
            ServerSubLevel serverCart = (ServerSubLevel)subLevel;
            if (!CoasterCartResolve.isCartSubLevel((SubLevel)serverCart)) {
                return;
            }
            CoasterCartTrainLinkConstraint.onCartRemoved(serverCart);
        }
    };

    private CoasterCartTrainLinkHooks() {
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)levelAccessor;
        ServerSubLevelContainer c = SubLevelContainer.getContainer((ServerLevel)sl);
        if (!(c instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer ssc = c;
        if (!OBSERVER_REGISTERED.add((ResourceKey<Level>)sl.dimension())) {
            return;
        }
        ssc.addObserver(TRAIN_LINK_OBSERVER);
        CoasterCartTrainLinkConstraint.restorePersistedLinks(sl);
        sl.getServer().execute(() -> CoasterCartTrainLinkSync.broadcastAllCartsInDimension(sl));
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)levelAccessor;
        CoasterCartTrainLinkConstraint.clearDimension((ResourceKey<Level>)sl.dimension());
        CoasterCartBearingTickCache.clearDimension(sl);
        CoasterCartIdIndex.clearDimension((ResourceKey<Level>)sl.dimension());
        OBSERVER_REGISTERED.remove(sl.dimension());
    }
}

