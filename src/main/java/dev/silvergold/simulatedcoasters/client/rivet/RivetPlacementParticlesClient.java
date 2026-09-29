package dev.silvergold.simulatedcoasters.client.rivet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RivetPlacementParticlesClient {
   private static final int PARTICLE_COUNT = 14;
   private static final double RING_RADIUS = 0.4375;
   private static final double RING_OUTWARD_JITTER = 0.04;
   private static final double OUTWARD_SPEED = 0.06;
   private static final float SEAT_SOUND_VOLUME_SCALE = 0.7F;
   private static final float SEAT_SOUND_PITCH = 1.2F;

   private RivetPlacementParticlesClient() {
   }

   static void onAnimationComplete(ClientLevel level, RivetPlacementAnimationClient.SeatFxSnapshot snapshot) {
      BlockState hostState = snapshot.hostBlockState();
      RivetPlacementParticlesClient.FaceFrame frame = faceFrame(snapshot.seatWorld(), snapshot.faceOutwardWorld());
      playHostBreakSound(level, hostState, frame.anchor());
      if (!hostState.isAir() && hostState.shouldSpawnTerrainParticles()) {
         BlockParticleOption options = new BlockParticleOption(ParticleTypes.BLOCK, hostState);
         RandomSource rand = level.random;

         for (int i = 0; i < 14; i++) {
            double theta = (Math.PI * 2) * (double)i / 14.0;
            Vec3 radial = frame.axisU().scale(Math.cos(theta)).add(frame.axisV().scale(Math.sin(theta)));
            double radius = 0.4375 + rand.nextDouble() * 0.04;
            Vec3 pos = frame.anchor().add(radial.scale(radius));
            double speed = 0.06 + rand.nextDouble() * 0.04;
            Vec3 outward = frame.outward();
            level.addAlwaysVisibleParticle(options, pos.x, pos.y, pos.z, outward.x * speed, outward.y * speed, outward.z * speed);
         }
      }
   }

   private static void playHostBreakSound(ClientLevel level, BlockState hostState, Vec3 at) {
      SoundType sound = hostState.isAir() ? Blocks.STONE.defaultBlockState().getSoundType() : hostState.getSoundType();
      Minecraft mc = Minecraft.getInstance();
      mc.getSoundManager()
         .play(
            new SimpleSoundInstance(
               sound.getBreakSound(),
               SoundSource.BLOCKS,
               (sound.getVolume() + 1.0F) / 2.0F * 0.7F,
               sound.getPitch() * 1.2F,
               level.getRandom(),
               at.x,
               at.y,
               at.z
            )
         );
   }

   private static RivetPlacementParticlesClient.FaceFrame faceFrame(Vec3 anchor, Vec3 faceOutwardWorld) {
      Vec3 outward = faceOutwardWorld.normalize();
      Vec3 ref = Math.abs(outward.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
      Vec3 axisU = ref.cross(outward).normalize();
      Vec3 axisV = outward.cross(axisU).normalize();
      return new RivetPlacementParticlesClient.FaceFrame(anchor, outward, axisU, axisV);
   }

   private static record FaceFrame(Vec3 anchor, Vec3 outward, Vec3 axisU, Vec3 axisV) {
   }
}
