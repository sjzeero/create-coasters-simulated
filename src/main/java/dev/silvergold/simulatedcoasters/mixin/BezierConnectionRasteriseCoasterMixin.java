/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.createmod.catnip.data.Couple
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Slice
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(value={BezierConnection.class})
public class BezierConnectionRasteriseCoasterMixin {
    @WrapOperation(method={"rasterise"}, slice={@Slice(from=@At(value="INVOKE", target="Lcom/simibubi/create/content/trains/track/BezierConnection;getHandleLength()D"), to=@At(value="INVOKE", target="Lnet/createmod/catnip/math/VecHelper;bezier(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal=0)})
    private Vec3 simulatedcoasters$rasterScale0(Vec3 axis, double factor, Operation<Vec3> original) {
        return BezierConnectionRasteriseCoasterMixin.scaleRaster((BezierConnection)this, axis, factor, original, 0);
    }

    @WrapOperation(method={"rasterise"}, slice={@Slice(from=@At(value="INVOKE", target="Lcom/simibubi/create/content/trains/track/BezierConnection;getHandleLength()D"), to=@At(value="INVOKE", target="Lnet/createmod/catnip/math/VecHelper;bezier(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal=1)})
    private Vec3 simulatedcoasters$rasterScale1(Vec3 axis, double factor, Operation<Vec3> original) {
        return BezierConnectionRasteriseCoasterMixin.scaleRaster((BezierConnection)this, axis, factor, original, 1);
    }

    private static Vec3 scaleRaster(BezierConnection self, Vec3 axis, double factor, Operation<Vec3> original, int endpointIndex) {
        if (!CoasterPeerCurveHandleLengths.isCoaster(self)) {
            return (Vec3)original.call(new Object[]{axis, factor});
        }
        Couple<Double> canon = CoasterPeerCurveHandleLengths.getCanonicalLengths(self);
        if (canon == null) {
            return (Vec3)original.call(new Object[]{axis, factor});
        }
        BlockPos lo = CoasterPeerCurveHandleLengths.lexMin((BlockPos)self.bePositions.getFirst(), (BlockPos)self.bePositions.getSecond());
        boolean firstIsLo = ((BlockPos)self.bePositions.getFirst()).equals((Object)lo);
        double h0 = firstIsLo ? (Double)canon.getFirst() : (Double)canon.getSecond();
        double h1 = firstIsLo ? (Double)canon.getSecond() : (Double)canon.getFirst();
        double h = endpointIndex == 0 ? h0 : h1;
        return axis.scale(h);
    }
}

