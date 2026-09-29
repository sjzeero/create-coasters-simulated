package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChainLiftAnchorShaftRenderer extends KineticBlockEntityRenderer<CoasterAnchorpointBlockEntity> {
   public ChainLiftAnchorShaftRenderer(Context context) {
      super(context);
   }

   protected void renderSafe(CoasterAnchorpointBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      AnchorPeerTrackCurveBerRender.render(be, ms, buffer);
      this.renderChainLiftShaft(be, partialTicks, ms, buffer, light);
   }

   private void renderChainLiftShaft(CoasterAnchorpointBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
      Axis axis = be.getChainLiftShaftAxis();
      if (axis != null) {
         BlockState anchorState = be.getBlockState();
         Direction facing = (Direction)anchorState.getValue(CoasterAnchorpointBlock.FACING);
         CoasterAnchorpointShaftModel.renderBody(ms, buffer, anchorState, facing, axis, light);
         BlockState shaftState = shaft(axis);
         SuperByteBuffer model = CachedBuffers.block(KINETIC_BLOCK, shaftState);
         BlockPos pos = be.getBlockPos();
         float angle = getAngleForBe(be, pos, axis);
         kineticRotationTransform(model, be, axis, angle, light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
      }
   }

   public boolean shouldRenderOffScreen(CoasterAnchorpointBlockEntity blockEntity) {
      return true;
   }

   public int getViewDistance() {
      return 192;
   }
}
