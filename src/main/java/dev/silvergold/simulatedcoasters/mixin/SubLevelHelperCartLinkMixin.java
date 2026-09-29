/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.SubLevelHelper
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import java.util.Collection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={SubLevelHelper.class})
public class SubLevelHelperCartLinkMixin {
    @Inject(method={"getLoadingDependencyChain"}, at={@At(value="RETURN")})
    private static void simulatedcoasters$augmentCartLinkChain(ServerSubLevel subLevel, CallbackInfoReturnable<Collection<ServerSubLevel>> cir) {
        CoasterCartTrainLinkConstraint.augmentLoadingDependencyChain((Collection)cir.getReturnValue());
    }
}

