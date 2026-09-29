package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({BezierConnection.class})
public class BezierConnectionFaceNormalSlerpCoasterMixin {
   private static boolean simulatedcoasters$forceSlerp(BezierConnection self, Vec3 fn1, Object other, Operation<Boolean> original) {
      return CoasterBezierRailFrames.isCoaster(self) ? false : (Boolean)original.call(new Object[]{fn1, other});
   }

   @WrapOperation(
      method = {"getNormal"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;equals(Ljava/lang/Object;)Z"
      )}
   )
   private boolean simulatedcoasters$alwaysSlerpFaceNormalsInGetNormal(Vec3 fn1, Object other, Operation<Boolean> original) {
      return simulatedcoasters$forceSlerp((BezierConnection)(Object)this, fn1, other, original);
   }

   @WrapOperation(
      method = {"rasterise"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;equals(Ljava/lang/Object;)Z"
      )}
   )
   private boolean simulatedcoasters$alwaysSlerpFaceNormalsInRasterise(Vec3 fn1, Object other, Operation<Boolean> original) {
      return simulatedcoasters$forceSlerp((BezierConnection)(Object)this, fn1, other, original);
   }
}
