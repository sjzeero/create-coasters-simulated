/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterAnchorCoplanarPlane {
    private CoasterAnchorCoplanarPlane() {
    }

    @Nullable
    public static Vec3 planeNormalFromAnchors(Level level, BlockPos a, BlockPos b, BlockPos c) {
        if (a.equals((Object)b) || a.equals((Object)c) || b.equals((Object)c)) {
            return null;
        }
        Vec3 ca = CoasterAnchorpointBlockEntity.worldCenterBase(level, a);
        Vec3 cb = CoasterAnchorpointBlockEntity.worldCenterBase(level, b);
        Vec3 cc = CoasterAnchorpointBlockEntity.worldCenterBase(level, c);
        return CoasterAnchorCoplanarPlane.planeNormalFromWorldCenters(ca, cb, cc);
    }

    @Nullable
    public static Vec3 planeNormalFromWorldCenters(Vec3 ca, Vec3 cb, Vec3 cc) {
        double minCross;
        Vec3 ab = cb.subtract(ca);
        Vec3 ac = cc.subtract(ca);
        double abLenSq = ab.lengthSqr();
        double acLenSq = ac.lengthSqr();
        if (abLenSq < 1.0E-12 || acLenSq < 1.0E-12) {
            return null;
        }
        Vec3 n = ab.cross(ac);
        double crossLenSq = n.lengthSqr();
        if (crossLenSq < (minCross = 1.0E-6 * abLenSq * acLenSq)) {
            return null;
        }
        return n.normalize();
    }

    @Nullable
    public static Vec3 placementTangentPlaneNormal(Level level, BlockPos endpoint, BlockPos spanPeer) {
        BlockEntity blockEntity = level.getBlockEntity(endpoint);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        BlockPos third = null;
        for (BlockPos peer : be.getAnchorPeerCurvesView().keySet()) {
            if (peer.equals((Object)spanPeer)) continue;
            third = peer.immutable();
            break;
        }
        if (third == null) {
            return null;
        }
        return CoasterAnchorCoplanarPlane.planeNormalFromAnchors(level, third, endpoint, spanPeer);
    }

    @Nullable
    public static Vec3 hubJoinPlaneNormal(Level level, BlockPos hub, BlockPos existingPeer, BlockPos newPeer) {
        return CoasterAnchorCoplanarPlane.planeNormalFromAnchors(level, existingPeer, hub, newPeer);
    }

    @Nullable
    public static Vec3 openEndApproachPlaneNormal(Level level, BlockPos openPos, BlockPos hubPos) {
        return CoasterAnchorCoplanarPlane.placementTangentPlaneNormal(level, hubPos, openPos);
    }

    public static Vec3 projectAxisOntoPlane(Vec3 axis, @Nullable Vec3 planeUnitNormal, Vec3 fallbackIfDegenerate) {
        if (planeUnitNormal == null || axis.lengthSqr() < 1.0E-12) {
            return axis.lengthSqr() < 1.0E-12 ? fallbackIfDegenerate : axis.normalize();
        }
        Vec3 projected = axis.subtract(planeUnitNormal.scale(axis.dot(planeUnitNormal)));
        if (projected.lengthSqr() < 1.0E-10) {
            return fallbackIfDegenerate.lengthSqr() < 1.0E-12 ? axis.normalize() : fallbackIfDegenerate.normalize();
        }
        return projected.normalize();
    }

    public static Vec3 projectAxisOntoPlane(Vec3 axis, @Nullable Vec3 planeUnitNormal) {
        return CoasterAnchorCoplanarPlane.projectAxisOntoPlane(axis, planeUnitNormal, axis);
    }
}

