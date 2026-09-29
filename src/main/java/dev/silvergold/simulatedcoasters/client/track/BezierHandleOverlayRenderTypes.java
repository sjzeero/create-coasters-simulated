/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.vertex.DefaultVertexFormat
 *  com.mojang.blaze3d.vertex.VertexFormat
 *  com.mojang.blaze3d.vertex.VertexFormat$Mode
 *  net.minecraft.client.renderer.GameRenderer
 *  net.minecraft.client.renderer.MultiBufferSource$BufferSource
 *  net.minecraft.client.renderer.RenderStateShard
 *  net.minecraft.client.renderer.RenderStateShard$EmptyTextureStateShard
 *  net.minecraft.client.renderer.RenderStateShard$LineStateShard
 *  net.minecraft.client.renderer.RenderStateShard$ShaderStateShard
 *  net.minecraft.client.renderer.RenderStateShard$TextureStateShard
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.RenderType$CompositeState
 *  net.minecraft.resources.ResourceLocation
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleArmTextures;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleLiftTextures;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleTangentTextures;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleTiltTextures;
import dev.silvergold.simulatedcoasters.client.track.EndpointHandleTextures;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class BezierHandleOverlayRenderTypes {
    private static final Map<ResourceLocation, RenderType> TANGENT_HANDLE_BILLBOARDS = new HashMap<ResourceLocation, RenderType>();
    public static final RenderType SEE_THROUGH_LINES = RenderType.create((String)"simulatedcoasters:bezier_handle_lines", (VertexFormat)DefaultVertexFormat.POSITION_COLOR_NORMAL, (VertexFormat.Mode)VertexFormat.Mode.LINES, (int)1536, (RenderType.CompositeState)RenderType.CompositeState.builder().setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER).setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty())).setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setOutputState(RenderStateShard.MAIN_TARGET).setWriteMaskState(RenderStateShard.COLOR_WRITE).setCullState(RenderStateShard.NO_CULL).setDepthTestState(RenderStateShard.NO_DEPTH_TEST).createCompositeState(false));

    private BezierHandleOverlayRenderTypes() {
    }

    public static RenderType tangentHandleBillboard(ResourceLocation texture) {
        return TANGENT_HANDLE_BILLBOARDS.computeIfAbsent(texture, tex -> RenderType.create((String)("simulatedcoasters:tangent_bb_" + tex.getNamespace() + "_" + tex.getPath().replace('/', '_')), (VertexFormat)DefaultVertexFormat.POSITION_TEX_COLOR, (VertexFormat.Mode)VertexFormat.Mode.QUADS, (int)1536, (RenderType.CompositeState)RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader)).setTextureState((RenderStateShard.EmptyTextureStateShard)new RenderStateShard.TextureStateShard(tex, false, false)).setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY).setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING).setOutputState(RenderStateShard.MAIN_TARGET).setWriteMaskState(RenderStateShard.COLOR_WRITE).setCullState(RenderStateShard.NO_CULL).setDepthTestState(RenderStateShard.NO_DEPTH_TEST).createCompositeState(false)));
    }

    public static void endTangentHandleBillboardBatches(MultiBufferSource.BufferSource bufferSource) {
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleTangentTextures.DEFAULT));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleTangentTextures.HOVER));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleTangentTextures.DRAGGING));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleArmTextures.DEFAULT));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleArmTextures.HOVER));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleArmTextures.DRAGGING));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleLiftTextures.DEFAULT));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleLiftTextures.HOVER));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleLiftTextures.DRAGGING));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleTiltTextures.DEFAULT));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleTiltTextures.HOVER));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(BezierHandleTiltTextures.DRAGGING));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(EndpointHandleTextures.DEFAULT));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(EndpointHandleTextures.HOVER));
        bufferSource.endBatch(BezierHandleOverlayRenderTypes.tangentHandleBillboard(EndpointHandleTextures.DRAGGING));
    }
}

