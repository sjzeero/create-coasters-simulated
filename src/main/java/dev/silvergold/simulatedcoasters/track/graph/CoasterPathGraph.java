/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.createmod.catnip.math.VecHelper
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.ChunkPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.levelgen.structure.BoundingBox
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CoasterPathGraph {
    private final Map<BlockPos, CoasterPathNode> nodes = new HashMap<BlockPos, CoasterPathNode>();
    private final List<CoasterPathEdge> edges = new ArrayList<CoasterPathEdge>();
    private final Map<Long, Set<BlockPos>> nodesByChunk = new HashMap<Long, Set<BlockPos>>();
    @Nullable
    private List<double[]> edgeCenterlineBounds;
    private long edgeCenterlineBoundsPoseDependentTick = Long.MIN_VALUE;
    @Nullable
    private List<double[]> edgeCenterlineBoundsPoseDependent;
    private int revision;

    public int revision() {
        return this.revision;
    }

    public void clear() {
        this.nodes.clear();
        this.edges.clear();
        this.nodesByChunk.clear();
        this.invalidateEdgeSpatialCache();
    }

    public void upsertNode(CoasterPathNode node) {
        BlockPos pos = node.pos();
        CoasterPathNode previous = this.nodes.put(pos, node);
        if (previous == null) {
            this.indexNode(pos);
        }
    }

    private static long chunkKey(BlockPos pos) {
        return ChunkPos.asLong((int)(pos.getX() >> 4), (int)(pos.getZ() >> 4));
    }

    private void indexNode(BlockPos pos) {
        this.nodesByChunk.computeIfAbsent(CoasterPathGraph.chunkKey(pos), k -> new HashSet()).add(pos.immutable());
    }

    private void unindexNode(BlockPos pos) {
        long key = CoasterPathGraph.chunkKey(pos);
        Set<BlockPos> set = this.nodesByChunk.get(key);
        if (set == null) {
            return;
        }
        set.remove(pos);
        if (set.isEmpty()) {
            this.nodesByChunk.remove(key);
        }
    }

    private void rebuildChunkIndex() {
        this.nodesByChunk.clear();
        for (BlockPos pos : this.nodes.keySet()) {
            this.indexNode(pos);
        }
    }

    public boolean removeGhostNodesMissingAnchors(ServerLevel level, ChunkPos chunk) {
        Set<BlockPos> inChunk = this.nodesByChunk.get(chunk.toLong());
        if (inChunk == null || inChunk.isEmpty()) {
            return false;
        }
        HashSet<BlockPos> ghosts = null;
        for (BlockPos pos : inChunk) {
            if (level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity) continue;
            if (ghosts == null) {
                ghosts = new HashSet<BlockPos>();
            }
            ghosts.add(pos);
        }
        if (ghosts == null || ghosts.isEmpty()) {
            return false;
        }
        this.removeEdgesTouching((Set<BlockPos>)ghosts);
        this.removeNodesIn((Set<BlockPos>)ghosts);
        this.pruneOrphanNodes();
        return true;
    }

    public void addEdge(CoasterPathEdge edge) {
        this.edges.add(edge);
        this.invalidateEdgeSpatialCache();
    }

    private void invalidateEdgeSpatialCache() {
        this.edgeCenterlineBounds = null;
        this.edgeCenterlineBoundsPoseDependent = null;
        this.edgeCenterlineBoundsPoseDependentTick = Long.MIN_VALUE;
        ++this.revision;
    }

    private boolean graphIncludesSubLevelAnchoredEdges(Level level) {
        for (CoasterPathEdge e : this.edges) {
            if (!CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, e.from()) && !CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, e.to())) continue;
            return true;
        }
        return false;
    }

    public Collection<CoasterPathNode> nodes() {
        return this.nodes.values();
    }

    public List<CoasterPathEdge> edges() {
        return this.edges;
    }

    public int indexOfEdge(CoasterPathEdge edge) {
        CoasterPathEdge candidate;
        int i;
        for (i = 0; i < this.edges.size(); ++i) {
            candidate = this.edges.get(i);
            if (candidate != edge) continue;
            return i;
        }
        for (i = 0; i < this.edges.size(); ++i) {
            candidate = this.edges.get(i);
            if (!candidate.from().equals((Object)edge.from()) || !candidate.to().equals((Object)edge.to())) continue;
            return i;
        }
        return -1;
    }

    public boolean edgesAreSameOrDirectNeighbors(int idxA, int idxB) {
        if (idxA < 0 || idxB < 0) {
            return false;
        }
        if (idxA == idxB) {
            return true;
        }
        if (idxA >= this.edges.size() || idxB >= this.edges.size()) {
            return false;
        }
        return CoasterPathGraph.edgesShareAnchorEndpoint(this.edges.get(idxA), this.edges.get(idxB));
    }

    private static boolean edgesShareAnchorEndpoint(CoasterPathEdge a, CoasterPathEdge b) {
        BlockPos af = a.from();
        BlockPos at = a.to();
        BlockPos bf = b.from();
        BlockPos bt = b.to();
        return af.equals((Object)bf) || af.equals((Object)bt) || at.equals((Object)bf) || at.equals((Object)bt);
    }

    public List<double[]> ensureEdgeCenterlineBounds(Level level) {
        boolean poseDependent = this.graphIncludesSubLevelAnchoredEdges(level);
        if (!poseDependent && this.edgeCenterlineBounds != null && this.edgeCenterlineBounds.size() == this.edges.size()) {
            return this.edgeCenterlineBounds;
        }
        if (poseDependent) {
            long tick = level.getGameTime();
            if (this.edgeCenterlineBoundsPoseDependent != null && tick == this.edgeCenterlineBoundsPoseDependentTick && this.edgeCenterlineBoundsPoseDependent.size() == this.edges.size()) {
                return this.edgeCenterlineBoundsPoseDependent;
            }
        }
        ArrayList<double[]> list = new ArrayList<double[]>(this.edges.size());
        for (CoasterPathEdge edge : this.edges) {
            list.add(CoasterPathGraph.computeCenterlineAabb(level, edge));
        }
        if (!poseDependent) {
            this.edgeCenterlineBounds = list;
        } else {
            this.edgeCenterlineBoundsPoseDependentTick = level.getGameTime();
            this.edgeCenterlineBoundsPoseDependent = list;
        }
        return list;
    }

    private static double[] computeCenterlineAabb(Level level, CoasterPathEdge edge) {
        double[] b = new double[]{Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        if (edge.bezier() != null) {
            if (CoasterPathGraphManager.isCoasterGraphBezier(edge.bezier())) {
                double[] tRange = CoasterPathGraphManager.coasterBezierSampleTRange(level, edge);
                for (int s = 0; s <= 8; ++s) {
                    double t = Mth.lerp((double)((double)s / 8.0), (double)tRange[0], (double)tRange[1]);
                    CoasterPathGraph.expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, t));
                }
            } else {
                for (int s = 0; s <= 8; ++s) {
                    CoasterPathGraph.expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, (double)s / 8.0));
                }
            }
        } else if (edge.straightFromWorld() != null && edge.straightToWorld() != null) {
            CoasterPathGraph.expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, 0.0));
            CoasterPathGraph.expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, 1.0));
        } else {
            CoasterPathGraph.expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, 0.0));
            CoasterPathGraph.expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, 1.0));
        }
        return b;
    }

    private static void expand(double[] b, Vec3 p) {
        b[0] = Math.min(b[0], p.x);
        b[1] = Math.min(b[1], p.y);
        b[2] = Math.min(b[2], p.z);
        b[3] = Math.max(b[3], p.x);
        b[4] = Math.max(b[4], p.y);
        b[5] = Math.max(b[5], p.z);
    }

    public void removeNodesIn(Set<BlockPos> region) {
        for (BlockPos p : region) {
            if (this.nodes.remove(p) == null) continue;
            this.unindexNode(p);
        }
    }

    public void removeEdgesTouching(Set<BlockPos> region) {
        boolean removed = this.edges.removeIf(e -> region.contains(e.from()) || region.contains(e.to()));
        if (removed) {
            this.invalidateEdgeSpatialCache();
        }
    }

    public void removeEdgesTouchingEndpointsIn(BoundingBox bbox) {
        boolean removed = this.edges.removeIf(e -> bbox.isInside((Vec3i)e.from()) || bbox.isInside((Vec3i)e.to()));
        if (removed) {
            this.invalidateEdgeSpatialCache();
        }
        this.pruneOrphanNodes();
    }

    public void pruneOrphanNodes() {
        HashSet<BlockPos> used = new HashSet<BlockPos>();
        for (CoasterPathEdge e : this.edges) {
            used.add(e.from());
            used.add(e.to());
        }
        this.nodes.keySet().removeIf(p -> {
            if (used.contains(p)) {
                return false;
            }
            this.unindexNode((BlockPos)p);
            return true;
        });
    }

    public CompoundTag write(HolderLookup.Provider registries) {
        ListTag nodeList = new ListTag();
        for (CoasterPathNode coasterPathNode : this.nodes.values()) {
            CompoundTag t = new CompoundTag();
            t.put("Pos", NbtUtils.writeBlockPos((BlockPos)coasterPathNode.pos()));
            t.put("Normal", (Tag)VecHelper.writeNBT((Vec3)coasterPathNode.normal()));
            nodeList.add((Object)t);
        }
        ListTag edgeList = new ListTag();
        for (CoasterPathEdge e : this.edges) {
            CompoundTag t = new CompoundTag();
            t.put("From", NbtUtils.writeBlockPos((BlockPos)e.from()));
            t.put("To", NbtUtils.writeBlockPos((BlockPos)e.to()));
            t.putDouble("Roll0", e.rollStart());
            t.putDouble("Roll1", e.rollEnd());
            t.putInt("TangentSign", e.tangentSign());
            if (e.bezier() != null) {
                t.putBoolean("Curve", true);
                t.put("Bezier", (Tag)e.bezier().write(BlockPos.ZERO));
            } else {
                t.putBoolean("Curve", false);
                if (e.straightFromWorld() != null && e.straightToWorld() != null) {
                    t.put("StraightA", (Tag)VecHelper.writeNBT((Vec3)e.straightFromWorld()));
                    t.put("StraightB", (Tag)VecHelper.writeNBT((Vec3)e.straightToWorld()));
                }
            }
            edgeList.add((Object)t);
        }
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put("Nodes", (Tag)nodeList);
        compoundTag.put("Edges", (Tag)edgeList);
        return compoundTag;
    }

    public static CoasterPathGraph read(CompoundTag tag, HolderLookup.Provider registries) {
        CoasterPathGraph g = new CoasterPathGraph();
        ListTag nodeList = tag.getList("Nodes", 10);
        for (int i = 0; i < nodeList.size(); ++i) {
            CompoundTag nt = nodeList.getCompound(i);
            BlockPos pos = (BlockPos)NbtUtils.readBlockPos((CompoundTag)nt, (String)"Pos").orElseThrow();
            Vec3 normal = VecHelper.readNBT((ListTag)nt.getList("Normal", 6));
            g.nodes.put(pos, new CoasterPathNode(pos, normal));
        }
        ListTag edgeList = tag.getList("Edges", 10);
        for (int i = 0; i < edgeList.size(); ++i) {
            int tangentSign;
            CompoundTag et = edgeList.getCompound(i);
            BlockPos from = (BlockPos)NbtUtils.readBlockPos((CompoundTag)et, (String)"From").orElseThrow();
            BlockPos to = (BlockPos)NbtUtils.readBlockPos((CompoundTag)et, (String)"To").orElseThrow();
            double r0 = et.getDouble("Roll0");
            double r1 = et.getDouble("Roll1");
            int n = tangentSign = et.contains("TangentSign", 3) ? et.getInt("TangentSign") : 1;
            if (et.getBoolean("Curve")) {
                BezierConnection bc = new BezierConnection(et.getCompound("Bezier"), BlockPos.ZERO);
                g.edges.add(new CoasterPathEdge(from, to, null, null, bc, r0, r1, tangentSign));
                continue;
            }
            if (et.contains("StraightA", 9) && et.contains("StraightB", 9)) {
                Vec3 a = VecHelper.readNBT((ListTag)et.getList("StraightA", 6));
                Vec3 b = VecHelper.readNBT((ListTag)et.getList("StraightB", 6));
                g.edges.add(new CoasterPathEdge(from, to, a, b, null, 0.0, 0.0, tangentSign));
                continue;
            }
            g.edges.add(new CoasterPathEdge(from, to, null, null, null, r0, r1, tangentSign));
        }
        g.rebuildChunkIndex();
        return g;
    }

    public void replaceFrom(CoasterPathGraph other) {
        this.clear();
        for (CoasterPathNode n : other.nodes.values()) {
            this.nodes.put(n.pos(), n);
        }
        this.edges.addAll(other.edges);
        this.rebuildChunkIndex();
    }
}

