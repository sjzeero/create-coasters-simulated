/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyReturnValue
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.world.phys.AABB
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterBezierBoundsCache;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={BezierConnection.class})
public class BezierConnectionGetBoundsCoasterMixin {
    @ModifyReturnValue(method={"getBounds"}, at={@At(value="RETURN")})
    private AABB simulatedcoasters$shiftBoundsForBakedRailYOffset(AABB bounds) {
        BezierConnection self = (BezierConnection)this;
        if (!self.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return bounds;
        }
        return CoasterBezierBoundsCache.shiftCoasterBounds(self, bounds);
    }
}

