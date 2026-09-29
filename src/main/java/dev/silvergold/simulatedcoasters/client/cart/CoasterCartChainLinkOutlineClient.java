package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleClientSounds;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import java.util.Locale;
import java.util.UUID;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartChainLinkOutlineClient {
   private static final String OUTLINER_KEY_SELECTED = "coaster_cart_chain_link_selected";
   private static final String OUTLINER_KEY_HOVER = "coaster_cart_chain_link_hover";
   private static final int BLOCKED_OUTLINE_COLOR = 16729927;
   private static boolean linkActionBarShowing;
   private static String linkActionBarKey = "";
   private static boolean frameLinkingToHover;
   private static boolean frameBlocked;
   @Nullable
   private static String frameStructuralRejectKey;
   private static double frameRawDistance = Double.NaN;
   private static double frameLinkDistance = Double.NaN;
   @Nullable
   private static Vec3 frameSelectedCenter;
   @Nullable
   private static Vec3 frameHoverCenter;

   private CoasterCartChainLinkOutlineClient() {
   }

   public static void renderFrame(Minecraft mc, float partialTick) {
      frameLinkingToHover = false;
      frameBlocked = false;
      frameStructuralRejectKey = null;
      frameRawDistance = Double.NaN;
      frameLinkDistance = Double.NaN;
      frameSelectedCenter = null;
      frameHoverCenter = null;
      Outliner outliner = Outliner.getInstance();
      if (mc.player != null && mc.level != null && !mc.options.hideGui) {
         ItemStack chain = mc.player.getMainHandItem();
         if (chain.is(Items.CHAIN) && CoasterCartLinkSelection.hasLinkTarget(chain)) {
            UUID selectedId = CoasterCartLinkSelection.readLinkTarget(chain);
            if (selectedId == null) {
               clear(outliner);
            } else {
               SubLevel selectedCart = CoasterCartResolve.findCartById(mc.level, selectedId);
               if (selectedCart != null && !selectedCart.isRemoved()) {
                  SubLevel hoverCart = mc.hitResult == null ? null : CoasterCartChainLinkInteraction.resolveHoveredCart(mc.level, mc.hitResult);
                  boolean linkingToHover = hoverCart != null && !hoverCart.getUniqueId().equals(selectedId);
                  double linkDistance = Double.NaN;
                  double rawDistance = Double.NaN;
                  boolean distanceRejected = false;
                  String structuralRejectKey = null;
                  Vec3 selectedCenter = null;
                  Vec3 hoverCenter = null;
                  if (linkingToHover) {
                     rawDistance = CoasterCartLinkPlacement.bearingCenterDistance(selectedCart, hoverCart, (double)partialTick);
                     linkDistance = CoasterCartLinkPlacement.resolveChainLinkDistance(
                        mc.level, selectedCart, hoverCart, (double)partialTick, CoasterCartLinkSnapClient.isSuppressLinkSnap()
                     );
                     distanceRejected = Double.isNaN(rawDistance) || CoasterCartLinkPlacement.rejectsLinkDistance(rawDistance);
                     if (!distanceRejected) {
                        structuralRejectKey = CoasterCartTrainLinkConstraint.coupleRejectLangKey(selectedCart, hoverCart);
                     }

                     selectedCenter = CoasterCartLinkPlacement.partnerBearingCenterWorld(selectedCart, (double)partialTick);
                     hoverCenter = CoasterCartLinkPlacement.partnerBearingCenterWorld(hoverCart, (double)partialTick);
                  }

                  boolean blocked = distanceRejected || structuralRejectKey != null;
                  int selectedColor = blocked ? 16729927 : 1940687;
                  drawCartOutline(outliner, "coaster_cart_chain_link_selected", selectedCart, partialTick, selectedColor);
                  if (linkingToHover && hoverCart != null) {
                     int hoverColor = blocked ? 16729927 : 1940687;
                     drawCartOutline(outliner, "coaster_cart_chain_link_hover", hoverCart, partialTick, hoverColor);
                  } else {
                     CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_chain_link_hover");
                  }

                  frameLinkingToHover = linkingToHover;
                  frameBlocked = blocked;
                  frameStructuralRejectKey = structuralRejectKey;
                  frameRawDistance = rawDistance;
                  frameLinkDistance = linkDistance;
                  frameSelectedCenter = selectedCenter;
                  frameHoverCenter = hoverCenter;
               } else {
                  clear(outliner);
               }
            }
         } else {
            clear(outliner);
         }
      } else {
         clear(outliner);
      }
   }

   public static void clientTick(Minecraft mc) {
      if (mc.player == null) {
         endLinkActionBarIfShowing(null);
      } else {
         ItemStack chain = mc.player.getMainHandItem();
         if (!chain.is(Items.CHAIN) || !CoasterCartLinkSelection.hasLinkTarget(chain)) {
            endLinkActionBarIfShowing(mc.player);
         } else if (!frameLinkingToHover) {
            endLinkActionBarIfShowing(mc.player);
         } else {
            if (!frameBlocked && frameSelectedCenter != null && frameHoverCenter != null) {
               ClientLevel key = mc.level;
               if (key instanceof ClientLevel) {
                  CoasterCartLinkPreviewParticles.spawnSpan(key, frameSelectedCenter, frameHoverCenter);
               }
            }

            Component message;
            String key;
            if (frameStructuralRejectKey != null) {
               message = CoasterTrackPlacement.placementErrorMessage(frameStructuralRejectKey);
               key = frameStructuralRejectKey;
            } else if (frameBlocked) {
               message = CoasterCartLinkPlacement.linkDistanceRejectActionBarComponent(frameRawDistance);
               key = CoasterCartLinkPlacement.isLinkDistanceTooClose(frameRawDistance) ? "too_close" : "too_far";
            } else {
               message = CoasterCartLinkPlacement.linkDistanceActionBarComponent(frameLinkDistance);
               key = "ok:" + String.format(Locale.ROOT, "%.2f", frameLinkDistance);
            }

            setLinkActionBar(mc.player, message, key);
         }
      }
   }

   public static void playDenyIfCoupleBlocked(ItemStack chain, SubLevel clickedCart) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && clickedCart != null) {
         UUID selectedId = CoasterCartLinkSelection.readLinkTarget(chain);
         if (selectedId != null && !selectedId.equals(clickedCart.getUniqueId())) {
            SubLevel partner = CoasterCartResolve.findCartById(mc.level, selectedId);
            if (partner != null && !partner.isRemoved()) {
               double rawDistance = CoasterCartLinkPlacement.bearingCenterDistance(partner, clickedCart, null);
               if (Double.isNaN(rawDistance)
                  || CoasterCartLinkPlacement.rejectsLinkDistance(rawDistance)
                  || CoasterCartTrainLinkConstraint.coupleRejectLangKey(partner, clickedCart) != null) {
                  BezierHandleClientSounds.playCreateDeny();
               }
            }
         }
      }
   }

   private static void setLinkActionBar(LocalPlayer player, Component message, String key) {
      if (!linkActionBarShowing || !key.equals(linkActionBarKey)) {
         player.displayClientMessage(message, true);
         linkActionBarKey = key;
         linkActionBarShowing = true;
      }
   }

   private static void endLinkActionBarIfShowing(@Nullable LocalPlayer player) {
      if (player != null && linkActionBarShowing) {
         player.displayClientMessage(Component.empty(), true);
         linkActionBarShowing = false;
         linkActionBarKey = "";
      }
   }

   private static void drawCartOutline(Outliner outliner, String key, SubLevel cart, float partialTick, int color) {
      Pose3d pose = new Pose3d();
      cart.lastPose().lerp(cart.logicalPose(), (double)partialTick, pose);
      Vec3 plotOrigin = CoasterCartMidTrackPlacement.plotOriginFromPlacedCart(cart, pose);
      if (plotOrigin == null) {
         CoasterCartOutlineRenderer.clear(outliner, key);
      } else {
         CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(outliner, key, plotOrigin, pose.orientation(), color);
      }
   }

   private static void clear(Outliner outliner) {
      CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_chain_link_selected");
      CoasterCartOutlineRenderer.clear(outliner, "coaster_cart_chain_link_hover");
   }
}
