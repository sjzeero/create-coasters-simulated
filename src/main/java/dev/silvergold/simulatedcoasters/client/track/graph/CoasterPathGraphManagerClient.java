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
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class CoasterPathGraphManagerClient {
   private static final Map<ResourceKey<Level>, CoasterPathGraph> CLIENT_GRAPHS = new HashMap<>();
   private static final Set<ResourceKey<Level>> CLIENT_TANGENT_ALIGNED = new HashSet<>();
   @Nullable
   private static CompoundTag pendingClientGraphSyncRoot;

   private CoasterPathGraphManagerClient() {
   }

   public static CoasterPathGraph get(Level level) {
      tryFlushPendingClientGraphSync();
      CoasterPathGraph g = CLIENT_GRAPHS.computeIfAbsent(level.dimension(), d -> new CoasterPathGraph());
      ensureClientTangentAligned(level, g);
      return g;
   }

   private static void ensureClientTangentAligned(Level level, CoasterPathGraph g) {
      if (level.isClientSide() && !g.edges().isEmpty()) {
         ResourceKey<Level> dim = level.dimension();
         if (!CLIENT_TANGENT_ALIGNED.contains(dim)) {
            CoasterPathTangentOrientation.alignConnectedComponents(level, g);
            CLIENT_TANGENT_ALIGNED.add(dim);
         }
      }
   }

   public static void applyClientSync(CompoundTag root) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) {
         pendingClientGraphSyncRoot = root;
      } else {
         applyClientGraphSyncNow(root);
      }
   }

   private static void tryFlushPendingClientGraphSync() {
      if (pendingClientGraphSyncRoot != null) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.level != null) {
            CompoundTag root = pendingClientGraphSyncRoot;
            pendingClientGraphSyncRoot = null;
            applyClientGraphSyncNow(root);
         }
      }
   }

   private static void applyClientGraphSyncNow(CompoundTag root) {
      Minecraft mc = Minecraft.getInstance();
      Level clientLevel = mc.level;
      if (clientLevel == null) {
         pendingClientGraphSyncRoot = root;
      } else {
         CLIENT_TANGENT_ALIGNED.clear();
         ListTag list = root.getList("Dimensions", 10);
         Provider registries = clientLevel.registryAccess();

         for (int i = 0; i < list.size(); i++) {
            CompoundTag dim = list.getCompound(i);
            ResourceLocation id = ResourceLocation.parse(dim.getString("Dimension"));
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);

            try {
               CoasterPathGraph g = CoasterPathGraph.read(dim.getCompound("Graph"), registries);
               CLIENT_GRAPHS.put(key, g);
               if (clientLevel.dimension().equals(key)) {
                  CoasterPathTangentOrientation.alignConnectedComponents(clientLevel, g);
                  CLIENT_TANGENT_ALIGNED.add(key);
               }
            } catch (Exception var10) {
               SimulatedCoasters.LOGGER.error("Failed to deserialize coaster path graph for {}", id, var10);
            }
         }
      }
   }

   public static void clientTick() {
      tryFlushPendingClientGraphSync();
      Minecraft mc = Minecraft.getInstance();
      Level level = mc.level;
      if (level != null) {
         if (!KineticDebugger.isF3DebugModeActive()) {
            CoasterPathGraphDebugRenderer.clearContinuityTraceState();
            CoasterCartDebugRenderer.clear();
         } else {
            CoasterPathGraph graph = get(level);
            CoasterPathGraphDebugRenderer.render(level, graph);
            CoasterCartDebugRenderer.render(level, graph);
         }
      }
   }
}
