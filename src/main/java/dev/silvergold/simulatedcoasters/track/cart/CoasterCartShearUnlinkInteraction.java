package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

public final class CoasterCartShearUnlinkInteraction {
   private CoasterCartShearUnlinkInteraction() {
   }

   public static boolean holdingShears(ItemStack stack) {
      return stack.is(Items.SHEARS);
   }

   public static void onRightClickBlock(RightClickBlock event) {
      if (event.getHand() == InteractionHand.MAIN_HAND) {
         Player player = event.getEntity();
         ItemStack stack = player.getMainHandItem();
         if (holdingShears(stack)) {
            Level level = player.level();
            SubLevel clickedCart = CoasterCartResolve.findCartSubLevelAt(level, event.getPos());
            if (clickedCart != null) {
               event.setCanceled(true);
               if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                  if (level instanceof ServerLevel sl) {
                     if (clickedCart instanceof ServerSubLevel serverCart) {
                        if (CoasterCartTrainLinkConstraint.clearAllLinks(sl, serverCart)) {
                           if (!serverPlayer.getAbilities().instabuild) {
                              stack.hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
                           }

                           serverPlayer.displayClientMessage(Component.translatable("simulatedcoasters.cart.links_cut").withStyle(ChatFormatting.GRAY), true);
                           SimulatedCoasters.LOGGER.info("[cart-link] shears cleared all links on cart {}", serverCart.getUniqueId());
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
