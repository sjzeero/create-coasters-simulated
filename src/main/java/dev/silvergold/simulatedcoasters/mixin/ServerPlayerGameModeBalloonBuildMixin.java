package dev.silvergold.simulatedcoasters.mixin;

import dev.silvergold.simulatedcoasters.balloon.BalloonPopInteraction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerPlayerGameMode.class})
public class ServerPlayerGameModeBalloonBuildMixin {
   @Inject(
      method = {"useItemOn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$blockBuildOnBalloon(
      ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir
   ) {
      if (BalloonPopInteraction.blockBuildOnBalloon(level, hitResult, stack)) {
         if (level instanceof ServerLevel serverLevel) {
            BalloonPopInteraction.handleBuildOnBalloon(serverLevel, hitResult);
         }

         cir.setReturnValue(InteractionResult.FAIL);
         cir.cancel();
      }
   }
}
