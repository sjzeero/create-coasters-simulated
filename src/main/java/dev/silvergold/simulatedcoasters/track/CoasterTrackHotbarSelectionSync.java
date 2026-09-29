/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.item.ItemStack
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.track.CoasterTrackHotbarSelectionSyncPayload;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class CoasterTrackHotbarSelectionSync {
    private CoasterTrackHotbarSelectionSync() {
    }

    public static void syncMainHand(ServerPlayer player, InteractionHand hand, @Nullable BlockPos anchorFirstOrNull) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof CoasterTrackItem)) {
            return;
        }
        player.setItemInHand(hand, stack);
        int slot = player.getInventory().selected;
        if (hand == InteractionHand.OFF_HAND) {
            return;
        }
        long anchorLong = anchorFirstOrNull == null ? 0L : anchorFirstOrNull.asLong();
        CoasterTrackHotbarSelectionSyncPayload.broadcast(player, slot, anchorLong);
    }
}

