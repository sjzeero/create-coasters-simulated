package dev.silvergold.simulatedcoasters.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.minecraft.core.BlockPos;

public final class CoasterPonderUndyeEffects {
   private CoasterPonderUndyeEffects() {
   }

   public static void spawnUndyeParticlesForDyedLayer(CreateSceneBuilder scene, int dyedSourceY, int displayY, BlockPos... anchorXz) {
      scene.addInstruction(new CoasterPonderUndyeParticleInstruction(dyedSourceY, displayY, anchorXz));
   }
}
