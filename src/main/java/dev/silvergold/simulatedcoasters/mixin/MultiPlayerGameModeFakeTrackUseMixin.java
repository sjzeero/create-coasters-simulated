package dev.silvergold.simulatedcoasters.mixin;

import dev.silvergold.simulatedcoasters.track.anchor.ManagedFakeTrackInteractionBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MultiPlayerGameMode.class})
public abstract class MultiPlayerGameModeFakeTrackUseMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"useItemOn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$blockUseOnManagedFakeTrack(
      LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir
   ) {
      if (this.minecraft.level != null && ManagedFakeTrackInteractionBlock.isBlockedTarget(this.minecraft.level, hitResult.getBlockPos())) {
         cir.setReturnValue(InteractionResult.FAIL);
         cir.cancel();
      }
   }
}
