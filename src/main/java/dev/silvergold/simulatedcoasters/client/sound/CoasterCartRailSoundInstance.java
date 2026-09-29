/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.companion.SubLevelAccess
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
 *  net.minecraft.client.resources.sounds.Sound
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.client.sounds.SoundManager
 *  net.minecraft.client.sounds.WeighedSoundEvents
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 */
package dev.silvergold.simulatedcoasters.client.sound;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientFrameCache;
import dev.silvergold.simulatedcoasters.client.sound.CoasterCartRailSoundCache;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartRailSoundInstance
extends AbstractTickableSoundInstance {
    private static final float PITCH_FLOOR = 0.5f;
    private static final float PITCH_AT_SLOW = 0.65f;
    private static final float PITCH_AT_FAST = 1.0f;
    private static final float SPEED_FOR_MIN_PITCH_BS = 1.0f;
    private static final float SPEED_FOR_MAX_PITCH_BS = 16.0f;
    private static final float VOLUME_MAX = 0.5f;
    private static final int ATTENUATION_DISTANCE = 42;
    private static final float VOLUME_REF_SPEED_BS = 10.0f;
    private static final float MIN_ROLL_SPEED_BS = 0.1f;
    private static final float PITCH_SMOOTH = 0.05f;
    private static final float VOLUME_SMOOTH = 0.18f;
    private static final float STOP_DECAY = 0.28f;
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
        this.volume = 0.0f;
        this.pitch = 0.65f;
        this.pitchSmoothed = 0.65f;
        this.volumeSmoothed = 0.0f;
        Vec3 w = sub.logicalPose().transformPosition(Vec3.atCenterOf((Vec3i)bearingPlotPos));
        this.x = w.x;
        this.y = w.y;
        this.z = w.z;
    }

    public WeighedSoundEvents resolve(SoundManager manager) {
        WeighedSoundEvents events = super.resolve(manager);
        Sound base = this.sound;
        if (base != null && base != SoundManager.EMPTY_SOUND && base != SoundManager.INTENTIONALLY_EMPTY_SOUND && base.getAttenuationDistance() < 42) {
            this.sound = new Sound(base.getLocation(), base.getVolume(), base.getPitch(), base.getWeight(), base.getType(), base.shouldStream(), base.shouldPreload(), 42);
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
        boolean rolling;
        if (this.sub.isRemoved()) {
            this.stop();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (!(level instanceof ClientLevel)) {
            this.stop();
            return;
        }
        ClientLevel cl = level;
        CoasterCartClientFrameCache.SoundFrame soundFrame = CoasterCartClientFrameCache.resolveSound(cl, this.sub, CoasterPathGraphManagerClient.get((Level)level), CoasterCartRailSoundCache.openPathEndRefs());
        Vec3 bearingWorld = soundFrame != null ? soundFrame.bearingWorldLogical() : this.sub.logicalPose().transformPosition(Vec3.atCenterOf((Vec3i)this.bearingPlotPos));
        this.x = bearingWorld.x;
        this.y = bearingWorld.y;
        this.z = bearingWorld.z;
        boolean onRail = soundFrame != null && soundFrame.onRail();
        Vec3 bearingLocalCenter = Vec3.atCenterOf((Vec3i)this.bearingPlotPos);
        Vec3 velBs = Sable.HELPER.getVelocity((Level)level, (SubLevelAccess)this.sub, bearingLocalCenter);
        if (velBs.lengthSqr() < 1.0E-10 && this.prevBearingWorld != null) {
            velBs = bearingWorld.subtract(this.prevBearingWorld).scale(20.0);
        }
        this.prevBearingWorld = bearingWorld;
        float alongTrackSpeedBs = this.alongTrackSpeedBlocksPerSecond(velBs);
        boolean bl = rolling = onRail && alongTrackSpeedBs >= 0.1f && level.tickRateManager().runsNormally();
        if (rolling) {
            float pitchTarget = CoasterCartRailSoundInstance.pitchForSpeed(alongTrackSpeedBs);
            float volumeTarget = CoasterCartRailSoundInstance.volumeForSpeed(alongTrackSpeedBs);
            this.pitchSmoothed = Mth.clamp((float)Mth.lerp((float)0.05f, (float)this.pitchSmoothed, (float)pitchTarget), (float)0.5f, (float)1.0f);
            this.volumeSmoothed = Mth.lerp((float)0.18f, (float)this.volumeSmoothed, (float)volumeTarget);
        } else {
            this.volumeSmoothed = Mth.lerp((float)0.28f, (float)this.volumeSmoothed, (float)0.0f);
            this.pitchSmoothed = Mth.clamp((float)this.pitchSmoothed, (float)0.5f, (float)1.0f);
        }
        this.pitch = this.pitchSmoothed;
        this.volume = this.volumeSmoothed;
    }

    private Vec3 bodyForwardWorld() {
        Quaterniond q = this.sub.logicalPose().orientation();
        Vector3f v = new Vector3f(0.0f, 0.0f, -1.0f);
        new Quaternionf((float)q.x(), (float)q.y(), (float)q.z(), (float)q.w()).transform(v);
        return new Vec3((double)v.x, (double)v.y, (double)v.z);
    }

    private float alongTrackSpeedBlocksPerSecond(Vec3 velocityBs) {
        Vec3 forward = this.bodyForwardWorld();
        float along = (float)velocityBs.dot(forward);
        if (Math.abs(along) >= 1.0E-5f) {
            return Math.abs(along);
        }
        return (float)velocityBs.horizontalDistance();
    }

    private static float pitchForSpeed(float speedBs) {
        float t = Mth.clamp((float)((speedBs - 1.0f) / 15.0f), (float)0.0f, (float)1.0f);
        t = Mth.sqrt((float)t);
        return Mth.clamp((float)Mth.lerp((float)t, (float)0.65f, (float)1.0f), (float)0.5f, (float)1.0f);
    }

    private static float volumeForSpeed(float speedBs) {
        float t = Mth.clamp((float)(speedBs / 10.0f), (float)0.0f, (float)1.0f);
        return t * t * 0.5f;
    }
}

