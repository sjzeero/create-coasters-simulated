/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.resources.sounds.SimpleSoundInstance
 *  net.minecraft.client.resources.sounds.SoundInstance
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.track;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class ChainLiftPlacementClientSounds {
    private static final float SNAP_ENGAGE_VOLUME = 0.4f;
    private static final float SNAP_ENGAGE_PITCH = 1.12f;

    private ChainLiftPlacementClientSounds() {
    }

    public static void playStartAnchorSelect(Minecraft mc, Vec3 worldPos) {
        if (mc.level == null) {
            return;
        }
        mc.getSoundManager().play((SoundInstance)new SimpleSoundInstance(SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.4f, 1.12f, mc.level.getRandom(), worldPos.x, worldPos.y, worldPos.z));
    }
}

