/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.ItemInteractionResult
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 *  net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent
 */
package dev.silvergold.simulatedcoasters.track.anchor;

import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;

public final class ManagedFakeTrackInteractionBlock {
    private ManagedFakeTrackInteractionBlock() {
    }

    public static boolean isBlockedTarget(Level level, BlockPos pos) {
        return AnchorPeerFakeTracks.isManagedFakeTrack(level, pos);
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (ManagedFakeTrackInteractionBlock.isBlockedTarget(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    public static void onUseItemOnBlock(UseItemOnBlockEvent event) {
        if (ManagedFakeTrackInteractionBlock.isBlockedTarget(event.getLevel(), event.getPos())) {
            event.cancelWithResult(ItemInteractionResult.FAIL);
        }
    }
}

