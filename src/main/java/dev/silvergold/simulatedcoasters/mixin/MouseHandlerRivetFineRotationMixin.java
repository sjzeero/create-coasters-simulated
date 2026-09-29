package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.silvergold.simulatedcoasters.client.item.RivetPlacementOutlineClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MouseHandler.class})
public class MouseHandlerRivetFineRotationMixin {
   @Inject(
      method = {"turnPlayer"},
      cancellable = true,
      at = {@At(
         value = "INVOKE",
         shift = Shift.BEFORE,
         target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"
      )}
   )
   private void simulatedcoasters$rivetFineRotationMouse(
      double movementTime, CallbackInfo ci, @Local(ordinal = 4) double yawDelta, @Local(ordinal = 5) double pitchDelta, @Local(ordinal = 0) int invertY
   ) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.player.isSpectator() && mc.screen == null) {
         if (RivetPlacementOutlineClient.onFineRotationMouseMove(yawDelta, pitchDelta * (double)invertY)) {
            ci.cancel();
         }
      }
   }
}
