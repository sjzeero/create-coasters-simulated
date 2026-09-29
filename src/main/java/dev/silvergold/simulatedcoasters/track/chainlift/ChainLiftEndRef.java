/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.RegistryFriendlyByteBuf
 *  net.minecraft.network.codec.ByteBufCodecs
 *  net.minecraft.network.codec.StreamCodec
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ChainLiftEndRef(Kind kind, BlockPos endAnchor, BlockPos edgeFrom, BlockPos edgeTo, double endT) {
    public static final StreamCodec<RegistryFriendlyByteBuf, ChainLiftEndRef> STREAM_CODEC = StreamCodec.composite((StreamCodec)ByteBufCodecs.VAR_INT, ref -> ref.kind().ordinal(), (StreamCodec)BlockPos.STREAM_CODEC, ChainLiftEndRef::endAnchor, (StreamCodec)BlockPos.STREAM_CODEC, ChainLiftEndRef::edgeFrom, (StreamCodec)BlockPos.STREAM_CODEC, ChainLiftEndRef::edgeTo, (StreamCodec)ByteBufCodecs.DOUBLE, ChainLiftEndRef::endT, (kindOrdinal, endAnchor, edgeFrom, edgeTo, endT) -> {
        Kind kind = Kind.values()[Math.floorMod(kindOrdinal, Kind.values().length)];
        return new ChainLiftEndRef(kind, (BlockPos)endAnchor, (BlockPos)edgeFrom, (BlockPos)edgeTo, (double)endT);
    });

    public static ChainLiftEndRef anchor(BlockPos anchor) {
        return new ChainLiftEndRef(Kind.ANCHOR, anchor, BlockPos.ZERO, BlockPos.ZERO, 0.0);
    }

    public static ChainLiftEndRef curve(BlockPos edgeFrom, BlockPos edgeTo, double endT) {
        return new ChainLiftEndRef(Kind.CURVE, BlockPos.ZERO, edgeFrom, edgeTo, endT);
    }

    public void write(CompoundTag tag) {
        tag.putString("Kind", this.kind.name());
        tag.putLong("EndAnchor", this.endAnchor.asLong());
        tag.putLong("EdgeFrom", this.edgeFrom.asLong());
        tag.putLong("EdgeTo", this.edgeTo.asLong());
        tag.putDouble("EndT", this.endT);
    }

    public static ChainLiftEndRef read(CompoundTag tag) {
        Kind kind = Kind.valueOf(tag.getString("Kind"));
        return new ChainLiftEndRef(kind, BlockPos.of((long)tag.getLong("EndAnchor")), BlockPos.of((long)tag.getLong("EdgeFrom")), BlockPos.of((long)tag.getLong("EdgeTo")), tag.contains("EndT", 6) ? tag.getDouble("EndT") : 0.0);
    }

    public ChainLiftEndRef applyAssemblyTransform(SubLevelAssemblyHelper.AssemblyTransform asm) {
        return new ChainLiftEndRef(this.kind, ChainLiftEndRef.remapPos(asm, this.endAnchor), ChainLiftEndRef.remapPos(asm, this.edgeFrom), ChainLiftEndRef.remapPos(asm, this.edgeTo), this.endT);
    }

    private static BlockPos remapPos(SubLevelAssemblyHelper.AssemblyTransform asm, BlockPos pos) {
        return pos == null || pos.equals((Object)BlockPos.ZERO) ? BlockPos.ZERO : asm.apply(pos).immutable();
    }

    public static enum Kind {
        ANCHOR,
        CURVE;

    }
}

