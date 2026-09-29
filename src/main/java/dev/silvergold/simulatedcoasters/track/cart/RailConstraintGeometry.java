/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix3d
 *  org.joml.Matrix3dc
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

final class RailConstraintGeometry {
    static final Quaterniond Q_JOINT_IN_BODY;

    private RailConstraintGeometry() {
    }

    static Quaterniond quaternionJointWorldFromBasis(CoasterPathTrackFrame.TrackBasis b) {
        Vec3 f = b.forward();
        Vec3 u = b.up();
        Vec3 r = b.right();
        Matrix3d m = new Matrix3d();
        m.setColumn(0, f.x, f.y, f.z);
        m.setColumn(1, u.x, u.y, u.z);
        m.setColumn(2, r.x, r.y, r.z);
        return new Quaterniond().setFromNormalized((Matrix3dc)m);
    }

    static Quaterniond quaternionJointLocalOnTrack(ServerSubLevel trackSub, Quaterniond qJointWorld) {
        return new Quaterniond((Quaterniondc)trackSub.logicalPose().orientation()).conjugate().mul((Quaterniondc)new Quaterniond((Quaterniondc)qJointWorld)).normalize();
    }

    static double jointLinearYRelativeToRailAnchor(Vec3 railSnapCenter, Vec3 bearingWorld, Quaterniond qJointWorld) {
        Vector3d delta = new Vector3d(bearingWorld.x - railSnapCenter.x, bearingWorld.y - railSnapCenter.y, bearingWorld.z - railSnapCenter.z);
        new Quaterniond((Quaterniondc)qJointWorld).conjugate().transform(delta);
        return delta.y;
    }

    static Vec3 staffBearingSnapCenterWithMinTrackUp(Vec3 bearingWorld, Vec3 railSnapCenter, Vec3 trackUp, Quaterniond qJointWorld, double minJointY) {
        double jointY = RailConstraintGeometry.jointLinearYRelativeToRailAnchor(railSnapCenter, bearingWorld, qJointWorld);
        if (jointY <= minJointY) {
            return railSnapCenter;
        }
        Vec3 up = trackUp.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : trackUp.normalize();
        return bearingWorld.subtract(up.scale(jointY - minJointY));
    }

    static {
        Matrix3d m = new Matrix3d();
        m.setColumn(0, 0.0, 0.0, -1.0);
        m.setColumn(1, 0.0, 1.0, 0.0);
        m.setColumn(2, 1.0, 0.0, 0.0);
        Q_JOINT_IN_BODY = new Quaterniond().setFromNormalized((Matrix3dc)m);
    }
}

