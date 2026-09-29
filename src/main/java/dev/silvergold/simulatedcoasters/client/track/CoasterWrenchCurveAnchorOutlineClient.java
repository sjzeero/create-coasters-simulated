/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllItems
 *  com.simibubi.create.content.trains.track.TrackBlockOutline
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.client.event.InputEvent$InteractionKeyMappingTriggered
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartEditMode;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleDragManager;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleEditMode;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackOpenEndpointOutlineClient;
import dev.silvergold.simulatedcoasters.client.track.HoveredCoasterCurve;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.List;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterWrenchCurveAnchorOutlineClient {
    private static final String OUTLINER_KEY_BRIGHT = "wrench_curve_nearest_anchor";
    private static final String OUTLINER_KEY_EDIT_ACTIVE = "wrench_edit_active_anchor";
    private static final int BRIGHT_OUTLINE_COLOR = 0xFFDD55;
    private static final double HOVER_GRAPH_SEARCH_SQ = 4096.0;

    private CoasterWrenchCurveAnchorOutlineClient() {
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
        if (!CoasterWrenchCurveAnchorOutlineClient.isHoldingWrench(mc.player)) {
            return false;
        }
        if (CoasterCartEditMode.isActive()) {
            return false;
        }
        if (BezierHandleDragManager.isHoveringOrDraggingAnyHandle(mc)) {
            return false;
        }
        if (!CoasterWrenchCurveAnchorOutlineClient.isLookingAtCoasterCurve(mc)) {
            return false;
        }
        if (mc.player.isShiftKeyDown()) {
            @Nullable CoasterPathTrackFrame.GraphHit hit = CoasterWrenchCurveAnchorOutlineClient.resolveHoveredCurveGraphHit(mc);
            if (hit == null) {
                return false;
            }
            if (!BezierHandleEditMode.tryRemoveChainLiftFromCurveInteract((Level)mc.level, (Player)mc.player, hit)) {
                return false;
            }
            event.setCanceled(true);
            event.setSwingHand(true);
            return true;
        }
        @Nullable BlockPos anchor = CoasterTrackOpenEndpointOutlineClient.resolveClosestCurveAnchor(mc);
        if (anchor == null) {
            return false;
        }
        BezierHandleEditMode.tryActivateFromCurveInteract((Level)mc.level, (Player)mc.player, anchor);
        event.setCanceled(true);
        event.setSwingHand(true);
        return true;
    }

    @Nullable
    private static CoasterPathTrackFrame.GraphHit resolveHoveredCurveGraphHit(Minecraft mc) {
        @Nullable HoveredCoasterCurve hovered = CoasterTrackOpenEndpointOutlineClient.resolveHoveredCoasterCurve(mc);
        if (hovered == null) {
            return null;
        }
        float pt = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level);
        CoasterPathGraph graph = CoasterPathGraphManagerClient.get((Level)mc.level);
        List<CoasterPathEdge> curveEdges = CoasterPathGraphManager.edgesForBezier(graph, hovered.bc());
        if (curveEdges.isEmpty()) {
            return null;
        }
        return CoasterPathTrackFrame.nearestGraphHit((Level)mc.level, curveEdges, hovered.hoverWorld(), 4096.0, Double.valueOf(pt));
    }

    public static void clientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) {
            CoasterWrenchCurveAnchorOutlineClient.clearAllOutlines();
            return;
        }
        if (!CoasterWrenchCurveAnchorOutlineClient.isHoldingWrench(mc.player)) {
            CoasterWrenchCurveAnchorOutlineClient.clearAllOutlines();
            return;
        }
        if (CoasterCartEditMode.isActive()) {
            CoasterWrenchCurveAnchorOutlineClient.clearAllOutlines();
            return;
        }
        @Nullable BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
        if (editAnchor != null) {
            CoasterWrenchCurveAnchorOutlineClient.showAnchorBlockOutline((Level)mc.level, editAnchor, OUTLINER_KEY_EDIT_ACTIVE, CoasterWrenchCurveAnchorOutlineClient.resolveEditAnchorOutlineColor());
        } else {
            Outliner.getInstance().remove((Object)OUTLINER_KEY_EDIT_ACTIVE);
        }
        if (BezierHandleDragManager.isHoveringOrDraggingAnyHandle(mc)) {
            Outliner.getInstance().remove((Object)OUTLINER_KEY_BRIGHT);
            return;
        }
        @Nullable BlockPos brightTarget = CoasterWrenchCurveAnchorOutlineClient.resolveDirectConnectedAnchorLook(mc);
        if (brightTarget == null && CoasterWrenchCurveAnchorOutlineClient.isLookingAtCoasterCurve(mc)) {
            brightTarget = CoasterTrackOpenEndpointOutlineClient.resolveClosestCurveAnchor(mc);
        }
        if (brightTarget != null && !brightTarget.equals((Object)editAnchor)) {
            CoasterWrenchCurveAnchorOutlineClient.showAnchorBlockOutline((Level)mc.level, brightTarget, OUTLINER_KEY_BRIGHT, 0xFFDD55);
        } else {
            Outliner.getInstance().remove((Object)OUTLINER_KEY_BRIGHT);
        }
    }

    private static int resolveEditAnchorOutlineColor() {
        if (!BezierHandleDragManager.isDraggingHandle()) {
            return 0x888888;
        }
         @Nullable CoasterBezierHandleEdit.PreviewResult pr = BezierHandleDragManager.currentDragPreview();
        if (pr != null && !pr.valid()) {
            return 15359019;
        }
        return 1940687;
    }

    private static boolean isHoldingWrench(LocalPlayer player) {
        return AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem());
    }

    @Nullable
    private static BlockPos resolveDirectConnectedAnchorLook(Minecraft mc) {
        BlockHitResult bhr;
        block7: {
            block6: {
                HitResult hit = mc.hitResult;
                if (!(hit instanceof BlockHitResult)) break block6;
                bhr = (BlockHitResult)hit;
                if (hit.getType() == HitResult.Type.BLOCK) break block7;
            }
            return null;
        }
        BlockPos pos = bhr.getBlockPos();
        if (!(mc.level.getBlockState(pos).getBlock() instanceof CoasterAnchorpointBlock)) {
            return null;
        }
        BlockEntity blockEntity = mc.level.getBlockEntity(pos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        if (be.legCount() < 1) {
            return null;
        }
        return pos;
    }

    private static boolean isLookingAtCoasterCurve(Minecraft mc) {
        if (AnchorPeerCurveHit.isActive()) {
            return true;
        }
        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            return false;
        }
        return TrackBlockOutline.result != null;
    }

    private static void showAnchorBlockOutline(Level level, BlockPos pos, String outlinerKey, int color) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CoasterAnchorpointBlock)) {
            Outliner.getInstance().remove((Object)outlinerKey);
            return;
        }
        VoxelShape shape = state.getShape((BlockGetter)level, pos);
        if (shape.isEmpty()) {
            Outliner.getInstance().remove((Object)outlinerKey);
            return;
        }
        Outliner.getInstance().showAABB((Object)outlinerKey, shape.bounds().move(pos)).colored(color).lineWidth(0.0625f);
    }

    private static void clearAllOutlines() {
        Outliner o = Outliner.getInstance();
        o.remove((Object)OUTLINER_KEY_BRIGHT);
        o.remove((Object)OUTLINER_KEY_EDIT_ACTIVE);
    }
}

