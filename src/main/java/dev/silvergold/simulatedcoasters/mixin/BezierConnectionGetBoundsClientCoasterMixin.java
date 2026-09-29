package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import dev.silvergold.simulatedcoasters.track.CoasterTrackCurvePickContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BezierConnection.class})
public class BezierConnectionGetBoundsClientCoasterMixin {
   @Inject(
      method = {"getBounds"},
      at = {@At("HEAD")}
   )
   private void simulatedcoasters$noteTrackEntityDuringCurvePick(CallbackInfoReturnable<AABB> cir) {
      if (CoasterTrackCurvePickContext.inPickCurves()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc != null && mc.level != null) {
            BlockPos p = (BlockPos)((BezierConnection)(Object)this).bePositions.getFirst();
            if (mc.level.getBlockEntity(p) instanceof TrackBlockEntity tbe) {
               CoasterTrackCurvePickContext.setTrackEntity(tbe);
            }
         }
      }
   }
}
