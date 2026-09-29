/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.createmod.catnip.data.Couple
 *  org.spongepowered.asm.mixin.Mixin
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import net.createmod.catnip.data.Couple;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={BezierConnection.class})
public class BezierConnectionGetHandleLengthCoasterMixin {
    @WrapMethod(method={"getHandleLength"})
    private double simulatedcoasters$wrapGetHandleLength(Operation<Double> original) {
        BezierConnection self = (BezierConnection)this;
        Couple<Double> L = CoasterPeerCurveHandleLengths.getCanonicalLengths(self);
        if (L != null) {
            return Math.max((Double)L.getFirst(), (Double)L.getSecond());
        }
        return (Double)original.call(new Object[0]);
    }
}

