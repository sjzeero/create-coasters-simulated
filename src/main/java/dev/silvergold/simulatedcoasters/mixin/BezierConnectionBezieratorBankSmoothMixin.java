package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.BezierBankBlend;
import dev.silvergold.simulatedcoasters.track.BezierBankBlendContext;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
   targets = {"com.simibubi.create.content.trains.track.BezierConnection$Bezierator"}
)
public class BezierConnectionBezieratorBankSmoothMixin {
   @WrapOperation(
      method = {"next"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/createmod/catnip/math/VecHelper;slerp(FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
      )}
   )
   private Vec3 simulatedcoasters$smoothBankSlerpInNext(float t, Vec3 fn1, Vec3 fn2, Operation<Vec3> original) {
      BezierConnection bc = BezierBankBlendContext.active();
      BezierBankBlendContext.setSegmentT(t);
      float tt = BezierBankBlend.smoothJointBankParameter(t, bc);
      return (Vec3)original.call(new Object[]{tt, fn1, fn2});
   }
}
