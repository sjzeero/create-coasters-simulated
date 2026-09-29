/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.core.particles.BlockParticleOption
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.rivet;

import dev.silvergold.simulatedcoasters.client.rivet.RivetPlacementAnimationClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class RivetPlacementParticlesClient {
    private static final int PARTICLE_COUNT = 14;
    private static final double RING_RADIUS = 0.4375;
    private static final double RING_OUTWARD_JITTER = 0.04;
    private static final double OUTWARD_SPEED = 0.06;
    private static final float SEAT_SOUND_VOLUME_SCALE = 0.7f;
    private static final float SEAT_SOUND_PITCH = 1.2f;

    private RivetPlacementParticlesClient() {
    }

    static void onAnimationComplete(ClientLevel level, RivetPlacementAnimationClient.SeatFxSnapshot snapshot) {
        BlockState hostState = snapshot.hostBlockState();
        FaceFrame frame = RivetPlacementParticlesClient.faceFrame(snapshot.seatWorld(), snapshot.faceOutwardWorld());
        RivetPlacementParticlesClient.playHostBreakSound(level, hostState, frame.anchor());
        if (hostState.isAir() || !hostState.shouldSpawnTerrainParticles()) {
            return;
        }
        BlockParticleOption options = new BlockParticleOption(ParticleTypes.BLOCK, hostState);
        RandomSource rand = level.random;
        for (int i = 0; i < 14; ++i) {
            double theta = Math.PI * 2 * (double)i / 14.0;
            Vec3 radial = frame.axisU().scale(Math.cos(theta)).add(frame.axisV().scale(Math.sin(theta)));
            double radius = 0.4375 + rand.nextDouble() * 0.04;
            Vec3 pos = frame.anchor().add(radial.scale(radius));
            double speed = 0.06 + rand.nextDouble() * 0.04;
            Vec3 outward = frame.outward();
            level.addAlwaysVisibleParticle((ParticleOptions)options, pos.x, pos.y, pos.z, outward.x * speed, outward.y * speed, outward.z * speed);
        }
    }

    private static void playHostBreakSound(ClientLevel level, BlockState hostState, Vec3 at) {
        SoundType sound = hostState.isAir() ? Blocks.STONE.defaultBlockState().getSoundType() : hostState.getSoundType();
        Minecraft mc = Minecraft.getInstance();
        mc.getSoundManager().play((SoundInstance)new SimpleSoundInstance(sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0f) / 2.0f * 0.7f, sound.getPitch() * 1.2f, level.getRandom(), at.x, at.y, at.z));
    }

    private static FaceFrame faceFrame(Vec3 anchor, Vec3 faceOutwardWorld) {
        Vec3 outward = faceOutwardWorld.normalize();
        Vec3 ref = Math.abs(outward.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 axisU = ref.cross(outward).normalize();
        Vec3 axisV = outward.cross(axisU).normalize();
        return new FaceFrame(anchor, outward, axisU, axisV);
    }

    private record FaceFrame(Vec3 anchor, Vec3 outward, Vec3 axisU, Vec3 axisV) {
    }
}

