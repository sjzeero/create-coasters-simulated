package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterCurveUndyeApplyPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public final class CoasterCurveUndyeApplyClient {
   private CoasterCurveUndyeApplyClient() {
   }

   public static boolean onUseItemKey(InteractionKeyMappingTriggered event) {
      if (!event.isUseItem()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (!trySendClear(mc)) {
            return false;
         } else {
            event.setCanceled(true);
            event.setSwingHand(true);
            return true;
         }
      }
   }

   private static boolean trySendClear(Minecraft mc) {
      if (mc.player == null || mc.level == null) {
         return false;
      } else if (CoasterCurveDyeOutlineClient.heldDyeItem(mc.player) != null) {
         return false;
      } else {
         InteractionHand hand = handWithAxe(mc.player);
         if (hand == null) {
            return false;
         } else {
            BezierConnection bc = CoasterCurveDyeOutlineClient.resolvePreviewHostedPrimary(mc);
            if (bc != null && AnchorPeerCurveOutlineSupport.hasSaneOutlineBounds(bc)) {
               Vec3 soundPos = CoasterCurveDyeOutlineClient.resolvePreviewWorldHit(mc);
               if (soundPos == null) {
                  return false;
               } else {
                  BlockPos owner = (BlockPos)bc.bePositions.getFirst();
                  BlockPos remote = (BlockPos)bc.bePositions.getSecond();
                  PacketDistributor.sendToServer(new CoasterCurveUndyeApplyPayload(owner, remote, soundPos, hand), new CustomPacketPayload[0]);
                  return true;
               }
            } else {
               return false;
            }
         }
      }
   }

   private static InteractionHand handWithAxe(Player player) {
      ItemStack main = player.getMainHandItem();
      if (main.getItem() instanceof AxeItem) {
         return InteractionHand.MAIN_HAND;
      } else {
         ItemStack off = player.getOffhandItem();
         return off.getItem() instanceof AxeItem ? InteractionHand.OFF_HAND : null;
      }
   }
}
