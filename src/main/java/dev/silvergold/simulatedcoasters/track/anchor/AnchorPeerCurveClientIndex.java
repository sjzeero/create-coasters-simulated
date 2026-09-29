package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.world.level.Level;

public final class AnchorPeerCurveClientIndex {
   private static final Set<CoasterAnchorpointBlockEntity> ANCHORS = Collections.newSetFromMap(new IdentityHashMap<>());

   private AnchorPeerCurveClientIndex() {
   }

   public static void refreshMembership(CoasterAnchorpointBlockEntity be) {
      ANCHORS.remove(be);
      if (!be.isRemoved()) {
         if (be.getLevel() != null && be.getLevel().isClientSide()) {
            Level level = be.getLevel();
            boolean anyPrimaryPeer = be.getAnchorPeerCurvesView()
               .values()
               .stream()
               .filter(c -> c != null)
               .map(c -> (BezierConnection)(c.isPrimary() ? c : c.secondary()))
               .anyMatch(p -> p.getMaterial().id.equals(CoasterTrackMaterials.COASTER.id));
            if (anyPrimaryPeer) {
               ANCHORS.add(be);
            }

            AnchorPeerCurvePendingRemoval.onAnchorSynced(be);
         }
      }
   }

   public static void remove(CoasterAnchorpointBlockEntity be) {
      ANCHORS.remove(be);
   }

   public static Iterable<CoasterAnchorpointBlockEntity> anchors() {
      return ANCHORS;
   }
}
