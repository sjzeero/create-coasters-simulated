package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenuSubmitPacket;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RadialWrenchMenuSubmitPacket.class})
public class RadialWrenchMenuSubmitAnchorMixin {
   @Inject(
      method = {"handle"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$rejectCoasterAnchor(ServerPlayer player, CallbackInfo ci) {
      RadialWrenchMenuSubmitPacket self = (RadialWrenchMenuSubmitPacket)(Object)this;
      BlockState current = player.level().getBlockState(self.blockPos());
      if (current.getBlock() instanceof CoasterAnchorpointBlock) {
         ci.cancel();
      }
   }
}
