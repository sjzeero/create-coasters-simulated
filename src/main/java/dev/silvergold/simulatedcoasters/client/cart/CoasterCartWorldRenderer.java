/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.client.Camera
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.block.ModelBlockRenderer
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.cart;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartBottomSliceRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientFrameCache;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientMembership;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartGlueRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartWheelAxisRenderer;
import dev.silvergold.simulatedcoasters.client.sound.CoasterCartRailSoundCache;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTraceEndpoints;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartWorldRenderer {
    private static final double DRAW_RAD_SQ = 9216.0;

    private CoasterCartWorldRenderer() {
    }

    public static void render(Minecraft mc, PoseStack poseStack, MultiBufferSource.BufferSource buffers, Camera camera, DeltaTracker deltaTracker) {
        if (!CoasterCartBottomSliceRenderer.hasModel() && !CoasterCartWheelAxisRenderer.hasModel()) {
            return;
        }
        ClientLevel level = mc.level;
        if (!(level instanceof ClientLevel)) {
            return;
        }
        ClientLevel cl = level;
        CoasterPathGraph pathGraph = CoasterPathGraphManagerClient.get((Level)level);
        List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs = CoasterCartRailSoundCache.openPathEndRefs();
        CoasterCartBlock bearingBlock = (CoasterCartBlock)((Object)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get());
        BlockState tintState = bearingBlock.defaultBlockState();
        int tint = mc.getBlockColors().getColor(tintState, null, null, 0);
        float cr = (float)(tint >> 16 & 0xFF) / 255.0f;
        float cg = (float)(tint >> 8 & 0xFF) / 255.0f;
        float cb = (float)(tint & 0xFF) / 255.0f;
        ModelBlockRenderer modelRenderer = mc.getBlockRenderer().getModelRenderer();
        Vec3 cam = camera.getPosition();
        double camX = cam.x;
        double camY = cam.y;
        double camZ = cam.z;
        Vec3 eye = cam;
        float ptF = deltaTracker.getGameTimeDeltaPartialTick(cl.tickRateManager().runsNormally());
        float dtSec = deltaTracker.getRealtimeDeltaTicks() / 20.0f;
        RandomSource bottomRand = RandomSource.create((long)42L);
        RandomSource wheelRand = RandomSource.create((long)43L);
        CoasterCartClientMembership.forEachCart(cl, arg_0 -> CoasterCartWorldRenderer.lambda$render$1(cl, pathGraph, openEndRefs, ptF, eye, dtSec, camX, camY, camZ, poseStack, modelRenderer, buffers, tintState, cr, cg, cb, bottomRand, (Level)level, wheelRand, arg_0));
    }

    private static /* synthetic */ void lambda$render$1(ClientLevel cl, CoasterPathGraph pathGraph, List openEndRefs, float ptF, Vec3 eye, float dtSec, double camX, double camY, double camZ, PoseStack poseStack, ModelBlockRenderer modelRenderer, MultiBufferSource.BufferSource buffers, BlockState tintState, float cr, float cg, float cb, RandomSource bottomRand, Level level, RandomSource wheelRand, SubLevel sub) {
        CoasterCartClientFrameCache.RenderFrame frame = CoasterCartClientFrameCache.resolveRender(cl, sub, pathGraph, openEndRefs, ptF);
        if (frame == null) {
            return;
        }
        if (frame.sampleVisual().distanceToSqr(eye) > 9216.0) {
            return;
        }
        boolean drawGlue = frame.drawStaffGlue();
        CoasterCartWheelAxisRenderer.SubWheelState wheelState = CoasterCartWheelAxisRenderer.prepareSubWheelState(sub, frame, dtSec, (float)camX, (float)camY, (float)camZ);
        CoasterCartClientMembership.forEachBearingWorldCell(sub, mut -> {
            CoasterCartBottomSliceRenderer.renderCell(poseStack, modelRenderer, buffers, tintState, cr, cg, cb, bottomRand, level, frame, camX, camY, camZ, mut);
            if (wheelState != null) {
                CoasterCartWheelAxisRenderer.renderCell(poseStack, modelRenderer, (MultiBufferSource)buffers, tintState, cr, cg, cb, wheelRand, level, sub, wheelState, mut);
            }
            if (drawGlue) {
                CoasterCartGlueRenderer.renderCell(poseStack, buffers, level, frame, camX, camY, camZ, mut);
            }
        });
    }
}

