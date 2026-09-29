package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurveClientIndex;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BezierHandleOverlay {
   private static final float LINE_ALPHA = 0.98F;
   private static final float BOX_ALPHA = 0.98F;

   private BezierHandleOverlay() {
   }

   public static void renderWrenchRangeCurves(Minecraft mc, PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation) {
      if (mc.player != null && !mc.options.hideGui && mc.gameMode.getPlayerMode() != GameType.SPECTATOR) {
         if (AllItems.WRENCH.isIn(mc.player.getMainHandItem()) || AllItems.WRENCH.isIn(mc.player.getOffhandItem())) {
            if (BezierHandleEditMode.isActive()) {
               BlockPos editAnchor = BezierHandleEditMode.getActiveAnchor();
               if (editAnchor != null) {
                  Level level = mc.level;
                  if (level != null) {
                     List<BezierHandleOverlay.PendingArmBillboard> pendingArmBillboards = new ArrayList<>();
                     List<BezierHandleOverlay.PendingTangentBillboard> pendingTangentBillboards = new ArrayList<>();
                     List<BezierHandleOverlay.PendingLiftBillboard> pendingLiftBillboards = new ArrayList<>();
                     List<BezierHandleOverlay.PendingTiltBillboard> pendingTiltBillboards = new ArrayList<>();
                     List<BezierHandleOverlay.PendingOpenEndpointBillboard> pendingOpenEndpointBillboards = new ArrayList<>();
                     Set<String> seen = new HashSet<>();
                     boolean draggingTangent = BezierHandleDragManager.isDraggingTangentHandle();
                     boolean draggingTilt = BezierHandleDragManager.isDraggingTiltHandle();
                     boolean draggingLift = BezierHandleDragManager.isDraggingLiftHandle();
                     boolean draggingOpenEndpoint = BezierHandleDragManager.isDraggingOpenEndpointHandle();
                     boolean isolateHandles = draggingTangent || draggingTilt || draggingLift || draggingOpenEndpoint;
                     boolean drawTangentHandles = !isolateHandles || draggingTangent;
                     boolean drawTiltHandle = !isolateHandles || draggingTilt;
                     boolean drawLiftHandle = !isolateHandles || draggingLift;
                     boolean drawOpenEndpointHandle = !isolateHandles || draggingOpenEndpoint;

                     for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
                        if (anchor.getLevel() == level && !anchor.isRemoved() && editAnchor.equals(anchor.getBlockPos())) {
                           BlockPos ap = anchor.getBlockPos();

                           for (BezierConnection bcRaw : anchor.getAnchorPeerCurvesView().values()) {
                              if (bcRaw != null) {
                                 BezierConnection bc = bcRaw.isPrimary() ? bcRaw : bcRaw.secondary();
                                 if (bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id) && seen.add(edgeKey(bc))) {
                                    String ek = edgeKey(bc);
                                    BezierConnection axisSource = BezierHandleDragManager.previewCurveOrWorld(ek, bc);
                                    BezierConnection rootSource = draggingTangent ? bc : axisSource;
                                    if (drawTangentHandles) {
                                       renderCurve(mc, pendingArmBillboards, pendingTangentBillboards, rootSource, axisSource, ek, ap);
                                    }
                                 }
                              }
                           }

                           if (drawTiltHandle && CoasterBezierHandleEdit.anchorEligibleForTiltEdit(level, ap)) {
                              renderJointTiltHandle(anchor, ap, pendingTiltBillboards);
                           }

                           if (drawLiftHandle && CoasterBezierHandleEdit.anchorEligibleForLiftEdit(level, ap)) {
                              renderAnchorLiftHandle(mc, ms, cameraPos, cameraRotation, anchor, ap, pendingLiftBillboards);
                           }

                           if (drawOpenEndpointHandle && anchor.legCount() == 1) {
                              renderOpenEndpointHandle(mc, level, ap, pendingArmBillboards, pendingOpenEndpointBillboards);
                           } else if (draggingTangent && anchor.legCount() == 1 && BezierHandleDragManager.isDraggingTangentOnAnchor(ap)) {
                              renderOpenEndExtensionJunctionCapDuringTangentDrag(mc, level, ap, pendingArmBillboards);
                           }
                        }
                     }

                     flushArmBillboardsByTexture(ms, buffer, cameraPos, cameraRotation, pendingArmBillboards);
                     flushTangentBillboardsByTexture(ms, buffer, cameraPos, cameraRotation, pendingTangentBillboards);
                     flushOpenEndpointBillboardsByTexture(ms, buffer, cameraPos, cameraRotation, pendingOpenEndpointBillboards);
                     flushLiftBillboardsByTexture(ms, buffer, cameraPos, cameraRotation, pendingLiftBillboards);
                     flushTiltHandlesByTexture(ms, buffer, cameraPos, cameraRotation, pendingTiltBillboards);
                  }
               }
            }
         }
      }
   }

   private static void flushArmBillboardsByTexture(
      PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation, List<BezierHandleOverlay.PendingArmBillboard> pending
   ) {
      for (ResourceLocation tex : new ResourceLocation[]{BezierHandleArmTextures.DEFAULT, BezierHandleArmTextures.HOVER, BezierHandleArmTextures.DRAGGING}) {
         VertexConsumer bb = null;
         Set<Long> junctionCapped = new HashSet<>();

         for (BezierHandleOverlay.PendingArmBillboard p : pending) {
            if (p.texture().equals(tex)) {
               if (bb == null) {
                  bb = buffer.getBuffer(BezierHandleOverlayRenderTypes.tangentHandleBillboard(tex));
               }

               billboardArmStrip(ms, bb, cameraPos, cameraRotation, p.tailWorld(), p.tipWorld(), p.r(), p.g(), p.b(), p.a());
               if (p.capTail()) {
                  long capKey = junctionCapKey(p.tailWorld());
                  if (junctionCapped.add(capKey)) {
                     billboardArmJunctionCap(ms, bb, cameraPos, cameraRotation, p.tailWorld(), p.r(), p.g(), p.b(), p.a());
                  }
               }
            }
         }
      }
   }

   private static long junctionCapKey(Vec3 junctionWorld) {
      return BlockPos.containing(junctionWorld).asLong();
   }

   private static void flushOpenEndpointBillboardsByTexture(
      PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation, List<BezierHandleOverlay.PendingOpenEndpointBillboard> pending
   ) {
      for (ResourceLocation tex : new ResourceLocation[]{EndpointHandleTextures.DEFAULT, EndpointHandleTextures.HOVER, EndpointHandleTextures.DRAGGING}) {
         VertexConsumer bb = null;

         for (BezierHandleOverlay.PendingOpenEndpointBillboard p : pending) {
            if (p.texture().equals(tex)) {
               if (bb == null) {
                  bb = buffer.getBuffer(BezierHandleOverlayRenderTypes.tangentHandleBillboard(tex));
               }

               billboardTangentQuad(ms, bb, cameraPos, cameraRotation, p.tipWorld(), p.r(), p.g(), p.b(), p.a());
            }
         }
      }
   }

   private static void flushTangentBillboardsByTexture(
      PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation, List<BezierHandleOverlay.PendingTangentBillboard> pending
   ) {
      for (ResourceLocation tex : new ResourceLocation[]{
         BezierHandleTangentTextures.DEFAULT, BezierHandleTangentTextures.HOVER, BezierHandleTangentTextures.DRAGGING
      }) {
         VertexConsumer bb = null;

         for (BezierHandleOverlay.PendingTangentBillboard p : pending) {
            if (p.texture().equals(tex)) {
               if (bb == null) {
                  bb = buffer.getBuffer(BezierHandleOverlayRenderTypes.tangentHandleBillboard(tex));
               }

               billboardTangentQuad(ms, bb, cameraPos, cameraRotation, p.tipWorld(), p.r(), p.g(), p.b(), p.a());
            }
         }
      }
   }

   private static void flushLiftBillboardsByTexture(
      PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation, List<BezierHandleOverlay.PendingLiftBillboard> pending
   ) {
      for (ResourceLocation tex : new ResourceLocation[]{BezierHandleLiftTextures.DEFAULT, BezierHandleLiftTextures.HOVER, BezierHandleLiftTextures.DRAGGING}) {
         VertexConsumer bb = null;

         for (BezierHandleOverlay.PendingLiftBillboard p : pending) {
            if (p.texture().equals(tex)) {
               if (bb == null) {
                  bb = buffer.getBuffer(BezierHandleOverlayRenderTypes.tangentHandleBillboard(tex));
               }

               billboardLiftQuad(ms, bb, cameraPos, cameraRotation, p.tipWorld(), p.normalWorld(), p.r(), p.g(), p.b(), p.a());
            }
         }
      }
   }

   private static void flushTiltHandlesByTexture(
      PoseStack ms, MultiBufferSource buffer, Vec3 cameraPos, Matrix4f cameraRotation, List<BezierHandleOverlay.PendingTiltBillboard> pending
   ) {
      for (ResourceLocation tex : new ResourceLocation[]{BezierHandleTiltTextures.DEFAULT, BezierHandleTiltTextures.HOVER, BezierHandleTiltTextures.DRAGGING}) {
         VertexConsumer bb = null;

         for (BezierHandleOverlay.PendingTiltBillboard p : pending) {
            if (p.texture().equals(tex)) {
               if (bb == null) {
                  bb = buffer.getBuffer(BezierHandleOverlayRenderTypes.tangentHandleBillboard(tex));
               }

               tiltPlanarHandleQuad(ms, bb, cameraPos, cameraRotation, p.tipWorld(), p.normalWorld(), p.tangentIntoWorld(), p.r(), p.g(), p.b(), p.a());
            }
         }
      }
   }

   private static void renderOpenEndpointHandle(
      Minecraft mc,
      Level level,
      BlockPos ap,
      List<BezierHandleOverlay.PendingArmBillboard> pendingArmBillboards,
      List<BezierHandleOverlay.PendingOpenEndpointBillboard> pendingOpenEndpointBillboards
   ) {
      if (level.getBlockEntity(ap) instanceof CoasterAnchorpointBlockEntity be) {
         BezierConnection bc = CoasterOpenEndExtension.primaryCoasterCurveAt(level, ap);
         if (bc != null) {
            String ek = edgeKey(bc);
            BezierConnection draw = BezierHandleDragManager.previewCurveOrWorld(ek, bc);
            float pt = AnimationTickHolder.getPartialTicks(level);
            Vec3 junction = BezierHandleDragManager.openEndpointJunctionRenderWorld(level, ap, draw, pt);
            Vec3 handle = BezierHandleDragManager.openEndpointHandleRenderWorld(level, ap, draw, pt);
            if (junction != null && handle != null) {
               BezierHandleOverlay.TangentBillboardStyle armSt = openEndpointArmStyle(ap);
               pendingArmBillboards.add(
                  new BezierHandleOverlay.PendingArmBillboard(junction, handle, armSt.texture(), armSt.r(), armSt.g(), armSt.b(), armSt.a(), false)
               );
               BezierHandleOverlay.OpenEndpointBillboardStyle st = openEndpointBillboardStyle(ap);
               pendingOpenEndpointBillboards.add(new BezierHandleOverlay.PendingOpenEndpointBillboard(handle, st.texture(), st.r(), st.g(), st.b(), st.a()));
            }
         }
      }
   }

   private static void renderOpenEndExtensionJunctionCapDuringTangentDrag(
      Minecraft mc, Level level, BlockPos ap, List<BezierHandleOverlay.PendingArmBillboard> pendingArmBillboards
   ) {
      BezierConnection bc = CoasterOpenEndExtension.primaryCoasterCurveAt(level, ap);
      if (bc != null) {
         String ek = edgeKey(bc);
         Vec3 tailPlot = BezierHandleDragManager.curveStartAtAnchor(bc, ap);
         if (tailPlot != null) {
            float pt = AnimationTickHolder.getPartialTicks(level);
            Vec3 tailWorld = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, ap, tailPlot, pt);
            BezierHandleOverlay.TangentBillboardStyle armSt = tangentArmStyle(mc, ek, ap);
            pendingArmBillboards.add(
               new BezierHandleOverlay.PendingArmBillboard(tailWorld, tailWorld, armSt.texture(), armSt.r(), armSt.g(), armSt.b(), armSt.a(), true)
            );
         }
      }
   }

   private static BezierHandleOverlay.TangentBillboardStyle openEndpointArmStyle(BlockPos anchorBlock) {
      float a = 0.98F;
      if (BezierHandleDragManager.isDraggingOpenEndpointHandle()) {
         return new BezierHandleOverlay.TangentBillboardStyle(BezierHandleArmTextures.DRAGGING, 1.0F, 1.0F, 1.0F, a);
      } else {
         return BezierHandleDragManager.isHoveredOpenEndpointAnchor(anchorBlock)
            ? new BezierHandleOverlay.TangentBillboardStyle(BezierHandleArmTextures.HOVER, 1.0F, 1.0F, 1.0F, a)
            : new BezierHandleOverlay.TangentBillboardStyle(BezierHandleArmTextures.DEFAULT, 1.0F, 1.0F, 1.0F, a);
      }
   }

   private static BezierHandleOverlay.OpenEndpointBillboardStyle openEndpointBillboardStyle(BlockPos anchorBlock) {
      float a = 0.98F;
      if (BezierHandleDragManager.isDraggingOpenEndpointHandle()) {
         return new BezierHandleOverlay.OpenEndpointBillboardStyle(EndpointHandleTextures.DRAGGING, 1.0F, 1.0F, 1.0F, a);
      } else {
         return BezierHandleDragManager.isHoveredOpenEndpointAnchor(anchorBlock)
            ? new BezierHandleOverlay.OpenEndpointBillboardStyle(EndpointHandleTextures.HOVER, 1.0F, 1.0F, 1.0F, a)
            : new BezierHandleOverlay.OpenEndpointBillboardStyle(EndpointHandleTextures.DEFAULT, 1.0F, 1.0F, 1.0F, a);
      }
   }

   private static void renderJointTiltHandle(
      CoasterAnchorpointBlockEntity anchor, BlockPos ap, List<BezierHandleOverlay.PendingTiltBillboard> pendingTiltBillboards
   ) {
      Level level = anchor.getLevel();
      if (level != null) {
         BezierHandleDragManager.AnchorFrameOverride override = BezierHandleDragManager.livePreviewedAnchorFrame(ap);
         Vec3 center;
         Vec3 face;
         Vec3 tangentInto;
         if (override != null) {
            center = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, ap, override.center());
            face = CoasterAnchorClientSpace.toRenderDirection(level, ap, override.faceNormal());
            tangentInto = CoasterAnchorClientSpace.toRenderDirection(level, ap, override.tangentInto());
         } else {
            List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, anchor);
            if (incident.isEmpty()) {
               return;
            }

            Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), ap);
            if (t == null) {
               return;
            }

            center = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, ap, CoasterAnchorpointBlockEntity.worldCenter(level, ap));
            face = CoasterAnchorClientSpace.toRenderDirection(level, ap, CoasterTrackPlacement.anchorFaceNormalForAxis(level, ap, t));
            tangentInto = CoasterAnchorClientSpace.toRenderDirection(level, ap, t);
         }

         if (!(face.lengthSqr() < 1.0E-12) && !(tangentInto.lengthSqr() < 1.0E-12)) {
            face = face.normalize();
            tangentInto = tangentInto.normalize();
            Vec3 tip = center.add(face.scale(0.75));
            BezierHandleOverlay.TiltBillboardStyle st = tiltBillboardStyle(ap);
            pendingTiltBillboards.add(new BezierHandleOverlay.PendingTiltBillboard(tip, face, tangentInto, st.texture(), st.r(), st.g(), st.b(), st.a()));
         }
      }
   }

   private static BezierHandleOverlay.TiltBillboardStyle tiltBillboardStyle(BlockPos anchorBlock) {
      float a = 0.98F;
      if (BezierHandleDragManager.previewInvalidDrag() && BezierHandleDragManager.isDraggingTiltOnAnchor(anchorBlock)) {
         return new BezierHandleOverlay.TiltBillboardStyle(BezierHandleTiltTextures.DEFAULT, 1.0F, 0.22F, 0.22F, a);
      } else if (BezierHandleDragManager.isDraggingTiltOnAnchor(anchorBlock)) {
         return new BezierHandleOverlay.TiltBillboardStyle(BezierHandleTiltTextures.DRAGGING, 1.0F, 1.0F, 1.0F, a);
      } else {
         return BezierHandleDragManager.isHoveredTiltAnchor(anchorBlock)
            ? new BezierHandleOverlay.TiltBillboardStyle(BezierHandleTiltTextures.HOVER, 1.0F, 1.0F, 1.0F, a)
            : new BezierHandleOverlay.TiltBillboardStyle(BezierHandleTiltTextures.DEFAULT, 1.0F, 1.0F, 1.0F, a);
      }
   }

   private static void renderAnchorLiftHandle(
      Minecraft mc,
      PoseStack ms,
      Vec3 cameraPos,
      Matrix4f cameraRotation,
      CoasterAnchorpointBlockEntity anchor,
      BlockPos ap,
      List<BezierHandleOverlay.PendingLiftBillboard> pendingLiftBillboards
   ) {
      Level level = mc.level;
      if (level != null) {
         BezierHandleDragManager.AnchorFrameOverride override = BezierHandleDragManager.livePreviewedAnchorFrame(ap);
         Vec3 center;
         Vec3 face;
         if (override != null) {
            center = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, ap, override.center());
            face = CoasterAnchorClientSpace.toRenderDirection(level, ap, override.faceNormal());
         } else {
            List<BezierConnection> incident = CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, anchor);
            if (incident.isEmpty()) {
               return;
            }

            Vec3 t = CoasterBezierHandleEdit.tangentIntoFromAnchor(incident.get(0), ap);
            if (t == null) {
               return;
            }

            center = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, ap, CoasterAnchorpointBlockEntity.worldCenter(level, ap));
            face = CoasterAnchorClientSpace.toRenderDirection(level, ap, CoasterTrackPlacement.anchorFaceNormalForAxis(level, ap, t));
         }

         if (!(face.lengthSqr() < 1.0E-12)) {
            face = face.normalize();
            Vec3 tip = center.add(face.scale(1.4F));
            BezierHandleOverlay.LiftBillboardStyle st = liftBillboardStyle(ap);
            pendingLiftBillboards.add(new BezierHandleOverlay.PendingLiftBillboard(tip, face, st.texture(), st.r(), st.g(), st.b(), st.a()));
         }
      }
   }

   private static BezierHandleOverlay.LiftBillboardStyle liftBillboardStyle(BlockPos anchorBlock) {
      float a = 0.98F;
      if (BezierHandleDragManager.previewInvalidDrag() && BezierHandleDragManager.isDraggingLiftOnAnchor(anchorBlock)) {
         return new BezierHandleOverlay.LiftBillboardStyle(BezierHandleLiftTextures.DEFAULT, 1.0F, 0.22F, 0.22F, a);
      } else if (BezierHandleDragManager.isDraggingLiftOnAnchor(anchorBlock)) {
         return new BezierHandleOverlay.LiftBillboardStyle(BezierHandleLiftTextures.DRAGGING, 1.0F, 1.0F, 1.0F, a);
      } else {
         return BezierHandleDragManager.isHoveredLiftAnchor(anchorBlock)
            ? new BezierHandleOverlay.LiftBillboardStyle(BezierHandleLiftTextures.HOVER, 1.0F, 1.0F, 1.0F, a)
            : new BezierHandleOverlay.LiftBillboardStyle(BezierHandleLiftTextures.DEFAULT, 1.0F, 1.0F, 1.0F, a);
      }
   }

   private static String edgeKey(BezierConnection bc) {
      long a = ((BlockPos)bc.bePositions.getFirst()).asLong();
      long b = ((BlockPos)bc.bePositions.getSecond()).asLong();
      return a <= b ? a + ":" + b : b + ":" + a;
   }

   private static void renderCurve(
      Minecraft mc,
      List<BezierHandleOverlay.PendingArmBillboard> pendingArmBillboards,
      List<BezierHandleOverlay.PendingTangentBillboard> pendingTangentBillboards,
      BezierConnection rootBc,
      BezierConnection axisBc,
      String edgeKey,
      BlockPos transformAnchor
   ) {
      Level level = mc.level;
      Vec3 e1 = (Vec3)rootBc.starts.getFirst();
      Vec3 e2 = (Vec3)rootBc.starts.getSecond();
      Vec3 f1 = handleControlPointWorld(rootBc, axisBc, 0);
      Vec3 f2 = handleControlPointWorld(rootBc, axisBc, 1);
      e1 = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, transformAnchor, e1);
      e2 = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, transformAnchor, e2);
      f1 = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, transformAnchor, f1);
      f2 = CoasterAnchorClientSpace.pathCenterlineRenderWorld(level, transformAnchor, f2);
      BlockPos tailAnchor = (BlockPos)rootBc.bePositions.getFirst();
      BezierHandleOverlay.TangentBillboardStyle armSt = tangentArmStyle(mc, edgeKey, tailAnchor);
      pendingArmBillboards.add(
         new BezierHandleOverlay.PendingArmBillboard(e1, f1, armSt.texture(), armSt.r(), armSt.g(), armSt.b(), armSt.a(), !tailAnchor.equals(transformAnchor))
      );
      BezierHandleOverlay.TangentBillboardStyle st = tangentBillboardStyle(mc, edgeKey, tailAnchor);
      pendingTangentBillboards.add(new BezierHandleOverlay.PendingTangentBillboard(f1, st.texture(), st.r(), st.g(), st.b(), st.a()));
      tailAnchor = (BlockPos)rootBc.bePositions.getSecond();
      armSt = tangentArmStyle(mc, edgeKey, tailAnchor);
      pendingArmBillboards.add(
         new BezierHandleOverlay.PendingArmBillboard(e2, f2, armSt.texture(), armSt.r(), armSt.g(), armSt.b(), armSt.a(), !tailAnchor.equals(transformAnchor))
      );
      st = tangentBillboardStyle(mc, edgeKey, tailAnchor);
      pendingTangentBillboards.add(new BezierHandleOverlay.PendingTangentBillboard(f2, st.texture(), st.r(), st.g(), st.b(), st.a()));
   }

   private static Vec3 handleControlPointWorld(BezierConnection rootBc, BezierConnection axisBc, int endpointIndex) {
      Vec3 start = endpointIndex == 0 ? (Vec3)rootBc.starts.getFirst() : (Vec3)rootBc.starts.getSecond();
      Vec3 axis = endpointIndex == 0 ? (Vec3)axisBc.axes.getFirst() : (Vec3)axisBc.axes.getSecond();
      double len = CoasterPeerCurveHandleLengths.getLengthAtEndpoint(axisBc, endpointIndex);
      return axis.lengthSqr() < 1.0E-12 ? start : start.add(axis.normalize().scale(len));
   }

   private static BezierHandleOverlay.TangentBillboardStyle tangentArmStyle(Minecraft mc, String edgeKey, BlockPos endpointBlock) {
      float a = 0.98F;
      if (BezierHandleDragManager.invalidTint(edgeKey)) {
         return new BezierHandleOverlay.TangentBillboardStyle(BezierHandleArmTextures.DEFAULT, 1.0F, 0.22F, 0.22F, a);
      } else if (BezierHandleDragManager.isDraggingTangentHandle()) {
         return new BezierHandleOverlay.TangentBillboardStyle(BezierHandleArmTextures.DRAGGING, 1.0F, 1.0F, 1.0F, a);
      } else {
         return BezierHandleDragManager.isHoveredEndpoint(mc, edgeKey, endpointBlock)
            ? new BezierHandleOverlay.TangentBillboardStyle(BezierHandleArmTextures.HOVER, 1.0F, 1.0F, 1.0F, a)
            : new BezierHandleOverlay.TangentBillboardStyle(BezierHandleArmTextures.DEFAULT, 1.0F, 1.0F, 1.0F, a);
      }
   }

   private static BezierHandleOverlay.TangentBillboardStyle tangentBillboardStyle(Minecraft mc, String edgeKey, BlockPos endpointBlock) {
      float a = 0.98F;
      if (BezierHandleDragManager.invalidTint(edgeKey)) {
         return new BezierHandleOverlay.TangentBillboardStyle(BezierHandleTangentTextures.DEFAULT, 1.0F, 0.22F, 0.22F, a);
      } else if (BezierHandleDragManager.isDraggingTangentHandle()) {
         return new BezierHandleOverlay.TangentBillboardStyle(BezierHandleTangentTextures.DRAGGING, 1.0F, 1.0F, 1.0F, a);
      } else {
         return BezierHandleDragManager.isHoveredEndpoint(mc, edgeKey, endpointBlock)
            ? new BezierHandleOverlay.TangentBillboardStyle(BezierHandleTangentTextures.HOVER, 1.0F, 1.0F, 1.0F, a)
            : new BezierHandleOverlay.TangentBillboardStyle(BezierHandleTangentTextures.DEFAULT, 1.0F, 1.0F, 1.0F, a);
      }
   }

   private static void billboardArmJunctionCap(
      PoseStack ms, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 junctionWorld, float r, float g, float bl, float alpha
   ) {
      float h = 0.087749995F;
      billboardEyeSpaceQuad(ms, vb, cameraPos, cameraRotation, junctionWorld, h, r, g, bl, alpha);
   }

   private static void billboardTangentQuad(
      PoseStack ms, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 tipWorld, float r, float g, float bl, float alpha
   ) {
      float h = 0.27F;
      billboardEyeSpaceQuad(ms, vb, cameraPos, cameraRotation, tipWorld, h, r, g, bl, alpha);
   }

   private static void billboardEyeSpaceQuad(
      PoseStack ms, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 centerWorld, float eyeHalfExtent, float r, float g, float bl, float alpha
   ) {
      Vec3 c = worldToEye(cameraRotation, cameraPos, centerWorld);
      float cx = (float)c.x;
      float cy = (float)c.y;
      float cz = (float)c.z;
      Matrix4f mat = ms.last().pose();
      vb.addVertex(mat, cx - eyeHalfExtent, cy - eyeHalfExtent, cz).setUv(0.0F, 1.0F).setColor(r, g, bl, alpha);
      vb.addVertex(mat, cx + eyeHalfExtent, cy - eyeHalfExtent, cz).setUv(1.0F, 1.0F).setColor(r, g, bl, alpha);
      vb.addVertex(mat, cx + eyeHalfExtent, cy + eyeHalfExtent, cz).setUv(1.0F, 0.0F).setColor(r, g, bl, alpha);
      vb.addVertex(mat, cx - eyeHalfExtent, cy + eyeHalfExtent, cz).setUv(0.0F, 0.0F).setColor(r, g, bl, alpha);
   }

   private static void billboardArmStrip(
      PoseStack ms, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 tailWorld, Vec3 tipWorld, float r, float g, float bl, float alpha
   ) {
      Vec3 delta = tipWorld.subtract(tailWorld);
      double len = delta.length();
      if (!(len < 1.0E-6)) {
         Vec3 axis = delta.scale(1.0 / len);
         Vec3 mid = tailWorld.add(tipWorld).scale(0.5);
         Vec3 view = cameraPos.subtract(mid);
         double vlen = Math.sqrt(view.lengthSqr());
         if (!(vlen < 1.0E-9)) {
            view = view.scale(1.0 / vlen);
            Vec3 right = view.cross(axis);
            if (right.lengthSqr() < 1.0E-10) {
               Vec3 ref = Math.abs(axis.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
               right = ref.cross(axis);
               if (right.lengthSqr() < 1.0E-12) {
                  return;
               }
            }

            right = right.normalize();
            float hw = 0.029249998F;
            Matrix4f mat = ms.last().pose();
            Vec3 tailL = tailWorld.subtract(right.scale((double)hw));
            Vec3 tailR = tailWorld.add(right.scale((double)hw));
            Vec3 tipL = tipWorld.subtract(right.scale((double)hw));
            Vec3 tipR = tipWorld.add(right.scale((double)hw));
            emitArmVertex(mat, vb, cameraPos, cameraRotation, tailL, 0.0F, 1.0F, r, g, bl, alpha);
            emitArmVertex(mat, vb, cameraPos, cameraRotation, tailR, 1.0F, 1.0F, r, g, bl, alpha);
            emitArmVertex(mat, vb, cameraPos, cameraRotation, tipR, 1.0F, 0.0F, r, g, bl, alpha);
            emitArmVertex(mat, vb, cameraPos, cameraRotation, tipL, 0.0F, 0.0F, r, g, bl, alpha);
         }
      }
   }

   private static void emitArmVertex(
      Matrix4f mat, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 world, float u, float v, float r, float g, float bl, float alpha
   ) {
      Vec3 eye = worldToEye(cameraRotation, cameraPos, world);
      vb.addVertex(mat, (float)eye.x, (float)eye.y, (float)eye.z).setUv(u, v).setColor(r, g, bl, alpha);
   }

   private static void billboardLiftQuad(
      PoseStack ms, VertexConsumer vb, Vec3 cameraPos, Matrix4f cameraRotation, Vec3 tipWorld, Vec3 upWorld, float r, float g, float bl, float alpha
   ) {
      Vec3 view = cameraPos.subtract(tipWorld);
      double vlen = Math.sqrt(view.lengthSqr());
      if (!(vlen < 1.0E-9)) {
         view = view.scale(1.0 / vlen);
         if (!(upWorld.lengthSqr() < 1.0E-12)) {
            Vec3 up = upWorld.normalize();
            Vec3 right = view.cross(up);
            if (right.lengthSqr() < 1.0E-10) {
               Vec3 ref = Math.abs(up.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
               right = ref.cross(up);
               if (right.lengthSqr() < 1.0E-12) {
                  return;
               }
            }

            right = right.normalize();
            Vec3 forward = up.cross(right);
            double fl = Math.sqrt(forward.lengthSqr());
            if (!(fl < 1.0E-9)) {
               forward = forward.scale(1.0 / fl);
               if (forward.dot(view) < 0.0) {
                  right = right.scale(-1.0);
               }

               float scale = 0.9F;
               float hw = 0.35F * scale;
               float hh = 0.35F * scale;
               Matrix4f mat = ms.last().pose();
               emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, -hw, -hh, 0.0F, 1.0F, r, g, bl, alpha);
               emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, hw, -hh, 1.0F, 1.0F, r, g, bl, alpha);
               emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, hw, hh, 1.0F, 0.0F, r, g, bl, alpha);
               emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, -hw, hh, 0.0F, 0.0F, r, g, bl, alpha);
            }
         }
      }
   }

   private static void tiltPlanarHandleQuad(
      PoseStack ms,
      VertexConsumer vb,
      Vec3 cameraPos,
      Matrix4f cameraRotation,
      Vec3 tipWorld,
      Vec3 normalWorld,
      Vec3 tangentIntoWorld,
      float r,
      float g,
      float bl,
      float alpha
   ) {
      if (!(normalWorld.lengthSqr() < 1.0E-12)) {
         Vec3 up = normalWorld.normalize();
         if (!(tangentIntoWorld.lengthSqr() < 1.0E-12)) {
            Vec3 t = tangentIntoWorld.normalize();
            Vec3 right = t.cross(up);
            if (right.lengthSqr() < 1.0E-10) {
               Vec3 ref = Math.abs(up.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
               right = ref.cross(up);
               if (right.lengthSqr() < 1.0E-12) {
                  return;
               }
            }

            right = right.normalize();
            float scale = 0.9F;
            float hw = 0.45F * scale;
            float hh = 0.45F * scale;
            Matrix4f mat = ms.last().pose();
            emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, -hw, -hh, 0.0F, 1.0F, r, g, bl, alpha);
            emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, hw, -hh, 1.0F, 1.0F, r, g, bl, alpha);
            emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, hw, hh, 1.0F, 0.0F, r, g, bl, alpha);
            emitLiftBillboardVertex(mat, vb, cameraPos, cameraRotation, tipWorld, right, up, -hw, hh, 0.0F, 0.0F, r, g, bl, alpha);
         }
      }
   }

   private static void emitLiftBillboardVertex(
      Matrix4f mat,
      VertexConsumer vb,
      Vec3 cameraPos,
      Matrix4f cameraRotation,
      Vec3 tip,
      Vec3 right,
      Vec3 up,
      float dx,
      float dy,
      float u,
      float v,
      float r,
      float g,
      float bl,
      float alpha
   ) {
      Vec3 world = tip.add(right.scale((double)dx)).add(up.scale((double)dy));
      Vec3 eye = worldToEye(cameraRotation, cameraPos, world);
      vb.addVertex(mat, (float)eye.x, (float)eye.y, (float)eye.z).setUv(u, v).setColor(r, g, bl, alpha);
   }

   private static Vec3 worldToEye(Matrix4f cameraRotation, Vec3 cameraPos, Vec3 world) {
      Vector3f v = new Vector3f((float)(world.x - cameraPos.x), (float)(world.y - cameraPos.y), (float)(world.z - cameraPos.z));
      cameraRotation.transformPosition(v);
      return new Vec3((double)v.x, (double)v.y, (double)v.z);
   }

   private static record LiftBillboardStyle(ResourceLocation texture, float r, float g, float b, float a) {
   }

   private static record OpenEndpointBillboardStyle(ResourceLocation texture, float r, float g, float b, float a) {
   }

   private static record PendingArmBillboard(Vec3 tailWorld, Vec3 tipWorld, ResourceLocation texture, float r, float g, float b, float a, boolean capTail) {
   }

   private static record PendingLiftBillboard(Vec3 tipWorld, Vec3 normalWorld, ResourceLocation texture, float r, float g, float b, float a) {
   }

   private static record PendingOpenEndpointBillboard(Vec3 tipWorld, ResourceLocation texture, float r, float g, float b, float a) {
   }

   private static record PendingTangentBillboard(Vec3 tipWorld, ResourceLocation texture, float r, float g, float b, float a) {
   }

   private static record PendingTiltBillboard(
      Vec3 tipWorld, Vec3 normalWorld, Vec3 tangentIntoWorld, ResourceLocation texture, float r, float g, float b, float a
   ) {
   }

   private static record TangentBillboardStyle(ResourceLocation texture, float r, float g, float b, float a) {
   }

   private static record TiltBillboardStyle(ResourceLocation texture, float r, float g, float b, float a) {
   }
}
