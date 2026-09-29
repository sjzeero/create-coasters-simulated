/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 *  org.joml.Vector3d
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostKey;
import dev.silvergold.simulatedcoasters.balloon.BalloonPlacement;
import dev.silvergold.simulatedcoasters.balloon.BalloonSpawner;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.joml.Vector3d;

public final class BalloonAttachInteraction {
    private BalloonAttachInteraction() {
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Vector3d soundPos;
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        if (!stack.is(SimulatedCoasters.RED_BALLOON)) {
            return;
        }
        Level level = player.level();
        if (!BalloonPlacement.isValidHost(level, event.getPos())) {
            if (!level.isClientSide() && player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)player;
                serverPlayer.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.balloon.must_attach_to_sublevel").withStyle(ChatFormatting.GRAY), true);
            }
            return;
        }
        event.setCanceled(true);
        if (level.isClientSide() || !(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        if (!(level instanceof ServerLevel)) {
            return;
        }
        ServerLevel sl = (ServerLevel)level;
        ServerSubLevel placed = BalloonSpawner.tryAttach(sl, event.getPos());
        if (placed == null) {
            serverPlayer.displayClientMessage((Component)Component.translatable((String)"simulatedcoasters.balloon.attach_failed").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }
        serverPlayer.swing(InteractionHand.MAIN_HAND, true);
        BalloonHostKey hostKey = BalloonHostKey.of((Level)sl, event.getPos());
        Vector3d vector3d = soundPos = hostKey != null ? BalloonPlacement.hostAnchorWorld(BalloonPlacement.resolveHostSub((Level)sl, hostKey.hostSubLevelId()), hostKey.cell()) : null;
        if (soundPos != null) {
            sl.playSound(null, BlockPos.containing((double)soundPos.x, (double)soundPos.y, (double)soundPos.z), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8f, 1.1f);
        }
        SimulatedCoasters.LOGGER.info("[balloon] attached {} to host plot cell {}", (Object)placed.getUniqueId(), (Object)(hostKey != null ? hostKey.cell() : event.getPos()));
    }
}

