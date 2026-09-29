package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class ChainLiftRibbonMeshCache {
   private static final double RIBBON_HALF_WIDTH_BLOCKS = 0.25;
   private static final double TRACK_UP_OFFSET_BLOCKS = 0.1875;
   private static final Map<CoasterAnchorpointBlockEntity, ChainLiftRibbonMeshCache.CachedMesh> CACHE = new IdentityHashMap<>();

   private ChainLiftRibbonMeshCache() {
   }

   public static void clearAll() {
      CACHE.clear();
   }

   public static void invalidate(CoasterAnchorpointBlockEntity anchor) {
      CACHE.remove(anchor);
   }

   public static boolean pathUsesSubLevelTrack(Level level, CoasterPathGraphSpans.SpanPath path) {
      for (CoasterPathGraphSpans.PathSegment segment : path.segments()) {
         if (CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, segment.edge().from())
            || CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, segment.edge().to())) {
            return true;
         }
      }

      return false;
   }

   @Nullable
   public static ChainLiftRibbonMeshCache.CachedMesh getOrBuild(
      CoasterAnchorpointBlockEntity anchor, Level level, CoasterPathGraphSpans.SpanPath path, double sampleSpacing, int lodTier
   ) {
      if (pathUsesSubLevelTrack(level, path)) {
         return null;
      } else if (anchor.isRemoved()) {
         CACHE.remove(anchor);
         return null;
      } else {
         int rebuildKey = ChainLiftSpanPathCache.ribbonMeshRebuildKey(anchor, path, lodTier);
         ChainLiftRibbonMeshCache.CachedMesh cached = CACHE.get(anchor);
         if (cached != null && cached.rebuildKey == rebuildKey) {
            return cached;
         } else {
            ChainLiftRibbonMeshCache.CachedMesh built = buildStaticMesh(level, path, sampleSpacing, rebuildKey);
            if (built == null) {
               CACHE.remove(anchor);
               return null;
            } else {
               CACHE.put(anchor, built);
               return built;
            }
         }
      }
   }

   public static void draw(
      ChainLiftRibbonMeshCache.CachedMesh mesh,
      Level level,
      Matrix4f mat,
      Pose poseEntry,
      VertexConsumer consumer,
      double partialTickForRender,
      float scrollPhase
   ) {
      for (int i = 0; i < mesh.vertices.length; i += 4) {
         emitQuad(mesh.vertices, i, level, mat, poseEntry, consumer, partialTickForRender, scrollPhase, false);
         emitQuad(mesh.vertices, i, level, mat, poseEntry, consumer, partialTickForRender, scrollPhase, true);
      }
   }

   public static void drawLiveSpan(
      Level level,
      CoasterPathGraphSpans.SpanPath path,
      double sampleSpacing,
      Matrix4f mat,
      Pose poseEntry,
      VertexConsumer consumer,
      double partialTickForRender,
      float scrollPhase
   ) {
      CoasterPathGraphSpans.SpanArcSampler sampler = CoasterPathGraphSpans.SpanArcSampler.of(level, path, partialTickForRender);
      double total = sampler.totalLength();
      if (!(total <= 1.0E-6)) {
         int steps = Math.max(2, (int)Math.ceil(total / sampleSpacing));
         ChainLiftRibbonMeshCache.RibbonSample prev = null;
         double texArc = 0.0;

         for (int i = 0; i <= steps; i++) {
            double arc = total * (double)i / (double)steps;
            CoasterPathTrackFrame.GraphHit hit = sampler.sampleGraphHit(level, arc, partialTickForRender);
            if (hit != null) {
               ChainLiftRibbonMeshCache.RibbonSample sample = sampleAtWorld(level, hit, partialTickForRender);
               if (prev != null) {
                  texArc += spineDistance(prev, sample);
               }

               sample = sample.withTexArc(texArc);
               if (prev != null) {
                  emitLiveSegment(prev, sample, level, mat, poseEntry, consumer, scrollPhase);
               }

               prev = sample;
            }
         }
      }
   }

   public static boolean spanNearCamera(Level level, Vec3 camera, ChainLiftRibbonMeshCache.CachedMesh mesh, double partialTickForRender, double maxRenderDistSq) {
      if (mesh.vertices.length == 0) {
         return true;
      } else {
         Vec3 start = vertexWorldPosition(mesh.vertices[0], level, partialTickForRender);
         Vec3 end = vertexWorldPosition(mesh.vertices[mesh.vertices.length - 1], level, partialTickForRender);
         return nearCamera(camera, start, end, maxRenderDistSq);
      }
   }

   public static boolean spanNearCameraLive(Level level, CoasterPathGraphSpans.SpanPath path, Vec3 camera, double partialTickForRender, double maxRenderDistSq) {
      CoasterPathGraphSpans.SpanArcSampler sampler = CoasterPathGraphSpans.SpanArcSampler.of(level, path, partialTickForRender);
      double total = sampler.totalLength();
      if (total <= 1.0E-6) {
         return false;
      } else {
         CoasterPathTrackFrame.GraphHit startHit = sampler.sampleGraphHit(level, 0.0, partialTickForRender);
         CoasterPathTrackFrame.GraphHit endHit = sampler.sampleGraphHit(level, total, partialTickForRender);
         if (startHit != null && endHit != null) {
            Vec3 start = ribbonCenterWorld(level, startHit, partialTickForRender);
            Vec3 end = ribbonCenterWorld(level, endHit, partialTickForRender);
            return nearCamera(camera, start, end, maxRenderDistSq);
         } else {
            return true;
         }
      }
   }

   private static boolean nearCamera(Vec3 camera, Vec3 start, Vec3 end, double maxRenderDistSq) {
      return camera.distanceToSqr(start) <= maxRenderDistSq
         || camera.distanceToSqr(end) <= maxRenderDistSq
         || camera.distanceToSqr(start.lerp(end, 0.5)) <= maxRenderDistSq;
   }

   @Nullable
   private static ChainLiftRibbonMeshCache.CachedMesh buildStaticMesh(Level level, CoasterPathGraphSpans.SpanPath path, double sampleSpacing, int rebuildKey) {
      CoasterPathGraphSpans.SpanArcSampler sampler = CoasterPathGraphSpans.SpanArcSampler.of(level, path, (Double)null);
      double total = sampler.totalLength();
      if (total <= 1.0E-6) {
         return null;
      } else {
         int steps = Math.max(2, (int)Math.ceil(total / sampleSpacing));
         List<ChainLiftRibbonMeshCache.RibbonVertex> vertices = new ArrayList<>(steps * 4);
         ChainLiftRibbonMeshCache.RibbonSample prev = null;
         double texArc = 0.0;

         for (int i = 0; i <= steps; i++) {
            double arc = total * (double)i / (double)steps;
            CoasterPathTrackFrame.GraphHit hit = sampler.sampleGraphHit(level, arc, null);
            if (hit != null) {
               ChainLiftRibbonMeshCache.RibbonSample sample = sampleAtWorld(level, hit, null);
               if (prev != null) {
                  texArc += spineDistance(prev, sample);
               }

               sample = sample.withTexArc(texArc);
               if (prev != null) {
                  appendSegment(vertices, prev, sample);
               }

               prev = sample;
            }
         }

         if (vertices.isEmpty()) {
            return null;
         } else {
            ChainLiftRibbonMeshCache.RibbonVertex[] array = vertices.toArray(ChainLiftRibbonMeshCache.RibbonVertex[]::new);
            ChainLiftRibbonMeshCache.RibbonEndcapFrame start = ChainLiftSpanEndcapRenderer.endcapFrameAt(level, sampler, 0.0, true, null);
            ChainLiftRibbonMeshCache.RibbonEndcapFrame end = ChainLiftSpanEndcapRenderer.endcapFrameAt(level, sampler, total, false, null);
            return new ChainLiftRibbonMeshCache.CachedMesh(rebuildKey, array, start, end);
         }
      }
   }

   private static void appendSegment(
      List<ChainLiftRibbonMeshCache.RibbonVertex> out, ChainLiftRibbonMeshCache.RibbonSample a, ChainLiftRibbonMeshCache.RibbonSample b
   ) {
      float v0 = baseV(a.texArc());
      float v1 = baseV(b.texArc());
      out.add(vertex(a.left(), a.normal(), 0.0F, v0));
      out.add(vertex(b.left(), b.normal(), 0.0F, v1));
      out.add(vertex(b.right(), b.normal(), 1.0F, v1));
      out.add(vertex(a.right(), a.normal(), 1.0F, v0));
   }

   private static void emitLiveSegment(
      ChainLiftRibbonMeshCache.RibbonSample a,
      ChainLiftRibbonMeshCache.RibbonSample b,
      Level level,
      Matrix4f mat,
      Pose poseEntry,
      VertexConsumer consumer,
      float scrollPhase
   ) {
      float v0 = baseV(a.texArc());
      float v1 = baseV(b.texArc());
      ChainLiftRibbonMeshCache.RibbonVertex[] quad = new ChainLiftRibbonMeshCache.RibbonVertex[]{
         vertex(a.left(), a.normal(), 0.0F, v0),
         vertex(b.left(), b.normal(), 0.0F, v1),
         vertex(b.right(), b.normal(), 1.0F, v1),
         vertex(a.right(), a.normal(), 1.0F, v0)
      };
      emitQuad(quad, 0, level, mat, poseEntry, consumer, 0.0, scrollPhase, false);
      emitQuad(quad, 0, level, mat, poseEntry, consumer, 0.0, scrollPhase, true);
   }

   private static ChainLiftRibbonMeshCache.RibbonSample sampleAtWorld(Level level, CoasterPathTrackFrame.GraphHit hit, @Nullable Double partialTickForRender) {
      float partialTick = partialTickForRender == null ? 0.0F : partialTickForRender.floatValue();
      boolean clientRender = partialTickForRender != null && level.isClientSide() && CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge());
      CoasterPathTrackFrame.TrackBasis basis = clientRender
         ? CoasterPathClientRenderSpace.trackBasisAtHit(level, hit, partialTick)
         : CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender);
      Vec3 up = basis.up();
      if (up.lengthSqr() < 1.0E-12) {
         up = new Vec3(0.0, 1.0, 0.0);
      } else {
         up = up.normalize();
      }

      Vec3 right = basis.right();
      if (right.lengthSqr() < 1.0E-12) {
         right = up.cross(basis.forward());
      }

      if (right.lengthSqr() < 1.0E-12) {
         right = new Vec3(1.0, 0.0, 0.0);
      } else {
         right = right.normalize();
      }

      Vec3 center = ribbonCenterWorld(level, hit, partialTickForRender);
      Vec3 left = center.subtract(right.scale(0.25));
      Vec3 rightPt = center.add(right.scale(0.25));
      return new ChainLiftRibbonMeshCache.RibbonSample(left, rightPt, up, 0.0);
   }

   private static Vec3 spineCenter(ChainLiftRibbonMeshCache.RibbonSample sample) {
      return sample.left().add(sample.right()).scale(0.5);
   }

   private static double spineDistance(ChainLiftRibbonMeshCache.RibbonSample a, ChainLiftRibbonMeshCache.RibbonSample b) {
      return spineCenter(a).distanceTo(spineCenter(b));
   }

   public static Vec3 ribbonCenterAt(Level level, CoasterPathTrackFrame.GraphHit hit, @Nullable Double partialTickForRender) {
      return ribbonCenterWorld(level, hit, partialTickForRender);
   }

   public static Vec3 ribbonCenterPlotLocal(Level level, CoasterPathTrackFrame.GraphHit hit) {
      CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit);
      Vec3 up = basis.up();
      if (up.lengthSqr() < 1.0E-12) {
         up = new Vec3(0.0, 1.0, 0.0);
      } else {
         up = up.normalize();
      }

      Vec3 center = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, hit.edge(), hit.t());
      return center.add(up.scale(0.1875));
   }

   private static Vec3 ribbonCenterWorld(Level level, CoasterPathTrackFrame.GraphHit hit, @Nullable Double partialTickForRender) {
      float partialTick = partialTickForRender == null ? 0.0F : partialTickForRender.floatValue();
      boolean clientRender = partialTickForRender != null && level.isClientSide() && CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge());
      CoasterPathTrackFrame.TrackBasis basis = clientRender
         ? CoasterPathClientRenderSpace.trackBasisAtHit(level, hit, partialTick)
         : CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender);
      Vec3 up = basis.up();
      if (up.lengthSqr() < 1.0E-12) {
         up = new Vec3(0.0, 1.0, 0.0);
      } else {
         up = up.normalize();
      }

      Vec3 center = clientRender
         ? CoasterPathClientRenderSpace.sampleEdgePosition(level, hit.edge(), hit.t(), partialTick)
         : (
            partialTickForRender == null
               ? CoasterPathGraphManager.sampleEdgePosition(level, hit.edge(), hit.t(), null)
               : CoasterPathGraphManager.sampleEdgePosition(level, hit.edge(), hit.t(), partialTickForRender)
         );
      return center.add(up.scale(0.1875));
   }

   private static ChainLiftRibbonMeshCache.RibbonVertex vertex(Vec3 pos, Vec3 normal, float u, float vBase) {
      return new ChainLiftRibbonMeshCache.RibbonVertex((float)pos.x, (float)pos.y, (float)pos.z, (float)normal.x, (float)normal.y, (float)normal.z, u, vBase);
   }

   private static float baseV(double spineArcBlocks) {
      return (float)(spineArcBlocks / 0.625);
   }

   private static float ribbonTextureV(float vBase, float scrollPhase) {
      return (float)((double)vBase + (double)scrollPhase);
   }

   private static void emitQuad(
      ChainLiftRibbonMeshCache.RibbonVertex[] vertices,
      int offset,
      Level level,
      Matrix4f mat,
      Pose poseEntry,
      VertexConsumer consumer,
      double partialTickForRender,
      float scrollPhase,
      boolean backFace
   ) {
      ChainLiftRibbonMeshCache.RibbonVertex v0 = vertices[offset];
      ChainLiftRibbonMeshCache.RibbonVertex v1 = vertices[offset + 1];
      ChainLiftRibbonMeshCache.RibbonVertex v2 = vertices[offset + 2];
      ChainLiftRibbonMeshCache.RibbonVertex v3 = vertices[offset + 3];
      Vec3 p0 = vertexWorldPosition(v0, level, partialTickForRender);
      Vec3 p1 = vertexWorldPosition(v1, level, partialTickForRender);
      Vec3 p2 = vertexWorldPosition(v2, level, partialTickForRender);
      Vec3 p3 = vertexWorldPosition(v3, level, partialTickForRender);
      int light = ribbonQuadLight(level, p0, p1, p2, p3);
      if (backFace) {
         Vec3 n = quadFaceNormal(p0, p3, p2);
         float nx = (float)n.x;
         float ny = (float)n.y;
         float nz = (float)n.z;
         vert(mat, poseEntry, consumer, p0, v0.u, ribbonTextureV(v0.vBase, scrollPhase), light, nx, ny, nz);
         vert(mat, poseEntry, consumer, p3, v3.u, ribbonTextureV(v3.vBase, scrollPhase), light, nx, ny, nz);
         vert(mat, poseEntry, consumer, p2, v2.u, ribbonTextureV(v2.vBase, scrollPhase), light, nx, ny, nz);
         vert(mat, poseEntry, consumer, p1, v1.u, ribbonTextureV(v1.vBase, scrollPhase), light, nx, ny, nz);
      } else {
         Vec3 n = quadFaceNormal(p0, p1, p2);
         float nx = (float)n.x;
         float ny = (float)n.y;
         float nz = (float)n.z;
         vert(mat, poseEntry, consumer, p0, v0.u, ribbonTextureV(v0.vBase, scrollPhase), light, nx, ny, nz);
         vert(mat, poseEntry, consumer, p1, v1.u, ribbonTextureV(v1.vBase, scrollPhase), light, nx, ny, nz);
         vert(mat, poseEntry, consumer, p2, v2.u, ribbonTextureV(v2.vBase, scrollPhase), light, nx, ny, nz);
         vert(mat, poseEntry, consumer, p3, v3.u, ribbonTextureV(v3.vBase, scrollPhase), light, nx, ny, nz);
      }
   }

   private static Vec3 vertexWorldPosition(ChainLiftRibbonMeshCache.RibbonVertex vertex, Level level, double partialTickForRender) {
      return new Vec3((double)vertex.x, (double)vertex.y, (double)vertex.z);
   }

   private static Vec3 quadFaceNormal(Vec3 a, Vec3 b, Vec3 c) {
      Vec3 n = b.subtract(a).cross(c.subtract(a));
      return n.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : n.normalize();
   }

   private static int ribbonQuadLight(Level level, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
      Vec3 center = p0.add(p1).add(p2).add(p3).scale(0.25);
      Vec3 outward = quadFaceNormal(p0, p1, p2);
      int front = LevelRenderer.getLightColor(level, BlockPos.containing(center));
      int back = LevelRenderer.getLightColor(level, BlockPos.containing(center.subtract(outward.scale(0.25))));
      return brighterLight(front, back);
   }

   private static int brighterLight(int a, int b) {
      int blockA = a >> 4 & 15;
      int skyA = a >> 20 & 15;
      int blockB = b >> 4 & 15;
      int skyB = b >> 20 & 15;
      return Math.max(blockA, blockB) << 4 | Math.max(skyA, skyB) << 20;
   }

   private static void vert(Matrix4f mat, Pose poseEntry, VertexConsumer consumer, Vec3 p, float u, float v, int light, float nx, float ny, float nz) {
      consumer.addVertex(mat, (float)p.x, (float)p.y, (float)p.z)
         .setColor(-1)
         .setUv(u, v)
         .setOverlay(OverlayTexture.NO_OVERLAY)
         .setLight(light)
         .setNormal(poseEntry, nx, ny, nz);
   }

   public static record CachedMesh(
      int rebuildKey,
      ChainLiftRibbonMeshCache.RibbonVertex[] vertices,
      @Nullable ChainLiftRibbonMeshCache.RibbonEndcapFrame start,
      @Nullable ChainLiftRibbonMeshCache.RibbonEndcapFrame end
   ) {
   }

   public static record RibbonEndcapFrame(Vec3 spine, Vec3 lateral, Vec3 chordIntoTrack) {
   }

   private static record RibbonSample(Vec3 left, Vec3 right, Vec3 normal, double texArc) {
      ChainLiftRibbonMeshCache.RibbonSample withTexArc(double texArc) {
         return new ChainLiftRibbonMeshCache.RibbonSample(this.left, this.right, this.normal, texArc);
      }
   }

   public static final class RibbonVertex {
      final float x;
      final float y;
      final float z;
      final float nx;
      final float ny;
      final float nz;
      final float u;
      final float vBase;

      RibbonVertex(float x, float y, float z, float nx, float ny, float nz, float u, float vBase) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.nx = nx;
         this.ny = ny;
         this.nz = nz;
         this.u = u;
         this.vBase = vBase;
      }
   }
}
