package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CoasterPathGraph {
   private final Map<BlockPos, CoasterPathNode> nodes = new HashMap<>();
   private final List<CoasterPathEdge> edges = new ArrayList<>();
   private final Map<Long, Set<BlockPos>> nodesByChunk = new HashMap<>();
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
      return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
   }

   private void indexNode(BlockPos pos) {
      this.nodesByChunk.computeIfAbsent(chunkKey(pos), k -> new HashSet<>()).add(pos.immutable());
   }

   private void unindexNode(BlockPos pos) {
      long key = chunkKey(pos);
      Set<BlockPos> set = this.nodesByChunk.get(key);
      if (set != null) {
         set.remove(pos);
         if (set.isEmpty()) {
            this.nodesByChunk.remove(key);
         }
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
      if (inChunk != null && !inChunk.isEmpty()) {
         Set<BlockPos> ghosts = null;

         for (BlockPos pos : inChunk) {
            if (!(level.getBlockEntity(pos) instanceof CoasterAnchorpointBlockEntity)) {
               if (ghosts == null) {
                  ghosts = new HashSet<>();
               }

               ghosts.add(pos);
            }
         }

         if (ghosts != null && !ghosts.isEmpty()) {
            this.removeEdgesTouching(ghosts);
            this.removeNodesIn(ghosts);
            this.pruneOrphanNodes();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void addEdge(CoasterPathEdge edge) {
      this.edges.add(edge);
      this.invalidateEdgeSpatialCache();
   }

   private void invalidateEdgeSpatialCache() {
      this.edgeCenterlineBounds = null;
      this.edgeCenterlineBoundsPoseDependent = null;
      this.edgeCenterlineBoundsPoseDependentTick = Long.MIN_VALUE;
      this.revision++;
   }

   private boolean graphIncludesSubLevelAnchoredEdges(Level level) {
      for (CoasterPathEdge e : this.edges) {
         if (CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, e.from()) || CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, e.to())) {
            return true;
         }
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
      for (int i = 0; i < this.edges.size(); i++) {
         CoasterPathEdge candidate = this.edges.get(i);
         if (candidate == edge) {
            return i;
         }
      }

      for (int ix = 0; ix < this.edges.size(); ix++) {
         CoasterPathEdge candidate = this.edges.get(ix);
         if (candidate.from().equals(edge.from()) && candidate.to().equals(edge.to())) {
            return ix;
         }
      }

      return -1;
   }

   public boolean edgesAreSameOrDirectNeighbors(int idxA, int idxB) {
      if (idxA < 0 || idxB < 0) {
         return false;
      } else if (idxA == idxB) {
         return true;
      } else {
         return idxA < this.edges.size() && idxB < this.edges.size() ? edgesShareAnchorEndpoint(this.edges.get(idxA), this.edges.get(idxB)) : false;
      }
   }

   private static boolean edgesShareAnchorEndpoint(CoasterPathEdge a, CoasterPathEdge b) {
      BlockPos af = a.from();
      BlockPos at = a.to();
      BlockPos bf = b.from();
      BlockPos bt = b.to();
      return af.equals(bf) || af.equals(bt) || at.equals(bf) || at.equals(bt);
   }

   public List<double[]> ensureEdgeCenterlineBounds(Level level) {
      boolean poseDependent = this.graphIncludesSubLevelAnchoredEdges(level);
      if (!poseDependent && this.edgeCenterlineBounds != null && this.edgeCenterlineBounds.size() == this.edges.size()) {
         return this.edgeCenterlineBounds;
      } else {
         if (poseDependent) {
            long tick = level.getGameTime();
            if (this.edgeCenterlineBoundsPoseDependent != null
               && tick == this.edgeCenterlineBoundsPoseDependentTick
               && this.edgeCenterlineBoundsPoseDependent.size() == this.edges.size()) {
               return this.edgeCenterlineBoundsPoseDependent;
            }
         }

         List<double[]> list = new ArrayList<>(this.edges.size());

         for (CoasterPathEdge edge : this.edges) {
            list.add(computeCenterlineAabb(level, edge));
         }

         if (!poseDependent) {
            this.edgeCenterlineBounds = list;
         } else {
            this.edgeCenterlineBoundsPoseDependentTick = level.getGameTime();
            this.edgeCenterlineBoundsPoseDependent = list;
         }

         return list;
      }
   }

   private static double[] computeCenterlineAabb(Level level, CoasterPathEdge edge) {
      double[] b = new double[]{
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.POSITIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY,
         Double.NEGATIVE_INFINITY
      };
      if (edge.bezier() != null) {
         if (CoasterPathGraphManager.isCoasterGraphBezier(edge.bezier())) {
            double[] tRange = CoasterPathGraphManager.coasterBezierSampleTRange(level, edge);

            for (int s = 0; s <= 8; s++) {
               double t = Mth.lerp((double)s / 8.0, tRange[0], tRange[1]);
               expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, t));
            }
         } else {
            for (int s = 0; s <= 8; s++) {
               expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, (double)s / 8.0));
            }
         }
      } else if (edge.straightFromWorld() != null && edge.straightToWorld() != null) {
         expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, 0.0));
         expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, 1.0));
      } else {
         expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, 0.0));
         expand(b, CoasterPathGraphManager.sampleEdgePosition(level, edge, 1.0));
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
         if (this.nodes.remove(p) != null) {
            this.unindexNode(p);
         }
      }
   }

   public void removeEdgesTouching(Set<BlockPos> region) {
      boolean removed = this.edges.removeIf(e -> region.contains(e.from()) || region.contains(e.to()));
      if (removed) {
         this.invalidateEdgeSpatialCache();
      }
   }

   public void removeEdgesTouchingEndpointsIn(BoundingBox bbox) {
      boolean removed = this.edges.removeIf(e -> bbox.isInside(e.from()) || bbox.isInside(e.to()));
      if (removed) {
         this.invalidateEdgeSpatialCache();
      }

      this.pruneOrphanNodes();
   }

   public void pruneOrphanNodes() {
      Set<BlockPos> used = new HashSet<>();

      for (CoasterPathEdge e : this.edges) {
         used.add(e.from());
         used.add(e.to());
      }

      this.nodes.keySet().removeIf(p -> {
         if (used.contains(p)) {
            return false;
         } else {
            this.unindexNode(p);
            return true;
         }
      });
   }

   public CompoundTag write(Provider registries) {
      ListTag nodeList = new ListTag();

      for (CoasterPathNode n : this.nodes.values()) {
         CompoundTag t = new CompoundTag();
         t.put("Pos", NbtUtils.writeBlockPos(n.pos()));
         t.put("Normal", VecHelper.writeNBT(n.normal()));
         nodeList.add(t);
      }

      ListTag edgeList = new ListTag();

      for (CoasterPathEdge e : this.edges) {
         CompoundTag t = new CompoundTag();
         t.put("From", NbtUtils.writeBlockPos(e.from()));
         t.put("To", NbtUtils.writeBlockPos(e.to()));
         t.putDouble("Roll0", e.rollStart());
         t.putDouble("Roll1", e.rollEnd());
         t.putInt("TangentSign", e.tangentSign());
         if (e.bezier() != null) {
            t.putBoolean("Curve", true);
            t.put("Bezier", e.bezier().write(BlockPos.ZERO));
         } else {
            t.putBoolean("Curve", false);
            if (e.straightFromWorld() != null && e.straightToWorld() != null) {
               t.put("StraightA", VecHelper.writeNBT(e.straightFromWorld()));
               t.put("StraightB", VecHelper.writeNBT(e.straightToWorld()));
            }
         }

         edgeList.add(t);
      }

      CompoundTag out = new CompoundTag();
      out.put("Nodes", nodeList);
      out.put("Edges", edgeList);
      return out;
   }

   public static CoasterPathGraph read(CompoundTag tag, Provider registries) {
      CoasterPathGraph g = new CoasterPathGraph();
      ListTag nodeList = tag.getList("Nodes", 10);

      for (int i = 0; i < nodeList.size(); i++) {
         CompoundTag nt = nodeList.getCompound(i);
         BlockPos pos = (BlockPos)NbtUtils.readBlockPos(nt, "Pos").orElseThrow();
         Vec3 normal = VecHelper.readNBT(nt.getList("Normal", 6));
         g.nodes.put(pos, new CoasterPathNode(pos, normal));
      }

      ListTag edgeList = tag.getList("Edges", 10);

      for (int i = 0; i < edgeList.size(); i++) {
         CompoundTag et = edgeList.getCompound(i);
         BlockPos from = (BlockPos)NbtUtils.readBlockPos(et, "From").orElseThrow();
         BlockPos to = (BlockPos)NbtUtils.readBlockPos(et, "To").orElseThrow();
         double r0 = et.getDouble("Roll0");
         double r1 = et.getDouble("Roll1");
         int tangentSign = et.contains("TangentSign", 3) ? et.getInt("TangentSign") : 1;
         if (et.getBoolean("Curve")) {
            BezierConnection bc = new BezierConnection(et.getCompound("Bezier"), BlockPos.ZERO);
            g.edges.add(new CoasterPathEdge(from, to, null, null, bc, r0, r1, tangentSign));
         } else if (et.contains("StraightA", 9) && et.contains("StraightB", 9)) {
            Vec3 a = VecHelper.readNBT(et.getList("StraightA", 6));
            Vec3 b = VecHelper.readNBT(et.getList("StraightB", 6));
            g.edges.add(new CoasterPathEdge(from, to, a, b, null, 0.0, 0.0, tangentSign));
         } else {
            g.edges.add(new CoasterPathEdge(from, to, null, null, null, r0, r1, tangentSign));
         }
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
