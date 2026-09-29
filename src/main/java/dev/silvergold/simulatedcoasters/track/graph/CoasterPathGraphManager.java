/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.createmod.catnip.data.Couple
 *  net.createmod.catnip.math.VecHelper
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.track.graph;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.CoasterBezierRailFrames;
import dev.silvergold.simulatedcoasters.track.CoasterOpenEndExtension;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterBezierEdgeSamplingContext;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathNode;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathSavedData;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTangentOrientation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class CoasterPathGraphManager {
    public static final Vec3 PATH_CENTER_OFFSET = new Vec3(0.0, 0.0, 0.0);
    private static final Set<ResourceKey<Level>> SERVER_TANGENT_ALIGNED = new HashSet<ResourceKey<Level>>();

    public static Vec3 pathCenterlineWorld(Vec3 rawBezierWorld) {
        return rawBezierWorld.add(PATH_CENTER_OFFSET);
    }

    public static Vec3 coasterBezierParametricSpineRaw(BezierConnection bc, double t) {
        float tf = (float)Mth.clamp((double)t, (double)0.0, (double)1.0);
        Vec3 e1 = (Vec3)bc.starts.getFirst();
        Vec3 e2 = (Vec3)bc.starts.getSecond();
        Vec3 c1 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 0);
        Vec3 c2 = CoasterPeerCurveHandleLengths.controlPointWorld(bc, 1);
        return VecHelper.bezier((Vec3)e1, (Vec3)e2, (Vec3)c1, (Vec3)c2, (float)tf);
    }

    public static boolean isCoasterGraphBezier(BezierConnection bc) {
        return bc != null && bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id);
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
        SubLevel sub = Sable.HELPER.getContaining(level, (Vec3i)anchor);
        if (sub == null) {
            return plotLocal;
        }
        if (partialTickForRender == null || !level.isClientSide()) {
            return sub.logicalPose().transformPosition(plotLocal);
        }
        Pose3d vis = new Pose3d();
        sub.lastPose().lerp((Pose3dc)sub.logicalPose(), partialTickForRender.doubleValue(), vis);
        return vis.transformPosition(plotLocal);
    }

    public static Vec3 sublevelWorldPosition(Level level, BlockPos anchor, Vec3 plotLocal) {
        return CoasterPathGraphManager.sublevelWorldPosition(level, anchor, plotLocal, null);
    }

    public static Vec3 sublevelWorldDirection(Level level, BlockPos anchor, Vec3 plotLocalDir, @Nullable Double partialTickForRender) {
        SubLevel sub = Sable.HELPER.getContaining(level, (Vec3i)anchor);
        if (sub == null) {
            return plotLocalDir;
        }
        Vector3d v = new Vector3d(plotLocalDir.x, plotLocalDir.y, plotLocalDir.z);
        if (partialTickForRender == null || !level.isClientSide()) {
            sub.logicalPose().orientation().transform(v);
        } else {
            Pose3d vis = new Pose3d();
            sub.lastPose().lerp((Pose3dc)sub.logicalPose(), partialTickForRender.doubleValue(), vis);
            vis.orientation().transform(v);
        }
        return new Vec3(v.x, v.y, v.z);
    }

    public static Vec3 sublevelWorldDirection(Level level, BlockPos anchor, Vec3 plotLocalDir) {
        return CoasterPathGraphManager.sublevelWorldDirection(level, anchor, plotLocalDir, null);
    }

    public static boolean anchorBlockIsOnSubLevel(Level level, BlockPos anchor) {
        return Sable.HELPER.getContaining(level, (Vec3i)anchor) != null;
    }

    public static boolean edgeHostedOnSubLevel(Level level, CoasterPathEdge edge) {
        return CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, edge.from()) || CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, edge.to());
    }

    public static BlockPos edgeSubLevelAnchor(Level level, CoasterPathEdge edge) {
        if (CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, edge.from())) {
            return edge.from();
        }
        if (CoasterPathGraphManager.anchorBlockIsOnSubLevel(level, edge.to())) {
            return edge.to();
        }
        return edge.from();
    }

    @Nullable
    public static Vec3 bearingWorldInTrackHostPlotLocal(Level level, CoasterPathEdge edge, Vec3 bearingWorld) {
        if (!CoasterPathGraphManager.edgeHostedOnSubLevel(level, edge)) {
            return null;
        }
        BlockPos anchor = CoasterPathGraphManager.edgeSubLevelAnchor(level, edge);
        SubLevel trackHost = Sable.HELPER.getContaining(level, (Vec3i)anchor);
        if (trackHost == null) {
            return null;
        }
        Vector3d local = trackHost.logicalPose().transformPositionInverse((Vector3dc)new Vector3d(bearingWorld.x, bearingWorld.y, bearingWorld.z), new Vector3d());
        return new Vec3(local.x, local.y, local.z);
    }

    public static Vec3 trackHostPlotLocalToWorld(Level level, CoasterPathEdge edge, Vec3 plotLocal) {
        BlockPos anchor = CoasterPathGraphManager.edgeSubLevelAnchor(level, edge);
        return CoasterPathGraphManager.sublevelWorldPosition(level, anchor, plotLocal);
    }

    public static CoasterPathGraph get(ServerLevel level) {
        CoasterPathGraph g = CoasterPathSavedData.get(level.getServer()).graphFor((ResourceKey<Level>)level.dimension());
        CoasterPathGraphManager.ensureServerTangentAligned(level, g);
        return g;
    }

    private static void ensureServerTangentAligned(ServerLevel level, CoasterPathGraph g) {
        if (g.edges().isEmpty()) {
            return;
        }
        ResourceKey dim = level.dimension();
        if (SERVER_TANGENT_ALIGNED.contains(dim)) {
            return;
        }
        CoasterPathTangentOrientation.alignConnectedComponents((Level)level, g);
        SERVER_TANGENT_ALIGNED.add((ResourceKey<Level>)dim);
    }

    public static Vec3 sampleNodePosition(Level level, CoasterPathNode node) {
        return CoasterPathGraphManager.sampleNodePosition(level, node, null);
    }

    public static Vec3 sampleNodePosition(Level level, CoasterPathNode node, @Nullable Double partialTickForRender) {
        BlockPos pos = node.pos();
        Vec3 local = CoasterPathGraphManager.sampleNodePositionPlotLocal(level, node);
        return CoasterPathGraphManager.sublevelWorldPosition(level, pos, local, partialTickForRender);
    }

    public static Vec3 sampleNodePositionPlotLocal(Level level, CoasterPathNode node) {
        BlockPos pos = node.pos();
        BlockState st = level.getBlockState(pos);
        if (st.getBlock() instanceof CoasterAnchorpointBlock) {
            return CoasterAnchorpointBlockEntity.worldCenter(level, pos).add(PATH_CENTER_OFFSET).add(CoasterAnchorpointBlockEntity.anchorLiftDirection(level, pos).scale(0.0625));
        }
        return Vec3.atBottomCenterOf((Vec3i)pos).add(PATH_CENTER_OFFSET);
    }

    public static Vec3 sampleEdgePosition(Level level, CoasterPathEdge edge, double t) {
        return CoasterPathGraphManager.sampleEdgePosition(level, edge, t, null);
    }

    public static Vec3 sampleEdgePositionForNearestQuery(Level level, CoasterPathEdge edge, double t) {
        return CoasterPathGraphManager.sampleEdgePositionForNearestQuery(level, edge, t, null);
    }

    public static Vec3 sampleEdgePositionForNearestQuery(Level level, CoasterPathEdge edge, double t, @Nullable Double partialTickForRender) {
        if (CoasterPathGraphManager.edgeHostedOnSubLevel(level, edge)) {
            return CoasterPathGraphManager.sampleEdgePosition(level, edge, t, partialTickForRender);
        }
        BlockPos anchor = CoasterPathGraphManager.edgeSubLevelAnchor(level, edge);
        if (edge.bezier() != null) {
            BezierConnection bc = edge.bezier();
            if (CoasterPathGraphManager.isCoasterGraphBezier(bc)) {
                Vec3 local = CoasterPathGraphManager.coasterBezierPlotLocalCenterline(level, edge, t);
                return CoasterPathGraphManager.sublevelWorldPosition(level, anchor, local, partialTickForRender);
            }
            return CoasterPathGraphManager.sublevelWorldPosition(level, anchor, bc.getPosition(t).add(PATH_CENTER_OFFSET), partialTickForRender);
        }
        if (edge.straightFromWorld() != null && edge.straightToWorld() != null) {
            return CoasterPathGraphManager.sublevelWorldPosition(level, anchor, edge.straightFromWorld().lerp(edge.straightToWorld(), t).add(PATH_CENTER_OFFSET), partialTickForRender);
        }
        Vec3 from = CoasterPathGraphManager.sampleNodePosition(level, new CoasterPathNode(edge.from(), Vec3.ZERO), partialTickForRender);
        Vec3 to = CoasterPathGraphManager.sampleNodePosition(level, new CoasterPathNode(edge.to(), Vec3.ZERO), partialTickForRender);
        return from.lerp(to, t);
    }

    public static boolean isOpenCurveEnd(Level level, CoasterPathEdge edge, boolean atFromEndpoint) {
        BezierConnection bc = edge.bezier();
        if (bc == null || !CoasterPathGraphManager.isCoasterGraphBezier(bc)) {
            return false;
        }
        BlockPos anchor = atFromEndpoint ? edge.from() : edge.to();
        BlockEntity blockEntity = level.getBlockEntity(anchor);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        return be.legCount() < 2;
    }

    public static double openEndExtensionTSpan(Level level, CoasterPathEdge edge, boolean atFromEndpoint) {
        Vec3 b;
        if (!CoasterPathGraphManager.isOpenCurveEnd(level, edge, atFromEndpoint)) {
            return 0.0;
        }
        BezierConnection bc = edge.bezier();
        if (bc == null) {
            return 0.0;
        }
        Vec3 a = CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(bc, 0.0));
        double chord = a.distanceTo(b = CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(bc, 1.0)));
        if (chord < 1.0E-8) {
            return 1.0;
        }
        BlockPos anchor = atFromEndpoint ? edge.from() : edge.to();
        return (double)CoasterOpenEndExtension.extensionBlocks(level, anchor) / chord;
    }

    @Deprecated
    public static double openEndExtensionTSpan(Level level, CoasterPathEdge edge) {
        return Math.max(CoasterPathGraphManager.openEndExtensionTSpan(level, edge, true), CoasterPathGraphManager.openEndExtensionTSpan(level, edge, false));
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
        Vec3 pM = CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(bc, tm));
        Vec3 pP = CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(bc, tp));
        Vec3 d = pP.subtract(pM);
        if (d.lengthSqr() < 1.0E-14) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return d;
    }

    public static Vec3 plotLocalCenterlineTangentAlongIncreasingT(Level level, CoasterPathEdge edge, double t) {
        BezierConnection bc = edge.bezier();
        if (bc == null || !CoasterPathGraphManager.isCoasterGraphBezier(bc)) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        double[] range = CoasterPathGraphManager.coasterBezierSampleTRange(level, edge);
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
        Vec3 pM = CoasterPathGraphManager.coasterBezierPlotLocalCenterline(level, edge, tm);
        Vec3 pP = CoasterPathGraphManager.coasterBezierPlotLocalCenterline(level, edge, tp);
        Vec3 d = pP.subtract(pM);
        if (d.lengthSqr() < 1.0E-14) {
            Vec3 fallback = CoasterPathGraphManager.coasterBezierPlotLocalTangent(bc, Mth.clamp((double)t, (double)0.0, (double)1.0));
            return fallback.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : fallback;
        }
        return d;
    }

    public static double[] coasterBezierSampleTRange(Level level, CoasterPathEdge edge) {
        CoasterBezierEdgeSamplingContext ctx = CoasterBezierEdgeSamplingContext.forCoasterEdge(level, edge);
        if (ctx != null) {
            return ctx.sampleTRange();
        }
        return new double[]{0.0, 1.0};
    }

    private static Vec3 coasterBezierPlotLocalCenterline(Level level, CoasterPathEdge edge, double t) {
        CoasterBezierEdgeSamplingContext ctx = CoasterBezierEdgeSamplingContext.forCoasterEdge(level, edge);
        if (ctx != null) {
            return ctx.plotLocalCenterline(t);
        }
        BezierConnection bc = edge.bezier();
        if (bc == null) {
            return Vec3.ZERO;
        }
        return CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(bc, Mth.clamp((double)t, (double)0.0, (double)1.0)));
    }

    public static Vec3 sampleEdgePosition(Level level, CoasterPathEdge edge, double t, @Nullable Double partialTickForRender) {
        BlockPos anchor = CoasterPathGraphManager.edgeSubLevelAnchor(level, edge);
        return CoasterPathGraphManager.sublevelWorldPosition(level, anchor, CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, t), partialTickForRender);
    }

    public static Vec3 sampleEdgePositionPlotLocal(Level level, CoasterPathEdge edge, double t) {
        return CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, t, null);
    }

    public static Vec3 sampleEdgePositionPlotLocal(Level level, CoasterPathEdge edge, double t, @Nullable CoasterBezierEdgeSamplingContext bezierContext) {
        if (edge.bezier() != null) {
            BezierConnection bc = edge.bezier();
            if (CoasterPathGraphManager.isCoasterGraphBezier(bc)) {
                float faceT = (float)Mth.clamp((double)t, (double)0.0, (double)1.0);
                Vec3 faceUp = CoasterBezierRailFrames.faceUpAt(bc, faceT, level);
                Vec3 centerline = bezierContext != null ? bezierContext.plotLocalCenterline(t) : CoasterPathGraphManager.coasterBezierPlotLocalCenterline(level, edge, t);
                return centerline.add(faceUp.scale(0.0625));
            }
            return bc.getPosition(t).add(PATH_CENTER_OFFSET);
        }
        if (edge.straightFromWorld() != null && edge.straightToWorld() != null) {
            return edge.straightFromWorld().lerp(edge.straightToWorld(), t).add(PATH_CENTER_OFFSET);
        }
        Vec3 from = CoasterPathGraphManager.sampleNodePositionPlotLocal(level, new CoasterPathNode(edge.from(), Vec3.ZERO));
        Vec3 to = CoasterPathGraphManager.sampleNodePositionPlotLocal(level, new CoasterPathNode(edge.to(), Vec3.ZERO));
        return from.lerp(to, t);
    }

    public static List<CoasterPathEdge> edgesForBezier(CoasterPathGraph graph, BezierConnection bc) {
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
}

