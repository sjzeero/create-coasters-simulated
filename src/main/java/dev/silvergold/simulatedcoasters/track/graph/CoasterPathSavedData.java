package dev.silvergold.simulatedcoasters.track.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedData.Factory;
import org.jetbrains.annotations.Nullable;

public class CoasterPathSavedData extends SavedData {
   public static final String DATA_ID = "simulatedcoasters_coaster_paths";
   private final Map<ResourceKey<Level>, CoasterPathGraph> graphs = new HashMap<>();

   public CoasterPathGraph graphFor(ResourceKey<Level> dimension) {
      return this.graphs.computeIfAbsent(dimension, d -> new CoasterPathGraph());
   }

   public Map<ResourceKey<Level>, CoasterPathGraph> allGraphs() {
      return this.graphs;
   }

   public CompoundTag save(CompoundTag tag, Provider registries) {
      ListTag list = new ListTag();

      for (Entry<ResourceKey<Level>, CoasterPathGraph> e : this.graphs.entrySet()) {
         CompoundTag dim = new CompoundTag();
         dim.putString("Dimension", e.getKey().location().toString());
         dim.put("Graph", e.getValue().write(registries));
         list.add(dim);
      }

      tag.put("Dimensions", list);
      return tag;
   }

   private static CoasterPathSavedData load(CompoundTag tag, Provider registries) {
      CoasterPathSavedData data = new CoasterPathSavedData();
      ListTag list = tag.getList("Dimensions", 10);

      for (int i = 0; i < list.size(); i++) {
         CompoundTag dim = list.getCompound(i);
         ResourceLocation id = ResourceLocation.parse(dim.getString("Dimension"));
         ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
         data.graphs.put(key, CoasterPathGraph.read(dim.getCompound("Graph"), registries));
      }

      return data;
   }

   public static Factory<CoasterPathSavedData> factory() {
      return new SavedData.Factory<>(CoasterPathSavedData::new, CoasterPathSavedData::load, null);
   }

   public static CoasterPathSavedData get(MinecraftServer server) {
      ServerLevel overworld = server.overworld();
      if (overworld == null) {
         throw new IllegalStateException("Coaster path saved data is unavailable while the overworld is still loading");
      } else {
         return (CoasterPathSavedData)overworld.getDataStorage().computeIfAbsent(factory(), "simulatedcoasters_coaster_paths");
      }
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

      return (CoasterPathSavedData)storage.getDataStorage().computeIfAbsent(factory(), "simulatedcoasters_coaster_paths");
   }

   private CoasterPathSavedData() {
   }
}
