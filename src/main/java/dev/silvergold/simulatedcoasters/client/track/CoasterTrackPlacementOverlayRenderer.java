package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;

public final class CoasterTrackPlacementOverlayRenderer {
   public static final Layer OVERLAY = CoasterTrackPlacementOverlayRenderer::renderOverlay;
   private static boolean active;
   private static int requiredTracks;
   private static boolean hasRequiredTracks;
   private static boolean validPreview;

   private CoasterTrackPlacementOverlayRenderer() {
   }

   public static void updateFromPlacementPreview(Minecraft mc, int tracksRequired, boolean hasEnoughTracks, boolean validPreview) {
      if (mc.player != null && mc.gameMode != null) {
         if (mc.gameMode.getPlayerMode() != GameType.SURVIVAL) {
            clear();
         } else if (tracksRequired <= 0) {
            clear();
         } else {
            active = true;
            requiredTracks = tracksRequired;
            hasRequiredTracks = hasEnoughTracks;
            CoasterTrackPlacementOverlayRenderer.validPreview = validPreview;
         }
      } else {
         clear();
      }
   }

   public static void clear() {
      active = false;
      requiredTracks = 0;
      hasRequiredTracks = true;
      validPreview = true;
   }

   public static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui && mc.screen == null) {
         if (active && requiredTracks > 0) {
            List<Pair<ItemStack, Boolean>> slots = new ArrayList<>();
            int remaining = requiredTracks;

            for (ItemStack icon = new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get()); remaining > 0; remaining -= 64) {
               slots.add(Pair.of(icon.copyWithCount(Math.min(64, remaining)), hasRequiredTracks));
            }

            int w = 21 * slots.size();
            int x = (guiGraphics.guiWidth() - w) / 2;
            int y = guiGraphics.guiHeight() - 100;

            for (Pair<ItemStack, Boolean> slot : slots) {
               RenderSystem.enableBlend();
               (slot.getSecond() ? AllGuiTextures.HOTSLOT_ACTIVE : AllGuiTextures.HOTSLOT).render(guiGraphics, x, y);
               ItemStack stack = (ItemStack)slot.getFirst();
               String count = null;
               if (!(Boolean)slot.getSecond()) {
                  count = ChatFormatting.GOLD.toString() + stack.getCount();
               } else if (!validPreview) {
                  count = ChatFormatting.GRAY.toString() + stack.getCount();
               }

               BlueprintOverlayRenderer.drawItemStack(guiGraphics, mc, x, y, stack, count);
               x += 21;
            }

            RenderSystem.disableBlend();
         }
      }
   }
}
