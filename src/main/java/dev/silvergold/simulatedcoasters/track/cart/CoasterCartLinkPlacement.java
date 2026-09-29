/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.ClientSubLevel
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.chat.Style
 *  net.minecraft.network.chat.TextColor
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Vector3d
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.track.CoasterBezierHandleEdit;
import dev.silvergold.simulatedcoasters.track.CoasterTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkSelection;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartMidTrackPlacement;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartRailSnapEligibility;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartSpawner;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrackHitCache;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrackSnap;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphLookup;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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
        Vector3d centerInPlot = CoasterCartLinkPlacement.bearingCenterOffsetPlot(orientation);
        return plotOriginWorld.add(centerInPlot.x, centerInPlot.y, centerInPlot.z);
    }

    public static Vec3 plotOriginForBearingCenter(Vec3 bearingWorld, Quaterniond orientation) {
        Vector3d centerInPlot = CoasterCartLinkPlacement.bearingCenterOffsetPlot(orientation);
        return bearingWorld.subtract(centerInPlot.x, centerInPlot.y, centerInPlot.z);
    }

    @Nullable
    public static Vec3 bearingCenterWorld(SubLevel cart, Pose3d pose) {
        BlockPos rep = CoasterCartTrackSnap.representativeBearingPlotPos(cart);
        if (rep == null) {
            return null;
        }
        return pose.transformPosition(Vec3.atCenterOf((Vec3i)rep));
    }

    public static AdjustedPlacement snapPlacementToLinkPartner(Level level, CoasterCartMidTrackPlacement.PlacementPose raw, Vec3 partnerBearingCenter, @Nullable Double partialTickForRender) {
        CoasterPathTrackFrame.TrackBasis basis;
        Vec3 rawCenter = CoasterCartLinkPlacement.bearingCenterWorld(raw.plotOriginWorld(), raw.orientation());
        Vec3 delta = rawCenter.subtract(partnerBearingCenter);
        double dist = delta.length();
        Quaterniond orientation = raw.orientation();
        Vec3 dir = dist >= 1.0E-6 ? delta.scale(1.0 / dist) : ((basis = CoasterPathTrackFrame.trackBasisAtHit(level, raw.graphHit(), partialTickForRender)).forward().lengthSqr() >= 1.0E-12 ? basis.forward().normalize() : CoasterCartLinkPlacement.plotForwardWorld(orientation));
        double snapped = dist < 1.0E-6 ? 1.5 : (double)Math.round(dist / 0.5) * 0.5;
        snapped = Mth.clamp((double)snapped, (double)1.5, (double)6.0);
        Vec3 snappedCenter = partnerBearingCenter.add(dir.scale(snapped));
        Vec3 plotOrigin = CoasterCartLinkPlacement.plotOriginForBearingCenter(snappedCenter, orientation);
        return new AdjustedPlacement(new CoasterCartMidTrackPlacement.PlacementPose(raw.graphHit(), plotOrigin, orientation), snapped, dist > 7.0);
    }

    public static boolean rejectsLinkPlacement(Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose raw, @Nullable Double partialTickForRender, boolean suppressLinkSnap) {
        AdjustedPlacement adjusted = CoasterCartLinkPlacement.adjustForLinkSelection(level, stack, raw, partialTickForRender, suppressLinkSnap);
        return adjusted != null && adjusted.tooFar();
    }

    public static boolean rejectsLinkPlacement(Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose raw, @Nullable Double partialTickForRender) {
        return CoasterCartLinkPlacement.rejectsLinkPlacement(level, stack, raw, partialTickForRender, false);
    }

    public static boolean isLinkCouplingPlacement(ItemStack stack) {
        return CoasterCartLinkSelection.hasLinkTarget(stack);
    }

    public static CoasterCartMidTrackPlacement.PlacementPose applyLinkSnapIfSelected(Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose pose, @Nullable Double partialTickForRender, boolean suppressLinkSnap) {
        AdjustedPlacement adjusted = CoasterCartLinkPlacement.adjustForLinkSelection(level, stack, pose, partialTickForRender, suppressLinkSnap);
        return adjusted != null ? adjusted.pose() : pose;
    }

    public static CoasterCartMidTrackPlacement.PlacementPose applyLinkSnapIfSelected(Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose pose, @Nullable Double partialTickForRender) {
        return CoasterCartLinkPlacement.applyLinkSnapIfSelected(level, stack, pose, partialTickForRender, false);
    }

    @Nullable
    public static AdjustedPlacement adjustForLinkSelection(Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose raw, @Nullable Double partialTickForRender, boolean suppressLinkSnap) {
        if (!CoasterCartLinkSelection.hasLinkTarget(stack)) {
            return null;
        }
        SubLevel partner = CoasterCartResolve.findCartById(level, CoasterCartLinkSelection.readLinkTarget(stack));
        if (partner == null || partner.isRemoved()) {
            return null;
        }
        Vec3 partnerCenter = CoasterCartLinkPlacement.partnerBearingCenterWorld(partner, partialTickForRender);
        if (partnerCenter == null) {
            return null;
        }
        if (!suppressLinkSnap && CoasterCartLinkPlacement.shouldSnapPlacementToLinkPartner(level, partner, raw, partialTickForRender)) {
            return CoasterCartLinkPlacement.snapPlacementToLinkPartner(level, raw, partnerCenter, partialTickForRender);
        }
        return CoasterCartLinkPlacement.placementAtLinkPartnerWithoutSnap(raw, partnerCenter);
    }

    @Nullable
    public static AdjustedPlacement adjustForLinkSelection(Level level, ItemStack stack, CoasterCartMidTrackPlacement.PlacementPose raw, @Nullable Double partialTickForRender) {
        return CoasterCartLinkPlacement.adjustForLinkSelection(level, stack, raw, partialTickForRender, false);
    }

    public static boolean shouldSnapPlacementToLinkPartner(Level level, SubLevel partner, CoasterCartMidTrackPlacement.PlacementPose preview, @Nullable Double partialTickForRender) {
        if (!CoasterCartLinkPlacement.isCartTrackEngaged(level, partner)) {
            return false;
        }
        CoasterPathTrackFrame.GraphHit partnerHit = CoasterCartLinkPlacement.engagedTrackHit(level, partner, partialTickForRender);
        CoasterPathTrackFrame.GraphHit previewHit = preview.graphHit();
        if (partnerHit == null || previewHit == null) {
            return false;
        }
        CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
        int partnerIdx = graph.indexOfEdge(partnerHit.edge());
        int previewIdx = graph.indexOfEdge(previewHit.edge());
        return graph.edgesAreSameOrDirectNeighbors(partnerIdx, previewIdx);
    }

    private static AdjustedPlacement placementAtLinkPartnerWithoutSnap(CoasterCartMidTrackPlacement.PlacementPose raw, Vec3 partnerBearingCenter) {
        double dist;
        Vec3 rawCenter = CoasterCartLinkPlacement.bearingCenterWorld(raw.plotOriginWorld(), raw.orientation());
        return new AdjustedPlacement(raw, dist, (dist = rawCenter.distanceTo(partnerBearingCenter)) > 7.0);
    }

    @Nullable
    public static Vec3 partnerBearingCenterWorld(SubLevel partner, @Nullable Double partialTickForRender) {
        Pose3d pose = new Pose3d();
        if (partialTickForRender != null && partner instanceof ClientSubLevel) {
            ClientSubLevel client = (ClientSubLevel)partner;
            client.lastPose().lerp((Pose3dc)client.logicalPose(), (double)partialTickForRender.floatValue(), pose);
        } else {
            pose.set((Pose3dc)partner.logicalPose());
        }
        return CoasterCartLinkPlacement.bearingCenterWorld(partner, pose);
    }

    @Nullable
    public static ServerSubLevel spawnPlacedCart(ServerLevel level, CoasterCartMidTrackPlacement.PlacementPose pose, boolean linkCoupling) {
        ServerSubLevel sub = CoasterCartSpawner.spawnMinimalContraption(level, pose.plotOriginWorld(), pose.orientation(), linkCoupling ? null : pose.graphHit(), !linkCoupling);
        if (sub != null && linkCoupling) {
            CoasterCartTrackSnap.markSpawnedDisengaged(sub);
        }
        return sub;
    }

    public static MutableComponent linkDistanceActionBarComponent(double centerDistanceBlocks) {
        return Component.translatable((String)"simulatedcoasters.cart.link_distance_blocks", (Object[])new Object[]{CoasterBezierHandleEdit.formatLiftMetersReadout((float)centerDistanceBlocks)}).withStyle(Style.EMPTY.withColor(TextColor.fromRgb((int)0xFFFFFF)));
    }

    public static Component linkTooFarActionBarComponent() {
        return CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.link_too_far");
    }

    public static Component linkTooCloseActionBarComponent() {
        return CoasterTrackPlacement.placementErrorMessage("simulatedcoasters.cart.link_too_close");
    }

    public static Component linkDistanceRejectActionBarComponent(double centerDistanceBlocks) {
        if (CoasterCartLinkPlacement.isLinkDistanceTooClose(centerDistanceBlocks)) {
            return CoasterCartLinkPlacement.linkTooCloseActionBarComponent();
        }
        return CoasterCartLinkPlacement.linkTooFarActionBarComponent();
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
        return CoasterCartLinkPlacement.isLinkDistanceTooClose(centerDistanceBlocks) || CoasterCartLinkPlacement.isLinkDistanceTooFar(centerDistanceBlocks);
    }

    public static double bearingCenterDistance(SubLevel cartA, SubLevel cartB, @Nullable Double partialTickForRender) {
        Vec3 a = CoasterCartLinkPlacement.partnerBearingCenterWorld(cartA, partialTickForRender);
        Vec3 b = CoasterCartLinkPlacement.partnerBearingCenterWorld(cartB, partialTickForRender);
        if (a == null || b == null) {
            return Double.NaN;
        }
        return a.distanceTo(b);
    }

    public static double resolveChainLinkDistance(Level level, SubLevel cartA, SubLevel cartB, @Nullable Double partialTickForRender, boolean suppressLinkSnap) {
        double raw = CoasterCartLinkPlacement.bearingCenterDistance(cartA, cartB, partialTickForRender);
        if (suppressLinkSnap || Double.isNaN(raw) || !CoasterCartLinkPlacement.shareEngagedTrackSpline(level, cartA, cartB, partialTickForRender)) {
            return raw;
        }
        return CoasterCartLinkPlacement.snapLinkDistanceToHalfBlocks(raw);
    }

    public static double resolveChainLinkDistance(Level level, SubLevel cartA, SubLevel cartB, @Nullable Double partialTickForRender) {
        return CoasterCartLinkPlacement.resolveChainLinkDistance(level, cartA, cartB, partialTickForRender, false);
    }

    public static double snapLinkDistanceToHalfBlocks(double centerDistanceBlocks) {
        double snapped = (double)Math.round(centerDistanceBlocks / 0.5) * 0.5;
        return Mth.clamp((double)snapped, (double)1.5, (double)6.0);
    }

    public static boolean shareEngagedTrackSpline(Level level, SubLevel cartA, SubLevel cartB, @Nullable Double partialTickForRender) {
        CoasterPathTrackFrame.GraphHit hitA = CoasterCartLinkPlacement.engagedTrackHit(level, cartA, partialTickForRender);
        CoasterPathTrackFrame.GraphHit hitB = CoasterCartLinkPlacement.engagedTrackHit(level, cartB, partialTickForRender);
        if (hitA == null || hitB == null) {
            return false;
        }
        CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
        int idxA = graph.indexOfEdge(hitA.edge());
        int idxB = graph.indexOfEdge(hitB.edge());
        return graph.edgesAreSameOrDirectNeighbors(idxA, idxB);
    }

    /*
     * Enabled aggressive block sorting
     */
    @Nullable
    private static CoasterPathTrackFrame.GraphHit engagedTrackHit(Level level, SubLevel cart, @Nullable Double partialTickForRender) {
        CoasterPathTrackFrame.GraphHit hit;
        if (!CoasterCartLinkPlacement.isCartTrackEngaged(level, cart)) {
            return null;
        }
        CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
        if (graph.edges().isEmpty()) {
            return null;
        }
        Vec3 bearing = CoasterCartLinkPlacement.partnerBearingCenterWorld(cart, partialTickForRender);
        if (bearing == null) {
            return null;
        }
        if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            if (cart instanceof ServerSubLevel) {
                ServerSubLevel serverCart = (ServerSubLevel)cart;
                hit = CoasterCartTrackHitCache.nearestGraphHit(serverLevel, graph, serverCart, bearing, 6.25);
                return CoasterCartRailSnapEligibility.filterHitForBearing(cart, level, hit);
            }
        }
        hit = CoasterPathTrackFrame.nearestGraphHit(level, graph, bearing, 6.25, partialTickForRender);
        return CoasterCartRailSnapEligibility.filterHitForBearing(cart, level, hit);
    }

    private static boolean isCartTrackEngaged(Level level, SubLevel cart) {
        CoasterCartBlock bearingBlock;
        if (level instanceof ServerLevel && cart instanceof ServerSubLevel) {
            ServerSubLevel serverCart = (ServerSubLevel)cart;
            return !CoasterCartTrackSnap.isBearingDisengaged(serverCart);
        }
        BlockPos plot = CoasterCartTrackSnap.representativeBearingPlotPos(cart);
        if (plot == null) {
            return false;
        }
        BlockState st = level.getBlockState(plot);
        return st.is((Block)(bearingBlock = (CoasterCartBlock)((Object)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get()))) && (Boolean)st.getValue((Property)CoasterCartBlock.SNAP_ENGAGED) != false;
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
        }
        forward.normalize();
        return new Vec3(forward.x, forward.y, forward.z);
    }

    public record AdjustedPlacement(CoasterCartMidTrackPlacement.PlacementPose pose, double centerDistanceBlocks, boolean tooFar) {
    }
}

