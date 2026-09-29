package dev.silvergold.simulatedcoasters.balloon;

import com.simibubi.create.AllItems;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

public final class BalloonWrenchPickupInteraction {
   private BalloonWrenchPickupInteraction() {
   }

   public static void onRightClickBlock(RightClickBlock event) {
      Player player = event.getEntity();
      if (player.isShiftKeyDown()) {
         ItemStack stack = player.getItemInHand(event.getHand());
         if (AllItems.WRENCH.isIn(stack)) {
            Level level = event.getLevel();
            if (BalloonResolve.targetsBalloonCell(level, event.getPos())) {
               event.setCanceled(true);
               event.setCancellationResult(InteractionResult.SUCCESS);
               if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                  if (level instanceof ServerLevel serverLevel) {
                     ServerSubLevel balloon = BalloonResolve.findBalloonAt(serverLevel, event.getPos());
                     if (balloon != null) {
                        if (BalloonRemoval.safelyRemove(serverLevel, balloon)) {
                           if (!serverPlayer.getAbilities().instabuild) {
                              ItemStack balloonItem = new ItemStack((ItemLike)SimulatedCoasters.RED_BALLOON.get());
                              if (!serverPlayer.getInventory().add(balloonItem)) {
                                 serverPlayer.drop(balloonItem, false);
                              }
                           }

                           serverPlayer.swing(event.getHand(), true);
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
