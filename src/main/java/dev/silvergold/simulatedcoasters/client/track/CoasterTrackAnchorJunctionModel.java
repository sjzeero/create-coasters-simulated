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

public final class CoasterTrackAnchorJunctionModel {
    public static final ResourceLocation TRACK_ANCHORPOINT_CONNECTION_RL = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/track/coaster_track/track_anchorpoint_connection");
    public static final ResourceLocation TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT_RL = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/track/coaster_track/track_anchorpoint_connection_chainlift");
    public static final ResourceLocation TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT_END_RL = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/track/coaster_track/track_anchorpoint_connection_chainlift_end");
    public static final PartialModel TRACK_ANCHORPOINT_CONNECTION = PartialModel.of((ResourceLocation)TRACK_ANCHORPOINT_CONNECTION_RL);
    public static final PartialModel TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT = PartialModel.of((ResourceLocation)TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT_RL);
    public static final PartialModel TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT_END = PartialModel.of((ResourceLocation)TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT_END_RL);

    private CoasterTrackAnchorJunctionModel() {
    }

    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone((ResourceLocation)TRACK_ANCHORPOINT_CONNECTION_RL));
        event.register(ModelResourceLocation.standalone((ResourceLocation)TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT_RL));
        event.register(ModelResourceLocation.standalone((ResourceLocation)TRACK_ANCHORPOINT_CONNECTION_CHAINLIFT_END_RL));
    }
}

