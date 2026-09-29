/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.createmod.catnip.data.Pair
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleDragManager;
import dev.silvergold.simulatedcoasters.client.track.BezierHandleEditMode;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import java.util.HashSet;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterBezierPlacementSpineDebugRenderer {
    private static final int SPINE_COLOR = 0x22FFFF;
    private static final int CONTROL_STALK_COLOR = 0xFFAA33;
    private static final double DRAW_DIST = 112.0;

    private CoasterBezierPlacementSpineDebugRenderer() {
    }

    public static void render(Minecraft mc, Level level) {
        if (!BezierHandleEditMode.isActive()) {
            return;
        }
        BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
        if (editAnchor == null || mc.player == null) {
            return;
        }
        Entity camEntity = mc.cameraEntity;
        if (camEntity == null) {
            return;
        }
        Vec3 camera = camEntity.getEyePosition();
        double drawSq = 12544.0;
        HashSet<String> seen = new HashSet<String>();
        for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
            if (anchor.getLevel() != level || anchor.isRemoved() || !editAnchor.equals((Object)anchor.getBlockPos())) continue;
            for (BezierConnection bcRaw : anchor.getAnchorPeerCurvesView().values()) {
                BezierConnection bc;
                if (bcRaw == null) continue;
                BezierConnection bezierConnection = bc = bcRaw.isPrimary() ? bcRaw : bcRaw.secondary();
                if (!bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id) || !seen.add(CoasterBezierPlacementSpineDebugRenderer.edgeKey(bc))) continue;
                String ek = CoasterBezierPlacementSpineDebugRenderer.edgeKey(bc);
                BezierConnection draw = BezierHandleDragManager.previewCurveOrWorld(ek, bc);
                CoasterBezierPlacementSpineDebugRenderer.renderOneCurve(draw, ek, camera, drawSq);
            }
        }
    }

    private static void renderOneCurve(BezierConnection bc, String edgeKey, Vec3 camera, double drawSq) {
        int samples = Math.max(48, bc.getSegmentCount() * 4);
        Vec3 prev = null;
        for (int i = 0; i <= samples; ++i) {
            double t = (double)i / (double)samples;
            Vec3 p = CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(bc, t));
            if (p.distanceToSqr(camera) > drawSq) {
                prev = p;
                continue;
            }
            if (prev != null && prev.distanceToSqr(camera) <= drawSq) {
                Outliner.getInstance().showLine((Object)Pair.of((Object)"sc_place_spine", (Object)(edgeKey + ":" + i)), prev, p).colored(0x22FFFF).lineWidth(0.071428575f);
            }
            prev = p;
        }
        Vec3 e1 = (Vec3)bc.starts.getFirst();
        Vec3 e2 = (Vec3)bc.starts.getSecond();
        Vec3 f1 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 0);
        Vec3 f2 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 1);
        CoasterBezierPlacementSpineDebugRenderer.drawStalk(CoasterPathGraphManager.pathCenterlineWorld(e1), CoasterPathGraphManager.pathCenterlineWorld(f1), edgeKey + ":stalk0", camera, drawSq);
        CoasterBezierPlacementSpineDebugRenderer.drawStalk(CoasterPathGraphManager.pathCenterlineWorld(e2), CoasterPathGraphManager.pathCenterlineWorld(f2), edgeKey + ":stalk1", camera, drawSq);
    }

    private static void drawStalk(Vec3 joint, Vec3 tip, String key, Vec3 camera, double drawSq) {
        if (joint.distanceToSqr(camera) > drawSq && tip.distanceToSqr(camera) > drawSq) {
            return;
        }
        Outliner.getInstance().showLine((Object)Pair.of((Object)"sc_place_ctrl", (Object)key), joint, tip).colored(0xFFAA33).lineWidth(0.055555556f);
    }

    private static String edgeKey(BezierConnection bc) {
        long b;
        long a = ((BlockPos)bc.bePositions.getFirst()).asLong();
        return a <= (b = ((BlockPos)bc.bePositions.getSecond()).asLong()) ? a + ":" + b : b + ":" + a;
    }
}

