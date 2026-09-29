package dev.silvergold.simulatedcoasters.mixin;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Minecraft.class})
public class MinecraftPickBlockAnchorMixin {
   @Inject(
      method = {"addCustomNbtData"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void simulatedcoasters$skipAnchorCtrlPickBlockData(ItemStack stack, BlockEntity blockEntity, RegistryAccess registryAccess, CallbackInfo ci) {
      if (blockEntity instanceof CoasterAnchorpointBlockEntity) {
         ci.cancel();
      }
   }
}
