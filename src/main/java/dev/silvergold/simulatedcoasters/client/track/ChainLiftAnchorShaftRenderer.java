/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.simibubi.create.content.kinetics.base.KineticBlockEntity
 *  com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer
 *  net.createmod.catnip.render.CachedBuffers
 *  net.createmod.catnip.render.SuperByteBuffer
 *  net.createmod.catnip.render.SuperByteBufferCache$Compartment
 *  net.minecraft.client.renderer.MultiBufferSource
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider$Context
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerTrackCurveBerRender;
import dev.silvergold.simulatedcoasters.client.track.CoasterAnchorpointShaftModel;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public class ChainLiftAnchorShaftRenderer
extends KineticBlockEntityRenderer<CoasterAnchorpointBlockEntity> {
    public ChainLiftAnchorShaftRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    protected void renderSafe(CoasterAnchorpointBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        AnchorPeerTrackCurveBerRender.render(be, ms, buffer);
        this.renderChainLiftShaft(be, partialTicks, ms, buffer, light);
    }

    private void renderChainLiftShaft(CoasterAnchorpointBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light) {
        Direction.Axis axis = be.getChainLiftShaftAxis();
        if (axis == null) {
            return;
        }
        BlockState anchorState = be.getBlockState();
        Direction facing = (Direction)anchorState.getValue((Property)CoasterAnchorpointBlock.FACING);
        CoasterAnchorpointShaftModel.renderBody(ms, buffer, anchorState, facing, axis, light);
        BlockState shaftState = ChainLiftAnchorShaftRenderer.shaft((Direction.Axis)axis);
        SuperByteBuffer model = CachedBuffers.block((SuperByteBufferCache.Compartment)KINETIC_BLOCK, (BlockState)shaftState);
        BlockPos pos = be.getBlockPos();
        float angle = ChainLiftAnchorShaftRenderer.getAngleForBe((KineticBlockEntity)be, (BlockPos)pos, (Direction.Axis)axis);
        ChainLiftAnchorShaftRenderer.kineticRotationTransform((SuperByteBuffer)model, (KineticBlockEntity)be, (Direction.Axis)axis, (float)angle, (int)light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }

    public boolean shouldRenderOffScreen(CoasterAnchorpointBlockEntity blockEntity) {
        return true;
    }

    public int getViewDistance() {
        return 192;
    }
}

