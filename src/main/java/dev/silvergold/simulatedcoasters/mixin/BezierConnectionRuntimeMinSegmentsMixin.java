package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
   targets = {"com.simibubi.create.content.trains.track.BezierConnection$Runtime"}
)
public class BezierConnectionRuntimeMinSegmentsMixin {
   private static final double MIN_LENGTH_FOR_ONE_SEGMENT = 0.500000001;

   @WrapOperation(
      method = {"<init>"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/trains/track/BezierConnection$Runtime;computeLength(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)D"
      )}
   )
   private static double simulatedcoasters$ensureMinCurveLengthForSegments(Vec3 a, Vec3 b, Vec3 c, Vec3 d, int scan, Operation<Double> original) {
      double len = (Double)original.call(new Object[]{a, b, c, d, scan});
      return Math.max(len, 0.500000001);
   }
}
