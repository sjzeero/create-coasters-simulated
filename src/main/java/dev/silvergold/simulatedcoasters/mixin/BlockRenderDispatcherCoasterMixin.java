package dev.silvergold.simulatedcoasters.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.silvergold.simulatedcoasters.client.track.AnchorCurveMiningFilter;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({BlockRenderDispatcher.class})
public class BlockRenderDispatcherCoasterMixin {
   @Inject(
      method = {"renderBreakingTexture(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/BlockAndTintGetter;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/neoforged/neoforge/client/model/data/ModelData;)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$skipAnchorCapCrackDuringCurveMine(
      BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack poseStack, VertexConsumer consumer, ModelData modelData, CallbackInfo ci
   ) {
      if (state.getBlock() instanceof CoasterAnchorpointBlock && AnchorCurveMiningFilter.suppressAnchorBlockBreakTexture(pos)) {
         ci.cancel();
      }
   }
}
