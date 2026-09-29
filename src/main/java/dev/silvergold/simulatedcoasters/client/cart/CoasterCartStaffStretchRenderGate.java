/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
final class CoasterCartStaffStretchRenderGate {
    static final int RELEASE_COOLDOWN_TICKS = 4;
    private static final Map<UUID, Long> COOLDOWN_UNTIL_GAME_TIME = new HashMap<UUID, Long>();
    private static final Map<UUID, Boolean> PREV_STAFF_GRABBING = new HashMap<UUID, Boolean>();
    private static long resultTickScope = Long.MIN_VALUE;
    private static final Map<UUID, Boolean> STRETCH_THIS_TICK = new HashMap<UUID, Boolean>();

    private CoasterCartStaffStretchRenderGate() {
    }

    static void clearSub(UUID subId) {
        COOLDOWN_UNTIL_GAME_TIME.remove(subId);
        PREV_STAFF_GRABBING.remove(subId);
        STRETCH_THIS_TICK.remove(subId);
    }

    static boolean stretchRendering(ClientLevel level, SubLevel sub, boolean snapEngaged, boolean staffGrabbingNow) {
        long now = level.getGameTime();
        if (now != resultTickScope) {
            resultTickScope = now;
            STRETCH_THIS_TICK.clear();
        }
        UUID id = sub.getUniqueId();
        return STRETCH_THIS_TICK.computeIfAbsent(id, k -> CoasterCartStaffStretchRenderGate.computeStretch(id, snapEngaged, staffGrabbingNow, now));
    }

    private static boolean computeStretch(UUID id, boolean snapEngaged, boolean staffGrabbingNow, long now) {
        Long until;
        boolean wasGrabbing = Boolean.TRUE.equals(PREV_STAFF_GRABBING.put(id, staffGrabbingNow));
        if (staffGrabbingNow) {
            COOLDOWN_UNTIL_GAME_TIME.remove(id);
            return snapEngaged;
        }
        if (!snapEngaged) {
            COOLDOWN_UNTIL_GAME_TIME.remove(id);
            return false;
        }
        if (wasGrabbing) {
            COOLDOWN_UNTIL_GAME_TIME.put(id, now + 4L);
        }
        if ((until = COOLDOWN_UNTIL_GAME_TIME.get(id)) == null) {
            return false;
        }
        if (now < until) {
            return true;
        }
        COOLDOWN_UNTIL_GAME_TIME.remove(id);
        return false;
    }
}

