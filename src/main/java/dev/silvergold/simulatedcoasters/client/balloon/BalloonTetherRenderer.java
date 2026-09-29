package dev.silvergold.simulatedcoasters.client.balloon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostKey;
import dev.silvergold.simulatedcoasters.balloon.BalloonPlacement;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3d;

@OnlyIn(Dist.CLIENT)
public final class BalloonTetherRenderer {
   private static final double MAX_RENDER_DISTANCE_SQ = 4096.0;
   private static final float STRING_R = 0.92941177F;
   private static final float STRING_G = 0.9490196F;
   private static final float STRING_B = 0.9490196F;
   private static final double HOST_RENDER_INSET_BLOCKS = 0.0625;
   private static final float STRING_WIDTH_BLOCKS = 0.032F;

   private BalloonTetherRenderer() {
   }

   public static void render(Minecraft mc, PoseStack poseStack, BufferSource buffers, Camera camera, float partialTick) {
      Level level = mc.level;
      if (level != null) {
         SubLevelContainer container = SubLevelContainer.getContainer(level);
         if (container != null) {
            Vec3 cam = camera.getPosition();

            for (SubLevel balloonSub : container.getAllSubLevels()) {
               if (!balloonSub.isRemoved() && BalloonResolve.isBalloonSubLevel(balloonSub)) {
                  EmbeddedPlotLevelAccessor embedded = balloonSub.getPlot().getEmbeddedLevelAccessor();
                  BlockEntity hostPlotLocal = embedded.getBlockEntity(BlockPos.ZERO);
                  if (hostPlotLocal instanceof BalloonBlockEntity) {
                     BalloonBlockEntity be = (BalloonBlockEntity)hostPlotLocal;
                     BlockPos hostPlotLocalx = be.hostPos();
                     SubLevel hostSub = BalloonClientSpace.resolveHostSubLevel(level, be);
                     if (hostSub != null) {
                        BlockPos balloonPlotLocal = BalloonHostKey.BALLOON_BLOCK_CELL;
                        Vec3 tieWorld = BalloonClientSpace.plotPointRenderWorld(
                           balloonSub, BalloonPlacement.stringTiePlot(balloonSub, balloonPlotLocal), partialTick
                        );
                        Vector3d hostPlot = BalloonPlacement.hostAnchorPlot(hostSub, hostPlotLocalx);
                        hostPlot.y -= 0.0625;
                        Vec3 hostWorld = BalloonClientSpace.plotPointRenderWorld(hostSub, hostPlot, partialTick);
                        if (!(cam.distanceToSqr(tieWorld) > 4096.0) || !(cam.distanceToSqr(hostWorld) > 4096.0)) {
                           renderLeashSegment(poseStack, buffers, cam, tieWorld, hostWorld, level);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void renderLeashSegment(PoseStack poseStack, BufferSource buffers, Vec3 camera, Vec3 startWorld, Vec3 endWorld, Level level) {
      poseStack.pushPose();
      poseStack.translate(startWorld.x - camera.x, startWorld.y - camera.y, startWorld.z - camera.z);
      float dx = (float)(endWorld.x - startWorld.x);
      float dy = (float)(endWorld.y - startWorld.y);
      float dz = (float)(endWorld.z - startWorld.z);
      Matrix4f pose = poseStack.last().pose();
      float halfWidth = 0.016F;
      float lenSq = dx * dx + dy * dy + dz * dz;
      if (lenSq < 1.0E-10F) {
         poseStack.popPose();
      } else {
         float invLen = Mth.invSqrt(lenSq);
         float dirX = dx * invLen;
         float dirY = dy * invLen;
         float dirZ = dz * invLen;
         float refX = 0.0F;
         float refY = 1.0F;
         float refZ = 0.0F;
         if (Math.abs(dirY) > 0.95F) {
            refX = 1.0F;
            refY = 0.0F;
         }

         float side1X = dirY * refZ - dirZ * refY;
         float side1Y = dirZ * refX - dirX * refZ;
         float side1Z = dirX * refY - dirY * refX;
         float side1LenSq = side1X * side1X + side1Y * side1Y + side1Z * side1Z;
         if (side1LenSq < 1.0E-10F) {
            poseStack.popPose();
         } else {
            float side1Scale = Mth.invSqrt(side1LenSq) * halfWidth;
            side1X *= side1Scale;
            side1Y *= side1Scale;
            side1Z *= side1Scale;
            float side2X = dirY * side1Z - dirZ * side1Y;
            float side2Y = dirZ * side1X - dirX * side1Z;
            float side2Z = dirX * side1Y - dirY * side1X;
            BlockPos startLightPos = BlockPos.containing(startWorld);
            BlockPos endLightPos = BlockPos.containing(endWorld);
            int startBlockLight = level.getBrightness(LightLayer.BLOCK, startLightPos);
            int endBlockLight = level.getBrightness(LightLayer.BLOCK, endLightPos);
            int startSkyLight = level.getBrightness(LightLayer.SKY, startLightPos);
            int endSkyLight = level.getBrightness(LightLayer.SKY, endLightPos);
            renderLeashRibbon(buffers, pose, dx, dy, dz, startBlockLight, endBlockLight, startSkyLight, endSkyLight, side1X, side1Y, side1Z);
            renderLeashRibbon(buffers, pose, dx, dy, dz, startBlockLight, endBlockLight, startSkyLight, endSkyLight, side2X, side2Y, side2Z);
            poseStack.popPose();
         }
      }
   }

   private static void renderLeashRibbon(
      BufferSource buffers,
      Matrix4f pose,
      float dx,
      float dy,
      float dz,
      int startBlockLight,
      int endBlockLight,
      int startSkyLight,
      int endSkyLight,
      float sideX,
      float sideY,
      float sideZ
   ) {
      VertexConsumer buffer = buffers.getBuffer(RenderType.leash());

      for (int i = 0; i <= 24; i++) {
         addLeashVertexPair(buffer, pose, dx, dy, dz, startBlockLight, endBlockLight, startSkyLight, endSkyLight, 0.032F, 0.032F, sideX, sideY, sideZ, i);
      }

      for (int i = 24; i >= 0; i--) {
         addLeashVertexPair(buffer, pose, dx, dy, dz, startBlockLight, endBlockLight, startSkyLight, endSkyLight, 0.032F, 0.0F, sideX, sideY, sideZ, i);
      }

      buffers.endBatch(RenderType.leash());
   }

   private static void addLeashVertexPair(
      VertexConsumer buffer,
      Matrix4f pose,
      float startX,
      float startY,
      float startZ,
      int startBlockLight,
      int endBlockLight,
      int startSkyLight,
      int endSkyLight,
      float yOffset,
      float dy,
      float sideX,
      float sideY,
      float sideZ,
      int index
   ) {
      float t = (float)index / 24.0F;
      int blockLight = (int)Mth.lerp(t, (float)startBlockLight, (float)endBlockLight);
      int skyLight = (int)Mth.lerp(t, (float)startSkyLight, (float)endSkyLight);
      int packedLight = LightTexture.pack(blockLight, skyLight);
      float x = startX * t;
      float y = startY > 0.0F ? startY * t * t : startY - startY * (1.0F - t) * (1.0F - t);
      float z = startZ * t;
      buffer.addVertex(pose, x - sideX, y + dy, z - sideZ).setColor(0.92941177F, 0.9490196F, 0.9490196F, 1.0F).setLight(packedLight);
      buffer.addVertex(pose, x + sideX, y + yOffset - dy, z + sideZ).setColor(0.92941177F, 0.9490196F, 0.9490196F, 1.0F).setLight(packedLight);
   }
}
