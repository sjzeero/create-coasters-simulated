package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalDouble;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderStateShard.LineStateShard;
import net.minecraft.client.renderer.RenderStateShard.ShaderStateShard;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BezierHandleOverlayRenderTypes {
   private static final Map<ResourceLocation, RenderType> TANGENT_HANDLE_BILLBOARDS = new HashMap<>();
   public static final RenderType SEE_THROUGH_LINES = RenderType.create(
      "simulatedcoasters:bezier_handle_lines",
      DefaultVertexFormat.POSITION_COLOR_NORMAL,
      Mode.LINES,
      1536,
      CompositeState.builder()
         .setShaderState(RenderStateShard.RENDERTYPE_LINES_SHADER)
         .setLineState(new LineStateShard(OptionalDouble.empty()))
         .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
         .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
         .setOutputState(RenderStateShard.MAIN_TARGET)
         .setWriteMaskState(RenderStateShard.COLOR_WRITE)
         .setCullState(RenderStateShard.NO_CULL)
         .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
         .createCompositeState(false)
   );

   private BezierHandleOverlayRenderTypes() {
   }

   public static RenderType tangentHandleBillboard(ResourceLocation texture) {
      return TANGENT_HANDLE_BILLBOARDS.computeIfAbsent(
         texture,
         tex -> RenderType.create(
               "simulatedcoasters:tangent_bb_" + tex.getNamespace() + "_" + tex.getPath().replace('/', '_'),
               DefaultVertexFormat.POSITION_TEX_COLOR,
               Mode.QUADS,
               1536,
               CompositeState.builder()
                  .setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                  .setTextureState(new TextureStateShard(tex, false, false))
                  .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                  .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                  .setOutputState(RenderStateShard.MAIN_TARGET)
                  .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                  .setCullState(RenderStateShard.NO_CULL)
                  .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                  .createCompositeState(false)
            )
      );
   }

   public static void endTangentHandleBillboardBatches(BufferSource bufferSource) {
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleTangentTextures.DEFAULT));
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleTangentTextures.HOVER));
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleTangentTextures.DRAGGING));
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleArmTextures.DEFAULT));
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleArmTextures.HOVER));
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleArmTextures.DRAGGING));
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleLiftTextures.DEFAULT));
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleLiftTextures.HOVER));
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleLiftTextures.DRAGGING));
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleTiltTextures.DEFAULT));
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleTiltTextures.HOVER));
      bufferSource.endBatch(tangentHandleBillboard(BezierHandleTiltTextures.DRAGGING));
      bufferSource.endBatch(tangentHandleBillboard(EndpointHandleTextures.DEFAULT));
      bufferSource.endBatch(tangentHandleBillboard(EndpointHandleTextures.HOVER));
      bufferSource.endBatch(tangentHandleBillboard(EndpointHandleTextures.DRAGGING));
   }
}
