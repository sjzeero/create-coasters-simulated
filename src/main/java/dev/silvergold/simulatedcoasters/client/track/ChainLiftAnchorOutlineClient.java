package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.TrackBlockOutline;
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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class ChainLiftAnchorOutlineClient {
   private static final String OUTLINER_KEY_NEAREST = "chain_lift_nearest_anchor";
   private static final String OUTLINER_KEY_SELECTED = "chain_lift_selected_anchor";
   private static final String OUTLINER_KEY_JUNCTION_PREFIX = "chain_lift_junction_";
   private static final String OUTLINER_KEY_CURVE_HOVER = "chain_lift_curve_hover";
   private static final String OUTLINER_KEY_SHAFT_NEAREST = "chain_lift_shaft_nearest";
   private static final String OUTLINER_KEY_SHAFT_SELECTED = "chain_lift_shaft_selected";
   private static final int OUTLINE_COLOR_VALID = 16777215;
   private static final int OUTLINE_COLOR_INVALID = 16729927;
   private static final int MAX_JUNCTION_MARKERS = 2;
   private static final double HOVER_GRAPH_SEARCH_SQ = 4096.0;
   @Nullable
   private static BlockPos selectedAnchor;
   @Nullable
   private static Vec3 lockedShaftAxis;
   private static boolean chainLiftActionBarShowing;
   @Nullable
   private static ChainLiftAnchorOutlineClient.SpanPreviewCacheKey spanPreviewCacheKey;
   @Nullable
   private static CoasterPathGraphSpans.SpanPath spanPreviewCachedPath;
   private static ChainLiftAnchorOutlineClient.SpanPreviewStatus spanPreviewCachedStatus = ChainLiftAnchorOutlineClient.SpanPreviewStatus.NONE;

   private ChainLiftAnchorOutlineClient() {
   }

   public static boolean onUseItemKey(InteractionKeyMappingTriggered event) {
      if (!event.isUseItem()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player == null || mc.level == null) {
            return false;
         } else if (mc.player.isShiftKeyDown()) {
            return false;
         } else {
            InteractionHand hand = event.getHand();
            if (!mc.player.getItemInHand(hand).is(Items.CHAIN)) {
               return false;
            } else if (selectedAnchor != null) {
               ChainLiftAnchorOutlineClient.SpanPreviewState preview = resolveSpanPreview(mc);
               if (preview.status() == ChainLiftAnchorOutlineClient.SpanPreviewStatus.VALID && preview.hoverMarker() != null && lockedShaftAxis != null) {
                  ChainLiftEndRef endRef = buildEndRef(mc, preview);
                  if (endRef != null) {
                     PacketDistributor.sendToServer(new ChainLiftCommitPayload(selectedAnchor, endRef, lockedShaftAxis), new CustomPacketPayload[0]);
                     clearSelectionState();
                     event.setSwingHand(true);
                  }
               }

               event.setCanceled(true);
               return true;
            } else {
               BlockPos anchor = resolveAnchorTarget(mc);
               if (anchor == null) {
                  return false;
               } else {
                  float partialTick = AnimationTickHolder.getPartialTicks(mc.level);
                  selectedAnchor = anchor.immutable();
                  clearSpanPreviewCache();
                  lockedShaftAxis = ChainLiftShaftPreviewRenderer.resolveShaftAxis(mc, anchor, partialTick);
                  ChainLiftPlacementClientSounds.playStartAnchorSelect(mc, CoasterAnchorClientSpace.anchorCenterRenderWorld(mc.level, anchor, partialTick));
                  event.setCanceled(true);
                  event.setSwingHand(true);
                  return true;
               }
            }
         }
      }
   }

   public static void clientTick(Minecraft mc) {
      if (mc.player == null || mc.level == null || mc.screen != null) {
         clearAllOutlines();
      } else if (!isHoldingChain(mc.player)) {
         selectedAnchor = null;
         lockedShaftAxis = null;
         clearAllOutlines();
         endChainLiftActionBarIfShowing(mc.player);
      } else {
         float partialTick = AnimationTickHolder.getPartialTicks(mc.level);
         Outliner outliner = Outliner.getInstance();
         if (selectedAnchor == null) {
            clearSelectedOutlines(outliner);
            BlockPos target = resolveAnchorTarget(mc);
            if (target != null) {
               showAnchorBlockOutline(mc.level, target, "chain_lift_nearest_anchor", 16777215);
               Vec3 shaftAxis = ChainLiftShaftPreviewRenderer.resolveShaftAxis(mc, target, partialTick);
               ChainLiftShaftPreviewRenderer.show(outliner, "chain_lift_shaft_nearest", target, shaftAxis);
            } else {
               outliner.remove("chain_lift_nearest_anchor");
               ChainLiftShaftPreviewRenderer.clear(outliner, "chain_lift_shaft_nearest");
            }
         } else {
            outliner.remove("chain_lift_nearest_anchor");
            ChainLiftShaftPreviewRenderer.clear(outliner, "chain_lift_shaft_nearest");
            if (mc.level.getBlockEntity(selectedAnchor) instanceof CoasterAnchorpointBlockEntity) {
               showAnchorBlockOutline(mc.level, selectedAnchor, "chain_lift_selected_anchor", 16777215);
               if (lockedShaftAxis != null) {
                  ChainLiftShaftPreviewRenderer.show(outliner, "chain_lift_shaft_selected", selectedAnchor, lockedShaftAxis);
               } else {
                  ChainLiftShaftPreviewRenderer.clear(outliner, "chain_lift_shaft_selected");
               }

               ChainLiftAnchorOutlineClient.SplineMarker hoverMarker = null;
               CoasterPathGraphSpans.SpanPath spanPath = null;
               ChainLiftAnchorOutlineClient.SpanPreviewState preview = resolveSpanPreview(mc);
               hoverMarker = preview.hoverMarker();
               spanPath = preview.spanPath();
               ChainLiftAnchorOutlineClient.SpanPreviewStatus spanStatus = preview.status();
               int splineOutlineColor = outlineColorForStatus(spanStatus);
               drawJunctionMarkers(mc.level, selectedAnchor, partialTick, outliner, splineOutlineColor);
               if (hoverMarker != null) {
                  SplineOrientedBoxOutlineRenderer.show(outliner, "chain_lift_curve_hover", hoverMarker.center(), hoverMarker.basis(), splineOutlineColor);
               } else {
                  SplineOrientedBoxOutlineRenderer.clear(outliner, "chain_lift_curve_hover");
               }

               updateSpanPreviewActionBar(mc.player, spanStatus);
               if (spanStatus == ChainLiftAnchorOutlineClient.SpanPreviewStatus.VALID && spanPath != null) {
                  ClientLevel var9 = mc.level;
                  if (var9 instanceof ClientLevel && (var9.getGameTime() & 1L) == 0L) {
                     ChainLiftPreviewParticles.spawnSpanAlongPath(var9, spanPath, partialTick);
                  }
               }
            } else {
               selectedAnchor = null;
               lockedShaftAxis = null;
               clearAllOutlines();
               endChainLiftActionBarIfShowing(mc.player);
            }
         }
      }
   }

   private static ChainLiftAnchorOutlineClient.SpanPreviewState resolveSpanPreview(Minecraft mc) {
      if (selectedAnchor != null && mc.level != null) {
         float partialTick = AnimationTickHolder.getPartialTicks(mc.level);
         CoasterPathGraph graph = CoasterPathGraphManagerClient.get(mc.level);
         ChainLiftAnchorOutlineClient.SplineMarker hoverMarker = null;
         CoasterPathGraphSpans.SpanPath spanPath = null;
         BlockPos endAnchor = resolveDirectConnectedAnchorLook(mc);
         HoveredCoasterCurve hovered = null;
         CoasterPathTrackFrame.GraphHit hoveredHit = null;
         if (endAnchor == null || endAnchor.equals(selectedAnchor)) {
            hovered = CoasterTrackOpenEndpointOutlineClient.resolveHoveredCoasterCurve(mc);
            if (hovered != null && isLookingAtCoasterCurve(mc)) {
               hoveredHit = resolveHoverGraphHit(mc.level, hovered, 0.0F);
            } else {
               hovered = null;
            }
         }

         ChainLiftAnchorOutlineClient.SpanPreviewCacheKey key = previewCacheKey(selectedAnchor, endAnchor, hoveredHit);
         if (key.equals(spanPreviewCacheKey) && spanPreviewCachedPath != null) {
            spanPath = spanPreviewCachedPath;
         } else {
            if (endAnchor != null && !endAnchor.equals(selectedAnchor)) {
               spanPath = CoasterPathGraphSpans.spanPathBetweenAnchors(mc.level, graph, selectedAnchor, endAnchor, 0.0);
            } else if (hoveredHit != null) {
               spanPath = CoasterPathGraphSpans.spanPathFromAnchor(mc.level, graph, selectedAnchor, hoveredHit, 0.0);
            }

            spanPreviewCacheKey = key;
            spanPreviewCachedPath = spanPath;
            spanPreviewCachedStatus = resolveSpanPreviewStatus(mc, endAnchor != null || hoveredHit != null, spanPath);
         }

         if (endAnchor != null && !endAnchor.equals(selectedAnchor)) {
            hoverMarker = resolveAnchorJunctionMarker(mc.level, endAnchor, partialTick);
         } else if (hovered != null) {
            hoverMarker = resolveHoverSplineMarker(mc.level, hovered, partialTick);
         }

         return new ChainLiftAnchorOutlineClient.SpanPreviewState(hoverMarker, spanPath, spanPreviewCachedStatus);
      } else {
         return new ChainLiftAnchorOutlineClient.SpanPreviewState(null, null, ChainLiftAnchorOutlineClient.SpanPreviewStatus.NONE);
      }
   }

   private static ChainLiftAnchorOutlineClient.SpanPreviewCacheKey previewCacheKey(
      BlockPos start, @Nullable BlockPos endAnchor, @Nullable CoasterPathTrackFrame.GraphHit hoveredHit
   ) {
      if (endAnchor != null && !endAnchor.equals(start)) {
         return new ChainLiftAnchorOutlineClient.SpanPreviewCacheKey(ChainLiftSpanPathCache.graphEpoch(), start, endAnchor, null, null, 0.0);
      } else {
         return hoveredHit != null
            ? new ChainLiftAnchorOutlineClient.SpanPreviewCacheKey(
               ChainLiftSpanPathCache.graphEpoch(), start, null, hoveredHit.edge().from(), hoveredHit.edge().to(), hoveredHit.t()
            )
            : new ChainLiftAnchorOutlineClient.SpanPreviewCacheKey(ChainLiftSpanPathCache.graphEpoch(), start, null, null, null, 0.0);
      }
   }

   @Nullable
   private static CoasterPathTrackFrame.GraphHit resolveHoverGraphHit(Level level, HoveredCoasterCurve hovered, float partialTick) {
      CoasterPathGraph graph = CoasterPathGraphManagerClient.get(level);
      List<CoasterPathEdge> curveEdges = CoasterPathGraphManager.edgesForBezier(graph, hovered.bc());
      return curveEdges.isEmpty() ? null : CoasterPathTrackFrame.nearestGraphHit(level, curveEdges, hovered.hoverWorld(), 4096.0, (double)partialTick);
   }

   private static void clearSpanPreviewCache() {
      spanPreviewCacheKey = null;
      spanPreviewCachedPath = null;
      spanPreviewCachedStatus = ChainLiftAnchorOutlineClient.SpanPreviewStatus.NONE;
   }

   @Nullable
   private static ChainLiftEndRef buildEndRef(Minecraft mc, ChainLiftAnchorOutlineClient.SpanPreviewState preview) {
      BlockPos endAnchor = resolveDirectConnectedAnchorLook(mc);
      if (endAnchor != null && !endAnchor.equals(selectedAnchor)) {
         return ChainLiftEndRef.anchor(endAnchor);
      } else {
         ChainLiftAnchorOutlineClient.SplineMarker marker = preview.hoverMarker();
         if (marker != null && marker.graphHit() != null) {
            CoasterPathTrackFrame.GraphHit hit = marker.graphHit();
            return ChainLiftEndRef.curve(hit.edge().from(), hit.edge().to(), hit.t());
         } else {
            return null;
         }
      }
   }

   private static void clearSelectionState() {
      selectedAnchor = null;
      lockedShaftAxis = null;
      clearSpanPreviewCache();
      endChainLiftActionBarIfShowing(Minecraft.getInstance().player);
      clearAllOutlines();
   }

   private static ChainLiftAnchorOutlineClient.SpanPreviewStatus resolveSpanPreviewStatus(
      Minecraft mc, boolean hasHoverTarget, @Nullable CoasterPathGraphSpans.SpanPath spanPath
   ) {
      if (!hasHoverTarget) {
         return ChainLiftAnchorOutlineClient.SpanPreviewStatus.NONE;
      } else if (spanPath == null) {
         return ChainLiftAnchorOutlineClient.SpanPreviewStatus.NOT_CONNECTED;
      } else if (!CoasterPathGraphSpans.isWithinChainLiftSpan(spanPath.totalLength())) {
         return ChainLiftAnchorOutlineClient.SpanPreviewStatus.TOO_LONG;
      } else {
         return selectedAnchor != null
               && mc.level != null
               && ChainLiftSpanOverlap.overlapsAnyExisting(mc.level, spanPath, selectedAnchor, (double)AnimationTickHolder.getPartialTicks(mc.level))
            ? ChainLiftAnchorOutlineClient.SpanPreviewStatus.OVERLAPS
            : ChainLiftAnchorOutlineClient.SpanPreviewStatus.VALID;
      }
   }

   private static int outlineColorForStatus(ChainLiftAnchorOutlineClient.SpanPreviewStatus status) {
      return switch (status) {
         case NONE, VALID -> 16777215;
         case TOO_LONG, NOT_CONNECTED, OVERLAPS -> 16729927;
      };
   }

   private static void updateSpanPreviewActionBar(LocalPlayer player, ChainLiftAnchorOutlineClient.SpanPreviewStatus status) {
      boolean shouldShow = status == ChainLiftAnchorOutlineClient.SpanPreviewStatus.TOO_LONG
         || status == ChainLiftAnchorOutlineClient.SpanPreviewStatus.NOT_CONNECTED
         || status == ChainLiftAnchorOutlineClient.SpanPreviewStatus.OVERLAPS;
      if (shouldShow) {
         Component message = (Component)(switch (status) {
            case TOO_LONG -> CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.chain_lift.too_long");
            case NOT_CONNECTED -> CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.chain_lift.not_connected");
            case OVERLAPS -> CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.chain_lift.overlaps");
            default -> Component.empty();
         });
         player.displayClientMessage(message, true);
         chainLiftActionBarShowing = true;
      } else if (chainLiftActionBarShowing) {
         player.displayClientMessage(Component.empty(), true);
         chainLiftActionBarShowing = false;
      }
   }

   private static void endChainLiftActionBarIfShowing(@Nullable LocalPlayer player) {
      if (player != null && chainLiftActionBarShowing) {
         player.displayClientMessage(Component.empty(), true);
         chainLiftActionBarShowing = false;
      }
   }

   private static void drawJunctionMarkers(Level level, BlockPos anchor, float partialTick, Outliner outliner, int color) {
      List<ChainLiftAnchorOutlineClient.SplineMarker> markers = junctionMarkersAtAnchor(level, anchor, partialTick);

      for (int i = 0; i < 2; i++) {
         String key = "chain_lift_junction_" + i;
         if (i < markers.size()) {
            ChainLiftAnchorOutlineClient.SplineMarker marker = markers.get(i);
            SplineOrientedBoxOutlineRenderer.show(outliner, key, marker.center(), marker.basis(), color);
         } else {
            SplineOrientedBoxOutlineRenderer.clear(outliner, key);
         }
      }
   }

   private static List<ChainLiftAnchorOutlineClient.SplineMarker> junctionMarkersAtAnchor(Level level, BlockPos anchor, float partialTick) {
      CoasterPathGraph graph = CoasterPathGraphManagerClient.get(level);
      List<ChainLiftAnchorOutlineClient.SplineMarker> markers = new ArrayList<>();
      Vec3 nodeCenterPlot = CoasterPathGraphManager.sampleNodePositionPlotLocal(level, new CoasterPathNode(anchor, Vec3.ZERO));

      for (CoasterPathTrackFrame.GraphHit hit : CoasterPathGraphSpans.anchorJunctionHits(level, graph, anchor, (double)partialTick)) {
         CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit);
         markers.add(new ChainLiftAnchorOutlineClient.SplineMarker(nodeCenterPlot, basis, hit));
         if (markers.size() >= 2) {
            break;
         }
      }

      return markers;
   }

   @Nullable
   private static ChainLiftAnchorOutlineClient.SplineMarker resolveAnchorJunctionMarker(Level level, BlockPos anchor, float partialTick) {
      List<ChainLiftAnchorOutlineClient.SplineMarker> markers = junctionMarkersAtAnchor(level, anchor, partialTick);
      return markers.isEmpty() ? null : markers.getFirst();
   }

   @Nullable
   private static ChainLiftAnchorOutlineClient.SplineMarker resolveHoverSplineMarker(Level level, HoveredCoasterCurve hovered, float partialTick) {
      CoasterPathTrackFrame.GraphHit hit = resolveHoverGraphHit(level, hovered, partialTick);
      if (hit == null) {
         return null;
      } else {
         Vec3 centerPlot = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, hit.edge(), hit.t());
         CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit);
         return new ChainLiftAnchorOutlineClient.SplineMarker(centerPlot, basis, hit);
      }
   }

   @Nullable
   private static BlockPos resolveAnchorTarget(Minecraft mc) {
      if (mc.level == null) {
         return null;
      } else {
         BlockPos direct = resolveDirectConnectedAnchorLook(mc);
         if (direct != null) {
            return isEligibleChainLiftStartAnchor(mc.level, direct) ? direct : null;
         } else {
            return resolveClosestEligibleStartAnchorOnCurve(mc);
         }
      }
   }

   private static boolean isEligibleChainLiftStartAnchor(Level level, BlockPos pos) {
      return !(level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity be) ? false : be.legCount() >= 1 && !be.hasChainLiftShaft();
   }

   @Nullable
   private static BlockPos resolveClosestEligibleStartAnchorOnCurve(Minecraft mc) {
      if (mc.level != null && isLookingAtCoasterCurve(mc)) {
         HoveredCoasterCurve hovered = CoasterTrackOpenEndpointOutlineClient.resolveHoveredCoasterCurve(mc);
         if (hovered == null) {
            return null;
         } else {
            float partialTick = AnimationTickHolder.getPartialTicks(mc.level);
            BlockPos ep1 = (BlockPos)hovered.bc().bePositions.getFirst();
            BlockPos ep2 = (BlockPos)hovered.bc().bePositions.getSecond();
            BlockPos closest = null;
            double bestDistSq = Double.POSITIVE_INFINITY;

            for (BlockPos ep : new BlockPos[]{ep1, ep2}) {
               if (isEligibleChainLiftStartAnchor(mc.level, ep)) {
                  double d = hovered.hoverWorld().distanceToSqr(CoasterTrackOpenEndpointOutlineClient.anchorJunctionWorld(mc.level, ep, partialTick));
                  if (d < bestDistSq) {
                     bestDistSq = d;
                     closest = ep;
                  }
               }
            }

            return closest;
         }
      } else {
         return null;
      }
   }

   private static boolean isHoldingChain(LocalPlayer player) {
      return player.getMainHandItem().is(Items.CHAIN) || player.getOffhandItem().is(Items.CHAIN);
   }

   @Nullable
   private static BlockPos resolveDirectConnectedAnchorLook(Minecraft mc) {
      HitResult hit = mc.hitResult;
      if (hit instanceof BlockHitResult bhr && hit.getType() == Type.BLOCK) {
         BlockPos pos = bhr.getBlockPos();
         if (!(mc.level.getBlockState(pos).getBlock() instanceof CoasterAnchorpointBlock)) {
            return null;
         }

         if (mc.level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity be) {
            if (be.legCount() < 1) {
               return null;
            }

            return pos;
         }

         return null;
      }

      return null;
   }

   private static boolean isLookingAtCoasterCurve(Minecraft mc) {
      if (AnchorPeerCurveHit.isActive()) {
         return true;
      } else {
         HitResult hit = mc.hitResult;
         return hit != null && hit.getType() == Type.BLOCK ? false : TrackBlockOutline.result != null;
      }
   }

   private static void showAnchorBlockOutline(Level level, BlockPos pos, String outlinerKey, int color) {
      BlockState state = level.getBlockState(pos);
      if (!(state.getBlock() instanceof CoasterAnchorpointBlock)) {
         Outliner.getInstance().remove(outlinerKey);
      } else {
         VoxelShape shape = state.getShape(level, pos);
         if (shape.isEmpty()) {
            Outliner.getInstance().remove(outlinerKey);
         } else {
            Outliner.getInstance().showAABB(outlinerKey, shape.bounds().move(pos)).colored(color).lineWidth(0.0625F);
         }
      }
   }

   private static void clearSelectedOutlines(Outliner outliner) {
      outliner.remove("chain_lift_selected_anchor");
      ChainLiftShaftPreviewRenderer.clear(outliner, "chain_lift_shaft_selected");
      SplineOrientedBoxOutlineRenderer.clear(outliner, "chain_lift_curve_hover");

      for (int i = 0; i < 2; i++) {
         SplineOrientedBoxOutlineRenderer.clear(outliner, "chain_lift_junction_" + i);
      }
   }

   private static void clearAllOutlines() {
      Outliner outliner = Outliner.getInstance();
      outliner.remove("chain_lift_nearest_anchor");
      ChainLiftShaftPreviewRenderer.clear(outliner, "chain_lift_shaft_nearest");
      clearSelectedOutlines(outliner);
   }

   public static void clearOnLevelUnload() {
      selectedAnchor = null;
      lockedShaftAxis = null;
      chainLiftActionBarShowing = false;
      clearSpanPreviewCache();
      clearAllOutlines();
   }

   private static record SpanPreviewCacheKey(
      int graphEpoch, BlockPos start, @Nullable BlockPos endAnchor, @Nullable BlockPos curveFrom, @Nullable BlockPos curveTo, double curveT
   ) {
   }

   private static record SpanPreviewState(
      @Nullable ChainLiftAnchorOutlineClient.SplineMarker hoverMarker,
      @Nullable CoasterPathGraphSpans.SpanPath spanPath,
      ChainLiftAnchorOutlineClient.SpanPreviewStatus status
   ) {
   }

   private static enum SpanPreviewStatus {
      NONE,
      VALID,
      TOO_LONG,
      NOT_CONNECTED,
      OVERLAPS;
   }

   private static record SplineMarker(Vec3 center, CoasterPathTrackFrame.TrackBasis basis, @Nullable CoasterPathTrackFrame.GraphHit graphHit) {
   }
}
