/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllSoundEvents
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackBlockOutline
 *  com.simibubi.create.content.trains.track.TrackBlockOutline$BezierPointSelection
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.sublevel.ClientSubLevel
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.neoforge.client.event.InputEvent$InteractionKeyMappingTriggered
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleClientSounds;
import dev.silvergold.simulatedcoasters.client.track.HoveredCoasterCurve;
import dev.silvergold.simulatedcoasters.client.track.TrackOutlineBezierAccess;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterTrackAnchorFirstFromCurvePayload;
import dev.silvergold.simulatedcoasters.track.CoasterTrackAnchorSecondFromCurvePayload;
import dev.silvergold.simulatedcoasters.track.CoasterTrackItem;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class CoasterTrackOpenEndpointOutlineClient {
    private CoasterTrackOpenEndpointOutlineClient() {
    }

    public static boolean onUseItemKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return false;
        }
        if (mc.player.isShiftKeyDown()) {
            return false;
        }
        ItemStack stack = mc.player.getMainHandItem();
        if (!(stack.getItem() instanceof CoasterTrackItem)) {
            return false;
        }
        @Nullable BlockPos exclude = CoasterTrackPlacement.hasAnchorFirstSelection(stack) ? CoasterTrackPlacement.readAnchorFirstSelection(stack) : null;
        @Nullable BlockPos anchor = CoasterTrackOpenEndpointOutlineClient.resolveClosestCurveEndpoint(mc, exclude, true);
        if (anchor == null) {
            return false;
        }
        if (exclude == null) {
            CoasterTrackPlacement.applyAnchorFirstSelection((Level)mc.level, stack, anchor);
            PacketDistributor.sendToServer((CustomPacketPayload)new CoasterTrackAnchorFirstFromCurvePayload(anchor), (CustomPacketPayload[])new CustomPacketPayload[0]);
        } else if (CoasterTrackPlacement.tryConnectSecondAnchorFromCurve((Level)mc.level, (Player)mc.player, stack, anchor)) {
            BezierHandleClientSounds.playCommitSuccessSound(mc);
            CoasterTrackItem.clearPendingConnection(stack);
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            PacketDistributor.sendToServer((CustomPacketPayload)new CoasterTrackAnchorSecondFromCurvePayload(anchor, exclude), (CustomPacketPayload[])new CustomPacketPayload[0]);
        } else {
            AllSoundEvents.DENY.playFrom((Entity)mc.player, 1.0f, 1.0f);
        }
        event.setCanceled(true);
        event.setSwingHand(true);
        return true;
    }

    @Nullable
    public static BlockPos resolveClosestOpenAnchor(Minecraft mc) {
        ItemStack stack = mc.player == null ? ItemStack.EMPTY : CoasterTrackOpenEndpointOutlineClient.findCoasterTrackStack(mc.player);
        @Nullable BlockPos exclude = stack != null && CoasterTrackPlacement.hasAnchorFirstSelection(stack) ? CoasterTrackPlacement.readAnchorFirstSelection(stack) : null;
        return CoasterTrackOpenEndpointOutlineClient.resolveClosestCurveEndpoint(mc, exclude, true);
    }

    @Nullable
    public static BlockPos resolveClosestCurveAnchor(Minecraft mc) {
        return CoasterTrackOpenEndpointOutlineClient.resolveClosestCurveEndpoint(mc, null, false);
    }

    @Nullable
    public static HoveredCoasterCurve resolveHoveredCoasterCurve(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level);
        return CoasterTrackOpenEndpointOutlineClient.resolveHoveredCurve(mc, (Level)mc.level, pt);
    }

    public static Vec3 anchorJunctionWorld(Level level, BlockPos anchor, float partialTick) {
        return CoasterTrackOpenEndpointOutlineClient.anchorCenterWorld(level, anchor, partialTick);
    }

    @Nullable
    private static BlockPos resolveClosestCurveEndpoint(Minecraft mc, @Nullable BlockPos excludeAnchor, boolean openEndpointsOnly) {
        if (mc.player == null || mc.level == null) {
            return null;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level);
        @Nullable HoveredCoasterCurve hovered = CoasterTrackOpenEndpointOutlineClient.resolveHoveredCurve(mc, (Level)mc.level, pt);
        if (hovered == null) {
            return null;
        }
        BlockPos ep1 = (BlockPos)hovered.bc().bePositions.getFirst();
        BlockPos ep2 = (BlockPos)hovered.bc().bePositions.getSecond();
        if (openEndpointsOnly && excludeAnchor != null && (excludeAnchor.equals((Object)ep1) || excludeAnchor.equals((Object)ep2))) {
            return null;
        }
        BlockPos closest = null;
        double bestDistSq = Double.POSITIVE_INFINITY;
        for (BlockPos ep : new BlockPos[]{ep1, ep2}) {
            double d;
            if (openEndpointsOnly && !CoasterTrackOpenEndpointOutlineClient.isOpenEndpoint((Level)mc.level, ep) || !CoasterTrackOpenEndpointOutlineClient.isCurveEndpointAnchor((Level)mc.level, ep) || !((d = hovered.hoverWorld().distanceToSqr(CoasterTrackOpenEndpointOutlineClient.anchorCenterWorld((Level)mc.level, ep, pt))) < bestDistSq)) continue;
            bestDistSq = d;
            closest = ep;
        }
        return closest;
    }

    private static boolean isCurveEndpointAnchor(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity;
    }

    private static boolean isOpenEndpoint(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        return be.legCount() < 2;
    }

    private static Vec3 anchorCenterWorld(Level level, BlockPos anchor, float partialTick) {
        Vec3 local = CoasterAnchorpointBlockEntity.worldCenter(level, anchor);
        return CoasterPathGraphManager.sublevelWorldPosition(level, anchor, local, Double.valueOf(partialTick));
    }

    @Nullable
    private static HoveredCoasterCurve resolveHoveredCurve(Minecraft mc, Level level, float pt) {
        if (AnchorPeerCurveHit.isActive()) {
            BezierConnection bc = AnchorPeerCurveHit.curve();
            if (bc == null || !CoasterBezierRailFrames.isCoaster(bc)) {
                return null;
            }
            BlockPos focus = AnchorPeerCurveHit.focusAnchor();
            Vec3 hoverWorld = CoasterTrackOpenEndpointOutlineClient.plotLocalMidToWorld(mc, AnchorPeerCurveHit.vec(), focus, pt);
            return new HoveredCoasterCurve(bc, hoverWorld);
        }
        TrackBlockOutline.BezierPointSelection sel = TrackBlockOutline.result;
        if (sel == null) {
            return null;
        }
        BezierConnection bc = TrackOutlineBezierAccess.primaryBezierForSelection(sel);
        if (bc == null || !CoasterBezierRailFrames.isCoaster(bc)) {
            return null;
        }
        return new HoveredCoasterCurve(bc, sel.vec());
    }

    private static Vec3 plotLocalMidToWorld(Minecraft mc, Vec3 plotLocalMid, BlockPos anchorPos, float pt) {
        ClientSubLevel sub = (ClientSubLevel)Sable.HELPER.getContaining((Level)mc.level, (Vec3i)anchorPos);
        if (sub == null) {
            return plotLocalMid;
        }
        return ((Pose3d)sub.renderPose(pt)).transformPosition(plotLocalMid);
    }

    @Nullable
    private static ItemStack findCoasterTrackStack(LocalPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof CoasterTrackItem) {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof CoasterTrackItem) {
            return off;
        }
        return null;
    }
}

