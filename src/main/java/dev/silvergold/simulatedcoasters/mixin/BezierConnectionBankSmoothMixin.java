/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyReturnValue
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.BezierConnection$Segment
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.BezierBankBlend;
import dev.silvergold.simulatedcoasters.track.BezierBankBlendContext;
import java.util.Iterator;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={BezierConnection.class})
public class BezierConnectionBankSmoothMixin {
    @ModifyReturnValue(method={"iterator"}, at={@At(value="RETURN")})
    private Iterator<BezierConnection.Segment> simulatedcoasters$wrapIteratorForBankSmooth(final Iterator<BezierConnection.Segment> original) {
        final BezierConnection self = (BezierConnection)this;
        return new Iterator<BezierConnection.Segment>(this){

            @Override
            public boolean hasNext() {
                return original.hasNext();
            }

            @Override
            public BezierConnection.Segment next() {
                BezierBankBlendContext.set(self);
                try {
                    BezierConnection.Segment segment = (BezierConnection.Segment)original.next();
                    return segment;
                }
                finally {
                    BezierBankBlendContext.clear();
                }
            }
        };
    }

    @WrapOperation(method={"getNormal"}, at={@At(value="INVOKE", target="Lnet/createmod/catnip/math/VecHelper;slerp(FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;")})
    private Vec3 simulatedcoasters$smoothBankSlerpInGetNormal(float t, Vec3 fn1, Vec3 fn2, Operation<Vec3> original) {
        BezierConnection self = (BezierConnection)this;
        float tt = BezierBankBlend.smoothJointBankParameter(t, self);
        return (Vec3)original.call(new Object[]{Float.valueOf(tt), fn1, fn2});
    }
}

