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
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.neoforged.bus.api.EventPriority
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.entity.ProjectileImpactEvent
 *  net.neoforged.neoforge.event.level.BlockEvent$BreakEvent
 *  net.neoforged.neoforge.event.level.LevelEvent$Load
 *  net.neoforged.neoforge.event.level.LevelEvent$Unload
 *  net.neoforged.neoforge.event.tick.ServerTickEvent$Post
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.silvergold.simulatedcoasters.balloon.BalloonAssembly;
import dev.silvergold.simulatedcoasters.balloon.BalloonCascadePop;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostIndex;
import dev.silvergold.simulatedcoasters.balloon.BalloonLoadReconciler;
import dev.silvergold.simulatedcoasters.balloon.BalloonRemoval;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import dev.silvergold.simulatedcoasters.balloon.BalloonTetherController;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid="simulatedcoasters")
public final class BalloonEvents {
    private static final Set<Level> OBSERVER_REGISTERED = ConcurrentHashMap.newKeySet();
    private static final SubLevelObserver BALLOON_OBSERVER = new SubLevelObserver(){

        public void onSubLevelAdded(SubLevel subLevel) {
            if (!(subLevel instanceof ServerSubLevel)) {
                return;
            }
            ServerSubLevel server = (ServerSubLevel)subLevel;
            Level level = subLevel.getLevel();
            if (!(level instanceof ServerLevel)) {
                return;
            }
            ServerLevel level2 = (ServerLevel)level;
            UUID splitFromId = server.getSplitFromSubLevel();
            if (splitFromId == null) {
                return;
            }
            ServerSubLevel splitFrom = BalloonEvents.subLevelById(level2, splitFromId);
            if (splitFrom != null && BalloonResolve.isBalloonSubLevel((SubLevel)splitFrom)) {
                BalloonRemoval.pop(level2, splitFrom);
                return;
            }
            BalloonAssembly.refreshAllBalloonHosts(level2);
        }

        public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
            if (reason != SubLevelRemovalReason.REMOVED) {
                return;
            }
            Level level = subLevel.getLevel();
            if (!(level instanceof ServerLevel)) {
                return;
            }
            ServerLevel level2 = (ServerLevel)level;
            BalloonHostIndex.get(level2).breakBalloonsOnHostSubLevel(level2, subLevel.getUniqueId());
            BalloonHostIndex.get(level2).unregisterBalloon(subLevel.getUniqueId());
            BalloonTetherController.unregister(subLevel.getUniqueId());
        }
    };

    private BalloonEvents() {
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel)levelAccessor;
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        if (!OBSERVER_REGISTERED.add((Level)level)) {
            return;
        }
        container.addObserver(BALLOON_OBSERVER);
        BalloonLoadReconciler.schedule(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        OBSERVER_REGISTERED.remove(event.getLevel());
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)levelAccessor;
            BalloonLoadReconciler.clear(level);
            BalloonCascadePop.clearDimension((ResourceKey<Level>)level.dimension());
        }
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        BalloonLoadReconciler.serverTick(event.getServer());
        BalloonCascadePop.serverTick(event.getServer());
    }

    @SubscribeEvent
    public static void onHostBroken(BlockEvent.BreakEvent event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel)levelAccessor;
        BalloonHostIndex.get(level).breakBalloonsOnHostCell(level, event.getPos());
    }

    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        Level level = event.getProjectile().level();
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel level2 = (ServerLevel)level;
        HitResult hit = event.getRayTraceResult();
        if (hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        ServerSubLevel balloon = BalloonResolve.findBalloonAtImpact(level2, (BlockHitResult)hit);
        if (balloon == null) {
            return;
        }
        BalloonRemoval.pop(level2, balloon);
        event.setCanceled(true);
    }

    @Nullable
    private static ServerSubLevel subLevelById(ServerLevel level, UUID id) {
        ServerSubLevel server;
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        SubLevel sub = container.getSubLevel(id);
        if (!(sub instanceof ServerSubLevel) || (server = (ServerSubLevel)sub).isRemoved()) {
            return null;
        }
        return server;
    }
}

