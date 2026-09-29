package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public final class CoasterCartSeatedEntityCollision {
   private CoasterCartSeatedEntityCollision() {
   }

   public static boolean shouldSkipEntityCartCollision(Entity entity, SubLevel candidate) {
      if (entity.getVehicle() != null && candidate != null) {
         SubLevel hostCart = hostCartFromVehicleChain(entity);
         if (hostCart == null || candidate == hostCart) {
            return false;
         } else {
            return !CoasterCartResolve.isCartSubLevel(candidate)
               ? false
               : CoasterCartTrainLinkConstraint.getTrainLinkedCartIds(hostCart).contains(candidate.getUniqueId());
         }
      } else {
         return false;
      }
   }

   public static void pruneLinkedNeighborCartSubLevels(Entity entity, ObjectSet<SubLevel> intersecting) {
      if (entity.getVehicle() != null) {
         SubLevel hostCart = hostCartFromVehicleChain(entity);
         if (hostCart != null) {
            Set<UUID> trainCartIds = CoasterCartTrainLinkConstraint.getTrainLinkedCartIds(hostCart);
            UUID hostId = hostCart.getUniqueId();
            intersecting.removeIf(
               other -> other != hostCart
                     && CoasterCartResolve.isCartSubLevel(other)
                     && trainCartIds.contains(other.getUniqueId())
                     && !other.getUniqueId().equals(hostId)
            );
         }
      }
   }

   @Nullable
   public static SubLevel hostCartFromVehicleChain(Entity entity) {
      for (Entity vehicle = entity.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
         SubLevel sub = resolveCartSubLevelForEntity(vehicle);
         if (sub != null) {
            return sub;
         }
      }

      return resolveCartSubLevelForEntity(entity);
   }

   @Nullable
   private static SubLevel resolveCartSubLevelForEntity(Entity entity) {
      SubLevel containing = Sable.HELPER.getContaining(entity);
      if (containing != null && CoasterCartResolve.isCartSubLevel(containing)) {
         return containing;
      } else {
         SubLevel tracking = Sable.HELPER.getTrackingSubLevel(entity);
         if (tracking != null && CoasterCartResolve.isCartSubLevel(tracking)) {
            return tracking;
         } else {
            BoundingBox3d query = new BoundingBox3d(entity.getBoundingBox());
            query.expand(0.5);
            List<SubLevel> matches = new ArrayList<>();

            for (SubLevel candidate : Sable.HELPER.getAllIntersecting(entity.level(), query)) {
               if (CoasterCartResolve.isCartSubLevel(candidate) && entityInPlotLocalBounds(candidate, entity)) {
                  matches.add(candidate);
               }
            }

            if (matches.isEmpty()) {
               return null;
            } else {
               return matches.size() == 1 ? matches.get(0) : disambiguateOverlappingCarts(matches, entity);
            }
         }
      }
   }

   private static boolean entityInPlotLocalBounds(SubLevel subLevel, Entity entity) {
      Vector3d world = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
      Vector3d local = subLevel.logicalPose().transformPositionInverse(world, new Vector3d());
      BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
      return local.x >= (double)bounds.minX()
         && local.x <= (double)bounds.maxX()
         && local.y >= (double)bounds.minY()
         && local.y <= (double)bounds.maxY()
         && local.z >= (double)bounds.minZ()
         && local.z <= (double)bounds.maxZ();
   }

   @Nullable
   private static SubLevel disambiguateOverlappingCarts(List<SubLevel> matches, Entity entity) {
      SubLevel best = null;
      double bestDist = Double.MAX_VALUE;
      Vector3d worldPos = new Vector3d(entity.position().x, entity.position().y, entity.position().z);

      for (SubLevel candidate : matches) {
         Vector3d local = candidate.logicalPose().transformPositionInverse(worldPos, new Vector3d());
         BoundingBox3ic bounds = candidate.getPlot().getBoundingBox();
         double cx = Math.max((double)bounds.minX(), Math.min(local.x, (double)bounds.maxX()));
         double cy = Math.max((double)bounds.minY(), Math.min(local.y, (double)bounds.maxY()));
         double cz = Math.max((double)bounds.minZ(), Math.min(local.z, (double)bounds.maxZ()));
         double dist = local.distanceSquared(cx, cy, cz);
         if (dist < bestDist) {
            bestDist = dist;
            best = candidate;
         }
      }

      return best;
   }
}
