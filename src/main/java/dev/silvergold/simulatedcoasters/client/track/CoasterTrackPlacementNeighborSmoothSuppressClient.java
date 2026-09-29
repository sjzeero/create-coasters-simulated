package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllKeys;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacementNeighborSmoothSuppressPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public final class CoasterTrackPlacementNeighborSmoothSuppressClient {
   private static boolean lastSentSuppress;

   private CoasterTrackPlacementNeighborSmoothSuppressClient() {
   }

   public static boolean isSuppressNeighborSmoothing() {
      return AllKeys.altDown();
   }

   public static void clientTick(Minecraft mc) {
      if (mc.player == null) {
         syncSuppress(false);
      } else if (!tracksPlacementContext(mc)) {
         syncSuppress(false);
      } else {
         syncSuppress(isSuppressNeighborSmoothing());
      }
   }

   private static boolean tracksPlacementContext(Minecraft mc) {
      ItemStack main = mc.player.getMainHandItem();
      if (main.getItem() instanceof CoasterTrackItem) {
         return true;
      } else {
         ItemStack off = mc.player.getOffhandItem();
         return off.getItem() instanceof CoasterTrackItem;
      }
   }

   private static void syncSuppress(boolean suppress) {
      if (suppress != lastSentSuppress) {
         lastSentSuppress = suppress;
         PacketDistributor.sendToServer(new CoasterTrackPlacementNeighborSmoothSuppressPayload(suppress), new CustomPacketPayload[0]);
      }
   }
}
