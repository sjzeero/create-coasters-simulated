package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.BezierConnection.SegmentAngles;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({BezierConnection.class})
public class BezierConnectionRailGaugeBakeMixin {
   @WrapMethod(
      method = {"getBakedSegments"}
   )
   private SegmentAngles simulatedcoasters$withRailGaugeBakeContext(Operation<SegmentAngles> original) {
      CoasterTrackGauge.BAKE_BEZIER.set((BezierConnection)(Object)this);

      SegmentAngles var2;
      try {
         var2 = (SegmentAngles)original.call(new Object[0]);
      } finally {
         CoasterTrackGauge.BAKE_BEZIER.remove();
      }

      return var2;
   }
}
