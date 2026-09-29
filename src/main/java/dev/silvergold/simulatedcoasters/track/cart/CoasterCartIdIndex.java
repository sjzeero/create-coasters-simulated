package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class CoasterCartIdIndex {
   private static final Map<ResourceKey<Level>, CoasterCartIdIndex.DimensionIndex> BY_DIM = new HashMap<>();

   private CoasterCartIdIndex() {
   }

   @Nullable
   static ServerSubLevel find(ServerLevel level, UUID cartId) {
      ensureFresh(level);
      CoasterCartIdIndex.DimensionIndex index = BY_DIM.get(level.dimension());
      return index != null ? index.byId.get(cartId) : null;
   }

   static void invalidateDimension(ServerLevel level) {
      synchronized (BY_DIM) {
         BY_DIM.remove(level.dimension());
      }
   }

   static void removeCart(ServerLevel level, ServerSubLevel sub) {
      synchronized (BY_DIM) {
         CoasterCartIdIndex.DimensionIndex index = BY_DIM.get(level.dimension());
         if (index != null) {
            index.byId.remove(sub.getUniqueId());
         }
      }
   }

   static void clearDimension(ResourceKey<Level> dimension) {
      synchronized (BY_DIM) {
         BY_DIM.remove(dimension);
      }
   }

   private static void ensureFresh(ServerLevel level) {
      long gameTime = level.getGameTime();
      ResourceKey<Level> dimension = level.dimension();
      synchronized (BY_DIM) {
         CoasterCartIdIndex.DimensionIndex existing = BY_DIM.get(dimension);
         if (existing == null || existing.gameTime != gameTime) {
            rebuildLocked(level, gameTime, dimension);
         }
      }
   }

   private static void rebuildLocked(ServerLevel level, long gameTime, ResourceKey<Level> dimension) {
      Map<UUID, ServerSubLevel> byId = new HashMap<>();
      SubLevelContainer raw = SubLevelContainer.getContainer(level);
      if (raw instanceof ServerSubLevelContainer container) {
         for (ServerSubLevel sub : container.getAllSubLevels()) {
            if (!sub.isRemoved() && CoasterCartBearingTickCache.scan(sub).representative() != null) {
               byId.put(sub.getUniqueId(), sub);
            }
         }
      }

      BY_DIM.put(dimension, new CoasterCartIdIndex.DimensionIndex(gameTime, byId));
   }

   private static record DimensionIndex(long gameTime, Map<UUID, ServerSubLevel> byId) {
   }
}
