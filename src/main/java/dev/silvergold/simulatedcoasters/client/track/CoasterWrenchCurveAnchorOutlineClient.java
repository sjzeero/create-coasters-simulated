package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartEditMode;
import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class CoasterWrenchCurveAnchorOutlineClient {
   private static final String OUTLINER_KEY_BRIGHT = "wrench_curve_nearest_anchor";
   private static final String OUTLINER_KEY_EDIT_ACTIVE = "wrench_edit_active_anchor";
   private static final int BRIGHT_OUTLINE_COLOR = 16768341;
   private static final double HOVER_GRAPH_SEARCH_SQ = 4096.0;

   private CoasterWrenchCurveAnchorOutlineClient() {
   }

   public static boolean onUseItemKey(InteractionKeyMappingTriggered event) {
      if (!event.isUseItem()) {
         return false;
      } else {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            if (event.getHand() != InteractionHand.MAIN_HAND) {
               return false;
            } else if (!isHoldingWrench(mc.player)) {
               return false;
            } else if (CoasterCartEditMode.isActive()) {
               return false;
            } else if (BezierHandleDragManager.isHoveringOrDraggingAnyHandle(mc)) {
               return false;
            } else if (!isLookingAtCoasterCurve(mc)) {
               return false;
            } else if (mc.player.isShiftKeyDown()) {
               CoasterPathTrackFrame.GraphHit hit = resolveHoveredCurveGraphHit(mc);
               if (hit == null) {
                  return false;
               } else if (!BezierHandleEditMode.tryRemoveChainLiftFromCurveInteract(mc.level, mc.player, hit)) {
                  return false;
               } else {
                  event.setCanceled(true);
                  event.setSwingHand(true);
                  return true;
               }
            } else {
               BlockPos anchor = CoasterTrackOpenEndpointOutlineClient.resolveClosestCurveAnchor(mc);
               if (anchor == null) {
                  return false;
               } else {
                  BezierHandleEditMode.tryActivateFromCurveInteract(mc.level, mc.player, anchor);
                  event.setCanceled(true);
                  event.setSwingHand(true);
                  return true;
               }
            }
         } else {
            return false;
         }
      }
   }

   @Nullable
   private static CoasterPathTrackFrame.GraphHit resolveHoveredCurveGraphHit(Minecraft mc) {
      HoveredCoasterCurve hovered = CoasterTrackOpenEndpointOutlineClient.resolveHoveredCoasterCurve(mc);
      if (hovered == null) {
         return null;
      } else {
         float pt = AnimationTickHolder.getPartialTicks(mc.level);
         CoasterPathGraph graph = CoasterPathGraphManagerClient.get(mc.level);
         List<CoasterPathEdge> curveEdges = CoasterPathGraphManager.edgesForBezier(graph, hovered.bc());
         return curveEdges.isEmpty() ? null : CoasterPathTrackFrame.nearestGraphHit(mc.level, curveEdges, hovered.hoverWorld(), 4096.0, (double)pt);
      }
   }

   public static void clientTick(Minecraft mc) {
      if (mc.player == null || mc.level == null || mc.screen != null) {
         clearAllOutlines();
      } else if (!isHoldingWrench(mc.player)) {
         clearAllOutlines();
      } else if (CoasterCartEditMode.isActive()) {
         clearAllOutlines();
      } else {
         BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
         if (editAnchor != null) {
            showAnchorBlockOutline(mc.level, editAnchor, "wrench_edit_active_anchor", resolveEditAnchorOutlineColor());
         } else {
            Outliner.getInstance().remove("wrench_edit_active_anchor");
         }

         if (BezierHandleDragManager.isHoveringOrDraggingAnyHandle(mc)) {
            Outliner.getInstance().remove("wrench_curve_nearest_anchor");
         } else {
            BlockPos brightTarget = resolveDirectConnectedAnchorLook(mc);
            if (brightTarget == null && isLookingAtCoasterCurve(mc)) {
               brightTarget = CoasterTrackOpenEndpointOutlineClient.resolveClosestCurveAnchor(mc);
            }

            if (brightTarget != null && !brightTarget.equals(editAnchor)) {
               showAnchorBlockOutline(mc.level, brightTarget, "wrench_curve_nearest_anchor", 16768341);
            } else {
               Outliner.getInstance().remove("wrench_curve_nearest_anchor");
            }
         }
      }
   }

   private static int resolveEditAnchorOutlineColor() {
      if (!BezierHandleDragManager.isDraggingHandle()) {
         return 8947848;
      } else {
         CoasterBezierHandleEdit.PreviewResult pr = BezierHandleDragManager.currentDragPreview();
         return pr != null && !pr.valid() ? 15359019 : 1940687;
      }
   }

   private static boolean isHoldingWrench(LocalPlayer player) {
      return AllItems.WRENCH.isIn(player.getMainHandItem()) || AllItems.WRENCH.isIn(player.getOffhandItem());
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

   private static void clearAllOutlines() {
      Outliner o = Outliner.getInstance();
      o.remove("wrench_curve_nearest_anchor");
      o.remove("wrench_edit_active_anchor");
   }
}
