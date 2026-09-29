package dev.silvergold.simulatedcoasters.track.anchor;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterTrackCurveDrops;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;

@EventBusSubscriber(
   modid = "simulatedcoasters"
)
public final class CoasterAnchorpointBreakHandler {
   private CoasterAnchorpointBreakHandler() {
   }

   @SubscribeEvent
   public static void onAnchorpointBroken(BreakEvent event) {
      if (!event.isCanceled()) {
         if (event.getLevel() instanceof ServerLevel level) {
            if (event.getState().getBlock() instanceof CoasterAnchorpointBlock) {
               Player player = event.getPlayer();
               if (player != null && !player.isCreative()) {
                  if (level.getBlockEntity(event.getPos()) instanceof CoasterAnchorpointBlockEntity anchor) {
                     if (!anchor.getAnchorPeerCurvesView().isEmpty()) {
                        for (BezierConnection bc : anchor.getAnchorPeerCurvesView().values()) {
                           if (bc != null) {
                              BezierConnection primary = bc.isPrimary() ? bc : bc.secondary();
                              CoasterTrackCurveDrops.spawnItems(level, primary);
                              primary.spawnDestroyParticles(level);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}
