/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.ItemInteractionResult
 *  net.minecraft.world.item.BlockItem
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.phys.BlockHitResult
 *  net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$LeftClickBlock
 *  net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 *  net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent
 *  net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent$UsePhase
 *  net.neoforged.neoforge.event.level.BlockEvent$EntityPlaceEvent
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.balloon.BalloonRemoval;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

public final class BalloonPopInteraction {
    private BalloonPopInteraction() {
    }

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!BalloonResolve.targetsBalloonCell(event.getLevel(), event.getPos())) {
            return;
        }
        event.setCanceled(true);
        Level level = event.getLevel();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            BalloonPopInteraction.popAt(serverLevel, event.getPos());
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!BalloonPopInteraction.blockBuildOnBalloon(event.getLevel(), event.getPos(), event.getFace(), event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        Level level = event.getLevel();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            BalloonPopInteraction.popForBuildAttempt(serverLevel, event.getPos(), event.getFace());
        }
    }

    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (event.getUsePhase() != UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK) {
            return;
        }
        if (!BalloonPopInteraction.blockBuildOnBalloon(event.getLevel(), event.getPos(), event.getFace(), event.getItemStack())) {
            return;
        }
        event.cancelWithResult(ItemInteractionResult.FAIL);
        Level level = event.getLevel();
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            BalloonPopInteraction.popForBuildAttempt(serverLevel, event.getPos(), event.getFace());
        }
    }

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        LevelAccessor levelAccessor = event.getLevel();
        if (!(levelAccessor instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = (ServerLevel)levelAccessor;
        ServerSubLevel balloon = BalloonResolve.findBalloonAt(level, event.getPos());
        if (balloon == null) {
            return;
        }
        event.setCanceled(true);
        BalloonRemoval.pop(level, balloon);
    }

    public static boolean blockBuildOnBalloon(Level level, BlockHitResult hit, ItemStack stack) {
        return BalloonPopInteraction.blockBuildOnBalloon(level, hit.getBlockPos(), hit.getDirection(), stack);
    }

    public static void handleBuildOnBalloon(ServerLevel level, BlockHitResult hit) {
        BalloonPopInteraction.popForBuildAttempt(level, hit.getBlockPos(), hit.getDirection());
    }

    public static boolean blockBuildOnBalloon(Level level, BlockPos clicked, @Nullable Direction face, ItemStack stack) {
        if (!BalloonPopInteraction.attemptsToBuildOn(stack)) {
            return false;
        }
        if (BalloonResolve.targetsBalloonCell(level, clicked)) {
            return true;
        }
        return face != null && BalloonResolve.targetsBalloonCell(level, clicked.relative(face));
    }

    private static void popAt(ServerLevel level, BlockPos pos) {
        ServerSubLevel balloon = BalloonResolve.findBalloonAt(level, pos);
        if (balloon != null) {
            BalloonRemoval.pop(level, balloon);
        }
    }

    private static void popForBuildAttempt(ServerLevel level, BlockPos clicked, @Nullable Direction face) {
        ServerSubLevel balloon = BalloonPopInteraction.findBalloonForBuildAttempt(level, clicked, face);
        if (balloon != null) {
            BalloonRemoval.pop(level, balloon);
        }
    }

    @Nullable
    private static ServerSubLevel findBalloonForBuildAttempt(ServerLevel level, BlockPos clicked, @Nullable Direction face) {
        ServerSubLevel balloon = BalloonResolve.findBalloonAt(level, clicked);
        if (balloon != null) {
            return balloon;
        }
        if (face == null) {
            return null;
        }
        return BalloonResolve.findBalloonAt(level, clicked.relative(face));
    }

    private static boolean attemptsToBuildOn(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlockItem;
    }
}

