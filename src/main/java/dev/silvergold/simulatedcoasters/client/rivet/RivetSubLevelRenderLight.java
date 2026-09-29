package dev.silvergold.simulatedcoasters.client.rivet;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.rivet.RivetBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class RivetSubLevelRenderLight {
   private static final ThreadLocal<BlockPos> PROBE = new ThreadLocal<>();

   private RivetSubLevelRenderLight() {
   }

   public static void begin(ClientSubLevel sub, BlockState state) {
      if (state.is((Block)SimulatedCoastersBlocks.RIVET.get())) {
         ClientLevel be = sub.getLevel();
         if (be instanceof ClientLevel) {
            RivetBlockEntity bex = sub.getPlot().getEmbeddedLevelAccessor().getBlockEntity(BlockPos.ZERO) instanceof RivetBlockEntity rivetBe ? rivetBe : null;
            PROBE.set(RivetSubLevelLightSample.probeCell(be, sub, state, bex));
         }
      }
   }

   public static void end() {
      PROBE.remove();
   }

   @Nullable
   public static BlockPos probePos() {
      return PROBE.get();
   }
}
