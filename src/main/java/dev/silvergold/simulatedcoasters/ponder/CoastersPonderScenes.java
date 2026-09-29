/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
 *  net.minecraft.resources.ResourceLocation
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.ponder;

import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.ponder.scenes.CoasterCartScenes;
import dev.silvergold.simulatedcoasters.ponder.scenes.CoasterTrackScenes;
import java.util.Map;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public final class CoastersPonderScenes {
    private static final Map<ResourceLocation, ResourceLocation> SCHEMATIC_PATHS = Map.of(ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"ponder_track_placement"), ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_track/ponder_track_placement"), ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"ponder_chainlift"), ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_track/ponder_chainlift"), ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"ponder_cart"), ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"coaster_track/ponder_cart"));

    private CoastersPonderScenes() {
    }

    @Nullable
    public static ResourceLocation schematicPathFor(ResourceLocation sceneId) {
        return SCHEMATIC_PATHS.get(sceneId);
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> registry) {
        String trackSchematic = "coaster_track/ponder_track_placement";
        registry.addStoryBoard((Object)SimulatedCoasters.COASTER_TRACK.getId(), trackSchematic, CoasterTrackScenes::placement, new ResourceLocation[0]);
        registry.addStoryBoard((Object)SimulatedCoasters.COASTER_ANCHORPOINT.getId(), trackSchematic, CoasterTrackScenes::placement, new ResourceLocation[0]);
        String chainLiftSchematic = "coaster_track/ponder_chainlift";
        registry.addStoryBoard((Object)SimulatedCoasters.COASTER_TRACK.getId(), chainLiftSchematic, CoasterTrackScenes::chainLifts, new ResourceLocation[0]);
        registry.addStoryBoard((Object)SimulatedCoasters.COASTER_ANCHORPOINT.getId(), chainLiftSchematic, CoasterTrackScenes::chainLifts, new ResourceLocation[0]);
        registry.addStoryBoard((Object)SimulatedCoasters.COASTER_CART.getId(), "coaster_track/ponder_cart", CoasterCartScenes::linking, new ResourceLocation[0]);
    }
}

