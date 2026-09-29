package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.ChainLiftClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftSpanPathCache {
   private static final Map<CoasterAnchorpointBlockEntity, ChainLiftSpanPathCache.Entry> CACHE = new IdentityHashMap<>();
   private static int graphEpoch;

   private ChainLiftSpanPathCache() {
   }

   @Nullable
   public static CoasterPathGraphSpans.SpanPath resolve(CoasterAnchorpointBlockEntity anchor, double partialTickForRender) {
      return resolve(anchor, partialTickForRender, true);
   }

   @Nullable
   public static CoasterPathGraphSpans.SpanPath resolve(CoasterAnchorpointBlockEntity anchor, double partialTickForRender, boolean updateIndex) {
      synchronized (ChainLiftCaches.LOCK) {
         if (anchor.getLevel() == null) {
            return null;
         } else if (!anchor.hasChainLiftSpan()) {
            CACHE.remove(anchor);
            if (updateIndex) {
               ChainLiftEdgeIndex.removeInternal(anchor);
            }

            return null;
         } else {
            int spanDataHash = spanDataHashInternal(anchor);
            ChainLiftSpanPathCache.Entry cached = CACHE.get(anchor);
            if (cached != null
               && cached.epoch == graphEpoch
               && cached.committedLength == anchor.getChainLiftCommittedLength()
               && cached.spanDataHash == spanDataHash) {
               return cached.path;
            } else {
               CoasterPathGraphSpans.SpanPath path = anchor.resolveChainLiftSpanPath(partialTickForRender);
               if (path == null) {
                  CACHE.remove(anchor);
                  if (updateIndex) {
                     ChainLiftEdgeIndex.removeInternal(anchor);
                  }

                  return null;
               } else {
                  CACHE.put(anchor, new ChainLiftSpanPathCache.Entry(path, graphEpoch, anchor.getChainLiftCommittedLength(), spanDataHash));
                  if (updateIndex) {
                     ChainLiftEdgeIndex.registerInternal(anchor, path);
                  }

                  return path;
               }
            }
         }
      }
   }

   public static void invalidate(CoasterAnchorpointBlockEntity anchor) {
      synchronized (ChainLiftCaches.LOCK) {
         CACHE.remove(anchor);
         ChainLiftEdgeIndex.removeInternal(anchor);
      }
   }

   public static void invalidateAll() {
      synchronized (ChainLiftCaches.LOCK) {
         CACHE.clear();
         ChainLiftEdgeIndex.clearInternal();
         graphEpoch++;
         warmCacheAndIndexFromCommittedSpans();
      }
   }

   public static void registerResolved(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path) {
      synchronized (ChainLiftCaches.LOCK) {
         if (anchor.getLevel() != null && !path.segments().isEmpty()) {
            CACHE.put(anchor, new ChainLiftSpanPathCache.Entry(path, graphEpoch, anchor.getChainLiftCommittedLength(), spanDataHashInternal(anchor)));
            ChainLiftEdgeIndex.registerInternal(anchor, path);
         }
      }
   }

   public static void clear() {
      synchronized (ChainLiftCaches.LOCK) {
         CACHE.clear();
         ChainLiftEdgeIndex.clearInternal();
      }
   }

   public static int graphEpoch() {
      return graphEpoch;
   }

   public static int spanDataHash(CoasterAnchorpointBlockEntity anchor) {
      ChainLiftEndRef endRef = anchor.getChainLiftEndRef();
      return Objects.hash(
         anchor.getBlockPos(),
         endRef == null ? null : endRef.kind(),
         endRef == null ? null : endRef.endAnchor(),
         endRef == null ? null : endRef.edgeFrom(),
         endRef == null ? null : endRef.edgeTo(),
         endRef == null ? 0.0 : endRef.endT()
      );
   }

   public static int pathFingerprint(CoasterPathGraphSpans.SpanPath path) {
      int h = 1;

      for (CoasterPathGraphSpans.PathSegment segment : path.segments()) {
         h = 31 * h + System.identityHashCode(segment.edge());
         h = 31 * h + Double.hashCode(segment.tStart());
         h = 31 * h + Double.hashCode(segment.tEnd());
      }

      return h;
   }

   public static int ribbonMeshRebuildKey(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path, int lodTier) {
      return Objects.hash(graphEpoch, spanDataHash(anchor), pathFingerprint(path), lodTier);
   }

   private static int spanDataHashInternal(CoasterAnchorpointBlockEntity anchor) {
      return spanDataHash(anchor);
   }

   private static void warmCacheAndIndexFromCommittedSpans() {
      for (CoasterAnchorpointBlockEntity anchor : ChainLiftClientIndex.snapshotAnchors()) {
         if (!anchor.isRemoved() && anchor.getLevel() != null && anchor.hasChainLiftSpan()) {
            CoasterPathGraphSpans.SpanPath path = ChainLiftSpan.rebindCommittedSpan(
               anchor.getLevel(), anchor.getChainLiftCommittedSegments(), anchor.getChainLiftCommittedLength(), 0.0
            );
            if (path != null) {
               CACHE.put(anchor, new ChainLiftSpanPathCache.Entry(path, graphEpoch, anchor.getChainLiftCommittedLength(), spanDataHashInternal(anchor)));
               ChainLiftEdgeIndex.registerInternal(anchor, path);
            }
         }
      }
   }

   private static record Entry(CoasterPathGraphSpans.SpanPath path, int epoch, double committedLength, int spanDataHash) {
   }
}
