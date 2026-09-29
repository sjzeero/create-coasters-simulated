/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import dev.silvergold.simulatedcoasters.rivet.RivetHostIndex;
import dev.silvergold.simulatedcoasters.rivet.RivetHostKey;
import dev.silvergold.simulatedcoasters.rivet.RivetPoseAlignment;
import dev.silvergold.simulatedcoasters.rivet.RivetRemoval;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    private static final Map<ResourceKey<Level>, Integer> PENDING = new HashMap<ResourceKey<Level>, Integer>();

    private RivetLoadReconciler() {
    }

    public static void schedule(ServerLevel level) {
        PENDING.put((ResourceKey<Level>)level.dimension(), 42);
    }

    public static void clear(ServerLevel level) {
        PENDING.remove(level.dimension());
    }

    public static void serverTick(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<ResourceKey<Level>, Integer>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceKey<Level>, Integer> entry = it.next();
            ServerLevel level = server.getLevel(entry.getKey());
            if (level == null) {
                it.remove();
                continue;
            }
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft > 40) {
                entry.setValue(ticksLeft);
                continue;
            }
            RivetLoadReconciler.reconcile(level);
            if (ticksLeft <= 0) {
                it.remove();
                continue;
            }
            entry.setValue(ticksLeft);
        }
    }

    public static void reconcile(ServerLevel level) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        RivetHostIndex index = RivetHostIndex.get(level);
        for (SubLevel rawSub : container.getAllSubLevels()) {
            BlockEntity blockEntity;
            ServerSubLevel rivetSub;
            if (!(rawSub instanceof ServerSubLevel) || (rivetSub = (ServerSubLevel)rawSub).isRemoved() || !((blockEntity = rivetSub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO)) instanceof RivetBlockEntity)) continue;
            RivetBlockEntity be = (RivetBlockEntity)blockEntity;
            if (!rivetSub.getPlot().getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO).is((Block)SimulatedCoastersBlocks.RIVET.get())) continue;
            RivetHostKey actualHost = RivetHostKey.of((Level)level, be.hostPos());
            if (!actualHost.equals(be.hostKey())) {
                be.rebindHost(actualHost, be.face(), be.attachment());
            }
            index.registerLoadedRivet(be.anchorKey(), rivetSub.getUniqueId());
            if (be.hasInvalidHost(level, rivetSub)) {
                RivetRemoval.onHostBroken(level, rivetSub);
                continue;
            }
            RivetPoseAlignment.alignToNominal(level, container, rivetSub, be);
            be.applyHostLock(rivetSub);
        }
    }
}

