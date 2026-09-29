/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.OpenEndExtensionPreviewProvider;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import dev.silvergold.simulatedcoasters.track.graph.CoasterPathGraphManager;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CoasterOpenEndExtension {
    private CoasterOpenEndExtension() {
    }

    public static float extensionBlocks(Level level, BlockPos anchorPos) {
        Float preview;
        if (level.isClientSide() && (preview = OpenEndExtensionPreviewProvider.extensionBlocksAt(anchorPos)) != null) {
            return CoasterOpenEndExtension.clampExtensionBlocks(preview.floatValue());
        }
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return 0.5f;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        return be.getOpenEndExtensionBlocks();
    }

    public static float extensionBlocksForCurveEndpoint(Level level, BezierConnection bc, boolean atFirstEndpoint) {
        CoasterAnchorpointBlockEntity be;
        BlockPos anchor = atFirstEndpoint ? (BlockPos)bc.bePositions.getFirst() : (BlockPos)bc.bePositions.getSecond();
        BlockEntity blockEntity = level.getBlockEntity(anchor);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity) || (be = (CoasterAnchorpointBlockEntity)blockEntity).legCount() != 1) {
            return 0.5f;
        }
        return CoasterOpenEndExtension.extensionBlocks(level, anchor);
    }

    public static boolean isOpenEndAnchor(Level level, BlockPos anchorPos) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return false;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        return be.legCount() == 1;
    }

    @Nullable
    public static BezierConnection primaryCoasterCurveAt(Level level, BlockPos anchorPos) {
        BlockEntity blockEntity = level.getBlockEntity(anchorPos);
        if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) {
            return null;
        }
        CoasterAnchorpointBlockEntity be = (CoasterAnchorpointBlockEntity)blockEntity;
        for (BezierConnection cRaw : be.getAnchorPeerCurvesView().values()) {
            BezierConnection p;
            if (cRaw == null) continue;
            BezierConnection bezierConnection = p = cRaw.isPrimary() ? cRaw : cRaw.secondary();
            if (!p.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) continue;
            return p;
        }
        return null;
    }

    public static Vec3 tipPlotLocalAt(Level level, BlockPos anchorPos, BezierConnection bc) {
        return CoasterOpenEndExtension.tipPlotLocal(bc, anchorPos, CoasterOpenEndExtension.extensionBlocks(level, anchorPos));
    }

    public static Vec3 junctionPlotLocal(BezierConnection bc, BlockPos anchorPos) {
        double t = ((BlockPos)bc.bePositions.getFirst()).equals((Object)anchorPos) ? 0.0 : 1.0;
        return CoasterPathGraphManager.pathCenterlineWorld(CoasterPathGraphManager.coasterBezierParametricSpineRaw(bc, t));
    }

    public static Vec3 outwardPlotLocal(BezierConnection bc, BlockPos anchorPos) {
        double t = ((BlockPos)bc.bePositions.getFirst()).equals((Object)anchorPos) ? 0.0 : 1.0;
        Vec3 tan = CoasterPathGraphManager.coasterBezierPlotLocalTangent(bc, t);
        tan = tan.lengthSqr() < 1.0E-14 ? new Vec3(0.0, 0.0, 1.0) : tan.normalize();
        return ((BlockPos)bc.bePositions.getFirst()).equals((Object)anchorPos) ? tan.scale(-1.0) : tan;
    }

    public static Vec3 tipPlotLocal(BezierConnection bc, BlockPos anchorPos, float extensionBlocks) {
        return CoasterOpenEndExtension.junctionPlotLocal(bc, anchorPos).add(CoasterOpenEndExtension.outwardPlotLocal(bc, anchorPos).scale((double)extensionBlocks));
    }

    public static float clampExtensionBlocks(float blocks) {
        return Mth.clamp((float)blocks, (float)0.15f, (float)1.25f);
    }

    public static float snapExtensionBlocksToCenter(float blocks) {
        float center;
        float clamped = CoasterOpenEndExtension.clampExtensionBlocks(blocks);
        if (Math.abs(clamped - (center = 0.5f)) <= 0.08f) {
            return center;
        }
        return clamped;
    }

    public static Vec3 virtualTargetPlotLocalForExtensionBlocks(BezierConnection bc, BlockPos anchorPos, Vec3 virtualTargetPlotLocal, float extensionBlocks) {
        Vec3 junction = CoasterOpenEndExtension.junctionPlotLocal(bc, anchorPos);
        Vec3 outward = CoasterOpenEndExtension.outwardPlotLocal(bc, anchorPos);
        if (outward.lengthSqr() < 1.0E-14) {
            return virtualTargetPlotLocal;
        }
        outward = outward.normalize();
        double rawAlong = virtualTargetPlotLocal.subtract(junction).dot(outward);
        return virtualTargetPlotLocal.add(outward.scale((double)extensionBlocks - rawAlong));
    }

    @Nullable
    public static Float extensionBlocksFromVirtualTarget(Level level, BlockPos anchorPos, BezierConnection bc, Vec3 virtualTargetPlotLocal, boolean applyCenterSnap) {
        if (!CoasterOpenEndExtension.isOpenEndAnchor(level, anchorPos)) {
            return null;
        }
        Vec3 junction = CoasterOpenEndExtension.junctionPlotLocal(bc, anchorPos);
        Vec3 outward = CoasterOpenEndExtension.outwardPlotLocal(bc, anchorPos);
        if (outward.lengthSqr() < 1.0E-14) {
            return null;
        }
        double along = virtualTargetPlotLocal.subtract(junction).dot(outward);
        float clamped = CoasterOpenEndExtension.clampExtensionBlocks((float)along);
        return Float.valueOf(applyCenterSnap ? CoasterOpenEndExtension.snapExtensionBlocksToCenter(clamped) : clamped);
    }

    @Nullable
    public static Float extensionBlocksFromVirtualTarget(Level level, BlockPos anchorPos, BezierConnection bc, Vec3 virtualTargetPlotLocal) {
        return CoasterOpenEndExtension.extensionBlocksFromVirtualTarget(level, anchorPos, bc, virtualTargetPlotLocal, true);
    }
}

