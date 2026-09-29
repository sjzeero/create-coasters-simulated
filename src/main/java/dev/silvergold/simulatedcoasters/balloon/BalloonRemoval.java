/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllSoundEvents
 *  dev.ryanhcode.sable.sublevel.ServerSubLevel
 *  dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.core.particles.BlockParticleOption
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 */
package dev.silvergold.simulatedcoasters.balloon;

import com.simibubi.create.AllSoundEvents;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.EmbeddedPlotLevelAccessor;
import dev.silvergold.simulatedcoasters.SimulatedCoastersBlocks;
import dev.silvergold.simulatedcoasters.SimulatedCoastersSoundEvents;
import dev.silvergold.simulatedcoasters.balloon.BalloonBlockEntity;
import dev.silvergold.simulatedcoasters.balloon.BalloonCascadePop;
import dev.silvergold.simulatedcoasters.balloon.BalloonHostIndex;
import dev.silvergold.simulatedcoasters.balloon.BalloonTetherController;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class BalloonRemoval {
    private static final Set<UUID> REMOVING = ConcurrentHashMap.newKeySet();
    private static final float POP_PITCH_SPREAD = 0.08f;

    private BalloonRemoval() {
    }

    public static boolean isRemoving(UUID balloonSubLevelId) {
        return REMOVING.contains(balloonSubLevelId);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void pop(ServerLevel level, ServerSubLevel balloonSub) {
        if (balloonSub.isRemoved()) {
            return;
        }
        if (!REMOVING.add(balloonSub.getUniqueId())) {
            return;
        }
        UUID balloonId = balloonSub.getUniqueId();
        UUID hostSubLevelId = BalloonCascadePop.hostSubLevelIdOf(level, balloonSub);
        Vector3d poppedPos = new Vector3d((Vector3dc)balloonSub.logicalPose().position());
        try {
            BalloonRemoval.playPopEffects(level, balloonSub);
            BalloonRemoval.detachAndRemove(level, balloonSub, true);
        }
        finally {
            REMOVING.remove(balloonId);
        }
        BalloonCascadePop.onBalloonPopped(level, hostSubLevelId, poppedPos, balloonId);
    }

    public static boolean safelyRemove(ServerLevel level, ServerSubLevel balloonSub) {
        if (balloonSub.isRemoved()) {
            return false;
        }
        if (!REMOVING.add(balloonSub.getUniqueId())) {
            return false;
        }
        UUID balloonId = balloonSub.getUniqueId();
        try {
            BalloonRemoval.playPickupEffects(level, balloonSub);
            BalloonRemoval.detachAndRemove(level, balloonSub, true);
        }
        finally {
            REMOVING.remove(balloonId);
        }
        return true;
    }

    public static void onHostBroken(ServerLevel level, ServerSubLevel balloonSub) {
        BalloonRemoval.pop(level, balloonSub);
    }

    public static void onBalloonBlockRemoved(ServerLevel level, ServerSubLevel balloonSub) {
        if (balloonSub.isRemoved() || BalloonRemoval.isRemoving(balloonSub.getUniqueId())) {
            return;
        }
        BalloonRemoval.detachAndRemove(level, balloonSub, false);
    }

    private static void detachAndRemove(ServerLevel level, ServerSubLevel balloonSub, boolean breakBalloonBlock) {
        EmbeddedPlotLevelAccessor embedded;
        BlockState state;
        if (balloonSub.isRemoved()) {
            return;
        }
        BalloonBlockEntity be = BalloonRemoval.balloonBlockEntity(balloonSub);
        if (be != null) {
            BalloonHostIndex.get(level).unregisterBalloon(balloonSub.getUniqueId());
        }
        BalloonTetherController.unregister(balloonSub.getUniqueId());
        if (breakBalloonBlock && (state = (embedded = balloonSub.getPlot().getEmbeddedLevelAccessor()).getBlockState(BlockPos.ZERO)).is((Block)SimulatedCoastersBlocks.RED_BALLOON.get())) {
            embedded.setBlock(BlockPos.ZERO, Blocks.AIR.defaultBlockState(), 3);
        }
        if (!balloonSub.isRemoved()) {
            balloonSub.markRemoved();
        }
    }

    private static void playPopEffects(ServerLevel level, ServerSubLevel balloonSub) {
        BlockPos cell = balloonSub.getPlot().getCenterBlock();
        BlockState state = balloonSub.getPlot().getEmbeddedLevelAccessor().getBlockState(BlockPos.ZERO);
        double x = (double)cell.getX() + 0.5;
        double y = (double)cell.getY() + 0.5;
        double z = (double)cell.getZ() + 0.5;
        float pitch = 1.0f + (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 0.08f;
        level.playSound(null, cell, (SoundEvent)SimulatedCoastersSoundEvents.BALLOON_POP.get(), SoundSource.BLOCKS, 1.0f, pitch);
        if (state.is((Block)SimulatedCoastersBlocks.RED_BALLOON.get())) {
            level.sendParticles((ParticleOptions)new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z, 12, 0.2, 0.2, 0.2, 0.05);
        }
        level.sendParticles((ParticleOptions)ParticleTypes.POOF, x, y, z, 8, 0.15, 0.15, 0.15, 0.02);
    }

    private static void playPickupEffects(ServerLevel level, ServerSubLevel balloonSub) {
        BlockPos cell = balloonSub.getPlot().getCenterBlock();
        float pitch = level.getRandom().nextFloat() * 0.5f + 0.5f;
        AllSoundEvents.WRENCH_REMOVE.playOnServer((Level)level, (Vec3i)cell, 1.0f, pitch);
    }

    @Nullable
    private static BalloonBlockEntity balloonBlockEntity(ServerSubLevel balloonSub) {
        EmbeddedPlotLevelAccessor embedded = balloonSub.getPlot().getEmbeddedLevelAccessor();
        BlockEntity blockEntity = embedded.getBlockEntity(BlockPos.ZERO);
        if (blockEntity instanceof BalloonBlockEntity) {
            BalloonBlockEntity be = (BalloonBlockEntity)blockEntity;
            return be;
        }
        return null;
    }
}

