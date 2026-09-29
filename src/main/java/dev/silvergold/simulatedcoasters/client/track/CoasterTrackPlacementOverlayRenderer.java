/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer
 *  com.simibubi.create.foundation.gui.AllGuiTextures
 *  net.createmod.catnip.data.Pair
 *  net.minecraft.ChatFormatting
 *  net.minecraft.client.DeltaTracker
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.gui.LayeredDraw$Layer
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.ItemLike
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.content.equipment.blueprint.BlueprintOverlayRenderer;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import java.util.ArrayList;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;

public final class CoasterTrackPlacementOverlayRenderer {
    public static final LayeredDraw.Layer OVERLAY = CoasterTrackPlacementOverlayRenderer::renderOverlay;
    private static boolean active;
    private static int requiredTracks;
    private static boolean hasRequiredTracks;
    private static boolean validPreview;

    private CoasterTrackPlacementOverlayRenderer() {
    }

    public static void updateFromPlacementPreview(Minecraft mc, int tracksRequired, boolean hasEnoughTracks, boolean validPreview) {
        if (mc.player == null || mc.gameMode == null) {
            CoasterTrackPlacementOverlayRenderer.clear();
            return;
        }
        if (mc.gameMode.getPlayerMode() != GameType.SURVIVAL) {
            CoasterTrackPlacementOverlayRenderer.clear();
            return;
        }
        if (tracksRequired <= 0) {
            CoasterTrackPlacementOverlayRenderer.clear();
            return;
        }
        active = true;
        requiredTracks = tracksRequired;
        hasRequiredTracks = hasEnoughTracks;
        CoasterTrackPlacementOverlayRenderer.validPreview = validPreview;
    }

    public static void clear() {
        active = false;
        requiredTracks = 0;
        hasRequiredTracks = true;
        validPreview = true;
    }

    public static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) {
            return;
        }
        if (!active || requiredTracks <= 0) {
            return;
        }
        ArrayList<Pair> slots = new ArrayList<Pair>();
        ItemStack icon = new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get());
        for (int remaining = requiredTracks; remaining > 0; remaining -= 64) {
            slots.add(Pair.of((Object)icon.copyWithCount(Math.min(64, remaining)), (Object)hasRequiredTracks));
        }
        int w = 21 * slots.size();
        int x = (guiGraphics.guiWidth() - w) / 2;
        int y = guiGraphics.guiHeight() - 100;
        for (Pair slot : slots) {
            RenderSystem.enableBlend();
            ((Boolean)slot.getSecond() != false ? AllGuiTextures.HOTSLOT_ACTIVE : AllGuiTextures.HOTSLOT).render(guiGraphics, x, y);
            ItemStack stack = (ItemStack)slot.getFirst();
            String count = null;
            if (!((Boolean)slot.getSecond()).booleanValue()) {
                count = ChatFormatting.GOLD.toString() + stack.getCount();
            } else if (!validPreview) {
                count = ChatFormatting.GRAY.toString() + stack.getCount();
            }
            BlueprintOverlayRenderer.drawItemStack((GuiGraphics)guiGraphics, (Minecraft)mc, (int)x, (int)y, (ItemStack)stack, (String)count);
            x += 21;
        }
        RenderSystem.disableBlend();
    }
}

