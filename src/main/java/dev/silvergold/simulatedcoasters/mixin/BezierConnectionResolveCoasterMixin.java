/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={BezierConnection.class})
public class BezierConnectionResolveCoasterMixin {
    @Inject(method={"resolve"}, at={@At(value="HEAD")})
    private void simulatedcoasters$enterResolve(CallbackInfoReturnable<Object> cir) {
        CoasterTrackGauge.RESOLVING_BEZIER.set((BezierConnection)this);
    }

    @Inject(method={"resolve"}, at={@At(value="RETURN")})
    private void simulatedcoasters$exitResolve(CallbackInfoReturnable<Object> cir) {
        CoasterTrackGauge.RESOLVING_BEZIER.remove();
    }
}

