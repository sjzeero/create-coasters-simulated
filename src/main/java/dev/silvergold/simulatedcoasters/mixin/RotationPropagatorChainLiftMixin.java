package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.kinetics.RotationPropagator;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftKineticConflict;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({RotationPropagator.class})
public class RotationPropagatorChainLiftMixin {
   @Redirect(
      method = {"propagateNewSource"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"
      )
   )
   private static boolean simulatedcoasters$protectChainLiftAnchor(Level level, BlockPos pos, boolean drop) {
      return ChainLiftKineticConflict.redirectDestroyAwayFromAnchor(level, pos, drop);
   }
}
