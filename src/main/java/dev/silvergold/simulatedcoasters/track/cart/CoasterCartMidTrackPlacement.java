/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.data.Couple
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.entity.ai.attributes.Attributes
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Vector3d
 */
package dev.silvergold.simulatedcoasters.track.cart;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartLinkPlacement;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphLookup;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class CoasterCartMidTrackPlacement {
    private CoasterCartMidTrackPlacement() {
    }

    public static Vec3 plotOriginForGraphHit(Level level, CoasterPathTrackFrame.GraphHit hit, Quaterniond orientation, @Nullable Double partialTickForRender) {
        Vec3 bearingCenter = partialTickForRender != null ? CoasterPathTrackFrame.pathAnchorForSnappedBlock(level, hit, partialTickForRender.floatValue()) : CoasterPathTrackFrame.pathAnchorForSnappedBlock(level, hit);
        Vector3d centerInPlot = new Vector3d(0.5, 0.5, 0.5);
        orientation.transform(centerInPlot);
        return bearingCenter.subtract(centerInPlot.x, centerInPlot.y, centerInPlot.z);
    }

    @Nullable
    public static Vec3 plotOriginFromPlacedCart(SubLevel cart, Pose3d pose) {
        Vec3 bearingWorld = CoasterCartLinkPlacement.bearingCenterWorld(cart, pose);
        if (bearingWorld == null) {
            return null;
        }
        return CoasterCartLinkPlacement.plotOriginForBearingCenter(bearingWorld, pose.orientation());
    }

    @Nullable
    public static PlacementPose resolveMidSpan(Level level, Player player, Vec3 lookDirection, @Nullable Double partialTickForRender, @Nullable BlockPos rasterHint) {
        if (rasterHint != null) {
            PlacementPose pose = CoasterCartMidTrackPlacement.resolveFromRaster(level, player, rasterHint, lookDirection, partialTickForRender);
            if (pose != null) {
                return pose;
            }
            if (CoasterCartMidTrackPlacement.isAcceptedAnchor(level, rasterHint)) {
                return CoasterCartMidTrackPlacement.resolveFromAnchor(level, player, rasterHint, lookDirection, null, partialTickForRender);
            }
            return null;
        }
        return CoasterCartMidTrackPlacement.resolveFromCoasterCurveLook(level, player, lookDirection, partialTickForRender);
    }

    @Nullable
    public static PlacementPose resolveForCurve(Level level, Player player, BezierConnection hoveredCurve, Vec3 lookDirection, @Nullable Double partialTickForRender) {
        CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
        return CoasterCartMidTrackPlacement.resolveOnEdges(level, player, CoasterCartMidTrackPlacement.edgesForCurve(graph, hoveredCurve), lookDirection, partialTickForRender);
    }

    @Nullable
    public static PlacementPose resolveFromCoasterCurveLook(Level level, Player player, Vec3 lookDirection, @Nullable Double partialTickForRender) {
        CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
        return CoasterCartMidTrackPlacement.resolveOnEdges(level, player, CoasterCartMidTrackPlacement.coasterMaterialEdges(graph), lookDirection, partialTickForRender);
    }

    @Nullable
    public static PlacementPose resolveFromRaster(Level level, Player player, BlockPos rasterCell, Vec3 lookDirection, @Nullable Double partialTickForRender) {
        CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
        return CoasterCartMidTrackPlacement.resolveOnEdges(level, player, CoasterCartMidTrackPlacement.edgesCoveringFakeRaster(graph, rasterCell), lookDirection, partialTickForRender);
    }

    @Nullable
    private static PlacementPose resolveOnEdges(Level level, Player player, List<CoasterPathEdge> edges, Vec3 lookDirection, @Nullable Double partialTickForRender) {
        Vec3 look;
        if (edges.isEmpty()) {
            return null;
        }
        float partialTick = partialTickForRender != null ? partialTickForRender.floatValue() : 1.0f;
        Vec3 eye = player.getEyePosition(partialTick);
        CoasterPathTrackFrame.GraphHit hit = CoasterCartMidTrackPlacement.nearestGraphHitOnEdgesNearRay(level, edges, eye, look = lookDirection.lengthSqr() < 1.0E-12 ? player.getLookAngle() : lookDirection.normalize(), player, partialTick);
        if (hit == null) {
            return null;
        }
        CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender);
        Quaterniond orientation = CoasterPathTrackFrame.quaternionFromTrackBasis(basis);
        Vec3 plotOrigin = CoasterCartMidTrackPlacement.plotOriginForGraphHit(level, hit, orientation, partialTickForRender);
        return new PlacementPose(hit, plotOrigin, orientation);
    }

    public static boolean isAcceptedAnchor(Level level, BlockPos anchorPos) {
        CoasterAnchorpointBlockEntity be;
        BlockEntity blockEntity;
        return level.getBlockState(anchorPos).getBlock() instanceof CoasterAnchorpointBlock && (blockEntity = level.getBlockEntity(anchorPos)) instanceof CoasterAnchorpointBlockEntity && (be = (CoasterAnchorpointBlockEntity)blockEntity).legCount() > 0;
    }

    @Nullable
    public static PlacementPose resolveFromAnchor(Level level, Player player, BlockPos anchorPos, Vec3 lookDirection, @Nullable Vec3 clickLocation, @Nullable Double partialTickForRender) {
        Vec3 look;
        if (!CoasterCartMidTrackPlacement.isAcceptedAnchor(level, anchorPos)) {
            return null;
        }
        CoasterPathGraph graph = CoasterPathGraphLookup.get(level);
        List<CoasterPathEdge> edges = CoasterCartMidTrackPlacement.edgesTouchingBlock(graph, anchorPos);
        if (edges.isEmpty()) {
            return null;
        }
        float partialTick = partialTickForRender != null ? partialTickForRender.floatValue() : 1.0f;
        Vec3 eye = player.getEyePosition(partialTick);
        CoasterPathTrackFrame.GraphHit hit = CoasterCartMidTrackPlacement.nearestGraphHitOnEdgesNearRay(level, edges, eye, look = lookDirection.lengthSqr() < 1.0E-12 ? player.getLookAngle() : lookDirection.normalize(), player, partialTick);
        if (hit == null && clickLocation != null) {
            Vec3 probe = CoasterCartMidTrackPlacement.graphProbeWorld(level, anchorPos, clickLocation);
            hit = CoasterPathTrackFrame.nearestGraphHit(level, edges, probe, 9216.0, partialTickForRender);
        }
        if (hit == null) {
            return null;
        }
        CoasterPathTrackFrame.TrackBasis basis = CoasterPathTrackFrame.trackBasisAtHit(level, hit, partialTickForRender);
        Quaterniond orientation = CoasterPathTrackFrame.quaternionFromTrackBasis(basis);
        Vec3 plotOrigin = CoasterCartMidTrackPlacement.plotOriginForGraphHit(level, hit, orientation, partialTickForRender);
        return new PlacementPose(hit, plotOrigin, orientation);
    }

    public static boolean isCoasterMidSpanRaster(Level level, BlockPos raster) {
        if (!AnchorPeerFakeTracks.isManagedFakeTrack(level, raster)) {
            return false;
        }
        for (BezierConnection bc : AnchorPeerFakeTracks.hostedPrimaryCoasterCurvesContainingCell(level, raster)) {
            BezierConnection p;
            BezierConnection bezierConnection = p = bc.isPrimary() ? bc : bc.secondary();
            if (!p.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) continue;
            return true;
        }
        return false;
    }

    private static List<CoasterPathEdge> coasterMaterialEdges(CoasterPathGraph graph) {
        ArrayList<CoasterPathEdge> out = new ArrayList<CoasterPathEdge>();
        for (CoasterPathEdge e : graph.edges()) {
            if (e.bezier() == null || !e.bezier().getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) continue;
            out.add(e);
        }
        return out;
    }

    private static List<CoasterPathEdge> edgesCoveringFakeRaster(CoasterPathGraph graph, BlockPos raster) {
        ArrayList<CoasterPathEdge> out = new ArrayList<CoasterPathEdge>();
        Vec3 rasterCenter = Vec3.atCenterOf((Vec3i)raster);
        for (CoasterPathEdge e : graph.edges()) {
            BezierConnection primary;
            if (e.bezier() == null || !(primary = e.bezier().isPrimary() ? e.bezier() : e.bezier().secondary()).getBounds().inflate(2.0).contains(rasterCenter) || !AnchorPeerFakeTracks.rasterContains(e.bezier(), raster)) continue;
            out.add(e);
        }
        return out;
    }

    private static List<CoasterPathEdge> edgesForCurve(CoasterPathGraph graph, BezierConnection bc) {
        BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
        BlockPos a = (BlockPos)primary.bePositions.getFirst();
        BlockPos b = (BlockPos)primary.bePositions.getSecond();
        ArrayList<CoasterPathEdge> out = new ArrayList<CoasterPathEdge>();
        for (CoasterPathEdge e : graph.edges()) {
            if (e.bezier() == null) continue;
            Couple bp = (e.bezier().isPrimary() ? e.bezier() : e.bezier().secondary()).bePositions;
            BlockPos ea = (BlockPos)bp.getFirst();
            BlockPos eb = (BlockPos)bp.getSecond();
            if ((!ea.equals((Object)a) || !eb.equals((Object)b)) && (!ea.equals((Object)b) || !eb.equals((Object)a))) continue;
            out.add(e);
        }
        return out;
    }

    private static List<CoasterPathEdge> edgesTouchingBlock(CoasterPathGraph graph, BlockPos pos) {
        ArrayList<CoasterPathEdge> out = new ArrayList<CoasterPathEdge>();
        for (CoasterPathEdge e : graph.edges()) {
            Couple bp;
            if (e.from().equals((Object)pos) || e.to().equals((Object)pos)) {
                out.add(e);
                continue;
            }
            if (e.bezier() == null || !((BlockPos)(bp = e.bezier().bePositions).getFirst()).equals((Object)pos) && !((BlockPos)bp.getSecond()).equals((Object)pos)) continue;
            out.add(e);
        }
        return out;
    }

    private static Vec3 graphProbeWorld(Level level, BlockPos anchorPos, @Nullable Vec3 clickLocation) {
        if (clickLocation == null) {
            return CoasterAnchorpointBlockEntity.worldCenter(level, anchorPos);
        }
        SubLevel host = Sable.HELPER.getContaining(level, (Vec3i)anchorPos);
        if (host == null) {
            return clickLocation;
        }
        return host.logicalPose().transformPosition(clickLocation);
    }

    private static double distPointToRaySq(Vec3 p, Vec3 rayOrigin, Vec3 rayDirUnit) {
        Vec3 v = p.subtract(rayOrigin);
        double along = v.dot(rayDirUnit);
        if (along < 0.0) {
            return Double.MAX_VALUE;
        }
        Vec3 closestOnRay = rayOrigin.add(rayDirUnit.scale(along));
        return p.distanceToSqr(closestOnRay);
    }

    @Nullable
    private static CoasterPathTrackFrame.GraphHit nearestGraphHitOnEdgesNearRay(Level level, List<CoasterPathEdge> edges, Vec3 eye, Vec3 lookDirUnit, Player player, float partialTick) {
        if (edges.isEmpty()) {
            return null;
        }
        double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
        Double pt = partialTick;
        int samples = 96;
        double bestLateralSq = Double.MAX_VALUE;
        double lookAlong = -1.7976931348623157E308;
        CoasterPathTrackFrame.GraphHit lookHit = null;
        for (CoasterPathEdge edge : edges) {
            for (int i = 0; i <= 96; ++i) {
                double lateralBand;
                double t = (double)i / 96.0;
                Vec3 sample = CoasterPathGraphManager.sampleEdgePosition(level, edge, t, pt);
                double dRay = CoasterCartMidTrackPlacement.distPointToRaySq(sample, eye, lookDirUnit);
                double along = sample.subtract(eye).dot(lookDirUnit);
                if (along < -0.25) continue;
                if (dRay < bestLateralSq - 1.0E-9) {
                    bestLateralSq = dRay;
                    lookAlong = along;
                    lookHit = new CoasterPathTrackFrame.GraphHit(sample, edge, t);
                    continue;
                }
                if (lookHit == null || !(dRay <= bestLateralSq + (lateralBand = Math.max(0.04, bestLateralSq * 0.01))) || !(along > lookAlong)) continue;
                lookAlong = along;
                lookHit = new CoasterPathTrackFrame.GraphHit(sample, edge, t);
            }
        }
        if (lookHit == null || bestLateralSq >= 9.0 || !CoasterCartMidTrackPlacement.isWithinBlockInteractionReach(eye, lookHit.point(), reach)) {
            return null;
        }
        return lookHit;
    }

    private static boolean isWithinBlockInteractionReach(Vec3 eye, Vec3 target, double reach) {
        double limit = reach + 0.5;
        return eye.distanceToSqr(target) <= limit * limit;
    }

    public record PlacementPose(CoasterPathTrackFrame.GraphHit graphHit, Vec3 plotOriginWorld, Quaterniond orientation) {
    }
}

