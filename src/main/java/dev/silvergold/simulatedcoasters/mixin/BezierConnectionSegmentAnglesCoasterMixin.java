/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.BezierConnection$SegmentAngles
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Constant
 *  org.spongepowered.asm.mixin.injection.ModifyConstant
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value={BezierConnection.SegmentAngles.class})
public class BezierConnectionSegmentAnglesCoasterMixin {
    private static final double VANILLA_RAIL_HALF_GAUGE = (double)0.965f;

    private static boolean simulatedcoasters$isCoaster(BezierConnection bc) {
        return bc != null && bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id);
    }

    private static boolean simulatedcoasters$isRailHalfGaugeFactor(double factor) {
        return Math.abs(factor - (double)0.965f) < 1.0E-5;
    }

    @WrapOperation(method={"<init>"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;")})
    private Vec3 simulatedcoasters$wrapVec3Scale(Vec3 instance, double factor, Operation<Vec3> original) {
        double f = factor;
        if (BezierConnectionSegmentAnglesCoasterMixin.simulatedcoasters$isCoaster(CoasterTrackGauge.bakingBezier()) && BezierConnectionSegmentAnglesCoasterMixin.simulatedcoasters$isRailHalfGaugeFactor(f)) {
            f *= (double)0.8f;
        }
        return (Vec3)original.call(new Object[]{instance, f});
    }

    @ModifyConstant(method={"<init>"}, constant={@Constant(floatValue=-0.12890625f)})
    private float simulatedcoasters$liftBezierModelAnchorY(float vanilla) {
        if (BezierConnectionSegmentAnglesCoasterMixin.simulatedcoasters$isCoaster(CoasterTrackGauge.bakingBezier())) {
            return vanilla + 0.375f;
        }
        return vanilla;
    }
}

