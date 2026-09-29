package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.BezierBankBlendContext;
import dev.silvergold.simulatedcoasters.track.BezierRailSpread;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
   targets = {"com.simibubi.create.content.trains.track.BezierConnection$Bezierator"}
)
public class BezierConnectionBezieratorDegenerateMixin {
   @WrapOperation(
      method = {"next"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;cross(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
      )}
   )
   private Vec3 simulatedcoasters$nonDegenerateFaceCrossTangent(Vec3 faceNormal, Vec3 derivative, Operation<Vec3> original) {
      BezierConnection bc = BezierBankBlendContext.active();
      if (bc != null && CoasterBezierRailFrames.isCoaster(bc)) {
         Float segmentT = BezierBankBlendContext.segmentT();
         return segmentT != null
            ? CoasterBezierRailFrames.lateralFromBlendedFaceAt(bc, derivative, segmentT)
            : CoasterBezierRailFrames.lateralFromBlendedFace(bc, faceNormal, derivative);
      } else {
         Vec3 c = (Vec3)original.call(new Object[]{faceNormal, derivative});
         return c.lengthSqr() >= 1.0E-10 ? c : BezierRailSpread.faceCrossTangentOrFallback(faceNormal, derivative);
      }
   }
}
