/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.outliner.Outliner
 *  net.createmod.catnip.theme.Color
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniondc
 */
package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartLinkPreviewParticles;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartLinkSnapClient;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartMidTrackPlacementClient;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartOutlineRenderer;
import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlacementCollision;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import java.util.UUID;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;

public final class CoasterCartPlacementPreviewRenderer {
    private static final String OUTLINER_KEY_PREFIX = "coaster_cart_placement_preview";
    private static final int VALID_OUTLINE_COLOR = Color.mixColors((int)0x9E9E9E, (int)9817409, (float)0.8f);
    private static final int LINK_PLACEMENT_OUTLINE_COLOR = 1940687;
    private static final int BLOCKED_OUTLINE_COLOR = 0xFF4747;
    private static boolean linkPlacementTooFar;
    private static boolean frameLinking;
    private static boolean frameLinkBlocked;
    @Nullable
    private static String frameStructuralRejectKey;
    private static double frameLinkDistanceBlocks;
    @Nullable
    private static Vec3 framePartnerCenter;
    @Nullable
    private static Vec3 framePreviewCenter;

    private CoasterCartPlacementPreviewRenderer() {
    }

    public static boolean isLinkPlacementTooFar() {
        return linkPlacementTooFar;
    }

    public static int linkPartnerOutlineColor() {
        return linkPlacementTooFar || frameStructuralRejectKey != null ? 0xFF4747 : 1940687;
    }

    public static boolean suppressDefaultCurveHighlight(Minecraft mc) {
        return CoasterCartMidTrackPlacementClient.holdingCoasterCart(mc) && CoasterCartMidTrackPlacementClient.midSpanCurveHover();
    }

