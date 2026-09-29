package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterAnchorCoplanarPlane {
   private CoasterAnchorCoplanarPlane() {
   }

   @Nullable
   public static Vec3 planeNormalFromAnchors(Level level, BlockPos a, BlockPos b, BlockPos c) {
      if (!a.equals(b) && !a.equals(c) && !b.equals(c)) {
         Vec3 ca = CoasterAnchorpointBlockEntity.worldCenterBase(level, a);
         Vec3 cb = CoasterAnchorpointBlockEntity.worldCenterBase(level, b);
         Vec3 cc = CoasterAnchorpointBlockEntity.worldCenterBase(level, c);
         return planeNormalFromWorldCenters(ca, cb, cc);
      } else {
         return null;
      }
   }

   @Nullable
   public static Vec3 planeNormalFromWorldCenters(Vec3 ca, Vec3 cb, Vec3 cc) {
      Vec3 ab = cb.subtract(ca);
      Vec3 ac = cc.subtract(ca);
      double abLenSq = ab.lengthSqr();
      double acLenSq = ac.lengthSqr();
      if (!(abLenSq < 1.0E-12) && !(acLenSq < 1.0E-12)) {
         Vec3 n = ab.cross(ac);
         double crossLenSq = n.lengthSqr();
         double minCross = 1.0E-6 * abLenSq * acLenSq;
         return crossLenSq < minCross ? null : n.normalize();
      } else {
         return null;
      }
   }

   @Nullable
   public static Vec3 placementTangentPlaneNormal(Level level, BlockPos endpoint, BlockPos spanPeer) {
      if (!(level.getBlockEntity(endpoint) instanceof CoasterAnchorpointBlockEntity be)) {
         return null;
      } else {
         BlockPos var7 = null;

         for (BlockPos peer : be.getAnchorPeerCurvesView().keySet()) {
            if (!peer.equals(spanPeer)) {
               var7 = peer.immutable();
               break;
            }
         }

         return var7 == null ? null : planeNormalFromAnchors(level, var7, endpoint, spanPeer);
      }
   }

   @Nullable
   public static Vec3 hubJoinPlaneNormal(Level level, BlockPos hub, BlockPos existingPeer, BlockPos newPeer) {
      return planeNormalFromAnchors(level, existingPeer, hub, newPeer);
   }

   @Nullable
   public static Vec3 openEndApproachPlaneNormal(Level level, BlockPos openPos, BlockPos hubPos) {
      return placementTangentPlaneNormal(level, hubPos, openPos);
   }

   public static Vec3 projectAxisOntoPlane(Vec3 axis, @Nullable Vec3 planeUnitNormal, Vec3 fallbackIfDegenerate) {
      if (planeUnitNormal != null && !(axis.lengthSqr() < 1.0E-12)) {
         Vec3 projected = axis.subtract(planeUnitNormal.scale(axis.dot(planeUnitNormal)));
         if (projected.lengthSqr() < 1.0E-10) {
            return fallbackIfDegenerate.lengthSqr() < 1.0E-12 ? axis.normalize() : fallbackIfDegenerate.normalize();
         } else {
            return projected.normalize();
         }
      } else {
         return axis.lengthSqr() < 1.0E-12 ? fallbackIfDegenerate : axis.normalize();
      }
   }

   public static Vec3 projectAxisOntoPlane(Vec3 axis, @Nullable Vec3 planeUnitNormal) {
      return projectAxisOntoPlane(axis, planeUnitNormal, axis);
   }
}
