/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.util.RandomSource
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.track;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class BezierHandleClientSounds {
    public static final float COMMIT_SUCCESS_VOLUME = 0.38f;
    public static final float COMMIT_SUCCESS_PITCH_MIN = 0.62f;
    public static final float COMMIT_SUCCESS_PITCH_RANGE = 0.06f;

    private BezierHandleClientSounds() {
    }

    public static void playCreateUi(SoundEvent sound, float volume, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || sound == null) {
            return;
        }
        mc.getSoundManager().play((SoundInstance)SimpleSoundInstance.forUI((SoundEvent)sound, (float)pitch, (float)volume));
    }

    public static void playCreateDeny() {
        BezierHandleClientSounds.playCreateUi((SoundEvent)SoundEvents.NOTE_BLOCK_BASS.value(), 1.0f, 0.5f);
    }

    public static float commitSuccessPitch(RandomSource random) {
        return 0.62f + random.nextFloat() * 0.06f;
    }

    public static void playCommitSuccessSound(Minecraft mc) {
        if (mc.level == null) {
            return;
        }
        BezierHandleClientSounds.playCreateUi(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.38f, BezierHandleClientSounds.commitSuccessPitch(mc.level.getRandom()));
    }
}

