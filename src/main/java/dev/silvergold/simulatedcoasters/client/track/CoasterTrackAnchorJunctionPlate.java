package dev.silvergold.simulatedcoasters.client.track;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackRenderer;
import com.simibubi.create.content.trains.track.BezierConnection.Segment;
import com.simibubi.create.foundation.render.SpecialModels;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

public final class CoasterTrackAnchorJunctionPlate {
   private CoasterTrackAnchorJunctionPlate() {
   }

   public static boolean shouldRenderGirderAt(Level level, BlockPos anchorPos, BezierConnection curve) {
      if (level != null && curve != null) {
         BezierConnection p = curve.isPrimary() ? curve : curve.secondary();
         if (!curveMeetsAnchor(p, anchorPos)) {
            return false;
         } else {
            List<BezierConnection> incident = incidentCoasterCurvesAt(level, anchorPos);
            if (incident.isEmpty()) {
               return false;
            } else if (incident.size() == 1) {
               return CoasterBezierHandleEdit.edgeKey(p).equals(CoasterBezierHandleEdit.edgeKey(incident.get(0)));
            } else {
               return incident.size() == 2 ? isWinningCurveAmongIncident(p, incident) : false;
            }
         }
      } else {
         return false;
      }
   }

   public static boolean shouldRenderJunctionTieAt(Level level, BlockPos anchorPos, BezierConnection curve) {
      return shouldRenderGirderAt(level, anchorPos, curve);
   }

