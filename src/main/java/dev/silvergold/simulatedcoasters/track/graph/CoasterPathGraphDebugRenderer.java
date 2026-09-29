package dev.silvergold.simulatedcoasters.track.graph;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CoasterPathGraphDebugRenderer {
   private static final int DEFAULT_NODE = 3776749;
   private static final int HIGHLIGHT_NODE = 15129460;
   private static final int TRACE_ENDPOINT_MARKER = 16724735;
   private static List<Vec3> lastContinuityTraceEndpointWorld = List.of();
   private static int lastContinuityHighlightEdgeCount;

   private CoasterPathGraphDebugRenderer() {
   }

   public static void clearContinuityTraceState() {
      lastContinuityTraceEndpointWorld = List.of();
      lastContinuityHighlightEdgeCount = 0;
   }

   public static List<Vec3> getLastContinuityTraceEndpointWorldPositions() {
      return lastContinuityTraceEndpointWorld;
   }

   public static int getLastContinuityHighlightEdgeCount() {
      return lastContinuityHighlightEdgeCount;
   }

   public static void render(Level level, CoasterPathGraph graph) {
      Minecraft mc = Minecraft.getInstance();
      Entity cameraEntity = mc.cameraEntity;
      if (cameraEntity != null) {
         Vec3 camera = cameraEntity.getEyePosition();
         double drawDist = 96.0;
         BlockPos lookTrack = crosshairCoasterAnchorPos(mc, level);
         Set<Integer> highlightEdges = lookTrack == null ? Set.of() : edgeIndicesConnectedToBlock(level, graph, lookTrack);
         lastContinuityHighlightEdgeCount = highlightEdges.size();
         lastContinuityTraceEndpointWorld = CoasterPathTraceEndpoints.pathEndpointWorldPositions(level, graph, highlightEdges);
         Set<BlockPos> highlightEndpoints = endpointsOfEdges(graph, highlightEdges);

         for (int ei = 0; ei < lastContinuityTraceEndpointWorld.size(); ei++) {
            Vec3 c = lastContinuityTraceEndpointWorld.get(ei);
            if (!(c.distanceTo(camera) > drawDist)) {
               double s = 0.22;
               Outliner.getInstance().showLine(Pair.of("coaster_trace_ep_a", ei), c.add(-s, 0.0, 0.0), c.add(s, 0.0, 0.0)).colored(16724735).lineWidth(0.125F);
               Outliner.getInstance().showLine(Pair.of("coaster_trace_ep_b", ei), c.add(0.0, -s, 0.0), c.add(0.0, s, 0.0)).colored(16724735).lineWidth(0.125F);
               Outliner.getInstance().showLine(Pair.of("coaster_trace_ep_c", ei), c.add(0.0, 0.0, -s), c.add(0.0, 0.0, s)).colored(16724735).lineWidth(0.125F);
            }
         }

         for (CoasterPathNode node : graph.nodes()) {
            Vec3 p = CoasterPathGraphManager.sampleNodePosition(level, node);
            if (!(p.distanceTo(camera) > drawDist)) {
               boolean hi = highlightEndpoints.contains(node.pos());
               Vec3 n = CoasterPathGraphManager.sublevelWorldDirection(level, node.pos(), node.normal()).normalize();
               Outliner.getInstance()
                  .showLine(Pair.of("coaster_node_normal", node.pos()), p, p.add(n.scale(0.45)))
                  .colored(hi ? 15129460 : 3776749)
                  .lineWidth(0.0625F);
            }
         }
      }
   }

   @Nullable
   private static BlockPos crosshairCoasterAnchorPos(Minecraft mc, Level level) {
      HitResult hit = mc.hitResult;
      if (hit instanceof BlockHitResult bhr && hit.getType() == Type.BLOCK) {
         BlockPos pos = bhr.getBlockPos();
         if (!(level.getBlockState(pos).getBlock() instanceof CoasterAnchorpointBlock)) {
            return null;
         }

         return pos;
      }

      return null;
   }

   private static Set<Integer> edgeIndicesConnectedToBlock(Level level, CoasterPathGraph graph, BlockPos block) {
      List<CoasterPathEdge> edges = graph.edges();
      if (edges.isEmpty()) {
         return Set.of();
      } else {
         Map<BlockPos, List<Integer>> posToEdgeIndices = new HashMap<>();

         for (int i = 0; i < edges.size(); i++) {
            CoasterPathEdge e = edges.get(i);
            posToEdgeIndices.computeIfAbsent(e.from(), k -> new ArrayList<>()).add(i);
            if (!e.from().equals(e.to())) {
               posToEdgeIndices.computeIfAbsent(e.to(), k -> new ArrayList<>()).add(i);
            }
         }

         ArrayDeque<Integer> q = new ArrayDeque<>();
         boolean[] seen = new boolean[edges.size()];

         for (int ix = 0; ix < edges.size(); ix++) {
            if (edgeTouchesBlock(level, edges.get(ix), block) && !seen[ix]) {
               seen[ix] = true;
               q.addLast(ix);
            }
         }

         if (q.isEmpty()) {
            return Set.of();
         } else {
            while (!q.isEmpty()) {
               int ixx = q.removeFirst();
               CoasterPathEdge e = edges.get(ixx);

               for (BlockPos end : List.of(e.from(), e.to())) {
                  List<Integer> neigh = posToEdgeIndices.get(end);
                  if (neigh != null) {
                     for (int j : neigh) {
                        if (!seen[j]) {
                           seen[j] = true;
                           q.addLast(j);
                        }
                     }
                  }
               }
            }

            Set<Integer> out = new HashSet<>();

            for (int ixx = 0; ixx < seen.length; ixx++) {
               if (seen[ixx]) {
                  out.add(ixx);
               }
            }

            return out;
         }
      }
   }

   private static Set<BlockPos> endpointsOfEdges(CoasterPathGraph graph, Set<Integer> edgeIndices) {
      Set<BlockPos> out = new HashSet<>();
      List<CoasterPathEdge> edges = graph.edges();

      for (int i : edgeIndices) {
         if (i >= 0 && i < edges.size()) {
            CoasterPathEdge e = edges.get(i);
            out.add(e.from());
            out.add(e.to());
         }
      }

      return out;
   }

   private static boolean edgeTouchesBlock(Level level, CoasterPathEdge edge, BlockPos pos) {
      if (!edge.from().equals(pos) && !edge.to().equals(pos)) {
         if (edge.bezier() != null) {
            for (int s = 0; s <= 24; s++) {
               Vec3 v = CoasterPathGraphManager.sampleEdgePosition(level, edge, (double)s / 24.0);
               if (BlockPos.containing(v).equals(pos)) {
                  return true;
               }
            }

            return false;
         } else {
            for (int sx = 0; sx <= 8; sx++) {
               Vec3 v = CoasterPathGraphManager.sampleEdgePosition(level, edge, (double)sx / 8.0);
               if (BlockPos.containing(v).equals(pos)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return true;
      }
   }
}
