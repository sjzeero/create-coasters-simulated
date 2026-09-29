/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.TrackBlockOutline
 *  net.createmod.catnip.animation.AnimationTickHolder
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.common.custom.CustomPacketPayload
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  net.neoforged.neoforge.client.event.InputEvent$InteractionKeyMappingTriggered
 *  net.neoforged.neoforge.network.PacketDistributor
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.TrackBlockOutline;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveHit;
import dev.silvergold.simulatedcoasters.client.track.ChainLiftPlacementClientSounds;
import dev.silvergold.simulatedcoasters.client.track.ChainLiftPreviewParticles;
import dev.silvergold.simulatedcoasters.client.track.ChainLiftShaftPreviewRenderer;
import dev.silvergold.simulatedcoasters.client.track.CoasterAnchorClientSpace;
import dev.silvergold.simulatedcoasters.client.track.CoasterTrackOpenEndpointOutlineClient;
import dev.silvergold.simulatedcoasters.client.track.HoveredCoasterCurve;
import dev.silvergold.simulatedcoasters.client.track.SplineOrientedBoxOutlineRenderer;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftCommitPayload;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftEndRef;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanOverlap;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanPathCache;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathNode;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class ChainLiftAnchorOutlineClient {
    private static final String OUTLINER_KEY_NEAREST = "chain_lift_nearest_anchor";
    private static final String OUTLINER_KEY_SELECTED = "chain_lift_selected_anchor";
    private static final String OUTLINER_KEY_JUNCTION_PREFIX = "chain_lift_junction_";
    private static final String OUTLINER_KEY_CURVE_HOVER = "chain_lift_curve_hover";
    private static final String OUTLINER_KEY_SHAFT_NEAREST = "chain_lift_shaft_nearest";
    private static final String OUTLINER_KEY_SHAFT_SELECTED = "chain_lift_shaft_selected";
    private static final int OUTLINE_COLOR_VALID = 0xFFFFFF;
    private static final int OUTLINE_COLOR_INVALID = 0xFF4747;
    private static final int MAX_JUNCTION_MARKERS = 2;
    private static final double HOVER_GRAPH_SEARCH_SQ = 4096.0;
    @Nullable
    private static BlockPos selectedAnchor;
    @Nullable
    private static Vec3 lockedShaftAxis;
    private static boolean chainLiftActionBarShowing;
    @Nullable
    private static SpanPreviewCacheKey spanPreviewCacheKey;
    @Nullable
    private static CoasterPathGraphSpans.SpanPath spanPreviewCachedPath;
    private static SpanPreviewStatus spanPreviewCachedStatus;

    private ChainLiftAnchorOutlineClient() {
    }

    public static boolean onUseItemKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return false;
        }
        if (mc.player.isShiftKeyDown()) {
            return false;
        }
        InteractionHand hand = event.getHand();
        if (!mc.player.getItemInHand(hand).is(Items.CHAIN)) {
            return false;
        }
        if (selectedAnchor != null) {
            ChainLiftEndRef endRef;
            SpanPreviewState preview = ChainLiftAnchorOutlineClient.resolveSpanPreview(mc);
            if (preview.status() == SpanPreviewStatus.VALID && preview.hoverMarker() != null && lockedShaftAxis != null && (endRef = ChainLiftAnchorOutlineClient.buildEndRef(mc, preview)) != null) {
                PacketDistributor.sendToServer((CustomPacketPayload)new ChainLiftCommitPayload(selectedAnchor, endRef, lockedShaftAxis), (CustomPacketPayload[])new CustomPacketPayload[0]);
                ChainLiftAnchorOutlineClient.clearSelectionState();
                event.setSwingHand(true);
            }
            event.setCanceled(true);
            return true;
        }
        @Nullable BlockPos anchor = ChainLiftAnchorOutlineClient.resolveAnchorTarget(mc);
        if (anchor == null) {
            return false;
        }
        float partialTick = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level);
        selectedAnchor = anchor.immutable();
        ChainLiftAnchorOutlineClient.clearSpanPreviewCache();
        lockedShaftAxis = ChainLiftShaftPreviewRenderer.resolveShaftAxis(mc, anchor, partialTick);
        ChainLiftPlacementClientSounds.playStartAnchorSelect(mc, CoasterAnchorClientSpace.anchorCenterRenderWorld((Level)mc.level, anchor, partialTick));
        event.setCanceled(true);
        event.setSwingHand(true);
        return true;
    }

    public static void clientTick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.screen != null) {
            ChainLiftAnchorOutlineClient.clearAllOutlines();
            return;
        }
        if (!ChainLiftAnchorOutlineClient.isHoldingChain(mc.player)) {
            selectedAnchor = null;
            lockedShaftAxis = null;
            ChainLiftAnchorOutlineClient.clearAllOutlines();
            ChainLiftAnchorOutlineClient.endChainLiftActionBarIfShowing(mc.player);
            return;
        }
        float partialTick = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level);
        Outliner outliner = Outliner.getInstance();
        if (selectedAnchor == null) {
            ChainLiftAnchorOutlineClient.clearSelectedOutlines(outliner);
            @Nullable BlockPos target = ChainLiftAnchorOutlineClient.resolveAnchorTarget(mc);
            if (target != null) {
                ChainLiftAnchorOutlineClient.showAnchorBlockOutline((Level)mc.level, target, OUTLINER_KEY_NEAREST, 0xFFFFFF);
                Vec3 shaftAxis = ChainLiftShaftPreviewRenderer.resolveShaftAxis(mc, target, partialTick);
                ChainLiftShaftPreviewRenderer.show(outliner, OUTLINER_KEY_SHAFT_NEAREST, target, shaftAxis);
            } else {
                outliner.remove((Object)OUTLINER_KEY_NEAREST);
                ChainLiftShaftPreviewRenderer.clear(outliner, OUTLINER_KEY_SHAFT_NEAREST);
            }
            return;
        }
        outliner.remove((Object)OUTLINER_KEY_NEAREST);
        ChainLiftShaftPreviewRenderer.clear(outliner, OUTLINER_KEY_SHAFT_NEAREST);
        if (mc.level.getBlockEntity(selectedAnchor) instanceof CoasterAnchorpointBlockEntity) {
            ClientLevel clientLevel;
            ClientLevel clientLevel2;
            ChainLiftAnchorOutlineClient.showAnchorBlockOutline((Level)mc.level, selectedAnchor, OUTLINER_KEY_SELECTED, 0xFFFFFF);
            if (lockedShaftAxis != null) {
                ChainLiftShaftPreviewRenderer.show(outliner, OUTLINER_KEY_SHAFT_SELECTED, selectedAnchor, lockedShaftAxis);
            } else {
                ChainLiftShaftPreviewRenderer.clear(outliner, OUTLINER_KEY_SHAFT_SELECTED);
            }
            @Nullable SplineMarker hoverMarker = null;
            CoasterPathGraphSpans.SpanPath spanPath = null;
            SpanPreviewState preview = ChainLiftAnchorOutlineClient.resolveSpanPreview(mc);
            hoverMarker = preview.hoverMarker();
            spanPath = preview.spanPath();
            SpanPreviewStatus spanStatus = preview.status();
            int splineOutlineColor = ChainLiftAnchorOutlineClient.outlineColorForStatus(spanStatus);
            ChainLiftAnchorOutlineClient.drawJunctionMarkers((Level)mc.level, selectedAnchor, partialTick, outliner, splineOutlineColor);
            if (hoverMarker != null) {
                SplineOrientedBoxOutlineRenderer.show(outliner, OUTLINER_KEY_CURVE_HOVER, hoverMarker.center(), hoverMarker.basis(), splineOutlineColor);
            } else {
                SplineOrientedBoxOutlineRenderer.clear(outliner, OUTLINER_KEY_CURVE_HOVER);
            }
            ChainLiftAnchorOutlineClient.updateSpanPreviewActionBar(mc.player, spanStatus);
            if (spanStatus == SpanPreviewStatus.VALID && spanPath != null && (clientLevel2 = mc.level) instanceof ClientLevel && ((clientLevel = clientLevel2).getGameTime() & 1L) == 0L) {
                ChainLiftPreviewParticles.spawnSpanAlongPath(clientLevel, spanPath, partialTick);
            }
        } else {
            selectedAnchor = null;
            lockedShaftAxis = null;
            ChainLiftAnchorOutlineClient.clearAllOutlines();
            ChainLiftAnchorOutlineClient.endChainLiftActionBarIfShowing(mc.player);
            return;
        }
    }

    private static SpanPreviewState resolveSpanPreview(Minecraft mc) {
        SpanPreviewCacheKey key;
        if (selectedAnchor == null || mc.level == null) {
            return new SpanPreviewState(null, null, SpanPreviewStatus.NONE);
        }
        float partialTick = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level);
        CoasterPathGraph graph = CoasterPathGraphManagerClient.get((Level)mc.level);
        @Nullable SplineMarker hoverMarker = null;
        CoasterPathGraphSpans.SpanPath spanPath = null;
        @Nullable BlockPos endAnchor = ChainLiftAnchorOutlineClient.resolveDirectConnectedAnchorLook(mc);
        HoveredCoasterCurve hovered = null;
        CoasterPathTrackFrame.GraphHit hoveredHit = null;
        if (endAnchor == null || endAnchor.equals((Object)selectedAnchor)) {
            hovered = CoasterTrackOpenEndpointOutlineClient.resolveHoveredCoasterCurve(mc);
            if (hovered == null || !ChainLiftAnchorOutlineClient.isLookingAtCoasterCurve(mc)) {
                hovered = null;
            } else {
                hoveredHit = ChainLiftAnchorOutlineClient.resolveHoverGraphHit((Level)mc.level, hovered, 0.0f);
            }
        }
        if ((key = ChainLiftAnchorOutlineClient.previewCacheKey(selectedAnchor, endAnchor, hoveredHit)).equals(spanPreviewCacheKey) && spanPreviewCachedPath != null) {
            spanPath = spanPreviewCachedPath;
        } else {
            if (endAnchor != null && !endAnchor.equals((Object)selectedAnchor)) {
                spanPath = CoasterPathGraphSpans.spanPathBetweenAnchors((Level)mc.level, graph, selectedAnchor, endAnchor, 0.0);
            } else if (hoveredHit != null) {
                spanPath = CoasterPathGraphSpans.spanPathFromAnchor((Level)mc.level, graph, selectedAnchor, hoveredHit, 0.0);
            }
            spanPreviewCacheKey = key;
            spanPreviewCachedPath = spanPath;
            spanPreviewCachedStatus = ChainLiftAnchorOutlineClient.resolveSpanPreviewStatus(mc, endAnchor != null || hoveredHit != null, spanPath);
        }
        if (endAnchor != null && !endAnchor.equals((Object)selectedAnchor)) {
            hoverMarker = ChainLiftAnchorOutlineClient.resolveAnchorJunctionMarker((Level)mc.level, endAnchor, partialTick);
        } else if (hovered != null) {
            hoverMarker = ChainLiftAnchorOutlineClient.resolveHoverSplineMarker((Level)mc.level, hovered, partialTick);
        }
        return new SpanPreviewState(hoverMarker, spanPath, spanPreviewCachedStatus);
    }

    private static SpanPreviewCacheKey previewCacheKey(BlockPos start, @Nullable BlockPos endAnchor, @Nullable CoasterPathTrackFrame.GraphHit hoveredHit) {
        if (endAnchor != null && !endAnchor.equals((Object)start)) {
            return new SpanPreviewCacheKey(ChainLiftSpanPathCache.graphEpoch(), start, endAnchor, null, null, 0.0);
        }
        if (hoveredHit != null) {
            return new SpanPreviewCacheKey(ChainLiftSpanPathCache.graphEpoch(), start, null, hoveredHit.edge().from(), hoveredHit.edge().to(), hoveredHit.t());
        }
        return new SpanPreviewCacheKey(ChainLiftSpanPathCache.graphEpoch(), start, null, null, null, 0.0);
    }

    @Nullable
    private static CoasterPathTrackFrame.GraphHit resolveHoverGraphHit(Level level, HoveredCoasterCurve hovered, float partialTick) {
        CoasterPathGraph graph = CoasterPathGraphManagerClient.get(level);
        List<CoasterPathEdge> curveEdges = CoasterPathGraphManager.edgesForBezier(graph, hovered.bc());
        if (curveEdges.isEmpty()) {
            return null;
        }
        return CoasterPathTrackFrame.nearestGraphHit(level, curveEdges, hovered.hoverWorld(), 4096.0, Double.valueOf(partialTick));
    }

    private static void clearSpanPreviewCache() {
        spanPreviewCacheKey = null;
        spanPreviewCachedPath = null;
        spanPreviewCachedStatus = SpanPreviewStatus.NONE;
    }

    @Nullable
    private static ChainLiftEndRef buildEndRef(Minecraft mc, SpanPreviewState preview) {
        @Nullable BlockPos endAnchor = ChainLiftAnchorOutlineClient.resolveDirectConnectedAnchorLook(mc);
        if (endAnchor != null && !endAnchor.equals((Object)selectedAnchor)) {
            return ChainLiftEndRef.anchor(endAnchor);
        }
        @Nullable SplineMarker marker = preview.hoverMarker();
        if (marker == null || marker.graphHit() == null) {
            return null;
        }
        CoasterPathTrackFrame.GraphHit hit = marker.graphHit();
        return ChainLiftEndRef.curve(hit.edge().from(), hit.edge().to(), hit.t());
    }

    private static void clearSelectionState() {
        selectedAnchor = null;
        lockedShaftAxis = null;
        ChainLiftAnchorOutlineClient.clearSpanPreviewCache();
        ChainLiftAnchorOutlineClient.endChainLiftActionBarIfShowing(Minecraft.getInstance().player);
        ChainLiftAnchorOutlineClient.clearAllOutlines();
    }

    private static SpanPreviewStatus resolveSpanPreviewStatus(Minecraft mc, boolean hasHoverTarget, @Nullable CoasterPathGraphSpans.SpanPath spanPath) {
        if (!hasHoverTarget) {
            return SpanPreviewStatus.NONE;
        }
        if (spanPath == null) {
            return SpanPreviewStatus.NOT_CONNECTED;
        }
        if (!CoasterPathGraphSpans.isWithinChainLiftSpan(spanPath.totalLength())) {
            return SpanPreviewStatus.TOO_LONG;
        }
        if (selectedAnchor != null && mc.level != null && ChainLiftSpanOverlap.overlapsAnyExisting((Level)mc.level, spanPath, selectedAnchor, AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level))) {
            return SpanPreviewStatus.OVERLAPS;
        }
        return SpanPreviewStatus.VALID;
    }

    private static int outlineColorForStatus(SpanPreviewStatus status) {
        return switch (status.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0, 1 -> 0xFFFFFF;
            case 2, 3, 4 -> 0xFF4747;
        };
    }

    private static void updateSpanPreviewActionBar(LocalPlayer player, SpanPreviewStatus status) {
        boolean shouldShow;
        boolean bl = shouldShow = status == SpanPreviewStatus.TOO_LONG || status == SpanPreviewStatus.NOT_CONNECTED || status == SpanPreviewStatus.OVERLAPS;
        if (shouldShow) {
            MutableComponent message = switch (status.ordinal()) {
                case 2 -> CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.chain_lift.too_long");
                case 3 -> CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.chain_lift.not_connected");
                case 4 -> CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.chain_lift.overlaps");
                default -> Component.empty();
            };
            player.displayClientMessage((Component)message, true);
            chainLiftActionBarShowing = true;
        } else if (chainLiftActionBarShowing) {
            player.displayClientMessage((Component)Component.empty(), true);
            chainLiftActionBarShowing = false;
        }
    }

    private static void endChainLiftActionBarIfShowing(@Nullable LocalPlayer player) {
        if (player == null || !chainLiftActionBarShowing) {
            return;
        }
        player.displayClientMessage((Component)Component.empty(), true);
        chainLiftActionBarShowing = false;
    }

    private static void drawJunctionMarkers(Level level, BlockPos anchor, float partialTick, Outliner outliner, int color) {
        List<SplineMarker> markers = ChainLiftAnchorOutlineClient.junctionMarkersAtAnchor(level, anchor, partialTick);
        for (int i = 0; i < 2; ++i) {
            String key = OUTLINER_KEY_JUNCTION_PREFIX + i;
            if (i < markers.size()) {
                SplineMarker marker = markers.get(i);
                SplineOrientedBoxOutlineRenderer.show(outliner, key, marker.center(), marker.basis(), color);
                continue;
            }
            SplineOrientedBoxOutlineRenderer.clear(outliner, key);
        }
    }

    private static List<SplineMarker> junctionMarkersAtAnchor(Level level, BlockPos anchor, float partialTick) {
        CoasterPathGraph graph = CoasterPathGraphManagerClient.get(level);
        ArrayList<SplineMarker> markers = new ArrayList<SplineMarker>();
        Vec3 nodeCenterPlot = CoasterPathGraphManager.sampleNodePositionPlotLocal(level, new CoasterPathNode(anchor, Vec3.ZERO));
        for (CoasterPathTrackFrame.GraphHit hit : CoasterPathGraphSpans.anchorJunctionHits(level, graph, anchor, partialTick)) {
            CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit);
            markers.add(new SplineMarker(nodeCenterPlot, basis, hit));
            if (markers.size() < 2) continue;
            break;
        }
        return markers;
    }

    @Nullable
    private static SplineMarker resolveAnchorJunctionMarker(Level level, BlockPos anchor, float partialTick) {
        List<SplineMarker> markers = ChainLiftAnchorOutlineClient.junctionMarkersAtAnchor(level, anchor, partialTick);
        return markers.isEmpty() ? null : markers.getFirst();
    }

    @Nullable
    private static SplineMarker resolveHoverSplineMarker(Level level, HoveredCoasterCurve hovered, float partialTick) {
        @Nullable CoasterPathTrackFrame.GraphHit hit = ChainLiftAnchorOutlineClient.resolveHoverGraphHit(level, hovered, partialTick);
        if (hit == null) {
            return null;
        }
        Vec3 centerPlot = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, hit.edge(), hit.t());
        CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit);
        return new SplineMarker(centerPlot, basis, hit);
    }

    @Nullable
    private static BlockPos resolveAnchorTarget(Minecraft mc) {
        if (mc.level == null) {
            return null;
        }
        @Nullable BlockPos direct = ChainLiftAnchorOutlineClient.resolveDirectConnectedAnchorLook(mc);
        if (direct != null) {
            return ChainLiftAnchorOutlineClient.isEligibleChainLiftStartAnchor((Level)mc.level, direct) ? direct : null;
        }
        return ChainLiftAnchorOutlineClient.resolveClosestEligibleStartAnchorOnCurve(mc);
    }

    private static boolean isEligibleChainLiftStartAnchor(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        return be.legCount() >= 1 && !be.hasChainLiftShaft();
    }

    @Nullable
    private static BlockPos resolveClosestEligibleStartAnchorOnCurve(Minecraft mc) {
        if (mc.level == null || !ChainLiftAnchorOutlineClient.isLookingAtCoasterCurve(mc)) {
            return null;
        }
        @Nullable HoveredCoasterCurve hovered = CoasterTrackOpenEndpointOutlineClient.resolveHoveredCoasterCurve(mc);
        if (hovered == null) {
            return null;
        }
        float partialTick = AnimationTickHolder.getPartialTicks((LevelAccessor)mc.level);
        BlockPos ep1 = (BlockPos)hovered.bc().bePositions.getFirst();
        BlockPos ep2 = (BlockPos)hovered.bc().bePositions.getSecond();
        BlockPos closest = null;
        double bestDistSq = Double.POSITIVE_INFINITY;
        for (BlockPos ep : new BlockPos[]{ep1, ep2}) {
            double d;
            if (!ChainLiftAnchorOutlineClient.isEligibleChainLiftStartAnchor((Level)mc.level, ep) || !((d = hovered.hoverWorld().distanceToSqr(CoasterTrackOpenEndpointOutlineClient.anchorJunctionWorld((Level)mc.level, ep, partialTick))) < bestDistSq)) continue;
            bestDistSq = d;
            closest = ep;
        }
        return closest;
    }

    private static boolean isHoldingChain(LocalPlayer player) {
        return player.getMainHandItem().is(Items.CHAIN) || player.getOffhandItem().is(Items.CHAIN);
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

    private static void clearSelectedOutlines(Outliner outliner) {
        outliner.remove((Object)OUTLINER_KEY_SELECTED);
        ChainLiftShaftPreviewRenderer.clear(outliner, OUTLINER_KEY_SHAFT_SELECTED);
        SplineOrientedBoxOutlineRenderer.clear(outliner, OUTLINER_KEY_CURVE_HOVER);
        for (int i = 0; i < 2; ++i) {
            SplineOrientedBoxOutlineRenderer.clear(outliner, OUTLINER_KEY_JUNCTION_PREFIX + i);
        }
    }

    private static void clearAllOutlines() {
        Outliner outliner = Outliner.getInstance();
        outliner.remove((Object)OUTLINER_KEY_NEAREST);
        ChainLiftShaftPreviewRenderer.clear(outliner, OUTLINER_KEY_SHAFT_NEAREST);
        ChainLiftAnchorOutlineClient.clearSelectedOutlines(outliner);
    }

    public static void clearOnLevelUnload() {
        selectedAnchor = null;
        lockedShaftAxis = null;
        chainLiftActionBarShowing = false;
        ChainLiftAnchorOutlineClient.clearSpanPreviewCache();
        ChainLiftAnchorOutlineClient.clearAllOutlines();
    }

    static {
        spanPreviewCachedStatus = SpanPreviewStatus.NONE;
    }

    private record SpanPreviewState(@Nullable SplineMarker hoverMarker, @Nullable CoasterPathGraphSpans.SpanPath spanPath, SpanPreviewStatus status) {
    }

    private static enum SpanPreviewStatus {
        NONE,
        VALID,
        TOO_LONG,
        NOT_CONNECTED,
        OVERLAPS;

    }

    private record SplineMarker(Vec3 center, CoasterPathTrackFrame.TrackBasis basis, @Nullable CoasterPathTrackFrame.GraphHit graphHit) {
    }

    private record SpanPreviewCacheKey(int graphEpoch, BlockPos start, @Nullable BlockPos endAnchor, @Nullable BlockPos curveFrom, @Nullable BlockPos curveTo, double curveT) {
    }
}

