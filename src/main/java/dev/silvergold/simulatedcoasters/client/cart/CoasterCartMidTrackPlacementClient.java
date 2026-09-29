package dev.silvergold.simulatedcoasters.client.cart;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacePayload;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlacementCollision;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class CoasterCartMidTrackPlacementClient {
   private CoasterCartMidTrackPlacementClient() {
   }

   public static boolean onUseItemKey(InteractionKeyMappingTriggered event) {
      if (!event.isUseItem()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            InteractionHand hand = handWithCoasterCart(mc.player);
            if (hand != null && midSpanCurveHover()) {
               ItemStack stack = mc.player.getItemInHand(hand);
               Vec3 look = mc.player.getLookAngle();
               float partialTick = AnimationTickHolder.getPartialTicks(mc.level);
               CoasterCartMidTrackPlacement.PlacementPose pose = resolveHoverPose(mc, look, (double)partialTick);
               if (pose == null) {
                  return false;
               } else if (CoasterCartLinkPlacement.rejectsLinkPlacement(
                  mc.level, stack, pose, (double)partialTick, CoasterCartLinkSnapClient.isSuppressLinkSnap()
               )) {
                  mc.player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.link_too_far"), true);
                  return false;
               } else {
                  pose = CoasterCartLinkPlacement.applyLinkSnapIfSelected(
                     mc.level, stack, pose, (double)partialTick, CoasterCartLinkSnapClient.isSuppressLinkSnap()
                  );
                  if (CoasterCartPlacementCollision.hasSubLevelObstruction(mc.level, pose.plotOriginWorld(), pose.orientation(), partialTick)) {
                     mc.player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.bogie.no_room_for_placement"), true);
                     return false;
                  } else {
                     PacketDistributor.sendToServer(
                        new CoasterCartMidTrackPlacePayload(midSpanRasterHint(), look, hand, CoasterCartLinkSnapClient.isSuppressLinkSnap()),
                        new CustomPacketPayload[0]
                     );
                     event.setCanceled(true);
                     event.setSwingHand(true);
                     return true;
                  }
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   public static boolean holdingCoasterCart(Minecraft mc) {
      return mc.player != null && handWithCoasterCart(mc.player) != null;
   }

   public static boolean midSpanCurveHover() {
      if (!AnchorPeerCurveHit.isActive()) {
         return false;
      } else {
         BezierConnection bc = AnchorPeerCurveHit.curve();
         return bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id);
      }
   }

   @Nullable
   public static CoasterCartMidTrackPlacement.PlacementPose resolveHoverPose(Minecraft mc, Vec3 look, @Nullable Double partialTick) {
      if (mc.level != null && mc.player != null) {
         if (midSpanCurveHover()) {
            BezierConnection bc = AnchorPeerCurveHit.curve();
            if (bc != null) {
               CoasterCartMidTrackPlacement.PlacementPose pose = CoasterCartMidTrackPlacement.resolveForCurve(mc.level, mc.player, bc, look, partialTick);
               if (pose != null) {
                  return pose;
               }
            }
         }

         return CoasterCartMidTrackPlacement.resolveMidSpan(mc.level, mc.player, look, partialTick, midSpanRasterHint());
      } else {
         return null;
      }
   }

   @Nullable
   public static BlockPos midSpanRasterHint() {
      BlockPos raster = AnchorPeerCurveHit.rasterBlockPos();
      return raster != null ? raster : AnchorPeerCurveHit.focusAnchor();
   }

   public static boolean anchorHover() {
      return anchorHoverPos() != null;
   }

   @Nullable
   public static BlockPos anchorHoverPos() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.hitResult != null && mc.hitResult.getType() == Type.BLOCK) {
         BlockPos pos = ((BlockHitResult)mc.hitResult).getBlockPos();
         return CoasterCartMidTrackPlacement.isAcceptedAnchor(mc.level, pos) ? pos : null;
      } else {
         return null;
      }
   }

   public static boolean placementPreviewActive() {
      return midSpanCurveHover() || anchorHover();
   }

   @Nullable
   private static InteractionHand handWithCoasterCart(Player player) {
      CoasterCartItem bearing = (CoasterCartItem)SimulatedCoasters.COASTER_CART.get();
      if (player.getMainHandItem().is(bearing)) {
         return InteractionHand.MAIN_HAND;
      } else {
         return player.getOffhandItem().is(bearing) ? InteractionHand.OFF_HAND : null;
      }
   }
}
