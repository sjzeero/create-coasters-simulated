/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.fml.loading.FMLEnvironment
 */
package dev.silvergold.simulatedcoasters.track.graph;

import dev.silvergold.simulatedcoasters.client.track.graph.CoasterPathGraphManagerClient;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

public final class CoasterPathGraphLookup {
    private CoasterPathGraphLookup() {
    }

    public static CoasterPathGraph get(Level level) {
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)level;
            return CoasterPathGraphManager.get(sl);
        }
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return new CoasterPathGraph();
        }
        return ClientHolder.get(level);
    }

    private static final class ClientHolder {
        private ClientHolder() {
        }

        static CoasterPathGraph get(Level level) {
            return CoasterPathGraphManagerClient.get(level);
        }
    }
}

