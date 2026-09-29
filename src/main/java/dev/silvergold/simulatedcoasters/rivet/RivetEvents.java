/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelObserver
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.common.EventBusSubscriber
 *  net.neoforged.neoforge.event.level.BlockEvent$BreakEvent
 *  net.neoforged.neoforge.event.level.BlockEvent$EntityPlaceEvent
 *  net.neoforged.neoforge.event.level.BlockEvent$NeighborNotifyEvent
 *  net.neoforged.neoforge.event.level.LevelEvent$Load
 *  net.neoforged.neoforge.event.level.LevelEvent$Unload
 *  net.neoforged.neoforge.event.tick.ServerTickEvent$Post
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.rivet.RivetAssemblyMoveContext;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.rivet.RivetHostIndex;
import dev.silvergold.simulatedcoasters.rivet.RivetLoadReconciler;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid="simulatedcoasters")
public final class RivetEvents {
    private static final Set<Level> OBSERVER_REGISTERED = ConcurrentHashMap.newKeySet();
    private static final SubLevelObserver RIVET_REMOVAL = new SubLevelObserver(){

        public void onSubLevelRemoved(SubLevel subLevel, SubLevelRemovalReason reason) {
            if (reason != SubLevelRemovalReason.REMOVED) {
                return;
            }
            Level level = subLevel.getLevel();
            if (!(level instanceof ServerLevel)) {
                return;
            }
            ServerLevel level2 = (ServerLevel)level;
            RivetHostIndex.get(level2).unregisterRivet(subLevel.getUniqueId());
        }
    };

    private RivetEvents() {
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
        container.addObserver(RIVET_REMOVAL);
        RivetLoadReconciler.schedule(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        OBSERVER_REGISTERED.remove(event.getLevel());
        LevelAccessor levelAccessor = event.getLevel();
        if (levelAccessor instanceof ServerLevel) {
            ServerLevel level = (ServerLevel)levelAccessor;
            RivetLoadReconciler.clear(level);
        }
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        RivetLoadReconciler.serverTick(event.getServer());
    }

    @SubscribeEvent
    public static void onHostBroken(BlockEvent.BreakEvent event) {
        if (RivetAssemblyMoveContext.isActive()) {
            return;
        }
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel)levelAccessor;
        RivetHostIndex.get(level).breakRivetsOnHostCell(level, event.getPos());
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (RivetAssemblyMoveContext.isActive()) {
            return;
        }
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel)levelAccessor;
        RivetHostIndex.get(level).breakRivetsOnHostCell(level, event.getPos());
        RivetEvents.refreshRivetWorldLock((LevelAccessor)level, event.getPos());
    }

    @SubscribeEvent
    public static void onHostCellEmptied(BlockEvent.NeighborNotifyEvent event) {
        if (RivetAssemblyMoveContext.isActive()) {
            return;
        }
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel)levelAccessor;
        if (!level.getBlockState(event.getPos()).isAir()) {
            return;
        }
        RivetHostIndex.get(level).breakRivetsOnHostCell(level, event.getPos());
    }

    @SubscribeEvent
    public static void onRivetPlotBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getState().is((Block)SimulatedCoastersBlocks.RIVET.get())) {
            return;
        }
        RivetEvents.refreshRivetWorldLock(event.getLevel(), event.getPos());
    }

    private static void refreshRivetWorldLock(LevelAccessor level, BlockPos changedPos) {
        ServerSubLevel ssl;
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        SubLevel sub = Sable.HELPER.getContaining((Level)serverLevel, (Vec3i)changedPos);
        if (!(sub instanceof ServerSubLevel) || (ssl = (ServerSubLevel)sub).isRemoved()) {
            return;
        }
        BlockEntity blockEntity = ssl.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO);
        if (!(blockEntity instanceof RivetBlockEntity)) {
            return;
        }
        RivetBlockEntity be = (RivetBlockEntity)blockEntity;
        be.applyHostLock(ssl);
    }
}

