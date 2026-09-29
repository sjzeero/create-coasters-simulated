package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ChainLiftEndRef(ChainLiftEndRef.Kind kind, BlockPos endAnchor, BlockPos edgeFrom, BlockPos edgeTo, double endT) {
   public static final StreamCodec<RegistryFriendlyByteBuf, ChainLiftEndRef> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      ref -> ref.kind().ordinal(),
      BlockPos.STREAM_CODEC,
      ChainLiftEndRef::endAnchor,
      BlockPos.STREAM_CODEC,
      ChainLiftEndRef::edgeFrom,
      BlockPos.STREAM_CODEC,
      ChainLiftEndRef::edgeTo,
      ByteBufCodecs.DOUBLE,
      ChainLiftEndRef::endT,
      (kindOrdinal, endAnchor, edgeFrom, edgeTo, endT) -> {
         ChainLiftEndRef.Kind kind = ChainLiftEndRef.Kind.values()[Math.floorMod(kindOrdinal, ChainLiftEndRef.Kind.values().length)];
         return new ChainLiftEndRef(kind, endAnchor, edgeFrom, edgeTo, endT);
      }
   );

   public static ChainLiftEndRef anchor(BlockPos anchor) {
      return new ChainLiftEndRef(ChainLiftEndRef.Kind.ANCHOR, anchor, BlockPos.ZERO, BlockPos.ZERO, 0.0);
   }

   public static ChainLiftEndRef curve(BlockPos edgeFrom, BlockPos edgeTo, double endT) {
      return new ChainLiftEndRef(ChainLiftEndRef.Kind.CURVE, BlockPos.ZERO, edgeFrom, edgeTo, endT);
   }

   public void write(CompoundTag tag) {
      tag.putString("Kind", this.kind.name());
      tag.putLong("EndAnchor", this.endAnchor.asLong());
      tag.putLong("EdgeFrom", this.edgeFrom.asLong());
      tag.putLong("EdgeTo", this.edgeTo.asLong());
      tag.putDouble("EndT", this.endT);
   }

   public static ChainLiftEndRef read(CompoundTag tag) {
      ChainLiftEndRef.Kind kind = ChainLiftEndRef.Kind.valueOf(tag.getString("Kind"));
      return new ChainLiftEndRef(
         kind,
         BlockPos.of(tag.getLong("EndAnchor")),
         BlockPos.of(tag.getLong("EdgeFrom")),
         BlockPos.of(tag.getLong("EdgeTo")),
         tag.contains("EndT", 6) ? tag.getDouble("EndT") : 0.0
      );
   }

   public ChainLiftEndRef applyAssemblyTransform(AssemblyTransform asm) {
      return new ChainLiftEndRef(this.kind, remapPos(asm, this.endAnchor), remapPos(asm, this.edgeFrom), remapPos(asm, this.edgeTo), this.endT);
   }

   private static BlockPos remapPos(AssemblyTransform asm, BlockPos pos) {
      return pos != null && !pos.equals(BlockPos.ZERO) ? asm.apply(pos).immutable() : BlockPos.ZERO;
   }

   public static enum Kind {
      ANCHOR,
      CURVE;
   }
}
