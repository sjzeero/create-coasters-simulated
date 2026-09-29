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

public final class CoasterTrackCenterBeamModel {
    public static final ResourceLocation CENTER_BEAM_SEGMENT_RL = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/track/coaster_track/segment_center_beam");
    public static final PartialModel CENTER_BEAM_SEGMENT = PartialModel.of((ResourceLocation)CENTER_BEAM_SEGMENT_RL);

    private CoasterTrackCenterBeamModel() {
    }

    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone((ResourceLocation)CENTER_BEAM_SEGMENT_RL));
    }
}

