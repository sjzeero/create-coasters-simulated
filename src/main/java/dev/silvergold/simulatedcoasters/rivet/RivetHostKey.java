/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.rivet;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public record RivetHostKey(@Nullable UUID hostSubLevelId, BlockPos cell) {
    public RivetHostKey(@Nullable UUID hostSubLevelId, BlockPos cell) {
        cell = cell.immutable();
    }

    public static RivetHostKey of(Level level, BlockPos cell) {
        SubLevel sub = Sable.HELPER.getContaining(level, (Vec3i)cell);
        if (sub != null && !sub.isRemoved()) {
            return new RivetHostKey(sub.getUniqueId(), cell);
        }
        return new RivetHostKey(null, cell);
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
        }
        if (!(obj instanceof RivetHostKey)) {
            return false;
        }
        RivetHostKey other = (RivetHostKey)obj;
        return Objects.equals(this.hostSubLevelId, other.hostSubLevelId) && this.cell.equals((Object)other.cell);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hostSubLevelId, this.cell);
    }
}

