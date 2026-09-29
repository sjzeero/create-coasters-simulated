package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

final class CoasterCartTrainLinkData {
   static final int MAX_LINKS = 2;
   static final String USER_DATA_LINKS = "simulatedcoasters:cart_train_links";
   private static final String USER_DATA_LINK_LEGACY = "simulatedcoasters:cart_train_link";
   private static final String PARTNER_TAG = "partner";
   private static final String DISTANCE_TAG = "distance";

   private CoasterCartTrainLinkData() {
   }

   static List<CoasterCartTrainLinkData.LinkRecord> readAll(ServerSubLevel cart) {
      CompoundTag root = cart.getUserDataTag();
      if (root == null) {
         return List.of();
      } else if (root.contains("simulatedcoasters:cart_train_links", 9)) {
         return readList(root.getList("simulatedcoasters:cart_train_links", 10));
      } else {
         CoasterCartTrainLinkData.LinkRecord legacy = readLegacyEntry(root);
         return legacy != null ? List.of(legacy) : List.of();
      }
   }

   static boolean hasLink(ServerSubLevel cart, UUID partnerId) {
      for (CoasterCartTrainLinkData.LinkRecord r : readAll(cart)) {
         if (r.partnerId().equals(partnerId)) {
            return true;
         }
      }

      return false;
   }

   static int linkCount(ServerSubLevel cart) {
      return readAll(cart).size();
   }

   static boolean canAddLink(ServerSubLevel cart, UUID partnerId) {
      return hasLink(cart, partnerId) ? false : linkCount(cart) < 2;
   }

   static void addLink(ServerSubLevel cart, UUID partnerId, double centerDistance) {
      List<CoasterCartTrainLinkData.LinkRecord> links = new ArrayList<>(readAll(cart));

      for (CoasterCartTrainLinkData.LinkRecord r : links) {
         if (r.partnerId().equals(partnerId)) {
            return;
         }
      }

      if (links.size() < 2) {
         links.add(new CoasterCartTrainLinkData.LinkRecord(partnerId, centerDistance));
         writeAll(cart, links);
      }
   }

   static void removeLink(ServerSubLevel cart, UUID partnerId) {
      List<CoasterCartTrainLinkData.LinkRecord> links = new ArrayList<>(readAll(cart));
      if (links.removeIf(r -> r.partnerId().equals(partnerId))) {
         writeAll(cart, links);
      }
   }

   static void setLinkDistance(ServerSubLevel cart, UUID partnerId, double centerDistance) {
      List<CoasterCartTrainLinkData.LinkRecord> links = new ArrayList<>(readAll(cart));

      for (int i = 0; i < links.size(); i++) {
         if (links.get(i).partnerId().equals(partnerId)) {
            links.set(i, new CoasterCartTrainLinkData.LinkRecord(partnerId, centerDistance));
            writeAll(cart, links);
            return;
         }
      }
   }

   static void clearAll(ServerSubLevel cart) {
      CompoundTag root = cart.getUserDataTag();
      if (root != null) {
         root.remove("simulatedcoasters:cart_train_links");
         root.remove("simulatedcoasters:cart_train_link");
         if (root.isEmpty()) {
            cart.setUserDataTag(null);
         } else {
            cart.setUserDataTag(root);
         }
      }
   }

   private static void writeAll(ServerSubLevel cart, List<CoasterCartTrainLinkData.LinkRecord> links) {
      CompoundTag root = cart.getUserDataTag();
      if (root == null) {
         root = new CompoundTag();
      }

      root.remove("simulatedcoasters:cart_train_link");
      if (links.isEmpty()) {
         root.remove("simulatedcoasters:cart_train_links");
         if (root.isEmpty()) {
            cart.setUserDataTag(null);
         } else {
            cart.setUserDataTag(root);
         }
      } else {
         ListTag list = new ListTag();

         for (CoasterCartTrainLinkData.LinkRecord r : links) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("partner", r.partnerId());
            entry.putDouble("distance", r.centerDistance());
            list.add(entry);
         }

         root.put("simulatedcoasters:cart_train_links", list);
         cart.setUserDataTag(root);
      }
   }

   private static List<CoasterCartTrainLinkData.LinkRecord> readList(ListTag list) {
      List<CoasterCartTrainLinkData.LinkRecord> out = new ArrayList<>(list.size());

      for (Tag tag : list) {
         if (tag instanceof CompoundTag) {
            CompoundTag entry = (CompoundTag)tag;
            if (entry.hasUUID("partner")) {
               out.add(new CoasterCartTrainLinkData.LinkRecord(entry.getUUID("partner"), entry.getDouble("distance")));
            }
         }
      }

      return out;
   }

   @Nullable
   private static CoasterCartTrainLinkData.LinkRecord readLegacyEntry(CompoundTag root) {
      if (!root.contains("simulatedcoasters:cart_train_link", 10)) {
         return null;
      } else {
         CompoundTag link = root.getCompound("simulatedcoasters:cart_train_link");
         return !link.hasUUID("partner") ? null : new CoasterCartTrainLinkData.LinkRecord(link.getUUID("partner"), link.getDouble("distance"));
      }
   }

   static record LinkRecord(UUID partnerId, double centerDistance) {
   }
}
