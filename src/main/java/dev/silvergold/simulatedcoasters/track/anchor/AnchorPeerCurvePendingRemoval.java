/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 */
package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class AnchorPeerCurvePendingRemoval {
    private static final Set<String> PENDING = new HashSet<String>();

    private AnchorPeerCurvePendingRemoval() {
    }

    public static void mark(BlockPos mined, BlockPos peer) {
        PENDING.add(CoasterPeerCurveHandleLengths.edgeKeyFromEndpoints(mined, peer));
    }

    public static boolean isPending(BezierConnection bc) {
        if (bc == null) {
            return false;
        }
        BezierConnection pri = bc.isPrimary() ? bc : bc.secondary();
        return PENDING.contains(CoasterPeerCurveHandleLengths.edgeKey(pri));
    }

    public static void onAnchorSynced(CoasterAnchorpointBlockEntity anchor) {
        if (PENDING.isEmpty() || anchor.getLevel() == null) {
            return;
        }
        PENDING.removeIf(key -> !AnchorPeerCurvePendingRemoval.isEdgeHostedAnywhere(anchor.getLevel(), key));
    }

    public static void clear() {
        PENDING.clear();
    }

    private static boolean isEdgeHostedAnywhere(Level level, String edgeKey) {
        for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
            if (anchor.isRemoved() || anchor.getLevel() != level) continue;
            BlockPos self = anchor.getBlockPos();
            for (BlockPos peer : anchor.getAnchorPeerCurvesView().keySet()) {
                if (!CoasterPeerCurveHandleLengths.edgeKeyFromEndpoints(self, peer).equals(edgeKey)) continue;
                return true;
            }
        }
        return false;
    }
}

