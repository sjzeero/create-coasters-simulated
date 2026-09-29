package dev.silvergold.simulatedcoasters.client.item;

import com.simibubi.create.AllKeys;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleDragManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class RivetPlacementHud {
   private static boolean actionBarShowing;

   private RivetPlacementHud() {
   }

   public static boolean shouldShow(Minecraft mc) {
      if (mc.player != null && !mc.options.hideGui && mc.screen == null) {
         return mc.gameMode.getPlayerMode() == GameType.SPECTATOR ? false : holdingRivet(mc);
      } else {
         return false;
      }
   }

   public static void clientTick(Minecraft mc) {
      if (!shouldShow(mc)) {
         clearActionBarIfShowing(mc.player);
      } else if (mc.player != null) {
         int angle = RivetPlacementOutlineClient.displayPreviewYRotationDegrees(mc);
         MutableComponent topRow = Component.translatable("simulatedcoasters.rivet.placement_angle", new Object[]{angle})
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(16777215)));
         mc.player.displayClientMessage(topRow, true);
         actionBarShowing = true;
      }
   }

   public static void render(Minecraft mc, GuiGraphics guiGraphics) {
      if (shouldShow(mc)) {
         renderBottomHintRow(mc, guiGraphics);
      }
   }

   public static void clearOnLevelUnload() {
      actionBarShowing = false;
   }

   private static void renderBottomHintRow(Minecraft mc, GuiGraphics guiGraphics) {
      int blueRgb = 11781103;
      int whiteRgb = 16777215;
      boolean freePlace = AllKeys.ctrlDown();
      boolean fineRotation = RivetPlacementOutlineClient.isFineRotationActive();
      int ctrlKeyRgb = freePlace ? whiteRgb : blueRgb;
      int altKeyRgb = fineRotation ? whiteRgb : blueRgb;
      Style ctrlKeyStyle = Style.EMPTY.withColor(TextColor.fromRgb(ctrlKeyRgb));
      Style altKeyStyle = Style.EMPTY.withColor(TextColor.fromRgb(altKeyRgb));
      Style blueStyle = Style.EMPTY.withColor(TextColor.fromRgb(blueRgb));
      Style whiteStyle = Style.EMPTY.withColor(TextColor.fromRgb(whiteRgb));
      MutableComponent bracketedCtrl = bracketedKey(AllKeys.CTRL_MODIFIER.getBoundKey(), ctrlKeyStyle);
      MutableComponent bracketedAlt = bracketedKey(AllKeys.ALT_MODIFIER.getBoundKey(), altKeyStyle);
      MutableComponent hint = Component.empty()
         .append(Component.translatable("simulatedcoasters.rivet.placement_controls_free", new Object[]{bracketedCtrl}).withStyle(blueStyle))
         .append(Component.translatable("simulatedcoasters.track.bezier_edit_anchor_status_sep").withStyle(whiteStyle))
         .append(Component.translatable("simulatedcoasters.rivet.placement_controls_rotate", new Object[]{bracketedAlt}).withStyle(blueStyle));
      Font font = mc.font;
      int w = font.width(hint);
      int x = (guiGraphics.guiWidth() - w) / 2;
      int y = BezierHandleDragManager.editHudRowAboveHotbarY(mc, guiGraphics);
      guiGraphics.drawStringWithBackdrop(font, hint, x, y, w, ARGB32.color(255, blueRgb));
   }

   private static MutableComponent bracketedKey(String keyLabel, Style keyStyle) {
      return Component.literal("[")
         .withStyle(keyStyle)
         .append(Component.literal(keyLabel).withStyle(keyStyle))
         .append(Component.literal("]").withStyle(keyStyle));
   }

   private static boolean holdingRivet(Minecraft mc) {
      Item rivet = (Item)SimulatedCoasters.RIVET.get();
      return mc.player.getMainHandItem().is(rivet) || mc.player.getOffhandItem().is(rivet);
   }

   private static void clearActionBarIfShowing(@Nullable LocalPlayer player) {
      if (player != null && actionBarShowing) {
         player.displayClientMessage(Component.empty(), true);
         actionBarShowing = false;
      }
   }
}
