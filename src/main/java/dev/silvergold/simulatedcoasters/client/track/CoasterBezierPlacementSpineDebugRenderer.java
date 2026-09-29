package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import java.util.HashSet;
import java.util.Set;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CoasterBezierPlacementSpineDebugRenderer {
   private static final int SPINE_COLOR = 2293759;
   private static final int CONTROL_STALK_COLOR = 16755251;
   private static final double DRAW_DIST = 112.0;

   private CoasterBezierPlacementSpineDebugRenderer() {
   }

   public static void render(Minecraft mc, Level level) {
      if (BezierHandleEditMode.isActive()) {
         BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
         if (editAnchor != null && mc.player != null) {
            Entity camEntity = mc.cameraEntity;
            if (camEntity != null) {
               Vec3 camera = camEntity.getEyePosition();
               double drawSq = 12544.0;
               Set<String> seen = new HashSet<>();

               for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
                  if (anchor.getLevel() == level && !anchor.isRemoved() && editAnchor.equals(anchor.getBlockPos())) {
                     for (BezierConnection bcRaw : anchor.getAnchorPeerCurvesView().values()) {
                        if (bcRaw != null) {
                           BezierConnection bc = bcRaw.isPrimary() ? bcRaw : bcRaw.secondary();
                           if (bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id) && seen.add(edgeKey(bc))) {
                              String ek = edgeKey(bc);
                              BezierConnection draw = BezierHandleDragManager.previewCurveOrWorld(ek, bc);
                              renderOneCurve(draw, ek, camera, drawSq);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void renderOneCurve(BezierConnection bc, String edgeKey, Vec3 camera, double drawSq) {
      int samples = Math.max(48, bc.getSegmentCount() * 4);
      Vec3 prev = null;

      for (int i = 0; i <= samples; i++) {
         double t = (double)i / (double)samples;
         Vec3 p = CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(bc, t));
         if (p.distanceToSqr(camera) > drawSq) {
            prev = p;
         } else {
            if (prev != null && prev.distanceToSqr(camera) <= drawSq) {
               Outliner.getInstance().showLine(Pair.of("sc_place_spine", edgeKey + ":" + i), prev, p).colored(2293759).lineWidth(0.071428575F);
            }

            prev = p;
         }
      }

      Vec3 e1 = (Vec3)bc.starts.getFirst();
      Vec3 e2 = (Vec3)bc.starts.getSecond();
      Vec3 f1 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 0);
      Vec3 f2 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 1);
      drawStalk(CoasterPathGraphManager.pathCenterlineWorld(e1), CoasterPathGraphManager.pathCenterlineWorld(f1), edgeKey + ":stalk0", camera, drawSq);
      drawStalk(CoasterPathGraphManager.pathCenterlineWorld(e2), CoasterPathGraphManager.pathCenterlineWorld(f2), edgeKey + ":stalk1", camera, drawSq);
   }

   private static void drawStalk(Vec3 joint, Vec3 tip, String key, Vec3 camera, double drawSq) {
      if (!(joint.distanceToSqr(camera) > drawSq) || !(tip.distanceToSqr(camera) > drawSq)) {
         Outliner.getInstance().showLine(Pair.of("sc_place_ctrl", key), joint, tip).colored(16755251).lineWidth(0.055555556F);
      }
   }

   private static String edgeKey(BezierConnection bc) {
      long a = ((BlockPos)bc.bePositions.getFirst()).asLong();
      long b = ((BlockPos)bc.bePositions.getSecond()).asLong();
      return a <= b ? a + ":" + b : b + ":" + a;
   }
}
