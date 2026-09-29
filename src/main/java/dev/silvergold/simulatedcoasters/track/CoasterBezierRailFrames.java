package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackRenderer;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterBezierRailFrames {
   private CoasterBezierRailFrames() {
   }

   public static boolean isCoaster(BezierConnection bc) {
      return bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id);
   }

   public static Vec3 unitTangentAt(BezierConnection bc, float t) {
      Vec3 end1 = (Vec3)bc.starts.getFirst();
      Vec3 end2 = (Vec3)bc.starts.getSecond();
      Vec3 finish1 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 0);
      Vec3 finish2 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 1);
      Vec3 derivative = VecHelper.bezierDerivative(end1, end2, finish1, finish2, t);
      return derivative.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : derivative.normalize();
   }

   public static Vec3 faceNormalAt(BezierConnection bc, float t, @Nullable Level level) {
      return faceNormalAt(bc, t, unitTangentAt(bc, t), level);
   }

   public static Vec3 faceNormalAt(BezierConnection bc, float t, Vec3 tangentUnit, @Nullable Level level) {
      return faceNormalAt(bc, t, tangentUnit, BezierBankBlend.smoothJointBankParameter(t, bc, level), level);
   }

   public static Vec3 faceNormalAt(BezierConnection bc, float t, Vec3 tangentUnit, float bankBlendAlpha, @Nullable Level level) {
      Vec3 tangentN = tangentUnit.lengthSqr() < 1.0E-12 ? unitTangentAt(bc, t) : tangentUnit.normalize();
      Vec3 transported = CoasterBezierParallelFrame.faceAt(bc, t, bankBlendAlpha);
      if (transported != null) {
         Vec3 perp = transported.subtract(tangentN.scale(tangentN.dot(transported)));
         return perp.lengthSqr() < 1.0E-10 ? transported : perp.normalize();
      } else {
         return projectedFaceNormalAt(bc, tangentN, bankBlendAlpha, level);
      }
   }

   private static Vec3 projectedFaceNormalAt(BezierConnection bc, Vec3 tangentN, float tt, @Nullable Level level) {
      Vec3 fn1 = (Vec3)bc.normals.getFirst();
      Vec3 fn2 = (Vec3)bc.normals.getSecond();
      Vec3 blended = VecHelper.slerp(tt, fn1, fn2);
      Vec3 face = BezierRailSpread.orthogonalFaceForTangent(blended, tangentN, fn1, fn2);
      if (level != null && Math.abs(face.dot(tangentN)) > 0.99) {
         Vec3 live1 = anchorFaceNormalAtEndpoint(level, bc, true);
         Vec3 live2 = anchorFaceNormalAtEndpoint(level, bc, false);
         if (live1 != null && live2 != null) {
            blended = VecHelper.slerp(tt, live1, live2);
            face = BezierRailSpread.orthogonalFaceForTangent(blended, tangentN, live1, live2);
         }
      }

      return face;
   }

   @Nullable
   private static Vec3 anchorFaceNormalAtEndpoint(Level level, BezierConnection bc, boolean firstEnd) {
      BlockPos pos = firstEnd ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
      if (!(level.getBlockState(pos).getBlock() instanceof CoasterAnchorpointBlock)) {
         return null;
      } else {
         Vec3 axis = firstEnd ? (Vec3)bc.axes.getFirst() : (Vec3)bc.axes.getSecond();
         return axis.lengthSqr() < 1.0E-12 ? null : CoasterTrackPlacement.anchorFaceNormalForAxis(level, pos, axis);
      }
   }

   public static Vec3 lateralFromBlendedFace(BezierConnection bc, Vec3 bankSmoothedFace, Vec3 derivative) {
      Vec3 fn1 = (Vec3)bc.normals.getFirst();
      Vec3 fn2 = (Vec3)bc.normals.getSecond();
      Vec3 tangent = derivative.lengthSqr() < 1.0E-12 ? unitTangentAt(bc, 0.0F) : derivative.normalize();
      Vec3 face = BezierRailSpread.orthogonalFaceForTangent(bankSmoothedFace, tangent, fn1, fn2);
      Vec3 rail = BezierRailSpread.faceCrossTangentOrFallback(face, tangent, fn1, fn2);
      return alignRailSignFromReference(rail, fn1, fn2, tangent, face);
   }

   public static Vec3 lateralFromBlendedFaceAt(BezierConnection bc, Vec3 derivative, float t) {
      Vec3 tangent = derivative.lengthSqr() < 1.0E-12 ? unitTangentAt(bc, t) : derivative.normalize();
      Vec3 face = faceNormalAt(bc, t, tangent, BezierBankBlend.smoothJointBankParameter(t, bc), null);
      return lateralFromFace(bc, face, tangent);
   }

   public static Vec3 lateralAt(BezierConnection bc, float t, @Nullable Level level) {
      return lateralAt(bc, t, unitTangentAt(bc, t), level);
   }

   public static Vec3 lateralAt(BezierConnection bc, float t, Vec3 unitTangent, @Nullable Level level) {
      Vec3 tan = unitTangent.lengthSqr() < 1.0E-12 ? unitTangentAt(bc, t) : unitTangent.normalize();
      Vec3 face = faceNormalAt(bc, t, tan, level);
      return lateralFromFace(bc, face, tan);
   }

   private static Vec3 lateralFromFace(BezierConnection bc, Vec3 face, Vec3 tangentUnit) {
      Vec3 fn1 = (Vec3)bc.normals.getFirst();
      Vec3 fn2 = (Vec3)bc.normals.getSecond();
      Vec3 rail = BezierRailSpread.faceCrossTangentOrFallback(face, tangentUnit, fn1, fn2);
      if (rail.lengthSqr() < 1.0E-14) {
         return rail;
      } else {
         return isCoaster(bc) ? rail.normalize() : alignRailSignFromReference(rail, fn1, fn2, tangentUnit, face).normalize();
      }
   }

   public static Vec3 faceUpAt(BezierConnection bc, float t, @Nullable Level level) {
      Vec3 tangent = unitTangentAt(bc, t);
      Vec3 lateral = lateralAt(bc, t, tangent, level);
      Vec3 up = tangent.cross(lateral);
      return up.lengthSqr() < 1.0E-12 ? faceNormalAt(bc, t, tangent, level) : up.normalize();
   }

   public static Vec3 curveInteractionLiftOffset(BezierConnection bc, float t, @Nullable Level level) {
      return faceUpAt(bc, t, level).scale((double)CoasterTrackGauge.curveInteractionLift()).add(0.0, -0.125, 0.0);
   }

   public static Vec3 modelAnglesForSegment(BezierConnection bc, float t, Vec3 chordDiff, @Nullable Level level) {
      Vec3 tan = chordDiff.lengthSqr() < 1.0E-12 ? unitTangentAt(bc, t) : chordDiff.normalize();
      return TrackRenderer.getModelAngles(lateralAt(bc, t, tan, level), chordDiff);
   }

   private static Vec3 alignRailSignFromReference(Vec3 rail, Vec3 fn1, Vec3 fn2, Vec3 tangentUnit, Vec3 blendedFace) {
      if (rail.lengthSqr() < 1.0E-14) {
         return rail;
      } else {
         Vec3 ref1 = fn1.cross(tangentUnit);
         Vec3 ref2 = fn2.cross(tangentUnit);
         boolean preferFirst = blendedFace.lengthSqr() < 1.0E-14
            || Math.abs(blendedFace.normalize().dot(fn1.lengthSqr() < 1.0E-14 ? fn1 : fn1.normalize()))
               >= Math.abs(blendedFace.normalize().dot(fn2.lengthSqr() < 1.0E-14 ? fn2 : fn2.normalize()));
         Vec3 ref = preferFirst ? ref1 : ref2;
         if (ref.lengthSqr() < 1.0E-10) {
            ref = preferFirst ? ref2 : ref1;
         }

         if (ref.lengthSqr() < 1.0E-10) {
            return rail;
         } else {
            return rail.dot(ref) < 0.0 ? rail.scale(-1.0) : rail;
         }
      }
   }
}
