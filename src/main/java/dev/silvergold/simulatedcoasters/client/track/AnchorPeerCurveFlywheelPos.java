/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.trains.track.BezierConnection
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.jetbrains.annotations.Nullable
 */
package dev.silvergold.simulatedcoasters.client.track;

import com.simibubi.create.content.trains.track.BezierConnection;
import dev.silvergold.simulatedcoasters.client.track.AnchorPeerCurveMineTarget;
import dev.silvergold.simulatedcoasters.track.anchor.CoasterAnchorpointBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public final class AnchorPeerCurveFlywheelPos {
    private AnchorPeerCurveFlywheelPos() {
    }

    public static BlockPos crumbleAnchor(Level level, AnchorPeerCurveMineTarget target) {
        return AnchorPeerCurveFlywheelPos.crumbleAnchor(level, target.mined(), target.peer());
    }

    public static BlockPos crumbleAnchor(Level level, BlockPos mined, BlockPos peer) {
        @Nullable BlockPos primaryPos = AnchorPeerCurveFlywheelPos.primaryHolder(level, mined, peer);
        return primaryPos != null ? primaryPos : mined;
    }

    @Nullable
    private static BlockPos primaryHolder(Level level, BlockPos mined, BlockPos peer) {
        CoasterAnchorpointBlockEntity ap;
        BezierConnection bc;
        CoasterAnchorpointBlockEntity am;
        BezierConnection bc2;
        BlockEntity atMined = level.getBlockEntity(mined);
        if (atMined instanceof CoasterAnchorpointBlockEntity && (bc2 = (am = (CoasterAnchorpointBlockEntity)atMined).getAnchorPeerCurvesView().get(peer)) != null && bc2.isPrimary()) {
            return mined;
        }
        BlockEntity atPeer = level.getBlockEntity(peer);
        if (atPeer instanceof CoasterAnchorpointBlockEntity && (bc = (ap = (CoasterAnchorpointBlockEntity)atPeer).getAnchorPeerCurvesView().get(mined)) != null && bc.isPrimary()) {
            return peer;
        }
        return null;
    }
}

