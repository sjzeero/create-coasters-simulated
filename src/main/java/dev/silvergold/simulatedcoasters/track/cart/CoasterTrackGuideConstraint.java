/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.physics.PhysicsPipelineBody
 *  dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis
 *  dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration
 *  dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle
 *  dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.track.cart.CoasterTrackStaffRailStabilizer;
import dev.silvergold.simulatedcoasters.track.cart.RailConstraintGeometry;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

final class CoasterTrackGuideConstraint {
    private static final Set<ConstraintJointAxis> RAIL_GUIDE_LOCKED_AXES = EnumSet.of(ConstraintJointAxis.LINEAR_Y, ConstraintJointAxis.LINEAR_Z, ConstraintJointAxis.ANGULAR_X, ConstraintJointAxis.ANGULAR_Y, ConstraintJointAxis.ANGULAR_Z);
    private static final Map<ServerSubLevel, GuideState> STATES = Collections.synchronizedMap(new WeakHashMap());

    private CoasterTrackGuideConstraint() {
    }

    @Nullable
    static ServerSubLevel resolveTrackHostingSublevel(ServerLevel level, CoasterPathEdge edge) {
        SubLevel a = Sable.HELPER.getContaining((Level)level, (Vec3i)edge.from());
        if (a instanceof ServerSubLevel) {
            ServerSubLevel sa = (ServerSubLevel)a;
            return sa;
        }
        SubLevel b = Sable.HELPER.getContaining((Level)level, (Vec3i)edge.to());
        if (b instanceof ServerSubLevel) {
            ServerSubLevel sb = (ServerSubLevel)b;
            return sb;
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void updateAfterSnap(ServerSubLevel bearingSub, ServerLevel level, SubLevelPhysicsSystem physics, CoasterPathEdge railEdge, Vector3d snapTargetWorld, CoasterPathTrackFrame.TrackBasis basis, Vec3 bearingPlotLocal) {
        ServerSubLevel trackSub = CoasterTrackGuideConstraint.resolveTrackHostingSublevel(level, railEdge);
        Quaterniond qWorldJoint = RailConstraintGeometry.quaternionJointWorldFromBasis(basis);
        Vector3d bodyAnchor = new Vector3d(bearingPlotLocal.x, bearingPlotLocal.y, bearingPlotLocal.z);
        Map<ServerSubLevel, GuideState> map = STATES;
        synchronized (map) {
            GenericConstraintConfiguration cfg;
            GuideState st = STATES.computeIfAbsent(bearingSub, k -> new GuideState());
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
                return;
            }
            if (trackSub == null) {
                cfg = new GenericConstraintConfiguration((Vector3dc)new Vector3d((Vector3dc)snapTargetWorld), (Vector3dc)bodyAnchor, (Quaterniondc)qWorldJoint, (Quaterniondc)RailConstraintGeometry.Q_JOINT_IN_BODY, RAIL_GUIDE_LOCKED_AXES);
            } else {
                Vec3 snapMc = trackSub.logicalPose().transformPositionInverse(new Vec3(snapTargetWorld.x, snapTargetWorld.y, snapTargetWorld.z));
                Vector3d snapOnTrack = new Vector3d(snapMc.x, snapMc.y, snapMc.z);
                Quaterniond qOnTrack = RailConstraintGeometry.quaternionJointLocalOnTrack(trackSub, qWorldJoint);
                cfg = new GenericConstraintConfiguration((Vector3dc)snapOnTrack, (Vector3dc)bodyAnchor, (Quaterniondc)qOnTrack, (Quaterniondc)RailConstraintGeometry.Q_JOINT_IN_BODY, RAIL_GUIDE_LOCKED_AXES);
            }
            GenericConstraintHandle h = st.handle;
            if (h == null || !h.isValid()) {
                h = trackSub == null ? (GenericConstraintHandle)physics.getPipeline().addConstraint(null, (PhysicsPipelineBody)bearingSub, (PhysicsConstraintConfiguration)cfg) : (GenericConstraintHandle)physics.getPipeline().addConstraint((PhysicsPipelineBody)trackSub, (PhysicsPipelineBody)bearingSub, (PhysicsConstraintConfiguration)cfg);
                st.handle = h;
            } else {
                if (trackSub == null) {
                    h.setFrame1((Vector3dc)snapTargetWorld, (Quaterniondc)qWorldJoint);
                } else {
                    Vec3 snapMc = trackSub.logicalPose().transformPositionInverse(new Vec3(snapTargetWorld.x, snapTargetWorld.y, snapTargetWorld.z));
                    h.setFrame1((Vector3dc)new Vector3d(snapMc.x, snapMc.y, snapMc.z), (Quaterniondc)RailConstraintGeometry.quaternionJointLocalOnTrack(trackSub, qWorldJoint));
                }
                h.setFrame2((Vector3dc)bodyAnchor, (Quaterniondc)RailConstraintGeometry.Q_JOINT_IN_BODY);
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void removeRigidGuideLocked(ServerSubLevel sub) {
        Map<ServerSubLevel, GuideState> map = STATES;
        synchronized (map) {
            GuideState st = STATES.remove(sub);
            if (st != null && st.handle != null && st.handle.isValid()) {
                st.handle.remove();
            }
        }
    }

    static void remove(ServerSubLevel sub) {
        CoasterTrackGuideConstraint.removeRigidGuideLocked(sub);
        CoasterTrackStaffRailStabilizer.remove(sub);
    }

    private static final class GuideState {
        @Nullable
        GenericConstraintHandle handle;
        @Nullable
        ServerSubLevel trackPartner;

        private GuideState() {
        }
    }
}

