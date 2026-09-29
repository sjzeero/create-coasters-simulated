/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.companion.math.Pose3dc
 *  dev.ryanhcode.sable.sublevel.ClientSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Quaternionf
 *  org.joml.Vector3f
 */
package dev.silvergold.simulatedcoasters.client.cart;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientEngagement;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartStaffStretchRenderGate;
import dev.silvergold.simulatedcoasters.compat.PhysicsStaffClientDragProbe;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartRailSnapEligibility;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrackSnap;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTraceEndpoints;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartClientFrameCache {
    private static long tickScopeGameTime = Long.MIN_VALUE;
    private static final Map<UUID, CoasterPathTrackFrame.GraphHit> ENGAGEMENT_HIT_BY_SUB = new HashMap<UUID, CoasterPathTrackFrame.GraphHit>();
    private static final Map<UUID, Boolean> SUBLEVEL_HOSTED_TRACK = new HashMap<UUID, Boolean>();
    private static final Map<UUID, SoundFrame> SOUND_BY_SUB = new HashMap<UUID, SoundFrame>();
    private static long renderScopeGameTime = Long.MIN_VALUE;
    private static int renderScopePartialBits = Integer.MIN_VALUE;
    private static final Map<UUID, RenderFrame> RENDER_BY_SUB = new HashMap<UUID, RenderFrame>();

    private CoasterCartClientFrameCache() {
    }

    static void clearSubLevelTrackAffinity(UUID id) {
        SUBLEVEL_HOSTED_TRACK.remove(id);
    }

    public static void beginClientTick(ClientLevel level) {
        long tick = level.getGameTime();
        if (tick == tickScopeGameTime) {
            return;
        }
        tickScopeGameTime = tick;
        ENGAGEMENT_HIT_BY_SUB.clear();
        SOUND_BY_SUB.clear();
        RENDER_BY_SUB.clear();
        renderScopeGameTime = Long.MIN_VALUE;
        renderScopePartialBits = Integer.MIN_VALUE;
    }

    public static void beginRenderFrame(ClientLevel level, float partialTick) {
        long tick = level.getGameTime();
        int ptBits = Float.floatToIntBits(partialTick);
        if (tick == renderScopeGameTime && ptBits == renderScopePartialBits) {
            return;
        }
        renderScopeGameTime = tick;
        renderScopePartialBits = ptBits;
        RENDER_BY_SUB.clear();
    }

    @Nullable
    public static SoundFrame resolveSound(ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs) {
        if (sub.isRemoved()) {
            CoasterCartClientEngagement.clearSubState(sub.getUniqueId());
            CoasterCartStaffStretchRenderGate.clearSub(sub.getUniqueId());
            return null;
        }
        BlockPos rep = CoasterCartTrackSnap.representativeBearingPlotPos(sub);
        if (rep == null) {
            return null;
        }
        return SOUND_BY_SUB.computeIfAbsent(sub.getUniqueId(), id -> CoasterCartClientFrameCache.computeSound(level, sub, pathGraph, openEndRefs, rep));
    }

    @Nullable
    public static RenderFrame resolveRender(ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs, float partialTick) {
        if (sub.isRemoved()) {
            CoasterCartClientEngagement.clearSubState(sub.getUniqueId());
            CoasterCartStaffStretchRenderGate.clearSub(sub.getUniqueId());
            return null;
        }
        BlockPos rep = CoasterCartTrackSnap.representativeBearingPlotPos(sub);
        if (rep == null) {
            return null;
        }
        return RENDER_BY_SUB.computeIfAbsent(sub.getUniqueId(), id -> CoasterCartClientFrameCache.computeRender(level, sub, pathGraph, openEndRefs, rep, partialTick));
    }

    private static SoundFrame computeSound(ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs, BlockPos rep) {
        Vec3 bearingWorld = sub.logicalPose().transformPosition(Vec3.atCenterOf((Vec3i)rep));
        if (PhysicsStaffClientDragProbe.isPhysicsStaffLocked((Level)level, sub)) {
            CoasterCartClientEngagement.forceDisengaged(sub.getUniqueId());
            return new SoundFrame(bearingWorld, false);
        }
        if (CoasterCartClientEngagement.isSnapEngagedBlock((Level)level, rep)) {
            return new SoundFrame(bearingWorld, true);
        }
        CoasterPathTrackFrame.GraphHit hit = CoasterCartClientFrameCache.engagementHitForTick(level, sub, pathGraph, bearingWorld);
        CoasterCartClientEngagement.applyEngagementState(level, pathGraph, sub, sub.getUniqueId(), hit, openEndRefs);
        boolean onRail = hit != null && !CoasterCartTrackSnap.shouldDisengageAtOpenEndHit((Level)level, pathGraph, hit, openEndRefs);
        return new SoundFrame(bearingWorld, onRail);
    }

    private static RenderFrame computeRender(ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs, BlockPos rep, float partialTick) {
        boolean clientEngaged;
        boolean staffStretchRendering;
        Vec3 plotCenter = Vec3.atCenterOf((Vec3i)rep);
        Vec3 sampleLogical = sub.logicalPose().transformPosition(plotCenter);
        Pose3d visualPose = CoasterCartClientFrameCache.clientVisualPose(sub, partialTick);
        Vec3 sampleVisual = visualPose.transformPosition(plotCenter);
        boolean snapEngaged = CoasterCartClientFrameCache.isSnapEngagedBlock((Level)level, rep);
        boolean staffFrozen = PhysicsStaffClientDragProbe.isPhysicsStaffLocked((Level)level, sub);
        boolean staffGrabbing = PhysicsStaffClientDragProbe.isAnyPhysicsStaffGrabbing((Level)level, sub);
        boolean bl = staffStretchRendering = !staffFrozen && CoasterCartStaffStretchRenderGate.stretchRendering(level, sub, snapEngaged, staffGrabbing);
        if (staffFrozen) {
            CoasterCartClientEngagement.forceDisengaged(sub.getUniqueId());
            clientEngaged = false;
        } else {
            clientEngaged = snapEngaged ? true : CoasterCartClientEngagement.isClientEngaged(sub.getUniqueId());
        }
        CoasterPathTrackFrame.GraphHit trackHit = null;
        CoasterPathTrackFrame.TrackBasis trackBasis = null;
        Vec3 pathAnchorBottom = null;
        Quaternionf qPath = null;
        if (staffStretchRendering && !pathGraph.edges().isEmpty()) {
            double trackSearchSq = 2.25;
            trackHit = CoasterPathTrackFrame.nearestGraphHit((Level)level, pathGraph, sampleVisual, trackSearchSq, Double.valueOf(partialTick));
            if (trackHit == null) {
                trackHit = CoasterPathTrackFrame.nearestGraphHit((Level)level, pathGraph, sampleLogical, trackSearchSq);
            }
            if ((trackHit = CoasterCartRailSnapEligibility.filterHitForBearing(sub, (Level)level, trackHit)) != null) {
                trackBasis = CoasterPathTrackFrame.trackBasisAtHit((Level)level, trackHit, partialTick);
                pathAnchorBottom = CoasterPathTrackFrame.pathAnchorForSnappedBlock((Level)level, trackHit, partialTick);
                Quaterniond qd = CoasterPathTrackFrame.quaternionFromTrackBasis(trackBasis);
                qPath = new Quaternionf((float)qd.x, (float)qd.y, (float)qd.z, (float)qd.w);
            }
        }
        Quaterniond qVisual = visualPose.orientation();
        Quaternionf qBody = new Quaternionf((float)qVisual.x, (float)qVisual.y, (float)qVisual.z, (float)qVisual.w);
        return new RenderFrame(rep, visualPose, sampleLogical, sampleVisual, snapEngaged, clientEngaged, staffStretchRendering, trackHit, trackBasis, pathAnchorBottom, qPath, qBody);
    }

    static Pose3d clientVisualPose(SubLevel sub, float partialTick) {
        Pose3d visualPose = new Pose3d();
        if (sub instanceof ClientSubLevel) {
            ClientSubLevel clientSub = (ClientSubLevel)sub;
            Pose3dc rp = clientSub.renderPose(partialTick);
            visualPose.position().set(rp.position());
            visualPose.orientation().set(rp.orientation());
            visualPose.rotationPoint().set(rp.rotationPoint());
            return visualPose;
        }
        sub.lastPose().lerp((Pose3dc)sub.logicalPose(), (double)partialTick, visualPose);
        return visualPose;
    }

    static Vec3 bodyTravelForwardWorld(Quaternionf bodyOrientation) {
        Vector3f v = new Vector3f(0.0f, 0.0f, -1.0f);
        bodyOrientation.transform(v);
        return new Vec3((double)v.x, (double)v.y, (double)v.z);
    }

    private static CoasterPathTrackFrame.GraphHit engagementHitForTick(ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, Vec3 bearingWorld) {
        UUID id = sub.getUniqueId();
        if (Boolean.TRUE.equals(SUBLEVEL_HOSTED_TRACK.get(id))) {
            return CoasterCartClientFrameCache.queryEngagementHit(level, sub, pathGraph, bearingWorld, id);
        }
        return ENGAGEMENT_HIT_BY_SUB.computeIfAbsent(id, i -> CoasterCartClientFrameCache.queryEngagementHit(level, sub, pathGraph, bearingWorld, id));
    }

    @Nullable
    private static CoasterPathTrackFrame.GraphHit queryEngagementHit(ClientLevel level, SubLevel sub, CoasterPathGraph pathGraph, Vec3 bearingWorld, UUID subId) {
        if (pathGraph.edges().isEmpty()) {
            return null;
        }
        boolean staffGrabbing = PhysicsStaffClientDragProbe.isAnyPhysicsStaffGrabbing((Level)level, sub);
        double searchSq = CoasterCartClientEngagement.engagementSearchDistSq(subId, staffGrabbing);
        CoasterPathTrackFrame.GraphHit hit = CoasterPathTrackFrame.nearestGraphHit((Level)level, pathGraph, bearingWorld, searchSq);
        if ((hit = CoasterCartRailSnapEligibility.filterHitForBearing(sub, (Level)level, hit)) != null) {
            SUBLEVEL_HOSTED_TRACK.put(subId, CoasterPathGraphManager.edgeHostedOnSubLevel((Level)level, hit.edge()));
        } else {
            SUBLEVEL_HOSTED_TRACK.remove(subId);
        }
        if (Boolean.TRUE.equals(SUBLEVEL_HOSTED_TRACK.get(subId))) {
            ENGAGEMENT_HIT_BY_SUB.remove(subId);
        }
        return hit;
    }

    private static boolean isSnapEngagedBlock(Level level, BlockPos bearingCell) {
        CoasterCartBlock bearingBlock;
        BlockState st = level.getBlockState(bearingCell);
        return st.is((Block)(bearingBlock = (CoasterCartBlock)((Object)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get()))) && (Boolean)st.getValue((Property)CoasterCartBlock.SNAP_ENGAGED) != false;
    }

    public record SoundFrame(Vec3 bearingWorldLogical, boolean onRail) {
    }

    public record RenderFrame(BlockPos representativePlotPos, Pose3d visualPose, Vec3 sampleLogical, Vec3 sampleVisual, boolean snapEngaged, boolean clientEngaged, boolean staffStretchRendering, @Nullable CoasterPathTrackFrame.GraphHit trackHit, @Nullable CoasterPathTrackFrame.TrackBasis trackBasis, @Nullable Vec3 pathAnchorBottom, @Nullable Quaternionf qPath, Quaternionf qBody) {
        public boolean trackLocked() {
            return this.trackHit != null;
        }

        public boolean drawStaffGlue() {
            return this.staffStretchRendering && this.trackLocked();
        }

        public boolean plotPoseAlignedExtras() {
            return this.snapEngaged && !this.trackLocked();
        }
    }
}

