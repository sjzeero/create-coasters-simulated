/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.AABB
 *  net.neoforged.api.distmarker.Dist
 *  net.neoforged.api.distmarker.OnlyIn
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.CoasterTrackMaterials;
import dev.silvergold.simulatedcoasters.track.anchor.AnchorPeerCurvePendingRemoval;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(value=Dist.CLIENT)
public final class AnchorPeerCurveOutlineSupport {
    private static final double MAX_OUTLINE_SPAN_BLOCKS = 512.0;

    private AnchorPeerCurveOutlineSupport() {
    }

    @Nullable
    public static BezierConnection resolveHostedPrimary(Level level, @Nullable BezierConnection bc) {
        if (bc == null || level == null) {
            return null;
        }
        if (!bc.getMaterial().id.equals((Object)CoasterTrackMaterials.COASTER.id)) {
            return null;
        }
        if (AnchorPeerCurvePendingRemoval.isPending(bc)) {
            return null;
        }
        BezierConnection pri = bc.isPrimary() ? bc : bc.secondary();
        BlockPos a = (BlockPos)pri.bePositions.getFirst();
        BlockPos b = (BlockPos)pri.bePositions.getSecond();
        for (BlockPos host : new BlockPos[]{a, b}) {
            BezierConnection resolved;
            BlockEntity blockEntity = level.getBlockEntity(host);
            if (!(blockEntity instanceof CoasterAnchorpointBlockEntity)) continue;
            CoasterAnchorpointBlockEntity anchor = (CoasterAnchorpointBlockEntity)blockEntity;
            BlockPos peer = host.equals((Object)a) ? b : a;
            BezierConnection hosted = anchor.getAnchorPeerCurvesView().get(peer);
            if (hosted == null) continue;
            BezierConnection bezierConnection = resolved = hosted.isPrimary() ? hosted : hosted.secondary();
            if (AnchorPeerCurvePendingRemoval.isPending(resolved)) {
                return null;
            }
            return resolved;
        }
        return null;
    }

    public static boolean hasSaneOutlineBounds(BezierConnection bc) {
        AABB bounds = bc.getBounds();
        double dx = bounds.getXsize();
        double dy = bounds.getYsize();
        double dz = bounds.getZsize();
        if (!(Double.isFinite(dx) && Double.isFinite(dy) && Double.isFinite(dz))) {
            return false;
        }
        if (dx <= 0.0 || dy <= 0.0 || dz <= 0.0) {
            return false;
        }
        return dx <= 512.0 && dy <= 512.0 && dz <= 512.0;
    }
}

