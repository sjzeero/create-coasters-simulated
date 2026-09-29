package dev.silvergold.simulatedcoasters.mixin;

import dev.ryanhcode.sable.sublevel.render.vanilla.SingleBlockSubLevelWrapper;
import dev.silvergold.simulatedcoasters.client.rivet.RivetSubLevelRenderLight;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SingleBlockSubLevelWrapper.class})
public abstract class SingleBlockSubLevelWrapperCoasterMixin {
   @Shadow
   private ClientLevel level;

   @Inject(
      method = {"getBrightness"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$rivetBrightness(LightLayer lightLayer, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
      BlockPos sample = RivetSubLevelRenderLight.probePos();
      if (sample != null && this.level != null) {
         cir.setReturnValue(this.level.getBrightness(lightLayer, sample));
      }
   }

   @Inject(
      method = {"getRawBrightness"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$rivetRawBrightness(BlockPos pos, int ambientDarkness, CallbackInfoReturnable<Integer> cir) {
      BlockPos sample = RivetSubLevelRenderLight.probePos();
      if (sample != null && this.level != null) {
         cir.setReturnValue(this.level.getLightEngine().getRawBrightness(sample, ambientDarkness));
      }
   }
}
