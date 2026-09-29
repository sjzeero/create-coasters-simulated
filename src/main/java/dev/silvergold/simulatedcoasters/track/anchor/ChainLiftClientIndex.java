/*
 * Decompiled with CFR 0.152.
 */
package dev.silvergold.simulatedcoasters.track.anchor;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class ChainLiftClientIndex {
    private static final Set<CoasterAnchorpointBlockEntity> ANCHORS = Collections.newSetFromMap(new IdentityHashMap());

    private ChainLiftClientIndex() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void refreshMembership(CoasterAnchorpointBlockEntity be) {
        Set<CoasterAnchorpointBlockEntity> set = ANCHORS;
        synchronized (set) {
            ANCHORS.remove((Object)be);
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

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void remove(CoasterAnchorpointBlockEntity be) {
        Set<CoasterAnchorpointBlockEntity> set = ANCHORS;
        synchronized (set) {
            ANCHORS.remove((Object)be);
        }
        ChainLiftSpanPathCache.invalidate(be);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void clearAll() {
        Set<CoasterAnchorpointBlockEntity> set = ANCHORS;
        synchronized (set) {
            ANCHORS.clear();
        }
        ChainLiftSpanPathCache.clear();
    }

    public static Iterable<CoasterAnchorpointBlockEntity> anchors() {
        return ChainLiftClientIndex.snapshotAnchors();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static List<CoasterAnchorpointBlockEntity> snapshotAnchors() {
        Set<CoasterAnchorpointBlockEntity> set = ANCHORS;
        synchronized (set) {
            return List.copyOf(ANCHORS);
        }
    }
}