    public static void renderFrame(Minecraft mc, float partialTick) {
        boolean linkBlocked;
        CoasterCartLinkPlacement.AdjustedPlacement adjusted;
        linkPlacementTooFar = false;
        frameLinking = false;
        frameLinkBlocked = false;
        frameStructuralRejectKey = null;
        frameLinkDistanceBlocks = Double.NaN;
        framePartnerCenter = null;
        framePreviewCenter = null;
        Outliner outliner = Outliner.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui || !CoasterCartMidTrackPlacementClient.holdingCoasterCart(mc) || !CoasterCartMidTrackPlacementClient.placementPreviewActive()) {
            CoasterCartPlacementPreviewRenderer.clearOutlines(outliner);
            return;
        }
        @Nullable CoasterCartMidTrackPlacement.PlacementPose pose = CoasterCartPlacementPreviewRenderer.resolvePreviewPose(mc, partialTick);
        if (pose == null) {
            CoasterCartPlacementPreviewRenderer.clearOutlines(outliner);
            return;
        }
        @Nullable ItemStack linkStack = CoasterCartPlacementPreviewRenderer.linkTargetStack(mc);
        boolean linkTooFar = false;
        String structuralRejectKey = null;
        boolean linking = false;
        double linkDistanceBlocks = Double.NaN;
        Vec3 partnerCenter = null;
        if (linkStack != null && (adjusted = CoasterCartLinkPlacement.adjustForLinkSelection((Level)mc.level, linkStack, pose, Double.valueOf(partialTick), CoasterCartLinkSnapClient.isSuppressLinkSnap())) != null) {
            SubLevel partnerCart;
            pose = adjusted.pose();
            linkDistanceBlocks = adjusted.centerDistanceBlocks();
            linkTooFar = adjusted.tooFar();
            linking = true;
            @Nullable UUID linkTargetId = CoasterCartLinkSelection.readLinkTarget(linkStack);
            if (linkTargetId != null && (partnerCart = CoasterCartResolve.findCartById((Level)mc.level, linkTargetId)) != null && !partnerCart.isRemoved()) {
                partnerCenter = CoasterCartLinkPlacement.partnerBearingCenterWorld(partnerCart, Double.valueOf(partialTick));
                if (!linkTooFar && CoasterCartTrainLinkConstraint.isAtLinkLimit(partnerCart)) {
                    structuralRejectKey = "simulatedcoasters.cart.link_full";
                }
            }
        }
        boolean obstructed = CoasterCartPlacementCollision.hasSubLevelObstruction((Level)mc.level, pose.plotOriginWorld(), pose.orientation(), Float.valueOf(partialTick));
        boolean bl = linkBlocked = linkTooFar || structuralRejectKey != null;
        if (!linkTooFar) {
            int color = obstructed || structuralRejectKey != null ? 0xFF4747 : (linking ? 1940687 : VALID_OUTLINE_COLOR);
            CoasterCartOutlineRenderer.showRotatedUnitCubeOutline(outliner, OUTLINER_KEY_PREFIX, pose.plotOriginWorld(), (Quaterniondc)pose.orientation(), color);
        } else {
            linkPlacementTooFar = true;
            CoasterCartPlacementPreviewRenderer.clearOutlines(outliner);
        }
        frameLinking = linking;
        frameLinkBlocked = linkBlocked;
        frameStructuralRejectKey = structuralRejectKey;
        frameLinkDistanceBlocks = linkDistanceBlocks;
        framePartnerCenter = partnerCenter;
        if (linking && !linkTooFar) {
            framePreviewCenter = CoasterCartLinkPlacement.bearingCenterWorld(pose.plotOriginWorld(), pose.orientation());
        }
        if (linkTooFar) {
            linkPlacementTooFar = true;
        }
    }

    public static void clientTick(Minecraft mc) {
        ClientLevel clientLevel;
        if (!frameLinking || mc.player == null) {
            return;
        }
        if (!frameLinkBlocked && framePartnerCenter != null && framePreviewCenter != null && (clientLevel = mc.level) instanceof ClientLevel) {
            ClientLevel cl = clientLevel;
            CoasterCartLinkPreviewParticles.spawnSpan(cl, framePartnerCenter, framePreviewCenter);
        }
        mc.player.displayClientMessage((Component)(frameStructuralRejectKey != null ? CoasterTrackPlacement.placementErrorMessage(frameStructuralRejectKey) : (linkPlacementTooFar ? CoasterCartLinkPlacement.linkTooFarActionBarComponent() : CoasterCartLinkPlacement.linkDistanceActionBarComponent(frameLinkDistanceBlocks))), true);
    }

    private static void clearOutlines(Outliner outliner) {
        CoasterCartOutlineRenderer.clear(outliner, OUTLINER_KEY_PREFIX);
    }

    @Nullable
    private static ItemStack linkTargetStack(Minecraft mc) {
        CoasterCartItem cart = (CoasterCartItem)((Object)SimulatedCoasters.COASTER_CART.get());
        if (mc.player.getMainHandItem().is((Item)cart) && CoasterCartItem.hasLinkSelection(mc.player.getMainHandItem())) {
            return mc.player.getMainHandItem();
        }
        if (mc.player.getOffhandItem().is((Item)cart) && CoasterCartItem.hasLinkSelection(mc.player.getOffhandItem())) {
            return mc.player.getOffhandItem();
        }
        return null;
    }

    @Nullable
    private static CoasterCartMidTrackPlacement.PlacementPose resolvePreviewPose(Minecraft mc, float partialTick) {
        if (mc.level == null || mc.player == null) {
            return null;
        }
        if (CoasterCartMidTrackPlacementClient.midSpanCurveHover()) {
            return CoasterCartMidTrackPlacementClient.resolveHoverPose(mc, mc.player.getLookAngle(), Double.valueOf(partialTick));
        }
        BlockPos anchor = CoasterCartMidTrackPlacementClient.anchorHoverPos();
        if (anchor == null) {
            return null;
        }
        return CoasterCartMidTrackPlacement.resolveFromAnchor((Level)mc.level, (Player)mc.player, anchor, mc.player.getLookAngle(), null, Double.valueOf(partialTick));
    }

    static {
        frameLinkDistanceBlocks = Double.NaN;
    }
}

