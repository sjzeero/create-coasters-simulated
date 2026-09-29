package dev.silvergold.simulatedcoasters.balloon;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

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
   private static final Map<UUID, BalloonTetherController.ActiveBalloon> ACTIVE = new ConcurrentHashMap<>();
   private static final Map<ResourceKey<Level>, Long> LAST_RESTORE_GAME_TIME = new ConcurrentHashMap<>();

   private BalloonTetherController() {
   }

   public static void register(ServerSubLevel balloonSub, BalloonBlockEntity be) {
      if (balloonSub.getLevel() != null) {
         ACTIVE.put(
            balloonSub.getUniqueId(),
            new BalloonTetherController.ActiveBalloon(balloonSub.getLevel().dimension(), balloonSub.getUniqueId(), be.hostSubLevelId(), be.hostPos())
         );
      }
   }

   public static void unregister(UUID balloonSubId) {
      ACTIVE.remove(balloonSubId);
   }

   public static void clearDimension(ResourceKey<Level> dimension) {
      ACTIVE.entrySet().removeIf(e -> e.getValue().dimension.equals(dimension));
      LAST_RESTORE_GAME_TIME.remove(dimension);
   }

   public static void restoreTethers(ServerLevel level) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         for (SubLevel rawSub : container.getAllSubLevels()) {
            if (rawSub instanceof ServerSubLevel) {
               ServerSubLevel balloonSub = (ServerSubLevel)rawSub;
               if (!balloonSub.isRemoved() && BalloonResolve.isBalloonSubLevel(balloonSub) && !ACTIVE.containsKey(balloonSub.getUniqueId())) {
                  BalloonLoadReconciler.reconcileBalloon(level, container, balloonSub);
               }
            }
         }
      }
   }

   private static void syncHost(BalloonTetherController.ActiveBalloon active, BalloonBlockEntity be, ServerSubLevel host) {
      BalloonHostKey oldKey = new BalloonHostKey(active.hostSubId, active.hostCell);
      BalloonHostKey newKey = be.hostKey();
      ServerLevel var6 = host.getLevel();
      if (var6 instanceof ServerLevel) {
         if (!oldKey.equals(newKey)) {
            BalloonHostIndex.get(var6).rebindHost(oldKey, newKey, active.balloonId);
         }

         active.hostSubId = host.getUniqueId();
         active.hostCell = be.hostPos().immutable();
      }
   }

   @Nullable
   private static BalloonBlockEntity balloonBlockEntity(ServerSubLevel balloonSub) {
      EmbeddedPlotLevelAccessor embedded = balloonSub.getPlot().getEmbeddedLevelAccessor();
      BlockEntity var3 = embedded.getBlockEntity(BalloonHostKey.BALLOON_BLOCK_CELL);
      return var3 instanceof BalloonBlockEntity ? (BalloonBlockEntity)var3 : null;
   }

   public static void onPrePhysicsTick(ForgeSablePrePhysicsTickEvent event) {
      SubLevelPhysicsSystem physics = event.getPhysicsSystem();
      ServerLevel level = physics.getLevel();
      maybeRestoreTethers(level);
      if (!ACTIVE.isEmpty()) {
         double timeStep = event.getTimeStep();
         Map<UUID, Double> hostSupportContribution = new HashMap<>();
         Map<UUID, Double> balloonContribution = new HashMap<>();

         for (BalloonTetherController.ActiveBalloon active : ACTIVE.values()) {
            if (active.dimension.equals(level.dimension())) {
               ServerSubLevel balloon = BalloonResolve.findBalloonById(level, active.balloonId);
               if (balloon != null) {
                  BalloonBlockEntity be = balloonBlockEntity(balloon);
                  if (be != null) {
                     ServerSubLevel host = be.resolveHostSubLevel(level);
                     if (host != null) {
                        double hostMass = Math.max(host.getMassTracker().getMass(), 0.05);
                        double contribution = balloonSupportContribution(level, host, balloon, be.hostPos(), hostMass);
                        balloonContribution.put(active.balloonId, contribution);
                        hostSupportContribution.merge(host.getUniqueId(), contribution, Double::sum);
                     }
                  }
               }
            }
         }

         Set<UUID> verticalDampedHosts = new HashSet<>();

         for (BalloonTetherController.ActiveBalloon activex : List.copyOf(ACTIVE.values())) {
            if (activex.dimension.equals(level.dimension())) {
               ServerSubLevel balloon = BalloonResolve.findBalloonById(level, activex.balloonId);
               if (balloon == null) {
                  if (isDefunctBalloon(level, activex.balloonId)) {
                     unregister(activex.balloonId);
                  }
               } else {
                  BalloonBlockEntity be = balloonBlockEntity(balloon);
                  if (be == null) {
                     unregister(activex.balloonId);
                  } else {
                     ServerSubLevel host = be.resolveHostSubLevel(level);
                     if (host != null) {
                        if (!activex.hostSubId.equals(host.getUniqueId()) || !activex.hostCell.equals(be.hostPos())) {
                           syncHost(activex, be, host);
                        }

                        double hostMass = Math.max(host.getMassTracker().getMass(), 0.05);
                        double totalContribution = hostSupportContribution.getOrDefault(host.getUniqueId(), 0.0);
                        double thisContribution = balloonContribution.getOrDefault(activex.balloonId, 0.0);
                        double totalSupported = Math.min(hostMass, totalContribution);
                        double payloadForBalloon = totalContribution > 1.0E-9 ? totalSupported * (thisContribution / totalContribution) : 0.0;
                        applyTetherSpring(physics, host, balloon, activex.hostCell, timeStep);
                        applyLift(physics, host, balloon, level, timeStep, payloadForBalloon);
                        if (verticalDampedHosts.add(host.getUniqueId())) {
                           dampHostVerticalInBand(physics, host, level, timeStep);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static double balloonSupportContribution(ServerLevel level, ServerSubLevel host, ServerSubLevel balloon, BlockPos hostPlotLocal, double hostMass) {
      Vector3d hostAnchorPlot = BalloonPlacement.hostAnchorPlot(host, hostPlotLocal);
      BlockPos balloonPlotLocal = BalloonHostKey.BALLOON_BLOCK_CELL;
      Vector3d stringTiePlot = BalloonPlacement.stringTiePlot(balloon, balloonPlotLocal);
      Vector3d hostWorld = host.logicalPose().transformPosition(hostAnchorPlot, new Vector3d());
      Vector3d stringWorld = balloon.logicalPose().transformPosition(stringTiePlot, new Vector3d());
      Vector3d delta = new Vector3d(stringWorld).sub(hostWorld);
      double distance = delta.length();
      if (distance < 1.0E-6) {
         return 0.0;
      } else {
         double rawVertical = Math.max(0.0, delta.y / distance);
         double verticalAlignment = rawVertical > 0.0 ? Math.pow(rawVertical, 0.55) : 0.0;
         double horizontalOffset = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
         double horizontalAlignment = Math.max(0.0, 1.0 - horizontalOffset / 4.0);
         double tautness = Math.min(1.0, distance / 4.0);
         double alignmentFactor = verticalAlignment * horizontalAlignment * tautness;
         double hoverFactor = groundHoverFactor(level, host, hostWorld);
         double perBalloonStrength = Math.min(Config.maxBalloonPayloadMass(), hostMass);
         return perBalloonStrength * alignmentFactor * hoverFactor;
      }
   }

   private static double groundHoverFactor(ServerLevel level, ServerSubLevel host, Vector3d hostWorld) {
      double bottomY = host.boundingBox().minY();
      Vec3 from = new Vec3(hostWorld.x, bottomY, hostWorld.z);
      Vec3 to = new Vec3(hostWorld.x, bottomY - 4.5, hostWorld.z);
      ClipContext ctx = new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, CollisionContext.empty());
      ((ClipContextExtension)ctx).sable$setIgnoredSubLevel(host);
      BlockHitResult hit = level.clip(ctx);
      if (hit.getType() == Type.MISS) {
         return 0.0;
      } else {
         double clearance = bottomY - hit.getLocation().y;
         if (clearance <= 3.0) {
            return 1.0;
         } else {
            return clearance >= 4.5 ? 0.0 : (4.5 - clearance) / 1.5;
         }
      }
   }

   private static void maybeRestoreTethers(ServerLevel level) {
      long gameTime = level.getGameTime();
      ResourceKey<Level> dimension = level.dimension();
      Long last = LAST_RESTORE_GAME_TIME.get(dimension);
      if (last == null || last != gameTime) {
         LAST_RESTORE_GAME_TIME.put(dimension, gameTime);
         restoreTethers(level);
      }
   }

   private static boolean isDefunctBalloon(ServerLevel level, UUID balloonId) {
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         SubLevel sub = container.getSubLevel(balloonId);
         return sub == null ? false : sub.isRemoved() || !BalloonResolve.isBalloonSubLevel(sub);
      } else {
         return false;
      }
   }

   private static void applyTetherSpring(SubLevelPhysicsSystem physics, ServerSubLevel host, ServerSubLevel balloon, BlockPos hostPlotLocal, double timeStep) {
      if (!(timeStep <= 0.0)) {
         Vector3d hostAnchorPlot = BalloonPlacement.hostAnchorPlot(host, hostPlotLocal);
         BlockPos balloonPlotLocal = BalloonHostKey.BALLOON_BLOCK_CELL;
         Vector3d stringTiePlot = BalloonPlacement.stringTiePlot(balloon, balloonPlotLocal);
         Vector3d hostWorld = host.logicalPose().transformPosition(hostAnchorPlot, new Vector3d());
         Vector3d stringWorld = balloon.logicalPose().transformPosition(stringTiePlot, new Vector3d());
         Vector3d hostToString = new Vector3d(stringWorld).sub(hostWorld);
         double distance = hostToString.length();
         double stretch = distance - 4.0;
         if (!(stretch <= 0.0)) {
            if (distance < 1.0E-8) {
               hostToString.set(0.0, 1.0, 0.0);
            } else {
               hostToString.div(distance);
            }

            double balloonMass = Math.max(balloon.getMassTracker().getMass(), 0.05);
            Vector3d worldImpulse = new Vector3d(hostToString).mul(-stretch * 160.0 * balloonMass * timeStep);
            Vector3d vBalloon = physics.getPipeline().getLinearVelocity(balloon, new Vector3d());
            Vector3d wBalloon = physics.getPipeline().getAngularVelocity(balloon, new Vector3d());
            Vector3d tieOffsetPlot = new Vector3d(stringTiePlot).sub(balloon.logicalPose().rotationPoint());
            Vector3d tieOffsetWorld = new Quaterniond(balloon.logicalPose().orientation()).transform(tieOffsetPlot, new Vector3d());
            Vector3d vString = new Vector3d(wBalloon).cross(tieOffsetWorld).add(vBalloon);
            Vector3d vHost = physics.getPipeline().getLinearVelocity(host, new Vector3d());
            Vector3d wHost = physics.getPipeline().getAngularVelocity(host, new Vector3d());
            Vector3d anchorOffsetPlot = new Vector3d(hostAnchorPlot).sub(host.logicalPose().rotationPoint());
            Vector3d anchorOffsetWorld = new Quaterniond(host.logicalPose().orientation()).transform(anchorOffsetPlot, new Vector3d());
            Vector3d vHostAnchor = new Vector3d(wHost).cross(anchorOffsetWorld).add(vHost);
            double relativeOutward = vString.dot(hostToString) - vHostAnchor.dot(hostToString);
            if (relativeOutward > 0.0) {
               worldImpulse.fma(-14.0 * relativeOutward * balloonMass * timeStep, hostToString, worldImpulse);
            }

            Vector3d balloonLocalImpulse = new Quaterniond(balloon.logicalPose().orientation()).conjugate().transform(worldImpulse, new Vector3d());
            Vector3d hostLocalImpulse = new Quaterniond(host.logicalPose().orientation())
               .conjugate()
               .transform(new Vector3d(worldImpulse).negate(), new Vector3d());
            ForceGroup forceGroup = (ForceGroup)ForceGroups.BALLOON_LIFT.get();
            balloon.getOrCreateQueuedForceGroup(forceGroup).applyAndRecordPointForce(stringTiePlot, balloonLocalImpulse);
            host.getOrCreateQueuedForceGroup(forceGroup).applyAndRecordPointForce(hostAnchorPlot, hostLocalImpulse);
            physics.getPipeline().wakeUp(balloon);
            physics.getPipeline().wakeUp(host);
         }
      }
   }

   private static void applyLift(
      SubLevelPhysicsSystem physics, ServerSubLevel host, ServerSubLevel balloon, ServerLevel level, double timeStep, double supportedPayload
   ) {
      Vector3d position = balloon.logicalPose().position();
      Vector3d gravity = new Vector3d(DimensionPhysicsData.getGravity(level, position));
      double g = gravity.length();
      if (!(g < 1.0E-8)) {
         Vector3d up = new Vector3d(gravity).normalize().negate();
         double balloonMass = Math.max(balloon.getMassTracker().getMass(), 0.05);
         Vector3d liftPointPlot = BalloonPlacement.liftPointPlot(balloon, BalloonHostKey.BALLOON_BLOCK_CELL);
         ForceGroup forceGroup = (ForceGroup)ForceGroups.BALLOON_LIFT.get();
         Quaterniond balloonOrientation = new Quaterniond(balloon.logicalPose().orientation()).conjugate();
         Quaterniond hostOrientation = new Quaterniond(host.logicalPose().orientation()).conjugate();
         Vector3d balloonHoverImpulse = new Vector3d(up).mul(balloonMass * g * 1.75 * timeStep);
         Vector3d balloonLocalHover = balloonOrientation.transform(balloonHoverImpulse, new Vector3d());
         balloon.getOrCreateQueuedForceGroup(forceGroup).applyAndRecordPointForce(liftPointPlot, balloonLocalHover);
         if (supportedPayload > 0.0) {
            Vector3d payloadImpulse = new Vector3d(up).mul(supportedPayload * g * timeStep);
            Vector3d hostLocalAssist = hostOrientation.transform(payloadImpulse, new Vector3d());
            host.getOrCreateQueuedForceGroup(forceGroup).getForceTotal().applyLinearImpulse(hostLocalAssist);
            double hostMass = Math.max(host.getMassTracker().getMass(), 0.05);
            double supportFraction = Math.min(1.0, supportedPayload / hostMass);
            dampHostSwing(physics, host, hostOrientation, up, supportFraction, timeStep);
            uprightHostAlignment(physics, host, hostOrientation, up, supportFraction, timeStep);
         }

         physics.getPipeline().wakeUp(balloon);
         physics.getPipeline().wakeUp(host);
      }
   }

   private static void dampHostSwing(
      SubLevelPhysicsSystem physics, ServerSubLevel host, Quaterniond hostOrientationConj, Vector3d up, double supportFraction, double timeStep
   ) {
      if (!(supportFraction <= 0.0) && !(timeStep <= 0.0)) {
         ForceTotal forceTotal = host.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.BALLOON_LIFT.get()).getForceTotal();
         double hostMass = Math.max(host.getMassTracker().getMass(), 0.05);
         Vector3d vWorld = physics.getPipeline().getLinearVelocity(host, new Vector3d());
         Vector3d vHorizontal = new Vector3d(vWorld).fma(-vWorld.dot(up), up);
         double linearScale = Math.min(1.0, 0.0 * supportFraction * timeStep);
         Vector3d linearImpulseLocal = hostOrientationConj.transform(new Vector3d(vHorizontal).mul(-linearScale * hostMass), new Vector3d());
         forceTotal.applyLinearImpulse(linearImpulseLocal);
         Vector3d wWorld = physics.getPipeline().getAngularVelocity(host, new Vector3d());
         Vector3d wLocal = hostOrientationConj.transform(wWorld, new Vector3d());
         double angularScale = Math.min(1.0, 0.2 * supportFraction * timeStep);
         Vector3d angularMomentumLocal = host.getMassTracker().getInertiaTensor().transform(wLocal, new Vector3d());
         forceTotal.applyAngularImpulse(new Vector3d(angularMomentumLocal).mul(-angularScale));
         physics.getPipeline().wakeUp(host);
      }
   }

   private static void uprightHostAlignment(
      SubLevelPhysicsSystem physics, ServerSubLevel host, Quaterniond hostOrientationConj, Vector3d worldUp, double supportFraction, double timeStep
   ) {
      if (!(supportFraction <= 0.0) && !(timeStep <= 0.0)) {
         Quaterniond hostOrient = new Quaterniond(hostOrientationConj).conjugate();
         Vector3d localYWorld = hostOrient.transform(new Vector3d(0.0, 1.0, 0.0), new Vector3d());
         Vector3d errorAxis = new Vector3d(localYWorld).cross(worldUp);
         if (!(errorAxis.lengthSquared() < 1.0E-10)) {
            Vector3d correctiveTorqueWorld = errorAxis.mul(0.4 * supportFraction);
            Vector3d correctiveTorqueLocal = hostOrientationConj.transform(correctiveTorqueWorld, new Vector3d());
            Vector3d angularImpulseLocal = host.getMassTracker().getInertiaTensor().transform(correctiveTorqueLocal, new Vector3d()).mul(timeStep);
            host.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.BALLOON_LIFT.get()).getForceTotal().applyAngularImpulse(angularImpulseLocal);
            physics.getPipeline().wakeUp(host);
         }
      }
   }

   private static void dampHostVerticalInBand(SubLevelPhysicsSystem physics, ServerSubLevel host, ServerLevel level, double timeStep) {
      if (!(timeStep <= 0.0)) {
         double centerX = (host.boundingBox().minX() + host.boundingBox().maxX()) * 0.5;
         double centerZ = (host.boundingBox().minZ() + host.boundingBox().maxZ()) * 0.5;
         double hoverFactor = groundHoverFactor(level, host, new Vector3d(centerX, 0.0, centerZ));
         if (!(hoverFactor <= 0.0) && !(hoverFactor >= 1.0)) {
            Vector3d gravity = new Vector3d(DimensionPhysicsData.getGravity(level, host.logicalPose().position()));
            double g = gravity.length();
            if (!(g < 1.0E-8)) {
               Vector3d up = new Vector3d(gravity).normalize().negate();
               double bandness = 4.0 * hoverFactor * (1.0 - hoverFactor);
               double scale = Math.min(1.0, 0.7 * bandness * timeStep);
               if (!(scale <= 0.0)) {
                  double hostMass = Math.max(host.getMassTracker().getMass(), 0.05);
                  Vector3d vWorld = physics.getPipeline().getLinearVelocity(host, new Vector3d());
                  double verticalSpeed = vWorld.dot(up);
                  Vector3d impulseWorld = new Vector3d(up).mul(-verticalSpeed * scale * hostMass);
                  Quaterniond hostOrientationConj = new Quaterniond(host.logicalPose().orientation()).conjugate();
                  Vector3d impulseLocal = hostOrientationConj.transform(impulseWorld, new Vector3d());
                  host.getOrCreateQueuedForceGroup((ForceGroup)ForceGroups.BALLOON_LIFT.get()).getForceTotal().applyLinearImpulse(impulseLocal);
                  physics.getPipeline().wakeUp(host);
               }
            }
         }
      }
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
