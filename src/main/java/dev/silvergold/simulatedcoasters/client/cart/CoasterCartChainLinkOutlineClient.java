/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniondc
 */
package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartLinkPreviewParticles;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartLinkSnapClient;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartOutlineRenderer;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleClientSounds;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLinkInteraction;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import java.util.Locale;
import java.util.UUID;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartChainLinkOutlineClient {
    private static final String OUTLINER_KEY_SELECTED = "coaster_cart_chain_link_selected";
    private static final String OUTLINER_KEY_HOVER = "coaster_cart_chain_link_hover";
    private static final int BLOCKED_OUTLINE_COLOR = 0xFF4747;
    private static boolean linkActionBarShowing;
    private static String linkActionBarKey;
    private static boolean frameLinkingToHover;
    private static boolean frameBlocked;
    @Nullable
    private static String frameStructuralRejectKey;
    private static double frameRawDistance;
    private static double frameLinkDistance;
    @Nullable
    private static Vec3 frameSelectedCenter;
    @Nullable
    private static Vec3 frameHoverCenter;

    private CoasterCartChainLinkOutlineClient() {
    }

    public static void renderFrame(Minecraft mc, float partialTick) {
        frameLinkingToHover = false;
        frameBlocked = false;
        frameStructuralRejectKey = null;
        frameRawDistance = Double.NaN;
        frameLinkDistance = Double.NaN;
        frameSelectedCenter = null;
        frameHoverCenter = null;
        Outliner outliner = Outliner.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            CoasterCartChainLinkOutlineClient.clear(outliner);
            return;
        }
        ItemStack chain = mc.player.getMainHandItem();
        if (!chain.is(Items.CHAIN) || !CoasterCartLinkSelection.hasLinkTarget(chain)) {
            CoasterCartChainLinkOutlineClient.clear(outliner);
            return;
        }
        @Nullable UUID selectedId = CoasterCartLinkSelection.readLinkTarget(chain);
        if (selectedId == null) {
            CoasterCartChainLinkOutlineClient.clear(outliner);
            return;
        }
        SubLevel selectedCart = CoasterCartResolve.findCartById((Level)mc.level, selectedId);
        if (selectedCart == null || selectedCart.isRemoved()) {
            CoasterCartChainLinkOutlineClient.clear(outliner);
            return;
        }
        @Nullable SubLevel hoverCart = mc.hitResult == null ? null : CoasterCartChainLinkInteraction.resolveHoveredCart((Level)mc.level, mc.hitResult);
        boolean linkingToHover = hoverCart != null && !hoverCart.getUniqueId().equals(selectedId);
        double linkDistance = Double.NaN;
        double rawDistance = Double.NaN;
        boolean distanceRejected = false;
        String structuralRejectKey = null;
        Vec3 selectedCenter = null;
        Vec3 hoverCenter = null;
        if (linkingToHover) {
            rawDistance = CoasterCartLinkPlacement.bearingCenterDistance(selectedCart, hoverCart, Double.valueOf(partialTick));
            linkDistance = CoasterCartLinkPlacement.resolveChainLinkDistance((Level)mc.level, selectedCart, hoverCart, Double.valueOf(partialTick), CoasterCartLinkSnapClient.isSuppressLinkSnap());
            boolean bl = distanceRejected = Double.isNaN(rawDistance) || CoasterCartLinkPlacement.rejectsLinkDistance(rawDistance);
            if (!distanceRejected) {
                structuralRejectKey = CoasterCartTrainLinkConstraint.coupleRejectLangKey(selectedCart, hoverCart);
            }
            selectedCenter = CoasterCartLinkPlacement.partnerBearingCenterWorld(selectedCart, Double.valueOf(partialTick));
            hoverCenter = CoasterCartLinkPlacement.partnerBearingCenterWorld(hoverCart, Double.valueOf(partialTick));
        }
        boolean blocked = distanceRejected || structuralRejectKey != null;
        int selectedColor = blocked ? 0xFF4747 : 1940687;
        CoasterCartChainLinkOutlineClient.drawCartOutline(outliner, OUTLINER_KEY_SELECTED, selectedCart, partialTick, selectedColor);
        if (linkingToHover && hoverCart != null) {
            int hoverColor = blocked ? 0xFF4747 : 1940687;
            CoasterCartChainLinkOutlineClient.drawCartOutline(outliner, OUTLINER_KEY_HOVER, hoverCart, partialTick, hoverColor);
        } else {
            CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY_HOVER);
        }
        frameLinkingToHover = linkingToHover;
        frameBlocked = blocked;
        frameStructuralRejectKey = structuralRejectKey;
        frameRawDistance = rawDistance;
        frameLinkDistance = linkDistance;
        frameSelectedCenter = selectedCenter;
        frameHoverCenter = hoverCenter;
    }

    public static void clientTick(Minecraft mc) {
        Object key;
        Component message;
        ClientLevel clientLevel;
        if (mc.player == null) {
            CoasterCartChainLinkOutlineClient.endLinkActionBarIfShowing(null);
            return;
        }
        ItemStack chain = mc.player.getMainHandItem();
        if (!chain.is(Items.CHAIN) || !CoasterCartLinkSelection.hasLinkTarget(chain)) {
            CoasterCartChainLinkOutlineClient.endLinkActionBarIfShowing(mc.player);
            return;
        }
        if (!frameLinkingToHover) {
            CoasterCartChainLinkOutlineClient.endLinkActionBarIfShowing(mc.player);
            return;
        }
        if (!frameBlocked && frameSelectedCenter != null && frameHoverCenter != null && (clientLevel = mc.level) instanceof ClientLevel) {
            ClientLevel cl = clientLevel;
            CoasterCartLinkPreviewParticles.spawnSpan(cl, frameSelectedCenter, frameHoverCenter);
        }
        if (frameStructuralRejectKey != null) {
            message = CoasterTrackPlacement.placementErrorMessage(frameStructuralRejectKey);
            key = frameStructuralRejectKey;
        } else if (frameBlocked) {
            message = CoasterCartLinkPlacement.linkDistanceRejectActionBarComponent(frameRawDistance);
            key = CoasterCartLinkPlacement.isLinkDistanceTooClose(frameRawDistance) ? "too_close" : "too_far";
        } else {
            message = CoasterCartLinkPlacement.linkDistanceActionBarComponent(frameLinkDistance);
            key = "ok:" + String.format(Locale.ROOT, "%.2f", frameLinkDistance);
        }
        CoasterCartChainLinkOutlineClient.setLinkActionBar(mc.player, message, (String)key);
    }

    public static void playDenyIfCoupleBlocked(ItemStack chain, SubLevel clickedCart) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || clickedCart == null) {
            return;
        }
        @Nullable UUID selectedId = CoasterCartLinkSelection.readLinkTarget(chain);
        if (selectedId == null || selectedId.equals(clickedCart.getUniqueId())) {
            return;
        }
        SubLevel partner = CoasterCartResolve.findCartById((Level)mc.level, selectedId);
        if (partner == null || partner.isRemoved()) {
            return;
        }
        double rawDistance = CoasterCartLinkPlacement.bearingCenterDistance(partner, clickedCart, null);
        if (Double.isNaN(rawDistance) || CoasterCartLinkPlacement.rejectsLinkDistance(rawDistance) || CoasterCartTrainLinkConstraint.coupleRejectLangKey(partner, clickedCart) != null) {
            BezierHandleClientSounds.playCreateDeny();
        }
    }

    private static void setLinkActionBar(LocalPlayer player, Component message, String key) {
        if (linkActionBarShowing && key.equals(linkActionBarKey)) {
            return;
        }
        player.displayClientMessage(message, true);
        linkActionBarKey = key;
        linkActionBarShowing = true;
    }

    private static void endLinkActionBarIfShowing(@Nullable LocalPlayer player) {
        if (player == null || !linkActionBarShowing) {
            return;
        }
        player.displayClientMessage((Component)Component.empty(), true);
        linkActionBarShowing = false;
        linkActionBarKey = "";
    }

    private static void drawCartOutline(Outliner outliner, String key, SubLevel cart, float partialTick, int color) {
        Pose3d pose = new Pose3d();
        cart.lastPose().lerp((Pose3dc)cart.logicalPose(), (double)partialTick, pose);
        Vec3 plotOrigin = CoasterCartMidTrackPlacement.plotOriginFromPlacedCart(cart, pose);
        if (plotOrigin == null) {
            CoasterCartOutlineRenderer.clear(outliner, key);
            return;
        }
        CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(outliner, key, plotOrigin, (Quaterniondc)pose.orientation(), color);
    }

    private static void clear(Outliner outliner) {
        CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY_SELECTED);
        CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY_HOVER);
    }

    static {
        linkActionBarKey = "";
        frameRawDistance = Double.NaN;
        frameLinkDistance = Double.NaN;
    }
}

