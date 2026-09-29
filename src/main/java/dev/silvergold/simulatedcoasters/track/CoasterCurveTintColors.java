/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.DyeColor
 */
package dev.silvergold.simulatedcoasters.track;

import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public final class CoasterCurveTintColors {
    private static final float VISUAL_DESATURATION = 0.14f;

    private CoasterCurveTintColors() {
    }

    public static int diffuseRgbFromDye(DyeColor dye) {
        return dye.getTextureDiffuseColor() & 0xFFFFFF;
    }

    public static int diffuseRgbForDyePreviewOutline(DyeColor dye) {
        if (dye == DyeColor.BLACK) {
            return CoasterCurveTintColors.diffuseRgbFromDye(dye);
        }
        return CoasterCurveTintColors.boostDarkDiffuseForPreview(CoasterCurveTintColors.diffuseRgbFromDye(dye));
    }

    private static int boostDarkDiffuseForPreview(int rgb) {
        int r = rgb >> 16 & 0xFF;
        float rf = (float)r / 255.0f;
        int g = rgb >> 8 & 0xFF;
        float gf = (float)g / 255.0f;
        int b = rgb & 0xFF;
        float bf = (float)b / 255.0f;
        float luma = 0.2126f * rf + 0.7152f * gf + 0.0722f * bf;
        float minLuma = 0.52f;
        if (luma >= minLuma || luma < 1.0E-6f) {
            return rgb;
        }
        float scale = minLuma / luma;
        int r2 = Mth.clamp((int)Mth.floor((float)(rf * scale * 255.0f)), (int)0, (int)255);
        int g2 = Mth.clamp((int)Mth.floor((float)(gf * scale * 255.0f)), (int)0, (int)255);
        int b2 = Mth.clamp((int)Mth.floor((float)(bf * scale * 255.0f)), (int)0, (int)255);
        return r2 << 16 | g2 << 8 | b2;
    }

    public static int diffuseRgbWithVisualMuting(int diffuseRgbPacked) {
        return CoasterCurveTintColors.mutedDiffuseRgb(diffuseRgbPacked & 0xFFFFFF);
    }

    public static int vertexColorMultiplierRgb(int diffuseRgb) {
        return CoasterCurveTintColors.mutedDiffuseRgb(diffuseRgb & 0xFFFFFF) & 0xFFFFFF;
    }

    public static int beamVertexColorMultiplierRgb(int diffuseRgb) {
        return CoasterCurveTintColors.beamDiffuseRgbOnly(CoasterCurveTintColors.mutedDiffuseRgb(diffuseRgb & 0xFFFFFF)) & 0xFFFFFF;
    }

    private static int mutedDiffuseRgb(int rgb) {
        if ((rgb & 0xFFFFFF) == 0) {
            return 0;
        }
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        float y = 0.2126f * (float)r + 0.7152f * (float)g + 0.0722f * (float)b;
        int r2 = Mth.clamp((int)Mth.floor((float)Mth.lerp((float)0.14f, (float)r, (float)y)), (int)0, (int)255);
        int g2 = Mth.clamp((int)Mth.floor((float)Mth.lerp((float)0.14f, (float)g, (float)y)), (int)0, (int)255);
        int b2 = Mth.clamp((int)Mth.floor((float)Mth.lerp((float)0.14f, (float)b, (float)y)), (int)0, (int)255);
        return r2 << 16 | g2 << 8 | b2;
    }

    private static int beamDiffuseRgbOnly(int diffuseRgb) {
        int r = diffuseRgb >> 16 & 0xFF;
        int g = diffuseRgb >> 8 & 0xFF;
        int b = diffuseRgb & 0xFF;
        float rf = (float)r / 255.0f;
        float gf = (float)g / 255.0f;
        float bf = (float)b / 255.0f;
        float w = 0.1f;
        rf = Mth.lerp((float)w, (float)rf, (float)0.48f);
        gf = Mth.lerp((float)w, (float)gf, (float)0.56f);
        bf = Mth.lerp((float)w, (float)bf, (float)0.82f);
        return Mth.clamp((int)Mth.floor((float)(rf * 255.0f)), (int)0, (int)255) << 16 | Mth.clamp((int)Mth.floor((float)(gf * 255.0f)), (int)0, (int)255) << 8 | Mth.clamp((int)Mth.floor((float)(bf * 255.0f)), (int)0, (int)255);
    }
}

