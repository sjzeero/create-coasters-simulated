package dev.silvergold.simulatedcoasters.track.graph;

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
      alignComponentsTouchingRegion(level, graph, null);
   }

   public static void alignComponentsTouchingRegion(Level level, CoasterPathGraph graph, @Nullable Set<BlockPos> region) {
      List<CoasterPathEdge> list = graph.edges();
      int n = list.size();
      if (n != 0) {
         Map<BlockPos, List<Integer>> adj = buildAdjacency(list);
         List<CoasterPathTangentOrientation.EndpointSample> endpoints = collectEndpointSamples(level, list);
         Map<Long, List<CoasterPathTangentOrientation.EndpointSample>> endpointBuckets = bucketizeEndpoints(endpoints);
         int[] flip = new int[n];

         for (int i = 0; i < n; i++) {
            flip[i] = list.get(i).tangentSign();
         }

         boolean[] componentAssigned = new boolean[n];

         for (int start = 0; start < n; start++) {
            if (!componentAssigned[start]) {
               List<Integer> component = new ArrayList<>();
               boolean touchesRegion = region == null;
               ArrayDeque<Integer> componentQ = new ArrayDeque<>();
               componentQ.addLast(start);
               componentAssigned[start] = true;

               while (!componentQ.isEmpty()) {
                  int idx = componentQ.removeFirst();
                  component.add(idx);
                  CoasterPathEdge e = list.get(idx);
                  if (region != null && (region.contains(e.from()) || region.contains(e.to()))) {
                     touchesRegion = true;
                  }

                  for (BlockPos v : endpoints(e)) {
                     List<Integer> neigh = adj.get(v);
                     if (neigh != null) {
                        for (int j : neigh) {
                           if (!componentAssigned[j]) {
                              componentAssigned[j] = true;
                              componentQ.addLast(j);
                           }
                        }
                     }
                  }
               }

               if (touchesRegion) {
                  orientComponentEdges(level, list, component, adj, endpointBuckets, flip);
               }
            }
         }

         for (int i = 0; i < n; i++) {
            list.set(i, list.get(i).withTangentSign(flip[i]));
         }
      }
   }

   private static Map<BlockPos, List<Integer>> buildAdjacency(List<CoasterPathEdge> list) {
      Map<BlockPos, List<Integer>> adj = new HashMap<>();

      for (int i = 0; i < list.size(); i++) {
         CoasterPathEdge e = list.get(i);
         addAdj(adj, e.from(), i);
         if (!e.from().equals(e.to())) {
            addAdj(adj, e.to(), i);
         }
      }

      return adj;
   }

   private static void orientComponentEdges(
      Level level,
      List<CoasterPathEdge> list,
      List<Integer> component,
      Map<BlockPos, List<Integer>> adj,
      Map<Long, List<CoasterPathTangentOrientation.EndpointSample>> endpointBuckets,
      int[] flip
   ) {
      if (!component.isEmpty()) {
         Set<Integer> componentSet = new HashSet<>(component);
         boolean[] vis = new boolean[list.size()];
         ArrayDeque<Integer> q = new ArrayDeque<>();
         int start = component.get(0);
         vis[start] = true;
         flip[start] = 1;
         q.addLast(start);

         while (!q.isEmpty()) {
            int cur = q.removeFirst();
            CoasterPathEdge e = list.get(cur);

            for (BlockPos v : endpoints(e)) {
               Vec3 pCur = v.equals(e.from())
                  ? CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0)
                  : CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0);
               Vec3 tCur = tangentAlongIncreasingT(level, e, v);
               propagateToNeighborsAtVertex(level, list, cur, flip, vis, q, tCur, adj.get(v), componentSet);
               propagateToNearbyEndpointSamples(level, list, cur, flip, vis, q, pCur, tCur, endpointBuckets, componentSet);
            }
         }
      }
   }

   private static void propagateToNeighborsAtVertex(
      Level level,
      List<CoasterPathEdge> list,
      int cur,
      int[] flip,
      boolean[] vis,
      ArrayDeque<Integer> q,
      Vec3 tCur,
      @Nullable List<Integer> neigh,
      Set<Integer> componentSet
   ) {
      if (neigh != null && tCur != null && !(tCur.lengthSqr() < 1.0E-12)) {
         for (int j : neigh) {
            if (j != cur && componentSet.contains(j)) {
               CoasterPathEdge eJ = list.get(j);
               Vec3 tJ;
               if (eJ.from().equals(eJ.to())) {
                  tJ = tangentAtEndpoint(level, eJ, true);
               } else if (!eJ.from().equals(list.get(cur).from()) && !eJ.from().equals(list.get(cur).to())) {
                  tJ = tangentAtEndpoint(level, eJ, false);
               } else {
                  tJ = tangentAtEndpoint(level, eJ, true);
               }

               if (tJ != null && !(tJ.lengthSqr() < 1.0E-12)) {
                  int want = tCur.dot(tJ) < 0.0 ? -flip[cur] : flip[cur];
                  if (!vis[j]) {
                     vis[j] = true;
                     flip[j] = want;
                     q.addLast(j);
                  }
               }
            }
         }
      }
   }

   private static void propagateToNearbyEndpointSamples(
      Level level,
      List<CoasterPathEdge> list,
      int cur,
      int[] flip,
      boolean[] vis,
      ArrayDeque<Integer> q,
      Vec3 pCur,
      Vec3 tCur,
      Map<Long, List<CoasterPathTangentOrientation.EndpointSample>> buckets,
      Set<Integer> componentSet
   ) {
      if (pCur != null && tCur != null && !(tCur.lengthSqr() < 1.0E-12)) {
         int qx = quantize(pCur.x);
         int qy = quantize(pCur.y);
         int qz = quantize(pCur.z);

         for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
               for (int dz = -1; dz <= 1; dz++) {
                  List<CoasterPathTangentOrientation.EndpointSample> bucket = buckets.get(endpointKey(qx + dx, qy + dy, qz + dz));
                  if (bucket != null) {
                     for (CoasterPathTangentOrientation.EndpointSample s : bucket) {
                        if (s.edgeIndex() != cur && componentSet.contains(s.edgeIndex()) && !(s.pos().distanceToSqr(pCur) > 0.0484)) {
                           Vec3 tJ = tangentAtEndpoint(level, list.get(s.edgeIndex()), s.atFrom());
                           if (tJ != null && !(tJ.lengthSqr() < 1.0E-12)) {
                              int want = tCur.dot(tJ) < 0.0 ? -flip[cur] : flip[cur];
                              if (!vis[s.edgeIndex()]) {
                                 vis[s.edgeIndex()] = true;
                                 flip[s.edgeIndex()] = want;
                                 q.addLast(s.edgeIndex());
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static List<CoasterPathTangentOrientation.EndpointSample> collectEndpointSamples(Level level, List<CoasterPathEdge> edges) {
      List<CoasterPathTangentOrientation.EndpointSample> out = new ArrayList<>(edges.size() * 2);

      for (int i = 0; i < edges.size(); i++) {
         CoasterPathEdge e = edges.get(i);
         out.add(new CoasterPathTangentOrientation.EndpointSample(i, true, CoasterPathGraphManager.sampleEdgePosition(level, e, 0.0)));
         out.add(new CoasterPathTangentOrientation.EndpointSample(i, false, CoasterPathGraphManager.sampleEdgePosition(level, e, 1.0)));
      }

      return out;
   }

   private static Map<Long, List<CoasterPathTangentOrientation.EndpointSample>> bucketizeEndpoints(List<CoasterPathTangentOrientation.EndpointSample> endpoints) {
      Map<Long, List<CoasterPathTangentOrientation.EndpointSample>> map = new HashMap<>();

      for (CoasterPathTangentOrientation.EndpointSample s : endpoints) {
         int x = quantize(s.pos().x);
         int y = quantize(s.pos().y);
         int z = quantize(s.pos().z);
         map.computeIfAbsent(endpointKey(x, y, z), k -> new ArrayList<>()).add(s);
      }

      return map;
   }

   private static int quantize(double v) {
      return (int)Math.floor(v * 16.0);
   }

   private static long endpointKey(int x, int y, int z) {
      long xx = (long)x & 2097151L;
      long yy = (long)y & 2097151L;
      long zz = (long)z & 2097151L;
      return xx << 42 | yy << 21 | zz;
   }

   private static void addAdj(Map<BlockPos, List<Integer>> adj, BlockPos p, int idx) {
      adj.computeIfAbsent(p.immutable(), k -> new ArrayList<>()).add(idx);
   }

   private static List<BlockPos> endpoints(CoasterPathEdge e) {
      return e.from().equals(e.to()) ? List.of(e.from()) : List.of(e.from(), e.to());
   }

   private static Vec3 tangentAlongIncreasingT(Level level, CoasterPathEdge edge, BlockPos v) {
      Vec3 straightDir = straightDirection(level, edge);
      if (straightDir != null) {
         return straightDir;
      } else if (v.equals(edge.from())) {
         return tangentAtEndpoint(level, edge, true);
      } else {
         return v.equals(edge.to()) ? tangentAtEndpoint(level, edge, false) : null;
      }
   }

   private static Vec3 tangentAtEndpoint(Level level, CoasterPathEdge edge, boolean atFrom) {
      Vec3 straightDir = straightDirection(level, edge);
      if (straightDir != null) {
         return straightDir;
      } else if (atFrom) {
         if (CoasterPathGraphManager.isOpenCurveEnd(level, edge, true)) {
            double ext = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, true);
            double t0 = -Math.min(0.001, ext);
            return diff(level, edge, t0, 0.0);
         } else {
            double t1 = Math.min(1.0, 0.001);
            return diff(level, edge, 0.0, t1);
         }
      } else if (CoasterPathGraphManager.isOpenCurveEnd(level, edge, false)) {
         double ext = CoasterPathGraphManager.openEndExtensionTSpan(level, edge, false);
         double t1 = Math.min(1.001, 1.0 + ext);
         return diff(level, edge, 1.0, t1);
      } else {
         double t0 = Math.max(0.0, 0.999);
         return diff(level, edge, t0, 1.0);
      }
   }

   private static Vec3 diff(Level level, CoasterPathEdge edge, double t0, double t1) {
      Vec3 a = CoasterPathGraphManager.sampleEdgePosition(level, edge, t0);
      Vec3 b = CoasterPathGraphManager.sampleEdgePosition(level, edge, t1);
      Vec3 d = b.subtract(a);
      return d.lengthSqr() < 1.0E-14 ? Vec3.ZERO : d.normalize();
   }

   private static Vec3 straightDirection(Level level, CoasterPathEdge edge) {
      if (edge.straightFromWorld() != null && edge.straightToWorld() != null) {
         Vec3 d = edge.straightToWorld().subtract(edge.straightFromWorld());
         if (d.lengthSqr() < 1.0E-14) {
            return null;
         } else {
            d = CoasterPathGraphManager.sublevelWorldDirection(level, CoasterPathGraphManager.edgeSubLevelAnchor(level, edge), d);
            return d.lengthSqr() < 1.0E-14 ? null : d.normalize();
         }
      } else {
         return null;
      }
   }

   private static record EndpointSample(int edgeIndex, boolean atFrom, Vec3 pos) {
   }
}
