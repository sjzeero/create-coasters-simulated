package dev.silvergold.simulatedcoasters.client.cart;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ModelEvent.ModifyBakingResult;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartBottomSliceRenderer {
   public static final ModelResourceLocation STANDALONE_BOTTOM_MRL = ModelResourceLocation.standalone(
      ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "block/coaster_cart_bottom")
   );
   private static BakedModel bottomModel;

   private CoasterCartBottomSliceRenderer() {
   }

   public static void registerStandaloneModel(RegisterAdditional event) {
      event.register(STANDALONE_BOTTOM_MRL);
   }

   public static void cacheBottomModel(ModifyBakingResult event) {
      bottomModel = (BakedModel)event.getModels().get(STANDALONE_BOTTOM_MRL);
   }

   static boolean hasModel() {
      return bottomModel != null;
   }

   public static void renderStandingAt(
      PoseStack poseStack,
      MultiBufferSource buffers,
      BlockState tintState,
      float cr,
      float cg,
      float cb,
      Level level,
      BlockPos corner,
      double camX,
      double camY,
      double camZ
   ) {
      if (bottomModel != null) {
         Minecraft mc = Minecraft.getInstance();
         ModelBlockRenderer modelRenderer = mc.getBlockRenderer().getModelRenderer();
         RandomSource renderRand = RandomSource.create(42L);
         int light = LevelRenderer.getLightColor(level, corner);
         poseStack.pushPose();
         poseStack.translate((double)corner.getX() - camX, (double)corner.getY() - camY, (double)corner.getZ() - camZ);

         for (RenderType rt : bottomModel.getRenderTypes(tintState, renderRand, ModelData.EMPTY)) {
            VertexConsumer consumer = buffers.getBuffer(rt);
            modelRenderer.renderModel(poseStack.last(), consumer, tintState, bottomModel, cr, cg, cb, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, rt);
         }

         poseStack.popPose();
      }
   }

   static Vec3 pathAnchorForBottomSlab(Level level, CoasterPathTrackFrame.GraphHit hit) {
      return CoasterPathTrackFrame.pathAnchorForSnappedBlock(level, hit);
   }

   static Vec3 pathAnchorForBottomSlab(Level level, CoasterPathTrackFrame.GraphHit hit, float partialTick) {
      return CoasterPathTrackFrame.pathAnchorForSnappedBlock(level, hit, partialTick);
   }

   static void renderCell(
      PoseStack poseStack,
      ModelBlockRenderer modelRenderer,
      BufferSource buffers,
      BlockState tintState,
      float cr,
      float cg,
      float cb,
      RandomSource renderRand,
      Level level,
      CoasterCartClientFrameCache.RenderFrame frame,
      double camX,
      double camY,
      double camZ,
      MutableBlockPos mut
   ) {
      if (bottomModel != null) {
         Pose3d visualPose = frame.visualPose();
         Quaternionf qBody = frame.qBody();
         Quaternionf qPath = frame.qPath();
         Vec3 pathAnchorBottom = frame.pathAnchorBottom();
         poseStack.pushPose();
         Vec3 lightSampleWorld = visualPose.transformPosition(Vec3.atCenterOf(mut));
         BlockPos lightPos = BlockPos.containing(lightSampleWorld);
         if (qPath != null && pathAnchorBottom != null) {
            poseStack.translate(pathAnchorBottom.x - camX, pathAnchorBottom.y - camY, pathAnchorBottom.z - camZ);
            poseStack.mulPose(qPath);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
         } else {
            Vec3 cornerWorld = visualPose.transformPosition(Vec3.atLowerCornerOf(mut));
            poseStack.translate(cornerWorld.x - camX, cornerWorld.y - camY, cornerWorld.z - camZ);
            poseStack.mulPose(qBody);
         }

         int light = LevelRenderer.getLightColor(level, lightPos);

         for (RenderType rt : bottomModel.getRenderTypes(tintState, renderRand, ModelData.EMPTY)) {
            VertexConsumer consumer = buffers.getBuffer(rt);
            modelRenderer.renderModel(poseStack.last(), consumer, tintState, bottomModel, cr, cg, cb, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, rt);
         }

         poseStack.popPose();
      }
   }

   @Deprecated
   public static void render(Minecraft mc, PoseStack poseStack, BufferSource buffers, Camera camera, DeltaTracker deltaTracker) {
      CoasterCartWorldRenderer.render(mc, poseStack, buffers, camera, deltaTracker);
   }
}
