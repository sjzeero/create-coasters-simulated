/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.kinetics.KineticDebugger
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.RegistryAccess
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track.graph;

import com.simibubi.create.content.kinetics.KineticDebugger;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.track.graph.CoasterCartDebugRenderer;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphDebugRenderer;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathTangentOrientation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class CoasterPathGraphManagerClient {
    private static final Map<ResourceKey<Level>, CoasterPathGraph> CLIENT_GRAPHS = new HashMap<ResourceKey<Level>, CoasterPathGraph>();
    private static final Set<ResourceKey<Level>> CLIENT_TANGENT_ALIGNED = new HashSet<ResourceKey<Level>>();
    @Nullable
    private static CompoundTag pendingClientGraphSyncRoot;

    private CoasterPathGraphManagerClient() {
    }

    public static CoasterPathGraph get(Level level) {
        CoasterPathGraphManagerClient.tryFlushPendingClientGraphSync();
        CoasterPathGraph g = CLIENT_GRAPHS.computeIfAbsent((ResourceKey<Level>)level.dimension(), d -> new CoasterPathGraph());
        CoasterPathGraphManagerClient.ensureClientTangentAligned(level, g);
        return g;
    }

    private static void ensureClientTangentAligned(Level level, CoasterPathGraph g) {
        if (!level.isClientSide() || g.edges().isEmpty()) {
            return;
        }
        ResourceKey dim = level.dimension();
        if (CLIENT_TANGENT_ALIGNED.contains(dim)) {
            return;
        }
        CoasterPathTangentOrientation.alignConnectedComponents(level, g);
        CLIENT_TANGENT_ALIGNED.add((ResourceKey<Level>)dim);
    }

    public static void applyClientSync(CompoundTag root) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            pendingClientGraphSyncRoot = root;
            return;
        }
        CoasterPathGraphManagerClient.applyClientGraphSyncNow(root);
    }

    private static void tryFlushPendingClientGraphSync() {
        if (pendingClientGraphSyncRoot == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        CompoundTag root = pendingClientGraphSyncRoot;
        pendingClientGraphSyncRoot = null;
        CoasterPathGraphManagerClient.applyClientGraphSyncNow(root);
    }

    private static void applyClientGraphSyncNow(CompoundTag root) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel clientLevel = mc.level;
        if (clientLevel == null) {
            pendingClientGraphSyncRoot = root;
            return;
        }
        CLIENT_TANGENT_ALIGNED.clear();
        ListTag list = root.getList("Dimensions", 10);
        RegistryAccess registries = clientLevel.registryAccess();
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag dim = list.getCompound(i);
            ResourceLocation id = ResourceLocation.parse((String)dim.getString("Dimension"));
            ResourceKey key = ResourceKey.create((ResourceKey)Registries.DIMENSION, (ResourceLocation)id);
            try {
                CoasterPathGraph g = CoasterPathGraph.read(dim.getCompound("Graph"), (HolderLookup.Provider)registries);
                CLIENT_GRAPHS.put((ResourceKey<Level>)key, g);
                if (!clientLevel.dimension().equals(key)) continue;
                CoasterPathTangentOrientation.alignConnectedComponents((Level)clientLevel, g);
                CLIENT_TANGENT_ALIGNED.add((ResourceKey<Level>)key);
                continue;
            }
            catch (Exception ex) {
                SimulatedCoasters.LOGGER.error("Failed to deserialize coaster path graph for {}", (Object)id, (Object)ex);
            }
        }
    }

    public static void clientTick() {
        CoasterPathGraphManagerClient.tryFlushPendingClientGraphSync();
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        if (!KineticDebugger.isF3DebugModeActive()) {
            CoasterPathGraphDebugRenderer.clearContinuityTraceState();
            CoasterCartDebugRenderer.clear();
            return;
        }
        CoasterPathGraph graph = CoasterPathGraphManagerClient.get((Level)level);
        CoasterPathGraphDebugRenderer.render((Level)level, graph);
        CoasterCartDebugRenderer.render((Level)level, graph);
    }
}

