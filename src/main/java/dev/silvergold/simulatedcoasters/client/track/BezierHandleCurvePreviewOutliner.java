package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BezierHandleCurvePreviewOutliner {
   private static final int PREVIEW_VALID = 1940687;
   private static final int PREVIEW_INVALID = 15359019;
   private static final int LEGACY_COMMITTED_OUTLINE_CLEAR_CAP = 512;
   private static int[] prevSegPreview = new int[4];
   private static int lastSlotCount;

   private BezierHandleCurvePreviewOutliner() {
   }

   public static void clientTick(Minecraft mc) {
      Level level = mc.level;
      if (level == null || !BezierHandleEditMode.isActive() || !BezierHandleDragManager.isDraggingHandle()) {
         clearAllSlots();
      } else if (BezierHandleDragManager.isDraggingOpenEndpointHandle()) {
         BlockPos anchor = BezierHandleDragManager.openEndpointDragAnchor();
         BezierConnection bc = anchor != null ? CoasterOpenEndExtension.primaryCoasterCurveAt(level, anchor) : null;
         if (bc == null) {
            clearAllSlots();
         } else {
            bc = bc.isPrimary() ? bc : bc.secondary();
            double lift = (double)CoasterTrackGauge.coasterPreviewSpineVerticalBumpBlocks();
            ensureCapacity(1);
            prevSegPreview[0] = CoasterTrackPlacement.drawCoasterCurveOutlinePreview(bc, "coaster_handle_edit_preview_0", 1940687, lift, prevSegPreview[0]);

            for (int i = 1; i < lastSlotCount; i++) {
               clearSlot(i);
            }

            lastSlotCount = 1;
         }
      } else {
         CoasterBezierHandleEdit.PreviewResult pr = BezierHandleDragManager.currentDragPreview();
         BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
         List<BezierConnection> previewCurves = pr != null && editAnchor != null ? pr.curvesIncidentTo(editAnchor) : List.of();
         if (previewCurves.isEmpty()) {
            clearAllSlots();
         } else {
            double lift = (double)CoasterTrackGauge.coasterPreviewSpineVerticalBumpBlocks();
            int previewRailColor = pr.valid() ? 1940687 : 15359019;
            int n = previewCurves.size();
            ensureCapacity(n);

            for (int i = 0; i < n; i++) {
               BezierConnection previewBc = previewCurves.get(i);
               previewBc = previewBc.isPrimary() ? previewBc : previewBc.secondary();
               String keyP = "coaster_handle_edit_preview_" + i;
               prevSegPreview[i] = CoasterTrackPlacement.drawCoasterCurveOutlinePreview(previewBc, keyP, previewRailColor, lift, prevSegPreview[i]);
            }

            for (int i = n; i < lastSlotCount; i++) {
               clearSlot(i);
            }

            lastSlotCount = n;
         }
      }
   }

   private static void clearSlot(int i) {
      CoasterTrackPlacement.clearCoasterCurveOutlinePreview("coaster_handle_edit_preview_" + i, prevSegPreview[i]);
      prevSegPreview[i] = 0;
      CoasterTrackPlacement.clearCoasterCurveOutlinePreview("coaster_handle_edit_committed_" + i, 512);
   }

   private static void clearAllSlots() {
      for (int i = 0; i < lastSlotCount; i++) {
         clearSlot(i);
      }

      lastSlotCount = 0;
   }

   private static void ensureCapacity(int n) {
      if (n > prevSegPreview.length) {
         int old = prevSegPreview.length;
         int newLen = Math.max(n, old * 2);
         prevSegPreview = Arrays.copyOf(prevSegPreview, newLen);
         Arrays.fill(prevSegPreview, old, newLen, 0);
      }
   }
}
