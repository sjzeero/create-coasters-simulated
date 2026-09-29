package dev.silvergold.simulatedcoasters.mixin;

import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SubLevelHelper.class})
public class SubLevelHelperCartLinkMixin {
   @Inject(
      method = {"getLoadingDependencyChain"},
      at = {@At("RETURN")}
   )
   private static void simulatedcoasters$augmentCartLinkChain(ServerSubLevel subLevel, CallbackInfoReturnable<Collection<ServerSubLevel>> cir) {
      CoasterCartTrainLinkConstraint.augmentLoadingDependencyChain((Collection<ServerSubLevel>)cir.getReturnValue());
   }
}
