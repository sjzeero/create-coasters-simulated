package dev.silvergold.simulatedcoasters.client.track;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;

public final class CoasterTrackCenterBeamModel {
   public static final ResourceLocation CENTER_BEAM_SEGMENT_RL = ResourceLocation.fromNamespaceAndPath(
      "simulatedcoasters", "block/track/coaster_track/segment_center_beam"
   );
   public static final PartialModel CENTER_BEAM_SEGMENT = PartialModel.of(CENTER_BEAM_SEGMENT_RL);

   private CoasterTrackCenterBeamModel() {
   }

   public static void registerAdditional(RegisterAdditional event) {
      event.register(ModelResourceLocation.standalone(CENTER_BEAM_SEGMENT_RL));
   }
}
