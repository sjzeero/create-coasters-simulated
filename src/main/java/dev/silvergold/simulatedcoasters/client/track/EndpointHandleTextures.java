package dev.silvergold.simulatedcoasters.client.track;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class EndpointHandleTextures {
   public static final ResourceLocation DEFAULT = rl("textures/ui/endpoint_handle.png");
   public static final ResourceLocation HOVER = rl("textures/ui/endpoint_handle_hover.png");
   public static final ResourceLocation DRAGGING = rl("textures/ui/endpoint_handle_drag.png");

   private EndpointHandleTextures() {
   }

   private static ResourceLocation rl(String path) {
      return ResourceLocation.fromNamespaceAndPath("simulatedcoasters", path);
   }
}
