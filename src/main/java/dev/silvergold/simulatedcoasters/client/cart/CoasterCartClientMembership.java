package dev.silvergold.simulatedcoasters.client.cart;

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
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartClientMembership {
   private static final Map<UUID, CoasterCartClientMembership.Entry> BY_ID = new HashMap<>();
   private static final Set<UUID> CART_IDS = new HashSet<>();
   private static final Set<UUID> DIRTY = new HashSet<>();
   private static long reconciledGameTime = Long.MIN_VALUE;

   private CoasterCartClientMembership() {
   }

   public static void reconcile(ClientLevel level) {
      long tick = level.getGameTime();
      if (tick != reconciledGameTime || !DIRTY.isEmpty()) {
         reconciledGameTime = tick;
         SubLevelContainer container = SubLevelContainer.getContainer(level);
         if (container == null) {
            clear();
         } else {
            Set<UUID> live = new HashSet<>();
            Block bearingBlock = (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();

            for (SubLevel sub : container.getAllSubLevels()) {
               if (!sub.isRemoved()) {
                  UUID id = sub.getUniqueId();
                  live.add(id);
                  CoasterCartClientMembership.Entry existing = BY_ID.get(id);
                  int chunkCount = sub.getPlot().getLoadedChunks().size();
                  boolean needsClassify = existing == null || DIRTY.contains(id) || !existing.isCart() && existing.loadedChunkCount != chunkCount;
                  if (needsClassify) {
                     classify(sub, bearingBlock);
                  }
               }
            }

            DIRTY.clear();
            if (BY_ID.size() != live.size() || !BY_ID.keySet().containsAll(live)) {
               Iterator<Map.Entry<UUID, CoasterCartClientMembership.Entry>> it = BY_ID.entrySet().iterator();

               while (it.hasNext()) {
                  UUID id = it.next().getKey();
                  if (!live.contains(id)) {
                     it.remove();
                     CART_IDS.remove(id);
                  }
               }
            }
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
      invalidate(cartId);
   }

   public static void clear() {
      BY_ID.clear();
      CART_IDS.clear();
      DIRTY.clear();
      reconciledGameTime = Long.MIN_VALUE;
   }

   public static boolean isCart(@Nullable SubLevel sub) {
      if (sub != null && !sub.isRemoved()) {
         CoasterCartClientMembership.Entry entry = entryFor(sub);
         return entry != null && entry.isCart();
      } else {
         return false;
      }
   }

   @Nullable
   public static BlockPos representativeBearingPlotPos(@Nullable SubLevel sub) {
      if (sub != null && !sub.isRemoved()) {
         CoasterCartClientMembership.Entry entry = entryFor(sub);
         return entry != null ? entry.scan.representative() : null;
      } else {
         return null;
      }
   }

   public static CoasterCartPlotScan.BearingScanResult scan(@Nullable SubLevel sub) {
      if (sub != null && !sub.isRemoved()) {
         CoasterCartClientMembership.Entry entry = entryFor(sub);
         return entry != null ? entry.scan : CoasterCartPlotScan.BearingScanResult.empty();
      } else {
         return CoasterCartPlotScan.BearingScanResult.empty();
      }
   }

   public static void forEachCart(ClientLevel level, Consumer<SubLevel> visitor) {
      reconcile(level);
      if (!CART_IDS.isEmpty()) {
         SubLevelContainer container = SubLevelContainer.getContainer(level);
         if (container != null) {
            for (UUID id : CART_IDS) {
               SubLevel sub = container.getSubLevel(id);
               if (sub != null && !sub.isRemoved()) {
                  visitor.accept(sub);
               }
            }
         }
      }
   }

   public static void forEachBearingWorldCell(SubLevel sub, Consumer<MutableBlockPos> visitor) {
      CoasterCartClientMembership.Entry entry = entryFor(sub);
      if (entry != null && entry.isCart()) {
         MutableBlockPos mut = new MutableBlockPos();
         List<BlockPos> cells = entry.scan.cells();
         if (cells.isEmpty()) {
            CoasterCartPlotScan.forEachBearingWorldCell(sub.getPlot(), (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get(), visitor);
         } else {
            for (BlockPos cell : cells) {
               mut.set(cell.getX(), cell.getY(), cell.getZ());
               visitor.accept(mut);
            }
         }
      }
   }

   @Nullable
   private static CoasterCartClientMembership.Entry entryFor(SubLevel sub) {
      UUID id = sub.getUniqueId();
      CoasterCartClientMembership.Entry cached = BY_ID.get(id);
      return cached != null && !DIRTY.contains(id) ? cached : classify(sub, (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get());
   }

   private static CoasterCartClientMembership.Entry classify(SubLevel sub, Block bearingBlock) {
      UUID id = sub.getUniqueId();
      DIRTY.remove(id);
      int chunkCount = sub.getPlot().getLoadedChunks().size();
      CoasterCartPlotScan.BearingScanResult scan = CoasterCartPlotScan.scanBearingCells(sub.getPlot(), bearingBlock);
      if (scan.representative() == null) {
         CART_IDS.remove(id);
      } else {
         CART_IDS.add(id);
      }

      CoasterCartClientMembership.Entry entry = new CoasterCartClientMembership.Entry(scan, chunkCount);
      BY_ID.put(id, entry);
      return entry;
   }

   private static record Entry(CoasterCartPlotScan.BearingScanResult scan, int loadedChunkCount) {
      boolean isCart() {
         return this.scan.representative() != null;
      }
   }
}
