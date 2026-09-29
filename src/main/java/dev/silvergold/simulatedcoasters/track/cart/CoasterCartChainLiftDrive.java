package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftDriveDirection;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanQuery;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class CoasterCartChainLiftDrive {
   private static final double SPEED_EPS = 1.0E-4;

   private CoasterCartChainLiftDrive() {
   }

   public static void applySpeedMatch(
      ServerSubLevel sub,
      ServerLevel level,
      SubLevelPhysicsSystem physics,
      CoasterPathTrackFrame.GraphHit graphHit,
      CoasterPathTrackFrame.TrackBasis basis,
      double dt
   ) {
      if (!(dt <= 0.0)) {
         ChainLiftSpanQuery.ChainLiftSpanHit lift = ChainLiftSpanQuery.findAtGraphHit(level, graphHit, 0.0);
         if (lift != null) {
            double chainSpeed = ChainLiftDriveDirection.signedChainBlocksPerSecond(lift.anchor(), lift.path(), 0.0, 256.0F, 16.0);
            Vec3 chainVel = ChainLiftSpanQuery.chainWorldVelocityAtHit(level, lift.path(), graphHit, chainSpeed, 0.0);
            Vec3 tangentMc = basis.forward();
            if (!(tangentMc.lengthSqr() < 1.0E-12)) {
               Vector3d tangent = new Vector3d(tangentMc.x, tangentMc.y, tangentMc.z).normalize();
               double target = -projectOntoUnitTangent(chainVel, tangent);
               PhysicsPipeline pipeline = physics.getPipeline();
               Vector3d v = pipeline.getLinearVelocity(sub, new Vector3d());
               double current = v.dot(tangent);
               boolean oppositeDirection = Math.abs(current) > 1.0E-4 && Math.abs(target) > 1.0E-4 && Math.signum(current) != Math.signum(target);
               boolean slowerThanChain = Math.abs(target) > 1.0E-4
                  && (Math.abs(current) <= 1.0E-4 || Math.signum(current) == Math.signum(target))
                  && Math.abs(current) + 1.0E-4 < Math.abs(target);
               boolean strongChainGrip = oppositeDirection || slowerThanChain;
               double rate = strongChainGrip ? 64.0 : 32.0;
               double maxDelta = rate * dt;
               double delta;
               if (oppositeDirection) {
                  double cancelReverse = Math.copySign(Math.min(Math.abs(current), maxDelta), -current);
                  delta = cancelReverse;
                  double remaining = target - (current + cancelReverse);
                  if (Math.abs(remaining) > 1.0E-4) {
                     double extra = Math.abs(remaining) <= maxDelta ? remaining : Math.copySign(maxDelta, remaining);
                     delta = cancelReverse + extra;
                  }
               } else {
                  double diff = target - current;
                  delta = Math.abs(diff) <= maxDelta ? diff : Math.copySign(maxDelta, diff);
               }

               if (!(Math.abs(delta) < 1.0E-12)) {
                  pipeline.addLinearAndAngularVelocity(sub, new Vector3d(tangent).mul(delta), new Vector3d());
               }
            }
         }
      }
   }

   private static double projectOntoUnitTangent(Vec3 vector, Vector3d unitTangent) {
      return vector.x * unitTangent.x + vector.y * unitTangent.y + vector.z * unitTangent.z;
   }
}
