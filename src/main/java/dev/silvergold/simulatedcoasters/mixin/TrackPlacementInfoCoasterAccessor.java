/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.TrackPlacement$PlacementInfo
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.trains.track.TrackPlacement;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={TrackPlacement.PlacementInfo.class})
public interface TrackPlacementInfoCoasterAccessor {
    @Accessor(value="end1")
    public Vec3 sc$getEnd1();

    @Accessor(value="axis1")
    public Vec3 sc$getAxis1();
}

