/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostKey;
import dev.silvergold.simulatedcoasters.balloon.BalloonRemoval;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class BalloonIntegrity {
    private BalloonIntegrity() {
    }

    public static void onPlotBlockChanged(ServerLevel level, BlockPos latticePos, BlockState newState) {
        ServerSubLevel balloon;
        if (newState.isAir()) {
            return;
        }
        @Nullable SubLevel sub = Sable.HELPER.getContaining((Level)level, (Vec3i)latticePos);
        if (!(sub instanceof ServerSubLevel) || (balloon = (ServerSubLevel)sub).isRemoved()) {
            return;
        }
        if (!BalloonResolve.isBalloonSubLevel((SubLevel)balloon)) {
            return;
        }
        BlockPos plotLocal = latticePos.subtract((Vec3i)balloon.getPlot().getCenterBlock());
        if (plotLocal.equals((Object)BalloonHostKey.BALLOON_BLOCK_CELL) && BalloonResolve.isBalloonBlockState(newState)) {
            return;
        }
        BalloonRemoval.pop(level, balloon);
    }
}

