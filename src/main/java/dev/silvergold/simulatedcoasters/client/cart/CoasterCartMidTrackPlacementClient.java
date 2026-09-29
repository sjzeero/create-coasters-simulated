/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.client.event.InputEvent$InteractionKeyMappingTriggered
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.cart;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartLinkSnapClient;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.item.CoasterCartItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacePayload;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlacementCollision;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class CoasterCartMidTrackPlacementClient {
    private CoasterCartMidTrackPlacementClient() {
    }

    public static boolean onUseItemKey(InputEvent.InteractionKeyMappingTriggered event) {
        float partialTick;
        if (!event.isUseItem()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        @Nullable InteractionHand hand = CoasterCartMidTrackPlacementClient.handWithCoasterCart((Player)mc.player);
        if (hand == null || !CoasterCartMidTrackPlacementClient.midSpanCurveHover()) {
            return false;
        }
        ItemStack stack = mc.player.getItemInHand(hand);
        Vec3 look = mc.player.getLookAngle();
        CoasterCartMidTrackPlacement.PlacementPose pose = CoasterCartMidTrackPlacementClient.resolveHoverPose(mc, look, Double.valueOf(partialTick = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level)));
        if (pose == null) {
            return false;
        }
        if (CoasterCartLinkPlacement.rejectsLinkPlacement((Level)mc.level, stack, pose, Double.valueOf(partialTick), CoasterCartLinkSnapClient.isSuppressLinkSnap())) {
            mc.player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.link_too_far"), true);
            return false;
        }
        if (CoasterCartPlacementCollision.hasSubLevelObstruction((Level)mc.level, (pose = CoasterCartLinkPlacement.applyLinkSnapIfSelected((Level)mc.level, stack, pose, Double.valueOf(partialTick), CoasterCartLinkSnapClient.isSuppressLinkSnap())).plotOriginWorld(), pose.orientation(), Float.valueOf(partialTick))) {
            mc.player.displayClientMessage(CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.bogie.no_room_for_placement"), true);
            return false;
        }
        PacketDistributor.sendToServer((CustomPacketPayload)new CoasterCartMidTrackPlacePayload(CoasterCartMidTrackPlacementClient.midSpanRasterHint(), look, hand, CoasterCartLinkSnapClient.isSuppressLinkSnap()), (CustomPacketPayload[])new CustomPacketPayload[0]);
        event.setCanceled(true);
        event.setSwingHand(true);
        return true;
    }

    public static boolean holdingCoasterCart(Minecraft mc) {
        return mc.player != null && CoasterCartMidTrackPlacementClient.handWithCoasterCart((Player)mc.player) != null;
    }

    public static boolean midSpanCurveHover() {
        if (!AnchorPeerCurveHit.isActive()) {
            return false;
        }
        BezierConnection bc = AnchorPeerCurveHit.curve();
        return bc != null && bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id);
    }

    @Nullable
    public static CoasterCartMidTrackPlacement.PlacementPose resolveHoverPose(Minecraft mc, Vec3 look, @Nullable Double partialTick) {
        CoasterCartMidTrackPlacement.PlacementPose pose;
        BezierConnection bc;
        if (mc.level == null || mc.player == null) {
            return null;
        }
        if (CoasterCartMidTrackPlacementClient.midSpanCurveHover() && (bc = AnchorPeerCurveHit.curve()) != null && (pose = CoasterCartMidTrackPlacement.resolveForCurve((Level)mc.level, (Player)mc.player, bc, look, partialTick)) != null) {
            return pose;
        }
        return CoasterCartMidTrackPlacement.resolveMidSpan((Level)mc.level, (Player)mc.player, look, partialTick, CoasterCartMidTrackPlacementClient.midSpanRasterHint());
    }

    @Nullable
    public static BlockPos midSpanRasterHint() {
        BlockPos raster = AnchorPeerCurveHit.rasterBlockPos();
        return raster != null ? raster : AnchorPeerCurveHit.focusAnchor();
    }

    public static boolean anchorHover() {
        return CoasterCartMidTrackPlacementClient.anchorHoverPos() != null;
    }

    @Nullable
    public static BlockPos anchorHoverPos() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = ((BlockHitResult)mc.hitResult).getBlockPos();
        return CoasterCartMidTrackPlacement.isAcceptedAnchor((Level)mc.level, pos) ? pos : null;
    }

    public static boolean placementPreviewActive() {
        return CoasterCartMidTrackPlacementClient.midSpanCurveHover() || CoasterCartMidTrackPlacementClient.anchorHover();
    }

    @Nullable
    private static InteractionHand handWithCoasterCart(Player player) {
        CoasterCartItem bearing = (CoasterCartItem)((Object)SimulatedCoasters.COASTER_CART.get());
        if (player.getMainHandItem().is((Item)bearing)) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is((Item)bearing)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }
}

