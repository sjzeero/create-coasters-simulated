/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.BezierConnection$SegmentAngles
 *  org.spongepowered.asm.mixin.Mixin
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={BezierConnection.class})
public class BezierConnectionRailGaugeBakeMixin {
    @WrapMethod(method={"getBakedSegments"})
    private BezierConnection.SegmentAngles simulatedcoasters$withRailGaugeBakeContext(Operation<BezierConnection.SegmentAngles> original) {
        CoasterTrackGauge.BAKE_BEZIER.set((BezierConnection)this);
        try {
            BezierConnection.SegmentAngles segmentAngles = (BezierConnection.SegmentAngles)original.call(new Object[0]);
            return segmentAngles;
        }
        finally {
            CoasterTrackGauge.BAKE_BEZIER.remove();
        }
    }
}

