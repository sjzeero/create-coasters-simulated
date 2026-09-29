/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.engine_room.flywheel.lib.model.baked.PartialModel
 *  net.minecraft.client.resources.model.ModelResourceLocation
 *  net.minecraft.resources.ResourceLocation
 *  net.neoforged.neoforge.client.event.ModelEvent$RegisterAdditional
 */
package dev.silvergold.simulatedcoasters.client.track;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class CoasterTrackDyedBezierModels {
    private static final String BASE = "block/track/coaster_track/";
    public static final ResourceLocation LEFT_SEGMENT_DYED_RL = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/track/coaster_track/segment_left_dyed");
    public static final ResourceLocation RIGHT_SEGMENT_DYED_RL = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/track/coaster_track/segment_right_dyed");
    public static final ResourceLocation TIE_DYED_RL = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/track/coaster_track/tie_dyed");
    public static final ResourceLocation CENTER_BEAM_SEGMENT_DYED_RL = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/track/coaster_track/segment_center_beam_dyed");
    public static final PartialModel LEFT_SEGMENT_DYED = PartialModel.of((ResourceLocation)LEFT_SEGMENT_DYED_RL);
    public static final PartialModel RIGHT_SEGMENT_DYED = PartialModel.of((ResourceLocation)RIGHT_SEGMENT_DYED_RL);
    public static final PartialModel TIE_DYED = PartialModel.of((ResourceLocation)TIE_DYED_RL);
    public static final PartialModel CENTER_BEAM_SEGMENT_DYED = PartialModel.of((ResourceLocation)CENTER_BEAM_SEGMENT_DYED_RL);

    private CoasterTrackDyedBezierModels() {
    }

    public static boolean hasDyedRailTint(int diffuseRgbPacked) {
        return (diffuseRgbPacked & 0xFFFFFF) != 0;
    }

    public static boolean hasDyedBeamTint(int diffuseRgbPacked) {
        return (diffuseRgbPacked & 0xFFFFFF) != 0;
    }

    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone((ResourceLocation)LEFT_SEGMENT_DYED_RL));
        event.register(ModelResourceLocation.standalone((ResourceLocation)RIGHT_SEGMENT_DYED_RL));
        event.register(ModelResourceLocation.standalone((ResourceLocation)TIE_DYED_RL));
        event.register(ModelResourceLocation.standalone((ResourceLocation)CENTER_BEAM_SEGMENT_DYED_RL));
    }
}

