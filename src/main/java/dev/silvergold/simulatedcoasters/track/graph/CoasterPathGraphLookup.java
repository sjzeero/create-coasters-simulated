package dev.silvergold.simulatedcoasters.track.graph;

import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public final class CoasterPathGraphLookup {
   private CoasterPathGraphLookup() {
   }

   public static CoasterPathGraph get(Level level) {
      if (level instanceof ServerLevel sl) {
         return CoasterPathGraphManager.get(sl);
      } else {
         return FMLEnvironment.dist != Dist.CLIENT ? new CoasterPathGraph() : CoasterPathGraphLookup.ClientHolder.get(level);
      }
   }

   private static final class ClientHolder {
      static CoasterPathGraph get(Level level) {
         return CoasterPathGraphManagerClient.get(level);
      }
   }
}
