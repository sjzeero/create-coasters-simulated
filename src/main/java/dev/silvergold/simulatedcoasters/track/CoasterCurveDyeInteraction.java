/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.item.AxeItem
 *  net.minecraft.world.item.DyeItem
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.client.track.CoasterCurveRailInstancing;
import dev.silvergold.simulatedcoasters.track.CoasterCurveTieredDye;
import dev.silvergold.simulatedcoasters.track.CoasterCurveTintColors;
import dev.silvergold.simulatedcoasters.track.CoasterCurveUndyeParticles;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterCurveDyeInteraction {
    private CoasterCurveDyeInteraction() {
    }

    public static void applyRailTintFromPreview(ServerPlayer player, BlockPos curveOwner, BlockPos curveRemote, Vec3 soundPos, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Item item = stack.getItem();
        if (!(item instanceof DyeItem)) {
            return;
        }
        DyeItem dyeItem = (DyeItem)item;
        ServerLevel level = player.serverLevel();
        if (!CoasterCurveDyeInteraction.validateHostedCurveEndpoints((Level)level, curveOwner, curveRemote)) {
            return;
        }
        if (CoasterCurveDyeInteraction.applyRailTint((Level)level, curveOwner, curveRemote, dyeItem)) {
            CoasterCurveDyeInteraction.playCurveTintSound(level, soundPos, SoundEvents.HONEYCOMB_WAX_ON, 0.55f);
            player.swing(hand, true);
        }
    }

    public static void clearDyeFromPreview(ServerPlayer player, BlockPos curveOwner, BlockPos curveRemote, Vec3 soundPos, InteractionHand hand) {
        if (!(player.getItemInHand(hand).getItem() instanceof AxeItem)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!CoasterCurveDyeInteraction.validateHostedCurveEndpoints((Level)level, curveOwner, curveRemote)) {
            return;
        }
        if (CoasterCurveDyeInteraction.clearDyeAtCurve((Level)level, curveOwner, curveRemote, soundPos)) {
            player.swing(hand, true);
        }
    }

    private static boolean applyRailTint(Level level, BlockPos owner, BlockPos remote, DyeItem dyeItem) {
        int diffuseRgb = CoasterCurveTintColors.diffuseRgbFromDye(dyeItem.getDyeColor());
        int beam = CoasterCurveDyeInteraction.getStoredCurveBeamRgb(level, owner, remote);
        return CoasterCurveDyeInteraction.applyTieredTintsToOwner(level, owner, remote, diffuseRgb, beam);
    }

    static boolean validateHostedCurveEndpoints(Level level, BlockPos owner, BlockPos remote) {
        BezierConnection primary;
        BlockEntity blockEntity = level.getBlockEntity(owner);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
        BezierConnection raw = anchor.getAnchorPeerCurvesView().get(remote);
        if (raw == null) {
            return false;
        }
        BezierConnection bezierConnection = primary = raw.isPrimary() ? raw : raw.secondary();
        if (!primary.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return false;
        }
        if (!CoasterCurveRailInstancing.useSplineRails(primary)) {
            return false;
        }
        return ((BlockPos)primary.bePositions.getFirst()).equals((Object)owner) && ((BlockPos)primary.bePositions.getSecond()).equals((Object)remote);
    }

    private static boolean clearDyeAtCurve(Level level, BlockPos owner, BlockPos remote, Vec3 soundPos) {
        int beam;
        BezierConnection primary = CoasterCurveDyeInteraction.primaryForHostedEndpoints(level, owner, remote);
        if (primary == null) {
            return false;
        }
        int rail = CoasterCurveDyeInteraction.getStoredCurveRailRgb(level, owner, remote);
        CoasterCurveTieredDye.AxeStep step = CoasterCurveTieredDye.nextAxeStep(rail, beam = CoasterCurveDyeInteraction.getStoredCurveBeamRgb(level, owner, remote));
        if (step.isNoOp()) {
            return false;
        }
        if (!CoasterCurveDyeInteraction.applyTieredTintsToOwner(level, owner, remote, step.newRailRgb(), step.newBeamRgb())) {
            return false;
        }
        if (level instanceof ServerLevel) {
            ServerLevel sl = (ServerLevel)level;
            int cleared = step.clearedRgbForParticles();
            if (cleared != 0) {
                CoasterCurveUndyeParticles.spawnAlongCurve(sl, primary, cleared);
            }
            CoasterCurveDyeInteraction.playCurveTintSound(sl, soundPos, SoundEvents.AXE_WAX_OFF, 0.6f);
        }
        return true;
    }

    private static void playCurveTintSound(ServerLevel level, Vec3 soundPos, SoundEvent event, float volume) {
        level.playSound(null, soundPos.x, soundPos.y, soundPos.z, event, SoundSource.PLAYERS, volume, 1.0f);
    }

    @Nullable
    private static BezierConnection primaryForHostedEndpoints(Level level, BlockPos owner, BlockPos remote) {
        BezierConnection primary;
        BlockEntity blockEntity = level.getBlockEntity(owner);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
        BezierConnection raw = anchor.getAnchorPeerCurvesView().get(remote);
        if (raw == null) {
            return null;
        }
        BezierConnection bezierConnection = primary = raw.isPrimary() ? raw : raw.secondary();
        if (!primary.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return null;
        }
        if (!CoasterCurveRailInstancing.useSplineRails(primary)) {
            return null;
        }
        return primary;
    }

    private static int getStoredCurveRailRgb(Level level, BlockPos owner, BlockPos remote) {
        BlockEntity blockEntity = level.getBlockEntity(owner);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity ape = (CoasterAnchorpointBlockEntity)blockEntity;
            return ape.getPeerCurveRailDiffuseRgb(remote);
        }
        return 0;
    }

    private static int getStoredCurveBeamRgb(Level level, BlockPos owner, BlockPos remote) {
        BlockEntity blockEntity = level.getBlockEntity(owner);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity ape = (CoasterAnchorpointBlockEntity)blockEntity;
            return ape.getPeerCurveBeamDiffuseRgb(remote);
        }
        return 0;
    }

    private static boolean applyTieredTintsToOwner(Level level, BlockPos owner, BlockPos remote, int railRgb, int beamRgb) {
        BlockEntity blockEntity = level.getBlockEntity(owner);
        if (blockEntity instanceof CoasterAnchorpointBlockEntity) {
            CoasterAnchorpointBlockEntity ape = (CoasterAnchorpointBlockEntity)blockEntity;
            return ape.setPeerCurveTieredTints(remote, railRgb, beamRgb);
        }
        return false;
    }
}

