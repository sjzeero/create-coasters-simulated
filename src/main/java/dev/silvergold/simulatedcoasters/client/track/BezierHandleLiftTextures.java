/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceLocation
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.track;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class BezierHandleLiftTextures {
    public static final ResourceLocation DEFAULT = BezierHandleLiftTextures.rl("textures/ui/lift_handle.png");
    public static final ResourceLocation HOVER = BezierHandleLiftTextures.rl("textures/ui/lift_handle_hover.png");
    public static final ResourceLocation DRAGGING = BezierHandleLiftTextures.rl("textures/ui/lift_handle_drag.png");

    private BezierHandleLiftTextures() {
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)path);
    }
}

