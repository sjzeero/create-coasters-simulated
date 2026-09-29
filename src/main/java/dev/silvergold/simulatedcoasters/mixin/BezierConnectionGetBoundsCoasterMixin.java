package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterBezierBoundsCache;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({BezierConnection.class})
public class BezierConnectionGetBoundsCoasterMixin {
   @ModifyReturnValue(
      method = {"getBounds"},
      at = {@At("RETURN")}
   )
   private AABB simulatedcoasters$shiftBoundsForBakedRailYOffset(AABB bounds) {
      BezierConnection self = (BezierConnection)(Object)this;
      return !self.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id) ? bounds : CoasterBezierBoundsCache.shiftCoasterBounds(self, bounds);
   }
}
