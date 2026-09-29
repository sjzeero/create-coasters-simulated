package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartChainLinkOutlineClient;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import org.jetbrains.annotations.Nullable;

public final class CoasterCartChainLinkInteraction {
   private CoasterCartChainLinkInteraction() {
   }

   public static boolean holdingChain(ItemStack stack) {
      return stack.is(Items.CHAIN);
   }

   public static void onRightClickBlock(RightClickBlock event) {
      if (event.getHand() == InteractionHand.MAIN_HAND) {
         Player player = event.getEntity();
         ItemStack stack = player.getMainHandItem();
         if (holdingChain(stack)) {
            Level level = player.level();
            if (player.isShiftKeyDown() && CoasterCartLinkSelection.hasLinkTarget(stack)) {
               CoasterCartLinkSelection.clearLinkTarget(stack);
               if (!level.isClientSide()) {
                  player.displayClientMessage(Component.translatable("simulatedcoasters.cart.link_cleared").withStyle(ChatFormatting.GRAY), true);
               }

               event.setCanceled(true);
            } else {
               SubLevel clickedCart = CoasterCartResolve.findCartSubLevelAt(level, event.getPos());
               if (clickedCart != null) {
                  event.setCanceled(true);
                  if (level.isClientSide()) {
                     if (FMLEnvironment.dist == Dist.CLIENT) {
                        CoasterCartChainLinkInteraction.ClientCoupleFeedback.playDenyIfBlocked(stack, clickedCart);
                     }
                  } else if (player instanceof ServerPlayer serverPlayer) {
                     if (level instanceof ServerLevel sl) {
                        if (clickedCart instanceof ServerSubLevel serverCart) {
                           handleLinkClick(sl, serverPlayer, stack, serverCart);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void handleLinkClick(ServerLevel level, ServerPlayer player, ItemStack stack, ServerSubLevel clickedCart) {
      UUID selectedId = CoasterCartLinkSelection.readLinkTarget(stack);
      if (selectedId == null) {
         CoasterCartTrainLinkConstraint.pruneInvalidLinksForCoupling(level, clickedCart);
         CoasterCartLinkSelection.applyLinkTarget(level, stack, clickedCart);
         player.displayClientMessage(Component.translatable("simulatedcoasters.cart.link_selected_chain").withStyle(ChatFormatting.GRAY), true);
         SimulatedCoasters.LOGGER.info("[cart-link] chain selected cart {} for coupling", clickedCart.getUniqueId());
      } else if (selectedId.equals(clickedCart.getUniqueId())) {
         CoasterCartLinkSelection.applyLinkTarget(level, stack, clickedCart);
         player.displayClientMessage(Component.translatable("simulatedcoasters.cart.link_selected_chain").withStyle(ChatFormatting.GRAY), true);
      } else {
         ServerSubLevel partner = CoasterCartResolve.findCartById(level, selectedId);
         if (partner != null && !partner.isRemoved() && partner != clickedCart) {
            double rawDistance = CoasterCartLinkPlacement.bearingCenterDistance(partner, clickedCart, null);
            if (!Double.isNaN(rawDistance) && !CoasterCartLinkPlacement.rejectsLinkDistance(rawDistance)) {
               if (CoasterCartTrainLinkConstraint.coupleRejectLangKey(partner, clickedCart) == null) {
                  double distance = CoasterCartLinkPlacement.resolveChainLinkDistance(
                     level, partner, clickedCart, null, CoasterCartLinkSnap.isSuppressLinkSnap(player.getUUID())
                  );
                  CoasterCartTrainLinkConstraint.pruneInvalidLinksForCoupling(level, partner);
                  CoasterCartTrainLinkConstraint.pruneInvalidLinksForCoupling(level, clickedCart);
                  if (CoasterCartTrainLinkConstraint.linkCarts(level, partner, clickedCart, distance)) {
                     CoasterCartLinkSelection.clearLinkTarget(stack);
                     player.displayClientMessage(Component.translatable("simulatedcoasters.cart.link_coupled").withStyle(ChatFormatting.GRAY), true);
                  }
               }
            }
         } else {
            CoasterCartLinkSelection.clearLinkTarget(stack);
            SimulatedCoasters.LOGGER.info("[cart-link] chain partner {} missing; selection cleared", selectedId);
            player.displayClientMessage(Component.translatable("simulatedcoasters.cart.link_cleared").withStyle(ChatFormatting.GRAY), true);
         }
      }
   }

   @Nullable
   public static SubLevel resolveHoveredCart(Level level, HitResult hitResult) {
      if (hitResult instanceof BlockHitResult bhr && bhr.getType() == Type.BLOCK) {
         SubLevel cart = CoasterCartResolve.findCartSubLevelAt(level, bhr.getBlockPos());
         if (cart != null && !cart.isRemoved()) {
            return cart;
         }

         return null;
      }

      return null;
   }

   private static final class ClientCoupleFeedback {
      static void playDenyIfBlocked(ItemStack chain, SubLevel clickedCart) {
         CoasterCartChainLinkOutlineClient.playDenyIfCoupleBlocked(chain, clickedCart);
      }
   }
}
