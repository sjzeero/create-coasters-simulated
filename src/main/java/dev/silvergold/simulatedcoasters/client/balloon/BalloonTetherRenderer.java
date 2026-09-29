/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.joml.Matrix4f
 *  org.joml.Vector3d
 */
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
import dev.silvergold.simulatedcoasters.client.balloon.BalloonClientSpace;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3d;

@OnlyIn(value=Dist.CLIENT)
public final class BalloonTetherRenderer {
    private static final double MAX_RENDER_DISTANCE_SQ = 4096.0;
    private static final float STRING_R = 0.92941177f;
    private static final float STRING_G = 0.9490196f;
    private static final float STRING_B = 0.9490196f;
    private static final double HOST_RENDER_INSET_BLOCKS = 0.0625;
    private static final float STRING_WIDTH_BLOCKS = 0.032f;

    private BalloonTetherRenderer() {
    }

    public static void render(Minecraft mc, PoseStack poseStack, MultiBufferSource.BufferSource buffers, Camera camera, float partialTick) {
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        SubLevelContainer container = SubLevelContainer.getContainer((Level)level);
        if (container == null) {
            return;
        }
        Vec3 cam = camera.getPosition();
        for (SubLevel balloonSub : container.getAllSubLevels()) {
            EmbeddedPlotLevelAccessor embedded;
            BlockEntity blockEntity;
            if (balloonSub.isRemoved() || !BalloonResolve.isBalloonSubLevel(balloonSub) || !((blockEntity = (embedded = balloonSub.getPlot().getEmbeddedLevelAccessor()).getBlockEntity(BlockPos.ZERO)) instanceof BalloonBlockEntity)) continue;
            BalloonBlockEntity be = (BalloonBlockEntity)blockEntity;
            BlockPos hostPlotLocal = be.hostPos();
            SubLevel hostSub = BalloonClientSpace.resolveHostSubLevel((Level)level, be);
            if (hostSub == null) continue;
            BlockPos balloonPlotLocal = BalloonHostKey.BALLOON_BLOCK_CELL;
            Vec3 tieWorld = BalloonClientSpace.plotPointRenderWorld(balloonSub, BalloonPlacement.stringTiePlot(balloonSub, balloonPlotLocal), partialTick);
            Vector3d hostPlot = BalloonPlacement.hostAnchorPlot(hostSub, hostPlotLocal);
            hostPlot.y -= 0.0625;
            Vec3 hostWorld = BalloonClientSpace.plotPointRenderWorld(hostSub, hostPlot, partialTick);
            if (cam.distanceToSqr(tieWorld) > 4096.0 && cam.distanceToSqr(hostWorld) > 4096.0) continue;
            BalloonTetherRenderer.renderLeashSegment(poseStack, buffers, cam, tieWorld, hostWorld, (Level)level);
        }
    }

    private static void renderLeashSegment(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Vec3 camera, Vec3 startWorld, Vec3 endWorld, Level level) {
        float side1Z;
        float side1Y;
        float side1X;
        float side1LenSq;
        poseStack.pushPose();
        poseStack.translate(startWorld.x - camera.x, startWorld.y - camera.y, startWorld.z - camera.z);
        float dx = (float)(endWorld.x - startWorld.x);
        float dy = (float)(endWorld.y - startWorld.y);
        float dz = (float)(endWorld.z - startWorld.z);
        Matrix4f pose = poseStack.last().pose();
        float halfWidth = 0.016f;
        float lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq < 1.0E-10f) {
            poseStack.popPose();
            return;
        }
        float invLen = Mth.invSqrt((float)lenSq);
        float dirX = dx * invLen;
        float dirY = dy * invLen;
        float dirZ = dz * invLen;
        float refX = 0.0f;
        float refY = 1.0f;
        float refZ = 0.0f;
        if (Math.abs(dirY) > 0.95f) {
            refX = 1.0f;
            refY = 0.0f;
        }
        if ((side1LenSq = (side1X = dirY * refZ - dirZ * refY) * side1X + (side1Y = dirZ * refX - dirX * refZ) * side1Y + (side1Z = dirX * refY - dirY * refX) * side1Z) < 1.0E-10f) {
            poseStack.popPose();
            return;
        }
        float side1Scale = Mth.invSqrt((float)side1LenSq) * halfWidth;
        float side2X = dirY * (side1Z *= side1Scale) - dirZ * (side1Y *= side1Scale);
        float side2Y = dirZ * (side1X *= side1Scale) - dirX * side1Z;
        float side2Z = dirX * side1Y - dirY * side1X;
        BlockPos startLightPos = BlockPos.containing((Position)startWorld);
        BlockPos endLightPos = BlockPos.containing((Position)endWorld);
        int startBlockLight = level.getBrightness(LightLayer.BLOCK, startLightPos);
        int endBlockLight = level.getBrightness(LightLayer.BLOCK, endLightPos);
        int startSkyLight = level.getBrightness(LightLayer.SKY, startLightPos);
        int endSkyLight = level.getBrightness(LightLayer.SKY, endLightPos);
        BalloonTetherRenderer.renderLeashRibbon(buffers, pose, dx, dy, dz, startBlockLight, endBlockLight, startSkyLight, endSkyLight, side1X, side1Y, side1Z);
        BalloonTetherRenderer.renderLeashRibbon(buffers, pose, dx, dy, dz, startBlockLight, endBlockLight, startSkyLight, endSkyLight, side2X, side2Y, side2Z);
        poseStack.popPose();
    }

    private static void renderLeashRibbon(MultiBufferSource.BufferSource buffers, Matrix4f pose, float dx, float dy, float dz, int startBlockLight, int endBlockLight, int startSkyLight, int endSkyLight, float sideX, float sideY, float sideZ) {
        int i;
        VertexConsumer buffer = buffers.getBuffer(RenderType.leash());
        for (i = 0; i <= 24; ++i) {
            BalloonTetherRenderer.addLeashVertexPair(buffer, pose, dx, dy, dz, startBlockLight, endBlockLight, startSkyLight, endSkyLight, 0.032f, 0.032f, sideX, sideY, sideZ, i);
        }
        for (i = 24; i >= 0; --i) {
            BalloonTetherRenderer.addLeashVertexPair(buffer, pose, dx, dy, dz, startBlockLight, endBlockLight, startSkyLight, endSkyLight, 0.032f, 0.0f, sideX, sideY, sideZ, i);
        }
        buffers.endBatch(RenderType.leash());
    }

    private static void addLeashVertexPair(VertexConsumer buffer, Matrix4f pose, float startX, float startY, float startZ, int startBlockLight, int endBlockLight, int startSkyLight, int endSkyLight, float yOffset, float dy, float sideX, float sideY, float sideZ, int index) {
        float t = (float)index / 24.0f;
        int blockLight = (int)Mth.lerp((float)t, (float)startBlockLight, (float)endBlockLight);
        int skyLight = (int)Mth.lerp((float)t, (float)startSkyLight, (float)endSkyLight);
        int packedLight = LightTexture.pack((int)blockLight, (int)skyLight);
        float x = startX * t;
        float y = startY > 0.0f ? startY * t * t : startY - startY * (1.0f - t) * (1.0f - t);
        float z = startZ * t;
        buffer.addVertex(pose, x - sideX, y + dy, z - sideZ).setColor(0.92941177f, 0.9490196f, 0.9490196f, 1.0f).setLight(packedLight);
        buffer.addVertex(pose, x + sideX, y + yOffset - dy, z + sideZ).setColor(0.92941177f, 0.9490196f, 0.9490196f, 1.0f).setLight(packedLight);
    }
}

