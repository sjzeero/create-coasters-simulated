/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.mojang.blaze3d.vertex.VertexConsumer
 *  com.mojang.math.Axis
 *  net.createmod.ponder.api.element.AnimatedSceneElement
 *  net.createmod.ponder.api.level.PonderLevel
 *  net.createmod.ponder.foundation.PonderScene
 *  net.createmod.ponder.foundation.element.AnimatedSceneElementBase
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.renderer.LightTexture
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package dev.silvergold.simulatedcoasters.ponder.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartBottomSliceRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartTrainCouplingRenderer;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartWheelAxisRenderer;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import java.util.ArrayList;
import java.util.List;
import net.createmod.ponder.api.element.AnimatedSceneElement;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.AnimatedSceneElementBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CoasterCartExtrasElement
extends AnimatedSceneElementBase
implements AnimatedSceneElement {
    private final List<BlockPos> cartCorners;
    private final float bodyYawDegrees;
    private final List<CouplerSpan> couplers;
    private final boolean hadCouplers;
    private boolean couplerVisible;

    public CoasterCartExtrasElement(List<BlockPos> cartCorners, float bodyYawDegrees, List<CouplerSpan> couplers) {
        this.cartCorners = List.copyOf(cartCorners);
        this.bodyYawDegrees = bodyYawDegrees;
        this.couplers = List.copyOf(couplers);
        this.couplerVisible = this.hadCouplers = !this.couplers.isEmpty();
    }

    public void setCouplerVisible(boolean visible) {
        this.couplerVisible = visible;
    }

    public void reset(PonderScene scene) {
        super.reset(scene);
        this.couplerVisible = this.hadCouplers;
    }

    public static CoasterCartExtrasElement carts(float bodyYawDegrees, BlockPos ... corners) {
        return new CoasterCartExtrasElement(List.of(corners), bodyYawDegrees, List.of());
    }

    public static CoasterCartExtrasElement linkedCarts(float bodyYawDegrees, BlockPos cartA, BlockPos cartB) {
        return CoasterCartExtrasElement.trainCarts(bodyYawDegrees, cartA, cartB);
    }

    public static CoasterCartExtrasElement trainCarts(float bodyYawDegrees, BlockPos ... corners) {
        List<BlockPos> carts = List.of(corners);
        ArrayList<CouplerSpan> spans = new ArrayList<CouplerSpan>();
        int i = 0;
        while (i + 1 < carts.size()) {
            Vec3 a = Vec3.atCenterOf((Vec3i)((Vec3i)carts.get(i)));
            Vec3 b = Vec3.atCenterOf((Vec3i)((Vec3i)carts.get(i + 1)));
            spans.add(new CouplerSpan(a, b, a.distanceTo(b)));
            ++i;
        }
        return new CoasterCartExtrasElement(carts, bodyYawDegrees, spans);
    }

    protected void renderLast(PonderLevel world, MultiBufferSource buffer, GuiGraphics graphics, float fade, float pt) {
        if (fade < 0.0625f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        BlockState tintState = ((CoasterCartBlock)((Object)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get())).defaultBlockState();
        int tint = mc.getBlockColors().getColor(tintState, null, null, 0);
        float cr = (float)(tint >> 16 & 0xFF) / 255.0f;
        float cg = (float)(tint >> 8 & 0xFF) / 255.0f;
        float cb = (float)(tint & 0xFF) / 255.0f;
        PoseStack poseStack = graphics.pose();
        for (BlockPos corner : this.cartCorners) {
            Vec3 center = Vec3.atCenterOf((Vec3i)corner);
            poseStack.pushPose();
            if (this.bodyYawDegrees != 0.0f) {
                poseStack.translate(center.x, center.y, center.z);
                poseStack.mulPose(Axis.YP.rotationDegrees(this.bodyYawDegrees));
                poseStack.translate(-center.x, -center.y, -center.z);
            }
            CoasterCartBottomSliceRenderer.renderStandingAt(poseStack, buffer, tintState, cr, cg, cb, (Level)world, corner, 0.0, 0.0, 0.0);
            CoasterCartWheelAxisRenderer.renderStandingAt(poseStack, buffer, tintState, cr, cg, cb, (Level)world, corner, 0.0, 0.0, 0.0);
            poseStack.popPose();
        }
        if (this.couplerVisible && !this.couplers.isEmpty()) {
            VertexConsumer vb = buffer.getBuffer(RenderType.solid());
            BlockState air = Blocks.AIR.defaultBlockState();
            for (CouplerSpan span : this.couplers) {
                int lightA = CoasterCartExtrasElement.lightAt(world, span.a());
                int lightB = CoasterCartExtrasElement.lightAt(world, span.b());
                CoasterCartTrainCouplingRenderer.renderCoupling(poseStack, vb, Vec3.ZERO, air, span.a(), span.b(), span.restLength(), lightA, lightB);
            }
        }
    }

    private static int lightAt(PonderLevel world, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing((Position)pos);
        return LightTexture.pack((int)world.getBrightness(LightLayer.BLOCK, blockPos), (int)world.getBrightness(LightLayer.SKY, blockPos));
    }

    private record CouplerSpan(Vec3 a, Vec3 b, double restLength) {
    }
}

