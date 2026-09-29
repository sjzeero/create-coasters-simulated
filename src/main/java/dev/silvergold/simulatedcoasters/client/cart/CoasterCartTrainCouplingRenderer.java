package dev.silvergold.simulatedcoasters.client.cart;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.sound.CoasterCartRailSoundCache;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkSync;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTraceEndpoints;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartTrainCouplingRenderer {
   private static final float MARGIN = 0.1875F;

   private CoasterCartTrainCouplingRenderer() {
   }

   public static void render(Minecraft mc, PoseStack poseStack, BufferSource buffers, Camera camera, DeltaTracker deltaTracker) {
      ClientLevel level = mc.level;
      if (level != null) {
         Vec3 cam = camera.getPosition();
         VertexConsumer vb = buffers.getBuffer(RenderType.solid());
         BlockState air = Blocks.AIR.defaultBlockState();
         CoasterPathGraph pathGraph = CoasterPathGraphManagerClient.get(level);
         List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs = CoasterCartRailSoundCache.openPathEndRefs();
         float partialTick = deltaTracker.getGameTimeDeltaPartialTick(level.tickRateManager().runsNormally());
         Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>> links = CoasterCartTrainLinkClientState.snapshot(level.dimension());
         if (!links.isEmpty()) {
            CoasterCartClientMembership.forEachCart(
               level,
               sub -> {
                  List<CoasterCartTrainLinkSync.LinkEntry> cartLinks = links.get(sub.getUniqueId());
                  if (cartLinks != null && !cartLinks.isEmpty()) {
                     CoasterCartClientFrameCache.RenderFrame frameA = CoasterCartClientFrameCache.resolveRender(level, sub, pathGraph, openEndRefs, partialTick);
                     if (frameA != null) {
                        Vec3 centerA = frameA.sampleVisual();
                        if (centerA.closerThan(cam, 64.0)) {
                           for (CoasterCartTrainLinkSync.LinkEntry link : cartLinks) {
                              if (sub.getUniqueId().compareTo(link.partnerId()) <= 0) {
                                 SubLevel partner = CoasterCartResolve.findCartById(level, link.partnerId());
                                 if (partner != null && !partner.isRemoved()) {
                                    CoasterCartClientFrameCache.RenderFrame frameB = CoasterCartClientFrameCache.resolveRender(
                                       level, partner, pathGraph, openEndRefs, partialTick
                                    );
                                    if (frameB != null) {
                                       Vec3 centerB = frameB.sampleVisual();
                                       int lightA = lightAt(level, centerA);
                                       int lightB = lightAt(level, centerB);
                                       renderCoupling(poseStack, vb, cam, air, centerA, centerB, link.centerDistance(), lightA, lightB);
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            );
         }
      }
   }

   public static void renderCoupling(
      PoseStack ms, VertexConsumer vb, Vec3 camera, BlockState air, Vec3 anchorWorld, Vec3 anchor2World, double restLength, int light1, int light2
   ) {
      double diffX = anchor2World.x - anchorWorld.x;
      double diffY = anchor2World.y - anchorWorld.y;
      double diffZ = anchor2World.z - anchorWorld.z;
      float yRot = AngleHelper.deg(Mth.atan2(diffZ, diffX)) + 90.0F;
      float xRot = AngleHelper.deg(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)));
      Vec3 span = anchor2World.subtract(anchorWorld);
      double len = span.length();
      if (!(len < 1.0E-6)) {
         Vec3 dir = span.scale(1.0 / len);
         Vec3 anchor = anchorWorld.add(dir.scale(0.1875));
         Vec3 anchor2 = anchor2World.add(dir.scale(-0.1875));
         int couplingSegments = Math.max(1, (int)Math.round((restLength - 0.375) * 4.0));
         double stretch = (anchor2.distanceTo(anchor) - 0.375) * 4.0 / (double)couplingSegments;
         ms.pushPose();
         ms.translate(anchor.x - camera.x, anchor.y - camera.y, anchor.z - camera.z);
         ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.TRAIN_COUPLING_HEAD, air).rotateYDegrees(-yRot)).rotateXDegrees(xRot))
            .light(light1)
            .renderInto(ms, vb);

         for (int j = 0; j < couplingSegments; j++) {
            ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(
                                 AllPartialModels.TRAIN_COUPLING_CABLE, air
                              )
                              .rotateYDegrees(-yRot + 180.0F))
                           .rotateXDegrees(-xRot))
                        .translate(0.0F, 0.0F, 0.3125F))
                     .scale(1.0F, 1.0F, (float)stretch))
                  .translate(0.0F, 0.0F, (float)j / 4.0F))
               .light(light1)
               .renderInto(ms, vb);
         }

         ms.popPose();
         ms.pushPose();
         ms.translate(anchor2.x - camera.x, anchor2.y - camera.y, anchor2.z - camera.z);
         ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(AllPartialModels.TRAIN_COUPLING_HEAD, air).rotateYDegrees(-yRot + 180.0F))
               .rotateXDegrees(-xRot))
            .light(light2)
            .renderInto(ms, vb);
         ms.popPose();
      }
   }

   private static int lightAt(Level level, Vec3 pos) {
      BlockPos blockpos = BlockPos.containing(pos);
      return LightTexture.pack(level.getBrightness(LightLayer.BLOCK, blockpos), level.getBrightness(LightLayer.SKY, blockpos));
   }
}
