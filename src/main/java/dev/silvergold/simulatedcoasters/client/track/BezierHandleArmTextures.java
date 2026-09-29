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
public final class BezierHandleArmTextures {
    public static final ResourceLocation DEFAULT = BezierHandleArmTextures.rl("textures/ui/handle_arm.png");
    public static final ResourceLocation HOVER = BezierHandleArmTextures.rl("textures/ui/handle_arm_hover.png");
    public static final ResourceLocation DRAGGING = BezierHandleArmTextures.rl("textures/ui/handle_arm_drag.png");

    private BezierHandleArmTextures() {
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath((String)"simulatedcoasters", (String)path);
    }
}

