package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;

public final class CoasterCartLinkDistanceEdit {
   private static final double SNAP_EPSILON = 1.0E-6;

   private CoasterCartLinkDistanceEdit() {
   }

   public static boolean isOnSnapGrid(double centerDistanceBlocks) {
      double step = 0.5;
      double snapped = (double)Math.round(centerDistanceBlocks / step) * step;
      return Math.abs(centerDistanceBlocks - snapped) < 1.0E-6;
   }

   public static double snapToGrid(double centerDistanceBlocks, boolean floor) {
      double step = 0.5;
      double scaled = centerDistanceBlocks / step;
      double snapped = (floor ? Math.floor(scaled) : Math.ceil(scaled)) * step;
      return Mth.clamp(snapped, 1.5, 6.0);
   }

   public static double steppedDistance(double current, boolean shorten) {
      double step = 0.5;
      double min = 1.5;
      double max = 6.0;
      if (shorten) {
         return current <= min + 1.0E-6 ? current : Math.max(min, current - step);
      } else {
         return current >= max - 1.0E-6 ? current : Math.min(max, current + step);
      }
   }

   public static List<CoasterCartLinkDistanceEdit.AdjustedLink> computeAdjustments(ServerSubLevel hostCart, boolean shorten) {
      List<CoasterCartTrainLinkData.LinkRecord> links = CoasterCartTrainLinkData.readAll(hostCart);
      if (links.isEmpty()) {
         return List.of();
      } else {
         boolean allSnapped = links.stream().allMatch(r -> isOnSnapGrid(r.centerDistance()));
         List<CoasterCartLinkDistanceEdit.AdjustedLink> out = new ArrayList<>(links.size());

         for (CoasterCartTrainLinkData.LinkRecord record : links) {
            ServerSubLevel partner = CoasterCartResolve.findCartById(hostCart.getLevel(), record.partnerId());
            if (partner != null && !partner.isRemoved()) {
               double current = record.centerDistance();
               double next = allSnapped ? steppedDistance(current, shorten) : snapToGrid(current, shorten);
               if (!(Math.abs(next - current) < 1.0E-6)) {
                  out.add(new CoasterCartLinkDistanceEdit.AdjustedLink(partner, next));
               }
            }
         }

         return out;
      }
   }

   public static record AdjustedLink(ServerSubLevel partner, double newDistance) {
   }
}
