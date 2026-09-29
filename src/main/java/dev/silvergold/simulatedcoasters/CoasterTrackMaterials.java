package dev.silvergold.simulatedcoasters;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackMaterialFactory;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.resources.ResourceLocation;

public final class CoasterTrackMaterials {
   public static final TrackMaterial COASTER = TrackMaterialFactory.make(ResourceLocation.fromNamespaceAndPath("simulatedcoasters", "coaster_track"))
      .lang("Coaster Track")
      .block(NonNullSupplier.lazy(() -> () -> (TrackBlock)SimulatedCoastersBlocks.COASTER_TRACK_MATERIAL.get()))
      .particle(ResourceLocation.fromNamespaceAndPath("create", "block/industrial_iron_block"))
      .standardModels()
      .noRecipeGen()
      .build();

   private CoasterTrackMaterials() {
   }
}
