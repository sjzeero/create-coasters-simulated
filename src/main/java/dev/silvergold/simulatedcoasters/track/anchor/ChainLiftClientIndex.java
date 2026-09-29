package dev.silvergold.simulatedcoasters.track.anchor;

import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class ChainLiftClientIndex {
   private static final Set<CoasterAnchorpointBlockEntity> ANCHORS = Collections.newSetFromMap(new IdentityHashMap<>());

   private ChainLiftClientIndex() {
   }

   public static void refreshMembership(CoasterAnchorpointBlockEntity be) {
      synchronized (ANCHORS) {
         ANCHORS.remove(be);
         if (be.isRemoved() || be.getLevel() == null) {
            return;
         }

         if (be.hasChainLiftSpan()) {
            ANCHORS.add(be);
         }
      }

      if (be.isRemoved() || be.getLevel() == null || !be.hasChainLiftSpan()) {
         ChainLiftSpanPathCache.invalidate(be);
      }
   }

   public static void remove(CoasterAnchorpointBlockEntity be) {
      synchronized (ANCHORS) {
         ANCHORS.remove(be);
      }

      ChainLiftSpanPathCache.invalidate(be);
   }

   public static void clearAll() {
      synchronized (ANCHORS) {
         ANCHORS.clear();
      }

      ChainLiftSpanPathCache.clear();
   }

   public static Iterable<CoasterAnchorpointBlockEntity> anchors() {
      return snapshotAnchors();
   }

   public static List<CoasterAnchorpointBlockEntity> snapshotAnchors() {
      synchronized (ANCHORS) {
         return List.copyOf(ANCHORS);
      }
   }
}
