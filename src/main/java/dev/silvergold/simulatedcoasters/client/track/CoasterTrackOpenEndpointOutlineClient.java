package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.content.trains.track.TrackBlockOutline.BezierPointSelection;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterTrackAnchorFirstFromCurvePayload;
import dev.silvergold.simulatedcoasters.track.CoasterTrackAnchorSecondFromCurvePayload;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class CoasterTrackOpenEndpointOutlineClient {
   private CoasterTrackOpenEndpointOutlineClient() {
   }

   public static boolean onUseItemKey(InteractionKeyMappingTriggered event) {
      if (!event.isUseItem()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            if (event.getHand() != InteractionHand.MAIN_HAND) {
               return false;
            } else if (mc.player.isShiftKeyDown()) {
               return false;
            } else {
               ItemStack stack = mc.player.getMainHandItem();
               if (!(stack.getItem() instanceof CoasterTrackItem)) {
                  return false;
               } else {
                  BlockPos exclude = CoasterTrackPlacement.hasAnchorFirstSelection(stack) ? CoasterTrackPlacement.readAnchorFirstSelection(stack) : null;
                  BlockPos anchor = resolveClosestCurveEndpoint(mc, exclude, true);
                  if (anchor == null) {
                     return false;
                  } else {
                     if (exclude == null) {
                        CoasterTrackPlacement.applyAnchorFirstSelection(mc.level, stack, anchor);
                        PacketDistributor.sendToServer(new CoasterTrackAnchorFirstFromCurvePayload(anchor), new CustomPacketPayload[0]);
                     } else if (CoasterTrackPlacement.tryConnectSecondAnchorFromCurve(mc.level, mc.player, stack, anchor)) {
                        BezierHandleClientSounds.playCommitSuccessSound(mc);
                        CoasterTrackItem.clearPendingConnection(stack);
                        mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                        PacketDistributor.sendToServer(new CoasterTrackAnchorSecondFromCurvePayload(anchor, exclude), new CustomPacketPayload[0]);
                     } else {
                        AllSoundEvents.DENY.playFrom(mc.player, 1.0F, 1.0F);
                     }

                     event.setCanceled(true);
                     event.setSwingHand(true);
                     return true;
                  }
               }
            }
         } else {
            return false;
         }
      }
   }

   @Nullable
   public static BlockPos resolveClosestOpenAnchor(Minecraft mc) {
      ItemStack stack = mc.player == null ? ItemStack.EMPTY : findCoasterTrackStack(mc.player);
      BlockPos exclude = stack != null && CoasterTrackPlacement.hasAnchorFirstSelection(stack) ? CoasterTrackPlacement.readAnchorFirstSelection(stack) : null;
      return resolveClosestCurveEndpoint(mc, exclude, true);
   }

   @Nullable
   public static BlockPos resolveClosestCurveAnchor(Minecraft mc) {
      return resolveClosestCurveEndpoint(mc, null, false);
   }

   @Nullable
   public static HoveredCoasterCurve resolveHoveredCoasterCurve(Minecraft mc) {
      if (mc.player != null && mc.level != null) {
         float pt = AnimationTickHolder.getPartialTicks(mc.level);
         return resolveHoveredCurve(mc, mc.level, pt);
      } else {
         return null;
      }
   }

   public static Vec3 anchorJunctionWorld(Level level, BlockPos anchor, float partialTick) {
      return anchorCenterWorld(level, anchor, partialTick);
   }

   @Nullable
   private static BlockPos resolveClosestCurveEndpoint(Minecraft mc, @Nullable BlockPos excludeAnchor, boolean openEndpointsOnly) {
      if (mc.player != null && mc.level != null) {
         float pt = AnimationTickHolder.getPartialTicks(mc.level);
         HoveredCoasterCurve hovered = resolveHoveredCurve(mc, mc.level, pt);
         if (hovered == null) {
            return null;
         } else {
            BlockPos ep1 = (BlockPos)hovered.bc().bePositions.getFirst();
            BlockPos ep2 = (BlockPos)hovered.bc().bePositions.getSecond();
            if (!openEndpointsOnly || excludeAnchor == null || !excludeAnchor.equals(ep1) && !excludeAnchor.equals(ep2)) {
               BlockPos closest = null;
               double bestDistSq = Double.POSITIVE_INFINITY;

               for (BlockPos ep : new BlockPos[]{ep1, ep2}) {
                  if ((!openEndpointsOnly || isOpenEndpoint(mc.level, ep)) && isCurveEndpointAnchor(mc.level, ep)) {
                     double d = hovered.hoverWorld().distanceToSqr(anchorCenterWorld(mc.level, ep, pt));
                     if (d < bestDistSq) {
                        bestDistSq = d;
                        closest = ep;
                     }
                  }
               }

               return closest;
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static boolean isCurveEndpointAnchor(Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity;
   }

   private static boolean isOpenEndpoint(Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity be ? be.legCount() < 2 : false;
   }

   private static Vec3 anchorCenterWorld(Level level, BlockPos anchor, float partialTick) {
      Vec3 local = CoasterAnchorpointBlockEntity.worldCenter(level, anchor);
      return CoasterPathGraphManager.sublevelWorldPosition(level, anchor, local, (double)partialTick);
   }

   @Nullable
   private static HoveredCoasterCurve resolveHoveredCurve(Minecraft mc, Level level, float pt) {
      if (AnchorPeerCurveHit.isActive()) {
         BezierConnection bc = AnchorPeerCurveHit.curve();
         if (bc != null && CoasterBezierRailFrames.isCoaster(bc)) {
            BlockPos focus = AnchorPeerCurveHit.focusAnchor();
            Vec3 hoverWorld = plotLocalMidToWorld(mc, AnchorPeerCurveHit.vec(), focus, pt);
            return new HoveredCoasterCurve(bc, hoverWorld);
         } else {
            return null;
         }
      } else {
         BezierPointSelection sel = TrackBlockOutline.result;
         if (sel == null) {
            return null;
         } else {
            BezierConnection bc = TrackOutlineBezierAccess.primaryBezierForSelection(sel);
            return bc != null && CoasterBezierRailFrames.isCoaster(bc) ? new HoveredCoasterCurve(bc, sel.vec()) : null;
         }
      }
   }

   private static Vec3 plotLocalMidToWorld(Minecraft mc, Vec3 plotLocalMid, BlockPos anchorPos, float pt) {
      ClientSubLevel sub = (ClientSubLevel)Sable.HELPER.getContaining(mc.level, anchorPos);
      return sub == null ? plotLocalMid : ((Pose3d)sub.renderPose(pt)).transformPosition(plotLocalMid);
   }

   @Nullable
   private static ItemStack findCoasterTrackStack(LocalPlayer player) {
      ItemStack main = player.getMainHandItem();
      if (main.getItem() instanceof CoasterTrackItem) {
         return main;
      } else {
         ItemStack off = player.getOffhandItem();
         return off.getItem() instanceof CoasterTrackItem ? off : null;
      }
   }
}
