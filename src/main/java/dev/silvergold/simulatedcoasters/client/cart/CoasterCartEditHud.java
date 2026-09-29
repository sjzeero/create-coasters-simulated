package dev.silvergold.simulatedcoasters.client.cart;

import com.simibubi.create.AllItems;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleDragManager;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkSync;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CoasterCartEditHud {
   private static boolean actionBarShowing;

   private CoasterCartEditHud() {
   }

   public static boolean shouldShow(Minecraft mc) {
      if (mc.player != null && !mc.options.hideGui && mc.screen == null) {
         if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return false;
         } else if (!CoasterCartEditMode.isActive()) {
            return false;
         } else if (!AllItems.WRENCH.isIn(mc.player.getMainHandItem()) && !AllItems.WRENCH.isIn(mc.player.getOffhandItem())) {
            return false;
         } else {
            UUID cartId = CoasterCartEditMode.getActiveCartId();
            return cartId != null && mc.level != null && CoasterCartResolve.findCartById(mc.level, cartId) != null;
         }
      } else {
         return false;
      }
   }

   public static void clientTick(Minecraft mc) {
      if (!shouldShow(mc)) {
         clearActionBarIfShowing(mc.player);
      } else {
         UUID cartId = CoasterCartEditMode.getActiveCartId();
         if (cartId != null && mc.level != null && mc.player != null) {
            List<CoasterCartTrainLinkSync.LinkEntry> links = CoasterCartTrainLinkClientState.linksFor(mc.level.dimension(), cartId);
            MutableComponent topRow = buildActionBarLine(links);
            if (topRow == null) {
               clearActionBarIfShowing(mc.player);
            } else {
               mc.player.displayClientMessage(topRow, true);
               actionBarShowing = true;
            }
         }
      }
   }

   public static void render(Minecraft mc, GuiGraphics guiGraphics) {
      if (shouldShow(mc)) {
         UUID cartId = CoasterCartEditMode.getActiveCartId();
         if (cartId != null && mc.level != null) {
            List<CoasterCartTrainLinkSync.LinkEntry> links = CoasterCartTrainLinkClientState.linksFor(mc.level.dimension(), cartId);
            renderBottomHintRow(mc, guiGraphics, links.isEmpty());
         }
      }
   }

   public static void clearOnLevelUnload() {
      actionBarShowing = false;
   }

   @Nullable
   private static MutableComponent buildActionBarLine(List<CoasterCartTrainLinkSync.LinkEntry> links) {
      if (links.isEmpty()) {
         return null;
      } else {
         int whiteRgb = 16777215;
         Style styleWhite = Style.EMPTY.withColor(TextColor.fromRgb(whiteRgb));
         if (links.size() == 1) {
            String meters = formatLinkMeters(links.getFirst().centerDistance());
            return Component.translatable("simulatedcoasters.cart.edit_link_distance", new Object[]{meters}).withStyle(styleWhite);
         } else {
            String metersA = formatLinkMeters(links.get(0).centerDistance());
            String metersB = formatLinkMeters(links.get(1).centerDistance());
            return Component.empty()
               .append(Component.translatable("simulatedcoasters.cart.edit_link_a", new Object[]{metersA}).withStyle(styleWhite))
               .append(Component.translatable("simulatedcoasters.track.bezier_edit_anchor_status_sep").withStyle(styleWhite))
               .append(Component.translatable("simulatedcoasters.cart.edit_link_b", new Object[]{metersB}).withStyle(styleWhite));
         }
      }
   }

   private static String formatLinkMeters(double centerDistanceBlocks) {
      return CoasterBezierHandleEdit.formatLiftMetersReadout((float)centerDistanceBlocks);
   }

   private static void renderBottomHintRow(Minecraft mc, GuiGraphics guiGraphics, boolean noConnections) {
      int whiteRgb = 16777215;
      MutableComponent hint = noConnections
         ? Component.translatable("simulatedcoasters.cart.edit_no_connections")
         : Component.translatable("simulatedcoasters.cart.edit_extend_shorten_hint");
      hint = hint.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(whiteRgb)));
      Font font = mc.font;
      int w = font.width(hint);
      int x = (guiGraphics.guiWidth() - w) / 2;
      int y = BezierHandleDragManager.editHudRowAboveHotbarY(mc, guiGraphics);
      guiGraphics.drawStringWithBackdrop(font, hint, x, y, w, ARGB32.color(255, whiteRgb));
   }

   private static void clearActionBarIfShowing(@Nullable LocalPlayer player) {
      if (player != null && actionBarShowing) {
         player.displayClientMessage(Component.empty(), true);
         actionBarShowing = false;
      }
   }
}
