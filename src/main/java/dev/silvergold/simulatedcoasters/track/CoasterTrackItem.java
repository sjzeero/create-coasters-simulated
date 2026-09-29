/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllDataComponents
 *  com.simibubi.create.AllSoundEvents
 *  com.simibubi.create.content.trains.track.TrackBlock
 *  com.simibubi.create.content.trains.track.TrackBlockEntity
 *  com.simibubi.create.foundation.utility.CreateLang
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.context.UseOnContext
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.network.PacketDistributor
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleClientSounds;
import dev.silvergold.simulatedcoasters.track.CoasterTrackAnchorFirstFromCurvePayload;
import dev.silvergold.simulatedcoasters.track.CoasterTrackHotbarSelectionSync;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

public class CoasterTrackItem
extends Item {
    public CoasterTrackItem(Item.Properties properties) {
        super(properties);
    }

    public static void clearPendingConnection(ItemStack stack) {
        stack.remove(AllDataComponents.TRACK_CONNECTING_FROM);
        stack.remove(AllDataComponents.TRACK_EXTENDED_CURVE);
        CoasterTrackPlacement.clearAnchorFirstSelection(stack);
    }

    public static boolean hasPendingConnection(ItemStack stack) {
        return stack.getItem() instanceof CoasterTrackItem && (stack.has(AllDataComponents.TRACK_CONNECTING_FROM) || CoasterTrackPlacement.hasAnchorFirstSelection(stack));
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
        }
        if (context.getHand() == InteractionHand.OFF_HAND) {
            return InteractionResult.PASS;
        }
        boolean pendingSecondClick = this.isFoil(stack);
        if (!pendingSecondClick) {
            TrackBlockEntity tbe;
            TrackBlock track;
            Block block = state.getBlock();
            if (block instanceof TrackBlock && (track = (TrackBlock)block).getTrackAxes((BlockGetter)level, pos, state).size() > 1) {
                if (!level.isClientSide) {
                    player.displayClientMessage((Component)CreateLang.translateDirect((String)"track.junction_start", (Object[])new Object[0]).withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.SUCCESS;
            }
            block = level.getBlockEntity(pos);
            if (block instanceof TrackBlockEntity && (tbe = (TrackBlockEntity)block).isTilted()) {
                if (!level.isClientSide) {
                    player.displayClientMessage((Component)CreateLang.translateDirect((String)"track.turn_start", (Object[])new Object[0]).withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.SUCCESS;
            }
            if (state.getBlock() instanceof CoasterAnchorpointBlock) {
                CoasterTrackPlacement.applyAnchorFirstSelection(level, stack, pos, player, context.getHand());
                if (level.isClientSide()) {
                    PacketDistributor.sendToServer((CustomPacketPayload)new CoasterTrackAnchorFirstFromCurvePayload(pos), (CustomPacketPayload[])new CustomPacketPayload[0]);
                }
                return InteractionResult.SUCCESS;
            }
            player.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.track.must_attach_to_anchors").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        if (player.isShiftKeyDown()) {
            CoasterTrackItem.clearPendingConnection(stack);
            if (!level.isClientSide()) {
                player.setItemInHand(context.getHand(), stack);
                if (player instanceof ServerPlayer) {
                    ServerPlayer sp = (ServerPlayer)player;
                    CoasterTrackHotbarSelectionSync.syncMainHand(sp, context.getHand(), null);
                }
            }
            return InteractionResult.SUCCESS;
        }
        stack.remove(AllDataComponents.TRACK_EXTENDED_CURVE);
        BlockPos firstAnchor = CoasterTrackPlacement.readAnchorFirstSelection(stack);
        if (level.isClientSide()) {
            if (!CoasterTrackPlacement.tryConnectSecondAnchorFromCurve(level, player, stack, pos, context.getHand())) {
                AllSoundEvents.DENY.playFrom((Entity)player, 1.0f, 1.0f);
                return InteractionResult.FAIL;
            }
            CoasterTrackItem.playConnectionCommitSound();
            CoasterTrackItem.clearPendingConnection(stack);
            player.setItemInHand(context.getHand(), stack);
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)player;
            if (firstAnchor != null && !firstAnchor.equals((Object)pos)) {
                CoasterTrackPlacement.commitSecondAnchorOnServer(sp, context.getHand(), firstAnchor, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @OnlyIn(value=Dist.CLIENT)
    private static void playConnectionCommitSound() {
        BezierHandleClientSounds.playCommitSuccessSound(Minecraft.getInstance());
    }
}

