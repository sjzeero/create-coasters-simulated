package dev.silvergold.simulatedcoasters.track.anchor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

public final class ManagedFakeTrackInteractionBlock {
   private ManagedFakeTrackInteractionBlock() {
   }

   public static boolean isBlockedTarget(Level level, BlockPos pos) {
      return AnchorPeerFakeTracks.isManagedFakeTrack(level, pos);
   }

   public static void onRightClickBlock(RightClickBlock event) {
      if (isBlockedTarget(event.getLevel(), event.getPos())) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.FAIL);
      }
   }

   public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
      if (isBlockedTarget(event.getLevel(), event.getPos())) {
         event.cancelWithResult(ItemInteractionResult.FAIL);
      }
   }
}
