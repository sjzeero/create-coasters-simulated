package dev.silvergold.simulatedcoasters.mixin;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({AssemblyTransform.class})
public interface AssemblyTransformAccessor {
   @Accessor("angle")
   int getQuarterTurnSteps();
}
