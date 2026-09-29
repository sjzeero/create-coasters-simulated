package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class CoasterPathGraphManager {
   public static final Vec3 PATH_CENTER_OFFSET = new Vec3(0.0, 0.0, 0.0);
   private static final Set<ResourceKey<Level>> SERVER_TANGENT_ALIGNED = new HashSet<>();

   public static Vec3 pathCenterlineWorld(Vec3 rawBezierWorld) {
      return rawBezierWorld.add(PATH_CENTER_OFFSET);
   }

   public static Vec3 coasterBezierParametricSpineRaw(BezierConnection bc, double t) {
      float tf = (float)Mth.clamp(t, 0.0, 1.0);
      Vec3 e1 = (Vec3)bc.starts.getFirst();
      Vec3 e2 = (Vec3)bc.starts.getSecond();
      Vec3 c1 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 0);
      Vec3 c2 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 1);
      return VecHelper.bezier(e1, e2, c1, c2, tf);
   }

   public static boolean isCoasterGraphBezier(BezierConnection bc) {
      return bc != null && bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id);
   }

   private CoasterPathGraphManager() {
   }

   public static void invalidateServerTangentAlignment(ResourceKey<Level> dim) {
      SERVER_TANGENT_ALIGNED.remove(dim);
   }

   public static void markServerTangentAlignmentStaleResolved(ResourceKey<Level> dim) {
      SERVER_TANGENT_ALIGNED.add(dim);
   }

   public static Vec3 sublevelWorldPosition(Level level, BlockPos anchor, Vec3 plotLocal, @Nullable Double partialTickForRender) {
      SubLevel sub = Sable.HELPER.getContaining(level, anchor);
      if (sub == null) {
         return plotLocal;
      } else if (partialTickForRender != null && level.isClientSide()) {
         Pose3d vis = new Pose3d();
         sub.lastPose().lerp(sub.logicalPose(), partialTickForRender, vis);
         return vis.transformPosition(plotLocal);
      } else {
         return sub.logicalPose().transformPosition(plotLocal);
      }
   }

   public static Vec3 sublevelWorldPosition(Level level, BlockPos anchor, Vec3 plotLocal) {
      return sublevelWorldPosition(level, anchor, plotLocal, null);
   }

   public static Vec3 sublevelWorldDirection(Level level, BlockPos anchor, Vec3 plotLocalDir, @Nullable Double partialTickForRender) {
      SubLevel sub = Sable.HELPER.getContaining(level, anchor);
      if (sub == null) {
         return plotLocalDir;
      } else {
         Vector3d v = new Vector3d(plotLocalDir.x, plotLocalDir.y, plotLocalDir.z);
         if (partialTickForRender != null && level.isClientSide()) {
            Pose3d vis = new Pose3d();
            sub.lastPose().lerp(sub.logicalPose(), partialTickForRender, vis);
            vis.orientation().transform(v);
         } else {
            sub.logicalPose().orientation().transform(v);
         }

         return new Vec3(v.x, v.y, v.z);
      }
   }

   public static Vec3 sublevelWorldDirection(Level level, BlockPos anchor, Vec3 plotLocalDir) {
      return sublevelWorldDirection(level, anchor, plotLocalDir, null);
   }

   public static boolean anchorBlockIsOnSubLevel(Level level, BlockPos anchor) {
      return Sable.HELPER.getContaining(level, anchor) != null;
   }

   public static boolean edgeHostedOnSubLevel(Level level, CoasterPathEdge edge) {
      return anchorBlockIsOnSubLevel(level, edge.from()) || anchorBlockIsOnSubLevel(level, edge.to());
   }

   public static BlockPos edgeSubLevelAnchor(Level level, CoasterPathEdge edge) {
      if (anchorBlockIsOnSubLevel(level, edge.from())) {
         return edge.from();
      } else {
         return anchorBlockIsOnSubLevel(level, edge.to()) ? edge.to() : edge.from();
      }
   }

   @Nullable
   public static Vec3 bearingWorldInTrackHostPlotLocal(Level level, CoasterPathEdge edge, Vec3 bearingWorld) {
      if (!edgeHostedOnSubLevel(level, edge)) {
         return null;
      } else {
         BlockPos anchor = edgeSubLevelAnchor(level, edge);
         SubLevel trackHost = Sable.HELPER.getContaining(level, anchor);
         if (trackHost == null) {
            return null;
         } else {
            Vector3d local = trackHost.logicalPose().transformPositionInverse(new Vector3d(bearingWorld.x, bearingWorld.y, bearingWorld.z), new Vector3d());
            return new Vec3(local.x, local.y, local.z);
         }
      }
   }

   public static Vec3 trackHostPlotLocalToWorld(Level level, CoasterPathEdge edge, Vec3 plotLocal) {
      BlockPos anchor = edgeSubLevelAnchor(level, edge);
      return sublevelWorldPosition(level, anchor, plotLocal);
   }

   public static CoasterPathGraph get(ServerLevel level) {
      CoasterPathGraph g = CoasterPathSavedData.get(level.getServer()).graphFor(level.dimension());
      ensureServerTangentAligned(level, g);
      return g;
   }

   private static void ensureServerTangentAligned(ServerLevel level, CoasterPathGraph g) {
      if (!g.edges().isEmpty()) {
         ResourceKey<Level> dim = level.dimension();
         if (!SERVER_TANGENT_ALIGNED.contains(dim)) {
            CoasterPathTangentOrientation.alignConnectedComponents(level, g);
            SERVER_TANGENT_ALIGNED.add(dim);
         }
      }
   }

   public static Vec3 sampleNodePosition(Level level, CoasterPathNode node) {
      return sampleNodePosition(level, node, null);
   }

   public static Vec3 sampleNodePosition(Level level, CoasterPathNode node, @Nullable Double partialTickForRender) {
      BlockPos pos = node.pos();
      Vec3 local = sampleNodePositionPlotLocal(level, node);
      return sublevelWorldPosition(level, pos, local, partialTickForRender);
   }

   public static Vec3 sampleNodePositionPlotLocal(Level level, CoasterPathNode node) {
      BlockPos pos = node.pos();
      BlockState st = level.getBlockState(pos);
      return st.getBlock() instanceof CoasterAnchorpointBlock
         ? CoasterAnchorpointBlockEntity.worldCenter(level, pos)
            .add(PATH_CENTER_OFFSET)
            .add(CoasterAnchorpointBlockEntity.anchorLiftDirection(level, pos).scale(0.0625))
         : Vec3.atBottomCenterOf(pos).add(PATH_CENTER_OFFSET);
   }

   public static Vec3 sampleEdgePosition(Level level, CoasterPathEdge edge, double t) {
      return sampleEdgePosition(level, edge, t, null);
   }

   public static Vec3 sampleEdgePositionForNearestQuery(Level level, CoasterPathEdge edge, double t) {
      return sampleEdgePositionForNearestQuery(level, edge, t, null);
   }

   public static Vec3 sampleEdgePositionForNearestQuery(Level level, CoasterPathEdge edge, double t, @Nullable Double partialTickForRender) {
      if (edgeHostedOnSubLevel(level, edge)) {
         return sampleEdgePosition(level, edge, t, partialTickForRender);
      } else {
         BlockPos anchor = edgeSubLevelAnchor(level, edge);
         if (edge.bezier() != null) {
            BezierConnection bc = edge.bezier();
            if (isCoasterGraphBezier(bc)) {
               Vec3 local = coasterBezierPlotLocalCenterline(level, edge, t);
               return sublevelWorldPosition(level, anchor, local, partialTickForRender);
            } else {
               return sublevelWorldPosition(level, anchor, bc.getPosition(t).add(PATH_CENTER_OFFSET), partialTickForRender);
            }
         } else if (edge.straightFromWorld() != null && edge.straightToWorld() != null) {
            return sublevelWorldPosition(level, anchor, edge.straightFromWorld().lerp(edge.straightToWorld(), t).add(PATH_CENTER_OFFSET), partialTickForRender);
         } else {
            Vec3 from = sampleNodePosition(level, new CoasterPathNode(edge.from(), Vec3.ZERO), partialTickForRender);
            Vec3 to = sampleNodePosition(level, new CoasterPathNode(edge.to(), Vec3.ZERO), partialTickForRender);
            return from.lerp(to, t);
         }
      }
   }

   public static boolean isOpenCurveEnd(Level level, CoasterPathEdge edge, boolean atFromEndpoint) {
      BezierConnection bc = edge.bezier();
      if (bc != null && isCoasterGraphBezier(bc)) {
         BlockPos anchor = atFromEndpoint ? edge.from() : edge.to();
         return level.getBlockEntity(anchor) instanceof CoasterAnchorpointBlockEntity be ? be.legCount() < 2 : false;
      } else {
         return false;
      }
   }

   public static double openEndExtensionTSpan(Level level, CoasterPathEdge edge, boolean atFromEndpoint) {
      if (!isOpenCurveEnd(level, edge, atFromEndpoint)) {
         return 0.0;
      } else {
         BezierConnection bc = edge.bezier();
         if (bc == null) {
            return 0.0;
         } else {
            Vec3 a = pathCenterlineWorld(coasterBezierParametricSpineRaw(bc, 0.0));
            Vec3 b = pathCenterlineWorld(coasterBezierParametricSpineRaw(bc, 1.0));
            double chord = a.distanceTo(b);
            if (chord < 1.0E-8) {
               return 1.0;
            } else {
               BlockPos anchor = atFromEndpoint ? edge.from() : edge.to();
               return (double)CoasterOpenEndExtension.extensionBlocks(level, anchor) / chord;
            }
         }
      }
   }

   @Deprecated
   public static double openEndExtensionTSpan(Level level, CoasterPathEdge edge) {
      return Math.max(openEndExtensionTSpan(level, edge, true), openEndExtensionTSpan(level, edge, false));
   }

   public static Vec3 coasterBezierPlotLocalTangent(BezierConnection bc, double t) {
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

      Vec3 pM = pathCenterlineWorld(coasterBezierParametricSpineRaw(bc, tm));
      Vec3 pP = pathCenterlineWorld(coasterBezierParametricSpineRaw(bc, tp));
      Vec3 d = pP.subtract(pM);
      return d.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : d;
   }

   public static Vec3 plotLocalCenterlineTangentAlongIncreasingT(Level level, CoasterPathEdge edge, double t) {
      BezierConnection bc = edge.bezier();
      if (bc != null && isCoasterGraphBezier(bc)) {
         double[] range = coasterBezierSampleTRange(level, edge);
         double tLo = range[0];
         double tHi = range[1];
         double dt = 1.0E-4;
         double tm = t - dt;
         double tp = t + dt;
         if (tm < tLo) {
            tm = tLo;
            tp = Math.min(tHi, tLo + 2.0 * dt);
         } else if (tp > tHi) {
            tp = tHi;
            tm = Math.max(tLo, tHi - 2.0 * dt);
         }

         Vec3 pM = coasterBezierPlotLocalCenterline(level, edge, tm);
         Vec3 pP = coasterBezierPlotLocalCenterline(level, edge, tp);
         Vec3 d = pP.subtract(pM);
         if (d.lengthSqr() < 1.0E-14) {
            Vec3 fallback = coasterBezierPlotLocalTangent(bc, Mth.clamp(t, 0.0, 1.0));
            return fallback.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : fallback;
         } else {
            return d;
         }
      } else {
         return new Vec3(0.0, 0.0, 1.0);
      }
   }

   public static double[] coasterBezierSampleTRange(Level level, CoasterPathEdge edge) {
      CoasterBezierEdgeSamplingContext ctx = CoasterBezierEdgeSamplingContext.forCoasterEdge(level, edge);
      return ctx != null ? ctx.sampleTRange() : new double[]{0.0, 1.0};
   }

   private static Vec3 coasterBezierPlotLocalCenterline(Level level, CoasterPathEdge edge, double t) {
      CoasterBezierEdgeSamplingContext ctx = CoasterBezierEdgeSamplingContext.forCoasterEdge(level, edge);
      if (ctx != null) {
         return ctx.plotLocalCenterline(t);
      } else {
         BezierConnection bc = edge.bezier();
         return bc == null ? Vec3.ZERO : pathCenterlineWorld(coasterBezierParametricSpineRaw(bc, Mth.clamp(t, 0.0, 1.0)));
      }
   }

   public static Vec3 sampleEdgePosition(Level level, CoasterPathEdge edge, double t, @Nullable Double partialTickForRender) {
      BlockPos anchor = edgeSubLevelAnchor(level, edge);
      return sublevelWorldPosition(level, anchor, sampleEdgePositionPlotLocal(level, edge, t), partialTickForRender);
   }

   public static Vec3 sampleEdgePositionPlotLocal(Level level, CoasterPathEdge edge, double t) {
      return sampleEdgePositionPlotLocal(level, edge, t, null);
   }

   public static Vec3 sampleEdgePositionPlotLocal(Level level, CoasterPathEdge edge, double t, @Nullable CoasterBezierEdgeSamplingContext bezierContext) {
      if (edge.bezier() != null) {
         BezierConnection bc = edge.bezier();
         if (isCoasterGraphBezier(bc)) {
            float faceT = (float)Mth.clamp(t, 0.0, 1.0);
            Vec3 faceUp = CoasterBezierRailFrames.faceUpAt(bc, faceT, level);
            Vec3 centerline = bezierContext != null ? bezierContext.plotLocalCenterline(t) : coasterBezierPlotLocalCenterline(level, edge, t);
            return centerline.add(faceUp.scale(0.0625));
         } else {
            return bc.getPosition(t).add(PATH_CENTER_OFFSET);
         }
      } else if (edge.straightFromWorld() != null && edge.straightToWorld() != null) {
         return edge.straightFromWorld().lerp(edge.straightToWorld(), t).add(PATH_CENTER_OFFSET);
      } else {
         Vec3 from = sampleNodePositionPlotLocal(level, new CoasterPathNode(edge.from(), Vec3.ZERO));
         Vec3 to = sampleNodePositionPlotLocal(level, new CoasterPathNode(edge.to(), Vec3.ZERO));
         return from.lerp(to, t);
      }
   }

   public static List<CoasterPathEdge> edgesForBezier(CoasterPathGraph graph, BezierConnection bc) {
      BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
      BlockPos a = (BlockPos)primary.bePositions.getFirst();
      BlockPos b = (BlockPos)primary.bePositions.getSecond();
      List<CoasterPathEdge> out = new ArrayList<>();

      for (CoasterPathEdge e : graph.edges()) {
         if (e.bezier() != null) {
            Couple<BlockPos> bp = (e.bezier().isPrimary() ? e.bezier() : e.bezier().secondary()).bePositions;
            BlockPos ea = (BlockPos)bp.getFirst();
            BlockPos eb = (BlockPos)bp.getSecond();
            if (ea.equals(a) && eb.equals(b) || ea.equals(b) && eb.equals(a)) {
               out.add(e);
            }
         }
      }

      return out;
   }
}
