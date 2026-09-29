package dev.silvergold.simulatedcoasters.item;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSnap;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlacementCollision;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartSpawner;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTangentOrientation;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CoasterCartItem extends Item {
   public CoasterCartItem(Properties properties) {
      super(properties);
   }

   public static void clearLinkSelection(ItemStack stack) {
      CoasterCartLinkSelection.clearLinkTarget(stack);
   }

   public static boolean hasLinkSelection(ItemStack stack) {
      return CoasterCartLinkSelection.hasLinkTarget(stack);
   }

   public boolean isFoil(ItemStack stack) {
      return CoasterCartLinkSelection.hasLinkTarget(stack);
   }

   public InteractionResult useOn(UseOnContext context) {
      Level level = context.getLevel();
      Player player = context.getPlayer();
      if (player != null && context.getHand() != InteractionHand.OFF_HAND) {
         ItemStack stack = context.getItemInHand();
         BlockPos clicked = context.getClickedPos();
         if (player.isShiftKeyDown() && hasLinkSelection(stack)) {
            clearLinkSelection(stack);
            player.displayClientMessage(Component.translatable("simulatedcoasters.cart.link_cleared").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.SUCCESS;
         } else {
            SubLevel clickedCart = CoasterCartResolve.findCartSubLevelAt(level, clicked);
            if (clickedCart != null) {
               if (!level.isClientSide()) {
                  if (clickedCart instanceof ServerSubLevel serverCart) {
                     CoasterCartTrainLinkConstraint.pruneInvalidLinksForCoupling((ServerLevel)level, serverCart);
                  }

                  CoasterCartLinkSelection.applyLinkTarget(level, stack, clickedCart);
                  SimulatedCoasters.LOGGER.info("[cart-link] selected cart {} for linking", clickedCart.getUniqueId());
                  player.displayClientMessage(Component.translatable("simulatedcoasters.cart.link_selected").withStyle(ChatFormatting.GRAY), true);
               }

               return InteractionResult.SUCCESS;
            } else if (AnchorPeerFakeTracks.isManagedFakeTrack(level, clicked)) {
               return InteractionResult.PASS;
            } else {
               BlockState clickedState = level.getBlockState(clicked);
               boolean onConnectedAnchor = clickedState.getBlock() instanceof CoasterAnchorpointBlock
                  && CoasterCartMidTrackPlacement.isAcceptedAnchor(level, clicked);
               if (onConnectedAnchor) {
                  return this.tryPlaceOnTrack(context, level, player, stack, clicked);
               } else {
                  SubLevel hostSub = CoasterCartResolve.findSubLevelAt(level, clicked);
                  if (hostSub != null) {
                     return this.tryPlaceOnSubLevel(context, level, player, stack, hostSub);
                  } else {
                     if (!level.isClientSide()) {
                        player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.must_place_on_track_or_sublevel"), true);
                     }

                     return InteractionResult.FAIL;
                  }
               }
            }
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   private InteractionResult tryPlaceOnSubLevel(UseOnContext context, Level level, Player player, ItemStack stack, SubLevel hostSub) {
      if (CoasterCartResolve.isCartSubLevel(hostSub)) {
         if (!level.isClientSide()) {
            player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.sublevel_already_has_cart"), true);
         }

         return InteractionResult.FAIL;
      } else if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else {
         if (level instanceof ServerLevel sl && hostSub instanceof ServerSubLevel serverSub) {
            BlockPos clicked = context.getClickedPos();
            Direction face = context.getClickedFace();
            BlockPos placePos = clicked.relative(face);
            ServerSubLevel cart = CoasterCartSpawner.retrofitSubLevelAsCart(serverSub, level, placePos);
            if (cart == null) {
               player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.bogie.no_room_for_placement"), true);
               return InteractionResult.FAIL;
            }

            CoasterCartTrainLinkConstraint.tryLinkFromItemSelection(sl, cart, stack, player instanceof ServerPlayer sp ? sp : null);
            if (!player.getAbilities().instabuild) {
               stack.shrink(1);
            }

            return InteractionResult.CONSUME;
         }

         return InteractionResult.PASS;
      }
   }

   private InteractionResult tryPlaceOnTrack(UseOnContext context, Level level, Player player, ItemStack stack, BlockPos clicked) {
      if (level.isClientSide()) {
         return InteractionResult.SUCCESS;
      } else if (level instanceof ServerLevel sl) {
         CoasterCartMidTrackPlacement.PlacementPose pose = CoasterCartMidTrackPlacement.resolveFromAnchor(
            sl, player, clicked, player.getLookAngle(), context.getClickLocation(), null
         );
         if (pose == null) {
            return InteractionResult.FAIL;
         } else {
            boolean linkCoupling;
            boolean var10000;
            label42: {
               linkCoupling = CoasterCartLinkPlacement.isLinkCouplingPlacement(stack);
               if (player instanceof ServerPlayer sp && CoasterCartLinkSnap.isSuppressLinkSnap(sp.getUUID())) {
                  var10000 = true;
                  break label42;
               }

               var10000 = false;
            }

            boolean suppressLinkSnap = var10000;
            if (CoasterCartLinkPlacement.rejectsLinkPlacement(sl, stack, pose, null, suppressLinkSnap)) {
               player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.link_too_far"), true);
               return InteractionResult.FAIL;
            } else {
               pose = CoasterCartLinkPlacement.applyLinkSnapIfSelected(sl, stack, pose, null, suppressLinkSnap);
               if (CoasterCartPlacementCollision.hasSubLevelObstruction(sl, pose.plotOriginWorld(), pose.orientation())) {
                  player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.bogie.no_room_for_placement"), true);
                  return InteractionResult.FAIL;
               } else {
                  CoasterPathGraph graph = CoasterPathGraphManager.get(sl);
                  CoasterPathTangentOrientation.alignConnectedComponents(sl, graph);
                  ServerSubLevel spawned = CoasterCartLinkPlacement.spawnPlacedCart(sl, pose, linkCoupling);
                  if (spawned == null) {
                     return InteractionResult.FAIL;
                  } else {
                     CoasterCartTrainLinkConstraint.tryLinkFromItemSelection(sl, spawned, stack, player instanceof ServerPlayer sp ? sp : null);
                     if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                     }

                     return InteractionResult.CONSUME;
                  }
               }
            }
         }
      } else {
         return InteractionResult.PASS;
      }
   }
}
