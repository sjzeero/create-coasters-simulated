package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurvePendingRemoval;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class AnchorPeerCurveOutlineSupport {
   private static final double MAX_OUTLINE_SPAN_BLOCKS = 512.0;

   private AnchorPeerCurveOutlineSupport() {
   }

   @Nullable
   public static BezierConnection resolveHostedPrimary(Level level, @Nullable BezierConnection bc) {
      if (bc != null && level != null) {
         if (!bc.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id)) {
            return null;
         } else if (AnchorPeerCurvePendingRemoval.isPending(bc)) {
            return null;
         } else {
            BezierConnection pri = bc.isPrimary() ? bc : bc.secondary();
            BlockPos a = (BlockPos)pri.bePositions.getFirst();
            BlockPos b = (BlockPos)pri.bePositions.getSecond();

            for (BlockPos host : new BlockPos[]{a, b}) {
               if (level.getBlockEntity(host) instanceof CoasterAnchorpointBlockEntity anchor) {
                  BlockPos peer = host.equals(a) ? b : a;
                  BezierConnection hosted = anchor.getAnchorPeerCurvesView().get(peer);
                  if (hosted != null) {
                     BezierConnection resolved = hosted.isPrimary() ? hosted : hosted.secondary();
                     if (AnchorPeerCurvePendingRemoval.isPending(resolved)) {
                        return null;
                     }

                     return resolved;
                  }
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   public static boolean hasSaneOutlineBounds(BezierConnection bc) {
      AABB bounds = bc.getBounds();
      double dx = bounds.getXsize();
      double dy = bounds.getYsize();
      double dz = bounds.getZsize();
      if (!Double.isFinite(dx) || !Double.isFinite(dy) || !Double.isFinite(dz)) {
         return false;
      } else {
         return !(dx <= 0.0) && !(dy <= 0.0) && !(dz <= 0.0) ? dx <= 512.0 && dy <= 512.0 && dz <= 512.0 : false;
      }
   }
}
