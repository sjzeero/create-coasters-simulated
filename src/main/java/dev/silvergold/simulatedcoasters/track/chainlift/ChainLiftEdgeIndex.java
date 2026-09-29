package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftEdgeIndex {
   private static final double T_EPS = 1.0E-5;
   private static final Map<ChainLiftEdgeIndex.EdgeKey, List<ChainLiftEdgeIndex.Registration>> BY_EDGE = new HashMap<>();
   private static final Map<CoasterAnchorpointBlockEntity, List<ChainLiftEdgeIndex.EdgeKey>> BY_ANCHOR = new IdentityHashMap<>();

   private ChainLiftEdgeIndex() {
   }

   public static void register(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path) {
      synchronized (ChainLiftCaches.LOCK) {
         registerInternal(anchor, path);
      }
   }

   static void registerInternal(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path) {
      removeInternal(anchor);
      if (!path.segments().isEmpty()) {
         List<ChainLiftEdgeIndex.EdgeKey> keys = new ArrayList<>();

         for (CoasterPathGraphSpans.PathSegment segment : path.segments()) {
            ChainLiftEdgeIndex.EdgeKey key = ChainLiftEdgeIndex.EdgeKey.of(segment.edge());
            double lo = Math.min(segment.tStart(), segment.tEnd());
            double hi = Math.max(segment.tStart(), segment.tEnd());
            BY_EDGE.computeIfAbsent(key, k -> new ArrayList<>()).add(new ChainLiftEdgeIndex.Registration(anchor, lo, hi));
            keys.add(key);
         }

         BY_ANCHOR.put(anchor, keys);
      }
   }

   public static void remove(CoasterAnchorpointBlockEntity anchor) {
      synchronized (ChainLiftCaches.LOCK) {
         removeInternal(anchor);
      }
   }

   static void removeInternal(CoasterAnchorpointBlockEntity anchor) {
      List<ChainLiftEdgeIndex.EdgeKey> keys = BY_ANCHOR.remove(anchor);
      if (keys != null) {
         for (ChainLiftEdgeIndex.EdgeKey key : keys) {
            List<ChainLiftEdgeIndex.Registration> registrations = BY_EDGE.get(key);
            if (registrations != null) {
               Iterator<ChainLiftEdgeIndex.Registration> it = registrations.iterator();

               while (it.hasNext()) {
                  ChainLiftEdgeIndex.Registration reg = it.next();
                  if (reg != null && reg.anchor == anchor) {
                     it.remove();
                  }
               }

               if (registrations.isEmpty()) {
                  BY_EDGE.remove(key);
               }
            }
         }
      }
   }

   public static void clear() {
      synchronized (ChainLiftCaches.LOCK) {
         clearInternal();
      }
   }

   static void clearInternal() {
      BY_EDGE.clear();
      BY_ANCHOR.clear();
   }

   @Nullable
   public static ChainLiftSpanQuery.ChainLiftSpanHit findAtGraphHit(Level level, CoasterPathTrackFrame.GraphHit hit, double partialTickForRender) {
      synchronized (ChainLiftCaches.LOCK) {
         List<ChainLiftEdgeIndex.Registration> registrations = BY_EDGE.get(ChainLiftEdgeIndex.EdgeKey.of(hit.edge()));
         if (registrations != null && !registrations.isEmpty()) {
            for (ChainLiftEdgeIndex.Registration registration : new ArrayList<>(registrations)) {
               if (registration != null) {
                  CoasterAnchorpointBlockEntity anchor = registration.anchor;
                  if (!anchor.isRemoved() && anchor.getLevel() == level && !(hit.t() < registration.tLo - 1.0E-5) && !(hit.t() > registration.tHi + 1.0E-5)) {
                     CoasterPathGraphSpans.SpanPath path = ChainLiftSpanPathCache.resolve(anchor, partialTickForRender, false);
                     if (path != null && ChainLiftSpanQuery.graphHitOnSpan(path, hit)) {
                        return new ChainLiftSpanQuery.ChainLiftSpanHit(anchor, path);
                     }
                  }
               }
            }

            return null;
         } else {
            return null;
         }
      }
   }

   private static record EdgeKey(BlockPos a, BlockPos b) {
      static ChainLiftEdgeIndex.EdgeKey of(CoasterPathEdge edge) {
         return of(edge.from(), edge.to());
      }

      static ChainLiftEdgeIndex.EdgeKey of(BlockPos from, BlockPos to) {
         return from.compareTo(to) <= 0 ? new ChainLiftEdgeIndex.EdgeKey(from, to) : new ChainLiftEdgeIndex.EdgeKey(to, from);
      }
   }

   private static record Registration(CoasterAnchorpointBlockEntity anchor, double tLo, double tHi) {
   }
}
