/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.physics.PhysicsPipeline
 *  dev.ryanhcode.sable.api.physics.PhysicsPipelineBody
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.particles.BlockParticleOption
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.core.particles.SimpleParticleType
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.fml.loading.FMLEnvironment
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.Config;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.SimulatedCoastersParticles;
import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientMembership;
import dev.silvergold.simulatedcoasters.compat.PhysicsStaffDragProbe;
import dev.silvergold.simulatedcoasters.physics.UniversalDragCompensation;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBearingTickCache;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBlock;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartChainLiftDrive;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartDuplicateEnforcer;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartPlotScan;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartRailSnapEligibility;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrackHitCache;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import dev.silvergold.simulatedcoasters.track.cart.CoasterTrackGuideConstraint;
import dev.silvergold.simulatedcoasters.track.cart.CoasterTrackStaffRailStabilizer;
import dev.silvergold.simulatedcoasters.track.cart.RailConstraintGeometry;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTraceEndpoints;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class CoasterCartTrackSnap {
    public static final double MAX_TRACK_SNAP_DISTANCE_BLOCKS = 2.5;
    public static final double MAX_TRACK_SNAP_DIST_SQ = 6.25;
    public static final double STAFF_DRAG_ENGAGED_TRACK_SNAP_DISTANCE_BLOCKS = 1.5;
    public static final double STAFF_DRAG_ENGAGED_TRACK_SNAP_DIST_SQ = 2.25;
    public static final double RE_ENGAGE_TRACK_SNAP_DISTANCE_BLOCKS = 1.0;
    public static final double RE_ENGAGE_TRACK_SNAP_DIST_SQ = 1.0;
    public static final double RE_ENGAGE_MAX_NORMAL_ANGLE_DEGREES = 90.0;
    private static final double RE_ENGAGE_MIN_NORMAL_DOT = Math.cos(Math.toRadians(90.0));
    public static final int ENDPOINT_REENGAGE_COOLDOWN_TICKS = 5;
    public static final double OPEN_PATH_END_MATCH_BLOCKS = 0.14;
    private static final double OPEN_PATH_END_MATCH_DIST_SQ = 0.019600000000000003;
    private static final double SNAP_OFFSET_BLOCKS_ALONG_TRACK_UP = 0.3125;
    private static final double GLUE_PEEL_STAFF_RELEASE_BLOCKS = 0.22;
    private static final double GLUE_PEEL_STAFF_RELEASE_DIST_SQ = 0.0484;
    private static final int GLUE_SNAP_SLIME_PARTICLE_COUNT = 28;
    private static final double GLUE_SNAP_SLIME_RING_RADIUS_MIN_BLOCKS = 0.56;
    private static final double GLUE_SNAP_SLIME_RING_RADIUS_SPREAD_BLOCKS = 0.16;
    private static final double GLUE_SNAP_SLIME_PARTICLE_SPEED = 0.18;
    private static final double GLUE_SNAP_SLIME_UPWARD_SPEED_MIN = 0.1;
    private static final double GLUE_SNAP_SLIME_UPWARD_SPEED_SPREAD = 0.1;
    private static final Map<ServerSubLevel, PendingGlueSnapParticles> GLUE_SNAP_PARTICLES_PENDING = Collections.synchronizedMap(new WeakHashMap());
    private static final int RE_ENGAGE_SPARK_COUNT_MAX = 9;
    private static final double RE_ENGAGE_SPARK_COUNT_FULL_BODY_SPEED_BLOCKS = 20.0;
    private static final double RE_ENGAGE_LINE_HALF_LENGTH_TANGENT_BLOCKS = 0.5;
    private static final double RE_ENGAGE_SPAWN_ALONG_NORMAL_BLOCKS = 0.4;
    private static final double RE_ENGAGE_SPAWN_BINORMAL_OFFSET_BLOCKS = 0.6;
    private static final double RE_ENGAGE_VEL_BINORMAL_BASE_BLOCKS = 0.03;
    private static final double RE_ENGAGE_VEL_BINORMAL_PER_TOTAL_BODY_SPEED = 0.01;
    private static final double RE_ENGAGE_VEL_BINORMAL_BODY_SPEED_CAP_BLOCKS = 3.0;
    private static final double RE_ENGAGE_VEL_NORMAL_BLOCKS = 0.06;
    private static final double RE_ENGAGE_VEL_JITTER_TANGENT_BASE_BLOCKS = 0.02;
    private static final double RE_ENGAGE_VEL_JITTER_TANGENT_PER_BODY_SPEED = 0.018;
    private static final double RE_ENGAGE_VEL_JITTER_TANGENT_MAX_HALF_WIDTH_BLOCKS = 0.45;
    private static final double RE_ENGAGE_VEL_JITTER_BINORMAL_MULT_HALF_WIDTH = 0.3;
    private static final double RE_ENGAGE_VEL_JITTER_NORMAL_BLOCKS = 0.05;
    private static final double RE_ENGAGE_SPARK_MIN_BODY_SPEED_BLOCKS = 0.1;
    private static final int PARTICLE_PACKET_COUNT_EXACT = 0;
    private static final double PARTICLE_VELOCITY_PACKET_SPEED = 1.0;
    private static final Map<ServerSubLevel, Boolean> BEARING_DISENGAGED = Collections.synchronizedMap(new WeakHashMap());
    private static final Map<ServerSubLevel, Long> ENDPOINT_REENGAGE_AFTER_GAME_TIME = Collections.synchronizedMap(new WeakHashMap());
    private static final Map<ServerSubLevel, Boolean> ENGAGED_TRAVEL_NEGATES_EDGE_TANGENT = Collections.synchronizedMap(new WeakHashMap());
    private static final Map<ServerSubLevel, Boolean> PHYSICS_STAFF_GRABBING_PREV = Collections.synchronizedMap(new WeakHashMap());
    private static final Map<ServerSubLevel, Boolean> PHYSICS_STAFF_LOCKED_PREV = Collections.synchronizedMap(new WeakHashMap());
    private static final Map<ServerSubLevel, Long> GLUE_PEEL_SOUND_AT_GAME_TIME = Collections.synchronizedMap(new WeakHashMap());

    private CoasterCartTrackSnap() {
    }

    public static double engagedTrackSearchDistSq(boolean wasDisengaged, boolean staffGrabbing) {
        if (wasDisengaged) {
            return 1.0;
        }
        if (staffGrabbing) {
            return 2.25;
        }
        return 6.25;
    }

    private static boolean isTrackHostedOnSubLevel(ServerLevel level, CoasterPathTrackFrame.GraphHit hit) {
        return CoasterPathGraphManager.edgeHostedOnSubLevel((Level)level, hit.edge());
    }

    public static boolean normalsAllowReengage(SubLevel sub, Level level, CoasterPathTrackFrame.GraphHit hit) {
        Vec3 cartUp = CoasterCartTrackSnap.bodyUpWorld(sub);
        Vec3 trackUp = CoasterPathTrackFrame.trackBasisAtHit(level, hit).up();
        if (cartUp.lengthSqr() < 1.0E-12 || trackUp.lengthSqr() < 1.0E-12) {
            return false;
        }
        return cartUp.normalize().dot(trackUp.normalize()) >= RE_ENGAGE_MIN_NORMAL_DOT;
    }

    private static Vec3 bodyUpWorld(SubLevel sub) {
        Vector3d up = new Vector3d(0.0, 1.0, 0.0);
        sub.logicalPose().orientation().transform(up);
        return new Vec3(up.x, up.y, up.z);
    }

    private static void clearEngagementAux(ServerSubLevel sub) {
        ENGAGED_TRAVEL_NEGATES_EDGE_TANGENT.remove(sub);
    }

    private static void tearDownAllRailCouplingForSub(ServerSubLevel sub) {
        CoasterTrackGuideConstraint.remove(sub);
        CoasterCartTrackHitCache.remove(sub);
        BEARING_DISENGAGED.remove(sub);
        ENDPOINT_REENGAGE_AFTER_GAME_TIME.remove(sub);
        CoasterCartTrackSnap.clearEngagementAux(sub);
        PHYSICS_STAFF_GRABBING_PREV.remove(sub);
        PHYSICS_STAFF_LOCKED_PREV.remove(sub);
        GLUE_PEEL_SOUND_AT_GAME_TIME.remove(sub);
        GLUE_SNAP_PARTICLES_PENDING.remove(sub);
    }

    private static Vec3 bodyTravelForwardWorld(Quaterniond bodyOrientation) {
        Vector3d v = new Vector3d(0.0, 0.0, -1.0);
        bodyOrientation.transform(v);
        return new Vec3(v.x, v.y, v.z);
    }

    private static CoasterPathTrackFrame.TrackBasis alignedTrackBasis(CoasterPathTrackFrame.TrackBasis raw, boolean negateEdgeTangent) {
        if (!negateEdgeTangent) {
            return raw;
        }
        return new CoasterPathTrackFrame.TrackBasis(raw.forward().scale(-1.0), raw.right().scale(-1.0), raw.up());
    }

    private static boolean computeTravelNegatesEdgeTangent(ServerSubLevel sub, Level level, CoasterPathTrackFrame.GraphHit graphHit) {
        CoasterPathTrackFrame.TrackBasis raw = CoasterPathTrackFrame.trackBasisAtHit(level, graphHit);
        Vec3 travel = CoasterCartTrackSnap.bodyTravelForwardWorld(sub.logicalPose().orientation());
        return travel.dot(raw.forward()) < 0.0;
    }

    private static boolean negateEdgeTangentForSnap(ServerSubLevel sub, Level level, CoasterPathTrackFrame.GraphHit graphHit) {
        CoasterPathTrackFrame.TrackBasis raw = CoasterPathTrackFrame.trackBasisAtHit(level, graphHit);
        if (!ENGAGED_TRAVEL_NEGATES_EDGE_TANGENT.containsKey(sub)) {
            return CoasterCartTrackSnap.computeTravelNegatesEdgeTangent(sub, level, graphHit);
        }
        boolean lockedNegate = Boolean.TRUE.equals(ENGAGED_TRAVEL_NEGATES_EDGE_TANGENT.get(sub));
        Vec3 travel = CoasterCartTrackSnap.bodyTravelForwardWorld(sub.logicalPose().orientation());
        Vec3 alignedForward = CoasterCartTrackSnap.alignedTrackBasis(raw, lockedNegate).forward();
        if (alignedForward.dot(travel) < 0.0) {
            return !lockedNegate;
        }
        return lockedNegate;
    }

    private static Vec3 idealRepresentativeSnapCenterWorld(ServerLevel level, CoasterPathTrackFrame.GraphHit graphHit, boolean negateEdgeTangent) {
        CoasterPathTrackFrame.TrackBasis basis = CoasterCartTrackSnap.alignedTrackBasis(CoasterPathTrackFrame.trackBasisAtHit((Level)level, graphHit), negateEdgeTangent);
        Vec3 trackUp = basis.up();
        trackUp = trackUp.lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : trackUp.normalize();
        return graphHit.point().add(trackUp.scale(0.3125));
    }

    private static Vec3 projectPerpendicular(Vec3 v, Vec3 unitDir) {
        return v.subtract(unitDir.scale(v.dot(unitDir)));
    }

    private static GlueSeamRingFrame computeGlueSeamRingFrame(ServerLevel sl, ServerSubLevel sub, CoasterPathGraph graph, BlockPos bearingPlotPos, Vec3 bearingWorld, boolean negateEdgeTangent) {
        Vec3 seamTopWorld;
        double seamY = 0.75;
        Vec3 seamTopPlot = new Vec3((double)bearingPlotPos.getX() + 0.5, (double)bearingPlotPos.getY() + 0.75, (double)bearingPlotPos.getZ() + 0.5);
        Vec3 seamBottomWorld = seamTopWorld = sub.logicalPose().transformPosition(seamTopPlot);
        CoasterPathTrackFrame.TrackBasis basis = null;
        if (!graph.edges().isEmpty()) {
            CoasterPathTrackFrame.GraphHit hit = CoasterCartTrackHitCache.nearestGraphHit(sl, graph, sub, bearingWorld, 6.25, new boolean[1]);
            if ((hit = CoasterCartRailSnapEligibility.filterHitForBearing((SubLevel)sub, (Level)sl, hit)) != null) {
                basis = CoasterCartTrackSnap.alignedTrackBasis(CoasterPathTrackFrame.trackBasisAtHit((Level)sl, hit), negateEdgeTangent);
                Quaterniond qPath = CoasterPathTrackFrame.quaternionFromTrackBasis(basis);
                Vec3 pathAnchorBottom = CoasterPathTrackFrame.pathAnchorForSnappedBlock((Level)sl, hit);
                Vector3d off = new Vector3d(0.0, 0.25, 0.0);
                qPath.transform(off);
                seamBottomWorld = pathAnchorBottom.add(off.x, off.y, off.z);
            }
        }
        return CoasterCartTrackSnap.buildGlueSeamRingFrame(sub, seamTopWorld, seamBottomWorld, basis);
    }

    private static GlueSeamRingFrame computePlotLocalGlueSeamRingFrame(ServerSubLevel sub, BlockPos bearingPlotPos) {
        double seamY = 0.75;
        Vec3 seamPlot = new Vec3((double)bearingPlotPos.getX() + 0.5, (double)bearingPlotPos.getY() + 0.75, (double)bearingPlotPos.getZ() + 0.5);
        Vec3 seamWorld = sub.logicalPose().transformPosition(seamPlot);
        return CoasterCartTrackSnap.buildGlueSeamRingFrame(sub, seamWorld, seamWorld, null);
    }

    private static GlueSeamRingFrame buildGlueSeamRingFrame(ServerSubLevel sub, Vec3 seamTopWorld, Vec3 seamBottomWorld, @Nullable CoasterPathTrackFrame.TrackBasis basis) {
        Vec3 axisU;
        Vec3 stretchDir;
        Vec3 center = seamBottomWorld.lerp(seamTopWorld, 0.5);
        Vec3 stretch = seamTopWorld.subtract(seamBottomWorld);
        double stretchLenSq = stretch.lengthSqr();
        if (stretchLenSq > 1.0E-12) {
            stretchDir = stretch.scale(1.0 / Math.sqrt(stretchLenSq));
        } else {
            Vector3d bodyUp = new Vector3d(0.0, 1.0, 0.0);
            sub.logicalPose().orientation().transform(bodyUp);
            stretchDir = new Vec3(bodyUp.x, bodyUp.y, bodyUp.z);
        }
        if (basis != null) {
            axisU = CoasterCartTrackSnap.projectPerpendicular(basis.right(), stretchDir);
            if (axisU.lengthSqr() < 1.0E-12) {
                axisU = CoasterCartTrackSnap.projectPerpendicular(basis.forward(), stretchDir);
            }
        } else {
            Vector3d rx = new Vector3d(1.0, 0.0, 0.0);
            sub.logicalPose().orientation().transform(rx);
            axisU = CoasterCartTrackSnap.projectPerpendicular(new Vec3(rx.x, rx.y, rx.z), stretchDir);
            if (axisU.lengthSqr() < 1.0E-12) {
                Vector3d rz = new Vector3d(0.0, 0.0, 1.0);
                sub.logicalPose().orientation().transform(rz);
                axisU = CoasterCartTrackSnap.projectPerpendicular(new Vec3(rz.x, rz.y, rz.z), stretchDir);
            }
        }
        axisU = axisU.normalize();
        Vec3 axisV = stretchDir.cross(axisU).normalize();
        return new GlueSeamRingFrame(center, axisU, axisV);
    }

    private static void spawnGlueSnapSlimeParticles(ServerLevel level, GlueSeamRingFrame frame) {
        BlockParticleOption options = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SLIME_BLOCK.defaultBlockState());
        RandomSource rand = level.random;
        int n = 28;
        for (int i = 0; i < 28; ++i) {
            double theta = Math.PI * 2 * ((double)i + rand.nextDouble() * 0.4) / 28.0;
            double r = 0.56 + rand.nextDouble() * 0.16;
            Vec3 outward = frame.axisU().scale(Math.cos(theta)).add(frame.axisV().scale(Math.sin(theta)));
            if (outward.lengthSqr() < 1.0E-12) continue;
            Vec3 radial = outward.normalize();
            Vec3 spawn = frame.center().add(radial.scale(r));
            double speed = 0.18 * (0.8 + rand.nextDouble() * 0.45);
            double upSpeed = 0.1 + rand.nextDouble() * 0.1;
            Vec3 vel = radial.scale(speed).add(0.0, upSpeed, 0.0);
            level.sendParticles((ParticleOptions)options, spawn.x, spawn.y, spawn.z, 0, vel.x, vel.y, vel.z, 1.0);
        }
    }

    private static void scheduleGlueSnapParticles(ServerSubLevel sub, long disengageGameTime, BlockPos bearingPlotPos, boolean negateEdgeTangent) {
        CoasterCartTrackSnap.scheduleGlueSnapParticles(sub, disengageGameTime, bearingPlotPos, negateEdgeTangent, false);
    }

    private static void scheduleGlueSnapParticles(ServerSubLevel sub, long disengageGameTime, BlockPos bearingPlotPos, boolean negateEdgeTangent, boolean plotLocalSeamOnly) {
        GLUE_SNAP_PARTICLES_PENDING.put(sub, new PendingGlueSnapParticles(disengageGameTime + 3L, bearingPlotPos.immutable(), negateEdgeTangent, plotLocalSeamOnly));
    }

    private static void tickScheduledGlueSnapParticles(ServerLevel sl, ServerSubLevel sub, CoasterPathGraph graph, long gameTime) {
        PendingGlueSnapParticles pending = GLUE_SNAP_PARTICLES_PENDING.get(sub);
        if (pending == null || gameTime < pending.spawnAtGameTime()) {
            return;
        }
        GLUE_SNAP_PARTICLES_PENDING.remove(sub);
        if (sub.isRemoved()) {
            return;
        }
        BlockPos bearingPlotPos = CoasterCartTrackSnap.representativeBearingPlotPos((SubLevel)sub);
        if (bearingPlotPos == null) {
            return;
        }
        GlueSeamRingFrame frame = pending.plotLocalSeamOnly() ? CoasterCartTrackSnap.computePlotLocalGlueSeamRingFrame(sub, pending.bearingPlotPos()) : CoasterCartTrackSnap.computeGlueSeamRingFrame(sl, sub, graph, pending.bearingPlotPos(), sub.logicalPose().transformPosition(Vec3.atCenterOf((Vec3i)pending.bearingPlotPos())), pending.negateEdgeTangent());
        CoasterCartTrackSnap.spawnGlueSnapSlimeParticles(sl, frame);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void tryPlayGluePeelDisengageSound(ServerLevel sl, ServerSubLevel sub, long gameTime, Vec3 soundPos) {
        Map<ServerSubLevel, Long> map = GLUE_PEEL_SOUND_AT_GAME_TIME;
        synchronized (map) {
            if (Objects.equals(GLUE_PEEL_SOUND_AT_GAME_TIME.get(sub), gameTime)) {
                return;
            }
            GLUE_PEEL_SOUND_AT_GAME_TIME.put(sub, gameTime);
        }
        BlockPos soundPosBlock = BlockPos.containing((Position)soundPos);
        sl.playSound(null, soundPosBlock, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.35f, 0.85f);
        sl.playSound(null, soundPosBlock, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS, 0.45f, 1.0f);
    }

    public static void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        SubLevelPhysicsSystem physics = event.getPhysicsSystem();
        ServerLevel sl = physics.getLevel();
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)sl);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        CoasterPathGraph graph = CoasterPathGraphManager.get(sl);
        boolean hasEdges = !graph.edges().isEmpty();
        List<CoasterPathTraceEndpoints.OpenEndRef> openPathEndRefs = hasEdges ? CoasterCartTrackHitCache.openEndRefs(sl, graph) : List.of();
        long gameTime = sl.getGameTime();
        double dt = event.getTimeStep();
        for (ServerSubLevel sub : container.getAllSubLevels()) {
            boolean normalAllowsReengage;
            boolean negateEdgeTangent;
            Vec3 idealSnap;
            Vec3 bearingWorld;
            Vec3 bearingPlot;
            BlockPos bearingPlotPos;
            if (sub.isRemoved()) {
                CoasterCartTrackSnap.tearDownAllRailCouplingForSub(sub);
                continue;
            }
            if (CoasterCartDuplicateEnforcer.enforceSingleCart(sub)) {
                CoasterCartBearingTickCache.invalidate(sub);
            }
            if ((bearingPlotPos = CoasterCartTrackSnap.representativeBearingPlotPos((SubLevel)sub)) == null) {
                CoasterCartTrackSnap.tearDownAllRailCouplingForSub(sub);
                CoasterCartBlock.syncPlotSnapEngaged(sub, false);
                CoasterCartTrainLinkConstraint.onCartRemoved(sub);
                continue;
            }
            physics.updatePose(sub);
            CoasterCartTrackSnap.tickScheduledGlueSnapParticles(sl, sub, graph, gameTime);
            boolean wasDisengaged = Boolean.TRUE.equals(BEARING_DISENGAGED.get(sub));
            boolean staffFrozenNow = PhysicsStaffDragProbe.isPhysicsStaffLocked(sl, sub);
            boolean staffWasFrozen = Boolean.TRUE.equals(PHYSICS_STAFF_LOCKED_PREV.put(sub, staffFrozenNow));
            if (staffFrozenNow) {
                if (!wasDisengaged) {
                    if (!staffWasFrozen) {
                        bearingPlot = Vec3.atCenterOf((Vec3i)bearingPlotPos);
                        bearingWorld = sub.logicalPose().transformPosition(bearingPlot);
                        if (hasEdges) {
                            boolean negateEdgeTangent2;
                            Vec3 idealSnap2;
                            CoasterPathTrackFrame.GraphHit freezeHit = CoasterCartTrackHitCache.nearestGraphHit(sl, graph, sub, bearingWorld, 6.25, new boolean[1]);
                            if ((freezeHit = CoasterCartRailSnapEligibility.filterHitForBearing((SubLevel)sub, (Level)sl, freezeHit)) != null && bearingWorld.distanceToSqr(idealSnap2 = CoasterCartTrackSnap.idealRepresentativeSnapCenterWorld(sl, freezeHit, negateEdgeTangent2 = CoasterCartTrackSnap.negateEdgeTangentForSnap(sub, (Level)sl, freezeHit))) > 0.0484) {
                                CoasterCartTrackSnap.tryPlayGluePeelDisengageSound(sl, sub, gameTime, bearingWorld);
                                CoasterCartTrackSnap.scheduleGlueSnapParticles(sub, gameTime, bearingPlotPos, negateEdgeTangent2, true);
                            }
                        }
                    }
                    BEARING_DISENGAGED.put(sub, Boolean.TRUE);
                    CoasterCartTrackSnap.clearEngagementAux(sub);
                }
                CoasterTrackGuideConstraint.remove(sub);
                CoasterCartTrackHitCache.clearEngagedTick(sub);
                CoasterCartBlock.syncPlotSnapEngaged(sub, false);
                continue;
            }
            bearingPlot = Vec3.atCenterOf((Vec3i)bearingPlotPos);
            bearingWorld = sub.logicalPose().transformPosition(bearingPlot);
            boolean staffGrabbingNow = PhysicsStaffDragProbe.isPhysicsStaffGrabbing(sl, sub);
            double searchDistSq = CoasterCartTrackSnap.engagedTrackSearchDistSq(wasDisengaged, staffGrabbingNow);
            boolean[] outSubLevelTrack = new boolean[]{false};
            CoasterPathTrackFrame.GraphHit graphHit = hasEdges ? CoasterCartTrackHitCache.nearestGraphHit(sl, graph, sub, bearingWorld, searchDistSq, outSubLevelTrack) : null;
            graphHit = CoasterCartRailSnapEligibility.filterHitForBearing((SubLevel)sub, (Level)sl, graphHit);
            long reengageBlockedUntil = ENDPOINT_REENGAGE_AFTER_GAME_TIME.getOrDefault(sub, 0L);
            boolean endpointCooldownActive = gameTime < reengageBlockedUntil;
            boolean hitIsTerminalOpenPathEnd = graphHit != null && CoasterCartTrackSnap.shouldDisengageAtOpenEndHit((Level)sl, graph, graphHit, openPathEndRefs);
            boolean staffWasGrabbing = Boolean.TRUE.equals(PHYSICS_STAFF_GRABBING_PREV.put(sub, staffGrabbingNow));
            if (graphHit != null && endpointCooldownActive) {
                if (!wasDisengaged) {
                    sl.playSound(null, BlockPos.containing((Position)bearingWorld), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.35f, 0.85f);
                    BEARING_DISENGAGED.put(sub, Boolean.TRUE);
                    CoasterCartTrackSnap.clearEngagementAux(sub);
                }
                CoasterTrackGuideConstraint.remove(sub);
                CoasterCartTrackHitCache.clearEngagedTick(sub);
                CoasterCartBlock.syncPlotSnapEngaged(sub, false);
                continue;
            }
            if (graphHit != null && hitIsTerminalOpenPathEnd) {
                if (!wasDisengaged) {
                    sl.playSound(null, BlockPos.containing((Position)bearingWorld), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.35f, 0.85f);
                    BEARING_DISENGAGED.put(sub, Boolean.TRUE);
                    CoasterCartTrackSnap.clearEngagementAux(sub);
                }
                CoasterTrackGuideConstraint.remove(sub);
                ENDPOINT_REENGAGE_AFTER_GAME_TIME.put(sub, gameTime + 5L);
                CoasterCartTrackHitCache.clearEngagedTick(sub);
                CoasterCartBlock.syncPlotSnapEngaged(sub, false);
                continue;
            }
            if (staffWasGrabbing && !staffGrabbingNow && graphHit != null && !wasDisengaged && bearingWorld.distanceToSqr(idealSnap = CoasterCartTrackSnap.idealRepresentativeSnapCenterWorld(sl, graphHit, negateEdgeTangent = CoasterCartTrackSnap.negateEdgeTangentForSnap(sub, (Level)sl, graphHit))) > 0.0484) {
                CoasterCartTrackSnap.tryPlayGluePeelDisengageSound(sl, sub, gameTime, bearingWorld);
                CoasterCartTrackSnap.scheduleGlueSnapParticles(sub, gameTime, bearingPlotPos, negateEdgeTangent);
            }
            boolean inRange = graphHit != null;
            boolean bl = normalAllowsReengage = !wasDisengaged || graphHit == null || CoasterCartTrackSnap.normalsAllowReengage((SubLevel)sub, (Level)sl, graphHit);
            if (inRange && normalAllowsReengage) {
                boolean firstSnapSession;
                boolean bl2 = firstSnapSession = !ENGAGED_TRAVEL_NEGATES_EDGE_TANGENT.containsKey(sub);
                if (firstSnapSession) {
                    ENGAGED_TRAVEL_NEGATES_EDGE_TANGENT.put(sub, CoasterCartTrackSnap.computeTravelNegatesEdgeTangent(sub, (Level)sl, graphHit));
                }
                boolean negateEdgeTangent3 = CoasterCartTrackSnap.negateEdgeTangentForSnap(sub, (Level)sl, graphHit);
                if (wasDisengaged) {
                    BEARING_DISENGAGED.remove(sub);
                    Vector3d bodyLinVel = physics.getPipeline().getLinearVelocity((PhysicsPipelineBody)sub, new Vector3d());
                    double bodySpeed = bodyLinVel.length();
                    CoasterCartTrackSnap.playReengageEffects(sl, graphHit, bodySpeed, negateEdgeTangent3, !staffGrabbingNow);
                }
                Vec3 bearingPlotLocal = Vec3.atCenterOf((Vec3i)bearingPlotPos);
                if (staffGrabbingNow) {
                    CoasterCartTrackHitCache.clearEngagedTick(sub);
                    CoasterCartTrackSnap.applySnap(sub, sl, physics, graphHit, bearingWorld, bearingPlotLocal, negateEdgeTangent3);
                } else {
                    CoasterCartTrackHitCache.EngagedTickFrame frame = CoasterCartTrackHitCache.engagedTickFrame(sl, graph, sub, graphHit, negateEdgeTangent3, outSubLevelTrack[0]);
                    CoasterCartTrackSnap.updateRailGuideOnly(sub, sl, physics, graphHit, bearingPlotLocal, frame);
                    CoasterCartChainLiftDrive.applySpeedMatch(sub, sl, physics, graphHit, frame.trackBasis(), dt);
                }
                CoasterCartTrackSnap.applyPartialUniversalDragCompensation(sub, sl, physics, dt);
            } else {
                CoasterCartTrackHitCache.clearEngagedTick(sub);
                if (!wasDisengaged) {
                    boolean negateEdgeTangent4 = Boolean.TRUE.equals(ENGAGED_TRAVEL_NEGATES_EDGE_TANGENT.get(sub));
                    GlueSeamRingFrame glueFrame = CoasterCartTrackSnap.computeGlueSeamRingFrame(sl, sub, graph, bearingPlotPos, bearingWorld, negateEdgeTangent4);
                    CoasterCartTrackSnap.tryPlayGluePeelDisengageSound(sl, sub, gameTime, glueFrame.center());
                    if (staffGrabbingNow || staffWasGrabbing) {
                        CoasterCartTrackSnap.scheduleGlueSnapParticles(sub, gameTime, bearingPlotPos, negateEdgeTangent4);
                    }
                    BEARING_DISENGAGED.put(sub, Boolean.TRUE);
                    CoasterCartTrackSnap.clearEngagementAux(sub);
                }
                CoasterTrackGuideConstraint.remove(sub);
            }
            CoasterCartBlock.syncPlotSnapEngaged(sub, inRange && normalAllowsReengage);
        }
        CoasterCartTrainLinkConstraint.tick(physics, sl);
    }

    public static boolean isNearOpenPathEnd(Vec3 worldHit, List<Vec3> openEnds) {
        for (Vec3 ep : openEnds) {
            if (!(worldHit.distanceToSqr(ep) <= 0.019600000000000003)) continue;
            return true;
        }
        return false;
    }

    public static boolean shouldDisengageAtOpenEndHit(Level level, CoasterPathGraph graph, @Nullable CoasterPathTrackFrame.GraphHit hit, List<CoasterPathTraceEndpoints.OpenEndRef> openEndRefs) {
        return CoasterPathTraceEndpoints.shouldDisengageAtOpenEndHit(level, graph, hit, openEndRefs, 0.019600000000000003);
    }

    @Nullable
    public static BlockPos representativeBearingPlotPos(SubLevel sub) {
        ServerSubLevel server;
        ServerLevel level;
        if (sub instanceof ServerSubLevel && (level = (server = (ServerSubLevel)sub).getLevel()) != null && !level.isClientSide()) {
            return CoasterCartBearingTickCache.scan(server).representative();
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return ClientMembership.representative(sub);
        }
        return CoasterCartPlotScan.representativeBearingPlotPos(sub.getPlot(), (Block)SimulatedCoastersBlocks.COASTER_CART_BLOCK.get());
    }

    public static void applyInitialSnapAfterSpawn(ServerSubLevel sub, ServerLevel level, CoasterPathTrackFrame.GraphHit graphHit) {
        BlockPos bearingPlotPos = CoasterCartTrackSnap.representativeBearingPlotPos((SubLevel)sub);
        if (bearingPlotPos == null) {
            return;
        }
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        SubLevelPhysicsSystem physics = container.physicsSystem();
        physics.updatePose(sub);
        Vec3 bearingPlot = Vec3.atCenterOf((Vec3i)bearingPlotPos);
        Vec3 bearingWorld = sub.logicalPose().transformPosition(bearingPlot);
        ENGAGED_TRAVEL_NEGATES_EDGE_TANGENT.put(sub, CoasterCartTrackSnap.computeTravelNegatesEdgeTangent(sub, (Level)level, graphHit));
        boolean negateEdgeTangent = CoasterCartTrackSnap.negateEdgeTangentForSnap(sub, (Level)level, graphHit);
        boolean subLevelTrack = CoasterCartTrackSnap.isTrackHostedOnSubLevel(level, graphHit);
        CoasterPathGraph graph = CoasterPathGraphManager.get(level);
        if (subLevelTrack) {
            CoasterCartTrackHitCache.clearEngagedTick(sub);
        }
        CoasterCartTrackHitCache.EngagedTickFrame frame = CoasterCartTrackHitCache.engagedTickFrame(level, graph, sub, graphHit, negateEdgeTangent, subLevelTrack);
        if (subLevelTrack) {
            CoasterCartTrackSnap.applySnap(sub, level, physics, graphHit, bearingWorld, bearingPlot, negateEdgeTangent);
        } else {
            CoasterCartTrackSnap.updateRailGuideOnly(sub, level, physics, graphHit, bearingPlot, frame);
        }
        CoasterCartBlock.syncPlotSnapEngaged(sub, true);
        CoasterCartTrackSnap.playSnapEngageSound(level, graphHit);
    }

    public static void markSpawnedDisengaged(ServerSubLevel sub) {
        BEARING_DISENGAGED.put(sub, Boolean.TRUE);
        CoasterCartBlock.syncPlotSnapEngaged(sub, false);
    }

    public static boolean isBearingDisengaged(ServerSubLevel sub) {
        return Boolean.TRUE.equals(BEARING_DISENGAGED.get(sub));
    }

    private static double uniformSigned(RandomSource rand) {
        return rand.nextDouble() * 2.0 - 1.0;
    }

    private static Vec3 pathBinormal(CoasterPathTrackFrame.TrackBasis basis) {
        Vec3 n;
        Vec3 t = basis.forward();
        Vec3 b = t.cross(n = basis.up());
        b = b.lengthSqr() < 1.0E-14 ? basis.right() : b.normalize();
        return b;
    }

    private static int reEngageSparkCountForBodySpeed(double bearingBodySpeed) {
        double speedFromBodyAtMin = 0.001;
        double speedFromBodyAtMax = 0.2;
        double speedFromBody = Math.min(0.01 * bearingBodySpeed, 0.2);
        double span = 0.199;
        double t = (speedFromBody - 0.001) / 0.199;
        return Math.max(1, (int)Math.round(9.0 * Math.max(0.0, Math.min(1.0, t))));
    }

    private static double reEngageTangentJitterHalfWidth(double bearingBodySpeed) {
        return Math.min(0.02 + 0.018 * bearingBodySpeed, 0.45);
    }

    private static void playSnapEngageSound(ServerLevel level, CoasterPathTrackFrame.GraphHit graphHit) {
        Vec3 rail = graphHit.point();
        level.playSound(null, rail.x, rail.y, rail.z, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.4f, 1.12f);
    }

    private static void playReengageEffects(ServerLevel level, CoasterPathTrackFrame.GraphHit graphHit, double bearingBodySpeed, boolean negateEdgeTangent, boolean spawnSnapSparkParticles) {
        CoasterCartTrackSnap.playSnapEngageSound(level, graphHit);
        if (!spawnSnapSparkParticles || bearingBodySpeed < 0.1) {
            return;
        }
        CoasterPathTrackFrame.TrackBasis basis = CoasterCartTrackSnap.alignedTrackBasis(CoasterPathTrackFrame.trackBasisAtHit((Level)level, graphHit), negateEdgeTangent);
        Vec3 binormal = CoasterCartTrackSnap.pathBinormal(basis);
        Vec3 normal = basis.up();
        Vec3 tangent = basis.forward();
        Vec3 rail = graphHit.point();
        RandomSource rand = level.random;
        int n = CoasterCartTrackSnap.reEngageSparkCountForBodySpeed(bearingBodySpeed);
        Vec3 base = rail.add(normal.scale(0.4));
        int countFirstSide = (n + 1) / 2;
        int countSecondSide = n - countFirstSide;
        double speedFromBody = Math.min(0.01 * bearingBodySpeed, 3.0);
        double binormalSpeedMag = 0.03 + speedFromBody;
        double tangentJitterHalfWidth = CoasterCartTrackSnap.reEngageTangentJitterHalfWidth(bearingBodySpeed);
        for (int side = 0; side < 2; ++side) {
            double binormalSign = side == 0 ? 1.0 : -1.0;
            int m = side == 0 ? countFirstSide : countSecondSide;
            Vec3 lineAnchor = base.add(binormal.scale(binormalSign * 0.6));
            for (int j = 0; j < m; ++j) {
                double tAlong = m <= 1 ? 0.0 : -0.5 + 1.0 * (double)j / (double)(m - 1);
                Vec3 spawn = lineAnchor.add(tangent.scale(tAlong));
                double jt = CoasterCartTrackSnap.uniformSigned(rand) * tangentJitterHalfWidth;
                double binormalMult = 1.0 + CoasterCartTrackSnap.uniformSigned(rand) * 0.3;
                double jn = CoasterCartTrackSnap.uniformSigned(rand) * 0.05;
                Vec3 vel = tangent.scale(jt).add(binormal.scale(binormalSign * binormalSpeedMag * binormalMult)).add(normal.scale(0.06 + jn));
                level.sendParticles((ParticleOptions)((SimpleParticleType)SimulatedCoastersParticles.TRACK_SNAP_SPARK.get()), spawn.x, spawn.y, spawn.z, 0, vel.x, vel.y, vel.z, 1.0);
            }
        }
    }

    private static Vector3d linearVelocityOfSubLevelAtWorldPoint(SubLevelPhysicsSystem physics, ServerSubLevel body, Vec3 worldPoint) {
        Vector3d vCom = physics.getPipeline().getLinearVelocity((PhysicsPipelineBody)body, new Vector3d());
        Vector3d omega = physics.getPipeline().getAngularVelocity((PhysicsPipelineBody)body, new Vector3d());
        Vector3d com = new Vector3d((Vector3dc)body.logicalPose().position());
        Vector3d r = new Vector3d(worldPoint.x - com.x, worldPoint.y - com.y, worldPoint.z - com.z);
        return vCom.add((Vector3dc)new Vector3d().cross((Vector3dc)omega, r));
    }

    private static Vector3d velocityBumpRemovePerpendicularToTangent(Vector3d v, Vector3d tangentDir) {
        Vector3d t = new Vector3d((Vector3dc)tangentDir);
        double tangentLenSq = t.lengthSquared();
        if (tangentLenSq < 1.0E-20) {
            return new Vector3d();
        }
        t.normalize();
        Vector3d parallel = new Vector3d((Vector3dc)t).mul(v.dot((Vector3dc)t));
        return parallel.sub((Vector3dc)v);
    }

    private static void updateRailGuideOnly(ServerSubLevel sub, ServerLevel sl, SubLevelPhysicsSystem physics, CoasterPathTrackFrame.GraphHit graphHit, Vec3 bearingPlotLocal, CoasterCartTrackHitCache.EngagedTickFrame frame) {
        CoasterTrackStaffRailStabilizer.remove(sub);
        CoasterTrackGuideConstraint.updateAfterSnap(sub, sl, physics, graphHit.edge(), frame.snapTargetWorld(), frame.trackBasis(), bearingPlotLocal);
    }

    private static void applySnap(ServerSubLevel sub, ServerLevel sl, SubLevelPhysicsSystem physics, CoasterPathTrackFrame.GraphHit graphHit, Vec3 bearingWorld, Vec3 bearingPlotLocal, boolean negateEdgeTangent) {
        Vector3d dvLinear;
        Quaterniond qTarget;
        CoasterPathTrackFrame.TrackBasis basis = CoasterCartTrackSnap.alignedTrackBasis(CoasterPathTrackFrame.trackBasisAtHit((Level)sl, graphHit), negateEdgeTangent);
        PhysicsPipeline pipeline = physics.getPipeline();
        Vector3d vBefore = pipeline.getLinearVelocity((PhysicsPipelineBody)sub, new Vector3d());
        Vec3 trackUp = basis.up().lengthSqr() < 1.0E-12 ? new Vec3(0.0, 1.0, 0.0) : basis.up().normalize();
        Vec3 snapTarget = graphHit.point().add(trackUp.scale(0.3125));
        boolean staffGrabbing = PhysicsStaffDragProbe.isPhysicsStaffGrabbing(sl, sub);
        Quaterniond qApply = qTarget = CoasterPathTrackFrame.quaternionFromTrackBasis(basis);
        Quaterniond qWorldJoint = RailConstraintGeometry.quaternionJointWorldFromBasis(basis);
        Vec3 bearingSnapCenter = staffGrabbing ? RailConstraintGeometry.staffBearingSnapCenterWithMinTrackUp(bearingWorld, snapTarget, trackUp, qWorldJoint, 0.0) : snapTarget;
        Vec3 tangentMc = basis.forward();
        Vector3d tangent = new Vector3d(tangentMc.x, tangentMc.y, tangentMc.z);
        ServerSubLevel railHost = CoasterTrackGuideConstraint.resolveTrackHostingSublevel(sl, graphHit.edge());
        if (staffGrabbing) {
            dvLinear = new Vector3d();
        } else if (railHost != null && railHost != sub) {
            Vector3d vRailAtSnap = CoasterCartTrackSnap.linearVelocityOfSubLevelAtWorldPoint(physics, railHost, snapTarget);
            dvLinear = CoasterCartTrackSnap.velocityBumpRemovePerpendicularToTangent(new Vector3d((Vector3dc)vBefore).sub((Vector3dc)vRailAtSnap), tangent);
        } else {
            dvLinear = railHost != null ? new Vector3d() : CoasterCartTrackSnap.velocityBumpRemovePerpendicularToTangent(vBefore, tangent);
        }
        Vector3d delta = new Vector3d(bearingSnapCenter.x - bearingWorld.x, bearingSnapCenter.y - bearingWorld.y, bearingSnapCenter.z - bearingWorld.z);
        Vector3d posOld = new Vector3d((Vector3dc)sub.logicalPose().position());
        Quaterniond qOld = new Quaterniond((Quaterniondc)sub.logicalPose().orientation());
        Vector3d posAfterTranslate = new Vector3d((Vector3dc)posOld).add((Vector3dc)delta);
        Vector3d rotShift = new Vector3d(bearingSnapCenter.x - posAfterTranslate.x, bearingSnapCenter.y - posAfterTranslate.y, bearingSnapCenter.z - posAfterTranslate.z);
        new Quaterniond((Quaterniondc)qOld).invert().transform(rotShift);
        qApply.transform(rotShift);
        Vector3d posFinal = new Vector3d(bearingSnapCenter.x, bearingSnapCenter.y, bearingSnapCenter.z).sub((Vector3dc)rotShift);
        pipeline.teleport((PhysicsPipelineBody)sub, (Vector3dc)posFinal, (Quaterniondc)qApply);
        Vector3d av = pipeline.getAngularVelocity((PhysicsPipelineBody)sub, new Vector3d());
        Vector3d dav = new Vector3d(-av.x, -av.y, -av.z);
        if (dvLinear.lengthSquared() >= 1.0E-18 || dav.lengthSquared() >= 1.0E-18) {
            pipeline.addLinearAndAngularVelocity((PhysicsPipelineBody)sub, (Vector3dc)dvLinear, (Vector3dc)dav);
        }
        Vector3d snapTargetWorld = new Vector3d(snapTarget.x, snapTarget.y, snapTarget.z);
        if (staffGrabbing) {
            CoasterTrackGuideConstraint.removeRigidGuideLocked(sub);
            CoasterTrackStaffRailStabilizer.updateDuringStaffRailSnap(sub, sl, physics, graphHit.edge(), snapTargetWorld, basis, bearingPlotLocal, bearingWorld);
        } else {
            CoasterTrackStaffRailStabilizer.remove(sub);
            CoasterTrackGuideConstraint.updateAfterSnap(sub, sl, physics, graphHit.edge(), snapTargetWorld, basis, bearingPlotLocal);
        }
    }

    private static void applyPartialUniversalDragCompensation(ServerSubLevel sub, ServerLevel sl, SubLevelPhysicsSystem physics, double dt) {
        UniversalDragCompensation.applyPartial(sl, physics, sub, dt, Config.universalDragCompensationFraction());
    }

    private record GlueSeamRingFrame(Vec3 center, Vec3 axisU, Vec3 axisV) {
    }

    private record PendingGlueSnapParticles(long spawnAtGameTime, BlockPos bearingPlotPos, boolean negateEdgeTangent, boolean plotLocalSeamOnly) {
    }

    private static final class ClientMembership {
        private ClientMembership() {
        }

        @Nullable
        static BlockPos representative(SubLevel sub) {
            return CoasterCartClientMembership.representativeBearingPlotPos(sub);
        }
    }
}

