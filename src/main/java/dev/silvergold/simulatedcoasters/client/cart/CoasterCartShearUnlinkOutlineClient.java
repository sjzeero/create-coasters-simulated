package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartShearUnlinkInteraction;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartShearUnlinkOutlineClient {
   private static final String OUTLINER_KEY = "coaster_cart_shear_unlink";
   private static final int OUTLINE_COLOR = 16729927;

   private CoasterCartShearUnlinkOutlineClient() {
   }

   public static void renderFrame(Minecraft mc, float partialTick) {
      Outliner outliner = Outliner.getInstance();
      if (mc.player != null && mc.level != null && !mc.options.hideGui && !CoasterCartEditMode.isActive()) {
         ItemStack shears = shearsInHand(mc);
         if (!CoasterCartShearUnlinkInteraction.holdingShears(shears)) {
            CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_shear_unlink");
         } else {
            SubLevel hoverCart = mc.hitResult == null ? null : CoasterCartChainLinkInteraction.resolveHoveredCart(mc.level, mc.hitResult);
            if (hoverCart != null && !CoasterCartTrainLinkClientState.linksFor(mc.level.dimension(), hoverCart.getUniqueId()).isEmpty()) {
               Pose3d pose = new Pose3d();
               hoverCart.lastPose().lerp(hoverCart.logicalPose(), (double)partialTick, pose);
               Vec3 plotOrigin = CoasterCartMidTrackPlacement.plotOriginFromPlacedCart(hoverCart, pose);
               if (plotOrigin == null) {
                  CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_shear_unlink");
               } else {
                  CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(outliner, "coaster_cart_shear_unlink", plotOrigin, pose.orientation(), 16729927);
               }
            } else {
               CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_shear_unlink");
            }
         }
      } else {
         CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_shear_unlink");
      }
   }

   @Nullable
   private static ItemStack shearsInHand(Minecraft mc) {
      ItemStack main = mc.player.getMainHandItem();
      if (CoasterCartShearUnlinkInteraction.holdingShears(main)) {
         return main;
      } else {
         ItemStack off = mc.player.getOffhandItem();
         return CoasterCartShearUnlinkInteraction.holdingShears(off) ? off : ItemStack.EMPTY;
      }
   }
}
