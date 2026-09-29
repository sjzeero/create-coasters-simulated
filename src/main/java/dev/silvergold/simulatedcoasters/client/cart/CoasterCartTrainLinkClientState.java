package dev.silvergold.simulatedcoasters.client.cart;

import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkSync;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartTrainLinkClientState {
   private static final Map<ResourceKey<Level>, Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>>> LINKS_BY_DIMENSION = new ConcurrentHashMap<>();

   private CoasterCartTrainLinkClientState() {
   }

   public static void applySync(ResourceKey<Level> dimension, UUID cartId, List<CoasterCartTrainLinkSync.LinkEntry> links) {
      CoasterCartClientMembership.noteCartSignal(cartId);
      if (links.isEmpty()) {
         Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>> dim = LINKS_BY_DIMENSION.get(dimension);
         if (dim != null) {
            dim.remove(cartId);
         }
      } else {
         LINKS_BY_DIMENSION.computeIfAbsent(dimension, d -> new ConcurrentHashMap<>()).put(cartId, List.copyOf(links));
      }
   }

   public static List<CoasterCartTrainLinkSync.LinkEntry> linksFor(ResourceKey<Level> dimension, UUID cartId) {
      Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>> dim = LINKS_BY_DIMENSION.get(dimension);
      return dim == null ? List.of() : dim.getOrDefault(cartId, List.of());
   }

   public static boolean hasLinkBetween(ResourceKey<Level> dimension, UUID cartA, UUID cartB) {
      if (cartA.equals(cartB)) {
         return true;
      } else {
         for (CoasterCartTrainLinkSync.LinkEntry entry : linksFor(dimension, cartA)) {
            if (entry.partnerId().equals(cartB)) {
               return true;
            }
         }

         for (CoasterCartTrainLinkSync.LinkEntry entryx : linksFor(dimension, cartB)) {
            if (entryx.partnerId().equals(cartA)) {
               return true;
            }
         }

         return false;
      }
   }

   public static List<UUID> partnerIds(ResourceKey<Level> dimension, UUID cartId) {
      return linksFor(dimension, cartId).stream().map(CoasterCartTrainLinkSync.LinkEntry::partnerId).toList();
   }

   public static void clearDimension(ResourceKey<Level> dimension) {
      LINKS_BY_DIMENSION.remove(dimension);
   }

   public static Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>> snapshot(ResourceKey<Level> dimension) {
      Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>> dim = LINKS_BY_DIMENSION.get(dimension);
      return dim != null && !dim.isEmpty() ? Collections.unmodifiableMap(dim) : Map.of();
   }
}
