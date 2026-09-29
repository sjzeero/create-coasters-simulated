package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.trains.track.TrackPlacement;
import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TrackPlacement.class})
public class TrackPlacementClientTickCoasterMixin {
   @Shadow
   private static PlacementInfo cached;

   @Inject(
      method = {"clientTick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private static void simulatedcoasters$skipCreateClientTickForCoasterItem(CallbackInfo ci) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         if (mc.player.getMainHandItem().getItem() instanceof CoasterTrackItem || mc.player.getOffhandItem().getItem() instanceof CoasterTrackItem) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"clientTick"},
      at = {@At(
         value = "INVOKE_ASSIGN",
         target = "Lcom/simibubi/create/content/trains/track/TrackPlacement;tryConnect(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/ItemStack;ZZ)Lcom/simibubi/create/content/trains/track/TrackPlacement$PlacementInfo;"
      )},
      cancellable = true
   )
   private static void simulatedcoasters$abortClientTickIfTryConnectMissingPreview(CallbackInfo ci) {
      if (cached != null) {
         TrackPlacementInfoCoasterAccessor access = (TrackPlacementInfoCoasterAccessor)cached;
         if (access.sc$getEnd1() == null || access.sc$getAxis1() == null) {
            ci.cancel();
         }
      }
   }
}
