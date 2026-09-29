/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package dev.silvergold.simulatedcoasters.mixin;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={SubLevelAssemblyHelper.AssemblyTransform.class})
public interface AssemblyTransformAccessor {
    @Accessor(value="angle")
    public int getQuarterTurnSteps();
}

