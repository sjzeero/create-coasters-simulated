/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.companion.math.BoundingBox3d
 *  dev.ryanhcode.sable.companion.math.BoundingBox3dc
 *  dev.ryanhcode.sable.companion.math.BoundingBox3ic
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  it.unimi.dsi.fastutil.objects.ObjectSet
 *  net.minecraft.world.entity.Entity
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkConstraint;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class CoasterCartSeatedEntityCollision {
    private CoasterCartSeatedEntityCollision() {
    }

    public static boolean shouldSkipEntityCartCollision(Entity entity, SubLevel candidate) {
        if (entity.getVehicle() == null || candidate == null) {
            return false;
        }
        SubLevel hostCart = CoasterCartSeatedEntityCollision.hostCartFromVehicleChain(entity);
        if (hostCart == null || candidate == hostCart) {
            return false;
        }
        if (!CoasterCartResolve.isCartSubLevel(candidate)) {
            return false;
        }
        return CoasterCartTrainLinkConstraint.getTrainLinkedCartIds(hostCart).contains(candidate.getUniqueId());
    }

    public static void pruneLinkedNeighborCartSubLevels(Entity entity, ObjectSet<SubLevel> intersecting) {
        if (entity.getVehicle() == null) {
            return;
        }
        SubLevel hostCart = CoasterCartSeatedEntityCollision.hostCartFromVehicleChain(entity);
        if (hostCart == null) {
            return;
        }
        Set<UUID> trainCartIds = CoasterCartTrainLinkConstraint.getTrainLinkedCartIds(hostCart);
        UUID hostId = hostCart.getUniqueId();
        intersecting.removeIf(other -> other != hostCart && CoasterCartResolve.isCartSubLevel(other) && trainCartIds.contains(other.getUniqueId()) && !other.getUniqueId().equals(hostId));
    }

    @Nullable
    public static SubLevel hostCartFromVehicleChain(Entity entity) {
        for (Entity vehicle = entity.getVehicle(); vehicle != null; vehicle = vehicle.getVehicle()) {
            SubLevel sub = CoasterCartSeatedEntityCollision.resolveCartSubLevelForEntity(vehicle);
            if (sub == null) continue;
            return sub;
        }
        return CoasterCartSeatedEntityCollision.resolveCartSubLevelForEntity(entity);
    }

    @Nullable
    private static SubLevel resolveCartSubLevelForEntity(Entity entity) {
        SubLevel containing = Sable.HELPER.getContaining(entity);
        if (containing != null && CoasterCartResolve.isCartSubLevel(containing)) {
            return containing;
        }
        SubLevel tracking = Sable.HELPER.getTrackingSubLevel(entity);
        if (tracking != null && CoasterCartResolve.isCartSubLevel(tracking)) {
            return tracking;
        }
        BoundingBox3d query = new BoundingBox3d(entity.getBoundingBox());
        query.expand(0.5);
        ArrayList<SubLevel> matches = new ArrayList<SubLevel>();
        for (SubLevel candidate : Sable.HELPER.getAllIntersecting(entity.level(), (BoundingBox3dc)query)) {
            if (!CoasterCartResolve.isCartSubLevel(candidate) || !CoasterCartSeatedEntityCollision.entityInPlotLocalBounds(candidate, entity)) continue;
            matches.add(candidate);
        }
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return (SubLevel)matches.get(0);
        }
        return CoasterCartSeatedEntityCollision.disambiguateOverlappingCarts(matches, entity);
    }

    private static boolean entityInPlotLocalBounds(SubLevel subLevel, Entity entity) {
        Vector3d world = new Vector3d(entity.getX(), entity.getY(), entity.getZ());
        Vector3d local = subLevel.logicalPose().transformPositionInverse((Vector3dc)world, new Vector3d());
        BoundingBox3ic bounds = subLevel.getPlot().getBoundingBox();
        return local.x >= (double)bounds.minX() && local.x <= (double)bounds.maxX() && local.y >= (double)bounds.minY() && local.y <= (double)bounds.maxY() && local.z >= (double)bounds.minZ() && local.z <= (double)bounds.maxZ();
    }

    @Nullable
    private static SubLevel disambiguateOverlappingCarts(List<SubLevel> matches, Entity entity) {
        SubLevel best = null;
        double bestDist = Double.MAX_VALUE;
        Vector3d worldPos = new Vector3d(entity.position().x, entity.position().y, entity.position().z);
        for (SubLevel candidate : matches) {
            double cz;
            double cy;
            BoundingBox3ic bounds;
            double cx;
            Vector3d local = candidate.logicalPose().transformPositionInverse((Vector3dc)worldPos, new Vector3d());
            double dist = local.distanceSquared(cx = Math.max((double)(bounds = candidate.getPlot().getBoundingBox()).minX(), Math.min(local.x, (double)bounds.maxX())), cy = Math.max((double)bounds.minY(), Math.min(local.y, (double)bounds.maxY())), cz = Math.max((double)bounds.minZ(), Math.min(local.z, (double)bounds.maxZ())));
            if (!(dist < bestDist)) continue;
            bestDist = dist;
            best = candidate;
        }
        return best;
    }
}

