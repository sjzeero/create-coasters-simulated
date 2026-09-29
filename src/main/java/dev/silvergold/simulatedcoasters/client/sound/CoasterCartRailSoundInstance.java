package dev.silvergold.simulatedcoasters.client.sound;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientFrameCache;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartRailSoundInstance extends AbstractTickableSoundInstance {
   private static final float PITCH_FLOOR = 0.5F;
   private static final float PITCH_AT_SLOW = 0.65F;
   private static final float PITCH_AT_FAST = 1.0F;
   private static final float SPEED_FOR_MIN_PITCH_BS = 1.0F;
   private static final float SPEED_FOR_MAX_PITCH_BS = 16.0F;
   private static final float VOLUME_MAX = 0.5F;
   private static final int ATTENUATION_DISTANCE = 42;
   private static final float VOLUME_REF_SPEED_BS = 10.0F;
   private static final float MIN_ROLL_SPEED_BS = 0.1F;
   private static final float PITCH_SMOOTH = 0.05F;
   private static final float VOLUME_SMOOTH = 0.18F;
   private static final float STOP_DECAY = 0.28F;
   private final SubLevel sub;
   private final BlockPos bearingPlotPos;
   private float pitchSmoothed;
   private float volumeSmoothed;
   @Nullable
   private Vec3 prevBearingWorld;

   public CoasterCartRailSoundInstance(SubLevel sub, BlockPos bearingPlotPos) {
      super(SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
      this.sub = sub;
      this.bearingPlotPos = bearingPlotPos;
      this.looping = true;
      this.delay = 0;
      this.volume = 0.0F;
      this.pitch = 0.65F;
      this.pitchSmoothed = 0.65F;
      this.volumeSmoothed = 0.0F;
      Vec3 w = sub.logicalPose().transformPosition(Vec3.atCenterOf(bearingPlotPos));
      this.x = w.x;
      this.y = w.y;
      this.z = w.z;
   }

   public WeighedSoundEvents resolve(SoundManager manager) {
      WeighedSoundEvents events = super.resolve(manager);
      Sound base = this.sound;
      if (base != null && base != SoundManager.EMPTY_SOUND && base != SoundManager.INTENTIONALLY_EMPTY_SOUND && base.getAttenuationDistance() < 42) {
         this.sound = new Sound(
            base.getLocation(), base.getVolume(), base.getPitch(), base.getWeight(), base.getType(), base.shouldStream(), base.shouldPreload(), 42
         );
      }

      return events;
   }

   public boolean canPlaySound() {
      return true;
   }

   public boolean canStartSilent() {
      return true;
   }

   public void tick() {
      if (this.sub.isRemoved()) {
         this.stop();
      } else {
         Minecraft mc = Minecraft.getInstance();
         Level level = mc.level;
         if (level instanceof ClientLevel cl) {
            CoasterCartClientFrameCache.SoundFrame soundFrame = CoasterCartClientFrameCache.resolveSound(
               cl, this.sub, CoasterPathGraphManagerClient.get(level), CoasterCartRailSoundCache.openPathEndRefs()
            );
            Vec3 bearingWorld = soundFrame != null
               ? soundFrame.bearingWorldLogical()
               : this.sub.logicalPose().transformPosition(Vec3.atCenterOf(this.bearingPlotPos));
            this.x = bearingWorld.x;
            this.y = bearingWorld.y;
            this.z = bearingWorld.z;
            boolean onRail = soundFrame != null && soundFrame.onRail();
            Vec3 bearingLocalCenter = Vec3.atCenterOf(this.bearingPlotPos);
            Vec3 velBs = Sable.HELPER.getVelocity(level, this.sub, bearingLocalCenter);
            if (velBs.lengthSqr() < 1.0E-10 && this.prevBearingWorld != null) {
               velBs = bearingWorld.subtract(this.prevBearingWorld).scale(20.0);
            }

            this.prevBearingWorld = bearingWorld;
            float alongTrackSpeedBs = this.alongTrackSpeedBlocksPerSecond(velBs);
            boolean rolling = onRail && alongTrackSpeedBs >= 0.1F && level.tickRateManager().runsNormally();
            if (rolling) {
               float pitchTarget = pitchForSpeed(alongTrackSpeedBs);
               float volumeTarget = volumeForSpeed(alongTrackSpeedBs);
               this.pitchSmoothed = Mth.clamp(Mth.lerp(0.05F, this.pitchSmoothed, pitchTarget), 0.5F, 1.0F);
               this.volumeSmoothed = Mth.lerp(0.18F, this.volumeSmoothed, volumeTarget);
            } else {
               this.volumeSmoothed = Mth.lerp(0.28F, this.volumeSmoothed, 0.0F);
               this.pitchSmoothed = Mth.clamp(this.pitchSmoothed, 0.5F, 1.0F);
            }

            this.pitch = this.pitchSmoothed;
            this.volume = this.volumeSmoothed;
         } else {
            this.stop();
         }
      }
   }

   private Vec3 bodyForwardWorld() {
      Quaterniond q = this.sub.logicalPose().orientation();
      Vector3f v = new Vector3f(0.0F, 0.0F, -1.0F);
      new Quaternionf((float)q.x(), (float)q.y(), (float)q.z(), (float)q.w()).transform(v);
      return new Vec3((double)v.x, (double)v.y, (double)v.z);
   }

   private float alongTrackSpeedBlocksPerSecond(Vec3 velocityBs) {
      Vec3 forward = this.bodyForwardWorld();
      float along = (float)velocityBs.dot(forward);
      return Math.abs(along) >= 1.0E-5F ? Math.abs(along) : (float)velocityBs.horizontalDistance();
   }

   private static float pitchForSpeed(float speedBs) {
      float t = Mth.clamp((speedBs - 1.0F) / 15.0F, 0.0F, 1.0F);
      t = Mth.sqrt(t);
      return Mth.clamp(Mth.lerp(t, 0.65F, 1.0F), 0.5F, 1.0F);
   }

   private static float volumeForSpeed(float speedBs) {
      float t = Mth.clamp(speedBs / 10.0F, 0.0F, 1.0F);
      return t * t * 0.5F;
   }
}
