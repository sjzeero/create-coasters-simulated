package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.silvergold.simulatedcoasters.track.anchor.ChainLiftClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftDriveDirection;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class ChainLiftSpanRenderer {
   private static final ResourceLocation CHAIN_TEXTURE = ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "textures/block/chain_lift/chain.png");
   private static final double SAMPLE_SPACING_NEAR_BLOCKS = 0.25;
   private static final double SAMPLE_SPACING_MID_BLOCKS = 0.5;
   private static final double SAMPLE_SPACING_FAR_BLOCKS = 1.0;
   private static final double MID_LOD_DISTANCE_SQ = 1024.0;
   private static final double FAR_LOD_DISTANCE_SQ = 4096.0;

   private ChainLiftSpanRenderer() {
   }

   public static void render(Minecraft mc, PoseStack poseStack, BufferSource buffers, Camera camera, float partialTick) {
      Level level = mc.level;
      if (level != null) {
         Vec3 cam = camera.getPosition();
         poseStack.pushPose();
         poseStack.translate(-cam.x, -cam.y, -cam.z);
         Matrix4f mat = poseStack.last().pose();
         Pose poseEntry = poseStack.last();
         double partialTickForRender = (double)partialTick;
         double maxRenderDistSq = 16384.0;

         for (CoasterAnchorpointBlockEntity anchor : ChainLiftClientIndex.snapshotAnchors()) {
            if (!anchor.isRemoved() && anchor.getLevel() == level) {
               BlockPos anchorPos = anchor.getBlockPos();
               Vec3 anchorWorld = anchorRenderCenter(level, anchorPos, partialTickForRender);
               if (!(cam.distanceToSqr(anchorWorld) > maxRenderDistSq)) {
                  CoasterPathGraphSpans.SpanPath path = ChainLiftSpanPathCache.resolve(anchor, partialTickForRender);
                  if (path != null && !path.segments().isEmpty()) {
                     double sampleSpacing = sampleSpacingForDistance(cam, anchorWorld);
                     int lodTier = lodTierForSpacing(sampleSpacing);
                     float scrollPhase = scrollPhase(
                        ChainLiftDriveDirection.signedChainBlocksPerSecond(anchor, path, partialTickForRender, 256.0F, 16.0), level, partialTick
                     );
                     ChainLiftRibbonMeshCache.CachedMesh mesh = null;
                     if (ChainLiftRibbonMeshCache.pathUsesSubLevelTrack(level, path)) {
                        if (!ChainLiftRibbonMeshCache.spanNearCameraLive(level, path, cam, partialTickForRender, maxRenderDistSq)) {
                           continue;
                        }

                        VertexConsumer ribbonConsumer = buffers.getBuffer(RenderType.entityCutout(CHAIN_TEXTURE));
                        ChainLiftRibbonMeshCache.drawLiveSpan(level, path, sampleSpacing, mat, poseEntry, ribbonConsumer, partialTickForRender, scrollPhase);
                     } else {
                        mesh = ChainLiftRibbonMeshCache.getOrBuild(anchor, level, path, sampleSpacing, lodTier);
                        if (mesh == null || !ChainLiftRibbonMeshCache.spanNearCamera(level, cam, mesh, partialTickForRender, maxRenderDistSq)) {
                           continue;
                        }

                        VertexConsumer ribbonConsumer = buffers.getBuffer(RenderType.entityCutout(CHAIN_TEXTURE));
                        ChainLiftRibbonMeshCache.draw(mesh, level, mat, poseEntry, ribbonConsumer, partialTickForRender, scrollPhase);
                     }

                     ChainLiftSpanEndcapRenderer.renderSpan(level, path, mesh, partialTickForRender, cam, maxRenderDistSq, poseStack, buffers);
                  }
               }
            }
         }

         poseStack.popPose();
      }
   }

   private static Vec3 anchorRenderCenter(Level level, BlockPos anchorPos, double partialTickForRender) {
      Vec3 local = CoasterAnchorpointBlockEntity.worldCenter(level, anchorPos);
      return CoasterAnchorClientSpace.toRenderWorld(level, anchorPos, local, (float)partialTickForRender);
   }

   private static double sampleSpacingForDistance(Vec3 camera, Vec3 anchorWorld) {
      double distSq = camera.distanceToSqr(anchorWorld);
      if (distSq >= 4096.0) {
         return 1.0;
      } else {
         return distSq >= 1024.0 ? 0.5 : 0.25;
      }
   }

   private static int lodTierForSpacing(double sampleSpacing) {
      if (sampleSpacing >= 1.0) {
         return 2;
      } else {
         return sampleSpacing >= 0.5 ? 1 : 0;
      }
   }

   private static float scrollPhase(double blocksPerSecond, Level level, float partialTick) {
      if (Math.abs(blocksPerSecond) <= 1.0E-9) {
         return 0.0F;
      } else {
         double time = (double)((float)level.getGameTime() + partialTick) / 20.0;
         double scroll = time * blocksPerSecond / 0.625;
         return (float)(scroll - Math.floor(scroll));
      }
   }
}
