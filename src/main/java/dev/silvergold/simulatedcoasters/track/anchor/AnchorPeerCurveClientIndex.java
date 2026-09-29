/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.Level
 */
package dev.silvergold.simulatedcoasters.track.anchor;

import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurvePendingRemoval;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.world.level.Level;

public final class AnchorPeerCurveClientIndex {
    private static final Set<CoasterAnchorpointBlockEntity> ANCHORS = Collections.newSetFromMap(new IdentityHashMap());

    private AnchorPeerCurveClientIndex() {
    }

    public static void refreshMembership(CoasterAnchorpointBlockEntity be) {
        ANCHORS.remove((Object)be);
        if (be.isRemoved()) {
            return;
        }
        if (be.getLevel() == null || !be.getLevel().isClientSide()) {
            return;
        }
        Level level = be.getLevel();
        boolean anyPrimaryPeer = be.getAnchorPeerCurvesView().values().stream().filter(c -> c != null).map(c -> c.isPrimary() ? c : c.secondary()).anyMatch(p -> p.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id));
        if (anyPrimaryPeer) {
            ANCHORS.add(be);
        }
        AnchorPeerCurvePendingRemoval.onAnchorSynced(be);
    }

    public static void remove(CoasterAnchorpointBlockEntity be) {
        ANCHORS.remove((Object)be);
    }

    public static Iterable<CoasterAnchorpointBlockEntity> anchors() {
        return ANCHORS;
    }
}

