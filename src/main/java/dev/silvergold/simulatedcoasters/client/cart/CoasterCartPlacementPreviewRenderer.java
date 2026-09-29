package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlacementCollision;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import java.util.UUID;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterCartPlacementPreviewRenderer {
   private static final String OUTLINER_KEY_PREFIX = "coaster_cart_placement_preview";
   private static final int VALID_OUTLINE_COLOR = Color.mixColors(10395294, 9817409, 0.8F);
   private static final int LINK_PLACEMENT_OUTLINE_COLOR = 1940687;
   private static final int BLOCKED_OUTLINE_COLOR = 16729927;
   private static boolean linkPlacementTooFar;
   private static boolean frameLinking;
   private static boolean frameLinkBlocked;
   @Nullable
   private static String frameStructuralRejectKey;
   private static double frameLinkDistanceBlocks = Double.NaN;
   @Nullable
   private static Vec3 framePartnerCenter;
   @Nullable
   private static Vec3 framePreviewCenter;

   private CoasterCartPlacementPreviewRenderer() {
   }

   public static boolean isLinkPlacementTooFar() {
      return linkPlacementTooFar;
   }

   public static int linkPartnerOutlineColor() {
      return !linkPlacementTooFar && frameStructuralRejectKey == null ? 1940687 : 16729927;
   }

   public static boolean suppressDefaultCurveHighlight(Minecraft mc) {
      return CoasterCartMidTrackPlacementClient.holdingCoasterCart(mc) && CoasterCartMidTrackPlacementClient.midSpanCurveHover();
   }

   public static void renderFrame(Minecraft mc, float partialTick) {
      linkPlacementTooFar = false;
      frameLinking = false;
      frameLinkBlocked = false;
      frameStructuralRejectKey = null;
      frameLinkDistanceBlocks = Double.NaN;
      framePartnerCenter = null;
      framePreviewCenter = null;
      Outliner outliner = Outliner.getInstance();
      if (mc.player != null
         && mc.level != null
         && !mc.options.hideGui
         && CoasterCartMidTrackPlacementClient.holdingCoasterCart(mc)
         && CoasterCartMidTrackPlacementClient.placementPreviewActive()) {
         CoasterCartMidTrackPlacement.PlacementPose pose = resolvePreviewPose(mc, partialTick);
         if (pose == null) {
            clearOutlines(outliner);
         } else {
            ItemStack linkStack = linkTargetStack(mc);
            boolean linkTooFar = false;
            String structuralRejectKey = null;
            boolean linking = false;
            double linkDistanceBlocks = Double.NaN;
            Vec3 partnerCenter = null;
            if (linkStack != null) {
               CoasterCartLinkPlacement.AdjustedPlacement adjusted = CoasterCartLinkPlacement.adjustForLinkSelection(
                  mc.level, linkStack, pose, (double)partialTick, CoasterCartLinkSnapClient.isSuppressLinkSnap()
               );
               if (adjusted != null) {
                  pose = adjusted.pose();
                  linkDistanceBlocks = adjusted.centerDistanceBlocks();
                  linkTooFar = adjusted.tooFar();
                  linking = true;
                  UUID linkTargetId = CoasterCartLinkSelection.readLinkTarget(linkStack);
                  if (linkTargetId != null) {
                     SubLevel partnerCart = CoasterCartResolve.findCartById(mc.level, linkTargetId);
                     if (partnerCart != null && !partnerCart.isRemoved()) {
                        partnerCenter = CoasterCartLinkPlacement.partnerBearingCenterWorld(partnerCart, (double)partialTick);
                        if (!linkTooFar && CoasterCartTrainLinkConstraint.isAtLinkLimit(partnerCart)) {
                           structuralRejectKey = "simulatedcoasters.cart.link_full";
                        }
                     }
                  }
               }
            }

            boolean obstructed = CoasterCartPlacementCollision.hasSubLevelObstruction(mc.level, pose.plotOriginWorld(), pose.orientation(), partialTick);
            boolean linkBlocked = linkTooFar || structuralRejectKey != null;
            if (!linkTooFar) {
               int color = !obstructed && structuralRejectKey == null ? (linking ? 1940687 : VALID_OUTLINE_COLOR) : 16729927;
               CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(
                  outliner, "coaster_cart_placement_preview", pose.plotOriginWorld(), pose.orientation(), color
               );
            } else {
               linkPlacementTooFar = true;
               clearOutlines(outliner);
            }

            frameLinking = linking;
            frameLinkBlocked = linkBlocked;
            frameStructuralRejectKey = structuralRejectKey;
            frameLinkDistanceBlocks = linkDistanceBlocks;
            framePartnerCenter = partnerCenter;
            if (linking && !linkTooFar) {
               framePreviewCenter = CoasterCartLinkPlacement.bearingCenterWorld(pose.plotOriginWorld(), pose.orientation());
            }

            if (linkTooFar) {
               linkPlacementTooFar = true;
            }
         }
      } else {
         clearOutlines(outliner);
      }
   }

   public static void clientTick(Minecraft mc) {
      if (frameLinking && mc.player != null) {
         if (!frameLinkBlocked && framePartnerCenter != null && framePreviewCenter != null) {
            ClientLevel var2 = mc.level;
            if (var2 instanceof ClientLevel) {
               CoasterCartLinkPreviewParticles.spawnSpan(var2, framePartnerCenter, framePreviewCenter);
            }
         }

         mc.player
            .displayClientMessage(
               (Component)(frameStructuralRejectKey != null
                  ? CoasterTrackPlacement.placementErrorMessage(frameStructuralRejectKey)
                  : (
                     linkPlacementTooFar
                        ? CoasterCartLinkPlacement.linkTooFarActionBarComponent()
                        : CoasterCartLinkPlacement.linkDistanceActionBarComponent(frameLinkDistanceBlocks)
                  )),
               true
            );
      }
   }

   private static void clearOutlines(Outliner outliner) {
      CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_placement_preview");
   }

   @Nullable
   private static ItemStack linkTargetStack(Minecraft mc) {
      CoasterCartItem cart = (CoasterCartItem)SimulatedCoasters.COASTER_CART.get();
      if (mc.player.getMainHandItem().is(cart) && CoasterCartItem.hasLinkSelection(mc.player.getMainHandItem())) {
         return mc.player.getMainHandItem();
      } else {
         return mc.player.getOffhandItem().is(cart) && CoasterCartItem.hasLinkSelection(mc.player.getOffhandItem()) ? mc.player.getOffhandItem() : null;
      }
   }

   @Nullable
   private static CoasterCartMidTrackPlacement.PlacementPose resolvePreviewPose(Minecraft mc, float partialTick) {
      if (mc.level == null || mc.player == null) {
         return null;
      } else if (CoasterCartMidTrackPlacementClient.midSpanCurveHover()) {
         return CoasterCartMidTrackPlacementClient.resolveHoverPose(mc, mc.player.getLookAngle(), (double)partialTick);
      } else {
         BlockPos anchor = CoasterCartMidTrackPlacementClient.anchorHoverPos();
         return anchor == null
            ? null
            : CoasterCartMidTrackPlacement.resolveFromAnchor(mc.level, mc.player, anchor, mc.player.getLookAngle(), null, (double)partialTick);
      }
   }
}
