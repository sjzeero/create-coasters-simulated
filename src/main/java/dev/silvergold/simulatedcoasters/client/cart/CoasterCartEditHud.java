/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllItems
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.Font
 *  net.minecraft.client.gui.GuiGraphics
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.FormattedText
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.network.chat.TextColor
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.util.FastColor$ARGB32
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.Level
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.cart;

import com.simibubi.create.AllItems;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartEditMode;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartTrainLinkClientState;
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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartEditHud {
    private static boolean actionBarShowing;

    private CoasterCartEditHud() {
    }

    public static boolean shouldShow(Minecraft mc) {
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return false;
        }
        if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return false;
        }
        if (!CoasterCartEditMode.isActive()) {
            return false;
        }
        if (!AllItems.WRENCH.isIn(mc.player.getMainHandItem()) && !AllItems.WRENCH.isIn(mc.player.getOffhandItem())) {
            return false;
        }
        @Nullable UUID cartId = CoasterCartEditMode.getActiveCartId();
        return cartId != null && mc.level != null && CoasterCartResolve.findCartById((Level)mc.level, cartId) != null;
    }

    public static void clientTick(Minecraft mc) {
        if (!CoasterCartEditHud.shouldShow(mc)) {
            CoasterCartEditHud.clearActionBarIfShowing(mc.player);
            return;
        }
        @Nullable UUID cartId = CoasterCartEditMode.getActiveCartId();
        if (cartId == null || mc.level == null || mc.player == null) {
            return;
        }
        List<CoasterCartTrainLinkSync.LinkEntry> links = CoasterCartTrainLinkClientState.linksFor((ResourceKey<Level>)mc.level.dimension(), cartId);
        @Nullable MutableComponent topRow = CoasterCartEditHud.buildActionBarLine(links);
        if (topRow == null) {
            CoasterCartEditHud.clearActionBarIfShowing(mc.player);
            return;
        }
        mc.player.displayClientMessage((Component)topRow, true);
        actionBarShowing = true;
    }

    public static void render(Minecraft mc, GuiGraphics guiGraphics) {
        if (!CoasterCartEditHud.shouldShow(mc)) {
            return;
        }
        @Nullable UUID cartId = CoasterCartEditMode.getActiveCartId();
        if (cartId == null || mc.level == null) {
            return;
        }
        List<CoasterCartTrainLinkSync.LinkEntry> links = CoasterCartTrainLinkClientState.linksFor((ResourceKey<Level>)mc.level.dimension(), cartId);
        CoasterCartEditHud.renderBottomHintRow(mc, guiGraphics, links.isEmpty());
    }

    public static void clearOnLevelUnload() {
        actionBarShowing = false;
    }

    @Nullable
    private static MutableComponent buildActionBarLine(List<CoasterCartTrainLinkSync.LinkEntry> links) {
        if (links.isEmpty()) {
            return null;
        }
        int whiteRgb = 0xFFFFFF;
        Style styleWhite = Style.EMPTY.withColor(TextColor.fromRgb((int)whiteRgb));
        if (links.size() == 1) {
            String meters = CoasterCartEditHud.formatLinkMeters(links.getFirst().centerDistance());
            return Component.translatable((String)"simulatedcoasters.cart.edit_link_distance", (Object[])new Object[]{meters}).withStyle(styleWhite);
        }
        String metersA = CoasterCartEditHud.formatLinkMeters(links.get(0).centerDistance());
        String metersB = CoasterCartEditHud.formatLinkMeters(links.get(1).centerDistance());
        return Component.empty().append((Component)Component.translatable((String)"simulatedcoasters.cart.edit_link_a", (Object[])new Object[]{metersA}).withStyle(styleWhite)).append((Component)Component.translatable((String)"simulatedcoasters.track.bezier_edit_anchor_status_sep").withStyle(styleWhite)).append((Component)Component.translatable((String)"simulatedcoasters.cart.edit_link_b", (Object[])new Object[]{metersB}).withStyle(styleWhite));
    }

    private static String formatLinkMeters(double centerDistanceBlocks) {
        return CoasterBezierHandleEdit.formatLiftMetersReadout((float)centerDistanceBlocks);
    }

    private static void renderBottomHintRow(Minecraft mc, GuiGraphics guiGraphics, boolean noConnections) {
        int whiteRgb = 0xFFFFFF;
        MutableComponent hint = noConnections ? Component.translatable((String)"simulatedcoasters.cart.edit_no_connections") : Component.translatable((String)"simulatedcoasters.cart.edit_extend_shorten_hint");
        hint = hint.withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)whiteRgb)));
        Font font = mc.font;
        int w = font.width((FormattedText)hint);
        int x = (guiGraphics.guiWidth() - w) / 2;
        int y = BezierHandleDragManager.editHudRowAboveHotbarY(mc, guiGraphics);
        guiGraphics.drawStringWithBackdrop(font, (Component)hint, x, y, w, FastColor.ARGB32.color((int)255, (int)whiteRgb));
    }

    private static void clearActionBarIfShowing(@Nullable LocalPlayer player) {
        if (player == null || !actionBarShowing) {
            return;
        }
        player.displayClientMessage((Component)Component.empty(), true);
        actionBarShowing = false;
    }
}

