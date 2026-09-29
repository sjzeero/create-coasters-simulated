package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class RivetPoseAlignment {
   private RivetPoseAlignment() {
   }

   public static RivetPlacement.Resolved resolvedFrom(RivetBlockEntity be) {
      return new RivetPlacement.Resolved(be.hostPos(), be.face(), be.attachment().lx(), be.attachment().ly(), be.attachment().lz(), be.yRotationDegrees());
   }

   public static Quaterniond nominalWorldOrientation(ServerLevel level, RivetBlockEntity be) {
      return RivetPlacement.worldOrientation(level, resolvedFrom(be));
   }

   public static Quaterniond nominalRelativeOrientation(ServerLevel level, ServerSubLevel hostSub, RivetBlockEntity be) {
      Quaterniond nominalWorld = nominalWorldOrientation(level, be);
      return new Quaterniond(hostSub.logicalPose().orientation()).conjugate(new Quaterniond()).mul(nominalWorld, new Quaterniond());
   }

   public static void alignToNominal(ServerLevel level, ServerSubLevelContainer container, ServerSubLevel rivetSub, RivetBlockEntity be) {
      rivetSub.updateMergedMassData(1.0F);
      RivetPlacement.Resolved resolved = resolvedFrom(be);
      Vec3 faceAnchor = RivetPlacement.faceAnchorWorld(level, resolved);
      Quaterniond orientation = RivetPlacement.worldOrientation(level, resolved);
      BlockPos plotCell = rivetSub.getPlot().getCenterBlock();
      Vector3dc rotationPoint = rivetSub.getMassTracker().getCenterOfMass();
      if (rotationPoint == null) {
         rotationPoint = new Vector3d((double)plotCell.getX() + 0.5, (double)plotCell.getY() + 0.5, (double)plotCell.getZ() + 0.5);
      }

      Vec3 anchorPlot = RivetPlacement.rivetOutwardAnchorPlot(plotCell);
      Vector3d delta = new Vector3d(anchorPlot.x - rotationPoint.x(), anchorPlot.y - rotationPoint.y(), anchorPlot.z - rotationPoint.z());
      orientation.transform(delta);
      Vec3 logicalPosition = faceAnchor.subtract(delta.x, delta.y, delta.z);
      rivetSub.logicalPose().rotationPoint().set(rotationPoint);
      rivetSub.logicalPose().orientation().set(orientation);
      rivetSub.logicalPose().position().set(logicalPosition.x, logicalPosition.y, logicalPosition.z);
      rivetSub.updateLastPose();
      container.physicsSystem().getPipeline().teleport(rivetSub, new Vector3d(logicalPosition.x, logicalPosition.y, logicalPosition.z), orientation);
      container.physicsSystem().getPipeline().onStatsChanged(rivetSub);
   }
}
