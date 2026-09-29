package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleClientSounds;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

public class CoasterTrackItem extends Item {
   public CoasterTrackItem(Properties properties) {
      super(properties);
   }

   public static void clearPendingConnection(ItemStack stack) {
      stack.remove(AllDataComponents.TRACK_CONNECTING_FROM);
      stack.remove(AllDataComponents.TRACK_EXTENDED_CURVE);
      CoasterTrackPlacement.clearAnchorFirstSelection(stack);
   }

   public static boolean hasPendingConnection(ItemStack stack) {
      return stack.getItem() instanceof CoasterTrackItem
         && (stack.has(AllDataComponents.TRACK_CONNECTING_FROM) || CoasterTrackPlacement.hasAnchorFirstSelection(stack));
   }

   public boolean isFoil(ItemStack stack) {
      return CoasterTrackPlacement.hasAnchorFirstSelection(stack);
   }

   public InteractionResult useOn(UseOnContext context) {
      ItemStack stack = context.getItemInHand();
      BlockPos pos = context.getClickedPos();
      Level level = context.getLevel();
      BlockState state = level.getBlockState(pos);
      Player player = context.getPlayer();
      if (player == null) {
         return InteractionResult.PASS;
      } else if (context.getHand() == InteractionHand.OFF_HAND) {
         return InteractionResult.PASS;
      } else {
         boolean pendingSecondClick = this.isFoil(stack);
         if (!pendingSecondClick) {
            if (state.getBlock() instanceof TrackBlock track && track.getTrackAxes(level, pos, state).size() > 1) {
               if (!level.isClientSide) {
                  player.displayClientMessage(CreateLang.translateDirect("track.junction_start", new Object[0]).withStyle(ChatFormatting.RED), true);
               }

               return InteractionResult.SUCCESS;
            }

            if (level.getBlockEntity(pos) instanceof TrackBlockEntity tbe && tbe.isTilted()) {
               if (!level.isClientSide) {
                  player.displayClientMessage(CreateLang.translateDirect("track.turn_start", new Object[0]).withStyle(ChatFormatting.RED), true);
               }

               return InteractionResult.SUCCESS;
            }

            if (state.getBlock() instanceof CoasterAnchorpointBlock) {
               CoasterTrackPlacement.applyAnchorFirstSelection(level, stack, pos, player, context.getHand());
               if (level.isClientSide()) {
                  PacketDistributor.sendToServer(new CoasterTrackAnchorFirstFromCurvePayload(pos), new CustomPacketPayload[0]);
               }

               return InteractionResult.SUCCESS;
            } else {
               player.displayClientMessage(Component.translatable("simulatedcoasters.track.must_attach_to_anchors").withStyle(ChatFormatting.RED), true);
               return InteractionResult.FAIL;
            }
         } else if (player.isShiftKeyDown()) {
            clearPendingConnection(stack);
            if (!level.isClientSide()) {
               player.setItemInHand(context.getHand(), stack);
               if (player instanceof ServerPlayer sp) {
                  CoasterTrackHotbarSelectionSync.syncMainHand(sp, context.getHand(), null);
               }
            }

            return InteractionResult.SUCCESS;
         } else {
            stack.remove(AllDataComponents.TRACK_EXTENDED_CURVE);
            BlockPos firstAnchor = CoasterTrackPlacement.readAnchorFirstSelection(stack);
            if (level.isClientSide()) {
               if (!CoasterTrackPlacement.tryConnectSecondAnchorFromCurve(level, player, stack, pos, context.getHand())) {
                  AllSoundEvents.DENY.playFrom(player, 1.0F, 1.0F);
                  return InteractionResult.FAIL;
               } else {
                  playConnectionCommitSound();
                  clearPendingConnection(stack);
                  player.setItemInHand(context.getHand(), stack);
                  return InteractionResult.SUCCESS;
               }
            } else {
               if (player instanceof ServerPlayer sp && firstAnchor != null && !firstAnchor.equals(pos)) {
                  CoasterTrackPlacement.commitSecondAnchorOnServer(sp, context.getHand(), firstAnchor, pos);
               }

               return InteractionResult.SUCCESS;
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   private static void playConnectionCommitSound() {
      BezierHandleClientSounds.playCommitSuccessSound(Minecraft.getInstance());
   }
}
