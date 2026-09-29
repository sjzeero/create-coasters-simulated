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
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(targets={"com.simibubi.create.content.trains.track.BezierConnection$Runtime"})
public class BezierConnectionIndependentHandleRuntimeMixin {
    private static Vec3 simulatedcoasters$scaleTangent(Vec3 axis, double createHandleLength, Operation<Vec3> original, int endpointIndex) {
        BezierConnection bc = CoasterTrackGauge.resolvingBezier();
        if (!CoasterPeerCurveHandleLengths.isCoaster(bc)) {
            return (Vec3)original.call(new Object[]{axis, createHandleLength});
        }
        Couple<Double> canon = CoasterPeerCurveHandleLengths.getCanonicalLengths(bc);
        if (canon == null) {
            return (Vec3)original.call(new Object[]{axis, createHandleLength});
        }
        BlockPos lo = CoasterPeerCurveHandleLengths.lexMin((BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond());
        boolean firstIsLo = ((BlockPos)bc.bePositions.getFirst()).equals((Object)lo);
        double h0 = firstIsLo ? (Double)canon.getFirst() : (Double)canon.getSecond();
        double h1 = firstIsLo ? (Double)canon.getSecond() : (Double)canon.getFirst();
        double h = endpointIndex == 0 ? h0 : h1;
        return axis.scale(h);
    }

    @WrapOperation(method={"<init>"}, slice={@Slice(from=@At(value="INVOKE", target="Lcom/simibubi/create/content/trains/track/BezierConnection$Runtime;determineHandles(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V"), to=@At(value="INVOKE", target="Lcom/simibubi/create/content/trains/track/BezierConnection$Runtime;computeLength(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)D"))}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal=0)})
    private Vec3 simulatedcoasters$scaleFirstHandle(Vec3 axis, double factor, Operation<Vec3> original) {
        return BezierConnectionIndependentHandleRuntimeMixin.simulatedcoasters$scaleTangent(axis, factor, original, 0);
    }

    @WrapOperation(method={"<init>"}, slice={@Slice(from=@At(value="INVOKE", target="Lcom/simibubi/create/content/trains/track/BezierConnection$Runtime;determineHandles(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V"), to=@At(value="INVOKE", target="Lcom/simibubi/create/content/trains/track/BezierConnection$Runtime;computeLength(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;I)D"))}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;", ordinal=1)})
    private Vec3 simulatedcoasters$scaleSecondHandle(Vec3 axis, double factor, Operation<Vec3> original) {
        return BezierConnectionIndependentHandleRuntimeMixin.simulatedcoasters$scaleTangent(axis, factor, original, 1);
    }
}

