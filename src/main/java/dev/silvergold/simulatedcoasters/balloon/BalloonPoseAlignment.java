package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class BalloonPoseAlignment {
   private BalloonPoseAlignment() {
   }

   public static void alignToNominal(
      ServerLevel level, ServerSubLevelContainer container, ServerSubLevel balloonSub, ServerSubLevel hostSub, BlockPos hostPlotCell
   ) {
      balloonSub.updateMergedMassData(0.35F);
      Vector3d desiredStringWorld = BalloonPlacement.desiredStringWorld(hostSub, hostPlotCell);
      if (desiredStringWorld != null) {
         BlockPos balloonCell = BalloonHostKey.BALLOON_BLOCK_CELL;
         Vector3d stringTiePlot = BalloonPlacement.stringTiePlot(balloonSub, balloonCell);
         Quaterniond orientation = new Quaterniond();
         BlockPos plotLattice = BalloonPlacement.plotLatticeCell(balloonSub, balloonCell);
         Vector3dc rotationPoint = balloonSub.getMassTracker().getCenterOfMass();
         if (rotationPoint == null) {
            rotationPoint = new Vector3d((double)plotLattice.getX() + 0.5, (double)plotLattice.getY() + 0.5, (double)plotLattice.getZ() + 0.5);
         }

         Vector3d delta = new Vector3d(stringTiePlot).sub(rotationPoint);
         orientation.transform(delta);
         Vector3d logicalPosition = new Vector3d(desiredStringWorld).sub(delta);
         Pose3d pose = balloonSub.logicalPose();
         pose.rotationPoint().set(rotationPoint);
         pose.orientation().set(orientation);
         pose.position().set(logicalPosition);
         balloonSub.updateLastPose();
         container.physicsSystem().getPipeline().teleport(balloonSub, logicalPosition, orientation);
         container.physicsSystem().getPipeline().onStatsChanged(balloonSub);
      }
   }
}
