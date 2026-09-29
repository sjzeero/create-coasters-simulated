/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.engine_room.flywheel.lib.model.baked.PartialModel
 *  net.minecraft.client.resources.model.ModelResourceLocation
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.neoforged.neoforge.client.event.ModelEvent$RegisterAdditional
 */
package dev.silvergold.simulatedcoasters.client.track;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.event.ModelEvent;

public final class CoasterTrackAnchorGirderModel {
    public static final ResourceLocation TRACK_ANCHORPOINT_GIRDER_RL = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/track/coaster_track/track_anchorpoint_girder");
    public static final ResourceLocation TRACK_ANCHORPOINT_GIRDER_CHAINLIFT_RL = ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)"block/track/coaster_track/track_anchorpoint_girder_chainlift");
    public static final PartialModel TRACK_ANCHORPOINT_GIRDER = PartialModel.of((ResourceLocation)TRACK_ANCHORPOINT_GIRDER_RL);
    public static final PartialModel TRACK_ANCHORPOINT_GIRDER_CHAINLIFT = PartialModel.of((ResourceLocation)TRACK_ANCHORPOINT_GIRDER_CHAINLIFT_RL);

    private CoasterTrackAnchorGirderModel() {
    }

    public static PartialModel partialForAnchor(Level level, BlockPos anchorPos) {
        CoasterAnchorpointBlockEntity anchor;
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity && (anchor = (CoasterAnchorpointBlockEntity)blockEntity).hasChainLiftShaft()) {
            return TRACK_ANCHORPOINT_GIRDER_CHAINLIFT;
        }
        return TRACK_ANCHORPOINT_GIRDER;
    }

    public static void registerAdditional(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone((ResourceLocation)TRACK_ANCHORPOINT_GIRDER_RL));
        event.register(ModelResourceLocation.standalone((ResourceLocation)TRACK_ANCHORPOINT_GIRDER_CHAINLIFT_RL));
    }
}

