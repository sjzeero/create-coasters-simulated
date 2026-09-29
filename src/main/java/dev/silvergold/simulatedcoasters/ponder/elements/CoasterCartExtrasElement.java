package dev.silvergold.simulatedcoasters.ponder.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartBottomSliceRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartTrainCouplingRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartWheelAxisRenderer;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import java.util.ArrayList;
import java.util.List;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CoasterCartExtrasElement extends AnimatedSceneElementBase implements AnimatedSceneElement {
   private final List<BlockPos> cartCorners;
   private final float bodyYawDegrees;
   private final List<CoasterCartExtrasElement.CouplerSpan> couplers;
   private final boolean hadCouplers;
   private boolean couplerVisible;

   public CoasterCartExtrasElement(List<BlockPos> cartCorners, float bodyYawDegrees, List<CoasterCartExtrasElement.CouplerSpan> couplers) {
      this.cartCorners = List.copyOf(cartCorners);
      this.bodyYawDegrees = bodyYawDegrees;
      this.couplers = List.copyOf(couplers);
      this.hadCouplers = !this.couplers.isEmpty();
      this.couplerVisible = this.hadCouplers;
   }

   public void setCouplerVisible(boolean visible) {
      this.couplerVisible = visible;
   }

   public void reset(PonderScene scene) {
      super.reset(scene);
      this.couplerVisible = this.hadCouplers;
   }

   public static CoasterCartExtrasElement carts(float bodyYawDegrees, BlockPos... corners) {
      return new CoasterCartExtrasElement(List.of(corners), bodyYawDegrees, List.of());
   }

   public static CoasterCartExtrasElement linkedCarts(float bodyYawDegrees, BlockPos cartA, BlockPos cartB) {
      return trainCarts(bodyYawDegrees, cartA, cartB);
   }

   public static CoasterCartExtrasElement trainCarts(float bodyYawDegrees, BlockPos... corners) {
      List<BlockPos> carts = List.of(corners);
      List<CoasterCartExtrasElement.CouplerSpan> spans = new ArrayList<>();

      for (int i = 0; i + 1 < carts.size(); i++) {
         Vec3 a = Vec3.atCenterOf((Vec3i)carts.get(i));
         Vec3 b = Vec3.atCenterOf((Vec3i)carts.get(i + 1));
         spans.add(new CoasterCartExtrasElement.CouplerSpan(a, b, a.distanceTo(b)));
      }

      return new CoasterCartExtrasElement(carts, bodyYawDegrees, spans);
   }

   protected void renderLast(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float fade, float pt) {
      if (!(fade < 0.0625F)) {
         Minecraft mc = Minecraft.getInstance();
         BlockState tintState = ((CoasterCartBlock)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get()).defaultBlockState();
         int tint = mc.getBlockColors().getColor(tintState, null, null, 0);
         float cr = (float)(tint >> 16 & 0xFF) / 255.0F;
         float cg = (float)(tint >> 8 & 0xFF) / 255.0F;
         float cb = (float)(tint & 0xFF) / 255.0F;
         PoseStack poseStack = graphics.pose();

         for (BlockPos corner : this.cartCorners) {
            Vec3 center = Vec3.atCenterOf(corner);
            poseStack.pushPose();
            if (this.bodyYawDegrees != 0.0F) {
               poseStack.translate(center.x, center.y, center.z);
               poseStack.mulPose(Axis.YP.rotationDegrees(this.bodyYawDegrees));
               poseStack.translate(-center.x, -center.y, -center.z);
            }

            CoasterCartBottomSliceRenderer.renderStandingAt(poseStack, buffer, tintState, cr, cg, cb, world, corner, 0.0, 0.0, 0.0);
            CoasterCartWheelAxisRenderer.renderStandingAt(poseStack, buffer, tintState, cr, cg, cb, world, corner, 0.0, 0.0, 0.0);
            poseStack.popPose();
         }

         if (this.couplerVisible && !this.couplers.isEmpty()) {
            VertexConsumer vb = buffer.getBuffer(RenderType.solid());
            BlockState air = Blocks.AIR.defaultBlockState();

            for (CoasterCartExtrasElement.CouplerSpan span : this.couplers) {
               int lightA = lightAt(world, span.a());
               int lightB = lightAt(world, span.b());
               CoasterCartTrainCouplingRenderer.renderCoupling(poseStack, vb, Vec3.ZERO, air, span.a(), span.b(), span.restLength(), lightA, lightB);
            }
         }
      }
   }

   private static int lightAt(PonderLevel world, Vec3 pos) {
      BlockPos blockPos = BlockPos.containing(pos);
      return LightTexture.pack(world.getBrightness(LightLayer.BLOCK, blockPos), world.getBrightness(LightLayer.SKY, blockPos));
   }

   private static record CouplerSpan(Vec3 a, Vec3 b, double restLength) {
   }
}
