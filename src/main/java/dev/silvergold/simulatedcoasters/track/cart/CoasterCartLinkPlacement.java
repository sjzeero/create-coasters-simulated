package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphLookup;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class CoasterCartLinkPlacement {
   public static final double PLACEMENT_DISTANCE_SNAP_BLOCKS = 0.5;
   public static final double PLACEMENT_DISTANCE_MIN_BLOCKS = 1.5;
   public static final double PLACEMENT_DISTANCE_MAX_BLOCKS = 6.0;
   public static final double PLACEMENT_DISTANCE_REJECT_BLOCKS = 7.0;

   private CoasterCartLinkPlacement() {
   }

   public static Vec3 bearingCenterWorld(Vec3 plotOriginWorld, Quaterniond orientation) {
      Vector3d centerInPlot = bearingCenterOffsetPlot(orientation);
      return plotOriginWorld.add(centerInPlot.x, centerInPlot.y, centerInPlot.z);
   }

   public static Vec3 plotOriginForBearingCenter(Vec3 bearingWorld, Quaterniond orientation) {
      Vector3d centerInPlot = bearingCenterOffsetPlot(orientation);
      return bearingWorld.subtract(centerInPlot.x, centerInPlot.y, centerInPlot.z);
   }

   @Nullable
   public static Vec3 bearingCenterWorld(SubLevel cart, Pose3d pose) {
      BlockPos rep = CoasterCartTrackSnap.representativeBearingPlotPos(cart);
      return rep == null ? null : pose.transformPosition(Vec3.atCenterOf(rep));
   }

   public static CoasterCartLinkPlacement.AdjustedPlacement snapPlacementToLinkPartner(
      Level level, CoasterCartMidTrackPlacement.PlacementPose raw, Vec3 partnerBearingCenter, @Nullable Double partialTickForRender
   ) {
      Vec3 rawCenter = bearingCenterWorld(raw.plotOriginWorld(), raw.orientation());
      Vec3 delta = rawCenter.subtract(partnerBearingCenter);
      double dist = delta.length();
      Quaterniond orientation = raw.orientation();
      Vec3 dir;
      if (dist >= 1.0E-6) {
         dir = delta.scale(1.0 / dist);
      } else {
         CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, raw.graphHit(), partialTickForRender);
         dir = basis.forward().lengthSqr() >= 1.0E-12 ? basis.forward().normalize() : plotForwardWorld(orientation);
      }

      double snapped = dist < 1.0E-6 ? 1.5 : (double)Math.round(dist / 0.5) * 0.5;
      snapped = Mth.clamp(snapped, 1.5, 6.0);
      Vec3 snappedCenter = partnerBearingCenter.add(dir.scale(snapped));
      Vec3 plotOrigin = plotOriginForBearingCenter(snappedCenter, orientation);
      return new CoasterCartLinkPlacement.AdjustedPlacement(
         new CoasterCartMidTrackPlacement.PlacementPose(raw.graphHit(), plotOrigin, orientation), snapped, dist > 7.0
      );
   }

   public static boolean rejectsLinkPlacement(
      Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose raw, @Nullable Double partialTickForRender, boolean suppressLinkSnap
   ) {
      CoasterCartLinkPlacement.AdjustedPlacement adjusted = adjustForLinkSelection(level, stack, raw, partialTickForRender, suppressLinkSnap);
      return adjusted != null && adjusted.tooFar();
   }

   public static boolean rejectsLinkPlacement(
      Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose raw, @Nullable Double partialTickForRender
   ) {
      return rejectsLinkPlacement(level, stack, raw, partialTickForRender, false);
   }

   public static boolean isLinkCouplingPlacement(ItemStack stack) {
      return CoasterCartLinkSelection.hasLinkTarget(stack);
   }

   public static CoasterCartMidTrackPlacement.PlacementPose applyLinkSnapIfSelected(
      Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose pose, @Nullable Double partialTickForRender, boolean suppressLinkSnap
   ) {
      CoasterCartLinkPlacement.AdjustedPlacement adjusted = adjustForLinkSelection(level, stack, pose, partialTickForRender, suppressLinkSnap);
      return adjusted != null ? adjusted.pose() : pose;
   }

   public static CoasterCartMidTrackPlacement.PlacementPose applyLinkSnapIfSelected(
      Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose pose, @Nullable Double partialTickForRender
   ) {
      return applyLinkSnapIfSelected(level, stack, pose, partialTickForRender, false);
   }

   @Nullable
   public static CoasterCartLinkPlacement.AdjustedPlacement adjustForLinkSelection(
      Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose raw, @Nullable Double partialTickForRender, boolean suppressLinkSnap
   ) {
      if (!CoasterCartLinkSelection.hasLinkTarget(stack)) {
         return null;
      } else {
         SubLevel partner = CoasterCartResolve.findCartById(level, CoasterCartLinkSelection.readLinkTarget(stack));
         if (partner != null && !partner.isRemoved()) {
            Vec3 partnerCenter = partnerBearingCenterWorld(partner, partialTickForRender);
            if (partnerCenter == null) {
               return null;
            } else {
               return !suppressLinkSnap && shouldSnapPlacementToLinkPartner(level, partner, raw, partialTickForRender)
                  ? snapPlacementToLinkPartner(level, raw, partnerCenter, partialTickForRender)
                  : placementAtLinkPartnerWithoutSnap(raw, partnerCenter);
            }
         } else {
            return null;
         }
      }
   }

   @Nullable
   public static CoasterCartLinkPlacement.AdjustedPlacement adjustForLinkSelection(
      Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose raw, @Nullable Double partialTickForRender
   ) {
      return adjustForLinkSelection(level, stack, raw, partialTickForRender, false);
   }

   public static boolean shouldSnapPlacementToLinkPartner(
      Level level, SubLevel partner, CoasterCartMidTrackPlacement.PlacementPose preview, @Nullable Double partialTickForRender
   ) {
      if (!isCartTrackEngaged(level, partner)) {
         return false;
      } else {
         CoasterPathTrackFrame.GraphHit partnerHit = engagedTrackHit(level, partner, partialTickForRender);
         CoasterPathTrackFrame.GraphHit previewHit = preview.graphHit();
         if (partnerHit != null && previewHit != null) {
            CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
            int partnerIdx = graph.indexOfEdge(partnerHit.edge());
            int previewIdx = graph.indexOfEdge(previewHit.edge());
            return graph.edgesAreSameOrDirectNeighbors(partnerIdx, previewIdx);
         } else {
            return false;
         }
      }
   }

   private static CoasterCartLinkPlacement.AdjustedPlacement placementAtLinkPartnerWithoutSnap(
      CoasterCartMidTrackPlacement.PlacementPose raw, Vec3 partnerBearingCenter
   ) {
      Vec3 rawCenter = bearingCenterWorld(raw.plotOriginWorld(), raw.orientation());
      double dist = rawCenter.distanceTo(partnerBearingCenter);
      return new CoasterCartLinkPlacement.AdjustedPlacement(raw, dist, dist > 7.0);
   }

   @Nullable
   public static Vec3 partnerBearingCenterWorld(SubLevel partner, @Nullable Double partialTickForRender) {
      Pose3d pose = new Pose3d();
      if (partialTickForRender != null && partner instanceof ClientSubLevel client) {
         client.lastPose().lerp(client.logicalPose(), (double)partialTickForRender.floatValue(), pose);
      } else {
         pose.set(partner.logicalPose());
      }

      return bearingCenterWorld(partner, pose);
   }

   @Nullable
   public static ServerSubLevel spawnPlacedCart(ServerLevel level, CoasterCartMidTrackPlacement.PlacementPose pose, boolean linkCoupling) {
      ServerSubLevel sub = CoasterCartSpawner.spawnMinimalContraption(
         level, pose.plotOriginWorld(), pose.orientation(), linkCoupling ? null : pose.graphHit(), !linkCoupling
      );
      if (sub != null && linkCoupling) {
         CoasterCartTrackSnap.markSpawnedDisengaged(sub);
      }

      return sub;
   }

   public static MutableComponent linkDistanceActionBarComponent(double centerDistanceBlocks) {
      return Component.translatable(
            "simulatedcoasters.cart.link_distance_blocks", new Object[]{CoasterBezierHandleEdit.formatLiftMetersReadout((float)centerDistanceBlocks)}
         )
         .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(16777215)));
   }

   public static Component linkTooFarActionBarComponent() {
      return CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.link_too_far");
   }

   public static Component linkTooCloseActionBarComponent() {
      return CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.link_too_close");
   }

   public static Component linkDistanceRejectActionBarComponent(double centerDistanceBlocks) {
      return isLinkDistanceTooClose(centerDistanceBlocks) ? linkTooCloseActionBarComponent() : linkTooFarActionBarComponent();
   }

   public static boolean isValidLinkDistance(double centerDistanceBlocks) {
      return centerDistanceBlocks + 1.0E-6 >= 1.5 && centerDistanceBlocks <= 6.0;
   }

   public static boolean isLinkDistanceTooClose(double centerDistanceBlocks) {
      return !Double.isNaN(centerDistanceBlocks) && centerDistanceBlocks + 1.0E-6 < 1.5;
   }

   public static boolean isLinkDistanceTooFar(double centerDistanceBlocks) {
      return Double.isNaN(centerDistanceBlocks) || centerDistanceBlocks > 7.0;
   }

   public static boolean rejectsLinkDistance(double centerDistanceBlocks) {
      return isLinkDistanceTooClose(centerDistanceBlocks) || isLinkDistanceTooFar(centerDistanceBlocks);
   }

   public static double bearingCenterDistance(SubLevel cartA, SubLevel cartB, @Nullable Double partialTickForRender) {
      Vec3 a = partnerBearingCenterWorld(cartA, partialTickForRender);
      Vec3 b = partnerBearingCenterWorld(cartB, partialTickForRender);
      return a != null && b != null ? a.distanceTo(b) : Double.NaN;
   }

   public static double resolveChainLinkDistance(Level level, SubLevel cartA, SubLevel cartB, @Nullable Double partialTickForRender, boolean suppressLinkSnap) {
      double raw = bearingCenterDistance(cartA, cartB, partialTickForRender);
      return !suppressLinkSnap && !Double.isNaN(raw) && shareEngagedTrackSpline(level, cartA, cartB, partialTickForRender)
         ? snapLinkDistanceToHalfBlocks(raw)
         : raw;
   }

   public static double resolveChainLinkDistance(Level level, SubLevel cartA, SubLevel cartB, @Nullable Double partialTickForRender) {
      return resolveChainLinkDistance(level, cartA, cartB, partialTickForRender, false);
   }

   public static double snapLinkDistanceToHalfBlocks(double centerDistanceBlocks) {
      double snapped = (double)Math.round(centerDistanceBlocks / 0.5) * 0.5;
      return Mth.clamp(snapped, 1.5, 6.0);
   }

   public static boolean shareEngagedTrackSpline(Level level, SubLevel cartA, SubLevel cartB, @Nullable Double partialTickForRender) {
      CoasterPathTrackFrame.GraphHit hitA = engagedTrackHit(level, cartA, partialTickForRender);
      CoasterPathTrackFrame.GraphHit hitB = engagedTrackHit(level, cartB, partialTickForRender);
      if (hitA != null && hitB != null) {
         CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
         int idxA = graph.indexOfEdge(hitA.edge());
         int idxB = graph.indexOfEdge(hitB.edge());
         return graph.edgesAreSameOrDirectNeighbors(idxA, idxB);
      } else {
         return false;
      }
   }

   @Nullable
   private static CoasterPathTrackFrame.GraphHit engagedTrackHit(Level level, SubLevel cart, @Nullable Double partialTickForRender) {
      if (!isCartTrackEngaged(level, cart)) {
         return null;
      } else {
         CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
         if (graph.edges().isEmpty()) {
            return null;
         } else {
            Vec3 bearing = partnerBearingCenterWorld(cart, partialTickForRender);
            if (bearing == null) {
               return null;
            } else {
               if (level instanceof ServerLevel serverLevel && cart instanceof ServerSubLevel serverCart) {
                  CoasterPathTrackFrame.GraphHit hit = CoasterCartTrackHitCache.nearestGraphHit(serverLevel, graph, serverCart, bearing, 6.25);
                  return CoasterCartRailSnapEligibility.filterHitForBearing(cart, level, hit);
               }

               CoasterPathTrackFrame.GraphHit hit = CoasterPathTrackFrame.nearestGraphHit(level, graph, bearing, 6.25, partialTickForRender);
               return CoasterCartRailSnapEligibility.filterHitForBearing(cart, level, hit);
            }
         }
      }
   }

   private static boolean isCartTrackEngaged(Level level, SubLevel cart) {
      if (level instanceof ServerLevel && cart instanceof ServerSubLevel serverCart) {
         return !CoasterCartTrackSnap.isBearingDisengaged(serverCart);
      } else {
         BlockPos plot = CoasterCartTrackSnap.representativeBearingPlotPos(cart);
         if (plot == null) {
            return false;
         } else {
            BlockState st = level.getBlockState(plot);
            CoasterCartBlock bearingBlock = (CoasterCartBlock)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get();
            return st.is(bearingBlock) && (Boolean)st.getValue(CoasterCartBlock.SNAP_ENGAGED);
         }
      }
   }

   private static Vector3d bearingCenterOffsetPlot(Quaterniond orientation) {
      Vector3d centerInPlot = new Vector3d(0.5, 0.5, 0.5);
      orientation.transform(centerInPlot);
      return centerInPlot;
   }

   private static Vec3 plotForwardWorld(Quaterniond orientation) {
      Vector3d forward = new Vector3d(0.0, 0.0, 1.0);
      orientation.transform(forward);
      if (forward.lengthSquared() < 1.0E-12) {
         return new Vec3(0.0, 0.0, 1.0);
      } else {
         forward.normalize();
         return new Vec3(forward.x, forward.y, forward.z);
      }
   }

   public static record AdjustedPlacement(CoasterCartMidTrackPlacement.PlacementPose pose, double centerDistanceBlocks, boolean tooFar) {
   }
}
