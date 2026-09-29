/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.Sable
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.BlockHitResult
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class BalloonResolve {
    private BalloonResolve() {
    }

    public static boolean isBalloonSubLevel(SubLevel sub) {
        return sub.getPlot().getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO).is((Block)SimulatedCoastersBlocks.RED_BALLOON.get());
    }

    public static boolean targetsBalloonCell(Level level, BlockPos worldCell) {
        SubLevel sub = Sable.HELPER.getContaining(level, (Vec3i)worldCell);
        if (sub == null || sub.isRemoved()) {
            return false;
        }
        if (!BalloonResolve.isBalloonSubLevel(sub)) {
            return false;
        }
        return sub.getPlot().getCenterBlock().equals((Object)worldCell);
    }

    @Nullable
    public static ServerSubLevel findBalloonAt(ServerLevel level, BlockPos worldCell) {
        ServerSubLevel server;
        SubLevel sub = Sable.HELPER.getContaining((Level)level, (Vec3i)worldCell);
        if (!(sub instanceof ServerSubLevel) || (server = (ServerSubLevel)sub).isRemoved()) {
            return null;
        }
        if (!BalloonResolve.isBalloonSubLevel((SubLevel)server)) {
            return null;
        }
        if (!server.getPlot().getCenterBlock().equals((Object)worldCell)) {
            return null;
        }
        return server;
    }

    @Nullable
    public static ServerSubLevel findBalloonAtImpact(ServerLevel level, BlockHitResult hit) {
        ServerSubLevel server;
        ServerSubLevel balloon = BalloonResolve.findBalloonAt(level, hit.getBlockPos());
        if (balloon != null) {
            return balloon;
        }
        balloon = BalloonResolve.findBalloonAt(level, BlockPos.containing((Position)hit.getLocation()));
        if (balloon != null) {
            return balloon;
        }
        SubLevel sub = Sable.HELPER.getContaining((Level)level, (Position)hit.getLocation());
        if (sub instanceof ServerSubLevel && !(server = (ServerSubLevel)sub).isRemoved() && BalloonResolve.isBalloonSubLevel((SubLevel)server)) {
            return server;
        }
        return null;
    }

    public static boolean isBalloonBlockState(BlockState state) {
        return state.is((Block)SimulatedCoastersBlocks.RED_BALLOON.get());
    }

    @Nullable
    public static ServerSubLevel findBalloonById(ServerLevel level, UUID id) {
        ServerSubLevel server;
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        SubLevel sub = container.getSubLevel(id);
        if (!(sub instanceof ServerSubLevel) || (server = (ServerSubLevel)sub).isRemoved() || !BalloonResolve.isBalloonSubLevel((SubLevel)server)) {
            return null;
        }
        return server;
    }
}

