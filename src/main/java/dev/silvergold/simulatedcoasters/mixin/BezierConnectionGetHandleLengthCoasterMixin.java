package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import net.createmod.catnip.data.Couple;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({BezierConnection.class})
public class BezierConnectionGetHandleLengthCoasterMixin {
   @WrapMethod(
      method = {"getHandleLength"}
   )
   private double simulatedcoasters$wrapGetHandleLength(Operation<Double> original) {
      BezierConnection self = (BezierConnection)(Object)this;
      Couple<Double> L = CoasterPeerCurveHandleLengths.getCanonicalLengths(self);
      return L != null ? Math.max((Double)L.getFirst(), (Double)L.getSecond()) : (Double)original.call(new Object[0]);
   }
}
