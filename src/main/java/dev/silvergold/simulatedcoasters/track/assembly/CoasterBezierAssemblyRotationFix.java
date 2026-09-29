/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.contraptions.StructureTransform
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  dev.ryanhcode.sable.api.SubLevelAssemblyHelper$AssemblyTransform
 *  net.createmod.catnip.data.Couple
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction$Axis
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Mirror
 *  net.minecraft.world.level.block.Rotation
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 */
package dev.silvergold.simulatedcoasters.track.assembly;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.trains.track.BezierConnection;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.silvergold.simulatedcoasters.track.CoasterPeerCurveHandleLengths;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerFakeTracks;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlock;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CoasterBezierAssemblyRotationFix {
    private CoasterBezierAssemblyRotationFix() {
    }

    public static void afterMoveBlocks(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> sourceCells) {
        StructureTransform st = CoasterBezierAssemblyRotationFix.yAxisStructureRotation(transform);
        List<BlockPos> placed = CoasterBezierAssemblyRotationFix.collectDestinations(transform, sourceCells);
        for (BlockPos dst : placed) {
            BlockEntity be = level.getBlockEntity(dst);
            if (be instanceof CoasterAnchorpointBlockEntity) {
                CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)be;
                if (st != null) {
                    CoasterBezierAssemblyRotationFix.applyStructureToDistinctAnchorBezier(level, anchor.getBlockPos(), anchor, st);
                }
                anchor.rewriteNeighborPositionsAfterAssemblyTransform(transform);
            }
            BlockState stBl = level.getBlockState(dst);
            CoasterBezierAssemblyRotationFix.scheduleCoasterTick(level, dst, stBl.getBlock());
        }
        if (st != null) {
            CoasterBezierAssemblyRotationFix.resyncMirrorSecondariesFromPrimaries(level, placed);
        }
    }

    private static List<BlockPos> collectDestinations(SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> sourceCells) {
        ArrayList<BlockPos> placed = new ArrayList<BlockPos>();
        for (BlockPos src : sourceCells) {
            if (src == null) continue;
            placed.add(transform.apply(src).immutable());
        }
        return placed;
    }

    private static void scheduleCoasterTick(ServerLevel level, BlockPos dst, Block block) {
        if (!(block instanceof CoasterAnchorpointBlock)) {
            return;
        }
        if (!level.getBlockTicks().hasScheduledTick(dst, (Object)block)) {
            level.scheduleTick(dst, block, 1);
        }
    }

    public static void applyBezierYStructureAroundOwner(ServerLevel level, BlockPos ownerWorldPos, BezierConnection primary, StructureTransform tf) {
        if (tf.rotationAxis != Direction.Axis.Y) {
            return;
        }
        if (!primary.isPrimary()) {
            return;
        }
        Couple<Double> handleLengths = CoasterPeerCurveHandleLengths.takeCanonicalLengthsForRemap(primary);
        AnchorPeerFakeTracks.manageFakeTracksAlong(level, primary, true);
        primary.normals.replace(arg_0 -> ((StructureTransform)tf).applyWithoutOffsetUncentered(arg_0));
        primary.axes.replace(arg_0 -> ((StructureTransform)tf).applyWithoutOffsetUncentered(arg_0));
        BlockPos diff = ((BlockPos)primary.bePositions.getSecond()).subtract((Vec3i)primary.bePositions.getFirst());
        primary.bePositions.setSecond((Object)BlockPos.containing((Position)Vec3.atCenterOf((Vec3i)((Vec3i)primary.bePositions.getFirst())).add(tf.applyWithoutOffsetUncentered(Vec3.atLowerCornerOf((Vec3i)diff)))));
        Vec3 beVec = Vec3.atLowerCornerOf((Vec3i)ownerWorldPos);
        Vec3 teCenterVec = beVec.add(0.5, 0.5, 0.5);
        Vec3 start = (Vec3)primary.starts.getFirst();
        Vec3 startToBE = start.subtract(teCenterVec);
        Vec3 endToStart = ((Vec3)primary.starts.getSecond()).subtract(start);
        startToBE = tf.applyWithoutOffsetUncentered(startToBE).add(teCenterVec);
        endToStart = tf.applyWithoutOffsetUncentered(endToStart).add(startToBE);
        primary.starts.setFirst((Object)startToBE);
        primary.starts.setSecond((Object)endToStart);
        AnchorPeerFakeTracks.manageFakeTracksAlong(level, primary, false);
        if (handleLengths != null) {
            CoasterPeerCurveHandleLengths.putCanonical(primary, (Double)handleLengths.getFirst(), (Double)handleLengths.getSecond());
        }
    }

    private static void applyStructureToDistinctAnchorBezier(ServerLevel level, BlockPos owner, CoasterAnchorpointBlockEntity anchor, StructureTransform st) {
        Map<BlockPos, BezierConnection> snap = anchor.viewAnchorPeerCurvesSnapshot();
        if (snap.isEmpty()) {
            return;
        }
        for (BezierConnection bc : snap.values()) {
            if (!bc.isPrimary()) continue;
            CoasterBezierAssemblyRotationFix.applyBezierYStructureAroundOwner(level, owner, bc, st);
        }
    }

    private static void resyncMirrorSecondariesFromPrimaries(ServerLevel level, List<BlockPos> placed) {
        for (BlockPos dst : placed) {
            BlockEntity be = level.getBlockEntity(dst);
            if (!(be instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)be;
            BlockPos owner = anchor.getBlockPos();
            for (Map.Entry<BlockPos, BezierConnection> e : anchor.getAnchorPeerCurvesView().entrySet()) {
                BlockPos peer;
                BlockEntity blockEntity;
                BezierConnection bc = e.getValue();
                if (bc == null || !bc.isPrimary() || !((blockEntity = level.getBlockEntity(peer = e.getKey())) instanceof CoasterAnchorpointBlockEntity)) continue;
                CoasterAnchorpointBlockEntity peerBe = (CoasterAnchorpointBlockEntity)blockEntity;
                peerBe.overwriteReciprocalForPeerCurve(owner, bc);
            }
        }
    }

    public static StructureTransform yAxisStructureRotation(SubLevelAssemblyHelper.AssemblyTransform asm) {
        Rotation rot = asm.getRotation();
        int quarters = CoasterBezierAssemblyRotationFix.readQuarterTurnSteps(asm);
        if (rot == Rotation.NONE && quarters != 0) {
            rot = Rotation.values()[Math.floorMod(4 - quarters, 4)];
        }
        if (rot == Rotation.NONE) {
            return null;
        }
        return new StructureTransform(BlockPos.ZERO, Direction.Axis.Y, rot, Mirror.NONE);
    }

    private static int readQuarterTurnSteps(SubLevelAssemblyHelper.AssemblyTransform asm) {
        try {
            Field field = asm.getClass().getDeclaredField("angle");
            field.setAccessible(true);
            return field.getInt(asm);
        }
        catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }
}

