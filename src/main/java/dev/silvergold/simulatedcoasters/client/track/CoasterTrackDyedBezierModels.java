package dev.silvergold.simulatedcoasters.client.track;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;

public final class CoasterTrackDyedBezierModels {
   private static final String BASE = "block/track/coaster_track/";
   public static final ResourceLocation LEFT_SEGMENT_DYED_RL = ResourceLocation.fromNamespaceAndPath(
      "simulatedcoasters", "block/track/coaster_track/segment_left_dyed"
   );
   public static final ResourceLocation RIGHT_SEGMENT_DYED_RL = ResourceLocation.fromNamespaceAndPath(
      "simulatedcoasters", "block/track/coaster_track/segment_right_dyed"
   );
   public static final ResourceLocation TIE_DYED_RL = ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "block/track/coaster_track/tie_dyed");
   public static final ResourceLocation CENTER_BEAM_SEGMENT_DYED_RL = ResourceLocation.fromNamespaceAndPath(
      "simulatedcoasters", "block/track/coaster_track/segment_center_beam_dyed"
   );
   public static final PartialModel LEFT_SEGMENT_DYED = PartialModel.of(LEFT_SEGMENT_DYED_RL);
   public static final PartialModel RIGHT_SEGMENT_DYED = PartialModel.of(RIGHT_SEGMENT_DYED_RL);
   public static final PartialModel TIE_DYED = PartialModel.of(TIE_DYED_RL);
   public static final PartialModel CENTER_BEAM_SEGMENT_DYED = PartialModel.of(CENTER_BEAM_SEGMENT_DYED_RL);

   private CoasterTrackDyedBezierModels() {
   }

   public static boolean hasDyedRailTint(int diffuseRgbPacked) {
      return (diffuseRgbPacked & 16777215) != 0;
   }

   public static boolean hasDyedBeamTint(int diffuseRgbPacked) {
      return (diffuseRgbPacked & 16777215) != 0;
   }

   public static void registerAdditional(RegisterAdditional event) {
      event.register(ModelResourceLocation.standalone(LEFT_SEGMENT_DYED_RL));
      event.register(ModelResourceLocation.standalone(RIGHT_SEGMENT_DYED_RL));
      event.register(ModelResourceLocation.standalone(TIE_DYED_RL));
      event.register(ModelResourceLocation.standalone(CENTER_BEAM_SEGMENT_DYED_RL));
   }
}
