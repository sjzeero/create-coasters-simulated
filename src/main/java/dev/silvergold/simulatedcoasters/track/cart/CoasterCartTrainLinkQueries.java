/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.world.level.Level
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.fml.loading.FMLEnvironment
 */
package dev.silvergold.simulatedcoasters.track.cart;

import dev.silvergold.simulatedcoasters.client.cart.CoasterCartTrainLinkClientState;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

final class CoasterCartTrainLinkQueries {
    private CoasterCartTrainLinkQueries() {
    }

    static boolean clientHasLinkBetween(ResourceKey<Level> dimension, UUID cartA, UUID cartB) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return false;
        }
        return ClientHolder.hasLinkBetween(dimension, cartA, cartB);
    }

    static List<UUID> clientPartnerIds(ResourceKey<Level> dimension, UUID cartId) {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return List.of();
        }
        return ClientHolder.partnerIds(dimension, cartId);
    }

    private static final class ClientHolder {
        private ClientHolder() {
        }

        static boolean hasLinkBetween(ResourceKey<Level> dimension, UUID cartA, UUID cartB) {
            return CoasterCartTrainLinkClientState.hasLinkBetween(dimension, cartA, cartB);
        }

        static List<UUID> partnerIds(ResourceKey<Level> dimension, UUID cartId) {
            return CoasterCartTrainLinkClientState.partnerIds(dimension, cartId);
        }
    }
}

