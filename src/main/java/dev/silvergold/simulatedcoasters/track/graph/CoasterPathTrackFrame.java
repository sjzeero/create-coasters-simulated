/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Matrix3d
 *  org.joml.Matrix3dc
 *  org.joml.Quaterniond
 */
package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.graph.CoasterBezierEdgeSamplingContext;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3d;
import org.joml.Matrix3dc;
import org.joml.Quaterniond;

public final class CoasterPathTrackFrame {
    public static final double DEFAULT_MAX_LINK_SQ = 9216.0;
    private static final Vec3 WORLD_UP = new Vec3(0.0, 1.0, 0.0);
    private static final int BEZIER_COARSE_STEPS = 64;
    private static final int BEZIER_TERNARY_ITERS = 48;
    private static final double WARM_START_MAX_BEARING_MOVE_BLOCKS = 1.25;
    private static final double WARM_START_MAX_BEARING_MOVE_SQ = 1.5625;
    private static final double WARM_START_T_HALF_WIDTH_BASE = 0.045;
    private static final double WARM_START_T_HALF_WIDTH_PER_BLOCK = 0.03;
    private static final int WARM_TERNARY_ITERS = 28;
    private static final int WARM_QUICK_COARSE_STEPS = 12;
    private static final double WARM_WINDOW_EDGE_DIST_EPS = 1.0E-5;
    private static final double WARM_QUICK_COARSE_IMPROVE_EPS = 1.0E-4;
    private static final double ENDPOINT_T_MARGIN = 0.06;

    private CoasterPathTrackFrame() {
    }

