/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CoasterBezierBoundsCache {
    private static final Map<BezierConnection, ShiftedBounds> SHIFTED_BOUNDS = new WeakHashMap<BezierConnection, ShiftedBounds>();

    private CoasterBezierBoundsCache() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static AABB shiftCoasterBounds(BezierConnection self, AABB spineBounds) {
        Map<BezierConnection, ShiftedBounds> map = SHIFTED_BOUNDS;
        synchronized (map) {
            ShiftedBounds cached = SHIFTED_BOUNDS.get(self);
            if (cached != null && cached.spine == spineBounds) {
                return cached.shifted;
            }
            Vec3 n = CoasterBezierRailFrames.faceUpAt(self, 0.5f, null);
            if (n.lengthSqr() < 1.0E-10) {
                n = new Vec3(0.0, 1.0, 0.0);
            }
            Vec3 offset = n.normalize().scale((double)CoasterTrackGauge.curveInteractionLift()).add(0.0, -0.125, 0.0);
            AABB shifted = spineBounds.move(offset.x, offset.y, offset.z);
            SHIFTED_BOUNDS.put(self, new ShiftedBounds(spineBounds, shifted));
            return shifted;
        }
    }

    private record ShiftedBounds(AABB spine, AABB shifted) {
    }
}

