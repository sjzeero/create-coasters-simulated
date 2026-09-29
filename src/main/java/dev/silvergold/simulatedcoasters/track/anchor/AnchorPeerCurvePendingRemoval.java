package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class AnchorPeerCurvePendingRemoval {
   private static final Set<String> PENDING = new HashSet<>();

   private AnchorPeerCurvePendingRemoval() {
   }

   public static void mark(BlockPos mined, BlockPos peer) {
      PENDING.add(CoasterPeerCurveHandleLengths.edgeKeyFromEndpoints(mined, peer));
   }

   public static boolean isPending(BezierConnection bc) {
      if (bc == null) {
         return false;
      } else {
         BezierConnection pri = bc.isPrimary() ? bc : bc.secondary();
         return PENDING.contains(CoasterPeerCurveHandleLengths.edgeKey(pri));
      }
   }

   public static void onAnchorSynced(CoasterAnchorpointBlockEntity anchor) {
      if (!PENDING.isEmpty() && anchor.getLevel() != null) {
         PENDING.removeIf(key -> !isEdgeHostedAnywhere(anchor.getLevel(), key));
      }
   }

   public static void clear() {
      PENDING.clear();
   }

   private static boolean isEdgeHostedAnywhere(Level level, String edgeKey) {
      for (CoasterAnchorpointBlockEntity anchor : AnchorPeerCurveClientIndex.anchors()) {
         if (!anchor.isRemoved() && anchor.getLevel() == level) {
            BlockPos self = anchor.getBlockPos();

            for (BlockPos peer : anchor.getAnchorPeerCurvesView().keySet()) {
               if (CoasterPeerCurveHandleLengths.edgeKeyFromEndpoints(self, peer).equals(edgeKey)) {
                  return true;
               }
            }
         }
      }

      return false;
   }
}
