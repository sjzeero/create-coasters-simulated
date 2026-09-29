package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import net.minecraft.world.phys.Vec3;

public final class CoasterTrackCurvePickContext {
   private static boolean inPickCurves;
   private static TrackBlockEntity trackEntity;
   private static BezierConnection pickCurvesBezier;
   private static TrackBlockEntity pickCurvesTrackEntity;
   private static double pickCurvesTForNormal;

   private CoasterTrackCurvePickContext() {
   }

   public static void enterPickCurves() {
      inPickCurves = true;
      trackEntity = null;
      clearPickCurvesBezierSample();
   }

   public static void exitPickCurves() {
      inPickCurves = false;
      trackEntity = null;
      clearPickCurvesBezierSample();
   }

   public static boolean inPickCurves() {
      return inPickCurves;
   }

   public static void setTrackEntity(TrackBlockEntity be) {
      trackEntity = be;
   }

   public static TrackBlockEntity getTrackEntity() {
      return trackEntity;
   }

   public static void stashPickCurvesBezierSample(BezierConnection bc, TrackBlockEntity be, double tForNormal) {
      pickCurvesBezier = bc;
      pickCurvesTrackEntity = be;
      pickCurvesTForNormal = tForNormal;
   }

   private static void clearPickCurvesBezierSample() {
      pickCurvesBezier = null;
      pickCurvesTrackEntity = null;
   }

   public static Vec3 curvePickSpineMidToBakedRailMid(Vec3 spineMid) {
      BezierConnection bc = pickCurvesBezier;
      double t = pickCurvesTForNormal;
      clearPickCurvesBezierSample();
      if (bc == null) {
         return spineMid;
      } else {
         return !bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)
            ? spineMid
            : spineMid.add(CoasterBezierRailFrames.curveInteractionLiftOffset(bc, (float)t, BezierBankBlendLevel.tryGetClientLevel()));
      }
   }
}
