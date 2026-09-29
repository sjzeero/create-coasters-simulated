/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.TrackBlock
 *  com.simibubi.create.content.trains.track.TrackMaterial
 *  com.simibubi.create.content.trains.track.TrackMaterialFactory
 *  com.tterrag.registrate.util.nullness.NonNullSupplier
 *  net.minecraft.resources.ResourceLocation
 */
package dev.silvergold.simulatedcoasters;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackMaterialFactory;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import net.minecraft.resources.ResourceLocation;

public final class CoasterTrackMaterials {
    public static final TrackMaterial COASTER = TrackMaterialFactory.make((ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_track")).lang("Coaster Track").block(NonNullSupplier.lazy(() -> () -> (TrackBlock)SimulatedCoastersBlocks.COASTER_TRACK_MATERIAL.get())).particle(ResourceLocation.fromNamespaceAndPath((String)"create", (String)"block/industrial_iron_block")).standardModels().noRecipeGen().build();

    private CoasterTrackMaterials() {
    }
}

