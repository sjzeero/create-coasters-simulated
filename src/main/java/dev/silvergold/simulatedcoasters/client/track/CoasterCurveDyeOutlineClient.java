/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackBlockOutline
 *  com.simibubi.create.content.trains.track.TrackBlockOutline$BezierPointSelection
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.sublevel.ClientSubLevel
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.DyeItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveOutlineSupport;
import dev.silvergold.simulatedcoasters.client.track.CoasterCurveSelectionOutlineRenderer;
import dev.silvergold.simulatedcoasters.client.track.TrackOutlineBezierAccess;
import dev.silvergold.simulatedcoasters.track.CoasterCurveTintColors;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCurveDyeOutlineClient {
    public static final float DYE_PREVIEW_OUTLINE_ALPHA = 0.85f;

    private CoasterCurveDyeOutlineClient() {
    }

    @Nullable
    public static BezierConnection resolvePreviewHostedPrimary(Minecraft mc) {
        BezierConnection raw;
        if (mc.level == null) {
            return null;
        }
        if (AnchorPeerCurveHit.isActive()) {
            raw = AnchorPeerCurveHit.curve();
        } else {
            TrackBlockOutline.BezierPointSelection sel = TrackBlockOutline.result;
            if (sel == null) {
                return null;
            }
            raw = TrackOutlineBezierAccess.primaryBezierForSelection(sel);
        }
        return AnchorPeerCurveOutlineSupport.resolveHostedPrimary((Level)mc.level, raw);
    }

    @Nullable
    public static Vec3 resolvePreviewWorldHit(Minecraft mc) {
        if (mc.level == null) {
            return null;
        }
        if (AnchorPeerCurveHit.isActive()) {
            if (mc.hitResult != null) {
                return mc.hitResult.getLocation();
            }
            BlockPos focus = AnchorPeerCurveHit.focusAnchor();
            Vec3 plotLocal = AnchorPeerCurveHit.vec();
            if (focus == null || plotLocal == null) {
                return null;
            }
            float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level);
            return CoasterCurveDyeOutlineClient.plotLocalMidToWorld(mc, plotLocal, focus, pt);
        }
        TrackBlockOutline.BezierPointSelection sel = TrackBlockOutline.result;
        return sel == null ? null : sel.vec();
    }

    private static Vec3 plotLocalMidToWorld(Minecraft mc, Vec3 plotLocalMid, BlockPos anchorPos, float pt) {
        ClientSubLevel sub = (ClientSubLevel)Sable.HELPER.getContaining((Level)mc.level, (Vec3i)anchorPos);
        if (sub == null) {
            return plotLocalMid;
        }
        return ((Pose3d)sub.renderPose(pt)).transformPosition(plotLocalMid);
    }

    @Nullable
    public static BlockPos resolvePreviewSublevelAnchor(Minecraft mc, BezierConnection bc) {
        if (AnchorPeerCurveHit.isActive()) {
            return AnchorPeerCurveHit.focusAnchor();
        }
        return CoasterCurveSelectionOutlineRenderer.sublevelHostForCurve((Level)mc.level, bc);
    }

    @Nullable
    public static Integer heldDyeDiffuseRgb(@Nullable Player player) {
        DyeItem dye = CoasterCurveDyeOutlineClient.heldDyeItem(player);
        if (dye == null) {
            return null;
        }
        return CoasterCurveTintColors.diffuseRgbForDyePreviewOutline(dye.getDyeColor());
    }

    @Nullable
    public static Integer heldDyeDiffuseRgb(Minecraft mc) {
        return mc.player == null ? null : CoasterCurveDyeOutlineClient.heldDyeDiffuseRgb((Player)mc.player);
    }

    @Nullable
    public static InteractionHand handWithDye(Player player) {
        if (player.getMainHandItem().getItem() instanceof DyeItem) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().getItem() instanceof DyeItem) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    @Nullable
    public static DyeItem heldDyeItem(@Nullable Player player) {
        if (player == null) {
            return null;
        }
        ItemStack main = player.getMainHandItem();
        Item item = main.getItem();
        if (item instanceof DyeItem) {
            DyeItem dye = (DyeItem)item;
            return dye;
        }
        ItemStack off = player.getOffhandItem();
        Item item2 = off.getItem();
        if (item2 instanceof DyeItem) {
            DyeItem dye = (DyeItem)item2;
            return dye;
        }
        return null;
    }
}

