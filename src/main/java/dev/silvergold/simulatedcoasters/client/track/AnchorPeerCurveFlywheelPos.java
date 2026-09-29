package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class AnchorPeerCurveFlywheelPos {
   private AnchorPeerCurveFlywheelPos() {
   }

   public static BlockPos crumbleAnchor(Level level, AnchorPeerCurveMineTarget target) {
      return crumbleAnchor(level, target.mined(), target.peer());
   }

   public static BlockPos crumbleAnchor(Level level, BlockPos mined, BlockPos peer) {
      BlockPos primaryPos = primaryHolder(level, mined, peer);
      return primaryPos != null ? primaryPos : mined;
   }

   @Nullable
   private static BlockPos primaryHolder(Level level, BlockPos mined, BlockPos peer) {
      if (level.getBlockEntity(mined) instanceof CoasterAnchorpointBlockEntity am) {
         BezierConnection bc = am.getAnchorPeerCurvesView().get(peer);
         if (bc != null && bc.isPrimary()) {
            return mined;
         }
      }

      if (level.getBlockEntity(peer) instanceof CoasterAnchorpointBlockEntity ap) {
         BezierConnection bc = ap.getAnchorPeerCurvesView().get(mined);
         if (bc != null && bc.isPrimary()) {
            return peer;
         }
      }

      return null;
   }
}
