package dev.silvergold.simulatedcoasters.client.cart;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.sound.CoasterCartRailSoundCache;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartWorldRenderer {
   private static final double DRAW_RAD_SQ = 9216.0;

   private CoasterCartWorldRenderer() {
   }

   public static void render(Minecraft mc, PoseStack poseStack, BufferSource buffers, Camera camera, DeltaTracker deltaTracker) {
      if (CoasterCartBottomSliceRenderer.hasModel() || CoasterCartWheelAxisRenderer.hasModel()) {
         Level level = mc.level;
         if (level instanceof ClientLevel cl) {
            CoasterPathGraph pathGraph = CoasterPathGraphManagerClient.get(level);
            List openEndRefs = CoasterCartRailSoundCache.openPathEndRefs();
            CoasterCartBlock bearingBlock = (CoasterCartBlock)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
            BlockState tintState = bearingBlock.defaultBlockState();
            int tint = mc.getBlockColors().getColor(tintState, null, null, 0);
            float cr = (float)(tint >> 16 & 0xFF) / 255.0F;
            float cg = (float)(tint >> 8 & 0xFF) / 255.0F;
            float cb = (float)(tint & 0xFF) / 255.0F;
            ModelBlockRenderer modelRenderer = mc.getBlockRenderer().getModelRenderer();
            Vec3 cam = camera.getPosition();
            double camX = cam.x;
            double camY = cam.y;
            double camZ = cam.z;
            float ptF = deltaTracker.getGameTimeDeltaPartialTick(cl.tickRateManager().runsNormally());
            float dtSec = deltaTracker.getRealtimeDeltaTicks() / 20.0F;
            RandomSource bottomRand = RandomSource.create(42L);
            RandomSource wheelRand = RandomSource.create(43L);
            CoasterCartClientMembership.forEachCart(
               cl,
               sub -> {
                  CoasterCartClientFrameCache.RenderFrame frame = CoasterCartClientFrameCache.resolveRender(cl, sub, pathGraph, openEndRefs, ptF);
                  if (frame != null) {
                     if (!(frame.sampleVisual().distanceToSqr(cam) > 9216.0)) {
                        boolean drawGlue = frame.drawStaffGlue();
                        CoasterCartWheelAxisRenderer.SubWheelState wheelState = CoasterCartWheelAxisRenderer.prepareSubWheelState(
                           sub, frame, dtSec, (float)camX, (float)camY, (float)camZ
                        );
                        CoasterCartClientMembership.forEachBearingWorldCell(
                           sub,
                           mut -> {
                              CoasterCartBottomSliceRenderer.renderCell(
                                 poseStack, modelRenderer, buffers, tintState, cr, cg, cb, bottomRand, level, frame, camX, camY, camZ, mut
                              );
                              if (wheelState != null) {
                                 CoasterCartWheelAxisRenderer.renderCell(
                                    poseStack, modelRenderer, buffers, tintState, cr, cg, cb, wheelRand, level, sub, wheelState, mut
                                 );
                              }

                              if (drawGlue) {
                                 CoasterCartGlueRenderer.renderCell(poseStack, buffers, level, frame, camX, camY, camZ, mut);
                              }
                           }
                        );
                     }
                  }
               }
            );
         }
      }
   }
}
