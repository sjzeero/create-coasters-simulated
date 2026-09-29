package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CoasterBezierBoundsCache {
   private static final Map<BezierConnection, CoasterBezierBoundsCache.ShiftedBounds> SHIFTED_BOUNDS = new WeakHashMap<>();

   private CoasterBezierBoundsCache() {
   }

   public static AABB shiftCoasterBounds(BezierConnection self, AABB spineBounds) {
      synchronized (SHIFTED_BOUNDS) {
         CoasterBezierBoundsCache.ShiftedBounds cached = SHIFTED_BOUNDS.get(self);
         if (cached != null && cached.spine == spineBounds) {
            return cached.shifted;
         } else {
            Vec3 n = CoasterBezierRailFrames.faceUpAt(self, 0.5F, null);
            if (n.lengthSqr() < 1.0E-10) {
               n = new Vec3(0.0, 1.0, 0.0);
            }

            Vec3 offset = n.normalize().scale((double)CoasterTrackGauge.curveInteractionLift()).add(0.0, -0.125, 0.0);
            AABB shifted = spineBounds.move(offset.x, offset.y, offset.z);
            SHIFTED_BOUNDS.put(self, new CoasterBezierBoundsCache.ShiftedBounds(spineBounds, shifted));
            return shifted;
         }
      }
   }

   private static record ShiftedBounds(AABB spine, AABB shifted) {
   }
}
