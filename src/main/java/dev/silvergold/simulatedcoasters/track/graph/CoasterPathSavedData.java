/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.HolderLookup$Provider
 *  net.minecraft.core.registries.Registries
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.saveddata.SavedData
 *  net.minecraft.world.level.saveddata.SavedData$Factory
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.graph;

import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraph;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

public class CoasterPathSavedData
extends SavedData {
    public static final String DATA_ID = "simulatedcoasters_coaster_paths";
    private final Map<ResourceKey<Level>, CoasterPathGraph> graphs = new HashMap<ResourceKey<Level>, CoasterPathGraph>();

    public CoasterPathGraph graphFor(ResourceKey<Level> dimension) {
        return this.graphs.computeIfAbsent(dimension, d -> new CoasterPathGraph());
    }

    public Map<ResourceKey<Level>, CoasterPathGraph> allGraphs() {
        return this.graphs;
    }

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<ResourceKey<Level>, CoasterPathGraph> e : this.graphs.entrySet()) {
            CompoundTag dim = new CompoundTag();
            dim.putString("Dimension", e.getKey().location().toString());
            dim.put("Graph", (Tag)e.getValue().write(registries));
            list.add((Object)dim);
        }
        tag.put("Dimensions", (Tag)list);
        return tag;
    }

    private static CoasterPathSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        CoasterPathSavedData data = new CoasterPathSavedData();
        ListTag list = tag.getList("Dimensions", 10);
        for (int i = 0; i < list.size(); ++i) {
            CompoundTag dim = list.getCompound(i);
            ResourceLocation id = ResourceLocation.parse((String)dim.getString("Dimension"));
            ResourceKey key = ResourceKey.create((ResourceKey)Registries.DIMENSION, (ResourceLocation)id);
            data.graphs.put((ResourceKey<Level>)key, CoasterPathGraph.read(dim.getCompound("Graph"), registries));
        }
        return data;
    }

    public static SavedData.Factory<CoasterPathSavedData> factory() {
        return new SavedData.Factory(CoasterPathSavedData::new, CoasterPathSavedData::load, null);
    }

    public static CoasterPathSavedData get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            throw new IllegalStateException("Coaster path saved data is unavailable while the overworld is still loading");
        }
        return (CoasterPathSavedData)overworld.getDataStorage().computeIfAbsent(CoasterPathSavedData.factory(), DATA_ID);
    }

    @Nullable
    public static CoasterPathSavedData tryGet(ServerLevel level) {
        ServerLevel storage = level.getServer().overworld();
        if (storage == null) {
            if (!level.dimension().equals(Level.OVERWORLD)) {
                return null;
            }
            storage = level;
        }
        return (CoasterPathSavedData)storage.getDataStorage().computeIfAbsent(CoasterPathSavedData.factory(), DATA_ID);
    }

    private CoasterPathSavedData() {
    }
}

