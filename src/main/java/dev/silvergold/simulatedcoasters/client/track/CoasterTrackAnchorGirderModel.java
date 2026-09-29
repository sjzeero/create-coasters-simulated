package dev.silvergold.simulatedcoasters.client.track;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional;

public final class CoasterTrackAnchorGirderModel {
   public static final ResourceLocation TRACK_ANCHORPOINT_GIRDER_RL = ResourceLocation.fromNamespaceAndPath(
      "simulatedcoasters", "block/track/coaster_track/track_anchorpoint_girder"
   );
   public static final ResourceLocation TRACK_ANCHORPOINT_GIRDER_CHAINLIFT_RL = ResourceLocation.fromNamespaceAndPath(
      "simulatedcoasters", "block/track/coaster_track/track_anchorpoint_girder_chainlift"
   );
   public static final PartialModel TRACK_ANCHORPOINT_GIRDER = PartialModel.of(TRACK_ANCHORPOINT_GIRDER_RL);
   public static final PartialModel TRACK_ANCHORPOINT_GIRDER_CHAINLIFT = PartialModel.of(TRACK_ANCHORPOINT_GIRDER_CHAINLIFT_RL);

   private CoasterTrackAnchorGirderModel() {
   }

   public static PartialModel partialForAnchor(Level level, BlockPos anchorPos) {
      if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity anchor && anchor.hasChainLiftShaft()) {
         return TRACK_ANCHORPOINT_GIRDER_CHAINLIFT;
      }

      return TRACK_ANCHORPOINT_GIRDER;
   }

   public static void registerAdditional(RegisterAdditional event) {
      event.register(ModelResourceLocation.standalone(TRACK_ANCHORPOINT_GIRDER_RL));
      event.register(ModelResourceLocation.standalone(TRACK_ANCHORPOINT_GIRDER_CHAINLIFT_RL));
   }
}
