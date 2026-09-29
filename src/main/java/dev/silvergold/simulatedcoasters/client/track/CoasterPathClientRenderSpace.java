package dev.silvergold.simulatedcoasters.client.track;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class CoasterPathClientRenderSpace {
   private CoasterPathClientRenderSpace() {
   }

   public static Vec3 sampleEdgePosition(Level level, CoasterPathEdge edge, double t, float partialTick) {
      BlockPos anchor = CoasterPathGraphManager.edgeSubLevelAnchor(level, edge);
      Vec3 plotLocal = CoasterPathGraphManager.sampleEdgePositionPlotLocal(level, edge, t);
      return CoasterAnchorClientSpace.toRenderWorld(level, anchor, plotLocal, partialTick);
   }

   public static CoasterPathTrackFrame.TrackBasis trackBasisAtHit(Level level, CoasterPathTrackFrame.GraphHit hit, float partialTick) {
      BlockPos host = CoasterPathGraphManager.edgeSubLevelAnchor(level, hit.edge());
      CoasterPathTrackFrame.TrackBasis plot = CoasterPathTrackFrame.trackBasisPlotLocalAtHit(level, hit);
      Pose3dc pose = CoasterAnchorClientSpace.renderPoseForAnchor(level, host, partialTick);
      return pose == null
         ? plot
         : new CoasterPathTrackFrame.TrackBasis(
            CoasterAnchorClientSpace.toRenderDirection(pose, plot.forward()),
            CoasterAnchorClientSpace.toRenderDirection(pose, plot.right()),
            CoasterAnchorClientSpace.toRenderDirection(pose, plot.up())
         );
   }
}
