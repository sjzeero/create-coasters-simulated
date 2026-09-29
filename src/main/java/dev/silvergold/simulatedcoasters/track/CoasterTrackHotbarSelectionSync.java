package dev.silvergold.simulatedcoasters.track;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class CoasterTrackHotbarSelectionSync {
   private CoasterTrackHotbarSelectionSync() {
   }

   public static void syncMainHand(ServerPlayer player, InteractionHand hand, @Nullable BlockPos anchorFirstOrNull) {
      ItemStack stack = player.getItemInHand(hand);
      if (stack.getItem() instanceof CoasterTrackItem) {
         player.setItemInHand(hand, stack);
         int slot = player.getInventory().selected;
         if (hand != InteractionHand.OFF_HAND) {
            long anchorLong = anchorFirstOrNull == null ? 0L : anchorFirstOrNull.asLong();
            CoasterTrackHotbarSelectionSyncPayload.broadcast(player, slot, anchorLong);
         }
      }
   }
}
