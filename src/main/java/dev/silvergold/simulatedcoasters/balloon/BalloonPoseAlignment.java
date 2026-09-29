/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.physics.PhysicsPipelineBody
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.companion.math.Pose3d
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Quaterniond
 *  org.joml.Quaterniondc
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostKey;
import dev.silvergold.simulatedcoasters.balloon.BalloonPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class BalloonPoseAlignment {
    private BalloonPoseAlignment() {
    }

    public static void alignToNominal(ServerLevel level, ServerSubLevelContainer container, ServerSubLevel balloonSub, ServerSubLevel hostSub, BlockPos hostPlotCell) {
        balloonSub.updateMergedMassData(0.35f);
        @Nullable Vector3d desiredStringWorld = BalloonPlacement.desiredStringWorld(hostSub, hostPlotCell);
        if (desiredStringWorld == null) {
            return;
        }
        BlockPos balloonCell = BalloonHostKey.BALLOON_BLOCK_CELL;
        Vector3d stringTiePlot = BalloonPlacement.stringTiePlot((SubLevel)balloonSub, balloonCell);
        Quaterniond orientation = new Quaterniond();
        BlockPos plotLattice = BalloonPlacement.plotLatticeCell((SubLevel)balloonSub, balloonCell);
        Vector3dc rotationPoint = balloonSub.getMassTracker().getCenterOfMass();
        if (rotationPoint == null) {
            rotationPoint = new Vector3d((double)plotLattice.getX() + 0.5, (double)plotLattice.getY() + 0.5, (double)plotLattice.getZ() + 0.5);
        }
        Vector3d delta = new Vector3d((Vector3dc)stringTiePlot).sub(rotationPoint);
        orientation.transform(delta);
        Vector3d logicalPosition = new Vector3d((Vector3dc)desiredStringWorld).sub((Vector3dc)delta);
        Pose3d pose = balloonSub.logicalPose();
        pose.rotationPoint().set(rotationPoint);
        pose.orientation().set((Quaterniondc)orientation);
        pose.position().set((Vector3dc)logicalPosition);
        balloonSub.updateLastPose();
        container.physicsSystem().getPipeline().teleport((PhysicsPipelineBody)balloonSub, (Vector3dc)logicalPosition, (Quaterniondc)orientation);
        container.physicsSystem().getPipeline().onStatsChanged(balloonSub);
    }
}

