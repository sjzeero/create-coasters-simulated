package dev.silvergold.simulatedcoasters.mixin;

import com.simibubi.create.content.trains.track.TrackPlacement.PlacementInfo;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PlacementInfo.class})
public interface TrackPlacementInfoCoasterAccessor {
   @Accessor("end1")
   Vec3 sc$getEnd1();

   @Accessor("axis1")
   Vec3 sc$getAxis1();
}
