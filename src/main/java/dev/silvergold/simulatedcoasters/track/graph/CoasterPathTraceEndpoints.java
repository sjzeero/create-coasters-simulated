/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.TrackBlock
 *  com.simibubi.create.content.trains.track.TrackBlockEntity
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterPathTraceEndpoints {
    private static final double BEZIER_HANDLE_MATCH_BLOCKS = 0.11;
    private static final double OPEN_END_TANGENT_DT = 0.001;
    public static final double OPEN_END_HANDOFF_JOIN_BLOCKS = 0.22;
    private static final double OPEN_END_HANDOFF_JOIN_DIST_SQ = 0.0484;
    private static final double OPEN_END_HANDOFF_MIN_TANGENT_ALIGN = 0.85;

    private CoasterPathTraceEndpoints() {
    }

    public static Set<Integer> allEdgeIndices(CoasterPathGraph graph) {
        List<CoasterPathEdge> edges = graph.edges();
        HashSet<Integer> out = new HashSet<Integer>(Math.max(16, edges.size() * 4 / 3));
        for (int i = 0; i < edges.size(); ++i) {
            out.add(i);
        }
        return out;
    }

    public static List<Vec3> pathEndpointWorldPositions(Level level, CoasterPathGraph graph, Set<Integer> edgeIndices) {
        List<OpenEndRef> refs = CoasterPathTraceEndpoints.pathOpenEndRefs(level, graph, edgeIndices);
        if (refs.isEmpty()) {
            return List.of();
        }
        ArrayList<Vec3> out = new ArrayList<Vec3>(refs.size());
        for (OpenEndRef ref : refs) {
            out.add(ref.position());
        }
        out.sort((a, b) -> {
            int c = Double.compare(a.x, b.x);
            if (c != 0) {
                return c;
            }
            c = Double.compare(a.y, b.y);
            if (c != 0) {
                return c;
            }
            return Double.compare(a.z, b.z);
        });
        return List.copyOf(out);
    }

    public static List<OpenEndRef> pathOpenEndRefs(Level level, CoasterPathGraph graph, Set<Integer> edgeIndices) {
        if (edgeIndices.isEmpty()) {
            return List.of();
        }
        List<CoasterPathEdge> edges = graph.edges();
        HashMap<BlockPos, Integer> degree = new HashMap<BlockPos, Integer>();
        ArrayList<OpenEndRef> out = new ArrayList<OpenEndRef>();
        HashSet<BlockPos> skipDegreeOneHandledBySelfEdge = new HashSet<BlockPos>();
        Iterator<Object> iterator = edgeIndices.iterator();
        while (iterator.hasNext()) {
            int n = iterator.next();
            if (n < 0 || n >= edges.size()) continue;
            CoasterPathEdge e = edges.get(n);
            if (e.from().equals((Object)e.to())) {
                CoasterPathTraceEndpoints.addSelfEdgeTraceEndpoints(level, n, e, out, skipDegreeOneHandledBySelfEdge, edges, edgeIndices);
                continue;
            }
            degree.merge(e.from(), 1, Integer::sum);
            degree.merge(e.to(), 1, Integer::sum);
        }
        for (Map.Entry entry : degree.entrySet()) {
            int incIdx;
            BlockPos p;
            if ((Integer)entry.getValue() != 1 || skipDegreeOneHandledBySelfEdge.contains(p = (BlockPos)entry.getKey()) || (incIdx = CoasterPathTraceEndpoints.uniqueHighlightedEdgeIndexIncidentOnDistinctEndpoints(edges, edgeIndices, p)) < 0) continue;
            CoasterPathEdge inc = edges.get(incIdx);
            if (p.equals((Object)inc.from())) {
                out.add(new OpenEndRef(CoasterPathTraceEndpoints.openCurveEndSample(level, inc, true), incIdx, true));
                continue;
            }
            out.add(new OpenEndRef(CoasterPathTraceEndpoints.openCurveEndSample(level, inc, false), incIdx, false));
        }
        return List.copyOf(out);
    }

    public static boolean shouldDisengageAtOpenEndHit(Level level, CoasterPathGraph graph, @Nullable CoasterPathTrackFrame.GraphHit hit, List<OpenEndRef> openEndRefs, double hitMatchDistSq) {
        if (hit == null || openEndRefs.isEmpty()) {
            return false;
        }
        ArrayList<OpenEndRef> atHit = new ArrayList<OpenEndRef>();
        for (OpenEndRef ref : openEndRefs) {
            if (!(ref.position().distanceToSqr(hit.point()) <= hitMatchDistSq)) continue;
            atHit.add(ref);
        }
        if (atHit.isEmpty()) {
            return false;
        }
        List<CoasterPathEdge> edges = graph.edges();
        for (OpenEndRef a : atHit) {
            boolean hasHandoff = false;
            for (OpenEndRef b : openEndRefs) {
                if (a.edgeIndex() == b.edgeIndex() || a.position().distanceToSqr(b.position()) > 0.0484 || !CoasterPathTraceEndpoints.openEndsFormAlignedHandoff(level, edges, a, b)) continue;
                hasHandoff = true;
                break;
            }
            if (hasHandoff) continue;
            return true;
        }
        return false;
    }

    private static boolean openEndsFormAlignedHandoff(Level level, List<CoasterPathEdge> edges, OpenEndRef a, OpenEndRef b) {
        if (a.edgeIndex() < 0 || a.edgeIndex() >= edges.size() || b.edgeIndex() < 0 || b.edgeIndex() >= edges.size()) {
            return false;
        }
        Vec3 tA = CoasterPathTraceEndpoints.openEndOutwardTangent(level, edges.get(a.edgeIndex()), a.atFrom());
        Vec3 tB = CoasterPathTraceEndpoints.openEndOutwardTangent(level, edges.get(b.edgeIndex()), b.atFrom());
        if (tA.lengthSqr() < 1.0E-12 || tB.lengthSqr() < 1.0E-12) {
            return false;
        }
        double align = Math.abs(tA.normalize().dot(tB.normalize()));
        return align >= 0.85;
    }

    private static Vec3 openEndOutwardTangent(Level level, CoasterPathEdge edge, boolean atFrom) {
        Vec3 straightDir = CoasterPathTraceEndpoints.straightDirection(level, edge);
        if (straightDir != null) {
            return atFrom ? straightDir.scale(-1.0) : straightDir;
        }
        if (atFrom) {
            if (CoasterPathGraphManager.isOpenCurveEnd(level, edge, true)) {
                double ext = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, true);
                double t0 = -Math.min(0.001, ext);
                return CoasterPathTraceEndpoints.diff(level, edge, t0, 0.0);
            }
            double t1 = Math.min(1.0, 0.001);
            return CoasterPathTraceEndpoints.diff(level, edge, 0.0, t1);
        }
        if (CoasterPathGraphManager.isOpenCurveEnd(level, edge, false)) {
            double ext = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, false);
            double t1 = Math.min(1.001, 1.0 + ext);
            return CoasterPathTraceEndpoints.diff(level, edge, 1.0, t1);
        }
        double t0 = Math.max(0.0, 0.999);
        return CoasterPathTraceEndpoints.diff(level, edge, t0, 1.0);
    }

    private static Vec3 diff(Level level, CoasterPathEdge edge, double t0, double t1) {
        Vec3 a = CoasterPathGraphManager.sampleEdgePosition(level, edge, t0);
        Vec3 b = CoasterPathGraphManager.sampleEdgePosition(level, edge, t1);
        Vec3 d = b.subtract(a);
        if (d.lengthSqr() < 1.0E-14) {
            return Vec3.ZERO;
        }
        return d.normalize();
    }

    @Nullable
    private static Vec3 straightDirection(Level level, CoasterPathEdge edge) {
        if (edge.straightFromWorld() == null || edge.straightToWorld() == null) {
            return null;
        }
        Vec3 d = edge.straightToWorld().subtract(edge.straightFromWorld());
        if (d.lengthSqr() < 1.0E-14) {
            return null;
        }
        d = CoasterPathGraphManager.sublevelWorldDirection(level, CoasterPathGraphManager.edgeSubLevelAnchor(level, edge), d);
        if (d.lengthSqr() < 1.0E-14) {
            return null;
        }
        return d.normalize();
    }

    private static Vec3 openCurveEndSample(Level level, CoasterPathEdge edge, boolean atFrom) {
        if (CoasterPathGraphManager.isOpenCurveEnd(level, edge, atFrom)) {
            double ext = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, atFrom);
            return CoasterPathGraphManager.sampleEdgePosition(level, edge, atFrom ? -ext : 1.0 + ext);
        }
        return CoasterPathGraphManager.sampleEdgePosition(level, edge, atFrom ? 0.0 : 1.0);
    }

    private static int trackBezierConnectionCount(Level level, BlockPos p) {
        BlockState st = level.getBlockState(p);
        if (!st.hasProperty((Property)TrackBlock.HAS_BE) || !((Boolean)st.getValue((Property)TrackBlock.HAS_BE)).booleanValue()) {
            return 0;
        }
        BlockEntity blockEntity = level.getBlockEntity(p);
        if (!(blockEntity instanceof TrackBlockEntity)) {
            return 0;
        }
        TrackBlockEntity tbe = (TrackBlockEntity)blockEntity;
        return tbe.getConnections().size();
    }

    @Nullable
    private static Vec3 bezierHandleWorldNoOffset(Level level, BezierConnection bc, BlockPos host) {
        double t;
        BlockPos a = (BlockPos)bc.bePositions.getFirst();
        BlockPos b = (BlockPos)bc.bePositions.getSecond();
        if (a.equals((Object)host)) {
            t = 0.0;
        } else if (b.equals((Object)host)) {
            t = 1.0;
        } else {
            return null;
        }
        if (CoasterPathGraphManager.isCoasterGraphBezier(bc)) {
            return CoasterPathGraphManager.coasterBezierParametricSpineRaw(bc, t);
        }
        return bc.getPosition(t);
    }

    private static void addSelfEdgeTraceEndpoints(Level level, int edgeIndex, CoasterPathEdge e, List<OpenEndRef> out, Set<BlockPos> skipDegreeOneForBlock, List<CoasterPathEdge> allEdges, Set<Integer> edgeIndices) {
        BlockPos p = e.from();
        if (e.bezier() != null || e.straightFromWorld() == null || e.straightToWorld() == null) {
            out.add(new OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0), edgeIndex, true));
            out.add(new OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0), edgeIndex, false));
            return;
        }
        int nConn = CoasterPathTraceEndpoints.trackBezierConnectionCount(level, p);
        if (nConn >= 2) {
            return;
        }
        if (nConn == 0) {
            out.add(new OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0), edgeIndex, true));
            out.add(new OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0), edgeIndex, false));
            return;
        }
        if (CoasterPathTraceEndpoints.highlightedInterBlockStraightConnectsCurveCapToAnotherCurveTrack(level, allEdges, edgeIndices, p)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(p);
        if (!(blockEntity instanceof TrackBlockEntity)) {
            return;
        }
        TrackBlockEntity tbe = (TrackBlockEntity)blockEntity;
        BezierConnection bc = (BezierConnection)tbe.getConnections().values().iterator().next();
        Vec3 handleRaw = CoasterPathTraceEndpoints.bezierHandleWorldNoOffset(level, bc, p);
        if (handleRaw == null) {
            out.add(new OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0), edgeIndex, true));
            out.add(new OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0), edgeIndex, false));
            return;
        }
        Vec3 w0 = e.straightFromWorld();
        Vec3 w1 = e.straightToWorld();
        double r2 = 0.0121;
        double d0 = w0.distanceToSqr(handleRaw);
        double d1 = w1.distanceToSqr(handleRaw);
        skipDegreeOneForBlock.add(p);
        if (d0 <= 0.0121 && d0 <= d1) {
            out.add(new OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0), edgeIndex, false));
        } else if (d1 <= 0.0121 && d1 <= d0) {
            out.add(new OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0), edgeIndex, true));
        } else if (d0 > d1) {
            out.add(new OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0), edgeIndex, true));
        } else {
            out.add(new OpenEndRef(CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0), edgeIndex, false));
        }
    }

    private static boolean highlightedInterBlockStraightConnectsCurveCapToAnotherCurveTrack(Level level, List<CoasterPathEdge> allEdges, Set<Integer> edgeIndices, BlockPos p) {
        for (int i : edgeIndices) {
            BlockPos other;
            CoasterPathEdge e;
            if (i < 0 || i >= allEdges.size() || (e = allEdges.get(i)).bezier() != null || e.from().equals((Object)e.to()) || e.straightFromWorld() == null || e.straightToWorld() == null) continue;
            if (e.from().equals((Object)p)) {
                other = e.to();
            } else {
                if (!e.to().equals((Object)p)) continue;
                other = e.from();
            }
            if (other.equals((Object)p) || CoasterPathTraceEndpoints.trackBezierConnectionCount(level, other) < 1) continue;
            return true;
        }
        return false;
    }

    private static int uniqueHighlightedEdgeIndexIncidentOnDistinctEndpoints(List<CoasterPathEdge> edges, Set<Integer> edgeIndices, BlockPos p) {
        int found = -1;
        for (int i : edgeIndices) {
            CoasterPathEdge e;
            if (i < 0 || i >= edges.size() || (e = edges.get(i)).from().equals((Object)e.to()) || !e.from().equals((Object)p) && !e.to().equals((Object)p)) continue;
            if (found >= 0) {
                return -1;
            }
            found = i;
        }
        return found;
    }

    public record OpenEndRef(Vec3 position, int edgeIndex, boolean atFrom) {
    }
}

