package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import java.util.UUID;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartLinkSelectionOutlineRenderer {
   private static final String OUTLINER_KEY_PREFIX = "coaster_cart_link_selection";

   private CoasterCartLinkSelectionOutlineRenderer() {
   }

   public static void renderFrame(Minecraft mc, float partialTick) {
      Outliner outliner = Outliner.getInstance();
      if (mc.player != null && mc.level != null && !mc.options.hideGui) {
         UUID targetId = linkTargetFromHands(mc);
         if (targetId == null) {
            CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_link_selection");
         } else {
            SubLevel cart = CoasterCartResolve.findCartById(mc.level, targetId);
            if (cart != null && !cart.isRemoved()) {
               Pose3d pose = new Pose3d();
               cart.lastPose().lerp(cart.logicalPose(), (double)partialTick, pose);
               Vec3 plotOrigin = CoasterCartMidTrackPlacement.plotOriginFromPlacedCart(cart, pose);
               if (plotOrigin == null) {
                  CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_link_selection");
               } else {
                  CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(
                     outliner, "coaster_cart_link_selection", plotOrigin, pose.orientation(), CoasterCartPlacementPreviewRenderer.linkPartnerOutlineColor()
                  );
               }
            } else {
               CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_link_selection");
            }
         }
      } else {
         CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_link_selection");
      }
   }

   @Nullable
   private static UUID linkTargetFromHands(Minecraft mc) {
      ItemStack main = mc.player.getMainHandItem();
      if (main.is(SimulatedCoasters.COASTER_CART) && CoasterCartItem.hasLinkSelection(main)) {
         return CoasterCartLinkSelection.readLinkTarget(main);
      } else {
         ItemStack off = mc.player.getOffhandItem();
         return off.is(SimulatedCoasters.COASTER_CART) && CoasterCartItem.hasLinkSelection(off) ? CoasterCartLinkSelection.readLinkTarget(off) : null;
      }
   }
}
