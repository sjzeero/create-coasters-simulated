/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  com.simibubi.create.content.trains.track.BezierConnection$Segment
 *  net.createmod.catnip.math.VecHelper
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.GameRules
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.SimulatedCoasters;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class CoasterTrackCurveDrops {
    private static final double MIN_CHORD_BLOCKS_FOR_SEGMENT_LUT = 0.500000001;

    private CoasterTrackCurveDrops() {
    }

    public static boolean isCoasterMaterial(BezierConnection bc) {
        return bc != null && bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id);
    }

    public static int trackItemCostForAnchorPair(Level level, BlockPos anchorA, BlockPos anchorB) {
        Vec3 c0 = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorA);
        Vec3 c1 = CoasterAnchorpointBlockEntity.worldCenterBase(level, anchorB);
        return CoasterTrackCurveDrops.trackItemCostForChordBlocks(c0.distanceTo(c1));
    }

    public static int trackItemCostForPreviewEndpoints(Level level, BlockPos fromAnchor, BlockPos toTarget) {
        Vec3 c0 = CoasterAnchorpointBlockEntity.worldCenterBase(level, fromAnchor);
        Vec3 c1 = CoasterAnchorpointBlockEntity.worldCenterBase(level, toTarget);
        return CoasterTrackCurveDrops.trackItemCostForChordBlocks(c0.distanceTo(c1));
    }

    public static int trackItemCostForConnection(Level level, BezierConnection bc) {
        if (!CoasterTrackCurveDrops.isCoasterMaterial(bc)) {
            return bc != null ? bc.getTrackItemCost() : 0;
        }
        return CoasterTrackCurveDrops.trackItemCostForAnchorPair(level, (BlockPos)bc.bePositions.getFirst(), (BlockPos)bc.bePositions.getSecond());
    }

    static int trackItemCostForChordBlocks(double chordBlocks) {
        double lengthForSegments = Math.max(chordBlocks, 0.500000001);
        int segmentCount = (int)(lengthForSegments * 2.0);
        return (segmentCount + 1) / 2;
    }

    public static void spawnItems(Level level, BezierConnection bc) {
        if (!CoasterTrackCurveDrops.isCoasterMaterial(bc)) {
            return;
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            return;
        }
        int tracks = CoasterTrackCurveDrops.trackItemCostForConnection(level, bc);
        if (tracks <= 0) {
            return;
        }
        Vec3 origin = Vec3.atLowerCornerOf((Vec3i)((Vec3i)bc.bePositions.getFirst()));
        ItemStack stack = new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get());
        int spawned = 0;
        for (BezierConnection.Segment segment : bc) {
            if (spawned >= tracks) break;
            if (segment.index % 2 != 0 || segment.index == bc.getSegmentCount()) continue;
            Vec3 v = VecHelper.offsetRandomly((Vec3)segment.position, (RandomSource)level.random, (float)0.125f).add(origin);
            ItemEntity entity = new ItemEntity(level, v.x, v.y, v.z, stack.copy());
            entity.setDefaultPickUpDelay();
            level.addFreshEntity((Entity)entity);
            ++spawned;
        }
        while (spawned < tracks) {
            Vec3 mid = CoasterAnchorpointBlockEntity.worldCenterBase(level, (BlockPos)bc.bePositions.getFirst()).add(CoasterAnchorpointBlockEntity.worldCenterBase(level, (BlockPos)bc.bePositions.getSecond())).scale(0.5);
            Vec3 v = VecHelper.offsetRandomly((Vec3)mid.subtract(origin), (RandomSource)level.random, (float)0.125f).add(origin);
            ItemEntity entity = new ItemEntity(level, v.x, v.y, v.z, stack.copy());
            entity.setDefaultPickUpDelay();
            level.addFreshEntity((Entity)entity);
            ++spawned;
        }
    }

    public static void addItemsToPlayer(Player player, BezierConnection bc) {
        if (!CoasterTrackCurveDrops.isCoasterMaterial(bc)) {
            return;
        }
        Level level = player.level();
        Inventory inv = player.getInventory();
        for (int tracks = CoasterTrackCurveDrops.trackItemCostForConnection(level, bc); tracks > 0; tracks -= 64) {
            inv.placeItemBackInInventory(new ItemStack((ItemLike)SimulatedCoasters.COASTER_TRACK.get(), Math.min(64, tracks)));
        }
    }
}

