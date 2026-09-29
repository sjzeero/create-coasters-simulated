/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.createmod.catnip.data.Pair
 *  net.createmod.catnip.outliner.Outliner
 *  net.minecraft.client.Minecraft
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.graph;

import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathNode;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTraceEndpoints;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public class CoasterPathGraphDebugRenderer {
    private static final int DEFAULT_NODE = 3776749;
    private static final int HIGHLIGHT_NODE = 15129460;
    private static final int TRACE_ENDPOINT_MARKER = 0xFF32FF;
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
        if (cameraEntity == null) {
            return;
        }
        Vec3 camera = cameraEntity.getEyePosition();
        double drawDist = 96.0;
        BlockPos lookTrack = CoasterPathGraphDebugRenderer.crosshairCoasterAnchorPos(mc, level);
        Set<Integer> highlightEdges = lookTrack == null ? Set.of() : CoasterPathGraphDebugRenderer.edgeIndicesConnectedToBlock(level, graph, lookTrack);
        lastContinuityHighlightEdgeCount = highlightEdges.size();
        lastContinuityTraceEndpointWorld = CoasterPathTraceEndpoints.pathEndpointWorldPositions(level, graph, highlightEdges);
        Set<BlockPos> highlightEndpoints = CoasterPathGraphDebugRenderer.endpointsOfEdges(graph, highlightEdges);
        for (int ei = 0; ei < lastContinuityTraceEndpointWorld.size(); ++ei) {
            Vec3 c = lastContinuityTraceEndpointWorld.get(ei);
            if (c.distanceTo(camera) > drawDist) continue;
            double s = 0.22;
            Outliner.getInstance().showLine((Object)Pair.of((Object)"coaster_trace_ep_a", (Object)ei), c.add(-s, 0.0, 0.0), c.add(s, 0.0, 0.0)).colored(0xFF32FF).lineWidth(0.125f);
            Outliner.getInstance().showLine((Object)Pair.of((Object)"coaster_trace_ep_b", (Object)ei), c.add(0.0, -s, 0.0), c.add(0.0, s, 0.0)).colored(0xFF32FF).lineWidth(0.125f);
            Outliner.getInstance().showLine((Object)Pair.of((Object)"coaster_trace_ep_c", (Object)ei), c.add(0.0, 0.0, -s), c.add(0.0, 0.0, s)).colored(0xFF32FF).lineWidth(0.125f);
        }
        for (CoasterPathNode node : graph.nodes()) {
            Vec3 p = CoasterPathGraphManager.sampleNodePosition(level, node);
            if (p.distanceTo(camera) > drawDist) continue;
            boolean hi = highlightEndpoints.contains(node.pos());
            Vec3 n = CoasterPathGraphManager.sublevelWorldDirection(level, node.pos(), node.normal()).normalize();
            Outliner.getInstance().showLine((Object)Pair.of((Object)"coaster_node_normal", (Object)node.pos()), p, p.add(n.scale(0.45))).colored(hi ? 15129460 : 3776749).lineWidth(0.0625f);
        }
    }

    @Nullable
    private static BlockPos crosshairCoasterAnchorPos(Minecraft mc, Level level) {
        BlockHitResult bhr;
        block5: {
            block4: {
                HitResult hit = mc.hitResult;
                if (!(hit instanceof BlockHitResult)) break block4;
                bhr = (BlockHitResult)hit;
                if (hit.getType() == HitResult.Type.BLOCK) break block5;
            }
            return null;
        }
        BlockPos pos = bhr.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof CoasterAnchorpointBlock)) {
            return null;
        }
        return pos;
    }

    private static Set<Integer> edgeIndicesConnectedToBlock(Level level, CoasterPathGraph graph, BlockPos block) {
        int i;
        List<CoasterPathEdge> edges = graph.edges();
        if (edges.isEmpty()) {
            return Set.of();
        }
        HashMap<BlockPos, List> posToEdgeIndices = new HashMap<BlockPos, List>();
        for (int i2 = 0; i2 < edges.size(); ++i2) {
            CoasterPathEdge e = edges.get(i2);
            posToEdgeIndices.computeIfAbsent(e.from(), k -> new ArrayList()).add(i2);
            if (e.from().equals((Object)e.to())) continue;
            posToEdgeIndices.computeIfAbsent(e.to(), k -> new ArrayList()).add(i2);
        }
        ArrayDeque<Integer> q = new ArrayDeque<Integer>();
        boolean[] seen = new boolean[edges.size()];
        for (i = 0; i < edges.size(); ++i) {
            if (!CoasterPathGraphDebugRenderer.edgeTouchesBlock(level, edges.get(i), block) || seen[i]) continue;
            seen[i] = true;
            q.addLast(i);
        }
        if (q.isEmpty()) {
            return Set.of();
        }
        while (!q.isEmpty()) {
            i = (Integer)q.removeFirst();
            CoasterPathEdge e = edges.get(i);
            for (BlockPos end : List.of(e.from(), e.to())) {
                List neigh = (List)posToEdgeIndices.get(end);
                if (neigh == null) continue;
                Iterator iterator = neigh.iterator();
                while (iterator.hasNext()) {
                    int j = (Integer)iterator.next();
                    if (seen[j]) continue;
                    seen[j] = true;
                    q.addLast(j);
                }
            }
        }
        HashSet<Integer> out = new HashSet<Integer>();
        for (int i3 = 0; i3 < seen.length; ++i3) {
            if (!seen[i3]) continue;
            out.add(i3);
        }
        return out;
    }

    private static Set<BlockPos> endpointsOfEdges(CoasterPathGraph graph, Set<Integer> edgeIndices) {
        HashSet<BlockPos> out = new HashSet<BlockPos>();
        List<CoasterPathEdge> edges = graph.edges();
        for (int i : edgeIndices) {
            if (i < 0 || i >= edges.size()) continue;
            CoasterPathEdge e = edges.get(i);
            out.add(e.from());
            out.add(e.to());
        }
        return out;
    }

    private static boolean edgeTouchesBlock(Level level, CoasterPathEdge edge, BlockPos pos) {
        if (edge.from().equals((Object)pos) || edge.to().equals((Object)pos)) {
            return true;
        }
        if (edge.bezier() != null) {
            for (int s = 0; s <= 24; ++s) {
                Vec3 v = CoasterPathGraphManager.sampleEdgePosition(level, edge, (double)s / 24.0);
                if (!BlockPos.containing((Position)v).equals((Object)pos)) continue;
                return true;
            }
            return false;
        }
        for (int s = 0; s <= 8; ++s) {
            Vec3 v = CoasterPathGraphManager.sampleEdgePosition(level, edge, (double)s / 8.0);
            if (!BlockPos.containing((Position)v).equals((Object)pos)) continue;
            return true;
        }
        return false;
    }
}

