/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllKeys
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.network.chat.TextColor
 *  net.minecraft.util.FastColor$ARGB32
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.level.GameType
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.item;

import com.simibubi.create.AllKeys;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.client.item.RivetPlacementOutlineClient;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleDragManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class RivetPlacementHud {
    private static boolean actionBarShowing;

    private RivetPlacementHud() {
    }

    public static boolean shouldShow(Minecraft mc) {
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return false;
        }
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return false;
        }
        return RivetPlacementHud.holdingRivet(mc);
    }

    public static void clientTick(Minecraft mc) {
        if (!RivetPlacementHud.shouldShow(mc)) {
            RivetPlacementHud.clearActionBarIfShowing(mc.player);
            return;
        }
        if (mc.player == null) {
            return;
        }
        int angle = RivetPlacementOutlineClient.displayPreviewYRotationDegrees(mc);
        MutableComponent topRow = Component.translatable((String)"simulatedcoasters.rivet.placement_angle", (Object[])new Object[]{angle}).withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)0xFFFFFF)));
        mc.player.displayClientMessage((Component)topRow, true);
        actionBarShowing = true;
    }

    public static void render(Minecraft mc, GuiGraphics guiGraphics) {
        if (!RivetPlacementHud.shouldShow(mc)) {
            return;
        }
        RivetPlacementHud.renderBottomHintRow(mc, guiGraphics);
    }

    public static void clearOnLevelUnload() {
        actionBarShowing = false;
    }

    private static void renderBottomHintRow(Minecraft mc, GuiGraphics guiGraphics) {
        int blueRgb = 11781103;
        int whiteRgb = 0xFFFFFF;
        boolean freePlace = AllKeys.ctrlDown();
        boolean fineRotation = RivetPlacementOutlineClient.isFineRotationActive();
        int ctrlKeyRgb = freePlace ? whiteRgb : blueRgb;
        int altKeyRgb = fineRotation ? whiteRgb : blueRgb;
        Style ctrlKeyStyle = Style.EMPTY.withColor(TextColor.fromRgb((int)ctrlKeyRgb));
        Style altKeyStyle = Style.EMPTY.withColor(TextColor.fromRgb((int)altKeyRgb));
        Style blueStyle = Style.EMPTY.withColor(TextColor.fromRgb((int)blueRgb));
        Style whiteStyle = Style.EMPTY.withColor(TextColor.fromRgb((int)whiteRgb));
        MutableComponent bracketedCtrl = RivetPlacementHud.bracketedKey(AllKeys.CTRL_MODIFIER.getBoundKey(), ctrlKeyStyle);
        MutableComponent bracketedAlt = RivetPlacementHud.bracketedKey(AllKeys.ALT_MODIFIER.getBoundKey(), altKeyStyle);
        MutableComponent hint = Component.empty().append((Component)Component.translatable((String)"simulatedcoasters.rivet.placement_controls_free", (Object[])new Object[]{bracketedCtrl}).withStyle(blueStyle)).append((Component)Component.translatable((String)"simulatedcoasters.track.bezier_edit_anchor_status_sep").withStyle(whiteStyle)).append((Component)Component.translatable((String)"simulatedcoasters.rivet.placement_controls_rotate", (Object[])new Object[]{bracketedAlt}).withStyle(blueStyle));
        Font font = mc.font;
        int w = font.width((FormattedText)hint);
        int x = (guiGraphics.guiWidth() - w) / 2;
        int y = BezierHandleDragManager.editHudRowAboveHotbarY(mc, guiGraphics);
        guiGraphics.drawStringWithBackdrop(font, (Component)hint, x, y, w, FastColor.ARGB32.color((int)255, (int)blueRgb));
    }

    private static MutableComponent bracketedKey(String keyLabel, Style keyStyle) {
        return Component.literal((String)"[").withStyle(keyStyle).append((Component)Component.literal((String)keyLabel).withStyle(keyStyle)).append((Component)Component.literal((String)"]").withStyle(keyStyle));
    }

    private static boolean holdingRivet(Minecraft mc) {
        Item rivet = (Item)SimulatedCoasters.RIVET.get();
        return mc.player.getMainHandItem().is(rivet) || mc.player.getOffhandItem().is(rivet);
    }

    private static void clearActionBarIfShowing(@Nullable LocalPlayer player) {
        if (player == null || !actionBarShowing) {
            return;
        }
        player.displayClientMessage((Component)Component.empty(), true);
        actionBarShowing = false;
    }
}

