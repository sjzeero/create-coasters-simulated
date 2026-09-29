package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

final class CoasterTrackStaffRailStabilizer {
   private static final EnumSet<ConstraintJointAxis> STAFF_ENGAGED_PULL_LOCKED_AXES = EnumSet.of(
      ConstraintJointAxis.LINEAR_Z, ConstraintJointAxis.ANGULAR_X, ConstraintJointAxis.ANGULAR_Y, ConstraintJointAxis.ANGULAR_Z
   );
   private static final double TRACK_UP_FLOOR_MOTOR_STIFFNESS = 140.0;
   private static final double TRACK_UP_FLOOR_MOTOR_DAMPING = 14.0;
   private static final Map<ServerSubLevel, CoasterTrackStaffRailStabilizer.StabilizerState> STATES = Collections.synchronizedMap(new WeakHashMap<>());

   private CoasterTrackStaffRailStabilizer() {
   }

   static void updateDuringStaffRailSnap(
      ServerSubLevel bearingSub,
      ServerLevel level,
      SubLevelPhysicsSystem physics,
      CoasterPathEdge railEdge,
      Vector3d snapTargetWorld,
      CoasterPathTrackFrame.TrackBasis basis,
      Vec3 bearingPlotLocal,
      Vec3 bearingWorld
   ) {
      ServerSubLevel trackSub = CoasterTrackGuideConstraint.resolveTrackHostingSublevel(level, railEdge);
      Quaterniond qWorldJoint = RailConstraintGeometry.quaternionJointWorldFromBasis(basis);
      Vector3d bodyAnchor = new Vector3d(bearingPlotLocal.x, bearingPlotLocal.y, bearingPlotLocal.z);
      synchronized (STATES) {
         CoasterTrackStaffRailStabilizer.StabilizerState st = STATES.computeIfAbsent(bearingSub, k -> new CoasterTrackStaffRailStabilizer.StabilizerState());
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
                  new Vector3d(snapTargetWorld), bodyAnchor, qWorldJoint, RailConstraintGeometry.Q_JOINT_IN_BODY, STAFF_ENGAGED_PULL_LOCKED_AXES
               );
            } else {
               Vec3 snapMc = trackSub.logicalPose().transformPositionInverse(new Vec3(snapTargetWorld.x, snapTargetWorld.y, snapTargetWorld.z));
               Quaterniond qOnTrack = RailConstraintGeometry.quaternionJointLocalOnTrack(trackSub, qWorldJoint);
               cfg = new GenericConstraintConfiguration(
                  new Vector3d(snapMc.x, snapMc.y, snapMc.z), bodyAnchor, qOnTrack, RailConstraintGeometry.Q_JOINT_IN_BODY, STAFF_ENGAGED_PULL_LOCKED_AXES
               );
            }

            GenericConstraintHandle h = st.handle;
            if (h != null && h.isValid()) {
               if (trackSub == null) {
                  h.setFrame1(snapTargetWorld, qWorldJoint);
               } else {
                  Vec3 snapMc = trackSub.logicalPose().transformPositionInverse(new Vec3(snapTargetWorld.x, snapTargetWorld.y, snapTargetWorld.z));
                  Quaterniond qOnTrack = RailConstraintGeometry.quaternionJointLocalOnTrack(trackSub, qWorldJoint);
                  h.setFrame1(new Vector3d(snapMc.x, snapMc.y, snapMc.z), qOnTrack);
               }

               h.setFrame2(bodyAnchor, RailConstraintGeometry.Q_JOINT_IN_BODY);
            } else {
               h = trackSub == null
                  ? (GenericConstraintHandle)physics.getPipeline().addConstraint(null, bearingSub, cfg)
                  : (GenericConstraintHandle)physics.getPipeline().addConstraint(trackSub, bearingSub, cfg);
               st.handle = h;
            }

            applyTrackUpFloorMotor(h, snapTargetWorld, qWorldJoint, bearingWorld);
         }
      }
   }

   private static void applyTrackUpFloorMotor(@Nullable GenericConstraintHandle handle, Vector3d snapTargetWorld, Quaterniond qJointWorld, Vec3 bearingWorld) {
      if (handle != null && handle.isValid()) {
         Vec3 snap = new Vec3(snapTargetWorld.x, snapTargetWorld.y, snapTargetWorld.z);
         double jointY = RailConstraintGeometry.jointLinearYRelativeToRailAnchor(snap, bearingWorld, qJointWorld);
         double maxJointY = 0.0;
         if (jointY > 0.0) {
            handle.setMotor(ConstraintJointAxis.LINEAR_Y, 0.0, 140.0, 14.0, false, 0.0);
         } else {
            handle.setMotor(ConstraintJointAxis.LINEAR_Y, 0.0, 0.0, 0.0, false, 0.0);
         }
      }
   }

   static void remove(ServerSubLevel bearingSub) {
      synchronized (STATES) {
         CoasterTrackStaffRailStabilizer.StabilizerState st = STATES.remove(bearingSub);
         if (st != null && st.handle != null && st.handle.isValid()) {
            st.handle.remove();
         }
      }
   }

   private static final class StabilizerState {
      @Nullable
      GenericConstraintHandle handle;
      @Nullable
      ServerSubLevel trackPartner;
   }
}
