package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.BezierConnection.Segment;
import dev.silvergold.simulatedcoasters.track.BezierBankBlend;
import dev.silvergold.simulatedcoasters.track.BezierBankBlendContext;
import java.util.Iterator;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({BezierConnection.class})
public class BezierConnectionBankSmoothMixin {
   @ModifyReturnValue(
      method = {"iterator"},
      at = {@At("RETURN")}
   )
   private Iterator<Segment> simulatedcoasters$wrapIteratorForBankSmooth(final Iterator<Segment> original) {
      final BezierConnection self = (BezierConnection)(Object)this;
      return new Iterator<Segment>() {
         @Override
         public boolean hasNext() {
            return original.hasNext();
         }

         public Segment next() {
            BezierBankBlendContext.set(self);

            Segment var1;
            try {
               var1 = original.next();
            } finally {
               BezierBankBlendContext.clear();
            }

            return var1;
         }
      };
   }

   @WrapOperation(
      method = {"getNormal"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/createmod/catnip/math/VecHelper;slerp(FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
      )}
   )
   private Vec3 simulatedcoasters$smoothBankSlerpInGetNormal(float t, Vec3 fn1, Vec3 fn2, Operation<Vec3> original) {
      BezierConnection self = (BezierConnection)(Object)this;
      float tt = BezierBankBlend.smoothJointBankParameter(t, self);
      return (Vec3)original.call(new Object[]{tt, fn1, fn2});
   }
}
