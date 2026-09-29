/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.ChainLiftClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftCaches;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftEdgeIndex;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftEndRef;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpan;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftSpanPathCache {
    private static final Map<CoasterAnchorpointBlockEntity, Entry> CACHE = new IdentityHashMap<CoasterAnchorpointBlockEntity, Entry>();
    private static int graphEpoch;

    private ChainLiftSpanPathCache() {
    }

    @Nullable
    public static CoasterPathGraphSpans.SpanPath resolve(CoasterAnchorpointBlockEntity anchor, double partialTickForRender) {
        return ChainLiftSpanPathCache.resolve(anchor, partialTickForRender, true);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public static CoasterPathGraphSpans.SpanPath resolve(CoasterAnchorpointBlockEntity anchor, double partialTickForRender, boolean updateIndex) {
        Object object = ChainLiftCaches.LOCK;
        synchronized (object) {
            if (anchor.getLevel() == null) {
                return null;
            }
            if (!anchor.hasChainLiftSpan()) {
                CACHE.remove((Object)anchor);
                if (updateIndex) {
                    ChainLiftEdgeIndex.removeInternal(anchor);
                }
                return null;
            }
            int spanDataHash = ChainLiftSpanPathCache.spanDataHashInternal(anchor);
            Entry cached = CACHE.get((Object)anchor);
            if (cached != null && cached.epoch == graphEpoch && cached.committedLength == anchor.getChainLiftCommittedLength() && cached.spanDataHash == spanDataHash) {
                return cached.path;
            }
             @Nullable CoasterPathGraphSpans.SpanPath path = anchor.resolveChainLiftSpanPath(partialTickForRender);
            if (path == null) {
                CACHE.remove((Object)anchor);
                if (updateIndex) {
                    ChainLiftEdgeIndex.removeInternal(anchor);
                }
                return null;
            }
            CACHE.put(anchor, new Entry(path, graphEpoch, anchor.getChainLiftCommittedLength(), spanDataHash));
            if (updateIndex) {
                ChainLiftEdgeIndex.registerInternal(anchor, path);
            }
            return path;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void invalidate(CoasterAnchorpointBlockEntity anchor) {
        Object object = ChainLiftCaches.LOCK;
        synchronized (object) {
            CACHE.remove((Object)anchor);
            ChainLiftEdgeIndex.removeInternal(anchor);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void invalidateAll() {
        Object object = ChainLiftCaches.LOCK;
        synchronized (object) {
            CACHE.clear();
            ChainLiftEdgeIndex.clearInternal();
            ++graphEpoch;
            ChainLiftSpanPathCache.warmCacheAndIndexFromCommittedSpans();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void registerResolved(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path) {
        Object object = ChainLiftCaches.LOCK;
        synchronized (object) {
            if (anchor.getLevel() == null || path.segments().isEmpty()) {
                return;
            }
            CACHE.put(anchor, new Entry(path, graphEpoch, anchor.getChainLiftCommittedLength(), ChainLiftSpanPathCache.spanDataHashInternal(anchor)));
            ChainLiftEdgeIndex.registerInternal(anchor, path);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void clear() {
        Object object = ChainLiftCaches.LOCK;
        synchronized (object) {
            CACHE.clear();
            ChainLiftEdgeIndex.clearInternal();
        }
    }

    public static int graphEpoch() {
        return graphEpoch;
    }

    public static int spanDataHash(CoasterAnchorpointBlockEntity anchor) {
        ChainLiftEndRef endRef = anchor.getChainLiftEndRef();
        return Objects.hash(new Object[]{anchor.getBlockPos(), endRef == null ? null : endRef.kind(), endRef == null ? null : endRef.endAnchor(), endRef == null ? null : endRef.edgeFrom(), endRef == null ? null : endRef.edgeTo(), endRef == null ? 0.0 : endRef.endT()});
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
        return Objects.hash(graphEpoch, ChainLiftSpanPathCache.spanDataHash(anchor), ChainLiftSpanPathCache.pathFingerprint(path), lodTier);
    }

    private static int spanDataHashInternal(CoasterAnchorpointBlockEntity anchor) {
        return ChainLiftSpanPathCache.spanDataHash(anchor);
    }

    private static void warmCacheAndIndexFromCommittedSpans() {
        for (CoasterAnchorpointBlockEntity anchor : ChainLiftClientIndex.snapshotAnchors()) {
            CoasterPathGraphSpans.SpanPath path;
            if (anchor.isRemoved() || anchor.getLevel() == null || !anchor.hasChainLiftSpan() || (path = ChainLiftSpan.rebindCommittedSpan(anchor.getLevel(), anchor.getChainLiftCommittedSegments(), anchor.getChainLiftCommittedLength(), 0.0)) == null) continue;
            CACHE.put(anchor, new Entry(path, graphEpoch, anchor.getChainLiftCommittedLength(), ChainLiftSpanPathCache.spanDataHashInternal(anchor)));
            ChainLiftEdgeIndex.registerInternal(anchor, path);
        }
    }

    private record Entry(CoasterPathGraphSpans.SpanPath path, int epoch, double committedLength, int spanDataHash) {
    }
}

