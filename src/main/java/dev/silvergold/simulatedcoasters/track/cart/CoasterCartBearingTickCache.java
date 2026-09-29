/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.plot.LevelPlot
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.Block
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlotScan;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

final class CoasterCartBearingTickCache {
    private static final Map<ServerSubLevel, Entry> BY_SUB = Collections.synchronizedMap(new IdentityHashMap());

    private CoasterCartBearingTickCache() {
    }

    static CoasterCartPlotScan.BearingScanResult scan(ServerSubLevel sub) {
        ServerLevel level = sub.getLevel();
        if (level == null) {
            return CoasterCartPlotScan.BearingScanResult.empty();
        }
        long gameTime = level.getGameTime();
        Entry cached = BY_SUB.get(sub);
        if (cached != null && cached.gameTime == gameTime) {
            return cached.scan;
        }
        CoasterCartPlotScan.BearingScanResult scan = CoasterCartPlotScan.scanBearingCells((LevelPlot)sub.getPlot(), (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get());
        BY_SUB.put(sub, new Entry(gameTime, scan));
        return scan;
    }

    static void invalidate(ServerSubLevel sub) {
        BY_SUB.remove(sub);
    }

    static void remove(ServerSubLevel sub) {
        BY_SUB.remove(sub);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void clearDimension(ServerLevel level) {
        Map<ServerSubLevel, Entry> map = BY_SUB;
        synchronized (map) {
            BY_SUB.keySet().removeIf(sub -> sub.getLevel() == level);
        }
    }

    private record Entry(long gameTime, CoasterCartPlotScan.BearingScanResult scan) {
    }
}

