package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BezierConnection.class})
public class BezierConnectionResolveCoasterMixin {
   @Inject(
      method = {"resolve"},
      at = {@At("HEAD")}
   )
   private void simulatedcoasters$enterResolve(CallbackInfoReturnable<Object> cir) {
      CoasterTrackGauge.RESOLVING_BEZIER.set((BezierConnection)(Object)this);
   }

   @Inject(
      method = {"resolve"},
      at = {@At("RETURN")}
   )
   private void simulatedcoasters$exitResolve(CallbackInfoReturnable<Object> cir) {
      CoasterTrackGauge.RESOLVING_BEZIER.remove();
   }
}
