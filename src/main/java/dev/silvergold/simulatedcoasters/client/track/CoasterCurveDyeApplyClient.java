package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterCurveDyeApplyPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public final class CoasterCurveDyeApplyClient {
   private CoasterCurveDyeApplyClient() {
   }

   public static boolean onUseItemKey(InteractionKeyMappingTriggered event) {
      if (!event.isUseItem()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (!trySendApply(mc)) {
            return false;
         } else {
            event.setCanceled(true);
            event.setSwingHand(true);
            return true;
         }
      }
   }

   private static boolean trySendApply(Minecraft mc) {
      if (mc.player != null && mc.level != null) {
         InteractionHand hand = CoasterCurveDyeOutlineClient.handWithDye(mc.player);
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
                  PacketDistributor.sendToServer(new CoasterCurveDyeApplyPayload(owner, remote, soundPos, hand), new CustomPacketPayload[0]);
                  return true;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }
}
