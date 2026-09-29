package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

final class CoasterCartBearingTickCache {
   private static final Map<ServerSubLevel, CoasterCartBearingTickCache.Entry> BY_SUB = Collections.synchronizedMap(new IdentityHashMap<>());

   private CoasterCartBearingTickCache() {
   }

   static CoasterCartPlotScan.BearingScanResult scan(ServerSubLevel sub) {
      ServerLevel level = sub.getLevel();
      if (level == null) {
         return CoasterCartPlotScan.BearingScanResult.empty();
      } else {
         long gameTime = level.getGameTime();
         CoasterCartBearingTickCache.Entry cached = BY_SUB.get(sub);
         if (cached != null) {
            long ttl = cached.scan.bearingCount() > 0 ? 40L : 100L;
            if (gameTime - cached.gameTime < ttl && gameTime >= cached.gameTime) {
               return cached.scan;
            }
         }
         CoasterCartPlotScan.BearingScanResult scan = CoasterCartPlotScan.scanBearingCells(
            sub.getPlot(), (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get()
         );
         BY_SUB.put(sub, new CoasterCartBearingTickCache.Entry(gameTime, scan));
         return scan;
      }
   }

   static void invalidate(ServerSubLevel sub) {
      BY_SUB.remove(sub);
   }

   static void remove(ServerSubLevel sub) {
      BY_SUB.remove(sub);
   }

   static void clearDimension(ServerLevel level) {
      synchronized (BY_SUB) {
         BY_SUB.keySet().removeIf(sub -> sub.getLevel() == level);
      }
   }

   private static record Entry(long gameTime, CoasterCartPlotScan.BearingScanResult scan) {
   }
}
