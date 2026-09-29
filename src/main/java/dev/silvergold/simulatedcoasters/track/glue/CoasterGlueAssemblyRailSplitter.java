/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.SoundType
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 */
package dev.silvergold.simulatedcoasters.track.glue;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.track.CoasterTrackCurveDrops;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CoasterGlueAssemblyRailSplitter {
    private CoasterGlueAssemblyRailSplitter() {
    }

    public static void splitRailsSpanningGlueBoundary(ServerLevel level, Iterable<BlockPos> glueBlocks) {
        HashSet<BlockPos> glue = new HashSet<BlockPos>();
        for (BlockPos p : glueBlocks) {
            if (p == null) continue;
            glue.add(p.immutable());
        }
        if (glue.isEmpty()) {
            return;
        }
        for (BlockPos pos : glue) {
            BlockEntity blockEntity;
            if (!level.hasChunkAt(pos) || !((blockEntity = level.getBlockEntity(pos)) instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
            CoasterGlueAssemblyRailSplitter.separateAnchorsTowardOutsidePeers(level, glue, anchor);
        }
    }

    private static void separateAnchorsTowardOutsidePeers(ServerLevel level, Set<BlockPos> glue, CoasterAnchorpointBlockEntity anchor) {
        for (BlockPos peer : Set.copyOf(anchor.getAnchorPeerCurvesView().keySet())) {
            if (glue.contains(peer)) continue;
            BlockPos here = anchor.getBlockPos();
            BezierConnection bc = anchor.getAnchorPeerCurvesView().get(peer);
            if (bc == null) continue;
            BlockState anchorState = anchor.getBlockState();
            SoundType sound = anchorState.getSoundType();
            CoasterGlueAssemblyRailSplitter.spawnCurveDropsDestroyFx(level, bc, here, sound);
            anchor.removeAnchorPeerCurve(level, peer);
        }
    }

    private static void spawnCurveDropsDestroyFx(ServerLevel level, BezierConnection bc, BlockPos soundPos, SoundType sound) {
        if (bc == null) {
            return;
        }
        CoasterTrackCurveDrops.spawnItems((Level)level, bc);
        bc.spawnDestroyParticles((Level)level);
        if (sound != null) {
            level.playSound(null, soundPos, sound.getBreakSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
        }
    }
}

