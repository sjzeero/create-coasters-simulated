/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftCaches;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanQuery;
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
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftEdgeIndex {
    private static final double T_EPS = 1.0E-5;
    private static final Map<EdgeKey, List<Registration>> BY_EDGE = new HashMap<EdgeKey, List<Registration>>();
    private static final Map<CoasterAnchorpointBlockEntity, List<EdgeKey>> BY_ANCHOR = new IdentityHashMap<CoasterAnchorpointBlockEntity, List<EdgeKey>>();

    private ChainLiftEdgeIndex() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void register(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path) {
        Object object = ChainLiftCaches.LOCK;
        synchronized (object) {
            ChainLiftEdgeIndex.registerInternal(anchor, path);
        }
    }

    static void registerInternal(CoasterAnchorpointBlockEntity anchor, CoasterPathGraphSpans.SpanPath path) {
        ChainLiftEdgeIndex.removeInternal(anchor);
        if (path.segments().isEmpty()) {
            return;
        }
        ArrayList<EdgeKey> keys = new ArrayList<EdgeKey>();
        for (CoasterPathGraphSpans.PathSegment segment : path.segments()) {
            EdgeKey key = EdgeKey.of(segment.edge());
            double lo = Math.min(segment.tStart(), segment.tEnd());
            double hi = Math.max(segment.tStart(), segment.tEnd());
            BY_EDGE.computeIfAbsent(key, k -> new ArrayList()).add(new Registration(anchor, lo, hi));
            keys.add(key);
        }
        BY_ANCHOR.put(anchor, keys);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void remove(CoasterAnchorpointBlockEntity anchor) {
        Object object = ChainLiftCaches.LOCK;
        synchronized (object) {
            ChainLiftEdgeIndex.removeInternal(anchor);
        }
    }

    static void removeInternal(CoasterAnchorpointBlockEntity anchor) {
        List<EdgeKey> keys = BY_ANCHOR.remove((Object)anchor);
        if (keys == null) {
            return;
        }
        for (EdgeKey key : keys) {
            List<Registration> registrations = BY_EDGE.get(key);
            if (registrations == null) continue;
            Iterator<Registration> it = registrations.iterator();
            while (it.hasNext()) {
                Registration reg = it.next();
                if (reg == null || reg.anchor != anchor) continue;
                it.remove();
            }
            if (!registrations.isEmpty()) continue;
            BY_EDGE.remove(key);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void clear() {
        Object object = ChainLiftCaches.LOCK;
        synchronized (object) {
            ChainLiftEdgeIndex.clearInternal();
        }
    }

    static void clearInternal() {
        BY_EDGE.clear();
        BY_ANCHOR.clear();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Nullable
    public static ChainLiftSpanQuery.ChainLiftSpanHit findAtGraphHit(Level level, CoasterPathTrackFrame.GraphHit hit, double partialTickForRender) {
        Object object = ChainLiftCaches.LOCK;
        synchronized (object) {
            List<Registration> registrations = BY_EDGE.get(EdgeKey.of(hit.edge()));
            if (registrations == null || registrations.isEmpty()) {
                return null;
            }
            for (Registration registration : new ArrayList<Registration>(registrations)) {
                CoasterPathGraphSpans.SpanPath path;
                CoasterAnchorpointBlockEntity anchor;
                if (registration == null || (anchor = registration.anchor).isRemoved() || anchor.getLevel() != level || hit.t() < registration.tLo - 1.0E-5 || hit.t() > registration.tHi + 1.0E-5 || (path = ChainLiftSpanPathCache.resolve(anchor, partialTickForRender, false)) == null || !ChainLiftSpanQuery.graphHitOnSpan(path, hit)) continue;
                return new ChainLiftSpanQuery.ChainLiftSpanHit(anchor, path);
            }
            return null;
        }
    }

    private record EdgeKey(BlockPos a, BlockPos b) {
        static EdgeKey of(CoasterPathEdge edge) {
            return EdgeKey.of(edge.from(), edge.to());
        }

        static EdgeKey of(BlockPos from, BlockPos to) {
            return from.compareTo((Vec3i)to) <= 0 ? new EdgeKey(from, to) : new EdgeKey(to, from);
        }
    }

    private record Registration(CoasterAnchorpointBlockEntity anchor, double tLo, double tHi) {
    }
}

