/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class PickInteractionGates {
    public static boolean destroyKeyDown;

    private PickInteractionGates() {
    }

    public static boolean isDestroyInputDown(@Nullable Minecraft mc) {
        return mc != null && mc.options != null && mc.options.keyAttack.isDown();
    }

    public static void clientTick(Minecraft mc) {
        destroyKeyDown = PickInteractionGates.isDestroyInputDown(mc);
    }
}