    @Nullable
    public static Quaterniond quaternionWorldFromTrackAt(Level level, CoasterPathGraph graph, Vec3 worldPos, double maxDistSq) {
        GraphHit hit = CoasterPathTrackFrame.nearestGraphHit(level, graph, worldPos, maxDistSq);
        if (hit == null) {
            return null;
        }
        TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, hit);
        return CoasterPathTrackFrame.quaternionFromTrackBasis(basis);
    }

    public static Quaterniond quaternionFromTrackBasis(TrackBasis b) {
        Vec3 r = b.right();
        Vec3 u = b.up();
        Vec3 f = b.forward();
        Matrix3d m = new Matrix3d();
        m.setColumn(0, r.x, r.y, r.z);
        m.setColumn(1, u.x, u.y, u.z);
        m.setColumn(2, -f.x, -f.y, -f.z);
        return new Quaterniond().setFromNormalized((Matrix3dc)m);
    }

    @Nullable
    public static GraphHit nearestGraphHit(Level level, CoasterPathGraph graph, Vec3 from, double maxDistSq) {
        return CoasterPathTrackFrame.nearestGraphHit(level, graph, from, maxDistSq, null);
    }

    @Nullable
    public static GraphHit nearestGraphHit(Level level, CoasterPathGraph graph, Vec3 from, double maxDistSq, @Nullable Double partialTickForRender) {
        List<CoasterPathEdge> edges = graph.edges();
        if (edges.isEmpty()) {
            return null;
        }
        List<double[]> bounds = graph.ensureEdgeCenterlineBounds(level);
        double margin = Math.sqrt(maxDistSq) + 0.001;
        GraphHit best = null;
        double bestD = Double.POSITIVE_INFINITY;
        for (int i = 0; i < edges.size(); ++i) {
            CoasterPathEdge edge;
            GraphHit cand;
            double d;
            if (!CoasterPathTrackFrame.pointInsideInflatedAabb(bounds.get(i), from, margin) || !((d = (cand = (edge = edges.get(i)).bezier() != null ? CoasterPathTrackFrame.closestHitOnBezierEdge(level, edge, from, partialTickForRender, true) : CoasterPathTrackFrame.closestHitOnStraightEdge(level, edge, from, partialTickForRender)).point().distanceToSqr(from)) < bestD)) continue;
            bestD = d;
            best = cand;
        }
        if (best == null || bestD > maxDistSq) {
            return null;
        }
        return best;
    }

    public static GraphHit closestHitOnEdge(Level level, CoasterPathEdge edge, Vec3 from) {
        return CoasterPathTrackFrame.closestHitOnEdge(level, edge, from, null);
    }

    public static GraphHit closestHitOnEdge(Level level, CoasterPathEdge edge, Vec3 from, @Nullable Double partialTickForRender) {
        return CoasterPathTrackFrame.closestHitOnEdge(level, edge, from, partialTickForRender, true);
    }

    public static GraphHit closestHitOnEdgeForEngagedSnap(Level level, CoasterPathEdge edge, Vec3 bearingWorld) {
        return CoasterPathTrackFrame.closestHitOnEdgeForEngagedSnap(level, edge, bearingWorld, Double.NaN, null);
    }

    public static GraphHit closestHitOnEdgeForEngagedSnap(Level level, CoasterPathEdge edge, Vec3 bearingWorld, double hintT, @Nullable Vec3 lastBearingWorld) {
        Vec3 bearingPlotLocal;
        if (CoasterPathGraphManager.edgeHostedOnSubLevel(level, edge) && (bearingPlotLocal = CoasterPathGraphManager.bearingWorldInTrackHostPlotLocal(level, edge, bearingWorld)) != null) {
            return CoasterPathTrackFrame.closestHitOnEngagedEdgeInTrackHostPlotLocal(level, edge, bearingPlotLocal, hintT, lastBearingWorld, bearingWorld);
        }
        return CoasterPathTrackFrame.closestHitOnEdge(level, edge, bearingWorld, null, false);
    }

    private static GraphHit closestHitOnEngagedEdgeInTrackHostPlotLocal(Level level, CoasterPathEdge edge, Vec3 bearingPlotLocal, double hintT, @Nullable Vec3 lastBearingWorld, Vec3 bearingWorld) {
        if (edge.bezier() != null) {
            if (CoasterPathGraphManager.isCoasterGraphBezier(edge.bezier())) {
                return CoasterPathTrackFrame.closestHitOnCoasterBezierEdgePlotLocalEngaged(level, edge, bearingPlotLocal, hintT, lastBearingWorld, bearingWorld);
            }
            Vec3 bearingWorldFromPlot = CoasterPathGraphManager.trackHostPlotLocalToWorld(level, edge, bearingPlotLocal);
            return CoasterPathTrackFrame.closestHitOnEdge(level, edge, bearingWorldFromPlot, null, false);
        }
        return CoasterPathTrackFrame.closestHitOnStraightEdgePlotLocalEngaged(level, edge, bearingPlotLocal);
    }

    private static GraphHit closestHitOnStraightEdgePlotLocalEngaged(Level level, CoasterPathEdge edge, Vec3 bearingPlotLocal) {
        Vec3 p0 = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, 0.0);
        Vec3 p1 = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, 1.0);
        Vec3 ab = p1.subtract(p0);
        double ab2 = ab.lengthSqr();
        if (ab2 < 1.0E-16) {
            Vec3 point = CoasterPathGraphManager.sampleEdgePosition(level, edge, 0.0);
            return new GraphHit(point, edge, 0.0);
        }
        double t = bearingPlotLocal.subtract(p0).dot(ab) / ab2;
        t = Mth.clamp((double)t, (double)0.0, (double)1.0);
        Vec3 point = CoasterPathGraphManager.sampleEdgePosition(level, edge, t);
        return new GraphHit(point, edge, t);
    }

    private static GraphHit closestHitOnCoasterBezierEdgePlotLocalEngaged(Level level, CoasterPathEdge edge, Vec3 bearingPlotLocal, double hintT, @Nullable Vec3 lastBearingWorld, Vec3 bearingWorld) {
        CoasterBezierEdgeSamplingContext bezierContext = CoasterBezierEdgeSamplingContext.forCoasterEdge(level, edge);
        if (bezierContext == null) {
            Vec3 bearingWorldFromPlot = CoasterPathGraphManager.trackHostPlotLocalToWorld(level, edge, bearingPlotLocal);
            return CoasterPathTrackFrame.closestHitOnEdge(level, edge, bearingWorldFromPlot, null, false);
        }
        @Nullable Double warmT = CoasterPathTrackFrame.tryWarmStartCoasterBezierPlotLocal(level, edge, bearingPlotLocal, bezierContext, hintT, lastBearingWorld, bearingWorld);
        if (warmT != null) {
            Vec3 point = CoasterPathGraphManager.sampleEdgePosition(level, edge, warmT);
            return new GraphHit(point, edge, warmT);
        }
        return CoasterPathTrackFrame.closestHitOnCoasterBezierEdgePlotLocalEngagedFull(level, edge, bearingPlotLocal, bezierContext);
    }

    @Nullable
    private static Double tryWarmStartCoasterBezierPlotLocal(Level level, CoasterPathEdge edge, Vec3 bearingPlotLocal, CoasterBezierEdgeSamplingContext bezierContext, double hintT, @Nullable Vec3 lastBearingWorld, Vec3 bearingWorld) {
        double tHi;
        if (!Double.isFinite(hintT)) {
            return null;
        }
        double tLo = bezierContext.tLo();
        if (CoasterPathTrackFrame.nearBezierSearchEndpoint(hintT, tLo, tHi = bezierContext.tHi())) {
            return null;
        }
        if (lastBearingWorld != null && bearingWorld.distanceToSqr(lastBearingWorld) > 1.5625) {
            return null;
        }
        double bearingMove = lastBearingWorld != null ? Math.sqrt(bearingWorld.distanceToSqr(lastBearingWorld)) : 0.0;
        double halfWidth = 0.045 + bearingMove * 0.03;
        double lo = Math.max(tLo, hintT - halfWidth);
        double hi = Math.min(tHi, hintT + halfWidth);
        if (hi - lo < 1.0E-9) {
            return null;
        }
        double warmT = CoasterPathTrackFrame.ternaryMinimizeDistancePlotLocal(level, edge, bearingPlotLocal, lo, hi, 28, bezierContext);
        double warmD = CoasterPathTrackFrame.plotLocalBankedDistSq(level, edge, warmT, bearingPlotLocal, bezierContext);
        double dLo = CoasterPathTrackFrame.plotLocalBankedDistSq(level, edge, lo, bearingPlotLocal, bezierContext);
        double dHi = CoasterPathTrackFrame.plotLocalBankedDistSq(level, edge, hi, bearingPlotLocal, bezierContext);
        if (dLo + 1.0E-5 < warmD || dHi + 1.0E-5 < warmD) {
            return null;
        }
        double quickBestT = warmT;
        double quickBestD = warmD;
        for (int i = 0; i <= 12; ++i) {
            double ti = Mth.lerp((double)((double)i / 12.0), (double)tLo, (double)tHi);
            double d = CoasterPathTrackFrame.plotLocalBankedDistSq(level, edge, ti, bearingPlotLocal, bezierContext);
            if (!(d < quickBestD)) continue;
            quickBestD = d;
            quickBestT = ti;
        }
        if (quickBestD + 1.0E-4 < warmD) {
            return null;
        }
        if (quickBestT < lo - halfWidth * 0.25 || quickBestT > hi + halfWidth * 0.25) {
            return null;
        }
        return warmT;
    }

    private static GraphHit closestHitOnCoasterBezierEdgePlotLocalEngagedFull(Level level, CoasterPathEdge edge, Vec3 bearingPlotLocal, CoasterBezierEdgeSamplingContext bezierContext) {
        int i;
        double tLo = bezierContext.tLo();
        double tHi = bezierContext.tHi();
        int n = 64;
        double[] d = new double[65];
        for (int i2 = 0; i2 <= 64; ++i2) {
            double ti = Mth.lerp((double)((double)i2 / 64.0), (double)tLo, (double)tHi);
            d[i2] = CoasterPathTrackFrame.plotLocalBankedDistSq(level, edge, ti, bearingPlotLocal, bezierContext);
        }
        double bestT = tLo;
        double bestD = d[0];
        for (i = 1; i <= 64; ++i) {
            if (!(d[i] < bestD)) continue;
            bestD = d[i];
            bestT = Mth.lerp((double)((double)i / 64.0), (double)tLo, (double)tHi);
        }
        for (i = 0; i <= 64; ++i) {
            double hi;
            double lo;
            double tRef;
            double dRef;
            boolean localMin;
            boolean bl = localMin = (i == 0 || d[i] <= d[i - 1]) && (i == 64 || d[i] <= d[i + 1]);
            if (!localMin || !((dRef = CoasterPathTrackFrame.plotLocalBankedDistSq(level, edge, tRef = CoasterPathTrackFrame.ternaryMinimizeDistancePlotLocal(level, edge, bearingPlotLocal, lo = i == 0 ? tLo : Mth.lerp((double)((double)(i - 1) / 64.0), (double)tLo, (double)tHi), hi = i == 64 ? tHi : Mth.lerp((double)((double)(i + 1) / 64.0), (double)tLo, (double)tHi), 48, bezierContext), bearingPlotLocal, bezierContext)) < bestD)) continue;
            bestD = dRef;
            bestT = tRef;
        }
        Vec3 point = CoasterPathGraphManager.sampleEdgePosition(level, edge, bestT);
        return new GraphHit(point, edge, bestT);
    }

    private static double plotLocalBankedDistSq(Level level, CoasterPathEdge edge, double t, Vec3 bearingPlotLocal, CoasterBezierEdgeSamplingContext bezierContext) {
        return CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, t, bezierContext).distanceToSqr(bearingPlotLocal);
    }

    private static boolean nearBezierSearchEndpoint(double t, double tLo, double tHi) {
        double span = Math.max(tHi - tLo, 1.0E-9);
        double margin = Math.max(0.06, 0.08 * span);
        return t <= tLo + margin || t >= tHi - margin;
    }

    private static double ternaryMinimizeDistancePlotLocal(Level level, CoasterPathEdge edge, Vec3 fromPlotLocal, double lo, double hi, int iters, CoasterBezierEdgeSamplingContext bezierContext) {
        for (int k = 0; k < iters && hi - lo > 1.0E-9; ++k) {
            double d2;
            double m1 = lo + (hi - lo) / 3.0;
            double m2 = hi - (hi - lo) / 3.0;
            double d1 = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, m1, bezierContext).distanceToSqr(fromPlotLocal);
            if (d1 < (d2 = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, m2, bezierContext).distanceToSqr(fromPlotLocal))) {
                hi = m2;
                continue;
            }
            lo = m1;
        }
        return (lo + hi) * 0.5;
    }

    private static GraphHit closestHitOnEdge(Level level, CoasterPathEdge edge, Vec3 from, @Nullable Double partialTickForRender, boolean spineOnlyForDistance) {
        return edge.bezier() != null ? CoasterPathTrackFrame.closestHitOnBezierEdge(level, edge, from, partialTickForRender, spineOnlyForDistance) : CoasterPathTrackFrame.closestHitOnStraightEdge(level, edge, from, partialTickForRender);
    }

    @Nullable
    public static GraphHit nearestGraphHit(Level level, List<CoasterPathEdge> edges, Vec3 from, double maxDistSq) {
        return CoasterPathTrackFrame.nearestGraphHit(level, edges, from, maxDistSq, null);
    }

    @Nullable
    public static GraphHit nearestGraphHit(Level level, List<CoasterPathEdge> edges, Vec3 from, double maxDistSq, @Nullable Double partialTickForRender) {
        GraphHit best = null;
        double bestD = Double.POSITIVE_INFINITY;
        for (CoasterPathEdge edge : edges) {
            GraphHit graphHit = edge.bezier() != null ? CoasterPathTrackFrame.closestHitOnBezierEdge(level, edge, from, partialTickForRender, true) : CoasterPathTrackFrame.closestHitOnStraightEdge(level, edge, from, partialTickForRender);
            GraphHit cand = graphHit;
            double d = cand.point().distanceToSqr(from);
            if (!(d < bestD)) continue;
            bestD = d;
            best = cand;
        }
        if (best == null || bestD > maxDistSq) {
            return null;
        }
        return best;
    }

    private static boolean pointInsideInflatedAabb(double[] b, Vec3 p, double margin) {
        return p.x >= b[0] - margin && p.x <= b[3] + margin && p.y >= b[1] - margin && p.y <= b[4] + margin && p.z >= b[2] - margin && p.z <= b[5] + margin;
    }

    public static TrackBasis trackBasisAtHit(Level level, GraphHit hit) {
        return CoasterPathTrackFrame.trackBasisAtHit(level, hit, null);
    }

    public static TrackBasis trackBasisAtHit(Level level, GraphHit hit, float partialTick) {
        return CoasterPathTrackFrame.trackBasisAtHit(level, hit, Double.valueOf(partialTick));
    }

    public static TrackBasis trackBasisAtHit(Level level, GraphHit hit, @Nullable Double partialTickForRender) {
        BlockPos host = CoasterPathGraphManager.edgeSubLevelAnchor(level, hit.edge());
        TrackBasis plot = CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit);
        if (partialTickForRender == null && !CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, host)) {
            return plot;
        }
        return CoasterPathTrackFrame.transformTrackBasisToWorld(level, host, plot, partialTickForRender);
    }

    public static TrackBasis trackBasisPlotLocalAtHit(Level level, GraphHit hit) {
        Vec3 right;
        Vec3 alt;
        Vec3 upHint;
        double t;
        CoasterPathEdge edge = hit.edge();
        Vec3 forwardPlotLocal = CoasterPathTrackFrame.plotLocalTangentAt(level, edge, t = hit.t());
        Vec3 forward = forwardPlotLocal.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : forwardPlotLocal.normalize();
        forward = forward.scale((double)edge.tangentSign());
        if (edge.bezier() != null) {
            BezierConnection bc = edge.bezier();
            if (bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
                float faceT = (float)Mth.clamp((double)t, (double)0.0, (double)1.0);
                Vec3 geomTanPlot = forwardPlotLocal.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : forwardPlotLocal.normalize();
                upHint = CoasterBezierRailFrames.faceNormalAt(bc, faceT, geomTanPlot, level);
            } else {
                upHint = bc.getNormal(t);
            }
            if (upHint.lengthSqr() < 1.0E-14) {
                upHint = WORLD_UP;
            }
        } else {
            upHint = WORLD_UP;
        }
        Vec3 upCandidate = upHint.subtract(forward.scale(forward.dot(upHint)));
        if (upCandidate.lengthSqr() < 1.0E-12 && (upCandidate = (alt = new Vec3(1.0, 0.0, 0.0)).subtract(forward.scale(forward.dot(alt)))).lengthSqr() < 1.0E-12) {
            upCandidate = new Vec3(0.0, 0.0, 1.0).subtract(forward.scale(forward.dot(new Vec3(0.0, 0.0, 1.0))));
        }
        if ((right = forward.cross(upCandidate = upCandidate.normalize())).lengthSqr() < 1.0E-12 && (right = forward.cross(new Vec3(1.0, 0.0, 0.0))).lengthSqr() < 1.0E-12) {
            right = forward.cross(new Vec3(0.0, 0.0, 1.0));
        }
        right = right.normalize();
        Vec3 up = right.cross(forward).normalize();
        return new TrackBasis(forward, right, up);
    }

    private static TrackBasis transformTrackBasisToWorld(Level level, BlockPos host, TrackBasis plot, @Nullable Double partialTickForRender) {
        Vec3 forward = CoasterPathGraphManager.sublevelWorldDirection(level, host, plot.forward(), partialTickForRender);
        Vec3 right = CoasterPathGraphManager.sublevelWorldDirection(level, host, plot.right(), partialTickForRender);
        Vec3 up = CoasterPathGraphManager.sublevelWorldDirection(level, host, plot.up(), partialTickForRender);
        return new TrackBasis(forward, right, up);
    }

    public static Vec3 pathAnchorForSnappedBlock(Level level, GraphHit hit) {
        return CoasterPathTrackFrame.pathAnchorForSnappedBlock(level, hit, null);
    }

    public static Vec3 pathAnchorForSnappedBlock(Level level, GraphHit hit, float partialTick) {
        return CoasterPathTrackFrame.pathAnchorForSnappedBlock(level, hit, Double.valueOf(partialTick));
    }

    private static Vec3 pathAnchorForSnappedBlock(Level level, GraphHit hit, @Nullable Double partialTickForRender) {
        Vec3 p = partialTickForRender == null ? hit.point() : CoasterPathGraphManager.sampleEdgePosition(level, hit.edge(), hit.t(), partialTickForRender);
        TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender);
        Vec3 up = basis.up();
        if (up.lengthSqr() < 1.0E-12) {
            return p.add(0.0, 0.3125, 0.0);
        }
        return p.add(up.normalize().scale(0.3125));
    }

    public static Vec3 pathAnchorForSnappedBlockAtSpine(Level level, GraphHit basisHit, Vec3 spineWorld) {
        TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, basisHit);
        Vec3 up = basis.up();
        if (up.lengthSqr() < 1.0E-12) {
            return spineWorld.add(0.0, 0.3125, 0.0);
        }
        return spineWorld.add(up.normalize().scale(0.3125));
    }

    private static GraphHit closestHitOnStraightEdge(Level level, CoasterPathEdge edge, Vec3 from, @Nullable Double partialTickForRender) {
        Vec3 p0 = CoasterPathGraphManager.sampleEdgePosition(level, edge, 0.0, partialTickForRender);
        Vec3 p1 = CoasterPathGraphManager.sampleEdgePosition(level, edge, 1.0, partialTickForRender);
        Vec3 ab = p1.subtract(p0);
        double ab2 = ab.lengthSqr();
        if (ab2 < 1.0E-16) {
            return new GraphHit(p0, edge, 0.0);
        }
        double t = from.subtract(p0).dot(ab) / ab2;
        t = Mth.clamp((double)t, (double)0.0, (double)1.0);
        Vec3 point = p0.add(ab.scale(t));
        return new GraphHit(point, edge, t);
    }

    private static GraphHit closestHitOnBezierEdge(Level level, CoasterPathEdge edge, Vec3 from, @Nullable Double partialTickForRender, boolean spineOnlyForDistance) {
        int i;
        double[] dArray;
        if (CoasterPathGraphManager.isCoasterGraphBezier(edge.bezier())) {
            dArray = CoasterPathGraphManager.coasterBezierSampleTRange(level, edge);
        } else {
            double[] dArray2 = new double[2];
            dArray2[0] = 0.0;
            dArray = dArray2;
            dArray2[1] = 1.0;
        }
        double[] tRange = dArray;
        double tLo = tRange[0];
        double tHi = tRange[1];
        int n = 64;
        double[] d = new double[65];
        for (int i2 = 0; i2 <= 64; ++i2) {
            double ti = Mth.lerp((double)((double)i2 / 64.0), (double)tLo, (double)tHi);
            d[i2] = CoasterPathTrackFrame.sampleForDistance(level, edge, ti, partialTickForRender, spineOnlyForDistance).distanceToSqr(from);
        }
        double bestT = tLo;
        double bestD = d[0];
        for (i = 1; i <= 64; ++i) {
            if (!(d[i] < bestD)) continue;
            bestD = d[i];
            bestT = Mth.lerp((double)((double)i / 64.0), (double)tLo, (double)tHi);
        }
        for (i = 0; i <= 64; ++i) {
            double hi;
            double lo;
            double tRef;
            double dRef;
            boolean localMin;
            boolean bl = localMin = (i == 0 || d[i] <= d[i - 1]) && (i == 64 || d[i] <= d[i + 1]);
            if (!localMin || !((dRef = CoasterPathTrackFrame.sampleForDistance(level, edge, tRef = CoasterPathTrackFrame.ternaryMinimizeDistance(level, edge, from, lo = i == 0 ? tLo : Mth.lerp((double)((double)(i - 1) / 64.0), (double)tLo, (double)tHi), hi = i == 64 ? tHi : Mth.lerp((double)((double)(i + 1) / 64.0), (double)tLo, (double)tHi), 48, partialTickForRender, spineOnlyForDistance), partialTickForRender, spineOnlyForDistance).distanceToSqr(from)) < bestD)) continue;
            bestD = dRef;
            bestT = tRef;
        }
        Vec3 point = CoasterPathGraphManager.sampleEdgePosition(level, edge, bestT, partialTickForRender);
        return new GraphHit(point, edge, bestT);
    }

    private static Vec3 sampleForDistance(Level level, CoasterPathEdge edge, double t, @Nullable Double partialTickForRender, boolean spineOnlyForDistance) {
        if (!spineOnlyForDistance) {
            return CoasterPathGraphManager.sampleEdgePosition(level, edge, t, partialTickForRender);
        }
        return CoasterPathTrackFrame.edgePositionForNearestSearch(level, edge, t, partialTickForRender);
    }

    private static Vec3 straightForward(CoasterPathEdge edge) {
        if (edge.straightFromWorld() == null || edge.straightToWorld() == null) {
            return null;
        }
        Vec3 d = edge.straightToWorld().subtract(edge.straightFromWorld());
        if (d.lengthSqr() < 1.0E-14) {
            return null;
        }
        return d;
    }

    private static Vec3 plotLocalTangentAt(Level level, CoasterPathEdge edge, double t) {
        Vec3 straight = CoasterPathTrackFrame.straightForward(edge);
        if (straight != null) {
            return straight;
        }
        BezierConnection bc = edge.bezier();
        if (bc == null) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        if (CoasterPathGraphManager.isCoasterGraphBezier(bc)) {
            return CoasterPathGraphManager.plotLocalCenterlineTangentAlongIncreasingT(level, edge, t);
        }
        double dt = 1.0E-4;
        double tm = t - dt;
        double tp = t + dt;
        if (tm < 0.0) {
            tm = 0.0;
            tp = Math.min(1.0, 2.0 * dt);
        } else if (tp > 1.0) {
            tp = 1.0;
            tm = Math.max(0.0, 1.0 - 2.0 * dt);
        }
        Vec3 pM = bc.getPosition(tm).add(CoasterPathGraphManager.PATH_CENTER_OFFSET);
        Vec3 pP = bc.getPosition(tp).add(CoasterPathGraphManager.PATH_CENTER_OFFSET);
        Vec3 d = pP.subtract(pM);
        if (d.lengthSqr() < 1.0E-14) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return d;
    }

    private static Vec3 edgePositionForNearestSearch(Level level, CoasterPathEdge edge, double t, @Nullable Double partialTickForRender) {
        if (CoasterPathGraphManager.edgeHostedOnSubLevel(level, edge)) {
            return CoasterPathGraphManager.sampleEdgePosition(level, edge, t, partialTickForRender);
        }
        return CoasterPathGraphManager.sampleEdgePositionForNearestQuery(level, edge, t, partialTickForRender);
    }

    private static double ternaryMinimizeDistance(Level level, CoasterPathEdge edge, Vec3 from, double lo, double hi, int iters, @Nullable Double partialTickForRender, boolean spineOnlyForDistance) {
        for (int k = 0; k < iters && hi - lo > 1.0E-9; ++k) {
            double d2;
            double m1 = lo + (hi - lo) / 3.0;
            double m2 = hi - (hi - lo) / 3.0;
            double d1 = CoasterPathTrackFrame.sampleForDistance(level, edge, m1, partialTickForRender, spineOnlyForDistance).distanceToSqr(from);
            if (d1 < (d2 = CoasterPathTrackFrame.sampleForDistance(level, edge, m2, partialTickForRender, spineOnlyForDistance).distanceToSqr(from))) {
                hi = m2;
                continue;
            }
            lo = m1;
        }
        return (lo + hi) * 0.5;
    }

    public record GraphHit(Vec3 point, CoasterPathEdge edge, double t) {
    }

    public record TrackBasis(Vec3 forward, Vec3 right, Vec3 up) {
    }
}

