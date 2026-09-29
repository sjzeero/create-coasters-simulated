/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.world.level.block.Block
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlotScan;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartClientMembership {
    private static final Map<UUID, Entry> BY_ID = new HashMap<UUID, Entry>();
    private static final Set<UUID> CART_IDS = new HashSet<UUID>();
    private static final Set<UUID> DIRTY = new HashSet<UUID>();
    private static long reconciledGameTime = Long.MIN_VALUE;

    private CoasterCartClientMembership() {
    }

    public static void reconcile(ClientLevel level) {
        long tick = level.getGameTime();
        if (tick == reconciledGameTime && DIRTY.isEmpty()) {
            return;
        }
        reconciledGameTime = tick;
        ClientSubLevelContainer container = SubLevelContainer.getContainer((ClientLevel)level);
        if (container == null) {
            CoasterCartClientMembership.clear();
            return;
        }
        HashSet<UUID> live = new HashSet<UUID>();
        Block bearingBlock = (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
        for (SubLevel sub : container.getAllSubLevels()) {
            if (sub.isRemoved()) continue;
            UUID id = sub.getUniqueId();
            live.add(id);
            Entry existing = BY_ID.get(id);
            int chunkCount = sub.getPlot().getLoadedChunks().size();
            boolean needsClassify = existing == null || DIRTY.contains(id) || !existing.isCart() && existing.loadedChunkCount != chunkCount;
            if (!needsClassify) continue;
            CoasterCartClientMembership.classify(sub, bearingBlock);
        }
        DIRTY.clear();
        if (BY_ID.size() != live.size() || !BY_ID.keySet().containsAll(live)) {
            Iterator<Map.Entry<UUID, Entry>> it = BY_ID.entrySet().iterator();
            while (it.hasNext()) {
                UUID id = it.next().getKey();
                if (live.contains(id)) continue;
                it.remove();
                CART_IDS.remove(id);
            }
        }
    }

    public static void invalidate(UUID cartId) {
        DIRTY.add(cartId);
        BY_ID.remove(cartId);
        CART_IDS.remove(cartId);
        reconciledGameTime = Long.MIN_VALUE;
    }

    public static void noteCartSignal(UUID cartId) {
        CoasterCartClientMembership.invalidate(cartId);
    }

    public static void clear() {
        BY_ID.clear();
        CART_IDS.clear();
        DIRTY.clear();
        reconciledGameTime = Long.MIN_VALUE;
    }

    public static boolean isCart(@Nullable SubLevel sub) {
        if (sub == null || sub.isRemoved()) {
            return false;
        }
        Entry entry = CoasterCartClientMembership.entryFor(sub);
        return entry != null && entry.isCart();
    }

    @Nullable
    public static BlockPos representativeBearingPlotPos(@Nullable SubLevel sub) {
        if (sub == null || sub.isRemoved()) {
            return null;
        }
        Entry entry = CoasterCartClientMembership.entryFor(sub);
        return entry != null ? entry.scan.representative() : null;
    }

    public static CoasterCartPlotScan.BearingScanResult scan(@Nullable SubLevel sub) {
        if (sub == null || sub.isRemoved()) {
            return CoasterCartPlotScan.BearingScanResult.empty();
        }
        Entry entry = CoasterCartClientMembership.entryFor(sub);
        return entry != null ? entry.scan : CoasterCartPlotScan.BearingScanResult.empty();
    }

    public static void forEachCart(ClientLevel level, Consumer<SubLevel> visitor) {
        CoasterCartClientMembership.reconcile(level);
        if (CART_IDS.isEmpty()) {
            return;
        }
        ClientSubLevelContainer container = SubLevelContainer.getContainer((ClientLevel)level);
        if (container == null) {
            return;
        }
        for (UUID id : CART_IDS) {
            SubLevel sub = container.getSubLevel(id);
            if (sub == null || sub.isRemoved()) continue;
            visitor.accept(sub);
        }
    }

    public static void forEachBearingWorldCell(SubLevel sub, Consumer<BlockPos.MutableBlockPos> visitor) {
        Entry entry = CoasterCartClientMembership.entryFor(sub);
        if (entry == null || !entry.isCart()) {
            return;
        }
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        List<BlockPos> cells = entry.scan.cells();
        if (!cells.isEmpty()) {
            for (BlockPos cell : cells) {
                mut.set(cell.getX(), cell.getY(), cell.getZ());
                visitor.accept(mut);
            }
            return;
        }
        CoasterCartPlotScan.forEachBearingWorldCell(sub.getPlot(), (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get(), visitor);
    }

    @Nullable
    private static Entry entryFor(SubLevel sub) {
        UUID id = sub.getUniqueId();
        Entry cached = BY_ID.get(id);
        if (cached != null && !DIRTY.contains(id)) {
            return cached;
        }
        return CoasterCartClientMembership.classify(sub, (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get());
    }

    private static Entry classify(SubLevel sub, Block bearingBlock) {
        UUID id = sub.getUniqueId();
        DIRTY.remove(id);
        int chunkCount = sub.getPlot().getLoadedChunks().size();
        CoasterCartPlotScan.BearingScanResult scan = CoasterCartPlotScan.scanBearingCells(sub.getPlot(), bearingBlock);
        if (scan.representative() == null) {
            CART_IDS.remove(id);
        } else {
            CART_IDS.add(id);
        }
        Entry entry = new Entry(scan, chunkCount);
        BY_ID.put(id, entry);
        return entry;
    }

    private record Entry(CoasterCartPlotScan.BearingScanResult scan, int loadedChunkCount) {
        boolean isCart() {
            return this.scan.representative() != null;
        }
    }
}

