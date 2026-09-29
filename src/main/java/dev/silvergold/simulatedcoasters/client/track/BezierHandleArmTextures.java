package dev.silvergold.simulatedcoasters.client.track;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BezierHandleArmTextures {
   public static final ResourceLocation DEFAULT = rl("textures/ui/handle_arm.png");
   public static final ResourceLocation HOVER = rl("textures/ui/handle_arm_hover.png");
   public static final ResourceLocation DRAGGING = rl("textures/ui/handle_arm_drag.png");

   private BezierHandleArmTextures() {
   }

   private static ResourceLocation rl(String path) {
      return ResourceLocation.fromNamespaceAndPath("simulatedcoasters", path);
   }
}
