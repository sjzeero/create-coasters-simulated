package dev.silvergold.simulatedcoasters.client.track;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BezierHandleClientSounds {
   public static final float COMMIT_SUCCESS_VOLUME = 0.38F;
   public static final float COMMIT_SUCCESS_PITCH_MIN = 0.62F;
   public static final float COMMIT_SUCCESS_PITCH_RANGE = 0.06F;

   private BezierHandleClientSounds() {
   }

   public static void playCreateUi(SoundEvent sound, float volume, float pitch) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && sound != null) {
         mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
      }
   }

   public static void playCreateDeny() {
      playCreateUi((SoundEvent)SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.5F);
   }

   public static float commitSuccessPitch(RandomSource random) {
      return 0.62F + random.nextFloat() * 0.06F;
   }

   public static void playCommitSuccessSound(Minecraft mc) {
      if (mc.level != null) {
         playCreateUi(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.38F, commitSuccessPitch(mc.level.getRandom()));
      }
   }
}
