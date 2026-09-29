/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={BezierConnection.class})
public class BezierConnectionFaceNormalSlerpCoasterMixin {
    private static boolean simulatedcoasters$forceSlerp(BezierConnection self, Vec3 fn1, Object other, Operation<Boolean> original) {
        if (CoasterBezierRailFrames.isCoaster(self)) {
            return false;
        }
        return (Boolean)original.call(new Object[]{fn1, other});
    }

    @WrapOperation(method={"getNormal"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;equals(Ljava/lang/Object;)Z")})
    private boolean simulatedcoasters$alwaysSlerpFaceNormalsInGetNormal(Vec3 fn1, Object other, Operation<Boolean> original) {
        return BezierConnectionFaceNormalSlerpCoasterMixin.simulatedcoasters$forceSlerp((BezierConnection)this, fn1, other, original);
    }

    @WrapOperation(method={"rasterise"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;equals(Ljava/lang/Object;)Z")})
    private boolean simulatedcoasters$alwaysSlerpFaceNormalsInRasterise(Vec3 fn1, Object other, Operation<Boolean> original) {
        return BezierConnectionFaceNormalSlerpCoasterMixin.simulatedcoasters$forceSlerp((BezierConnection)this, fn1, other, original);
    }
}

