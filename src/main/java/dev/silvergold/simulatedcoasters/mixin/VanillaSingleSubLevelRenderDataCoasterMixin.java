/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  dev.ryanhcode.sable.sublevel.ClientSubLevel
 *  dev.ryanhcode.sable.sublevel.render.vanilla.VanillaSingleSubLevelRenderData
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  org.joml.Matrix3f
 *  org.joml.Matrix3fc
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaSingleSubLevelRenderData;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.rivet.RivetPlacementAnimationClient;
import dev.silvergold.simulatedcoasters.client.rivet.RivetSubLevelRenderLight;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={VanillaSingleSubLevelRenderData.class})
public abstract class VanillaSingleSubLevelRenderDataCoasterMixin {
    @Shadow
    private ClientSubLevel subLevel;
    @Shadow
    private BlockState singleBlockState;

    @Inject(method={"renderSingleBlock(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;DDD)V"}, at={@At(value="INVOKE", target="Ldev/ryanhcode/sable/platform/SableSubLevelRenderPlatform;tesselateBlock(Ldev/ryanhcode/sable/sublevel/render/vanilla/SingleBlockSubLevelWrapper;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/util/RandomSource;JILnet/minecraft/client/renderer/RenderType;)V", shift=At.Shift.BEFORE)})
    private void simulatedcoasters$beforeTesselate(RenderType layer, VertexConsumer consumer, Matrix4f modelView, double camX, double camY, double camZ, CallbackInfo ci, @Local(name={"stack"}) PoseStack stack) {
        RivetSubLevelRenderLight.begin(this.subLevel, this.singleBlockState);
        if (!RivetPlacementAnimationClient.hasActive()) {
            return;
        }
        if (!this.singleBlockState.is((Block)SimulatedCoastersBlocks.RIVET.get())) {
            return;
        }
        float progress = RivetPlacementAnimationClient.progress(this.subLevel.getUniqueId());
        if (progress < 0.0f) {
            return;
        }
        float remaining = 1.0f - RivetPlacementAnimationClient.easeIn(progress);
        float yOffset = 0.5f * remaining;
        float angleRad = (float)Math.toRadians(90.0 * (double)remaining);
        Matrix4f anim = new Matrix4f().translate(0.5f, 0.9375f, 0.5f).rotateY(angleRad).translate(-0.5f, -0.9375f, -0.5f).translate(0.0f, yOffset, 0.0f);
        stack.last().pose().mul((Matrix4fc)anim);
        stack.last().normal().mul((Matrix3fc)new Matrix3f((Matrix4fc)anim));
    }

    @Inject(method={"renderSingleBlock(Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lorg/joml/Matrix4f;DDD)V"}, at={@At(value="INVOKE", target="Ldev/ryanhcode/sable/platform/SableSubLevelRenderPlatform;tesselateBlock(Ldev/ryanhcode/sable/sublevel/render/vanilla/SingleBlockSubLevelWrapper;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/util/RandomSource;JILnet/minecraft/client/renderer/RenderType;)V", shift=At.Shift.AFTER)})
    private void simulatedcoasters$afterTesselate(RenderType layer, VertexConsumer consumer, Matrix4f modelView, double camX, double camY, double camZ, CallbackInfo ci) {
        RivetSubLevelRenderLight.end();
    }
}

