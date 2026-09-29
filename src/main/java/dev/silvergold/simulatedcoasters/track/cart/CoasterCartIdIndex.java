/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartBearingTickCache;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

final class CoasterCartIdIndex {
    private static final Map<ResourceKey<Level>, DimensionIndex> BY_DIM = new HashMap<ResourceKey<Level>, DimensionIndex>();

    private CoasterCartIdIndex() {
    }

    @Nullable
    static ServerSubLevel find(ServerLevel level, UUID cartId) {
        CoasterCartIdIndex.ensureFresh(level);
        DimensionIndex index = BY_DIM.get(level.dimension());
        return index != null ? index.byId.get(cartId) : null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void invalidateDimension(ServerLevel level) {
        Map<ResourceKey<Level>, DimensionIndex> map = BY_DIM;
        synchronized (map) {
            BY_DIM.remove(level.dimension());
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void removeCart(ServerLevel level, ServerSubLevel sub) {
        Map<ResourceKey<Level>, DimensionIndex> map = BY_DIM;
        synchronized (map) {
            DimensionIndex index = BY_DIM.get(level.dimension());
            if (index != null) {
                index.byId.remove(sub.getUniqueId());
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static void clearDimension(ResourceKey<Level> dimension) {
        Map<ResourceKey<Level>, DimensionIndex> map = BY_DIM;
        synchronized (map) {
            BY_DIM.remove(dimension);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void ensureFresh(ServerLevel level) {
        long gameTime = level.getGameTime();
        ResourceKey dimension = level.dimension();
        Map<ResourceKey<Level>, DimensionIndex> map = BY_DIM;
        synchronized (map) {
            DimensionIndex existing = BY_DIM.get(dimension);
            if (existing != null && existing.gameTime == gameTime) {
                return;
            }
            CoasterCartIdIndex.rebuildLocked(level, gameTime, (ResourceKey<Level>)dimension);
        }
    }

    private static void rebuildLocked(ServerLevel level, long gameTime, ResourceKey<Level> dimension) {
        HashMap<UUID, ServerSubLevel> byId = new HashMap<UUID, ServerSubLevel>();
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (raw instanceof ServerSubLevelContainer) {
            ServerSubLevelContainer container = raw;
            for (ServerSubLevel sub : container.getAllSubLevels()) {
                if (sub.isRemoved() || CoasterCartBearingTickCache.scan(sub).representative() == null) continue;
                byId.put(sub.getUniqueId(), sub);
            }
        }
        BY_DIM.put(dimension, new DimensionIndex(gameTime, byId));
    }

    private record DimensionIndex(long gameTime, Map<UUID, ServerSubLevel> byId) {
    }
}

