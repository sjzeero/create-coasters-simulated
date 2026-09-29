package dev.silvergold.simulatedcoasters.track;

import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;

public final class CoasterCurveTintColors {
   private static final float VISUAL_DESATURATION = 0.14F;

   private CoasterCurveTintColors() {
   }

   public static int diffuseRgbFromDye(DyeColor dye) {
      return dye.getTextureDiffuseColor() & 16777215;
   }

   public static int diffuseRgbForDyePreviewOutline(DyeColor dye) {
      return dye == DyeColor.BLACK ? diffuseRgbFromDye(dye) : boostDarkDiffuseForPreview(diffuseRgbFromDye(dye));
   }

   private static int boostDarkDiffuseForPreview(int rgb) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      float rf = (float)r / 255.0F;
      float gf = (float)g / 255.0F;
      float bf = (float)b / 255.0F;
      float luma = 0.2126F * rf + 0.7152F * gf + 0.0722F * bf;
      float minLuma = 0.52F;
      if (!(luma >= minLuma) && !(luma < 1.0E-6F)) {
         float scale = minLuma / luma;
         int r2 = Mth.clamp(Mth.floor(rf * scale * 255.0F), 0, 255);
         int g2 = Mth.clamp(Mth.floor(gf * scale * 255.0F), 0, 255);
         int b2 = Mth.clamp(Mth.floor(bf * scale * 255.0F), 0, 255);
         return r2 << 16 | g2 << 8 | b2;
      } else {
         return rgb;
      }
   }

   public static int diffuseRgbWithVisualMuting(int diffuseRgbPacked) {
      return mutedDiffuseRgb(diffuseRgbPacked & 16777215);
   }

   public static int vertexColorMultiplierRgb(int diffuseRgb) {
      return mutedDiffuseRgb(diffuseRgb & 16777215) & 16777215;
   }

   public static int beamVertexColorMultiplierRgb(int diffuseRgb) {
      return beamDiffuseRgbOnly(mutedDiffuseRgb(diffuseRgb & 16777215)) & 16777215;
   }

   private static int mutedDiffuseRgb(int rgb) {
      if ((rgb & 16777215) == 0) {
         return 0;
      } else {
         int r = rgb >> 16 & 0xFF;
         int g = rgb >> 8 & 0xFF;
         int b = rgb & 0xFF;
         float y = 0.2126F * (float)r + 0.7152F * (float)g + 0.0722F * (float)b;
         int r2 = Mth.clamp(Mth.floor(Mth.lerp(0.14F, (float)r, y)), 0, 255);
         int g2 = Mth.clamp(Mth.floor(Mth.lerp(0.14F, (float)g, y)), 0, 255);
         int b2 = Mth.clamp(Mth.floor(Mth.lerp(0.14F, (float)b, y)), 0, 255);
         return r2 << 16 | g2 << 8 | b2;
      }
   }

   private static int beamDiffuseRgbOnly(int diffuseRgb) {
      int r = diffuseRgb >> 16 & 0xFF;
      int g = diffuseRgb >> 8 & 0xFF;
      int b = diffuseRgb & 0xFF;
      float rf = (float)r / 255.0F;
      float gf = (float)g / 255.0F;
      float bf = (float)b / 255.0F;
      float w = 0.1F;
      rf = Mth.lerp(w, rf, 0.48F);
      gf = Mth.lerp(w, gf, 0.56F);
      bf = Mth.lerp(w, bf, 0.82F);
      return Mth.clamp(Mth.floor(rf * 255.0F), 0, 255) << 16 | Mth.clamp(Mth.floor(gf * 255.0F), 0, 255) << 8 | Mth.clamp(Mth.floor(bf * 255.0F), 0, 255);
   }
}
