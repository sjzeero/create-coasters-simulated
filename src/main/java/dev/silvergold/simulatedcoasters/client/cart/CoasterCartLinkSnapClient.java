package dev.silvergold.simulatedcoasters.client.cart;

import com.simibubi.create.AllKeys;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSnapSuppressPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartLinkSnapClient {
   private static boolean lastSentSuppress;

   private CoasterCartLinkSnapClient() {
   }

   public static boolean isSuppressLinkSnap() {
      return AllKeys.altDown();
   }

   public static void clientTick(Minecraft mc) {
      if (mc.player == null) {
         syncSuppress(false);
      } else if (!tracksLinkSnapContext(mc)) {
         syncSuppress(false);
      } else {
         syncSuppress(isSuppressLinkSnap());
      }
   }

   private static boolean tracksLinkSnapContext(Minecraft mc) {
      ItemStack main = mc.player.getMainHandItem();
      if (CoasterCartChainLinkInteraction.holdingChain(main)) {
         return true;
      } else if (main.is(SimulatedCoasters.COASTER_CART)) {
         return CoasterCartLinkSelection.hasLinkTarget(main)
            ? true
            : CoasterCartMidTrackPlacementClient.holdingCoasterCart(mc) && CoasterCartMidTrackPlacementClient.placementPreviewActive();
      } else {
         return false;
      }
   }

   private static void syncSuppress(boolean suppress) {
      if (suppress != lastSentSuppress) {
         lastSentSuppress = suppress;
         PacketDistributor.sendToServer(new CoasterCartLinkSnapSuppressPayload(suppress), new CustomPacketPayload[0]);
      }
   }
}
