/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.graph;

import dev.silvergold.simulatedcoasters.Config;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathNode;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterPathGraphSpans {
    private static final int ARC_SAMPLES_PER_UNIT = 16;

    private CoasterPathGraphSpans() {
    }

    @Nullable
    public static SpanPath spanPathFromAnchor(Level level, CoasterPathGraph graph, BlockPos startAnchor, CoasterPathTrackFrame.GraphHit end, double partialTickForRender) {
        if (end == null) {
            return null;
        }
        List<CoasterPathEdge> edges = graph.edges();
        int endIdx = CoasterPathGraphSpans.indexOfEdge(edges, end.edge());
        if (endIdx < 0) {
            return null;
        }
        SpanPath best = null;
        for (int i = 0; i < edges.size(); ++i) {
            double startT;
            CoasterPathEdge startEdge = edges.get(i);
            if (startEdge.from().equals((Object)startAnchor)) {
                startT = 0.0;
            } else {
                if (!startEdge.to().equals((Object)startAnchor)) continue;
                startT = 1.0;
            }
            SpanPath path = CoasterPathGraphSpans.resolveSpanPathBetween(level, edges, i, startT, endIdx, end.t(), partialTickForRender);
            if (path == null || best != null && !(path.totalLength() < best.totalLength())) continue;
            best = path;
        }
        return best;
    }

    @Nullable
    public static SpanPath spanPathBetweenAnchors(Level level, CoasterPathGraph graph, BlockPos startAnchor, BlockPos endAnchor, double partialTickForRender) {
        if (startAnchor.equals((Object)endAnchor)) {
            return null;
        }
        SpanPath best = null;
        for (CoasterPathTrackFrame.GraphHit endHit : CoasterPathGraphSpans.anchorJunctionHits(level, graph, endAnchor, partialTickForRender)) {
            SpanPath path = CoasterPathGraphSpans.spanPathFromAnchor(level, graph, startAnchor, endHit, partialTickForRender);
            if (path == null || best != null && !(path.totalLength() < best.totalLength())) continue;
            best = path;
        }
        return best;
    }

    public static List<CoasterPathTrackFrame.GraphHit> anchorJunctionHits(Level level, CoasterPathGraph graph, BlockPos anchor, double partialTickForRender) {
        Vec3 center = CoasterPathGraphManager.sampleNodePosition(level, new CoasterPathNode(anchor, Vec3.ZERO), partialTickForRender);
        ArrayList<CoasterPathTrackFrame.GraphHit> hits = new ArrayList<CoasterPathTrackFrame.GraphHit>();
        for (CoasterPathEdge edge : graph.edges()) {
            double t;
            if (edge.from().equals((Object)anchor)) {
                t = 0.0;
            } else {
                if (!edge.to().equals((Object)anchor)) continue;
                t = 1.0;
            }
            hits.add(new CoasterPathTrackFrame.GraphHit(center, edge, t));
        }
        return hits;
    }

    @Nullable
    public static Double pathLengthFromAnchor(Level level, CoasterPathGraph graph, BlockPos startAnchor, CoasterPathTrackFrame.GraphHit end, double partialTickForRender) {
        SpanPath path = CoasterPathGraphSpans.spanPathFromAnchor(level, graph, startAnchor, end, partialTickForRender);
        return path == null ? null : Double.valueOf(path.totalLength());
    }

    public static boolean isWithinChainLiftSpan(@Nullable Double pathLengthBlocks) {
        return pathLengthBlocks != null && pathLengthBlocks <= Config.maxChainLiftSpanBlocks();
    }

    public static double arcLengthBetween(Level level, CoasterPathEdge edge, double t0, double t1, double partialTickForRender) {
        return CoasterPathGraphSpans.arcLengthBetweenInternal(level, edge, t0, t1, partialTickForRender);
    }

    public static SpanPath truncate(SpanPath path, double maxLength, Level level, double partialTickForRender) {
        if (path.segments().isEmpty() || maxLength <= 0.0) {
            return new SpanPath(List.of(), 0.0);
        }
        if (path.totalLength() <= maxLength + 1.0E-9) {
            return path;
        }
        ArrayList<PathSegment> out = new ArrayList<PathSegment>();
        double walked = 0.0;
        for (PathSegment segment : path.segments()) {
            double segLen = CoasterPathGraphSpans.arcLengthBetweenInternal(level, segment.edge(), segment.tStart(), segment.tEnd(), partialTickForRender);
            if (walked + segLen <= maxLength + 1.0E-9) {
                out.add(segment);
                walked += segLen;
                continue;
            }
            double remaining = maxLength - walked;
            if (remaining <= 1.0E-9) break;
            double frac = segLen < 1.0E-9 ? 0.0 : remaining / segLen;
            double cutT = Mth.lerp((double)frac, (double)segment.tStart(), (double)segment.tEnd());
            out.add(new PathSegment(segment.edge(), segment.tStart(), cutT));
            walked = maxLength;
            break;
        }
        return new SpanPath(List.copyOf(out), walked);
    }

    public static Vec3 samplePositionAtArcLength(Level level, SpanPath path, double arcLength, double partialTickForRender) {
        CoasterPathTrackFrame.GraphHit hit = CoasterPathGraphSpans.sampleGraphHitAtArcLength(level, path, arcLength, partialTickForRender);
        return hit == null ? Vec3.ZERO : hit.point();
    }

    @Nullable
    public static CoasterPathTrackFrame.GraphHit sampleGraphHitAtArcLength(Level level, SpanPath path, double arcLength, double partialTickForRender) {
        if (path.segments().isEmpty()) {
            return null;
        }
        double target = Mth.clamp((double)arcLength, (double)0.0, (double)path.totalLength());
        double walked = 0.0;
        for (PathSegment segment : path.segments()) {
            double segLen = CoasterPathGraphSpans.arcLengthBetweenInternal(level, segment.edge(), segment.tStart(), segment.tEnd(), partialTickForRender);
            if (walked + segLen >= target - 1.0E-9) {
                double local = target - walked;
                double frac = segLen < 1.0E-9 ? 0.0 : local / segLen;
                double t = Mth.lerp((double)frac, (double)segment.tStart(), (double)segment.tEnd());
                Vec3 point = CoasterPathGraphSpans.samplePositionForArcParameter(level, segment.edge(), t, partialTickForRender);
                return new CoasterPathTrackFrame.GraphHit(point, segment.edge(), t);
            }
            walked += segLen;
        }
        PathSegment last = path.segments().getLast();
        Vec3 point = CoasterPathGraphSpans.samplePositionForArcParameter(level, last.edge(), last.tEnd(), partialTickForRender);
        return new CoasterPathTrackFrame.GraphHit(point, last.edge(), last.tEnd());
    }

    @Nullable
    private static SpanPath resolveSpanPathBetween(Level level, List<CoasterPathEdge> edges, int startEdgeIdx, double startT, int endEdgeIdx, double endT, double partialTickForRender) {
        CoasterPathEdge startEdge = edges.get(startEdgeIdx);
        CoasterPathEdge endEdge = edges.get(endEdgeIdx);
        if (startEdgeIdx == endEdgeIdx) {
            double len = CoasterPathGraphSpans.arcLengthBetweenInternal(level, startEdge, startT, endT, partialTickForRender);
            return new SpanPath(List.of(new PathSegment(startEdge, startT, endT)), len);
        }
        BlockPos startAnchor = CoasterPathGraphSpans.nodeAtT(startEdge, startT);
        double startExitT = startT <= 0.5 ? 1.0 : 0.0;
        BlockPos startExitNode = CoasterPathGraphSpans.nodeAtT(startEdge, startExitT);
        double baseDist = CoasterPathGraphSpans.arcLengthBetweenInternal(level, startEdge, startT, startExitT, partialTickForRender);
        PriorityQueue<NodeDist> queue = new PriorityQueue<NodeDist>(Comparator.comparingDouble(NodeDist::distance));
        HashMap<BlockPos, Double> bestDist = new HashMap<BlockPos, Double>();
        HashMap<BlockPos, Arrival> cameFrom = new HashMap<BlockPos, Arrival>();
        queue.add(new NodeDist(startExitNode, baseDist));
        bestDist.put(startExitNode, baseDist);
        EndCandidate bestEnd = null;
        while (!queue.isEmpty()) {
            NodeDist current = queue.poll();
            double dist = current.distance();
            Double recorded = (Double)bestDist.get(current.node());
            if (recorded == null || dist > recorded + 1.0E-6) continue;
            for (int edgeIdx = 0; edgeIdx < edges.size(); ++edgeIdx) {
                double candidate;
                double exitT;
                double entryT;
                BlockPos nextNode;
                CoasterPathEdge edge = edges.get(edgeIdx);
                if (edge.from().equals((Object)edge.to())) continue;
                if (edge.from().equals((Object)current.node())) {
                    nextNode = edge.to();
                    entryT = 0.0;
                    exitT = 1.0;
                } else {
                    if (!edge.to().equals((Object)current.node())) continue;
                    nextNode = edge.from();
                    entryT = 1.0;
                    exitT = 0.0;
                }
                if (edgeIdx == startEdgeIdx && nextNode.equals((Object)startAnchor)) continue;
                if (edgeIdx == endEdgeIdx) {
                    candidate = dist + CoasterPathGraphSpans.arcLengthBetweenInternal(level, edge, entryT, endT, partialTickForRender);
                    if (bestEnd != null && !(candidate < bestEnd.distance())) continue;
                    bestEnd = new EndCandidate(candidate, current.node(), entryT);
                    continue;
                }
                candidate = dist + CoasterPathGraphSpans.arcLengthBetweenInternal(level, edge, entryT, exitT, partialTickForRender);
                Double prev = (Double)bestDist.get(nextNode);
                if (prev != null && !(candidate + 1.0E-6 < prev)) continue;
                bestDist.put(nextNode, candidate);
                cameFrom.put(nextNode, new Arrival(current.node(), edgeIdx, entryT, exitT));
                queue.add(new NodeDist(nextNode, candidate));
            }
        }
        if (bestEnd == null) {
            return null;
        }
        ArrayList<PathSegment> segments = new ArrayList<PathSegment>();
        segments.add(new PathSegment(endEdge, bestEnd.entryT(), endT));
        BlockPos node = bestEnd.entryNode();
        while (node != null && !node.equals((Object)startExitNode)) {
            Arrival arrival = (Arrival)cameFrom.get(node);
            if (arrival == null) {
                return null;
            }
            segments.add(0, new PathSegment(edges.get(arrival.edgeIdx()), arrival.entryT(), arrival.exitT()));
            node = arrival.prevNode();
        }
        segments.add(0, new PathSegment(startEdge, startT, startExitT));
        return new SpanPath(List.copyOf(segments), bestEnd.distance());
    }

    private static BlockPos nodeAtT(CoasterPathEdge edge, double t) {
        return t <= 0.5 ? edge.from() : edge.to();
    }

    private static double arcLengthBetweenInternal(Level level, CoasterPathEdge edge, double t0, double t1, @Nullable Double partialTickForRender) {
        double lo = Math.min(t0, t1);
        double hi = Math.max(t0, t1);
        if (hi - lo < 1.0E-9) {
            return 0.0;
        }
        int steps = Math.max(4, (int)Math.ceil((hi - lo) * 16.0));
        Vec3 prev = CoasterPathGraphSpans.samplePositionForArcParameter(level, edge, lo, partialTickForRender);
        double sum = 0.0;
        for (int i = 1; i <= steps; ++i) {
            double t = Mth.lerp((double)((double)i / (double)steps), (double)lo, (double)hi);
            Vec3 cur = CoasterPathGraphSpans.samplePositionForArcParameter(level, edge, t, partialTickForRender);
            sum += cur.distanceTo(prev);
            prev = cur;
        }
        return sum;
    }

    private static Vec3 samplePositionForArcParameter(Level level, CoasterPathEdge edge, double t, @Nullable Double partialTickForRender) {
        if (partialTickForRender != null && level.isClientSide() && CoasterPathGraphManager.edgeHostedOnSubLevel(level, edge)) {
            return CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, t);
        }
        return CoasterPathGraphManager.sampleEdgePosition(level, edge, t, partialTickForRender);
    }

    private static int indexOfEdge(List<CoasterPathEdge> edges, CoasterPathEdge target) {
        for (int i = 0; i < edges.size(); ++i) {
            if (edges.get(i) != target) continue;
            return i;
        }
        return -1;
    }

    public record SpanPath(List<PathSegment> segments, double totalLength) {
    }

    public record PathSegment(CoasterPathEdge edge, double tStart, double tEnd) {
    }

    private record NodeDist(BlockPos node, double distance) {
    }

    private record EndCandidate(double distance, BlockPos entryNode, double entryT) {
    }

    private record Arrival(BlockPos prevNode, int edgeIdx, double entryT, double exitT) {
    }

    public static final class SpanArcSampler {
        private final SpanPath path;
        private final double[] cumulativeEnds;

        private SpanArcSampler(SpanPath path, double[] cumulativeEnds) {
            this.path = path;
            this.cumulativeEnds = cumulativeEnds;
        }

        public static SpanArcSampler of(Level level, SpanPath path, double partialTickForRender) {
            return SpanArcSampler.of(level, path, (Double)partialTickForRender);
        }

        public static SpanArcSampler of(Level level, SpanPath path, @Nullable Double partialTickForRender) {
            List<PathSegment> segments = path.segments();
            double[] cumulative = new double[segments.size() + 1];
            for (int i = 0; i < segments.size(); ++i) {
                PathSegment segment = segments.get(i);
                cumulative[i + 1] = cumulative[i] + CoasterPathGraphSpans.arcLengthBetweenInternal(level, segment.edge(), segment.tStart(), segment.tEnd(), partialTickForRender);
            }
            return new SpanArcSampler(path, cumulative);
        }

        public SpanPath path() {
            return this.path;
        }

        public double totalLength() {
            return this.cumulativeEnds.length == 0 ? 0.0 : this.cumulativeEnds[this.cumulativeEnds.length - 1];
        }

        @Nullable
        public CoasterPathTrackFrame.GraphHit sampleGraphHit(Level level, double arcLength, double partialTickForRender) {
            return this.sampleGraphHit(level, arcLength, (Double)partialTickForRender);
        }

        @Nullable
        public CoasterPathTrackFrame.GraphHit sampleGraphHit(Level level, double arcLength, @Nullable Double partialTickForRender) {
            if (this.path.segments().isEmpty()) {
                return null;
            }
            double target = Mth.clamp((double)arcLength, (double)0.0, (double)this.totalLength());
            int segIdx = this.segmentIndexAt(target);
            PathSegment segment = this.path.segments().get(segIdx);
            double segStart = this.cumulativeEnds[segIdx];
            double segLen = this.cumulativeEnds[segIdx + 1] - segStart;
            double frac = segLen < 1.0E-9 ? 0.0 : (target - segStart) / segLen;
            double t = Mth.lerp((double)frac, (double)segment.tStart(), (double)segment.tEnd());
            Vec3 point = CoasterPathGraphSpans.samplePositionForArcParameter(level, segment.edge(), t, partialTickForRender);
            return new CoasterPathTrackFrame.GraphHit(point, segment.edge(), t);
        }

        private int segmentIndexAt(double arcLength) {
            int lo = 0;
            int hi = this.path.segments().size() - 1;
            while (lo < hi) {
                int mid = lo + hi + 1 >>> 1;
                if (this.cumulativeEnds[mid] <= arcLength + 1.0E-9) {
                    lo = mid;
                    continue;
                }
                hi = mid - 1;
            }
            return lo;
        }
    }
}

