/*
 * Decompiled with CFR 0.152.
 */
package dev.silvergold.simulatedcoasters.track;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CoasterTrackPlacementNeighborSmoothSuppress {
    private static final Map<UUID, Boolean> SERVER_SUPPRESS = new ConcurrentHashMap<UUID, Boolean>();

    private CoasterTrackPlacementNeighborSmoothSuppress() {
    }

    public static void setServerSuppress(UUID playerId, boolean suppress) {
        if (suppress) {
            SERVER_SUPPRESS.put(playerId, Boolean.TRUE);
        } else {
            SERVER_SUPPRESS.remove(playerId);
        }
    }

    public static boolean isSuppress(UUID playerId) {
        return SERVER_SUPPRESS.containsKey(playerId);
    }

    public static void clearPlayer(UUID playerId) {
        SERVER_SUPPRESS.remove(playerId);
    }
}

