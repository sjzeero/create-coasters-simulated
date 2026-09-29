package dev.silvergold.simulatedcoasters.mixin;

import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorJunctionVisualRefresh;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({CoasterAnchorpointBlockEntity.class})
public class CoasterAnchorpointBlockEntityClientMixin {
   @Inject(
      method = {"read"},
      at = {@At("TAIL")}
   )
   private void simulatedcoasters$scheduleJunctionClusterRefreshAfterRead(CompoundTag tag, Provider registries, boolean clientPacket, CallbackInfo ci) {
      CoasterAnchorpointBlockEntity self = (CoasterAnchorpointBlockEntity)(Object)this;
      if (self.getLevel() != null && self.getLevel().isClientSide()) {
         AnchorJunctionVisualRefresh.scheduleClusterRefresh(self.getLevel(), self.getBlockPos());
         VisualizationHelper.queueUpdate(self);
      }
   }
}
