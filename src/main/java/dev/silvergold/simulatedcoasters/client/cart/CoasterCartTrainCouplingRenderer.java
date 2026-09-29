/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.simibubi.create.AllPartialModels
 *  dev.engine_room.flywheel.lib.model.baked.PartialModel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.math.AngleHelper
 *  net.createmod.catnip.render.CachedBuffers
 *  net.createmod.catnip.render.SuperByteBuffer
 *  net.minecraft.client.Camera
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.cart;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientFrameCache;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientMembership;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartTrainLinkClientState;
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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartTrainCouplingRenderer {
    private static final float MARGIN = 0.1875f;

    private CoasterCartTrainCouplingRenderer() {
    }

    public static void render(Minecraft mc, PoseStack poseStack, MultiBufferSource.BufferSource buffers, Camera camera, DeltaTracker deltaTracker) {
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        Vec3 cam = camera.getPosition();
        VertexConsumer vb = buffers.getBuffer(RenderType.solid());
        BlockState air = Blocks.AIR.defaultBlockState();
        CoasterPathGraph pathGraph = CoasterPathGraphManagerClient.get((Level)level);
        List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs = CoasterCartRailSoundCache.openPathEndRefs();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(level.tickRateManager().runsNormally());
        Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>> links = CoasterCartTrainLinkClientState.snapshot((ResourceKey<Level>)level.dimension());
        if (links.isEmpty()) {
            return;
        }
        CoasterCartClientMembership.forEachCart(level, sub -> {
            List cartLinks = (List)links.get(sub.getUniqueId());
            if (cartLinks == null || cartLinks.isEmpty()) {
                return;
            }
            CoasterCartClientFrameCache.RenderFrame frameA = CoasterCartClientFrameCache.resolveRender(level, sub, pathGraph, openEndRefs, partialTick);
            if (frameA == null) {
                return;
            }
            Vec3 centerA = frameA.sampleVisual();
            if (!centerA.closerThan((Position)cam, 64.0)) {
                return;
            }
            for (CoasterCartTrainLinkSync.LinkEntry link : cartLinks) {
                CoasterCartClientFrameCache.RenderFrame frameB;
                SubLevel partner;
                if (sub.getUniqueId().compareTo(link.partnerId()) > 0 || (partner = CoasterCartResolve.findCartById((Level)level, link.partnerId())) == null || partner.isRemoved() || (frameB = CoasterCartClientFrameCache.resolveRender(level, partner, pathGraph, openEndRefs, partialTick)) == null) continue;
                Vec3 centerB = frameB.sampleVisual();
                int lightA = CoasterCartTrainCouplingRenderer.lightAt((Level)level, centerA);
                int lightB = CoasterCartTrainCouplingRenderer.lightAt((Level)level, centerB);
                CoasterCartTrainCouplingRenderer.renderCoupling(poseStack, vb, cam, air, centerA, centerB, link.centerDistance(), lightA, lightB);
            }
        });
    }

    public static void renderCoupling(PoseStack ms, VertexConsumer vb, Vec3 camera, BlockState air, Vec3 anchorWorld, Vec3 anchor2World, double restLength, int light1, int light2) {
        double diffX = anchor2World.x - anchorWorld.x;
        double diffY = anchor2World.y - anchorWorld.y;
        double diffZ = anchor2World.z - anchorWorld.z;
        float yRot = AngleHelper.deg((double)Mth.atan2((double)diffZ, (double)diffX)) + 90.0f;
        float xRot = AngleHelper.deg((double)Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)));
        Vec3 span = anchor2World.subtract(anchorWorld);
        double len = span.length();
        if (len < 1.0E-6) {
            return;
        }
        Vec3 dir = span.scale(1.0 / len);
        Vec3 anchor = anchorWorld.add(dir.scale(0.1875));
        Vec3 anchor2 = anchor2World.add(dir.scale(-0.1875));
        int couplingSegments = Math.max(1, (int)Math.round((restLength - 0.375) * 4.0));
        double stretch = (anchor2.distanceTo(anchor) - 0.375) * 4.0 / (double)couplingSegments;
        ms.pushPose();
        ms.translate(anchor.x - camera.x, anchor.y - camera.y, anchor.z - camera.z);
        ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial((PartialModel)AllPartialModels.TRAIN_COUPLING_HEAD, (BlockState)air).rotateYDegrees(-yRot)).rotateXDegrees(xRot)).light(light1).renderInto(ms, vb);
        for (int j = 0; j < couplingSegments; ++j) {
            ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial((PartialModel)AllPartialModels.TRAIN_COUPLING_CABLE, (BlockState)air).rotateYDegrees(-yRot + 180.0f)).rotateXDegrees(-xRot)).translate(0.0f, 0.0f, 0.3125f)).scale(1.0f, 1.0f, (float)stretch)).translate(0.0f, 0.0f, (float)j / 4.0f)).light(light1).renderInto(ms, vb);
        }
        ms.popPose();
        ms.pushPose();
        ms.translate(anchor2.x - camera.x, anchor2.y - camera.y, anchor2.z - camera.z);
        ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial((PartialModel)AllPartialModels.TRAIN_COUPLING_HEAD, (BlockState)air).rotateYDegrees(-yRot + 180.0f)).rotateXDegrees(-xRot)).light(light2).renderInto(ms, vb);
        ms.popPose();
    }

    private static int lightAt(Level level, Vec3 pos) {
        BlockPos blockpos = BlockPos.containing((Position)pos);
        return LightTexture.pack((int)level.getBrightness(LightLayer.BLOCK, blockpos), (int)level.getBrightness(LightLayer.SKY, blockpos));
    }
}

