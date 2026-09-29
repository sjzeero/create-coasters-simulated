package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import java.util.UUID;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartHoverOutlineClient {
   private static final String OUTLINER_KEY = "coaster_cart_hover";
   private static final int OUTLINE_COLOR = 1940687;

   private CoasterCartHoverOutlineClient() {
   }

   public static void renderFrame(Minecraft mc, float partialTick) {
      Outliner outliner = Outliner.getInstance();
      if (mc.player != null
         && mc.level != null
         && !mc.options.hideGui
         && CoasterCartMidTrackPlacementClient.holdingCoasterCart(mc)
         && !CoasterCartEditMode.isActive()) {
         SubLevel hoverCart = mc.hitResult == null ? null : CoasterCartChainLinkInteraction.resolveHoveredCart(mc.level, mc.hitResult);
         if (hoverCart == null) {
            CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_hover");
         } else {
            UUID linkTargetId = linkTargetFromHands(mc);
            if (linkTargetId != null && linkTargetId.equals(hoverCart.getUniqueId())) {
               CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_hover");
            } else {
               Pose3d pose = new Pose3d();
               hoverCart.lastPose().lerp(hoverCart.logicalPose(), (double)partialTick, pose);
               Vec3 plotOrigin = CoasterCartMidTrackPlacement.plotOriginFromPlacedCart(hoverCart, pose);
               if (plotOrigin == null) {
                  CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_hover");
               } else {
                  CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(outliner, "coaster_cart_hover", plotOrigin, pose.orientation(), 1940687);
               }
            }
         }
      } else {
         CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_hover");
      }
   }

   @Nullable
   private static UUID linkTargetFromHands(Minecraft mc) {
      ItemStack main = mc.player.getMainHandItem();
      if (CoasterCartItem.hasLinkSelection(main)) {
         return CoasterCartLinkSelection.readLinkTarget(main);
      } else {
         ItemStack off = mc.player.getOffhandItem();
         return CoasterCartItem.hasLinkSelection(off) ? CoasterCartLinkSelection.readLinkTarget(off) : null;
      }
   }
}
