/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.physics.PhysicsPipelineBody
 *  dev.ryanhcode.sable.api.physics.force.ForceGroup
 *  dev.ryanhcode.sable.api.physics.force.ForceGroups
 *  dev.ryanhcode.sable.api.physics.force.ForceTotal
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension
 *  dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent
 *  dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.core.BlockPos
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraft.world.phys.shapes.CollisionContext
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.Config;
import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostIndex;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostKey;
import dev.silvergold.simulatedcoasters.balloon.BalloonLoadReconciler;
import dev.silvergold.simulatedcoasters.balloon.BalloonPlacement;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class BalloonTetherController {
    public static final double TETHER_RADIUS_BLOCKS = 4.0;
    private static final double LIFT_MULTIPLIER = 1.75;
    private static final double TETHER_SPRING_STIFFNESS = 160.0;
    private static final double TETHER_DAMPING = 14.0;
    private static final double HOVER_FULL_CLEARANCE = 3.0;
    private static final double HOVER_CUTOFF_CLEARANCE = 4.5;
    private static final double SWING_LINEAR_DAMPING = 0.0;
    private static final double SWING_ANGULAR_DAMPING = 0.2;
    private static final double UPRIGHT_ALIGN_STRENGTH = 0.4;
    private static final double VERTICAL_HOVER_DAMPING = 0.7;
    private static final double VERTICAL_ALIGNMENT_FORGIVENESS = 0.55;
    private static final Map<UUID, ActiveBalloon> ACTIVE = new ConcurrentHashMap<UUID, ActiveBalloon>();
    private static final Map<ResourceKey<Level>, Long> LAST_RESTORE_GAME_TIME = new ConcurrentHashMap<ResourceKey<Level>, Long>();

    private BalloonTetherController() {
    }

    public static void register(ServerSubLevel balloonSub, BalloonBlockEntity be) {
        if (balloonSub.getLevel() == null) {
            return;
        }
        ACTIVE.put(balloonSub.getUniqueId(), new ActiveBalloon((ResourceKey<Level>)balloonSub.getLevel().dimension(), balloonSub.getUniqueId(), be.hostSubLevelId(), be.hostPos()));
    }

    public static void unregister(UUID balloonSubId) {
        ACTIVE.remove(balloonSubId);
    }

    public static void clearDimension(ResourceKey<Level> dimension) {
        ACTIVE.entrySet().removeIf(e -> ((ActiveBalloon)e.getValue()).dimension.equals((Object)dimension));
        LAST_RESTORE_GAME_TIME.remove(dimension);
    }

    public static void restoreTethers(ServerLevel level) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return;
        }
        ServerSubLevelContainer container = raw;
        for (SubLevel rawSub : container.getAllSubLevels()) {
            ServerSubLevel balloonSub;
            if (!(rawSub instanceof ServerSubLevel) || (balloonSub = (ServerSubLevel)rawSub).isRemoved() || !BalloonResolve.isBalloonSubLevel((SubLevel)balloonSub) || ACTIVE.containsKey(balloonSub.getUniqueId())) continue;
            BalloonLoadReconciler.reconcileBalloon(level, container, balloonSub);
        }
    }

    private static void syncHost(ActiveBalloon active, BalloonBlockEntity be, ServerSubLevel host) {
        BalloonHostKey oldKey = new BalloonHostKey(active.hostSubId, active.hostCell);
        BalloonHostKey newKey = be.hostKey();
        ServerLevel serverLevel = host.getLevel();
        if (!(serverLevel instanceof ServerLevel)) {
            return;
        }
        ServerLevel level = serverLevel;
        if (!oldKey.equals(newKey)) {
            BalloonHostIndex.get(level).rebindHost(oldKey, newKey, active.balloonId);
        }
        active.hostSubId = host.getUniqueId();
        active.hostCell = be.hostPos().immutable();
    }

    @Nullable
    private static BalloonBlockEntity balloonBlockEntity(ServerSubLevel balloonSub) {
        EmbeddedPlotLevelAccessor embedded = balloonSub.getPlot().getEmbeddedLevelAccessor();
        BlockEntity blockEntity = embedded.getBlockEntity(BalloonHostKey.BALLOON_BLOCK_CELL);
        if (blockEntity instanceof BalloonBlockEntity) {
            BalloonBlockEntity be = (BalloonBlockEntity)blockEntity;
            return be;
        }
        return null;
    }

    public static void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
        SubLevelPhysicsSystem physics = event.getPhysicsSystem();
        ServerLevel level = physics.getLevel();
        BalloonTetherController.maybeRestoreTethers(level);
        if (ACTIVE.isEmpty()) {
            return;
        }
        double timeStep = event.getTimeStep();
        HashMap<UUID, Double> hostSupportContribution = new HashMap<UUID, Double>();
        HashMap<UUID, Double> balloonContribution = new HashMap<UUID, Double>();
        for (ActiveBalloon active : ACTIVE.values()) {
            ServerSubLevel host;
            BalloonBlockEntity be;
            ServerSubLevel balloon;
            if (!active.dimension.equals((Object)level.dimension()) || (balloon = BalloonResolve.findBalloonById(level, active.balloonId)) == null || (be = BalloonTetherController.balloonBlockEntity(balloon)) == null || (host = be.resolveHostSubLevel(level)) == null) continue;
            double hostMass = Math.max(host.getMassTracker().getMass(), 0.05);
            double contribution = BalloonTetherController.balloonSupportContribution(level, host, balloon, be.hostPos(), hostMass);
            balloonContribution.put(active.balloonId, contribution);
            hostSupportContribution.merge(host.getUniqueId(), contribution, Double::sum);
        }
        HashSet<UUID> verticalDampedHosts = new HashSet<UUID>();
        for (ActiveBalloon active : List.copyOf(ACTIVE.values())) {
            if (!active.dimension.equals((Object)level.dimension())) continue;
            ServerSubLevel balloon = BalloonResolve.findBalloonById(level, active.balloonId);
            if (balloon == null) {
                if (!BalloonTetherController.isDefunctBalloon(level, active.balloonId)) continue;
                BalloonTetherController.unregister(active.balloonId);
                continue;
            }
            BalloonBlockEntity be = BalloonTetherController.balloonBlockEntity(balloon);
            if (be == null) {
                BalloonTetherController.unregister(active.balloonId);
                continue;
            }
            ServerSubLevel host = be.resolveHostSubLevel(level);
            if (host == null) continue;
            if (!active.hostSubId.equals(host.getUniqueId()) || !active.hostCell.equals((Object)be.hostPos())) {
                BalloonTetherController.syncHost(active, be, host);
            }
            double hostMass = Math.max(host.getMassTracker().getMass(), 0.05);
            double totalContribution = hostSupportContribution.getOrDefault(host.getUniqueId(), 0.0);
            double thisContribution = balloonContribution.getOrDefault(active.balloonId, 0.0);
            double totalSupported = Math.min(hostMass, totalContribution);
            double payloadForBalloon = totalContribution > 1.0E-9 ? totalSupported * (thisContribution / totalContribution) : 0.0;
            BalloonTetherController.applyTetherSpring(physics, host, balloon, active.hostCell, timeStep);
            BalloonTetherController.applyLift(physics, host, balloon, level, timeStep, payloadForBalloon);
            if (!verticalDampedHosts.add(host.getUniqueId())) continue;
            BalloonTetherController.dampHostVerticalInBand(physics, host, level, timeStep);
        }
    }

    private static double balloonSupportContribution(ServerLevel level, ServerSubLevel host, ServerSubLevel balloon, BlockPos hostPlotLocal, double hostMass) {
        Vector3d hostAnchorPlot = BalloonPlacement.hostAnchorPlot((SubLevel)host, hostPlotLocal);
        BlockPos balloonPlotLocal = BalloonHostKey.BALLOON_BLOCK_CELL;
        Vector3d stringTiePlot = BalloonPlacement.stringTiePlot((SubLevel)balloon, balloonPlotLocal);
        Vector3d hostWorld = host.logicalPose().transformPosition((Vector3dc)hostAnchorPlot, new Vector3d());
        Vector3d stringWorld = balloon.logicalPose().transformPosition((Vector3dc)stringTiePlot, new Vector3d());
        Vector3d delta = new Vector3d((Vector3dc)stringWorld).sub((Vector3dc)hostWorld);
        double distance = delta.length();
        if (distance < 1.0E-6) {
            return 0.0;
        }
        double rawVertical = Math.max(0.0, delta.y / distance);
        double verticalAlignment = rawVertical > 0.0 ? Math.pow(rawVertical, 0.55) : 0.0;
        double horizontalOffset = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        double horizontalAlignment = Math.max(0.0, 1.0 - horizontalOffset / 4.0);
        double tautness = Math.min(1.0, distance / 4.0);
        double alignmentFactor = verticalAlignment * horizontalAlignment * tautness;
        double hoverFactor = BalloonTetherController.groundHoverFactor(level, host, hostWorld);
        double perBalloonStrength = Math.min(Config.maxBalloonPayloadMass(), hostMass);
        return perBalloonStrength * alignmentFactor * hoverFactor;
    }

    private static double groundHoverFactor(ServerLevel level, ServerSubLevel host, Vector3d hostWorld) {
        double bottomY = host.boundingBox().minY();
        Vec3 from = new Vec3(hostWorld.x, bottomY, hostWorld.z);
        Vec3 to = new Vec3(hostWorld.x, bottomY - 4.5, hostWorld.z);
        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        ((ClipContextExtension)ctx).sable$setIgnoredSubLevel((SubLevel)host);
        BlockHitResult hit = level.clip(ctx);
        if (hit.getType() == HitResult.Type.MISS) {
            return 0.0;
        }
        double clearance = bottomY - hit.getLocation().y;
        if (clearance <= 3.0) {
            return 1.0;
        }
        if (clearance >= 4.5) {
            return 0.0;
        }
        return (4.5 - clearance) / 1.5;
    }

    private static void maybeRestoreTethers(ServerLevel level) {
        long gameTime = level.getGameTime();
        ResourceKey dimension = level.dimension();
        Long last = LAST_RESTORE_GAME_TIME.get(dimension);
        if (last != null && last == gameTime) {
            return;
        }
        LAST_RESTORE_GAME_TIME.put((ResourceKey<Level>)dimension, gameTime);
        BalloonTetherController.restoreTethers(level);
    }

    private static boolean isDefunctBalloon(ServerLevel level, UUID balloonId) {
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return false;
        }
        ServerSubLevelContainer container = raw;
        SubLevel sub = container.getSubLevel(balloonId);
        if (sub == null) {
            return false;
        }
        return sub.isRemoved() || !BalloonResolve.isBalloonSubLevel(sub);
    }

    private static void applyTetherSpring(SubLevelPhysicsSystem physics, ServerSubLevel host, ServerSubLevel balloon, BlockPos hostPlotLocal, double timeStep) {
        if (timeStep <= 0.0) {
            return;
        }
        Vector3d hostAnchorPlot = BalloonPlacement.hostAnchorPlot((SubLevel)host, hostPlotLocal);
        BlockPos balloonPlotLocal = BalloonHostKey.BALLOON_BLOCK_CELL;
        Vector3d stringTiePlot = BalloonPlacement.stringTiePlot((SubLevel)balloon, balloonPlotLocal);
        Vector3d hostWorld = host.logicalPose().transformPosition((Vector3dc)hostAnchorPlot, new Vector3d());
        Vector3d stringWorld = balloon.logicalPose().transformPosition((Vector3dc)stringTiePlot, new Vector3d());
        Vector3d hostToString = new Vector3d((Vector3dc)stringWorld).sub((Vector3dc)hostWorld);
        double distance = hostToString.length();
        double stretch = distance - 4.0;
        if (stretch <= 0.0) {
            return;
        }
        if (distance < 1.0E-8) {
            hostToString.set(0.0, 1.0, 0.0);
        } else {
            hostToString.div(distance);
        }
        double balloonMass = Math.max(balloon.getMassTracker().getMass(), 0.05);
        Vector3d worldImpulse = new Vector3d((Vector3dc)hostToString).mul(-stretch * 160.0 * balloonMass * timeStep);
        Vector3d vBalloon = physics.getPipeline().getLinearVelocity((PhysicsPipelineBody)balloon, new Vector3d());
        Vector3d wBalloon = physics.getPipeline().getAngularVelocity((PhysicsPipelineBody)balloon, new Vector3d());
        Vector3d tieOffsetPlot = new Vector3d((Vector3dc)stringTiePlot).sub((Vector3dc)balloon.logicalPose().rotationPoint());
        Vector3d tieOffsetWorld = new Quaterniond((Quaterniondc)balloon.logicalPose().orientation()).transform((Vector3dc)tieOffsetPlot, new Vector3d());
        Vector3d vString = new Vector3d((Vector3dc)wBalloon).cross((Vector3dc)tieOffsetWorld).add((Vector3dc)vBalloon);
        Vector3d vHost = physics.getPipeline().getLinearVelocity((PhysicsPipelineBody)host, new Vector3d());
        Vector3d wHost = physics.getPipeline().getAngularVelocity((PhysicsPipelineBody)host, new Vector3d());
        Vector3d anchorOffsetPlot = new Vector3d((Vector3dc)hostAnchorPlot).sub((Vector3dc)host.logicalPose().rotationPoint());
        Vector3d anchorOffsetWorld = new Quaterniond((Quaterniondc)host.logicalPose().orientation()).transform((Vector3dc)anchorOffsetPlot, new Vector3d());
        Vector3d vHostAnchor = new Vector3d((Vector3dc)wHost).cross((Vector3dc)anchorOffsetWorld).add((Vector3dc)vHost);
        double relativeOutward = vString.dot((Vector3dc)hostToString) - vHostAnchor.dot((Vector3dc)hostToString);
        if (relativeOutward > 0.0) {
            worldImpulse.fma(-14.0 * relativeOutward * balloonMass * timeStep, (Vector3dc)hostToString, worldImpulse);
        }
        Vector3d balloonLocalImpulse = new Quaterniond((Quaterniondc)balloon.logicalPose().orientation()).conjugate().transform((Vector3dc)worldImpulse, new Vector3d());
        Vector3d hostLocalImpulse = new Quaterniond((Quaterniondc)host.logicalPose().orientation()).conjugate().transform((Vector3dc)new Vector3d((Vector3dc)worldImpulse).negate(), new Vector3d());
        ForceGroup forceGroup = (ForceGroup)ForceGroups.BALLOON_LIFT.get();
        balloon.getOrCreateQueuedForceGroup(forceGroup).applyAndRecordPointForce((Vector3dc)stringTiePlot, (Vector3dc)balloonLocalImpulse);
        host.getOrCreateQueuedForceGroup(forceGroup).applyAndRecordPointForce((Vector3dc)hostAnchorPlot, (Vector3dc)hostLocalImpulse);
        physics.getPipeline().wakeUp((PhysicsPipelineBody)balloon);
        physics.getPipeline().wakeUp((PhysicsPipelineBody)host);
    }

    private static void applyLift(SubLevelPhysicsSystem physics, ServerSubLevel host, ServerSubLevel balloon, ServerLevel level, double timeStep, double supportedPayload) {
        Vector3d position = balloon.logicalPose().position();
        Vector3d gravity = new Vector3d((Vector3dc)DimensionPhysicsData.getGravity((Level)level, (Vector3dc)position));
        double g = gravity.length();
        if (g < 1.0E-8) {
            return;
        }
        Vector3d up = new Vector3d((Vector3dc)gravity).normalize().negate();
        double balloonMass = Math.max(balloon.getMassTracker().getMass(), 0.05);
        Vector3d liftPointPlot = BalloonPlacement.liftPointPlot((SubLevel)balloon, BalloonHostKey.BALLOON_BLOCK_CELL);
        ForceGroup forceGroup = (ForceGroup)ForceGroups.BALLOON_LIFT.get();
        Quaterniond balloonOrientation = new Quaterniond((Quaterniondc)balloon.logicalPose().orientation()).conjugate();
        Quaterniond hostOrientation = new Quaterniond((Quaterniondc)host.logicalPose().orientation()).conjugate();
        Vector3d balloonHoverImpulse = new Vector3d((Vector3dc)up).mul(balloonMass * g * 1.75 * timeStep);
        Vector3d balloonLocalHover = balloonOrientation.transform((Vector3dc)balloonHoverImpulse, new Vector3d());
        balloon.getOrCreateQueuedForceGroup(forceGroup).applyAndRecordPointForce((Vector3dc)liftPointPlot, (Vector3dc)balloonLocalHover);
        if (supportedPayload > 0.0) {
            Vector3d payloadImpulse = new Vector3d((Vector3dc)up).mul(supportedPayload * g * timeStep);
            Vector3d hostLocalAssist = hostOrientation.transform((Vector3dc)payloadImpulse, new Vector3d());
            host.getOrCreateQueuedForceGroup(forceGroup).getForceTotal().applyLinearImpulse((Vector3dc)hostLocalAssist);
            double hostMass = Math.max(host.getMassTracker().getMass(), 0.05);
            double supportFraction = Math.min(1.0, supportedPayload / hostMass);
            BalloonTetherController.dampHostSwing(physics, host, hostOrientation, up, supportFraction, timeStep);
            BalloonTetherController.uprightHostAlignment(physics, host, hostOrientation, up, supportFraction, timeStep);
        }
        physics.getPipeline().wakeUp((PhysicsPipelineBody)balloon);
        physics.getPipeline().wakeUp((PhysicsPipelineBody)host);
    }

    private static void dampHostSwing(SubLevelPhysicsSystem physics, ServerSubLevel host, Quaterniond hostOrientationConj, Vector3d up, double supportFraction, double timeStep) {
        if (supportFraction <= 0.0 || timeStep <= 0.0) {
            return;
        }
        ForceTotal forceTotal = host.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.BALLOON_LIFT.get()).getForceTotal();
        double hostMass = Math.max(host.getMassTracker().getMass(), 0.05);
        Vector3d vWorld = physics.getPipeline().getLinearVelocity((PhysicsPipelineBody)host, new Vector3d());
        Vector3d vHorizontal = new Vector3d((Vector3dc)vWorld).fma(-vWorld.dot((Vector3dc)up), (Vector3dc)up);
        double linearScale = Math.min(1.0, 0.0 * supportFraction * timeStep);
        Vector3d linearImpulseLocal = hostOrientationConj.transform((Vector3dc)new Vector3d((Vector3dc)vHorizontal).mul(-linearScale * hostMass), new Vector3d());
        forceTotal.applyLinearImpulse((Vector3dc)linearImpulseLocal);
        Vector3d wWorld = physics.getPipeline().getAngularVelocity((PhysicsPipelineBody)host, new Vector3d());
        Vector3d wLocal = hostOrientationConj.transform((Vector3dc)wWorld, new Vector3d());
        double angularScale = Math.min(1.0, 0.2 * supportFraction * timeStep);
        Vector3d angularMomentumLocal = host.getMassTracker().getInertiaTensor().transform((Vector3dc)wLocal, new Vector3d());
        forceTotal.applyAngularImpulse((Vector3dc)new Vector3d((Vector3dc)angularMomentumLocal).mul(-angularScale));
        physics.getPipeline().wakeUp((PhysicsPipelineBody)host);
    }

    private static void uprightHostAlignment(SubLevelPhysicsSystem physics, ServerSubLevel host, Quaterniond hostOrientationConj, Vector3d worldUp, double supportFraction, double timeStep) {
        if (supportFraction <= 0.0 || timeStep <= 0.0) {
            return;
        }
        Quaterniond hostOrient = new Quaterniond((Quaterniondc)hostOrientationConj).conjugate();
        Vector3d localYWorld = hostOrient.transform((Vector3dc)new Vector3d(0.0, 1.0, 0.0), new Vector3d());
        Vector3d errorAxis = new Vector3d((Vector3dc)localYWorld).cross((Vector3dc)worldUp);
        if (errorAxis.lengthSquared() < 1.0E-10) {
            return;
        }
        Vector3d correctiveTorqueWorld = errorAxis.mul(0.4 * supportFraction);
        Vector3d correctiveTorqueLocal = hostOrientationConj.transform((Vector3dc)correctiveTorqueWorld, new Vector3d());
        Vector3d angularImpulseLocal = host.getMassTracker().getInertiaTensor().transform((Vector3dc)correctiveTorqueLocal, new Vector3d()).mul(timeStep);
        host.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.BALLOON_LIFT.get()).getForceTotal().applyAngularImpulse((Vector3dc)angularImpulseLocal);
        physics.getPipeline().wakeUp((PhysicsPipelineBody)host);
    }

    private static void dampHostVerticalInBand(SubLevelPhysicsSystem physics, ServerSubLevel host, ServerLevel level, double timeStep) {
        double centerZ;
        if (timeStep <= 0.0) {
            return;
        }
        double centerX = (host.boundingBox().minX() + host.boundingBox().maxX()) * 0.5;
        double hoverFactor = BalloonTetherController.groundHoverFactor(level, host, new Vector3d(centerX, 0.0, centerZ = (host.boundingBox().minZ() + host.boundingBox().maxZ()) * 0.5));
        if (hoverFactor <= 0.0 || hoverFactor >= 1.0) {
            return;
        }
        Vector3d gravity = new Vector3d((Vector3dc)DimensionPhysicsData.getGravity((Level)level, (Vector3dc)host.logicalPose().position()));
        double g = gravity.length();
        if (g < 1.0E-8) {
            return;
        }
        Vector3d up = new Vector3d((Vector3dc)gravity).normalize().negate();
        double bandness = 4.0 * hoverFactor * (1.0 - hoverFactor);
        double scale = Math.min(1.0, 0.7 * bandness * timeStep);
        if (scale <= 0.0) {
            return;
        }
        double hostMass = Math.max(host.getMassTracker().getMass(), 0.05);
        Vector3d vWorld = physics.getPipeline().getLinearVelocity((PhysicsPipelineBody)host, new Vector3d());
        double verticalSpeed = vWorld.dot((Vector3dc)up);
        Vector3d impulseWorld = new Vector3d((Vector3dc)up).mul(-verticalSpeed * scale * hostMass);
        Quaterniond hostOrientationConj = new Quaterniond((Quaterniondc)host.logicalPose().orientation()).conjugate();
        Vector3d impulseLocal = hostOrientationConj.transform((Vector3dc)impulseWorld, new Vector3d());
        host.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.BALLOON_LIFT.get()).getForceTotal().applyLinearImpulse((Vector3dc)impulseLocal);
        physics.getPipeline().wakeUp((PhysicsPipelineBody)host);
    }

    private static final class ActiveBalloon {
        final ResourceKey<Level> dimension;
        final UUID balloonId;
        UUID hostSubId;
        BlockPos hostCell;

        ActiveBalloon(ResourceKey<Level> dimension, UUID balloonId, UUID hostSubId, BlockPos hostCell) {
            this.dimension = dimension;
            this.balloonId = balloonId;
            this.hostSubId = hostSubId;
            this.hostCell = hostCell.immutable();
        }
    }
}

