package dev.silvergold.simulatedcoasters.client.track;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ChainLiftPlacementClientSounds {
   private static final float SNAP_ENGAGE_VOLUME = 0.4F;
   private static final float SNAP_ENGAGE_PITCH = 1.12F;

   private ChainLiftPlacementClientSounds() {
   }

   public static void playStartAnchorSelect(Minecraft mc, Vec3 worldPos) {
      if (mc.level != null) {
         mc.getSoundManager()
            .play(
               new SimpleSoundInstance(
                  SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.4F, 1.12F, mc.level.getRandom(), worldPos.x, worldPos.y, worldPos.z
               )
            );
      }
   }
}
