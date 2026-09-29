/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer
 *  dev.ryanhcode.sable.api.sublevel.SubLevelContainer
 *  dev.ryanhcode.sable.companion.math.BoundingBox3ic
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.SubLevel
 *  dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.Level
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.balloon;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import dev.silvergold.simulatedcoasters.balloon.BalloonPlacement;
import dev.silvergold.simulatedcoasters.balloon.BalloonResolve;
import dev.silvergold.simulatedcoasters.track.cart.CoasterCartResolve;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public record BalloonHostKey(UUID hostSubLevelId, BlockPos cell) {
    public static final BlockPos BALLOON_BLOCK_CELL = BlockPos.ZERO;

    public BalloonHostKey {
        cell = cell.immutable();
    }

    @Nullable
    public static BalloonHostKey of(Level level, BlockPos clicked) {
        SubLevel sub = CoasterCartResolve.findSubLevelAt(level, clicked);
        if (sub == null || sub.isRemoved()) {
            return null;
        }
        BlockPos plotCell = CoasterCartResolve.plotLocalCellAt(level, sub, clicked);
        if (plotCell == null) {
            return null;
        }
        if (!(sub instanceof ServerSubLevel)) {
            return null;
        }
        ServerSubLevel hostSub = (ServerSubLevel)sub;
        if (hostSub.getPlot().getEmbeddedLevelAccessor().getBlockState(plotCell).isAir()) {
            return null;
        }
        return new BalloonHostKey(sub.getUniqueId(), plotCell);
    }

    public static BlockPos normalizeHostPlotCell(ServerSubLevel hostSub, BlockPos stored) {
        BlockPos fromPlaySpace;
        if (BalloonHostKey.isLikelyPlotLocal(stored) && BalloonHostKey.hasBlockAt(hostSub, stored)) {
            return stored;
        }
        BlockPos fromCenterOffset = stored.subtract((Vec3i)hostSub.getPlot().getCenterBlock());
        if (BalloonHostKey.hasBlockAt(hostSub, fromCenterOffset)) {
            return fromCenterOffset;
        }
        if (hostSub.getLevel() != null && (fromPlaySpace = CoasterCartResolve.plotLocalCellAt((Level)hostSub.getLevel(), (SubLevel)hostSub, stored)) != null && BalloonHostKey.hasBlockAt(hostSub, fromPlaySpace)) {
            return fromPlaySpace;
        }
        return stored;
    }

    @Nullable
    public static BalloonHostKey relocateHostKey(ServerLevel level, BalloonBlockEntity be, @Nullable ServerSubLevel balloonSub) {
        BalloonHostKey fromTether;
        BalloonHostKey fromLattice;
        ServerSubLevel hostSub = be.resolveHostSubLevel(level);
        if (hostSub != null) {
            BlockPos plotLocal = BalloonHostKey.normalizeHostPlotCell(hostSub, be.hostPos());
            return new BalloonHostKey(hostSub.getUniqueId(), plotLocal);
        }
        BlockPos lattice = be.hostLatticePos();
        if (lattice != null && (fromLattice = BalloonHostKey.of((Level)level, lattice)) != null) {
            return fromLattice;
        }
        if (balloonSub != null && (fromTether = BalloonHostKey.locateByStringTie(level, balloonSub)) != null) {
            return fromTether;
        }
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        ArrayList<BalloonHostKey> candidates = new ArrayList<BalloonHostKey>();
        for (SubLevel sub : container.getAllSubLevels()) {
            BlockPos plotLocal;
            ServerSubLevel ssl;
            if (!(sub instanceof ServerSubLevel) || (ssl = (ServerSubLevel)sub).isRemoved() || BalloonResolve.isBalloonSubLevel((SubLevel)ssl) || !BalloonHostKey.hasBlockAt(ssl, plotLocal = BalloonHostKey.normalizeHostPlotCell(ssl, be.hostPos()))) continue;
            candidates.add(new BalloonHostKey(ssl.getUniqueId(), plotLocal));
        }
        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return (BalloonHostKey)candidates.get(0);
        }
        if (balloonSub == null) {
            return (BalloonHostKey)candidates.get(0);
        }
        Vector3d stringWorld = balloonSub.logicalPose().transformPosition((Vector3dc)BalloonPlacement.stringTiePlot((SubLevel)balloonSub, BALLOON_BLOCK_CELL), new Vector3d());
        BalloonHostKey best = null;
        double bestDistSq = Double.MAX_VALUE;
        double maxDistSq = 64.0;
        for (BalloonHostKey candidate : candidates) {
            double distSq;
            Vector3d anchor;
            ServerSubLevel host = BalloonPlacement.resolveHostSub((Level)level, candidate.hostSubLevelId());
            if (host == null || (anchor = BalloonPlacement.hostAnchorWorld(host, candidate.cell())) == null || !((distSq = anchor.distanceSquared((Vector3dc)stringWorld)) < bestDistSq) || !(distSq <= maxDistSq)) continue;
            bestDistSq = distSq;
            best = candidate;
        }
        return best != null ? best : (BalloonHostKey)candidates.get(0);
    }

    @Nullable
    public static BalloonHostKey locateByStringTie(ServerLevel level, ServerSubLevel balloonSub) {
        Vector3d stringWorld = balloonSub.logicalPose().transformPosition((Vector3dc)BalloonPlacement.stringTiePlot((SubLevel)balloonSub, BALLOON_BLOCK_CELL), new Vector3d());
        ServerSubLevelContainer raw = SubLevelContainer.getContainer((ServerLevel)level);
        if (!(raw instanceof ServerSubLevelContainer)) {
            return null;
        }
        ServerSubLevelContainer container = raw;
        BalloonHostKey best = null;
        double bestScore = Double.MAX_VALUE;
        double maxHorizSq = 144.0;
        for (SubLevel sub : container.getAllSubLevels()) {
            ServerSubLevel hostSub;
            if (!(sub instanceof ServerSubLevel) || (hostSub = (ServerSubLevel)sub).isRemoved() || BalloonResolve.isBalloonSubLevel((SubLevel)hostSub)) continue;
            EmbeddedPlotLevelAccessor embedded = hostSub.getPlot().getEmbeddedLevelAccessor();
            BoundingBox3ic bounds = hostSub.getPlot().getBoundingBox();
            BlockPos center = hostSub.getPlot().getCenterBlock();
            for (int x = bounds.minX(); x <= bounds.maxX(); ++x) {
                for (int y = bounds.minY(); y <= bounds.maxY(); ++y) {
                    for (int z = bounds.minZ(); z <= bounds.maxZ(); ++z) {
                        double score;
                        double horizSq;
                        double dy;
                        Vector3d anchor;
                        BlockPos plotLocal = new BlockPos(x, y, z).subtract((Vec3i)center);
                        if (embedded.getBlockState(plotLocal).isAir() || (anchor = BalloonPlacement.hostAnchorWorld(hostSub, plotLocal)) == null || (dy = stringWorld.y - anchor.y) < -0.5 || (horizSq = (anchor.x - stringWorld.x) * (anchor.x - stringWorld.x) + (anchor.z - stringWorld.z) * (anchor.z - stringWorld.z)) > maxHorizSq || !((score = horizSq + dy * dy * 0.25) < bestScore)) continue;
                        bestScore = score;
                        best = new BalloonHostKey(hostSub.getUniqueId(), plotLocal);
                    }
                }
            }
        }
        return best;
    }

    private static boolean isLikelyPlotLocal(BlockPos pos) {
        return Math.abs(pos.getX()) < 4096 && Math.abs(pos.getZ()) < 4096;
    }

    private static boolean hasBlockAt(ServerSubLevel hostSub, BlockPos plotCell) {
        return !hostSub.getPlot().getEmbeddedLevelAccessor().getBlockState(plotCell).isAir();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BalloonHostKey)) {
            return false;
        }
        BalloonHostKey other = (BalloonHostKey)obj;
        return this.hostSubLevelId.equals(other.hostSubLevelId) && this.cell.equals((Object)other.cell);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.hostSubLevelId, this.cell);
    }
}

