package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class RivetDisassembly {
   private RivetDisassembly() {
   }

   @Nullable
   public static ServerSubLevel splitAttachedBlocks(ServerLevel level, ServerSubLevel rivetSub) {
      if (rivetSub.isRemoved()) {
         return null;
      } else {
         ObjectArrayList<BlockPos> blocks = collectAttachedBlocks(level, rivetSub.getPlot());
         if (blocks.isEmpty()) {
            return null;
         } else if (totalMass(level, blocks) <= 0.0) {
            SimulatedCoasters.LOGGER
               .debug("Rivet {} attachments are weightless — popping {} block(s) instead of assembling", rivetSub.getUniqueId(), blocks.size());
            popAttachedBlocks(level, blocks);
            return null;
         } else {
            BoundingBox3i bounds = BoundingBox3i.from(blocks);
            return bounds == null ? null : SubLevelAssemblyHelper.assembleBlocks(level, (BlockPos)blocks.getFirst(), blocks, bounds);
         }
      }
   }

   private static double totalMass(ServerLevel level, ObjectArrayList<BlockPos> blocks) {
      double mass = 0.0;
      ObjectListIterator var4 = blocks.iterator();

      while (var4.hasNext()) {
         BlockPos pos = (BlockPos)var4.next();
         BlockState state = level.getBlockState(pos);
         if (!state.isAir()) {
            mass += PhysicsBlockPropertyHelper.getMass(level, pos, state);
         }
      }

      return mass;
   }

   private static void popAttachedBlocks(ServerLevel level, ObjectArrayList<BlockPos> blocks) {
      ObjectListIterator var2 = blocks.iterator();

      while (var2.hasNext()) {
         BlockPos pos = (BlockPos)var2.next();
         if (!level.getBlockState(pos).isAir()) {
            level.destroyBlock(pos, true);
         }
      }
   }

   private static ObjectArrayList<BlockPos> collectAttachedBlocks(ServerLevel level, LevelPlot plot) {
      ObjectArrayList<BlockPos> blocks = new ObjectArrayList();
      RivetBlock rivet = (RivetBlock)SimulatedCoastersBlocks.RIVET.get();
      BlockPos rivetCell = plot.getCenterBlock();

      for (PlotChunkHolder chunk : plot.getLoadedChunks()) {
         BoundingBox3ic localChunkBounds = chunk.getBoundingBox();
         if (localChunkBounds != null && localChunkBounds != BoundingBox3i.EMPTY) {
            for (int x = localChunkBounds.minX(); x <= localChunkBounds.maxX(); x++) {
               for (int y = localChunkBounds.minY(); y <= localChunkBounds.maxY(); y++) {
                  for (int z = localChunkBounds.minZ(); z <= localChunkBounds.maxZ(); z++) {
                     BlockPos pos = new BlockPos(x + chunk.getPos().getMinBlockX(), y, z + chunk.getPos().getMinBlockZ());
                     if (!pos.equals(rivetCell)) {
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && !state.is(rivet)) {
                           blocks.add(pos.immutable());
                        }
                     }
                  }
               }
            }
         }
      }

      return blocks;
   }
}
