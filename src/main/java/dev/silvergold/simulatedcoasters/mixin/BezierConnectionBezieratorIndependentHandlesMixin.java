/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets={"com.simibubi.create.content.trains.track.BezierConnection$Bezierator"})
public class BezierConnectionBezieratorIndependentHandlesMixin {
    @WrapOperation(method={"<init>"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal=0)})
    private Vec3 simulatedcoasters$scaleFirstFinish(Vec3 axis, double vanillaFactor, Operation<Vec3> original, @Local(argsOnly=true) BezierConnection bc) {
        return BezierConnectionBezieratorIndependentHandlesMixin.simulatedcoasters$scaleForEndpoint(axis, vanillaFactor, original, bc, 0);
    }

    @WrapOperation(method={"<init>"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal=1)})
    private Vec3 simulatedcoasters$scaleSecondFinish(Vec3 axis, double vanillaFactor, Operation<Vec3> original, @Local(argsOnly=true) BezierConnection bc) {
        return BezierConnectionBezieratorIndependentHandlesMixin.simulatedcoasters$scaleForEndpoint(axis, vanillaFactor, original, bc, 1);
    }

    private static Vec3 simulatedcoasters$scaleForEndpoint(Vec3 axis, double vanillaFactor, Operation<Vec3> original, BezierConnection bc, int endpointIndex) {
        if (!CoasterPeerCurveHandleLengths.isCoaster(bc)) {
            return (Vec3)original.call(new Object[]{axis, vanillaFactor});
        }
        double h = CoasterPeerCurveHandleLengths.getLengthAtEndpoint(bc, endpointIndex);
        return axis.scale(h);
    }
}

