package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import java.util.UUID;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartEditOutlineClient {
   private static final String OUTLINER_KEY = "coaster_cart_edit_active";
   private static final int OUTLINE_COLOR_CONNECTED = 16768341;
   private static final int OUTLINE_COLOR_UNCONNECTED = 8947848;

   private CoasterCartEditOutlineClient() {
   }

   @Deprecated
   public static void clientTick(Minecraft mc) {
      if (mc.level != null) {
         renderFrame(mc, AnimationTickHolder.getPartialTicks(mc.level));
      }
   }

   public static void renderFrame(Minecraft mc, float partialTick) {
      Outliner outliner = Outliner.getInstance();
      if (!CoasterCartEditMode.isActive()) {
         CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_edit_active");
      } else if (mc.player != null && mc.level != null && !mc.options.hideGui) {
         UUID cartId = CoasterCartEditMode.getActiveCartId();
         if (cartId == null) {
            CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_edit_active");
         } else {
            SubLevel cart = CoasterCartResolve.findCartById(mc.level, cartId);
            if (cart != null && !cart.isRemoved()) {
               boolean hasConnections = !CoasterCartTrainLinkClientState.linksFor(mc.level.dimension(), cartId).isEmpty();
               int color = hasConnections ? 16768341 : 8947848;
               drawCartOutline(outliner, cart, partialTick, color);
            } else {
               CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_edit_active");
            }
         }
      } else {
         CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_edit_active");
      }
   }

   private static void drawCartOutline(Outliner outliner, SubLevel cart, float partialTick, int color) {
      Pose3d pose = new Pose3d();
      cart.lastPose().lerp(cart.logicalPose(), (double)partialTick, pose);
      Vec3 plotOrigin = CoasterCartMidTrackPlacement.plotOriginFromPlacedCart(cart, pose);
      if (plotOrigin == null) {
         CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_edit_active");
      } else {
         CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(outliner, "coaster_cart_edit_active", plotOrigin, pose.orientation(), color);
      }
   }
}
