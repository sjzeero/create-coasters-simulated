/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.kinetics.RotationPropagator
 *  com.simibubi.create.content.kinetics.base.KineticBlockEntity
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track.chainlift;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class ChainLiftKineticConflict {
    private static final ThreadLocal<Boolean> REDIRECTING = ThreadLocal.withInitial(() -> false);

    private ChainLiftKineticConflict() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static boolean redirectDestroyAwayFromAnchor(Level level, BlockPos pos, boolean drop) {
        CoasterAnchorpointBlockEntity anchor;
        if (level.isClientSide() || Boolean.TRUE.equals(REDIRECTING.get())) {
            return level.destroyBlock(pos, drop);
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity) || !(anchor = (CoasterAnchorpointBlockEntity)blockEntity).hasChainLiftShaft()) {
            return level.destroyBlock(pos, drop);
        }
        KineticBlockEntity weaker = ChainLiftKineticConflict.findWeakerShaftNeighbour(level, anchor);
        if (weaker == null) {
            return level.destroyBlock(pos, drop);
        }
        REDIRECTING.set(true);
        try {
            boolean bl = level.destroyBlock(weaker.getBlockPos(), drop);
            return bl;
        }
        finally {
            REDIRECTING.set(false);
        }
    }

    @Nullable
    private static KineticBlockEntity findWeakerShaftNeighbour(Level level, CoasterAnchorpointBlockEntity anchor) {
        Direction.Axis axis = anchor.getChainLiftShaftAxis();
        if (axis == null) {
            return null;
        }
        KineticBlockEntity weaker = null;
        float weakerAbsSpeed = Float.POSITIVE_INFINITY;
        long weakerPosKey = Long.MAX_VALUE;
        for (Direction face : Direction.values()) {
            boolean tieBreak;
            KineticBlockEntity neighbour;
            BlockPos neighbourPos;
            BlockEntity blockEntity;
            if (face.getAxis() != axis || !((blockEntity = level.getBlockEntity(neighbourPos = anchor.getBlockPos().relative(face))) instanceof KineticBlockEntity) || (neighbour = (KineticBlockEntity)blockEntity) instanceof CoasterAnchorpointBlockEntity || !RotationPropagator.isConnected((KineticBlockEntity)anchor, (KineticBlockEntity)neighbour) && !RotationPropagator.isConnected((KineticBlockEntity)neighbour, (KineticBlockEntity)anchor)) continue;
            float absSpeed = Math.abs(neighbour.getTheoreticalSpeed());
            long posKey = neighbourPos.asLong();
            boolean weakerSpeed = absSpeed < weakerAbsSpeed - 1.0E-4f;
            boolean bl = tieBreak = Math.abs(absSpeed - weakerAbsSpeed) <= 1.0E-4f && posKey < weakerPosKey;
            if (weaker != null && !weakerSpeed && !tieBreak) continue;
            weaker = neighbour;
            weakerAbsSpeed = absSpeed;
            weakerPosKey = posKey;
        }
        return weaker;
    }
}

