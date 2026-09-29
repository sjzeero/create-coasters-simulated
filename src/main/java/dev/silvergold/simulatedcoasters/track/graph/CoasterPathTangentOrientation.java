/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.graph;

import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterPathTangentOrientation {
    private static final double DT = 0.001;
    private static final double ENDPOINT_JOIN_EPS_SQ = 0.0484;
    private static final int ENDPOINT_BUCKETS_PER_BLOCK = 16;

    private CoasterPathTangentOrientation() {
    }

    public static void alignConnectedComponents(Level level, CoasterPathGraph graph) {
        CoasterPathTangentOrientation.alignComponentsTouchingRegion(level, graph, null);
    }

    public static void alignComponentsTouchingRegion(Level level, CoasterPathGraph graph, @Nullable Set<BlockPos> region) {
        List<CoasterPathEdge> list = graph.edges();
        int n = list.size();
        if (n == 0) {
            return;
        }
        Map<BlockPos, List<Integer>> adj = CoasterPathTangentOrientation.buildAdjacency(list);
        List<EndpointSample> endpoints = CoasterPathTangentOrientation.collectEndpointSamples(level, list);
        Map<Long, List<EndpointSample>> endpointBuckets = CoasterPathTangentOrientation.bucketizeEndpoints(endpoints);
        int[] flip = new int[n];
        for (int i = 0; i < n; ++i) {
            flip[i] = list.get(i).tangentSign();
        }
        boolean[] componentAssigned = new boolean[n];
        for (int start = 0; start < n; ++start) {
            if (componentAssigned[start]) continue;
            ArrayList<Integer> component = new ArrayList<Integer>();
            boolean touchesRegion = region == null;
            ArrayDeque<Integer> componentQ = new ArrayDeque<Integer>();
            componentQ.addLast(start);
            componentAssigned[start] = true;
            while (!componentQ.isEmpty()) {
                int idx = (Integer)componentQ.removeFirst();
                component.add(idx);
                CoasterPathEdge e = list.get(idx);
                if (region != null && (region.contains(e.from()) || region.contains(e.to()))) {
                    touchesRegion = true;
                }
                for (BlockPos v : CoasterPathTangentOrientation.endpoints(e)) {
                    List<Integer> neigh = adj.get(v);
                    if (neigh == null) continue;
                    for (int j : neigh) {
                        if (componentAssigned[j]) continue;
                        componentAssigned[j] = true;
                        componentQ.addLast(j);
                    }
                }
            }
            if (!touchesRegion) continue;
            CoasterPathTangentOrientation.orientComponentEdges(level, list, component, adj, endpointBuckets, flip);
        }
        for (int i = 0; i < n; ++i) {
            list.set(i, list.get(i).withTangentSign(flip[i]));
        }
    }

    private static Map<BlockPos, List<Integer>> buildAdjacency(List<CoasterPathEdge> list) {
        HashMap<BlockPos, List<Integer>> adj = new HashMap<BlockPos, List<Integer>>();
        for (int i = 0; i < list.size(); ++i) {
            CoasterPathEdge e = list.get(i);
            CoasterPathTangentOrientation.addAdj(adj, e.from(), i);
            if (e.from().equals((Object)e.to())) continue;
            CoasterPathTangentOrientation.addAdj(adj, e.to(), i);
        }
        return adj;
    }

    private static void orientComponentEdges(Level level, List<CoasterPathEdge> list, List<Integer> component, Map<BlockPos, List<Integer>> adj, Map<Long, List<EndpointSample>> endpointBuckets, int[] flip) {
        if (component.isEmpty()) {
            return;
        }
        HashSet<Integer> componentSet = new HashSet<Integer>(component);
        boolean[] vis = new boolean[list.size()];
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        int start = component.get(0);
        vis[start] = true;
        flip[start] = 1;
        q.addLast(start);
        while (!q.isEmpty()) {
            int cur = (Integer)q.removeFirst();
            CoasterPathEdge e = list.get(cur);
            for (BlockPos v : CoasterPathTangentOrientation.endpoints(e)) {
                Vec3 pCur = v.equals((Object)e.from()) ? CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0) : CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0);
                Vec3 tCur = CoasterPathTangentOrientation.tangentAlongIncreasingT(level, e, v);
                CoasterPathTangentOrientation.propagateToNeighborsAtVertex(level, list, cur, flip, vis, q, tCur, adj.get(v), componentSet);
                CoasterPathTangentOrientation.propagateToNearbyEndpointSamples(level, list, cur, flip, vis, q, pCur, tCur, endpointBuckets, componentSet);
            }
        }
    }

    private static void propagateToNeighborsAtVertex(Level level, List<CoasterPathEdge> list, int cur, int[] flip, boolean[] vis, ArrayDeque<Integer> q, Vec3 tCur, @Nullable List<Integer> neigh, Set<Integer> componentSet) {
        if (neigh == null || tCur == null || tCur.lengthSqr() < 1.0E-12) {
            return;
        }
        for (int j : neigh) {
            int want;
            CoasterPathEdge eJ;
            Vec3 tJ;
            if (j == cur || !componentSet.contains(j) || (tJ = (eJ = list.get(j)).from().equals((Object)eJ.to()) ? CoasterPathTangentOrientation.tangentAtEndpoint(level, eJ, true) : (eJ.from().equals((Object)list.get(cur).from()) || eJ.from().equals((Object)list.get(cur).to()) ? CoasterPathTangentOrientation.tangentAtEndpoint(level, eJ, true) : CoasterPathTangentOrientation.tangentAtEndpoint(level, eJ, false))) == null || tJ.lengthSqr() < 1.0E-12) continue;
            int n = want = tCur.dot(tJ) < 0.0 ? -flip[cur] : flip[cur];
            if (vis[j]) continue;
            vis[j] = true;
            flip[j] = want;
            q.addLast(j);
        }
    }

    private static void propagateToNearbyEndpointSamples(Level level, List<CoasterPathEdge> list, int cur, int[] flip, boolean[] vis, ArrayDeque<Integer> q, Vec3 pCur, Vec3 tCur, Map<Long, List<EndpointSample>> buckets, Set<Integer> componentSet) {
        if (pCur == null || tCur == null || tCur.lengthSqr() < 1.0E-12) {
            return;
        }
        int qx = CoasterPathTangentOrientation.quantize(pCur.x);
        int qy = CoasterPathTangentOrientation.quantize(pCur.y);
        int qz = CoasterPathTangentOrientation.quantize(pCur.z);
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dy = -1; dy <= 1; ++dy) {
                for (int dz = -1; dz <= 1; ++dz) {
                    List<EndpointSample> bucket = buckets.get(CoasterPathTangentOrientation.endpointKey(qx + dx, qy + dy, qz + dz));
                    if (bucket == null) continue;
                    for (EndpointSample s : bucket) {
                        int want;
                        Vec3 tJ;
                        if (s.edgeIndex() == cur || !componentSet.contains(s.edgeIndex()) || s.pos().distanceToSqr(pCur) > 0.0484 || (tJ = CoasterPathTangentOrientation.tangentAtEndpoint(level, list.get(s.edgeIndex()), s.atFrom())) == null || tJ.lengthSqr() < 1.0E-12) continue;
                        int n = want = tCur.dot(tJ) < 0.0 ? -flip[cur] : flip[cur];
                        if (vis[s.edgeIndex()]) continue;
                        vis[s.edgeIndex()] = true;
                        flip[s.edgeIndex()] = want;
                        q.addLast(s.edgeIndex());
                    }
                }
            }
        }
    }

    private static List<EndpointSample> collectEndpointSamples(Level level, List<CoasterPathEdge> edges) {
        ArrayList<EndpointSample> out = new ArrayList<EndpointSample>(edges.size() * 2);
        for (int i = 0; i < edges.size(); ++i) {
            CoasterPathEdge e = edges.get(i);
            out.add(new EndpointSample(i, true, CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0)));
            out.add(new EndpointSample(i, false, CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0)));
        }
        return out;
    }

    private static Map<Long, List<EndpointSample>> bucketizeEndpoints(List<EndpointSample> endpoints) {
        HashMap<Long, List<EndpointSample>> map = new HashMap<Long, List<EndpointSample>>();
        for (EndpointSample s : endpoints) {
            int x = CoasterPathTangentOrientation.quantize(s.pos().x);
            int y = CoasterPathTangentOrientation.quantize(s.pos().y);
            int z = CoasterPathTangentOrientation.quantize(s.pos().z);
            map.computeIfAbsent(CoasterPathTangentOrientation.endpointKey(x, y, z), k -> new ArrayList()).add(s);
        }
        return map;
    }

    private static int quantize(double v) {
        return (int)Math.floor(v * 16.0);
    }

    private static long endpointKey(int x, int y, int z) {
        long xx = (long)x & 0x1FFFFFL;
        long yy = (long)y & 0x1FFFFFL;
        long zz = (long)z & 0x1FFFFFL;
        return xx << 42 | yy << 21 | zz;
    }

    private static void addAdj(Map<BlockPos, List<Integer>> adj, BlockPos p, int idx) {
        adj.computeIfAbsent(p.immutable(), k -> new ArrayList()).add(idx);
    }

    private static List<BlockPos> endpoints(CoasterPathEdge e) {
        if (e.from().equals((Object)e.to())) {
            return List.of(e.from());
        }
        return List.of(e.from(), e.to());
    }

    private static Vec3 tangentAlongIncreasingT(Level level, CoasterPathEdge edge, BlockPos v) {
        Vec3 straightDir = CoasterPathTangentOrientation.straightDirection(level, edge);
        if (straightDir != null) {
            return straightDir;
        }
        if (v.equals((Object)edge.from())) {
            return CoasterPathTangentOrientation.tangentAtEndpoint(level, edge, true);
        }
        if (v.equals((Object)edge.to())) {
            return CoasterPathTangentOrientation.tangentAtEndpoint(level, edge, false);
        }
        return null;
    }

    private static Vec3 tangentAtEndpoint(Level level, CoasterPathEdge edge, boolean atFrom) {
        Vec3 straightDir = CoasterPathTangentOrientation.straightDirection(level, edge);
        if (straightDir != null) {
            return straightDir;
        }
        if (atFrom) {
            if (CoasterPathGraphManager.isOpenCurveEnd(level, edge, true)) {
                double ext = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, true);
                double t0 = -Math.min(0.001, ext);
                return CoasterPathTangentOrientation.diff(level, edge, t0, 0.0);
            }
            double t1 = Math.min(1.0, 0.001);
            return CoasterPathTangentOrientation.diff(level, edge, 0.0, t1);
        }
        if (CoasterPathGraphManager.isOpenCurveEnd(level, edge, false)) {
            double ext = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, false);
            double t1 = Math.min(1.001, 1.0 + ext);
            return CoasterPathTangentOrientation.diff(level, edge, 1.0, t1);
        }
        double t0 = Math.max(0.0, 0.999);
        return CoasterPathTangentOrientation.diff(level, edge, t0, 1.0);
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

    private record EndpointSample(int edgeIndex, boolean atFrom, Vec3 pos) {
    }
}

