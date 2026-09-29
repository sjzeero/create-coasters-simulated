/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllKeys
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.network.chat.TextColor
 *  net.minecraft.util.FastColor$ARGB32
 *  net.minecraft.world.level.GameType
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllKeys;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleDragManager;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackPlacementNeighborSmoothSuppressClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
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
        if (!CoasterTrackPlacementSmoothHud.shouldShow() || mc.player == null || mc.options.hideGui) {
            return;
        }
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }
        int y = BezierHandleDragManager.editHudRowAboveHotbarY(mc, guiGraphics);
        int blueRgb = 11781103;
        int keyRgb = CoasterTrackPlacementNeighborSmoothSuppressClient.isSuppressNeighborSmoothing() ? 0xFFFFFF : blueRgb;
        Style keyStyle = Style.EMPTY.withColor(TextColor.fromRgb((int)keyRgb));
        MutableComponent bracketedKey = Component.literal((String)"[").withStyle(keyStyle).append((Component)Component.literal((String)AllKeys.ALT_MODIFIER.getBoundKey()).withStyle(keyStyle)).append((Component)Component.literal((String)"]").withStyle(keyStyle));
        MutableComponent hint = Component.translatable((String)"simulatedcoasters.track.placement_neighbor_smooth_hint", (Object[])new Object[]{bracketedKey}).withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)blueRgb)));
        Font font = mc.font;
        int w = font.width((FormattedText)hint);
        int x = (guiGraphics.guiWidth() - w) / 2;
        guiGraphics.drawStringWithBackdrop(font, (Component)hint, x, y, w, FastColor.ARGB32.color((int)255, (int)blueRgb));
    }
}

