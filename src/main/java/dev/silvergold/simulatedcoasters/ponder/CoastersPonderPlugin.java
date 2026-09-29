/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.TrackBlock
 *  com.simibubi.create.foundation.ponder.CreatePonderPlugin
 *  net.createmod.ponder.api.level.PonderLevel
 *  net.createmod.ponder.api.registration.IndexExclusionHelper
 *  net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
 *  net.createmod.ponder.api.registration.PonderTagRegistrationHelper
 *  net.createmod.ponder.api.registration.SharedTextRegistrationHelper
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.block.Block
 */
package dev.silvergold.simulatedcoasters.ponder;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.foundation.ponder.CreatePonderPlugin;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.ponder.CoasterPonderRestore;
import dev.silvergold.simulatedcoasters.ponder.CoastersPonderScenes;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public class CoastersPonderPlugin
extends CreatePonderPlugin {
    public String getModId() {
        return "simulatedcoasters";
    }

    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        CoastersPonderScenes.register(helper);
    }

    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
    }

    public void registerSharedText(SharedTextRegistrationHelper helper) {
    }

    public void onPonderLevelRestore(PonderLevel ponderLevel) {
        CoasterPonderRestore.onLevelRestore(ponderLevel);
    }

    public void indexExclusions(IndexExclusionHelper helper) {
        helper.excludeBlockVariants(TrackBlock.class, (Block)SimulatedCoastersBlocks.COASTER_TRACK_MATERIAL.get());
    }
}

