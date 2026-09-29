/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllItems
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 */
package dev.silvergold.simulatedcoasters.balloon;

import com.simibubi.create.AllItems;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.balloon.BalloonRemoval;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class BalloonWrenchPickupInteraction {
    private BalloonWrenchPickupInteraction() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isShiftKeyDown()) {
            return;
        }
        ItemStack stack = player.getItemInHand(event.getHand());
        if (!AllItems.WRENCH.isIn(stack)) {
            return;
        }
        Level level = event.getLevel();
        if (!BalloonResolve.targetsBalloonCell(level, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        ServerSubLevel balloon = BalloonResolve.findBalloonAt(serverLevel, event.getPos());
        if (balloon == null) {
            return;
        }
        if (!BalloonRemoval.safelyRemove(serverLevel, balloon)) {
            return;
        }
        if (!serverPlayer.getAbilities().instabuild) {
            ItemStack balloonItem = new ItemStack((ItemLike)SimulatedCoasters.RED_BALLOON.get());
            if (!serverPlayer.getInventory().add(balloonItem)) {
                serverPlayer.drop(balloonItem, false);
            }
        }
        serverPlayer.swing(event.getHand(), true);
    }
}

