package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.BezierConnection.SegmentAngles;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin({SegmentAngles.class})
public class BezierConnectionSegmentAnglesCoasterMixin {
   private static final double VANILLA_RAIL_HALF_GAUGE = 0.965F;

   private static boolean simulatedcoasters$isCoaster(BezierConnection bc) {
      return bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id);
   }

   private static boolean simulatedcoasters$isRailHalfGaugeFactor(double factor) {
      return Math.abs(factor - 0.965F) < 1.0E-5;
   }

   @WrapOperation(
      method = {"<init>"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"
      )}
   )
   private Vec3 simulatedcoasters$wrapVec3Scale(Vec3 instance, double factor, Operation<Vec3> original) {
      double f = factor;
      if (simulatedcoasters$isCoaster(CoasterTrackGauge.bakingBezier()) && simulatedcoasters$isRailHalfGaugeFactor(factor)) {
         f = factor * 0.8F;
      }

      return (Vec3)original.call(new Object[]{instance, f});
   }

   @ModifyConstant(
      method = {"<init>"},
      constant = {@Constant(
         floatValue = -0.12890625F
      )}
   )
   private float simulatedcoasters$liftBezierModelAnchorY(float vanilla) {
      return simulatedcoasters$isCoaster(CoasterTrackGauge.bakingBezier()) ? vanilla + 0.375F : vanilla;
   }
}
