/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleDragManager;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleEditMode;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterTrackGauge;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class BezierHandleCurvePreviewOutliner {
    private static final int PREVIEW_VALID = 1940687;
    private static final int PREVIEW_INVALID = 15359019;
    private static final int LEGACY_COMMITTED_OUTLINE_CLEAR_CAP = 512;
    private static int[] prevSegPreview = new int[4];
    private static int lastSlotCount;

    private BezierHandleCurvePreviewOutliner() {
    }

    public static void clientTick(Minecraft mc) {
        int i;
        List previewCurves;
        ClientLevel level = mc.level;
        if (level == null || !BezierHandleEditMode.isActive() || !BezierHandleDragManager.isDraggingHandle()) {
            BezierHandleCurvePreviewOutliner.clearAllSlots();
            return;
        }
        if (BezierHandleDragManager.isDraggingOpenEndpointHandle()) {
            BezierConnection bc;
            BlockPos anchor = BezierHandleDragManager.openEndpointDragAnchor();
            BezierConnection bezierConnection = bc = anchor != null ? CoasterOpenEndExtension.primaryCoasterCurveAt((Level)level, anchor) : null;
            if (bc == null) {
                BezierHandleCurvePreviewOutliner.clearAllSlots();
                return;
            }
            bc = bc.isPrimary() ? bc : bc.secondary();
            double lift = CoasterTrackGauge.coasterPreviewSpineVerticalBumpBlocks();
            BezierHandleCurvePreviewOutliner.ensureCapacity(1);
            BezierHandleCurvePreviewOutliner.prevSegPreview[0] = CoasterTrackPlacement.drawCoasterCurveOutlinePreview(bc, "coaster_handle_edit_preview_0", 1940687, lift, prevSegPreview[0]);
            for (int i2 = 1; i2 < lastSlotCount; ++i2) {
                BezierHandleCurvePreviewOutliner.clearSlot(i2);
            }
            lastSlotCount = 1;
            return;
        }
        CoasterBezierHandleEdit.PreviewResult pr = BezierHandleDragManager.currentDragPreview();
        BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
        List<Object> list = previewCurves = pr == null || editAnchor == null ? List.of() : pr.curvesIncidentTo(editAnchor);
        if (previewCurves.isEmpty()) {
            BezierHandleCurvePreviewOutliner.clearAllSlots();
            return;
        }
        double lift = CoasterTrackGauge.coasterPreviewSpineVerticalBumpBlocks();
        int previewRailColor = pr.valid() ? 1940687 : 15359019;
        int n = previewCurves.size();
        BezierHandleCurvePreviewOutliner.ensureCapacity(n);
        for (i = 0; i < n; ++i) {
            BezierConnection previewBc = (BezierConnection)previewCurves.get(i);
            previewBc = previewBc.isPrimary() ? previewBc : previewBc.secondary();
            String keyP = "coaster_handle_edit_preview_" + i;
            BezierHandleCurvePreviewOutliner.prevSegPreview[i] = CoasterTrackPlacement.drawCoasterCurveOutlinePreview(previewBc, keyP, previewRailColor, lift, prevSegPreview[i]);
        }
        for (i = n; i < lastSlotCount; ++i) {
            BezierHandleCurvePreviewOutliner.clearSlot(i);
        }
        lastSlotCount = n;
    }

    private static void clearSlot(int i) {
        CoasterTrackPlacement.clearCoasterCurveOutlinePreview("coaster_handle_edit_preview_" + i, prevSegPreview[i]);
        BezierHandleCurvePreviewOutliner.prevSegPreview[i] = 0;
        CoasterTrackPlacement.clearCoasterCurveOutlinePreview("coaster_handle_edit_committed_" + i, 512);
    }

    private static void clearAllSlots() {
        for (int i = 0; i < lastSlotCount; ++i) {
            BezierHandleCurvePreviewOutliner.clearSlot(i);
        }
        lastSlotCount = 0;
    }

    private static void ensureCapacity(int n) {
        if (n <= prevSegPreview.length) {
            return;
        }
        int old = prevSegPreview.length;
        int newLen = Math.max(n, old * 2);
        prevSegPreview = Arrays.copyOf(prevSegPreview, newLen);
        Arrays.fill(prevSegPreview, old, newLen, 0);
    }
}

