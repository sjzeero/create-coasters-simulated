/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  net.minecraft.client.renderer.LevelRenderer
 *  net.minecraft.client.renderer.texture.OverlayTexture
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix4f
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.silvergold.simulatedcoasters.client.track.ChainLiftSpanEndcapRenderer;
import dev.silvergold.simulatedcoasters.client.track.CoasterPathClientRenderSpace;
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
import net.minecraft.core.Position;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

@OnlyIn(value=Dist.CLIENT)
public final class ChainLiftRibbonMeshCache {
    private static final double RIBBON_HALF_WIDTH_BLOCKS = 0.25;
    private static final double TRACK_UP_OFFSET_BLOCKS = 0.1875;
    private static final Map<CoasterAnchorpointBlockEntity, CachedMesh> CACHE = new IdentityHashMap<CoasterAnchorpointBlockEntity, CachedMesh>();

    private ChainLiftRibbonMeshCache() {
    }

    public static void clearAll() {
        CACHE.clear();
    }

    public static void invalidate(CoasterAnchorpointBlockEntity anchor) {
        CACHE.remove((Object)anchor);
    }

    public static boolean pathUsesSubLevelTrack(Level level, CoasterPathGraphSpans.SpanPath path) {
        for (CoasterPathGraphSpans.PathSegment segment : path.segments()) {
            if (!CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, segment.edge().from()) && !CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, segment.edge().to())) continue;
            return true;
        }
        return false;
    }

    @Nullable
    public static CachedMesh getOrBuild(CoasterAnchorpointBlockEntity anchor, Level level, CoasterPathGraphSpans.SpanPath path, double sampleSpacing, int lodTier) {
        if (ChainLiftRibbonMeshCache.pathUsesSubLevelTrack(level, path)) {
            return null;
        }
        if (anchor.isRemoved()) {
            CACHE.remove((Object)anchor);
            return null;
        }
        int rebuildKey = ChainLiftSpanPathCache.ribbonMeshRebuildKey(anchor, path, lodTier);
        CachedMesh cached = CACHE.get((Object)anchor);
        if (cached != null && cached.rebuildKey == rebuildKey) {
            return cached;
        }
        CachedMesh built = ChainLiftRibbonMeshCache.buildStaticMesh(level, path, sampleSpacing, rebuildKey);
        if (built == null) {
            CACHE.remove((Object)anchor);
            return null;
        }
        CACHE.put(anchor, built);
        return built;
    }

    public static void draw(CachedMesh mesh, Level level, Matrix4f mat, PoseStack.Pose poseEntry, VertexConsumer consumer, double partialTickForRender, float scrollPhase) {
        for (int i = 0; i < mesh.vertices.length; i += 4) {
            ChainLiftRibbonMeshCache.emitQuad(mesh.vertices, i, level, mat, poseEntry, consumer, partialTickForRender, scrollPhase, false);
            ChainLiftRibbonMeshCache.emitQuad(mesh.vertices, i, level, mat, poseEntry, consumer, partialTickForRender, scrollPhase, true);
        }
    }

    public static void drawLiveSpan(Level level, CoasterPathGraphSpans.SpanPath path, double sampleSpacing, Matrix4f mat, PoseStack.Pose poseEntry, VertexConsumer consumer, double partialTickForRender, float scrollPhase) {
        CoasterPathGraphSpans.SpanArcSampler sampler = CoasterPathGraphSpans.SpanArcSampler.of(level, path, partialTickForRender);
        double total = sampler.totalLength();
        if (total <= 1.0E-6) {
            return;
        }
        int steps = Math.max(2, (int)Math.ceil(total / sampleSpacing));
        RibbonSample prev = null;
        double texArc = 0.0;
        for (int i = 0; i <= steps; ++i) {
            double arc = total * (double)i / (double)steps;
            @Nullable CoasterPathTrackFrame.GraphHit hit = sampler.sampleGraphHit(level, arc, partialTickForRender);
            if (hit == null) continue;
            RibbonSample sample = ChainLiftRibbonMeshCache.sampleAtWorld(level, hit, partialTickForRender);
            if (prev != null) {
                texArc += ChainLiftRibbonMeshCache.spineDistance(prev, sample);
            }
            sample = sample.withTexArc(texArc);
            if (prev != null) {
                ChainLiftRibbonMeshCache.emitLiveSegment(prev, sample, level, mat, poseEntry, consumer, scrollPhase);
            }
            prev = sample;
        }
    }

    public static boolean spanNearCamera(Level level, Vec3 camera, CachedMesh mesh, double partialTickForRender, double maxRenderDistSq) {
        if (mesh.vertices.length == 0) {
            return true;
        }
        Vec3 start = ChainLiftRibbonMeshCache.vertexWorldPosition(mesh.vertices[0], level, partialTickForRender);
        Vec3 end = ChainLiftRibbonMeshCache.vertexWorldPosition(mesh.vertices[mesh.vertices.length - 1], level, partialTickForRender);
        return ChainLiftRibbonMeshCache.nearCamera(camera, start, end, maxRenderDistSq);
    }

    public static boolean spanNearCameraLive(Level level, CoasterPathGraphSpans.SpanPath path, Vec3 camera, double partialTickForRender, double maxRenderDistSq) {
        CoasterPathGraphSpans.SpanArcSampler sampler = CoasterPathGraphSpans.SpanArcSampler.of(level, path, partialTickForRender);
        double total = sampler.totalLength();
        if (total <= 1.0E-6) {
            return false;
        }
        @Nullable CoasterPathTrackFrame.GraphHit startHit = sampler.sampleGraphHit(level, 0.0, partialTickForRender);
        @Nullable CoasterPathTrackFrame.GraphHit endHit = sampler.sampleGraphHit(level, total, partialTickForRender);
        if (startHit == null || endHit == null) {
            return true;
        }
        Vec3 start = ChainLiftRibbonMeshCache.ribbonCenterWorld(level, startHit, partialTickForRender);
        Vec3 end = ChainLiftRibbonMeshCache.ribbonCenterWorld(level, endHit, partialTickForRender);
        return ChainLiftRibbonMeshCache.nearCamera(camera, start, end, maxRenderDistSq);
    }

    private static boolean nearCamera(Vec3 camera, Vec3 start, Vec3 end, double maxRenderDistSq) {
        return camera.distanceToSqr(start) <= maxRenderDistSq || camera.distanceToSqr(end) <= maxRenderDistSq || camera.distanceToSqr(start.lerp(end, 0.5)) <= maxRenderDistSq;
    }

    @Nullable
    private static CachedMesh buildStaticMesh(Level level, CoasterPathGraphSpans.SpanPath path, double sampleSpacing, int rebuildKey) {
        CoasterPathGraphSpans.SpanArcSampler sampler = CoasterPathGraphSpans.SpanArcSampler.of(level, path, null);
        double total = sampler.totalLength();
        if (total <= 1.0E-6) {
            return null;
        }
        int steps = Math.max(2, (int)Math.ceil(total / sampleSpacing));
        ArrayList<RibbonVertex> vertices = new ArrayList<RibbonVertex>(steps * 4);
        RibbonSample prev = null;
        double texArc = 0.0;
        for (int i = 0; i <= steps; ++i) {
            double arc = total * (double)i / (double)steps;
            @Nullable CoasterPathTrackFrame.GraphHit hit = sampler.sampleGraphHit(level, arc, null);
            if (hit == null) continue;
            RibbonSample sample = ChainLiftRibbonMeshCache.sampleAtWorld(level, hit, null);
            if (prev != null) {
                texArc += ChainLiftRibbonMeshCache.spineDistance(prev, sample);
            }
            sample = sample.withTexArc(texArc);
            if (prev != null) {
                ChainLiftRibbonMeshCache.appendSegment(vertices, prev, sample);
            }
            prev = sample;
        }
        if (vertices.isEmpty()) {
            return null;
        }
        RibbonVertex[] array = (RibbonVertex[])vertices.toArray(RibbonVertex[]::new);
        @Nullable RibbonEndcapFrame start = ChainLiftSpanEndcapRenderer.endcapFrameAt(level, sampler, 0.0, true, null);
        @Nullable RibbonEndcapFrame end = ChainLiftSpanEndcapRenderer.endcapFrameAt(level, sampler, total, false, null);
        return new CachedMesh(rebuildKey, array, start, end);
    }

    private static void appendSegment(List<RibbonVertex> out, RibbonSample a, RibbonSample b) {
        float v0 = ChainLiftRibbonMeshCache.baseV(a.texArc());
        float v1 = ChainLiftRibbonMeshCache.baseV(b.texArc());
        out.add(ChainLiftRibbonMeshCache.vertex(a.left(), a.normal(), 0.0f, v0));
        out.add(ChainLiftRibbonMeshCache.vertex(b.left(), b.normal(), 0.0f, v1));
        out.add(ChainLiftRibbonMeshCache.vertex(b.right(), b.normal(), 1.0f, v1));
        out.add(ChainLiftRibbonMeshCache.vertex(a.right(), a.normal(), 1.0f, v0));
    }

    private static void emitLiveSegment(RibbonSample a, RibbonSample b, Level level, Matrix4f mat, PoseStack.Pose poseEntry, VertexConsumer consumer, float scrollPhase) {
        float v0 = ChainLiftRibbonMeshCache.baseV(a.texArc());
        float v1 = ChainLiftRibbonMeshCache.baseV(b.texArc());
        RibbonVertex[] quad = new RibbonVertex[]{ChainLiftRibbonMeshCache.vertex(a.left(), a.normal(), 0.0f, v0), ChainLiftRibbonMeshCache.vertex(b.left(), b.normal(), 0.0f, v1), ChainLiftRibbonMeshCache.vertex(b.right(), b.normal(), 1.0f, v1), ChainLiftRibbonMeshCache.vertex(a.right(), a.normal(), 1.0f, v0)};
        ChainLiftRibbonMeshCache.emitQuad(quad, 0, level, mat, poseEntry, consumer, 0.0, scrollPhase, false);
        ChainLiftRibbonMeshCache.emitQuad(quad, 0, level, mat, poseEntry, consumer, 0.0, scrollPhase, true);
    }

    private static RibbonSample sampleAtWorld(Level level, CoasterPathTrackFrame.GraphHit hit, @Nullable Double partialTickForRender) {
        float partialTick = partialTickForRender == null ? 0.0f : partialTickForRender.floatValue();
        boolean clientRender = partialTickForRender != null && level.isClientSide() && CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge());
        CoasterPathTrackFrame.TrackBasis basis = clientRender ? CoasterPathClientRenderSpace.trackBasisAtHit(level, hit, partialTick) : CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender);
        Vec3 up = basis.up();
        up = up.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : up.normalize();
        Vec3 right = basis.right();
        if (right.lengthSqr() < 1.0E-12) {
            right = up.cross(basis.forward());
        }
        right = right.lengthSqr() < 1.0E-12 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        Vec3 center = ChainLiftRibbonMeshCache.ribbonCenterWorld(level, hit, partialTickForRender);
        Vec3 left = center.subtract(right.scale(0.25));
        Vec3 rightPt = center.add(right.scale(0.25));
        return new RibbonSample(left, rightPt, up, 0.0);
    }

    private static Vec3 spineCenter(RibbonSample sample) {
        return sample.left().add(sample.right()).scale(0.5);
    }

    private static double spineDistance(RibbonSample a, RibbonSample b) {
        return ChainLiftRibbonMeshCache.spineCenter(a).distanceTo(ChainLiftRibbonMeshCache.spineCenter(b));
    }

    public static Vec3 ribbonCenterAt(Level level, CoasterPathTrackFrame.GraphHit hit, @Nullable Double partialTickForRender) {
        return ChainLiftRibbonMeshCache.ribbonCenterWorld(level, hit, partialTickForRender);
    }

    public static Vec3 ribbonCenterPlotLocal(Level level, CoasterPathTrackFrame.GraphHit hit) {
        CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit);
        Vec3 up = basis.up();
        up = up.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : up.normalize();
        Vec3 center = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, hit.edge(), hit.t());
        return center.add(up.scale(0.1875));
    }

    private static Vec3 ribbonCenterWorld(Level level, CoasterPathTrackFrame.GraphHit hit, @Nullable Double partialTickForRender) {
        float partialTick = partialTickForRender == null ? 0.0f : partialTickForRender.floatValue();
        boolean clientRender = partialTickForRender != null && level.isClientSide() && CoasterPathGraphManager.edgeHostedOnSubLevel(level, hit.edge());
        CoasterPathTrackFrame.TrackBasis basis = clientRender ? CoasterPathClientRenderSpace.trackBasisAtHit(level, hit, partialTick) : CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender);
        Vec3 up = basis.up();
        up = up.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : up.normalize();
        Vec3 center = clientRender ? CoasterPathClientRenderSpace.sampleEdgePosition(level, hit.edge(), hit.t(), partialTick) : (partialTickForRender == null ? CoasterPathGraphManager.sampleEdgePosition(level, hit.edge(), hit.t(), null) : CoasterPathGraphManager.sampleEdgePosition(level, hit.edge(), hit.t(), partialTickForRender));
        return center.add(up.scale(0.1875));
    }

    private static RibbonVertex vertex(Vec3 pos, Vec3 normal, float u, float vBase) {
        return new RibbonVertex((float)pos.x, (float)pos.y, (float)pos.z, (float)normal.x, (float)normal.y, (float)normal.z, u, vBase);
    }

    private static float baseV(double spineArcBlocks) {
        return (float)(spineArcBlocks / 0.625);
    }

    private static float ribbonTextureV(float vBase, float scrollPhase) {
        return (float)((double)vBase + (double)scrollPhase);
    }

    private static void emitQuad(RibbonVertex[] vertices, int offset, Level level, Matrix4f mat, PoseStack.Pose poseEntry, VertexConsumer consumer, double partialTickForRender, float scrollPhase, boolean backFace) {
        RibbonVertex v0 = vertices[offset];
        RibbonVertex v1 = vertices[offset + 1];
        RibbonVertex v2 = vertices[offset + 2];
        RibbonVertex v3 = vertices[offset + 3];
        Vec3 p0 = ChainLiftRibbonMeshCache.vertexWorldPosition(v0, level, partialTickForRender);
        Vec3 p1 = ChainLiftRibbonMeshCache.vertexWorldPosition(v1, level, partialTickForRender);
        Vec3 p2 = ChainLiftRibbonMeshCache.vertexWorldPosition(v2, level, partialTickForRender);
        Vec3 p3 = ChainLiftRibbonMeshCache.vertexWorldPosition(v3, level, partialTickForRender);
        int light = ChainLiftRibbonMeshCache.ribbonQuadLight(level, p0, p1, p2, p3);
        if (backFace) {
            Vec3 n = ChainLiftRibbonMeshCache.quadFaceNormal(p0, p3, p2);
            float nx = (float)n.x;
            float ny = (float)n.y;
            float nz = (float)n.z;
            ChainLiftRibbonMeshCache.vert(mat, poseEntry, consumer, p0, v0.u, ChainLiftRibbonMeshCache.ribbonTextureV(v0.vBase, scrollPhase), light, nx, ny, nz);
            ChainLiftRibbonMeshCache.vert(mat, poseEntry, consumer, p3, v3.u, ChainLiftRibbonMeshCache.ribbonTextureV(v3.vBase, scrollPhase), light, nx, ny, nz);
            ChainLiftRibbonMeshCache.vert(mat, poseEntry, consumer, p2, v2.u, ChainLiftRibbonMeshCache.ribbonTextureV(v2.vBase, scrollPhase), light, nx, ny, nz);
            ChainLiftRibbonMeshCache.vert(mat, poseEntry, consumer, p1, v1.u, ChainLiftRibbonMeshCache.ribbonTextureV(v1.vBase, scrollPhase), light, nx, ny, nz);
        } else {
            Vec3 n = ChainLiftRibbonMeshCache.quadFaceNormal(p0, p1, p2);
            float nx = (float)n.x;
            float ny = (float)n.y;
            float nz = (float)n.z;
            ChainLiftRibbonMeshCache.vert(mat, poseEntry, consumer, p0, v0.u, ChainLiftRibbonMeshCache.ribbonTextureV(v0.vBase, scrollPhase), light, nx, ny, nz);
            ChainLiftRibbonMeshCache.vert(mat, poseEntry, consumer, p1, v1.u, ChainLiftRibbonMeshCache.ribbonTextureV(v1.vBase, scrollPhase), light, nx, ny, nz);
            ChainLiftRibbonMeshCache.vert(mat, poseEntry, consumer, p2, v2.u, ChainLiftRibbonMeshCache.ribbonTextureV(v2.vBase, scrollPhase), light, nx, ny, nz);
            ChainLiftRibbonMeshCache.vert(mat, poseEntry, consumer, p3, v3.u, ChainLiftRibbonMeshCache.ribbonTextureV(v3.vBase, scrollPhase), light, nx, ny, nz);
        }
    }

    private static Vec3 vertexWorldPosition(RibbonVertex vertex, Level level, double partialTickForRender) {
        return new Vec3((double)vertex.x, (double)vertex.y, (double)vertex.z);
    }

    private static Vec3 quadFaceNormal(Vec3 a, Vec3 b, Vec3 c) {
        Vec3 n = b.subtract(a).cross(c.subtract(a));
        if (n.lengthSqr() < 1.0E-12) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        return n.normalize();
    }

    private static int ribbonQuadLight(Level level, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3) {
        Vec3 center = p0.add(p1).add(p2).add(p3).scale(0.25);
        Vec3 outward = ChainLiftRibbonMeshCache.quadFaceNormal(p0, p1, p2);
        int front = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)BlockPos.containing((Position)center));
        int back = LevelRenderer.getLightColor((BlockAndTintGetter)level, (BlockPos)BlockPos.containing((Position)center.subtract(outward.scale(0.25))));
        return ChainLiftRibbonMeshCache.brighterLight(front, back);
    }

    private static int brighterLight(int a, int b) {
        int blockA = a >> 4 & 0xF;
        int skyA = a >> 20 & 0xF;
        int blockB = b >> 4 & 0xF;
        int skyB = b >> 20 & 0xF;
        return Math.max(blockA, blockB) << 4 | Math.max(skyA, skyB) << 20;
    }

    private static void vert(Matrix4f mat, PoseStack.Pose poseEntry, VertexConsumer consumer, Vec3 p, float u, float v, int light, float nx, float ny, float nz) {
        consumer.addVertex(mat, (float)p.x, (float)p.y, (float)p.z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(poseEntry, nx, ny, nz);
    }

    public record CachedMesh(int rebuildKey, RibbonVertex[] vertices, @Nullable RibbonEndcapFrame start, @Nullable RibbonEndcapFrame end) {
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

    private record RibbonSample(Vec3 left, Vec3 right, Vec3 normal, double texArc) {
        RibbonSample withTexArc(double texArc) {
            return new RibbonSample(this.left, this.right, this.normal, texArc);
        }
    }

    public record RibbonEndcapFrame(Vec3 spine, Vec3 lateral, Vec3 chordIntoTrack) {
    }
}

