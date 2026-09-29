package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphSpans;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public record ChainLiftCommittedSegment(BlockPos edgeFrom, BlockPos edgeTo, double tStart, double tEnd) {
   public static ChainLiftCommittedSegment from(CoasterPathGraphSpans.PathSegment segment) {
      return new ChainLiftCommittedSegment(segment.edge().from(), segment.edge().to(), segment.tStart(), segment.tEnd());
   }

   public void write(CompoundTag tag) {
      tag.putLong("EdgeFrom", this.edgeFrom.asLong());
      tag.putLong("EdgeTo", this.edgeTo.asLong());
      tag.putDouble("TStart", this.tStart);
      tag.putDouble("TEnd", this.tEnd);
   }

   public static ChainLiftCommittedSegment read(CompoundTag tag) {
      return new ChainLiftCommittedSegment(
         BlockPos.of(tag.getLong("EdgeFrom")), BlockPos.of(tag.getLong("EdgeTo")), tag.getDouble("TStart"), tag.getDouble("TEnd")
      );
   }

   public static ListTag writeList(List<ChainLiftCommittedSegment> segments) {
      ListTag list = new ListTag();

      for (ChainLiftCommittedSegment segment : segments) {
         CompoundTag tag = new CompoundTag();
         segment.write(tag);
         list.add(tag);
      }

      return list;
   }

   public static List<ChainLiftCommittedSegment> readList(ListTag list) {
      List<ChainLiftCommittedSegment> out = new ArrayList<>(list.size());

      for (Tag value : list) {
         if (value instanceof CompoundTag tag) {
            out.add(read(tag));
         }
      }

      return List.copyOf(out);
   }
}
