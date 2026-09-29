package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

final class CoasterTrackGuideConstraint {
   private static final Set<ConstraintJointAxis> RAIL_GUIDE_LOCKED_AXES = EnumSet.of(
      ConstraintJointAxis.LINEAR_Y, ConstraintJointAxis.LINEAR_Z, ConstraintJointAxis.ANGULAR_X, ConstraintJointAxis.ANGULAR_Y, ConstraintJointAxis.ANGULAR_Z
   );
   private static final Map<ServerSubLevel, CoasterTrackGuideConstraint.GuideState> STATES = Collections.synchronizedMap(new WeakHashMap<>());

   private CoasterTrackGuideConstraint() {
   }

   @Nullable
   static ServerSubLevel resolveTrackHostingSublevel(ServerLevel level, CoasterPathEdge edge) {
      SubLevel a = Sable.HELPER.getContaining(level, edge.from());
      if (a instanceof ServerSubLevel) {
         return (ServerSubLevel)a;
      } else {
         SubLevel b = Sable.HELPER.getContaining(level, edge.to());
         return b instanceof ServerSubLevel ? (ServerSubLevel)b : null;
      }
   }

   static void updateAfterSnap(
      ServerSubLevel bearingSub,
      ServerLevel level,
      SubLevelPhysicsSystem physics,
      CoasterPathEdge railEdge,
      Vector3d snapTargetWorld,
      CoasterPathTrackFrame.TrackBasis basis,
      Vec3 bearingPlotLocal
   ) {
      ServerSubLevel trackSub = resolveTrackHostingSublevel(level, railEdge);
      Quaterniond qWorldJoint = RailConstraintGeometry.quaternionJointWorldFromBasis(basis);
      Vector3d bodyAnchor = new Vector3d(bearingPlotLocal.x, bearingPlotLocal.y, bearingPlotLocal.z);
      synchronized (STATES) {
         CoasterTrackGuideConstraint.GuideState st = STATES.computeIfAbsent(bearingSub, k -> new CoasterTrackGuideConstraint.GuideState());
         if (!Objects.equals(st.trackPartner, trackSub)) {
            if (st.handle != null && st.handle.isValid()) {
               st.handle.remove();
            }

            st.handle = null;
            st.trackPartner = trackSub;
         }

         if (trackSub != null && trackSub == bearingSub) {
            if (st.handle != null && st.handle.isValid()) {
               st.handle.remove();
            }

            st.handle = null;
            st.trackPartner = null;
         } else {
            GenericConstraintConfiguration cfg;
            if (trackSub == null) {
               cfg = new GenericConstraintConfiguration(
                  new Vector3d(snapTargetWorld), bodyAnchor, qWorldJoint, RailConstraintGeometry.Q_JOINT_IN_BODY, RAIL_GUIDE_LOCKED_AXES
               );
            } else {
               Vec3 snapMc = trackSub.logicalPose().transformPositionInverse(new Vec3(snapTargetWorld.x, snapTargetWorld.y, snapTargetWorld.z));
               Vector3d snapOnTrack = new Vector3d(snapMc.x, snapMc.y, snapMc.z);
               Quaterniond qOnTrack = RailConstraintGeometry.quaternionJointLocalOnTrack(trackSub, qWorldJoint);
               cfg = new GenericConstraintConfiguration(snapOnTrack, bodyAnchor, qOnTrack, RailConstraintGeometry.Q_JOINT_IN_BODY, RAIL_GUIDE_LOCKED_AXES);
            }

            GenericConstraintHandle h = st.handle;
            if (h != null && h.isValid()) {
               if (trackSub == null) {
                  h.setFrame1(snapTargetWorld, qWorldJoint);
               } else {
                  Vec3 snapMc = trackSub.logicalPose().transformPositionInverse(new Vec3(snapTargetWorld.x, snapTargetWorld.y, snapTargetWorld.z));
                  h.setFrame1(new Vector3d(snapMc.x, snapMc.y, snapMc.z), RailConstraintGeometry.quaternionJointLocalOnTrack(trackSub, qWorldJoint));
               }

               h.setFrame2(bodyAnchor, RailConstraintGeometry.Q_JOINT_IN_BODY);
            } else {
               if (trackSub == null) {
                  h = (GenericConstraintHandle)physics.getPipeline().addConstraint(null, bearingSub, cfg);
               } else {
                  h = (GenericConstraintHandle)physics.getPipeline().addConstraint(trackSub, bearingSub, cfg);
               }

               st.handle = h;
            }
         }
      }
   }

   static void removeRigidGuideLocked(ServerSubLevel sub) {
      synchronized (STATES) {
         CoasterTrackGuideConstraint.GuideState st = STATES.remove(sub);
         if (st != null && st.handle != null && st.handle.isValid()) {
            st.handle.remove();
         }
      }
   }

   static void remove(ServerSubLevel sub) {
      removeRigidGuideLocked(sub);
      CoasterTrackStaffRailStabilizer.remove(sub);
   }

   private static final class GuideState {
      @Nullable
      GenericConstraintHandle handle;
      @Nullable
      ServerSubLevel trackPartner;
   }
}
