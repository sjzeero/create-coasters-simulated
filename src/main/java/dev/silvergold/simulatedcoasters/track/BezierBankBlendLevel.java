/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.world.level.Level
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.fml.loading.FMLEnvironment
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

public final class BezierBankBlendLevel {
    private BezierBankBlendLevel() {
    }

    @Nullable
    public static Level tryGetClientLevel() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return null;
        }
        return ClientHolder.get();
    }

    private static final class ClientHolder {
        private ClientHolder() {
        }

        @Nullable
        static Level get() {
            Minecraft mc = Minecraft.getInstance();
            return mc == null ? null : mc.level;
        }
    }
}