   public static boolean shouldRenderPlateAt(Level level, BlockPos anchorPos, BezierConnection curve) {
      if (level != null && curve != null) {
         if (level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity anchor) {
            if (anchor.hasChainLiftShaft()) {
               return false;
            } else {
               BezierConnection p = curve.isPrimary() ? curve : curve.secondary();
               if (!curveMeetsAnchor(p, anchorPos)) {
                  return false;
               } else {
                  List<BezierConnection> incident = incidentCoasterCurvesAt(level, anchorPos);
                  return incident.size() != 1 ? false : CoasterBezierHandleEdit.edgeKey(p).equals(CoasterBezierHandleEdit.edgeKey(incident.get(0)));
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static long mixEndpointJunctionTopologySignature(Level level, BezierConnection bc, long hash) {
      if (level != null && bc != null) {
         BezierConnection p = bc.isPrimary() ? bc : bc.secondary();

         for (BlockPos ep : List.of((BlockPos)p.bePositions.getFirst(), (BlockPos)p.bePositions.getSecond())) {
            List<BezierConnection> incident = incidentCoasterCurvesAt(level, ep);
            hash ^= ep.asLong();
            hash *= 1099511628211L;
            hash ^= (long)incident.size();
            hash *= 1099511628211L;
            if (level.getBlockEntity(ep) instanceof CoasterAnchorpointBlockEntity anchor) {
               hash ^= anchor.hasChainLiftShaft() ? 1L : 0L;
               hash *= 1099511628211L;
            }
         }

         return hash;
      } else {
         return hash;
      }
   }

   private static List<BezierConnection> incidentCoasterCurvesAt(Level level, BlockPos anchorPos) {
      return level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity anchor
         ? CoasterBezierHandleEdit.incidentPrimaryCoasterCurves(level, anchor)
         : List.of();
   }

   private static boolean curveMeetsAnchor(BezierConnection primary, BlockPos anchorPos) {
      return ((BlockPos)primary.bePositions.getFirst()).equals(anchorPos) || ((BlockPos)primary.bePositions.getSecond()).equals(anchorPos);
   }

   private static boolean isWinningCurveAmongIncident(BezierConnection primary, List<BezierConnection> incident) {
      String winning = null;

      for (BezierConnection c : incident) {
         String k = CoasterBezierHandleEdit.edgeKey(c);
         if (winning == null || k.compareTo(winning) < 0) {
            winning = k;
         }
      }

      return winning != null && CoasterBezierHandleEdit.edgeKey(primary).equals(winning);
   }

   @Nullable
   public static CoasterTrackAnchorJunctionPlate.EndpointSpine spineAtEndpoint(BezierConnection bc, BlockPos anchorPos, @Nullable Level level) {
      BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
      List<Vec3> pos = new ArrayList<>();
      List<Vec3> lat = new ArrayList<>();
      Vec3 prevLat = null;

      for (Segment seg : p) {
         pos.add(seg.position);
         float segT = p.getSegmentT(seg.index);
         Vec3 currLat = CoasterBezierRailFrames.lateralAt(p, segT, seg.derivative, level);
         if (prevLat != null && currLat.dot(prevLat) < 0.0) {
            currLat = currLat.scale(-1.0);
         }

         lat.add(currLat);
         prevLat = currLat;
      }

      int n = pos.size();
      if (n < 2) {
         return null;
      } else {
         boolean atFirst = ((BlockPos)p.bePositions.getFirst()).equals(anchorPos);
         Vec3 chord = atFirst ? pos.get(1).subtract(pos.get(0)) : pos.get(n - 1).subtract(pos.get(n - 2));
         return chord.lengthSqr() < 1.0E-12
            ? null
            : new CoasterTrackAnchorJunctionPlate.EndpointSpine(atFirst ? pos.getFirst() : pos.get(n - 1), atFirst ? lat.getFirst() : lat.getLast(), chord);
      }
   }

   public static Vec3 junctionPlateCenterInHostRelativeSpace(CoasterTrackAnchorJunctionPlate.EndpointSpine frame) {
      float modelAnchorY = -0.75390625F;
      Vec3 angles = TrackRenderer.getModelAngles(frame.lateral(), frame.chordIntoTrack());
      PoseStack p = new PoseStack();
      ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(p).translate(frame.spine()))
                  .rotateY((float)angles.y))
               .rotateX((float)angles.x))
            .rotateZ((float)angles.z))
         .translate(-0.5F, modelAnchorY, -0.5F);
      Vector4f h = new Vector4f(0.5F, 0.75F, 0.5F, 1.0F);
      p.last().pose().transform(h);
      return new Vec3((double)h.x, (double)h.y, (double)h.z).add(0.0, -0.1875, 0.0);
   }

   public static Vec3 junctionPlateCenterForVisualRoot(CoasterTrackAnchorJunctionPlate.EndpointSpine frame) {
      return junctionPlateCenterInHostRelativeSpace(frame).subtract(0.0, -0.1875, 0.0);
   }

   public static PoseStack platePoseBelowOrigin(CoasterTrackAnchorJunctionPlate.EndpointSpine frame) {
      return connectionPoseBelowOrigin(frame.spine(), frame.lateral(), frame.chordIntoTrack());
   }

   public static PoseStack connectionPoseBelowOrigin(Vec3 spine, Vec3 lateral, Vec3 inward) {
      float modelAnchorY = -0.75390625F;
      Vec3 angles = TrackRenderer.getModelAngles(lateral, inward);
      PoseStack pose = new PoseStack();
      ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(pose).translate(spine)).rotateY((float)angles.y))
               .rotateX((float)angles.x))
            .rotateZ((float)angles.z))
         .translate(-0.5F, modelAnchorY, -0.5F);
      return pose;
   }

   public static PoseStack spineTiePoseBelowOrigin(Vec3 spine, Vec3 lateral, Vec3 chordIntoTrack, float anchorExtraY, float localY) {
      float modelAnchorY = 0.24609375F + anchorExtraY;
      Vec3 angles = TrackRenderer.getModelAngles(lateral, chordIntoTrack);
      PoseStack pose = new PoseStack();
      ((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)((PoseTransformStack)TransformStack.of(pose).translate(spine)).rotateY((float)angles.y))
               .rotateX((float)angles.x))
            .rotateZ((float)angles.z))
         .translate(-0.5F, modelAnchorY, -0.5F);
      TransformStack.of(pose).translate(0.5F, localY, 0.5F);
      TransformStack.of(pose).translate(-0.5F, 0.1315F, -0.25F);
      return pose;
   }

   public static PoseStack tiePoseBelowOrigin(CoasterTrackAnchorJunctionPlate.EndpointSpine frame, float anchorExtraY, float localY) {
      return spineTiePoseBelowOrigin(frame.spine(), frame.lateral(), frame.chordIntoTrack(), anchorExtraY, localY);
   }

   public static PoseStack midspanTiePoseBelowOrigin(Vec3 spine, Vec3 lateral, Vec3 chordIntoTrack) {
      return spineTiePoseBelowOrigin(spine, lateral, chordIntoTrack, 0.0F, -0.1315F);
   }

   public static PoseStack openEndTiePoseBelowOrigin(CoasterTrackAnchorJunctionPlate.EndpointSpine frame) {
      return tiePoseBelowOrigin(frame, -1.0F, 0.8685F);
   }

   public static PoseStack junctionTiePoseBelowOrigin(CoasterTrackAnchorJunctionPlate.EndpointSpine frame) {
      return tiePoseBelowOrigin(frame, 0.0F, -0.1315F);
   }

   @Nullable
   public static TransformedInstance createFlywheelPlate(
      InstancerProvider instancerProvider, Level level, BezierConnection bc, BlockPos anchorEndpoint, Vec3 visualOrigin
   ) {
      if (!shouldRenderPlateAt(level, anchorEndpoint, bc)) {
         return null;
      } else {
         CoasterTrackAnchorJunctionPlate.EndpointSpine frame = spineAtEndpoint(bc, anchorEndpoint, level);
         if (frame == null) {
            return null;
         } else {
            TransformedInstance[] slot = new TransformedInstance[1];
            instancerProvider.instancer(InstanceTypes.TRANSFORMED, SpecialModels.flatChunk(CoasterTrackAnchorJunctionModel.TRACK_ANCHORPOINT_CONNECTION))
               .createInstances(slot);
            TransformedInstance inst = slot[0];
            PoseStack root = new PoseStack();
            TransformStack.of(root).translate(visualOrigin.add(0.0, -0.1875, 0.0));
            PoseStack local = platePoseBelowOrigin(frame);
            inst.setTransform(root).mul(local.last()).setChanged();
            return inst;
         }
      }
   }

   public static void renderBerPlate(Level level, BezierConnection bc, BlockPos anchorEndpoint, PoseStack ms, VertexConsumer vb, BlockPos curveHostPos) {
      if (shouldRenderPlateAt(level, anchorEndpoint, bc)) {
         CoasterTrackAnchorJunctionPlate.EndpointSpine frame = spineAtEndpoint(bc, anchorEndpoint, level);
         if (frame != null) {
            BlockState air = Blocks.AIR.defaultBlockState();
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(frame.spine()).offset(curveHostPos));
            PoseStack local = platePoseBelowOrigin(frame);
            ((SuperByteBuffer)((SuperByteBuffer)CachedBuffers.partial(CoasterTrackAnchorJunctionModel.TRACK_ANCHORPOINT_CONNECTION, air)
                     .mulPose(local.last().pose()))
                  .mulNormal(local.last().normal()))
               .light(light)
               .renderInto(ms, vb);
         }
      }
   }

   public static record EndpointSpine(Vec3 spine, Vec3 lateral, Vec3 chordIntoTrack) {
   }
}
