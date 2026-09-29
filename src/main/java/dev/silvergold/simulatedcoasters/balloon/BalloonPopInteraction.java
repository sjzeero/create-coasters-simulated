package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent.UsePhase;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import org.jetbrains.annotations.Nullable;

public final class BalloonPopInteraction {
   private BalloonPopInteraction() {
   }

   public static void onLeftClickBlock(LeftClickBlock event) {
      if (event.getHand() == InteractionHand.MAIN_HAND) {
         if (BalloonResolve.targetsBalloonCell(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            if (event.getLevel() instanceof ServerLevel serverLevel) {
               popAt(serverLevel, event.getPos());
            }
         }
      }
   }

   public static void onRightClickBlock(RightClickBlock event) {
      if (blockBuildOnBalloon(event.getLevel(), event.getPos(), event.getFace(), event.getItemStack())) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.FAIL);
         if (event.getLevel() instanceof ServerLevel serverLevel) {
            popForBuildAttempt(serverLevel, event.getPos(), event.getFace());
         }
      }
   }

   public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
      if (event.getUsePhase() == UsePhase.ITEM_BEFORE_BLOCK) {
         if (blockBuildOnBalloon(event.getLevel(), event.getPos(), event.getFace(), event.getItemStack())) {
            event.cancelWithResult(ItemInteractionResult.FAIL);
            if (event.getLevel() instanceof ServerLevel serverLevel) {
               popForBuildAttempt(serverLevel, event.getPos(), event.getFace());
            }
         }
      }
   }

   public static void onBlockPlaced(EntityPlaceEvent event) {
      if (event.getLevel() instanceof ServerLevel level) {
         ServerSubLevel balloon = BalloonResolve.findBalloonAt(level, event.getPos());
         if (balloon != null) {
            event.setCanceled(true);
            BalloonRemoval.pop(level, balloon);
         }
      }
   }

   public static boolean blockBuildOnBalloon(Level level, BlockHitResult hit, ItemStack stack) {
      return blockBuildOnBalloon(level, hit.getBlockPos(), hit.getDirection(), stack);
   }

   public static void handleBuildOnBalloon(ServerLevel level, BlockHitResult hit) {
      popForBuildAttempt(level, hit.getBlockPos(), hit.getDirection());
   }

   public static boolean blockBuildOnBalloon(Level level, BlockPos clicked, @Nullable Direction face, ItemStack stack) {
      if (!attemptsToBuildOn(stack)) {
         return false;
      } else {
         return BalloonResolve.targetsBalloonCell(level, clicked) ? true : face != null && BalloonResolve.targetsBalloonCell(level, clicked.relative(face));
      }
   }

   private static void popAt(ServerLevel level, BlockPos pos) {
      ServerSubLevel balloon = BalloonResolve.findBalloonAt(level, pos);
      if (balloon != null) {
         BalloonRemoval.pop(level, balloon);
      }
   }

   private static void popForBuildAttempt(ServerLevel level, BlockPos clicked, @Nullable Direction face) {
      ServerSubLevel balloon = findBalloonForBuildAttempt(level, clicked, face);
      if (balloon != null) {
         BalloonRemoval.pop(level, balloon);
      }
   }

   @Nullable
   private static ServerSubLevel findBalloonForBuildAttempt(ServerLevel level, BlockPos clicked, @Nullable Direction face) {
      ServerSubLevel balloon = BalloonResolve.findBalloonAt(level, clicked);
      if (balloon != null) {
         return balloon;
      } else {
         return face == null ? null : BalloonResolve.findBalloonAt(level, clicked.relative(face));
      }
   }

   private static boolean attemptsToBuildOn(ItemStack stack) {
      return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
   }
}
