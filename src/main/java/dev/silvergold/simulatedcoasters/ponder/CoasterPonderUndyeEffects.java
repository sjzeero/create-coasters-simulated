/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.foundation.ponder.CreateSceneBuilder
 *  net.createmod.ponder.foundation.instruction.PonderInstruction
 *  net.minecraft.core.BlockPos
 */
package dev.silvergold.simulatedcoasters.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.silvergold.simulatedcoasters.ponder.CoasterPonderUndyeParticleInstruction;
import net.createmod.ponder.foundation.instruction.PonderInstruction;
import net.minecraft.core.BlockPos;

public final class CoasterPonderUndyeEffects {
    private CoasterPonderUndyeEffects() {
    }

    public static void spawnUndyeParticlesForDyedLayer(CreateSceneBuilder scene, int dyedSourceY, int displayY, BlockPos ... anchorXz) {
        scene.addInstruction((PonderInstruction)new CoasterPonderUndyeParticleInstruction(dyedSourceY, displayY, anchorXz));
    }
}

