/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.Level
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 */
package dev.silvergold.simulatedcoasters.client.cart;

import dev.silvergold.simulatedcoasters.client.cart.CoasterCartClientMembership;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartTrainLinkSync;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(value=Dist.CLIENT)
public final class CoasterCartTrainLinkClientState {
    private static final Map<ResourceKey<Level>, Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>>> LINKS_BY_DIMENSION = new ConcurrentHashMap<ResourceKey<Level>, Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>>>();

    private CoasterCartTrainLinkClientState() {
    }

    public static void applySync(ResourceKey<Level> dimension, UUID cartId, List<CoasterCartTrainLinkSync.LinkEntry> links) {
        CoasterCartClientMembership.noteCartSignal(cartId);
        if (links.isEmpty()) {
            Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>> dim = LINKS_BY_DIMENSION.get(dimension);
            if (dim != null) {
                dim.remove(cartId);
            }
            return;
        }
        LINKS_BY_DIMENSION.computeIfAbsent(dimension, d -> new ConcurrentHashMap()).put(cartId, List.copyOf(links));
    }

    public static List<CoasterCartTrainLinkSync.LinkEntry> linksFor(ResourceKey<Level> dimension, UUID cartId) {
        Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>> dim = LINKS_BY_DIMENSION.get(dimension);
        if (dim == null) {
            return List.of();
        }
        return dim.getOrDefault(cartId, List.of());
    }

    public static boolean hasLinkBetween(ResourceKey<Level> dimension, UUID cartA, UUID cartB) {
        if (cartA.equals(cartB)) {
            return true;
        }
        for (CoasterCartTrainLinkSync.LinkEntry entry : CoasterCartTrainLinkClientState.linksFor(dimension, cartA)) {
            if (!entry.partnerId().equals(cartB)) continue;
            return true;
        }
        for (CoasterCartTrainLinkSync.LinkEntry entry : CoasterCartTrainLinkClientState.linksFor(dimension, cartB)) {
            if (!entry.partnerId().equals(cartA)) continue;
            return true;
        }
        return false;
    }

    public static List<UUID> partnerIds(ResourceKey<Level> dimension, UUID cartId) {
        return CoasterCartTrainLinkClientState.linksFor(dimension, cartId).stream().map(CoasterCartTrainLinkSync.LinkEntry::partnerId).toList();
    }

    public static void clearDimension(ResourceKey<Level> dimension) {
        LINKS_BY_DIMENSION.remove(dimension);
    }

    public static Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>> snapshot(ResourceKey<Level> dimension) {
        Map<UUID, List<CoasterCartTrainLinkSync.LinkEntry>> dim = LINKS_BY_DIMENSION.get(dimension);
        if (dim == null || dim.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(dim);
    }
}

