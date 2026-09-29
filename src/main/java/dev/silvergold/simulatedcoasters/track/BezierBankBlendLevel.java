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
      return FMLEnvironment.dist != Dist.CLIENT ? null : BezierBankBlendLevel.ClientHolder.get();
   }

   private static final class ClientHolder {
      @Nullable
      static Level get() {
         Minecraft mc = Minecraft.getInstance();
         return mc == null ? null : mc.level;
      }
   }
}
