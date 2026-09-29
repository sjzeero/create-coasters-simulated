package dev.silvergold.simulatedcoasters.track.cart;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphLookup;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class CoasterCartMidTrackPlacement {
   private CoasterCartMidTrackPlacement() {
   }

   public static Vec3 plotOriginForGraphHit(Level level, CoasterPathTrackFrame.GraphHit hit, Quaterniond orientation, @Nullable Double partialTickForRender) {
      Vec3 bearingCenter = partialTickForRender != null
         ? CoasterPathTrackFrame.pathAnchorForSnappedBlock(level, hit, partialTickForRender.floatValue())
         : CoasterPathTrackFrame.pathAnchorForSnappedBlock(level, hit);
      Vector3d centerInPlot = new Vector3d(0.5, 0.5, 0.5);
      orientation.transform(centerInPlot);
      return bearingCenter.subtract(centerInPlot.x, centerInPlot.y, centerInPlot.z);
   }

   @Nullable
   public static Vec3 plotOriginFromPlacedCart(SubLevel cart, Pose3d pose) {
      Vec3 bearingWorld = CoasterCartLinkPlacement.bearingCenterWorld(cart, pose);
      return bearingWorld == null ? null : CoasterCartLinkPlacement.plotOriginForBearingCenter(bearingWorld, pose.orientation());
   }

   @Nullable
   public static CoasterCartMidTrackPlacement.PlacementPose resolveMidSpan(
      Level level, Player player, Vec3 lookDirection, @Nullable Double partialTickForRender, @Nullable BlockPos rasterHint
   ) {
      if (rasterHint != null) {
         CoasterCartMidTrackPlacement.PlacementPose pose = resolveFromRaster(level, player, rasterHint, lookDirection, partialTickForRender);
         if (pose != null) {
            return pose;
         } else {
            return isAcceptedAnchor(level, rasterHint) ? resolveFromAnchor(level, player, rasterHint, lookDirection, null, partialTickForRender) : null;
         }
      } else {
         return resolveFromCoasterCurveLook(level, player, lookDirection, partialTickForRender);
      }
   }

   @Nullable
   public static CoasterCartMidTrackPlacement.PlacementPose resolveForCurve(
      Level level, Player player, BezierConnection hoveredCurve, Vec3 lookDirection, @Nullable Double partialTickForRender
   ) {
      CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
      return resolveOnEdges(level, player, edgesForCurve(graph, hoveredCurve), lookDirection, partialTickForRender);
   }

   @Nullable
   public static CoasterCartMidTrackPlacement.PlacementPose resolveFromCoasterCurveLook(
      Level level, Player player, Vec3 lookDirection, @Nullable Double partialTickForRender
   ) {
      CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
      return resolveOnEdges(level, player, coasterMaterialEdges(graph), lookDirection, partialTickForRender);
   }

   @Nullable
   public static CoasterCartMidTrackPlacement.PlacementPose resolveFromRaster(
      Level level, Player player, BlockPos rasterCell, Vec3 lookDirection, @Nullable Double partialTickForRender
   ) {
      CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
      return resolveOnEdges(level, player, edgesCoveringFakeRaster(graph, rasterCell), lookDirection, partialTickForRender);
   }

   @Nullable
   private static CoasterCartMidTrackPlacement.PlacementPose resolveOnEdges(
      Level level, Player player, List<CoasterPathEdge> edges, Vec3 lookDirection, @Nullable Double partialTickForRender
   ) {
      if (edges.isEmpty()) {
         return null;
      } else {
         float partialTick = partialTickForRender != null ? partialTickForRender.floatValue() : 1.0F;
         Vec3 eye = player.getEyePosition(partialTick);
         Vec3 look = lookDirection.lengthSqr() < 1.0E-12 ? player.getLookAngle() : lookDirection.normalize();
         CoasterPathTrackFrame.GraphHit hit = nearestGraphHitOnEdgesNearRay(level, edges, eye, look, player, partialTick);
         if (hit == null) {
            return null;
         } else {
            CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender);
            Quaterniond orientation = CoasterPathTrackFrame.quaternionFromTrackBasis(basis);
            Vec3 plotOrigin = plotOriginForGraphHit(level, hit, orientation, partialTickForRender);
            return new CoasterCartMidTrackPlacement.PlacementPose(hit, plotOrigin, orientation);
         }
      }
   }

   public static boolean isAcceptedAnchor(Level level, BlockPos anchorPos) {
      return level.getBlockState(anchorPos).getBlock() instanceof CoasterAnchorpointBlock
         && level.getBlockEntity(anchorPos) instanceof CoasterAnchorpointBlockEntity be
         && be.legCount() > 0;
   }

   @Nullable
   public static CoasterCartMidTrackPlacement.PlacementPose resolveFromAnchor(
      Level level, Player player, BlockPos anchorPos, Vec3 lookDirection, @Nullable Vec3 clickLocation, @Nullable Double partialTickForRender
   ) {
      if (!isAcceptedAnchor(level, anchorPos)) {
         return null;
      } else {
         CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
         List<CoasterPathEdge> edges = edgesTouchingBlock(graph, anchorPos);
         if (edges.isEmpty()) {
            return null;
         } else {
            float partialTick = partialTickForRender != null ? partialTickForRender.floatValue() : 1.0F;
            Vec3 eye = player.getEyePosition(partialTick);
            Vec3 look = lookDirection.lengthSqr() < 1.0E-12 ? player.getLookAngle() : lookDirection.normalize();
            CoasterPathTrackFrame.GraphHit hit = nearestGraphHitOnEdgesNearRay(level, edges, eye, look, player, partialTick);
            if (hit == null && clickLocation != null) {
               Vec3 probe = graphProbeWorld(level, anchorPos, clickLocation);
               hit = CoasterPathTrackFrame.nearestGraphHit(level, edges, probe, 9216.0, partialTickForRender);
            }

            if (hit == null) {
               return null;
            } else {
               CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender);
               Quaterniond orientation = CoasterPathTrackFrame.quaternionFromTrackBasis(basis);
               Vec3 plotOrigin = plotOriginForGraphHit(level, hit, orientation, partialTickForRender);
               return new CoasterCartMidTrackPlacement.PlacementPose(hit, plotOrigin, orientation);
            }
         }
      }
   }

   public static boolean isCoasterMidSpanRaster(Level level, BlockPos raster) {
      if (!AnchorPeerFakeTracks.isManagedFakeTrack(level, raster)) {
         return false;
      } else {
         for (BezierConnection bc : AnchorPeerFakeTracks.hostedPrimaryCoasterCurvesContainingCell(level, raster)) {
            BezierConnection p = bc.isPrimary() ? bc : bc.secondary();
            if (p.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
               return true;
            }
         }

         return false;
      }
   }

   private static List<CoasterPathEdge> coasterMaterialEdges(CoasterPathGraph graph) {
      List<CoasterPathEdge> out = new ArrayList<>();

      for (CoasterPathEdge e : graph.edges()) {
         if (e.bezier() != null && e.bezier().getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
            out.add(e);
         }
      }

      return out;
   }

   private static List<CoasterPathEdge> edgesCoveringFakeRaster(CoasterPathGraph graph, BlockPos raster) {
      List<CoasterPathEdge> out = new ArrayList<>();
      Vec3 rasterCenter = Vec3.atCenterOf(raster);

      for (CoasterPathEdge e : graph.edges()) {
         if (e.bezier() != null) {
            BezierConnection primary = e.bezier().isPrimary() ? e.bezier() : e.bezier().secondary();
            if (primary.getBounds().inflate(2.0).contains(rasterCenter) && AnchorPeerFakeTracks.rasterContains(e.bezier(), raster)) {
               out.add(e);
            }
         }
      }

      return out;
   }

   private static List<CoasterPathEdge> edgesForCurve(CoasterPathGraph graph, BezierConnection bc) {
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

   private static List<CoasterPathEdge> edgesTouchingBlock(CoasterPathGraph graph, BlockPos pos) {
      List<CoasterPathEdge> out = new ArrayList<>();

      for (CoasterPathEdge e : graph.edges()) {
         if (e.from().equals(pos) || e.to().equals(pos)) {
            out.add(e);
         } else if (e.bezier() != null) {
            Couple<BlockPos> bp = e.bezier().bePositions;
            if (((BlockPos)bp.getFirst()).equals(pos) || ((BlockPos)bp.getSecond()).equals(pos)) {
               out.add(e);
            }
         }
      }

      return out;
   }

   private static Vec3 graphProbeWorld(Level level, BlockPos anchorPos, @Nullable Vec3 clickLocation) {
      if (clickLocation == null) {
         return CoasterAnchorpointBlockEntity.worldCenter(level, anchorPos);
      } else {
         SubLevel host = Sable.HELPER.getContaining(level, anchorPos);
         return host == null ? clickLocation : host.logicalPose().transformPosition(clickLocation);
      }
   }

   private static double distPointToRaySq(Vec3 p, Vec3 rayOrigin, Vec3 rayDirUnit) {
      Vec3 v = p.subtract(rayOrigin);
      double along = v.dot(rayDirUnit);
      if (along < 0.0) {
         return Double.MAX_VALUE;
      } else {
         Vec3 closestOnRay = rayOrigin.add(rayDirUnit.scale(along));
         return p.distanceToSqr(closestOnRay);
      }
   }

   @Nullable
   private static CoasterPathTrackFrame.GraphHit nearestGraphHitOnEdgesNearRay(
      Level level, List<CoasterPathEdge> edges, Vec3 eye, Vec3 lookDirUnit, Player player, float partialTick
   ) {
      if (edges.isEmpty()) {
         return null;
      } else {
         double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
         Double pt = (double)partialTick;
         int samples = 96;
         double bestLateralSq = Double.MAX_VALUE;
         double lookAlong = -Double.MAX_VALUE;
         CoasterPathTrackFrame.GraphHit lookHit = null;

         for (CoasterPathEdge edge : edges) {
            for (int i = 0; i <= 96; i++) {
               double t = (double)i / 96.0;
               Vec3 sample = CoasterPathGraphManager.sampleEdgePosition(level, edge, t, pt);
               double dRay = distPointToRaySq(sample, eye, lookDirUnit);
               double along = sample.subtract(eye).dot(lookDirUnit);
               if (!(along < -0.25)) {
                  if (dRay < bestLateralSq - 1.0E-9) {
                     bestLateralSq = dRay;
                     lookAlong = along;
                     lookHit = new CoasterPathTrackFrame.GraphHit(sample, edge, t);
                  } else if (lookHit != null) {
                     double lateralBand = Math.max(0.04, bestLateralSq * 0.01);
                     if (dRay <= bestLateralSq + lateralBand && along > lookAlong) {
                        lookAlong = along;
                        lookHit = new CoasterPathTrackFrame.GraphHit(sample, edge, t);
                     }
                  }
               }
            }
         }

         return lookHit != null && !(bestLateralSq >= 9.0) && isWithinBlockInteractionReach(eye, lookHit.point(), reach) ? lookHit : null;
      }
   }

   private static boolean isWithinBlockInteractionReach(Vec3 eye, Vec3 target, double reach) {
      double limit = reach + 0.5;
      return eye.distanceToSqr(target) <= limit * limit;
   }

   public static record PlacementPose(CoasterPathTrackFrame.GraphHit graphHit, Vec3 plotOriginWorld, Quaterniond orientation) {
   }
}
