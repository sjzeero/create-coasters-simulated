/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.particles.DustParticleOptions
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.joml.Vector3f
 */
package dev.silvergold.simulatedcoasters.client.cart;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartLinkPreviewParticles {
    private static final float DUST_SCALE = 1.0f;

    private CoasterCartLinkPreviewParticles() {
    }

    public static void spawnSpan(ClientLevel level, Vec3 from, Vec3 to) {
        DustParticleOptions dust = CoasterCartLinkPreviewParticles.displayLinkBlueDust();
        int segments = Math.max(4, (int)Math.floor(from.distanceTo(to)) + 1);
        for (int i = 0; i < segments; ++i) {
            Vec3 vec = from.lerp(to, (double)level.random.nextFloat());
            level.addParticle((ParticleOptions)dust, vec.x, vec.y, vec.z, 0.0, 0.0, 0.0);
        }
    }

    private static DustParticleOptions displayLinkBlueDust() {
        int rgb = 1940687;
        return new DustParticleOptions(new Vector3f((float)(rgb >> 16 & 0xFF) / 255.0f, (float)(rgb >> 8 & 0xFF) / 255.0f, (float)(rgb & 0xFF) / 255.0f), 1.0f);
    }
}

