/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.KeyMapping
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.neoforged.neoforge.client.ClientHooks
 *  net.neoforged.neoforge.client.event.InputEvent$InteractionKeyMappingTriggered
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurvePick;
import dev.silvergold.simulatedcoasters.client.track.CoasterConnectedAnchorBreakClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Minecraft.class})
public abstract class MinecraftCoasterCurvePickMixin {
    @Shadow
    protected int missTime;

    @Inject(method={"pickBlock"}, at={@At(value="HEAD")})
    private void simulatedcoasters$retargetPickBlockHoveredCurve(CallbackInfo ci) {
        Minecraft mc = (Minecraft)this;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult pickHit = AnchorPeerCurvePick.syntheticPickBlockHit(mc);
        if (pickHit != null) {
            mc.hitResult = pickHit;
        }
    }

    @Inject(method={"continueAttack"}, at={@At(value="HEAD")})
    private void simulatedcoasters$clearMissTimeForConnectedAnchorBreak(boolean leftClick, CallbackInfo ci) {
        BlockHitResult bhr;
        Minecraft mc;
        block7: {
            block6: {
                if (!leftClick) {
                    return;
                }
                mc = (Minecraft)this;
                HitResult hitResult = mc.hitResult;
                if (!(hitResult instanceof BlockHitResult)) break block6;
                bhr = (BlockHitResult)hitResult;
                if (mc.hitResult.getType() == HitResult.Type.BLOCK) break block7;
            }
            return;
        }
        if (mc.level == null) {
            return;
        }
        if (CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak((Level)mc.level, bhr.getBlockPos())) {
            this.missTime = 0;
        }
    }

    @Inject(method={"continueAttack"}, at={@At(value="HEAD")}, cancellable=true)
    private void simulatedcoasters$continueAttackHoveredCurve(boolean leftClick, CallbackInfo ci) {
        Minecraft mc = (Minecraft)this;
        if (!leftClick) {
            return;
        }
        if (mc.hitResult instanceof BlockHitResult && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult miningHit = AnchorPeerCurvePick.syntheticMiningHit(mc);
        if (miningHit == null ? this.missTime > 0 || mc.player.isUsingItem() : mc.player == null || mc.player.isUsingItem()) {
            return;
        }
        if (miningHit == null || mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }
        this.missTime = 0;
        BlockPos blockpos = miningHit.getBlockPos();
        if (mc.level.getBlockState(blockpos).isAir()) {
            return;
        }
        InputEvent.InteractionKeyMappingTriggered inputEvent = ClientHooks.onClickInput((int)0, (KeyMapping)mc.options.keyAttack, (InteractionHand)InteractionHand.MAIN_HAND);
        if (inputEvent.isCanceled()) {
            if (inputEvent.shouldSwingHand()) {
                mc.particleEngine.addBlockHitEffects(blockpos, miningHit);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
            ci.cancel();
            return;
        }
        Direction direction = miningHit.getDirection();
        if (mc.gameMode.continueDestroyBlock(blockpos, direction) && inputEvent.shouldSwingHand()) {
            mc.particleEngine.addBlockHitEffects(blockpos, miningHit);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        ci.cancel();
    }

    @Inject(method={"startAttack"}, at={@At(value="HEAD")})
    private void simulatedcoasters$clearMissTimeForConnectedAnchorStartAttack(CallbackInfoReturnable<Boolean> cir) {
        BlockHitResult bhr;
        Minecraft mc;
        block6: {
            block5: {
                mc = (Minecraft)this;
                HitResult hitResult = mc.hitResult;
                if (!(hitResult instanceof BlockHitResult)) break block5;
                bhr = (BlockHitResult)hitResult;
                if (mc.hitResult.getType() == HitResult.Type.BLOCK) break block6;
            }
            return;
        }
        if (mc.level == null) {
            return;
        }
        if (CoasterConnectedAnchorBreakClient.requiresCreativePacedBreak((Level)mc.level, bhr.getBlockPos())) {
            this.missTime = 0;
        }
    }

    @Inject(method={"startAttack"}, at={@At(value="HEAD")}, cancellable=true)
    private void simulatedcoasters$startAttackHoveredCurve(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = (Minecraft)this;
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            return;
        }
        BlockHitResult miningHit = AnchorPeerCurvePick.syntheticMiningHit(mc);
        if (miningHit == null ? this.missTime > 0 || mc.player == null || mc.player.isHandsBusy() : mc.player == null || mc.player.isHandsBusy()) {
            return;
        }
        if (miningHit == null || mc.level == null || mc.gameMode == null) {
            return;
        }
        this.missTime = 0;
        ItemStack itemstack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!itemstack.isItemEnabled(mc.level.enabledFeatures())) {
            return;
        }
        InputEvent.InteractionKeyMappingTriggered inputEvent = ClientHooks.onClickInput((int)0, (KeyMapping)mc.options.keyAttack, (InteractionHand)InteractionHand.MAIN_HAND);
        if (inputEvent.isCanceled()) {
            if (inputEvent.shouldSwingHand()) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
            cir.setReturnValue((Object)false);
            cir.cancel();
            return;
        }
        BlockPos blockpos = miningHit.getBlockPos();
        if (!mc.level.getBlockState(blockpos).isAir()) {
            mc.gameMode.startDestroyBlock(blockpos, miningHit.getDirection());
        }
        if (inputEvent.shouldSwingHand()) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
        cir.setReturnValue((Object)false);
        cir.cancel();
    }
}

