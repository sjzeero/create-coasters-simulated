package dev.silvergold.simulatedcoasters.mixin;

import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurvePick;
import dev.silvergold.simulatedcoasters.client.track.CoasterConnectedAnchorBreakClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public abstract class MinecraftCoasterCurvePickMixin {
   @Shadow
   protected int missTime;

   @Inject(
      method = {"pickBlock"},
      at = {@At("HEAD")}
   )
   private void simulatedcoasters$retargetPickBlockHoveredCurve(CallbackInfo ci) {
      Minecraft mc = (Minecraft)(Object)this;
      if (mc.hitResult == null || mc.hitResult.getType() != Type.BLOCK) {
         BlockHitResult pickHit = AnchorPeerCurvePick.syntheticPickBlockHit(mc);
         if (pickHit != null) {
            mc.hitResult = pickHit;
         }
      }
   }

   @Inject(
      method = {"continueAttack"},
      at = {@At("HEAD")}
   )
   private void simulatedcoasters$clearMissTimeForConnectedAnchorBreak(boolean leftClick, CallbackInfo ci) {
      if (leftClick) {
         Minecraft mc = (Minecraft)(Object)this;
         if (!(mc.hitResult instanceof BlockHitResult bhr) || mc.hitResult.getType() != Type.BLOCK) {
            return;
         }

         if (mc.level != null) {
            if (CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak(mc.level, bhr.getBlockPos())) {
               this.missTime = 0;
            }
         }
      }
   }

   @Inject(
      method = {"continueAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$continueAttackHoveredCurve(boolean leftClick, CallbackInfo ci) {
      Minecraft mc = (Minecraft)(Object)this;
      if (leftClick) {
         if (!(mc.hitResult instanceof BlockHitResult) || mc.hitResult.getType() != Type.BLOCK) {
            BlockHitResult miningHit = AnchorPeerCurvePick.syntheticMiningHit(mc);
            if (miningHit == null) {
               if (this.missTime > 0 || mc.player.isUsingItem()) {
                  return;
               }
            } else if (mc.player == null || mc.player.isUsingItem()) {
               return;
            }

            if (miningHit != null && mc.player != null && mc.level != null && mc.gameMode != null) {
               this.missTime = 0;
               BlockPos blockpos = miningHit.getBlockPos();
               if (!mc.level.getBlockState(blockpos).isAir()) {
                  InteractionKeyMappingTriggered inputEvent = ClientHooks.onClickInput(0, mc.options.keyAttack, InteractionHand.MAIN_HAND);
                  if (inputEvent.isCanceled()) {
                     if (inputEvent.shouldSwingHand()) {
                        mc.particleEngine.addBlockHitEffects(blockpos, miningHit);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                     }

                     ci.cancel();
                  } else {
                     Direction direction = miningHit.getDirection();
                     if (mc.gameMode.continueDestroyBlock(blockpos, direction) && inputEvent.shouldSwingHand()) {
                        mc.particleEngine.addBlockHitEffects(blockpos, miningHit);
                        mc.player.swing(InteractionHand.MAIN_HAND);
                     }

                     ci.cancel();
                  }
               }
            }
         }
      }
   }

   @Inject(
      method = {"startAttack"},
      at = {@At("HEAD")}
   )
   private void simulatedcoasters$clearMissTimeForConnectedAnchorStartAttack(CallbackInfoReturnable<Boolean> cir) {
      Minecraft mc = (Minecraft)(Object)this;
      if (!(mc.hitResult instanceof BlockHitResult bhr) || mc.hitResult.getType() != Type.BLOCK) {
         return;
      }

      if (mc.level != null) {
         if (CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak(mc.level, bhr.getBlockPos())) {
            this.missTime = 0;
         }
      }
   }

   @Inject(
      method = {"startAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$startAttackHoveredCurve(CallbackInfoReturnable<Boolean> cir) {
      Minecraft mc = (Minecraft)(Object)this;
      if (mc.hitResult == null || mc.hitResult.getType() != Type.BLOCK) {
         BlockHitResult miningHit = AnchorPeerCurvePick.syntheticMiningHit(mc);
         if (miningHit == null) {
            if (this.missTime > 0 || mc.player == null || mc.player.isHandsBusy()) {
               return;
            }
         } else if (mc.player == null || mc.player.isHandsBusy()) {
            return;
         }

         if (miningHit != null && mc.level != null && mc.gameMode != null) {
            this.missTime = 0;
            ItemStack itemstack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
            if (itemstack.isItemEnabled(mc.level.enabledFeatures())) {
               InteractionKeyMappingTriggered inputEvent = ClientHooks.onClickInput(0, mc.options.keyAttack, InteractionHand.MAIN_HAND);
               if (inputEvent.isCanceled()) {
                  if (inputEvent.shouldSwingHand()) {
                     mc.player.swing(InteractionHand.MAIN_HAND);
                  }

                  cir.setReturnValue(false);
                  cir.cancel();
               } else {
                  BlockPos blockpos = miningHit.getBlockPos();
                  if (!mc.level.getBlockState(blockpos).isAir()) {
                     mc.gameMode.startDestroyBlock(blockpos, miningHit.getDirection());
                  }

                  if (inputEvent.shouldSwingHand()) {
                     mc.player.swing(InteractionHand.MAIN_HAND);
                  }

                  cir.setReturnValue(false);
                  cir.cancel();
               }
            }
         }
      }
   }
}
