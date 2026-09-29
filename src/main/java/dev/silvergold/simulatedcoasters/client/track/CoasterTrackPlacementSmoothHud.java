package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CoasterTrackPlacementSmoothHud {
   private static boolean hintActive;

   private CoasterTrackPlacementSmoothHud() {
   }

   public static void setHintActive(boolean active) {
      hintActive = active;
   }

   public static boolean shouldShow() {
      return hintActive;
   }

   public static void render(Minecraft mc, GuiGraphics guiGraphics) {
      if (shouldShow() && mc.player != null && !mc.options.hideGui) {
         if (mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
            int y = BezierHandleDragManager.editHudRowAboveHotbarY(mc, guiGraphics);
            int blueRgb = 11781103;
            int keyRgb = CoasterTrackPlacementNeighborSmoothSuppressClient.isSuppressNeighborSmoothing() ? 16777215 : blueRgb;
            Style keyStyle = Style.EMPTY.withColor(TextColor.fromRgb(keyRgb));
            MutableComponent bracketedKey = Component.literal("[")
               .withStyle(keyStyle)
               .append(Component.literal(AllKeys.ALT_MODIFIER.getBoundKey()).withStyle(keyStyle))
               .append(Component.literal("]").withStyle(keyStyle));
            MutableComponent hint = Component.translatable("simulatedcoasters.track.placement_neighbor_smooth_hint", new Object[]{bracketedKey})
               .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(blueRgb)));
            Font font = mc.font;
            int w = font.width(hint);
            int x = (guiGraphics.guiWidth() - w) / 2;
            guiGraphics.drawStringWithBackdrop(font, hint, x, y, w, ARGB32.color(255, blueRgb));
         }
      }
   }
}
