package dev.silvergold.simulatedcoasters.ponder.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorJunctionModel;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackAnchorJunctionPlate;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class ChainLiftExtrasElement extends AnimatedSceneElementBase implements AnimatedSceneElement {
   private static final ResourceLocation CHAIN_TEXTURE = ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "textures/block/chain_lift/chain.png");
   private static final double TRACK_UP_OFFSET_BLOCKS = 0.1875;
   private static final double PONDER_BER_Y_COMPENSATION = 0.1875;
   private static final double RIBBON_HALF_WIDTH_BLOCKS = 0.25;
   private static final int SAMPLES = 24;
   private static final double TANGENT_EPS_T = 0.041666666666666664;
   private final BlockPos startAnchor;
   private final BlockPos endAnchor;

   private ChainLiftExtrasElement(BlockPos startAnchor, BlockPos endAnchor) {
      this.startAnchor = startAnchor.immutable();
      this.endAnchor = endAnchor.immutable();
   }

   public static ChainLiftExtrasElement span(BlockPos startAnchor, BlockPos endAnchor) {
      return new ChainLiftExtrasElement(startAnchor, endAnchor);
   }

   protected void renderLast(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float fade, float pt) {
      if (!(fade < 0.0625F)) {
         if (world.getBlockEntity(this.startAnchor) instanceof CoasterAnchorpointBlockEntity anchor) {
            BezierConnection bc = anchor.getAnchorPeerCurvesView().get(this.endAnchor);
            if (bc != null) {
               BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
               boolean reversed = !((BlockPos)primary.bePositions.getFirst()).equals(this.startAnchor);
               CoasterPathEdge edge = CoasterPathEdge.curve((BlockPos)primary.bePositions.getFirst(), (BlockPos)primary.bePositions.getSecond(), primary);
               ChainLiftExtrasElement.Sample[] samples = new ChainLiftExtrasElement.Sample[25];

               for (int i = 0; i <= 24; i++) {
                  samples[i] = this.sampleAt(world, edge, primary, (double)i / 24.0, reversed);
               }

               PoseStack poseStack = graphics.pose();
               this.renderRibbon(world, buffer, poseStack, samples);
               this.renderEndcap(world, buffer, poseStack, primary, samples[0], reversed, true);
               this.renderEndcap(world, buffer, poseStack, primary, samples[24], reversed, false);
            }
         }
      }
   }

   private ChainLiftExtrasElement.Sample sampleAt(PonderLevel world, CoasterPathEdge edge, BezierConnection bc, double s, boolean reversed) {
      double t = reversed ? 1.0 - s : s;
      float ft = (float)t;
      Vec3 up = CoasterBezierRailFrames.faceUpAt(bc, ft, world);
      if (up.lengthSqr() < 1.0E-12) {
         up = new Vec3(0.0, 1.0, 0.0);
      } else {
         up = up.normalize();
      }

      Vec3 center = CoasterPathGraphManager.sampleEdgePosition(world, edge, t).add(up.scale(0.1875)).add(0.0, 0.1875, 0.0);
      Vec3 tangent = this.curveTangent(world, edge, t, reversed);
      Vec3 lateral = CoasterBezierRailFrames.lateralAt(bc, ft, tangent, world);
      if (lateral.lengthSqr() < 1.0E-12) {
         lateral = up.cross(tangent);
      }

      if (lateral.lengthSqr() < 1.0E-12) {
         lateral = new Vec3(1.0, 0.0, 0.0);
      } else {
         lateral = lateral.normalize();
      }

      return new ChainLiftExtrasElement.Sample(center, up, lateral, tangent);
   }

   private Vec3 curveTangent(PonderLevel world, CoasterPathEdge edge, double t, boolean reversed) {
      double tA = Math.max(0.0, t - 0.041666666666666664);
      double tB = Math.min(1.0, t + 0.041666666666666664);
      Vec3 d = CoasterPathGraphManager.sampleEdgePosition(world, edge, tB).subtract(CoasterPathGraphManager.sampleEdgePosition(world, edge, tA));
      if (reversed) {
         d = d.scale(-1.0);
      }

      return d.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 0.0, 1.0) : d.normalize();
   }

   private void renderRibbon(PonderLevel world, MultiBufferSource buffer, PoseStack poseStack, ChainLiftExtrasElement.Sample[] samples) {
      VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(CHAIN_TEXTURE));
      double arc = 0.0;

      for (int i = 0; i < 24; i++) {
         ChainLiftExtrasElement.Sample a = samples[i];
         ChainLiftExtrasElement.Sample b = samples[i + 1];
         double nextArc = arc + a.center().distanceTo(b.center());
         float vA = (float)(arc / 0.625);
         float vB = (float)(nextArc / 0.625);
         arc = nextArc;
         Vec3 offsetA = a.lateral().scale(0.25);
         Vec3 offsetB = b.lateral().scale(0.25);
         Vec3 leftA = a.center().subtract(offsetA);
         Vec3 rightA = a.center().add(offsetA);
         Vec3 leftB = b.center().subtract(offsetB);
         Vec3 rightB = b.center().add(offsetB);
         int light = LevelRenderer.getLightColor(world, BlockPos.containing(a.center()));
         Vec3 up = a.up();
         Vec3 down = up.scale(-1.0);
         this.vertex(poseStack, consumer, leftA, 0.0F, vA, light, up);
         this.vertex(poseStack, consumer, leftB, 0.0F, vB, light, up);
         this.vertex(poseStack, consumer, rightB, 1.0F, vB, light, up);
         this.vertex(poseStack, consumer, rightA, 1.0F, vA, light, up);
         this.vertex(poseStack, consumer, leftA, 0.0F, vA, light, down);
         this.vertex(poseStack, consumer, rightA, 1.0F, vA, light, down);
         this.vertex(poseStack, consumer, rightB, 1.0F, vB, light, down);
         this.vertex(poseStack, consumer, leftB, 0.0F, vB, light, down);
      }
   }

   private void vertex(PoseStack poseStack, VertexConsumer consumer, Vec3 pos, float u, float v, int light, Vec3 normal) {
      consumer.addVertex(poseStack.last().pose(), (float)pos.x, (float)pos.y, (float)pos.z)
         .setColor(-1)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(poseStack.last(), (float)normal.x, (float)normal.y, (float)normal.z);
   }

   private void renderEndcap(
      PonderLevel world,
      MultiBufferSource buffer,
      PoseStack poseStack,
      BezierConnection bc,
      ChainLiftExtrasElement.Sample sample,
      boolean reversed,
      boolean atStartAnchor
   ) {
      Vec3 inward = atStartAnchor ? sample.tangent() : sample.tangent().scale(-1.0);
      float ft = (float)(reversed == atStartAnchor ? 1.0 : 0.0);
      Vec3 lateral = CoasterBezierRailFrames.lateralAt(bc, ft, inward, world);
      if (lateral.lengthSqr() < 1.0E-12) {
         lateral = sample.lateral();
      } else {
         lateral = lateral.normalize();
      }

      PoseStack local = CoasterTrackAnchorJunctionPlate.connectionPoseBelowOrigin(sample.center(), lateral, inward);
      int light = LevelRenderer.getLightColor(world, BlockPos.containing(sample.center()));
      SuperByteBuffer model = CachedBuffers.partial(
         atStartAnchor
            ? CoasterTrackAnchorJunctionModel.TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT
            : CoasterTrackAnchorJunctionModel.TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT_END,
         Blocks.AIR.defaultBlockState()
      );
      ((SuperByteBuffer)model.transform(local.last())).light(light).renderInto(poseStack, buffer.getBuffer(RenderType.cutout()));
   }

   private static record Sample(Vec3 center, Vec3 up, Vec3 lateral, Vec3 tangent) {
   }
}
