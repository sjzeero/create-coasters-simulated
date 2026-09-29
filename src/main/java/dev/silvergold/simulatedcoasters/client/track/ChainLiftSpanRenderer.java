/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.PoseStack$Pose
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.joml.Matrix4f
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.silvergold.simulatedcoasters.client.track.ChainLiftRibbonMeshCache;
import dev.silvergold.simulatedcoasters.client.track.ChainLiftSpanEndcapRenderer;
import dev.silvergold.simulatedcoasters.client.track.CoasterAnchorClientSpace;
import dev.silvergold.simulatedcoasters.track.anchor.ChainLiftClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftDriveDirection;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(value=Dist.CLIENT)
public final class ChainLiftSpanRenderer {
    private static final ResourceLocation CHAIN_TEXTURE = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"textures/block/chain_lift/chain.png");
    private static final double SAMPLE_SPACING_NEAR_BLOCKS = 0.25;
    private static final double SAMPLE_SPACING_MID_BLOCKS = 0.5;
    private static final double SAMPLE_SPACING_FAR_BLOCKS = 1.0;
    private static final double MID_LOD_DISTANCE_SQ = 1024.0;
    private static final double FAR_LOD_DISTANCE_SQ = 4096.0;

    private ChainLiftSpanRenderer() {
    }

    public static void render(Minecraft mc, PoseStack poseStack, MultiBufferSource.BufferSource buffers, Camera camera, float partialTick) {
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Vec3 cam = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f mat = poseStack.last().pose();
        PoseStack.Pose poseEntry = poseStack.last();
        double partialTickForRender = partialTick;
        double maxRenderDistSq = 16384.0;
        for (CoasterAnchorpointBlockEntity anchor : ChainLiftClientIndex.snapshotAnchors()) {
            CoasterPathGraphSpans.SpanPath path;
            BlockPos anchorPos;
            Vec3 anchorWorld;
            if (anchor.isRemoved() || anchor.getLevel() != level || cam.distanceToSqr(anchorWorld = ChainLiftSpanRenderer.anchorRenderCenter((Level)level, anchorPos = anchor.getBlockPos(), partialTickForRender)) > maxRenderDistSq || (path = ChainLiftSpanPathCache.resolve(anchor, partialTickForRender)) == null || path.segments().isEmpty()) continue;
            double sampleSpacing = ChainLiftSpanRenderer.sampleSpacingForDistance(cam, anchorWorld);
            int lodTier = ChainLiftSpanRenderer.lodTierForSpacing(sampleSpacing);
            float scrollPhase = ChainLiftSpanRenderer.scrollPhase(ChainLiftDriveDirection.signedChainBlocksPerSecond(anchor, path, partialTickForRender, 256.0f, 16.0), (Level)level, partialTick);
            ChainLiftRibbonMeshCache.CachedMesh mesh = null;
            if (ChainLiftRibbonMeshCache.pathUsesSubLevelTrack((Level)level, path)) {
                if (!ChainLiftRibbonMeshCache.spanNearCameraLive((Level)level, path, cam, partialTickForRender, maxRenderDistSq)) continue;
                ribbonConsumer = buffers.getBuffer(RenderType.entityCutout((ResourceLocation)CHAIN_TEXTURE));
                ChainLiftRibbonMeshCache.drawLiveSpan((Level)level, path, sampleSpacing, mat, poseEntry, ribbonConsumer, partialTickForRender, scrollPhase);
            } else {
                mesh = ChainLiftRibbonMeshCache.getOrBuild(anchor, (Level)level, path, sampleSpacing, lodTier);
                if (mesh == null || !ChainLiftRibbonMeshCache.spanNearCamera((Level)level, cam, mesh, partialTickForRender, maxRenderDistSq)) continue;
                ribbonConsumer = buffers.getBuffer(RenderType.entityCutout((ResourceLocation)CHAIN_TEXTURE));
                ChainLiftRibbonMeshCache.draw(mesh, (Level)level, mat, poseEntry, ribbonConsumer, partialTickForRender, scrollPhase);
            }
            ChainLiftSpanEndcapRenderer.renderSpan((Level)level, path, mesh, partialTickForRender, cam, maxRenderDistSq, poseStack, buffers);
        }
        poseStack.popPose();
    }

    private static Vec3 anchorRenderCenter(Level level, BlockPos anchorPos, double partialTickForRender) {
        Vec3 local = CoasterAnchorpointBlockEntity.worldCenter(level, anchorPos);
        return CoasterAnchorClientSpace.toRenderWorld(level, anchorPos, local, (float)partialTickForRender);
    }

    private static double sampleSpacingForDistance(Vec3 camera, Vec3 anchorWorld) {
        double distSq = camera.distanceToSqr(anchorWorld);
        if (distSq >= 4096.0) {
            return 1.0;
        }
        if (distSq >= 1024.0) {
            return 0.5;
        }
        return 0.25;
    }

    private static int lodTierForSpacing(double sampleSpacing) {
        if (sampleSpacing >= 1.0) {
            return 2;
        }
        if (sampleSpacing >= 0.5) {
            return 1;
        }
        return 0;
    }

    private static float scrollPhase(double blocksPerSecond, Level level, float partialTick) {
        if (Math.abs(blocksPerSecond) <= 1.0E-9) {
            return 0.0f;
        }
        double time = (double)((float)level.getGameTime() + partialTick) / 20.0;
        double scroll = time * blocksPerSecond / 0.625;
        return (float)(scroll - Math.floor(scroll));
    }
}

