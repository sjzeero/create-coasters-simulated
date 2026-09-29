package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
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
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import org.joml.Vector3d;

public final class BalloonAttachInteraction {
   private BalloonAttachInteraction() {
   }

   public static void onRightClickBlock(RightClickBlock event) {
      if (event.getHand() == InteractionHand.MAIN_HAND) {
         Player player = event.getEntity();
         ItemStack stack = player.getMainHandItem();
         if (stack.is(SimulatedCoasters.RED_BALLOON)) {
            Level level = player.level();
            if (!BalloonPlacement.isValidHost(level, event.getPos())) {
               if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                  serverPlayer.displayClientMessage(
                     Component.translatable("simulatedcoasters.balloon.must_attach_to_sublevel").withStyle(ChatFormatting.GRAY), true
                  );
               }
            } else {
               event.setCanceled(true);
               if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                  if (level instanceof ServerLevel sl) {
                     ServerSubLevel placed = BalloonSpawner.tryAttach(sl, event.getPos());
                     if (placed == null) {
                        serverPlayer.displayClientMessage(Component.translatable("simulatedcoasters.balloon.attach_failed").withStyle(ChatFormatting.RED), true);
                     } else {
                        if (!serverPlayer.getAbilities().instabuild) {
                           stack.shrink(1);
                        }

                        serverPlayer.swing(InteractionHand.MAIN_HAND, true);
                        BalloonHostKey hostKey = BalloonHostKey.of(sl, event.getPos());
                        Vector3d soundPos = hostKey != null
                           ? BalloonPlacement.hostAnchorWorld(BalloonPlacement.resolveHostSub(sl, hostKey.hostSubLevelId()), hostKey.cell())
                           : null;
                        if (soundPos != null) {
                           sl.playSound(null, BlockPos.containing(soundPos.x, soundPos.y, soundPos.z), SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.8F, 1.1F);
                        }

                        SimulatedCoasters.LOGGER
                           .info("[balloon] attached {} to host plot cell {}", placed.getUniqueId(), hostKey != null ? hostKey.cell() : event.getPos());
                     }
                  }
               }
            }
         }
      }
   }
}
