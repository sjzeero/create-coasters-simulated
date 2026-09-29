package dev.silvergold.simulatedcoasters.client.cart;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartGlueRenderer {
   private static final ResourceLocation STRAND_TEXTURE = ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "textures/block/merging_glue/strand.png");
   private static final double SEAM_Y = 0.75;

   private CoasterCartGlueRenderer() {
   }

   static VertexConsumer acquireStrandBuffer(BufferSource buffers) {
      return buffers.getBuffer(RenderType.entityCutoutNoCull(STRAND_TEXTURE));
   }

   static void renderCell(
      PoseStack poseStack,
      BufferSource buffers,
      Level level,
      CoasterCartClientFrameCache.RenderFrame frame,
      double camX,
      double camY,
      double camZ,
      MutableBlockPos mut
   ) {
      VertexConsumer buffer = acquireStrandBuffer(buffers);
      Pose3d visualPose = frame.visualPose();
      Quaternionf qBody = frame.qBody();
      Quaternionf qPath = frame.qPath();
      Vec3 pathAnchorBottom = frame.pathAnchorBottom();
      CoasterPathTrackFrame.GraphHit hit = frame.trackHit();
      Vec3 seamTopWorld = visualPose.transformPosition(new Vec3((double)mut.getX() + 0.5, (double)mut.getY() + 0.75, (double)mut.getZ() + 0.5));
      Vec3 seamBottomWorld;
      if (hit != null && qPath != null && pathAnchorBottom != null) {
         Vector3f off = new Vector3f(0.0F, 0.25F, 0.0F);
         qPath.transform(off);
         seamBottomWorld = pathAnchorBottom.add((double)off.x, (double)off.y, (double)off.z);
      } else {
         Vec3 cornerPlot = Vec3.atLowerCornerOf(mut);
         Vec3 cornerWorld = visualPose.transformPosition(cornerPlot);
         Vector3f local = new Vector3f(0.5F, 0.75F, 0.5F);
         qBody.transform(local);
         seamBottomWorld = cornerWorld.add((double)local.x, (double)local.y, (double)local.z);
      }

      Vec3 stretch = seamTopWorld.subtract(seamBottomWorld);
      double lenSq = stretch.lengthSqr();
      if (!(lenSq < 1.0E-8)) {
         Vec3 stretchDir = stretch.scale(1.0 / Math.sqrt(lenSq));
         CoasterPathTrackFrame.TrackBasis basis = frame.trackBasis();
         Vec3 upW;
         Vec3 rightW;
         if (basis != null) {
            Vec3 r = basis.right();
            rightW = r.subtract(stretchDir.scale(r.dot(stretchDir)));
            if (rightW.lengthSqr() < 1.0E-8) {
               Vec3 f = basis.forward();
               rightW = f.cross(stretchDir);
            }

            rightW = rightW.normalize();
            upW = stretchDir.cross(rightW).normalize();
         } else {
            Vector3f rx = new Vector3f(1.0F, 0.0F, 0.0F);
            qBody.transform(rx);
            rightW = new Vec3((double)rx.x, (double)rx.y, (double)rx.z);
            rightW = rightW.subtract(stretchDir.scale(rightW.dot(stretchDir)));
            if (rightW.lengthSqr() < 1.0E-8) {
               Vector3f uz = new Vector3f(0.0F, 0.0F, 1.0F);
               qBody.transform(uz);
               rightW = new Vec3((double)uz.x, (double)uz.y, (double)uz.z);
               rightW = rightW.subtract(stretchDir.scale(rightW.dot(stretchDir)));
            }

            rightW = rightW.normalize();
            upW = stretchDir.cross(rightW).normalize();
         }

         Vec3 seamMid = seamBottomWorld.lerp(seamTopWorld, 0.5);
         int light = LevelRenderer.getLightColor(level, BlockPos.containing(seamMid));
         poseStack.pushPose();
         poseStack.translate(-camX, -camY, -camZ);
         Pose poseLast = poseStack.last();
         Matrix4f mat = poseLast.pose();
         renderGlueStrands(mat, poseLast, buffer, seamBottomWorld, rightW, upW, seamTopWorld, rightW, upW, light);
         poseStack.popPose();
      }
   }

   @Deprecated
   public static void render(Minecraft mc, PoseStack poseStack, BufferSource buffers, Camera camera, DeltaTracker deltaTracker) {
      CoasterCartWorldRenderer.render(mc, poseStack, buffers, camera, deltaTracker);
   }

   private static void renderGlueStrands(
      Matrix4f pose, Pose poseEntry, VertexConsumer buffer, Vec3 posA, Vec3 rightA, Vec3 upA, Vec3 posB, Vec3 rightB, Vec3 upB, int light
   ) {
      double[][] strandPositions = new double[][]{{0.25, 0.25, 0.45, 0.3}, {0.6, 0.6, 0.65, 0.7}};

      for (int i = 0; i < 2; i++) {
         double ax = (strandPositions[i][0] - 0.5) * 0.75;
         double ay = (strandPositions[i][1] - 0.5) * 0.75;
         double bx = (strandPositions[i][2] - 0.5) * 0.75;
         double by = (strandPositions[i][3] - 0.5) * 0.75;
         Vec3 strandA = posA.add(rightA.scale(ax)).add(upA.scale(ay));
         Vec3 strandB = posB.add(rightB.scale(bx)).add(upB.scale(by));
         renderGlueCross(pose, poseEntry, buffer, strandA, upA, rightA, strandB, upB, rightB, light);
      }
   }

   private static void renderGlueCross(
      Matrix4f mat, Pose poseEntry, VertexConsumer buffer, Vec3 posA, Vec3 upA, Vec3 rightA, Vec3 posB, Vec3 upB, Vec3 rightB, int light
   ) {
      Vec3 ua = scaled(upA, 0.5);
      Vec3 ub = scaled(upB, 0.5);
      Vec3 ra = scaled(rightA, 0.5);
      Vec3 rb = scaled(rightB, 0.5);
      emitQuad(mat, poseEntry, buffer, posA.subtract(ua), posA.add(ua), posB.add(ub), posB.subtract(ub), 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, light);
      emitQuad(mat, poseEntry, buffer, posA.subtract(ra), posA.add(ra), posB.add(rb), posB.subtract(rb), 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, light);
   }

   private static Vec3 scaled(Vec3 v, double s) {
      return new Vec3(v.x * s, v.y * s, v.z * s);
   }

   private static void emitQuad(
      Matrix4f mat,
      Pose poseEntry,
      VertexConsumer buffer,
      Vec3 v0,
      Vec3 v1,
      Vec3 v2,
      Vec3 v3,
      float u0,
      float tv0,
      float u1,
      float tv1,
      float u2,
      float tv2,
      float u3,
      float tv3,
      int light
   ) {
      Vec3 e1 = v1.subtract(v0);
      Vec3 e2 = v3.subtract(v0);
      Vec3 n = e1.cross(e2);
      double nl = n.length();
      float nx = nl > 1.0E-8 ? (float)(n.x / nl) : 0.0F;
      float ny = nl > 1.0E-8 ? (float)(n.y / nl) : 1.0F;
      float nz = nl > 1.0E-8 ? (float)(n.z / nl) : 0.0F;
      vert(mat, poseEntry, buffer, v0, u0, tv0, light, nx, ny, nz);
      vert(mat, poseEntry, buffer, v1, u1, tv1, light, nx, ny, nz);
      vert(mat, poseEntry, buffer, v2, u2, tv2, light, nx, ny, nz);
      vert(mat, poseEntry, buffer, v3, u3, tv3, light, nx, ny, nz);
   }

   private static void vert(Matrix4f mat, Pose poseEntry, VertexConsumer buffer, Vec3 p, float u, float v, int light, float nx, float ny, float nz) {
      buffer.addVertex(mat, (float)p.x, (float)p.y, (float)p.z)
         .setColor(-1)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(poseEntry, nx, ny, nz);
   }
}
