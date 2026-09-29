package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.BezierBankBlendContext;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
   targets = {"com.simibubi.create.content.trains.track.BezierConnection$Bezierator"}
)
public class BezierConnectionBezieratorFaceNormalSlerpCoasterMixin {
   @WrapOperation(
      method = {"next"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;equals(Ljava/lang/Object;)Z"
      )}
   )
   private boolean simulatedcoasters$alwaysSlerpFaceNormalsInBezierator(Vec3 fn1, Object other, Operation<Boolean> original) {
      BezierConnection bc = BezierBankBlendContext.active();
      return bc != null && CoasterBezierRailFrames.isCoaster(bc) ? false : (Boolean)original.call(new Object[]{fn1, other});
   }
}
