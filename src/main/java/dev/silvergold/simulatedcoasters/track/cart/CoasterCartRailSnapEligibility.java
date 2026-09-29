package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathEdge;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTrackFrame;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class CoasterCartRailSnapEligibility {
   private CoasterCartRailSnapEligibility() {
   }

   public static boolean isRailEdgeFullyOnBearingSubLevel(SubLevel bearingSub, Level level, CoasterPathEdge edge) {
      SubLevel a = Sable.HELPER.getContaining(level, edge.from());
      SubLevel b = Sable.HELPER.getContaining(level, edge.to());
      if (a != null && b != null) {
         UUID id = bearingSub.getUniqueId();
         return Objects.equals(id, a.getUniqueId()) && Objects.equals(id, b.getUniqueId());
      } else {
         return false;
      }
   }

   public static boolean isRailEdgeFullyOnBearingSubLevel(SubLevel bearingSub, Level level, CoasterPathTrackFrame.GraphHit hit) {
      return hit != null && isRailEdgeFullyOnBearingSubLevel(bearingSub, level, hit.edge());
   }

   @Nullable
   public static CoasterPathTrackFrame.GraphHit filterHitForBearing(SubLevel bearingSub, Level level, @Nullable CoasterPathTrackFrame.GraphHit hit) {
      return isRailEdgeFullyOnBearingSubLevel(bearingSub, level, hit) ? null : hit;
   }
}
