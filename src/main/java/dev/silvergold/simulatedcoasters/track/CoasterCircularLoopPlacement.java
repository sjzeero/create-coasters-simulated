/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackMaterial
 *  net.createmod.catnip.data.Couple
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackMaterial;
import dev.silvergold.simulatedcoasters.track.CoasterAnchorBezierOptimizer;
import dev.silvergold.simulatedcoasters.track.CoasterAnchorCoplanarPlane;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterCircularLoopPlacement {
    private static final double RADIUS_RELATIVE_TOL = 0.06;
    private static final double MIN_CIRCLE_RADIUS_BLOCKS = 1.25;
    private static final double PARALLEL_PLANE_NORMAL_DOT = 0.85;
    private static final double RADIAL_FACING_DOT = 0.45;
    private static final double MIN_ARC_RADIANS = Math.toRadians(35.0);
    private static final double MAX_ARC_RADIANS = Math.toRadians(145.0);
    private static final double COPLANAR_HEIGHT_TOL_BLOCKS = 0.35;

    private CoasterCircularLoopPlacement() {
    }

    @Nullable
    public static CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult tryBuildCircularLoop(Level level, BlockPos anchorA, BlockPos anchorB, TrackMaterial material, boolean girder) {
        List<BlockPos> ordered;
        CircleFit fit;
        @Nullable List<BlockPos> cycle = CoasterCircularLoopPlacement.detectFourCycle(level, anchorA, anchorB);
        if (cycle == null) {
            return null;
        }
        if (!CoasterCircularLoopPlacement.allAnchorsHaveDefaultTilt(level, cycle)) {
            return null;
        }
        @Nullable CircleFit baseFit = CoasterCircularLoopPlacement.fitCircle(level, cycle, false);
        if (baseFit == null) {
            return null;
        }
        @Nullable OrientationLayout orientation = CoasterCircularLoopPlacement.classifyOrientation(level, cycle, baseFit);
        if (orientation == null) {
            return null;
        }
        if (orientation.radial()) {
            if (!CoasterCircularLoopPlacement.allAnchorsHaveDefaultLift(level, cycle)) {
                return null;
            }
            fit = CoasterCircularLoopPlacement.fitCircle(level, cycle, true);
            if (fit == null) {
                return null;
            }
        } else {
            fit = baseFit;
        }
        if ((ordered = CoasterCircularLoopPlacement.orderAroundCircle(level, cycle, fit, orientation.radial())).size() != 4) {
            return null;
        }
        ArrayList<BezierConnection> arcs = new ArrayList<BezierConnection>(4);
        for (int i = 0; i < 4; ++i) {
            BlockPos to;
            BlockPos from = ordered.get(i);
            BezierConnection arc = CoasterCircularLoopPlacement.buildCircularArc(level, from, to = ordered.get((i + 1) % 4), fit, orientation, material, girder);
            if (arc == null || !CoasterAnchorBezierOptimizer.isBuiltPlacementCurveValid(arc)) {
                return null;
            }
            arcs.add(arc);
        }
        BezierConnection primary = null;
        ArrayList<BezierConnection> neighbors = new ArrayList<BezierConnection>(3);
        for (BezierConnection arc : arcs) {
            boolean isClosing;
            BlockPos lo = (BlockPos)arc.bePositions.getFirst();
            BlockPos hi = (BlockPos)arc.bePositions.getSecond();
            boolean bl = isClosing = lo.equals((Object)anchorA) && hi.equals((Object)anchorB) || lo.equals((Object)anchorB) && hi.equals((Object)anchorA);
            if (isClosing) {
                primary = arc;
                continue;
            }
            neighbors.add(arc);
        }
        if (primary == null || neighbors.size() != 3) {
            return null;
        }
        return new CoasterAnchorBezierOptimizer.AnchorAnchorBuildResult(primary, List.of(), List.copyOf(neighbors));
    }

    private static boolean allAnchorsHaveDefaultTilt(Level level, List<BlockPos> cycle) {
        for (BlockPos pos : cycle) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
                return false;
            }
            CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
            if (!(Math.abs(be.getJointBankRadians() - 0.0f) > 1.0E-4f)) continue;
            return false;
        }
        return true;
    }

    private static boolean allAnchorsHaveDefaultLift(Level level, List<BlockPos> cycle) {
        float def = 0.5f;
        for (BlockPos pos : cycle) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
                return false;
            }
            CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
            if (!(Math.abs(be.getLiftBlocks() - def) > 0.001f)) continue;
            return false;
        }
        return true;
    }

    @Nullable
    public static List<BlockPos> detectFourCycle(Level level, BlockPos a, BlockPos b) {
        CoasterAnchorpointBlockEntity beP;
        BlockEntity blockEntity;
        BlockPos q;
        BlockPos p;
        block16: {
            block15: {
                CoasterAnchorpointBlockEntity beA;
                BlockEntity blockEntity2;
                block14: {
                    block13: {
                        if (a.equals((Object)b)) {
                            return null;
                        }
                        blockEntity2 = level.getBlockEntity(a);
                        if (!(blockEntity2 instanceof CoasterAnchorpointBlockEntity)) break block13;
                        beA = (CoasterAnchorpointBlockEntity)blockEntity2;
                        blockEntity2 = level.getBlockEntity(b);
                        if (blockEntity2 instanceof CoasterAnchorpointBlockEntity) break block14;
                    }
                    return null;
                }
                CoasterAnchorpointBlockEntity beB = (CoasterAnchorpointBlockEntity)blockEntity2;
                if (beA.legCount() != 1 || beB.legCount() != 1) {
                    return null;
                }
                if (beA.getAnchorPeerCurvesView().containsKey(b) || beB.getAnchorPeerCurvesView().containsKey(a)) {
                    return null;
                }
                p = CoasterCircularLoopPlacement.solePeer(beA);
                q = CoasterCircularLoopPlacement.solePeer(beB);
                if (p == null || q == null || p.equals((Object)q) || p.equals((Object)b) || q.equals((Object)a)) {
                    return null;
                }
                blockEntity = level.getBlockEntity(p);
                if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) break block15;
                beP = (CoasterAnchorpointBlockEntity)blockEntity;
                blockEntity = level.getBlockEntity(q);
                if (blockEntity instanceof CoasterAnchorpointBlockEntity) break block16;
            }
            return null;
        }
        CoasterAnchorpointBlockEntity beQ = (CoasterAnchorpointBlockEntity)blockEntity;
        if (beP.legCount() != 2 || beQ.legCount() != 2) {
            return null;
        }
        BlockPos pOther = CoasterCircularLoopPlacement.otherPeer(beP, a);
        BlockPos qOther = CoasterCircularLoopPlacement.otherPeer(beQ, b);
        if (pOther == null || qOther == null) {
            return null;
        }
        if (!pOther.equals((Object)q) || !qOther.equals((Object)p)) {
            return null;
        }
        return List.of(a.immutable(), p.immutable(), q.immutable(), b.immutable());
    }

    @Nullable
    private static BlockPos solePeer(CoasterAnchorpointBlockEntity be) {
        if (be.legCount() != 1) {
            return null;
        }
        return be.getAnchorPeerCurvesView().keySet().iterator().next().immutable();
    }

    @Nullable
    private static BlockPos otherPeer(CoasterAnchorpointBlockEntity be, BlockPos exclude) {
        BlockPos found = null;
        for (BlockPos peer : be.getAnchorPeerCurvesView().keySet()) {
            if (peer.equals((Object)exclude)) continue;
            if (found != null) {
                return null;
            }
            found = peer.immutable();
        }
        return found;
    }

    @Nullable
    private static CircleFit fitCircle(Level level, List<BlockPos> cycle, boolean useLiftedCenters) {
        double r0;
        Vec3[] pts = new Vec3[4];
        Vec3 sum = Vec3.ZERO;
        for (int i = 0; i < 4; ++i) {
            pts[i] = CoasterCircularLoopPlacement.anchorPointForFit(level, cycle.get(i), useLiftedCenters);
            sum = sum.add(pts[i]);
        }
        Vec3 centroid = sum.scale(0.25);
        @Nullable Vec3 planeN = CoasterAnchorCoplanarPlane.planeNormalFromWorldCenters(pts[0], pts[1], pts[2]);
        if (planeN == null) {
            return null;
        }
        double maxHeight = 0.0;
        for (Vec3 p : pts) {
            maxHeight = Math.max(maxHeight, Math.abs(p.subtract(centroid).dot(planeN)));
        }
        if (maxHeight > 0.35) {
            return null;
        }
        @Nullable Vec3 center = CoasterCircularLoopPlacement.circumcenterInPlane(pts[0], pts[1], pts[2], planeN);
        if (center == null) {
            center = centroid;
        }
        if ((r0 = pts[0].distanceTo(center = center.subtract(planeN.scale(center.subtract(centroid).dot(planeN))))) < 1.25) {
            return null;
        }
        for (int i = 1; i < 4; ++i) {
            double ri = pts[i].distanceTo(center);
            if (!(Math.abs(ri - r0) > 0.06 * r0)) continue;
            return null;
        }
        double radius = 0.0;
        for (Vec3 p : pts) {
            radius += p.distanceTo(center);
        }
        if ((radius *= 0.25) < 1.25) {
            return null;
        }
        Vec3 basisU = planeN.cross(Math.abs(planeN.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0));
        if (basisU.lengthSqr() < 1.0E-12) {
            return null;
        }
        basisU = basisU.normalize();
        Vec3 basisV = planeN.cross(basisU).normalize();
        return new CircleFit(center, radius, planeN, basisU, basisV);
    }

    @Nullable
    private static Vec3 circumcenterInPlane(Vec3 a, Vec3 b, Vec3 c, Vec3 planeN) {
        Vec3 ab = b.subtract(a);
        Vec3 ac = c.subtract(a);
        Vec3 n = planeN.normalize();
        Vec3 abPerp = n.cross(ab);
        Vec3 acPerp = n.cross(ac);
        if (abPerp.lengthSqr() < 1.0E-12 || acPerp.lengthSqr() < 1.0E-12) {
            return null;
        }
        abPerp = abPerp.normalize();
        acPerp = acPerp.normalize();
        Vec3 midAb = a.add(b).scale(0.5);
        Vec3 midAc = a.add(c).scale(0.5);
        Vec3 u = abPerp;
        Vec3 v = n.cross(u).normalize();
        double dx = midAc.subtract(midAb).dot(u);
        double dy = midAc.subtract(midAb).dot(v);
        double a12 = -acPerp.dot(u);
        double a22 = -acPerp.dot(v);
        double det = a22;
        if (Math.abs(det) < 1.0E-12) {
            return null;
        }
        double s = (dx * a22 - a12 * dy) / det;
        return midAb.add(abPerp.scale(s));
    }

    @Nullable
    private static OrientationLayout classifyOrientation(Level level, List<BlockPos> cycle, CircleFit fit) {
        int parallelSign = 0;
        int radialSign = 0;
        int parallelCount = 0;
        int radialCount = 0;
        for (BlockPos pos : cycle) {
            int s;
            Vec3 up = CoasterAnchorpointBlockEntity.localUp(level, pos);
            double planeDot = up.dot(fit.planeNormal());
            if (Math.abs(planeDot) >= 0.85) {
                int s2;
                int n = s2 = planeDot >= 0.0 ? 1 : -1;
                if (parallelSign == 0) {
                    parallelSign = s2;
                } else if (parallelSign != s2) {
                    return null;
                }
                ++parallelCount;
                continue;
            }
            Vec3 towardCenter = CoasterCircularLoopPlacement.inPlaneTowardCenter(level, pos, fit);
            if (towardCenter == null) {
                return null;
            }
            double radialDot = up.dot(towardCenter);
            if (Math.abs(radialDot) < 0.45) {
                return null;
            }
            int n = s = radialDot >= 0.0 ? 1 : -1;
            if (radialSign == 0) {
                radialSign = s;
            } else if (radialSign != s) {
                return null;
            }
            ++radialCount;
        }
        if (parallelCount == 4) {
            Vec3 faceUp = fit.planeNormal().normalize();
            if (parallelSign < 0) {
                faceUp = faceUp.scale(-1.0);
            }
            return new OrientationLayout(false, false, faceUp);
        }
        if (radialCount == 4) {
            return new OrientationLayout(true, radialSign > 0, Vec3.ZERO);
        }
        return null;
    }

    @Nullable
    private static Vec3 inPlaneTowardCenter(Level level, BlockPos pos, CircleFit fit) {
        Vec3 toward = fit.center().subtract(CoasterCircularLoopPlacement.anchorPointForFit(level, pos, false));
        if ((toward = toward.subtract(fit.planeNormal().scale(toward.dot(fit.planeNormal())))).lengthSqr() < 1.0E-12) {
            return null;
        }
        return toward.normalize();
    }

    private static List<BlockPos> orderAroundCircle(Level level, List<BlockPos> cycle, CircleFit fit, boolean useLiftedCenters) {
        record Angled(BlockPos pos, double angle) {
        }
        ArrayList<Angled> angled = new ArrayList<Angled>(4);
        for (BlockPos pos : cycle) {
            angled.add(new Angled(pos, CoasterCircularLoopPlacement.angleOnCircle(CoasterCircularLoopPlacement.anchorPointForFit(level, pos, useLiftedCenters), fit)));
        }
        angled.sort(Comparator.comparingDouble(Angled::angle));
        ArrayList<BlockPos> ordered = new ArrayList<BlockPos>(4);
        for (Angled a : angled) {
            ordered.add(a.pos());
        }
        return ordered;
    }

    private static Vec3 anchorPointForFit(Level level, BlockPos pos, boolean useLiftedCenters) {
        return useLiftedCenters ? CoasterAnchorpointBlockEntity.worldCenter(level, pos) : CoasterAnchorpointBlockEntity.worldCenterBase(level, pos);
    }

    @Nullable
    private static BezierConnection buildCircularArc(Level level, BlockPos from, BlockPos to, CircleFit fit, OrientationLayout orientation, TrackMaterial material, boolean girder) {
        Vec3 normalTo;
        Vec3 normalFrom;
        boolean useLifted = orientation.radial();
        double angFrom = CoasterCircularLoopPlacement.angleOnCircle(CoasterCircularLoopPlacement.anchorPointForFit(level, from, useLifted), fit);
        double angTo = CoasterCircularLoopPlacement.angleOnCircle(CoasterCircularLoopPlacement.anchorPointForFit(level, to, useLifted), fit);
        double alpha = angTo - angFrom;
        if (alpha <= 1.0E-6) {
            alpha += Math.PI * 2;
        }
        if (alpha < MIN_ARC_RADIANS || alpha > MAX_ARC_RADIANS) {
            return null;
        }
        Vec3 startFrom = CoasterCircularLoopPlacement.pointOnCircle(fit, angFrom);
        Vec3 startTo = CoasterCircularLoopPlacement.pointOnCircle(fit, angTo);
        Vec3 travelFrom = CoasterCircularLoopPlacement.unitTangentCcW(fit, angFrom);
        Vec3 travelTo = CoasterCircularLoopPlacement.unitTangentCcW(fit, angTo);
        if (travelFrom.lengthSqr() < 1.0E-12 || travelTo.lengthSqr() < 1.0E-12) {
            return null;
        }
        Vec3 axisFrom = travelFrom;
        Vec3 axisTo = travelTo.scale(-1.0);
        double handle = fit.radius() * 1.3333333333333333 * Math.tan(alpha * 0.25);
        handle = Math.max(1.0E-4, handle);
        Vec3 radialFrom = CoasterCircularLoopPlacement.radialAtAngle(fit, angFrom);
        Vec3 radialTo = CoasterCircularLoopPlacement.radialAtAngle(fit, angTo);
        if (orientation.radial()) {
            Vec3 faceFrom = orientation.towardCenter() ? radialFrom.scale(-1.0) : radialFrom;
            Vec3 faceTo = orientation.towardCenter() ? radialTo.scale(-1.0) : radialTo;
            normalFrom = CoasterCircularLoopPlacement.orthonormalToAxis(faceFrom, axisFrom);
            normalTo = CoasterCircularLoopPlacement.orthonormalToAxis(faceTo, axisTo);
        } else {
            normalFrom = CoasterCircularLoopPlacement.orthonormalToAxis(orientation.parallelFaceUp(), axisFrom);
            normalTo = CoasterCircularLoopPlacement.orthonormalToAxis(orientation.parallelFaceUp(), axisTo);
        }
        long fl = from.asLong();
        long tl = to.asLong();
        BlockPos lo = fl <= tl ? from : to;
        BlockPos hi = fl <= tl ? to : from;
        Vec3 startLo = lo.equals((Object)from) ? startFrom : startTo;
        Vec3 startHi = hi.equals((Object)from) ? startFrom : startTo;
        Vec3 axisLo = lo.equals((Object)from) ? axisFrom : axisTo;
        Vec3 axisHi = hi.equals((Object)from) ? axisFrom : axisTo;
        Vec3 normalLo = lo.equals((Object)from) ? normalFrom : normalTo;
        Vec3 normalHi = hi.equals((Object)from) ? normalFrom : normalTo;
        BezierConnection bc = new BezierConnection(Couple.create((Object)lo, (Object)hi), Couple.create((Object)startLo, (Object)startHi), Couple.create((Object)axisLo, (Object)axisHi), Couple.create((Object)normalLo, (Object)normalHi), true, girder, material);
        CoasterPeerCurveHandleLengths.putCanonical(bc, handle, handle);
        return bc;
    }

    private static Vec3 pointOnCircle(CircleFit fit, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return fit.center().add(fit.basisU().scale(fit.radius() * c)).add(fit.basisV().scale(fit.radius() * s));
    }

    private static Vec3 radialAtAngle(CircleFit fit, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return fit.basisU().scale(c).add(fit.basisV().scale(s)).normalize();
    }

    private static Vec3 unitTangentCcW(CircleFit fit, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        Vec3 t = fit.basisU().scale(-s).add(fit.basisV().scale(c));
        if (t.lengthSqr() < 1.0E-12) {
            return Vec3.ZERO;
        }
        return t.normalize();
    }

    private static double angleOnCircle(Vec3 worldPoint, CircleFit fit) {
        Vec3 p = worldPoint.subtract(fit.center());
        return Math.atan2(p.dot(fit.basisV()), p.dot(fit.basisU()));
    }

    private static Vec3 orthonormalToAxis(Vec3 normal, Vec3 axis) {
        Vec3 a = axis.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : axis.normalize();
        Vec3 n = normal.subtract(a.scale(a.dot(normal)));
        if (n.lengthSqr() < 1.0E-12) {
            n = new Vec3(0.0, 1.0, 0.0).subtract(a.scale(a.dot(new Vec3(0.0, 1.0, 0.0))));
        }
        if (n.lengthSqr() < 1.0E-12) {
            n = a.cross(new Vec3(1.0, 0.0, 0.0));
        }
        if (n.lengthSqr() < 1.0E-12) {
            n = a.cross(new Vec3(0.0, 0.0, 1.0));
        }
        return n.normalize();
    }

    private record CircleFit(Vec3 center, double radius, Vec3 planeNormal, Vec3 basisU, Vec3 basisV) {
    }

    private record OrientationLayout(boolean radial, boolean towardCenter, Vec3 parallelFaceUp) {
    }
}

