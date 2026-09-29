/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.physics.PhysicsPipeline
 *  dev.ryanhcode.sable.api.physics.PhysicsPipelineBody
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftDriveDirection;
import dev.silvergold.simulatedcoasters.track.chainlift.ChainLiftSpanQuery;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class CoasterCartChainLiftDrive {
    private static final double SPEED_EPS = 1.0E-4;

    private CoasterCartChainLiftDrive() {
    }

    public static void applySpeedMatch(ServerSubLevel sub, ServerLevel level, SubLevelPhysicsSystem physics, CoasterPathTrackFrame.GraphHit graphHit, CoasterPathTrackFrame.TrackBasis basis, double dt) {
        double delta;
        if (dt <= 0.0) {
            return;
        }
        @Nullable ChainLiftSpanQuery.ChainLiftSpanHit lift = ChainLiftSpanQuery.findAtGraphHit((Level)level, graphHit, 0.0);
        if (lift == null) {
            return;
        }
        double chainSpeed = ChainLiftDriveDirection.signedChainBlocksPerSecond(lift.anchor(), lift.path(), 0.0, 256.0f, 16.0);
        Vec3 chainVel = ChainLiftSpanQuery.chainWorldVelocityAtHit((Level)level, lift.path(), graphHit, chainSpeed, 0.0);
        Vec3 tangentMc = basis.forward();
        if (tangentMc.lengthSqr() < 1.0E-12) {
            return;
        }
        Vector3d tangent = new Vector3d(tangentMc.x, tangentMc.y, tangentMc.z).normalize();
        double target = -CoasterCartChainLiftDrive.projectOntoUnitTangent(chainVel, tangent);
        PhysicsPipeline pipeline = physics.getPipeline();
        Vector3d v = pipeline.getLinearVelocity((PhysicsPipelineBody)sub, new Vector3d());
        double current = v.dot((Vector3dc)tangent);
        boolean oppositeDirection = Math.abs(current) > 1.0E-4 && Math.abs(target) > 1.0E-4 && Math.signum(current) != Math.signum(target);
        boolean slowerThanChain = Math.abs(target) > 1.0E-4 && (Math.abs(current) <= 1.0E-4 || Math.signum(current) == Math.signum(target)) && Math.abs(current) + 1.0E-4 < Math.abs(target);
        boolean strongChainGrip = oppositeDirection || slowerThanChain;
        double rate = strongChainGrip ? 64.0 : 32.0;
        double maxDelta = rate * dt;
        if (oppositeDirection) {
            double cancelReverse;
            delta = cancelReverse = Math.copySign(Math.min(Math.abs(current), maxDelta), -current);
            double remaining = target - (current + cancelReverse);
            if (Math.abs(remaining) > 1.0E-4) {
                double extra = Math.abs(remaining) <= maxDelta ? remaining : Math.copySign(maxDelta, remaining);
                delta += extra;
            }
        } else {
            double diff = target - current;
            double d = delta = Math.abs(diff) <= maxDelta ? diff : Math.copySign(maxDelta, diff);
        }
        if (Math.abs(delta) < 1.0E-12) {
            return;
        }
        pipeline.addLinearAndAngularVelocity((PhysicsPipelineBody)sub, (Vector3dc)new Vector3d((Vector3dc)tangent).mul(delta), (Vector3dc)new Vector3d());
    }

    private static double projectOntoUnitTangent(Vec3 vector, Vector3d unitTangent) {
        return vector.x * unitTangent.x + vector.y * unitTangent.y + vector.z * unitTangent.z;
    }
}

