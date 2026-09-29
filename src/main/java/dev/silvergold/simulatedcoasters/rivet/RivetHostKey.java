package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public record RivetHostKey(@Nullable UUID hostSubLevelId, BlockPos cell) {
   public RivetHostKey(@Nullable UUID hostSubLevelId, BlockPos cell) {
      cell = cell.immutable();
      this.hostSubLevelId = hostSubLevelId;
      this.cell = cell;
   }

   public static RivetHostKey of(Level level, BlockPos cell) {
      SubLevel sub = Sable.HELPER.getContaining(level, cell);
      return sub != null && !sub.isRemoved() ? new RivetHostKey(sub.getUniqueId(), cell) : new RivetHostKey(null, cell);
   }

   public static boolean isRivetSubLevel(SubLevel sub) {
      return sub.getPlot().getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO).is((Block)SimulatedCoastersBlocks.RIVET.get());
   }

   public static RivetHostKey world(BlockPos cell) {
      return new RivetHostKey(null, cell.immutable());
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else {
         return !(obj instanceof RivetHostKey other) ? false : Objects.equals(this.hostSubLevelId, other.hostSubLevelId) && this.cell.equals(other.cell);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.hostSubLevelId, this.cell);
   }
}
