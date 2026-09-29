package dev.silvergold.simulatedcoasters.mixin;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkSync;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SubLevelTrackingSystem.class})
public class SubLevelTrackingSystemCartLinkMixin {
   @Inject(
      method = {"sendFullSync"},
      at = {@At("TAIL")}
   )
   private void simulatedcoasters$syncCartTrainLinks(ServerPlayer player, ServerSubLevel subLevel, @Nullable CustomPacketPayload extraPacket, CallbackInfo ci) {
      if (CoasterCartResolve.isCartSubLevel(subLevel)) {
         CoasterCartTrainLinkSync.sendCartToPlayer(player, subLevel);
      }
   }
}
